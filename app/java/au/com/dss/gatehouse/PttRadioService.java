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
            startForeground(NOTIFICATION_ID, buildServiceNotification("Channel 1 · standby"));
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
        GatehouseNotify.prepare(this);
        NotificationChannel chan = new NotificationChannel(CHANNEL_ID, "Push-to-talk", NotificationManager.IMPORTANCE_LOW);
        chan.setDescription("Keeps the radio connected in the background");
        chan.setShowBadge(false);
        try { chan.setGroup(GatehouseNotify.GROUP_APP); } catch (Throwable ignored) {}
        if (notificationManager != null) notificationManager.createNotificationChannel(chan);
    }

    private Notification buildServiceNotification(String statusText) {
        Intent appIntent = new Intent(this, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = GatehouseNotify.open(this, 0, appIntent);

        return GatehouseNotify.builder(this, CHANNEL_ID, GatehouseNotify.Tier.NOTICE)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle("Push-to-talk radio")
                .setContentText(statusText)
                .setContentIntent(pi)
                .setOngoing(true)
                .setAutoCancel(false)
                .setShowWhen(false)
                .build();
    }

    @Override
    public void onTxStateChanged(boolean isTransmitting) {
        if (notificationManager != null) {
            String text = isTransmitting ? "Transmitting · channel 1" : "Channel 1 · standby";
            notificationManager.notify(NOTIFICATION_ID, buildServiceNotification(text));
        }
    }

    @Override
    public void onRxStateChanged(boolean isReceiving, String senderName) {
        if (notificationManager != null) {
            String text = isReceiving ? ("Receiving · " + (senderName.isEmpty() ? "Desk" : senderName)) : "Channel 1 · standby";
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
