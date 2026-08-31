package au.com.dss.gatehouse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * PttRadioService — Android Foreground Service for Push-to-Talk Digital Radio.
 * 
 * Ensures continuous background listening and real-time audio playback through
 * the phone speaker / Bluetooth earpiece even when the screen is locked in a pocket.
 */
public class PttRadioService extends Service implements PttRadioEngine.PttListener {
    private static final String TAG = "PttRadioService";

    public static final String CHANNEL_ID = "ptt_radio_service_channel";
    public static final int NOTIFICATION_ID = 41208;

    public static final String ACTION_START_RADIO = "au.com.dss.gatehouse.START_RADIO";
    public static final String ACTION_STOP_RADIO = "au.com.dss.gatehouse.STOP_RADIO";
    public static final String ACTION_REPLAY_CALL = "au.com.dss.gatehouse.REPLAY_CALL";

    private PttRadioEngine engine;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        initNotificationChannel();

        engine = PttRadioEngine.getInstance(this);
        engine.setListener(this);
        engine.start();

        try {
            startForeground(NOTIFICATION_ID, buildServiceNotification("● DSS Digital Radio Active · Channel 01"));
            Log.i(TAG, "PttRadioService started in foreground");
        } catch (Throwable t) {
            Log.w(TAG, "Foreground notification start deferred: " + t.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP_RADIO.equals(action)) {
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_REPLAY_CALL.equals(action)) {
                if (engine != null) engine.replayLastCall();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (engine != null) {
            engine.stop();
        }
        super.onDestroy();
        Log.i(TAG, "PttRadioService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "DSS Digital Push-to-Talk Radio",
                    NotificationManager.IMPORTANCE_LOW
            );
            chan.setDescription("Maintains background connectivity for 2-way digital radio audio reception");
            chan.setShowBadge(false);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(chan);
            }
        }
    }

    private Notification buildServiceNotification(String statusText) {
        Intent appIntent = new Intent(this, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_LOW);
        }

        int iconShield = getResources().getIdentifier("ic_shield_gold", "drawable", getPackageName());
        if (iconShield == 0) iconShield = getApplicationInfo().icon;

        builder.setSmallIcon(iconShield)
                .setContentTitle("🛡️ DSS Push-to-Talk Radio")
                .setContentText(statusText)
                .setContentIntent(pi)
                .setOngoing(true);

        try {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), getApplicationInfo().icon));
        } catch (Throwable ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(0xFFE5A93C); // DSS Gold
        }

        return builder.build();
    }

    @Override
    public void onTxStateChanged(boolean isTransmitting) {
        if (notificationManager != null) {
            String text = isTransmitting ? "🔴 TRANSMITTING AUDIO · Channel 01" : "● DSS Digital Radio Active · Channel 01";
            notificationManager.notify(NOTIFICATION_ID, buildServiceNotification(text));
        }
    }

    @Override
    public void onRxStateChanged(boolean isReceiving, String senderName) {
        if (notificationManager != null) {
            String text = isReceiving ? ("🔊 INCOMING RADIO: " + (senderName.isEmpty() ? "Desk" : senderName)) : "● DSS Digital Radio Active · Channel 01";
            notificationManager.notify(NOTIFICATION_ID, buildServiceNotification(text));
        }
    }

    @Override
    public void onPeerDetected(String peerId, String name, long lastSeenMs) {}

    @Override
    public void onAudioLevelChanged(int decibels) {}

    @Override
    public void onError(String message) {}
}
