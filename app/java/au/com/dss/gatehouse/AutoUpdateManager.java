package au.com.dss.gatehouse;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

public final class AutoUpdateManager {

    private static final String PREFS_NAME = "gatehouse_autoupdate";
    private static final String KEY_LAST_CHECK = "last_check_ms";
    private static final String KEY_LAST_SHA = "last_installed_sha";
    private static final String CHANNEL_UPDATES = "gatehouse_updates";
    private static final int NOTIF_ID_UPDATE = 8801;
    public static final String ACTION_CHECK_UPDATE = "au.com.dss.gatehouse.ACTION_CHECK_UPDATE";

    // Primary GitHub master APK endpoint
    private static final String APK_DOWNLOAD_URL =
            "https://raw.githubusercontent.com/NoxToxCipher/gatehouse-android/master/build/gatehouse.apk";

    public interface UpdateCheckCallback {
        void onUpdateFound(String newSha, long bytes);
        void onNoUpdateAvailable();
        void onError(String message);
    }

    private AutoUpdateManager() {}

    public static void init(Context context) {
        initChannel(context);
        scheduleHourlyAlarm(context);
        // Check on app launch if it has been > 1 hour since last check
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastCheck = prefs.getLong(KEY_LAST_CHECK, 0);
        long now = System.currentTimeMillis();
        if (now - lastCheck >= 60 * 60 * 1000L) {
            checkForUpdateAsync(context, false, null);
        }
    }

    public static void initChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel chan = new NotificationChannel(
                        CHANNEL_UPDATES,
                        "GateHouse App Updates",
                        NotificationManager.IMPORTANCE_HIGH);
                chan.setDescription("Hourly automatic OTA application update notifications");
                chan.enableVibration(true);
                chan.enableLights(true);
                chan.setLightColor(0xFFF59E0B);
                nm.createNotificationChannel(chan);
            }
        }
    }

    public static void scheduleHourlyAlarm(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AutoUpdateReceiver.class);
            intent.setAction(ACTION_CHECK_UPDATE);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context, 1088, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

            if (am != null) {
                long intervalMs = 60 * 60 * 1000L; // 1 hour
                long triggerAt = SystemClock.elapsedRealtime() + intervalMs;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
                } else {
                    am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, intervalMs, pi);
                }
            }
        } catch (Exception e) {}
    }

    public static void checkForUpdateAsync(final Context context, final boolean isManual, final UpdateCheckCallback callback) {
        final Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String currentAppSha = computeFileSha256(new File(context.getPackageCodePath()));
                    File tempApk = new File(context.getCacheDir(), "gatehouse-update.apk");

                    URL url = new URL(APK_DOWNLOAD_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.setUseCaches(false);
                    conn.setRequestProperty("User-Agent", "Gatehouse-OTA/" + getAppVersion(context));
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        final String errMsg = "HTTP error " + responseCode + " fetching update";
                        mainHandler.post(new Runnable() {
                            public void run() {
                                if (callback != null) callback.onError(errMsg);
                                if (isManual) Toast.makeText(context, "Update server unavailable", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    long totalLen = conn.getContentLengthLong();
                    InputStream is = new BufferedInputStream(conn.getInputStream(), 8192);
                    FileOutputStream fos = new FileOutputStream(tempApk);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                    conn.disconnect();

                    if (!tempApk.exists() || tempApk.length() < 100000) {
                        final String errMsg = "Downloaded file too small or invalid";
                        mainHandler.post(new Runnable() {
                            public void run() {
                                if (callback != null) callback.onError(errMsg);
                            }
                        });
                        return;
                    }

                    String downloadedSha = computeFileSha256(tempApk);

                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();

                    if (downloadedSha != null && !downloadedSha.equalsIgnoreCase(currentAppSha)) {
                        // New build available!
                        final String newSha = downloadedSha;
                        final long bytes = tempApk.length();

                        mainHandler.post(new Runnable() {
                            public void run() {
                                if (callback != null) callback.onUpdateFound(newSha, bytes);
                                launchApkInstaller(context, tempApk);
                                showUpdateNotification(context, tempApk, newSha);
                            }
                        });
                    } else {
                        mainHandler.post(new Runnable() {
                            public void run() {
                                if (callback != null) callback.onNoUpdateAvailable();
                                if (isManual) {
                                    Toast.makeText(context, "✓ GateHouse is up to date", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } catch (final Exception e) {
                    mainHandler.post(new Runnable() {
                        public void run() {
                            if (callback != null) callback.onError(e.getMessage());
                            if (isManual) Toast.makeText(context, "Update check failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public static void launchApkInstaller(Context context, File apkFile) {
        try {
            Uri apkUri = GatehouseFileProvider.getUriForFile(apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                // Fallback direct intent
                Uri apkUri = Uri.fromFile(apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {}
        }
    }

    private static void showUpdateNotification(Context context, File apkFile, String newSha) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            Uri apkUri = GatehouseFileProvider.getUriForFile(apkFile);
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pi = PendingIntent.getActivity(
                    context, 1089, installIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

            android.app.Notification.Builder nb;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nb = new android.app.Notification.Builder(context, CHANNEL_UPDATES);
            } else {
                nb = new android.app.Notification.Builder(context);
            }

            int iconShield = context.getResources().getIdentifier("ic_stat_gatehouse", "drawable", context.getPackageName());
            if (iconShield == 0) iconShield = context.getResources().getIdentifier("ic_shield_gold", "drawable", context.getPackageName());
            if (iconShield == 0) iconShield = context.getApplicationInfo().icon;

            String shaShort = newSha.length() > 8 ? newSha.substring(0, 8) : newSha;
            nb.setContentTitle("⚡ GateHouse OTA Update Ready")
              .setContentText("Tap to install new build (SHA " + shaShort + ") · All shift data preserved")
              .setSmallIcon(iconShield)
              .setContentIntent(pi)
              .setAutoCancel(true);

            try {
                nb.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), context.getApplicationInfo().icon));
            } catch (Throwable ignored) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                nb.setColor(0xFFF59E0B);
            }

            nm.notify(NOTIF_ID_UPDATE, nb.build());
        } catch (Exception e) {}
    }

    public static String computeFileSha256(File file) {
        if (file == null || !file.exists()) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[8192];
            int r;
            while ((r = fis.read(buf)) != -1) {
                md.update(buf, 0, r);
            }
            fis.close();
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format(Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getAppVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0";
        } catch (Exception e) {
            return "1.0";
        }
    }

    public static class AutoUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            AutoUpdateManager.scheduleHourlyAlarm(context);
            AutoUpdateManager.checkForUpdateAsync(context, false, null);
        }
    }
}