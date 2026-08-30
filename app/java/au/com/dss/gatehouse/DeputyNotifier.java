package au.com.dss.gatehouse;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

public class DeputyNotifier {
    public static final String ACTION_OPEN_DEPUTY = "au.com.dss.gatehouse.ACTION_OPEN_DEPUTY";
    public static final String CHANNEL_DEPUTY = "dss_deputy_roster_channel";
    public static final String CHANNEL_SHIFTS = "dss_shift_alerts_channel";

    public static void initChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel ch1 = new NotificationChannel(
                        CHANNEL_DEPUTY,
                        "Deputy Roster Updates",
                        NotificationManager.IMPORTANCE_DEFAULT);
                ch1.setDescription("Notifications for roster sync and shift schedule changes");
                nm.createNotificationChannel(ch1);

                NotificationChannel ch2 = new NotificationChannel(
                        CHANNEL_SHIFTS,
                        "Shift Alerts & Weather",
                        NotificationManager.IMPORTANCE_HIGH);
                ch2.setDescription("Alerts for shift starts, handovers, and weather conditions");
                nm.createNotificationChannel(ch2);
            }
        }
    }

    public static void clearNotificationHistory(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancelAll();
        }
    }

    public static void schedulePeriodicAlarm(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                Intent intent = new Intent(context, DeputyAlarmReceiver.class);
                intent.setAction("au.com.dss.gatehouse.DEPUTY_SYNC_ALARM");
                PendingIntent pi = PendingIntent.getBroadcast(
                        context,
                        101,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
                am.setInexactRepeating(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 15 * 60 * 1000,
                        15 * 60 * 1000,
                        pi);
            }
        } catch (Exception e) {}
    }

    public static void processSyncResult(Context context, DeputyApi.DeputyRosterResult result) {
    }
}