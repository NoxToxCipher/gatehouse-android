package au.com.dss.gatehouse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Holds the accelerometer open all shift on a hut phone and hands each
 * sample to {@link FallDetector}. When it calls a fall, this posts the
 * check-in over the lock screen and goes looking for a location fix, so
 * the text that may follow carries somewhere to drive to.
 *
 * Where the phone offers a wake-up accelerometer the CPU sleeps between
 * samples. Where it does not (the Galaxy A20 does not) a partial wake lock
 * keeps sampling alive with the screen off, which is the battery cost the
 * hut phones carry so that personal phones need not.
 */
public class FallDetectionService extends Service implements SensorEventListener, FallDetector.Listener {
    private static final String TAG = "FallDetection";
    static final String CHANNEL_SERVICE = "fall_detection_service";
    static final String CHANNEL_CHECKIN = "fall_checkin";
    static final int NOTIF_SERVICE = 0x5A10;
    static final int NOTIF_CHECKIN = 0x5A11;
    /** A test fall, delivered after a delay so the phone can be locked first. */
    static final String ACTION_TEST_FALL = "au.com.dss.gatehouse.TEST_FALL";
    /** 25 Hz: enough to see a 100 ms free-fall and a 50 ms impact, half the wake-ups of 50 Hz. */
    private static final int SAMPLE_US = 40_000;

    private SensorManager sensors;
    private Sensor accel;
    private boolean wakeUpSensor;
    private PowerManager.WakeLock wakeLock;
    private FallDetector detector;
    private long lastSampleMs;
    private LocationManager locations;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels();
        startForegroundQuietly();
        detector = new FallDetector(this);
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensors != null) {
            accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true);
            wakeUpSensor = accel != null;
            if (accel == null) accel = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (accel == null) {
            Log.w(TAG, "No accelerometer; fall detection cannot run here");
            stopSelf();
            return;
        }
        if (!wakeUpSensor) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "gatehouse:fall");
                wakeLock.acquire();
            }
        }
        sensors.registerListener(this, accel, SAMPLE_US, 0);
        locations = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Log.i(TAG, "Watching on " + MainActivity.getHutPhoneHardwareTag()
                + (wakeUpSensor ? " (wake-up sensor)" : " (wake lock)"));
        handler.postDelayed(recheck, RECHECK_MS);
    }

    /** The staffing rule can change at a shift boundary; stop when it says so. */
    private static final long RECHECK_MS = 15L * 60 * 1000;
    private final Runnable recheck = new Runnable() {
        @Override public void run() {
            if (!FallDetection.shouldRun(FallDetectionService.this)) {
                Log.i(TAG, "Standing down: the staffing rule no longer wants this phone watching");
                stopSelf();
                return;
            }
            handler.postDelayed(this, RECHECK_MS);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!FallDetection.shouldRun(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && ACTION_TEST_FALL.equals(intent.getAction())) {
            final long delay = intent.getLongExtra("delay", 10_000L);
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    long now = System.currentTimeMillis();
                    FallDetection.setLastEvent(FallDetectionService.this, "Test check-in " + FallDetection.clock(now));
                    seekLocation();
                    openCheckIn(now, "manual test", true);
                }
            }, delay);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try { if (sensors != null) sensors.unregisterListener(this); } catch (Throwable ignored) {}
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Throwable ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ------------------------------------------------------------- sensing

    @Override
    public void onSensorChanged(SensorEvent event) {
        lastSampleMs = event.timestamp / 1_000_000L;
        detector.feed(lastSampleMs, event.values[0], event.values[1], event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onFall(FallDetector.Event e) {
        long wallImpact = System.currentTimeMillis() - (lastSampleMs - e.impactElapsedMs);
        Log.w(TAG, "Fall called: " + e.describe());
        FallDetection.setLastEvent(this, "Detected " + FallDetection.clock(wallImpact) + ": " + e.describe());
        seekLocation();
        openCheckIn(wallImpact, e.describe(), false);
    }

    private void seekLocation() {
        if (locations == null) return;
        try {
            Location best = null;
            for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                Location l = locations.getLastKnownLocation(p);
                if (l != null && (best == null || l.getElapsedRealtimeNanos() > best.getElapsedRealtimeNanos())) best = l;
            }
            if (best != null) FallDetection.lastFix = best;
            LocationListener take = new LocationListener() {
                @Override public void onLocationChanged(Location l) {
                    Location cur = FallDetection.lastFix;
                    if (cur == null || l.getAccuracy() <= cur.getAccuracy()
                            || l.getElapsedRealtimeNanos() - cur.getElapsedRealtimeNanos() > 60_000_000_000L) {
                        FallDetection.lastFix = l;
                    }
                }
                @Override public void onStatusChanged(String p, int s, Bundle b) {}
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
            };
            for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                try { locations.requestSingleUpdate(p, take, getMainLooper()); } catch (Throwable ignored) {}
            }
        } catch (SecurityException se) {
            Log.w(TAG, "No location permission; the text will say so");
        } catch (Throwable ignored) {}
    }

    private void openCheckIn(long wallImpact, String detail, boolean test) {
        Intent i = new Intent(this, FallCheckInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(FallCheckInActivity.EXTRA_IMPACT_AT, wallImpact)
                .putExtra(FallCheckInActivity.EXTRA_DETAIL, detail)
                .putExtra(FallCheckInActivity.EXTRA_TEST, test);
        PendingIntent full = PendingIntent.getActivity(this, NOTIF_CHECKIN, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            Notification n = GatehouseNotify.builder(this, CHANNEL_CHECKIN, GatehouseNotify.Tier.ALERT)
                    .setContentTitle("Are you all right?")
                    .setContentText(MainActivity.getHutPhoneHardwareTag() + " felt a hard fall. Tap to answer.")
                    .setContentIntent(full)
                    .setFullScreenIntent(full, true)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .build();
            nm.notify(NOTIF_CHECKIN, n);
        }
        // Works when the app is already up; the full-screen intent covers the rest.
        try { startActivity(i); } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------- foreground

    private void createChannels() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        GatehouseNotify.prepare(this);
        NotificationChannel svc = new NotificationChannel(CHANNEL_SERVICE, "Fall detection", NotificationManager.IMPORTANCE_LOW);
        svc.setDescription("Keeps watching for a hard fall while the screen is off");
        svc.setShowBadge(false);
        try { svc.setGroup(GatehouseNotify.GROUP_APP); } catch (Throwable ignored) {}
        nm.createNotificationChannel(svc);
        GatehouseNotify.createChannel(this, CHANNEL_CHECKIN, "Fall check-in",
                "The question asked after a hard fall, over the lock screen",
                GatehouseNotify.Tier.ALERT, GatehouseNotify.GROUP_SAFETY);
    }

    private void startForegroundQuietly() {
        Intent open = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = GatehouseNotify.open(this, NOTIF_SERVICE, open);
        Notification n = GatehouseNotify.builder(this, CHANNEL_SERVICE, GatehouseNotify.Tier.NOTICE)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle("Fall detection · on")
                .setContentText(MainActivity.getHutPhoneHardwareTag() + " · watching for a hard fall")
                .setContentIntent(pi)
                .setOngoing(true)
                .setAutoCancel(false)
                .setShowWhen(false)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_SERVICE, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_SERVICE, n);
        }
    }

    /**
     * Asks the running service to stage a test fall after a delay, through the
     * same path a real one takes. False where the service is not running.
     */
    static boolean requestTestFall(Context ctx, long delayMs) {
        if (!FallDetection.shouldRun(ctx)) return false;
        try {
            ctx.startForegroundService(new Intent(ctx, FallDetectionService.class)
                    .setAction(ACTION_TEST_FALL).putExtra("delay", delayMs));
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Brings the detector back after a reboot, on hut phones only. */
    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
            FallDetection.scheduleTicks(context);
            FallDetection.ensureRunning(context);
        }
    }

    /** Every fifteen minutes: start or stop according to the staffing rule. */
    public static class TickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            FallDetection.ensureRunning(context);
        }
    }
}
