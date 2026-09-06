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
    private static final String KEY_LAST_NOTIFIED_SHA = "last_notified_sha";
    private static final String CHANNEL_UPDATES = "gatehouse_updates_v3"; // v3: Gatehouse notice tone (tier three)
    private static final int NOTIF_ID_UPDATE = 8801;
    public static final String ACTION_CHECK_UPDATE = "au.com.dss.gatehouse.ACTION_CHECK_UPDATE";

    // Primary GitHub master APK endpoint
    private static final String APK_DOWNLOAD_URL =
            "https://raw.githubusercontent.com/NoxToxCipher/gatehouse-android/master/build/gatehouse.apk";
    // Private-repo fetch: the GitHub contents API streams the raw file when the
    // repo is private, given a read token. Used only when a token is on the phone.
    private static final String APK_API_URL =
            "https://api.github.com/repos/NoxToxCipher/gatehouse-android/contents/build/gatehouse.apk?ref=master";

    /** Opens the APK either from the public raw URL or, when the site book holds a
     *  read token, from the authenticated contents API (so a private repo still updates). */
    private static HttpURLConnection openApkConnection(Context ctx) throws java.io.IOException {
        String token = "";
        try { token = SiteBook.otaToken(ctx); } catch (Throwable ignored) {}
        boolean priv = token != null && !token.isEmpty();
        URL url = new URL(priv ? APK_API_URL : APK_DOWNLOAD_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "Gatehouse-OTA/" + getAppVersion(ctx));
        if (priv) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/vnd.github.raw");
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        }
        return conn;
    }

    public interface UpdateCheckCallback {
        void onUpdateFound(String newSha, long bytes);
        void onNoUpdateAvailable();
        void onError(String message);
    }

    private AutoUpdateManager() {}

    public static void init(Context context) {
        initChannel(context);
        scheduleHourlyAlarm(context);

        // Cancel any pending update notification since app is currently running
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIF_ID_UPDATE);
            }
        } catch (Exception ignored) {}

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        try {
            String currentAppSha = computeFileSha256(new File(context.getPackageCodePath()));
            prefs.edit().putString(KEY_LAST_SHA, currentAppSha)
                        .putString(KEY_LAST_NOTIFIED_SHA, currentAppSha)
                        .apply();
        } catch (Exception ignored) {}

        // Check on app launch if it has been > 1 hour since last check
        long lastCheck = prefs.getLong(KEY_LAST_CHECK, 0);
        long now = System.currentTimeMillis();
        if (now - lastCheck >= 60 * 60 * 1000L) {
            checkForUpdateAsync(context, false, null);
        }
    }

    public static void initChannel(Context context) {
        GatehouseNotify.createChannel(context, CHANNEL_UPDATES,
                "Updates", "New builds ready to install",
                GatehouseNotify.Tier.NOTICE, GatehouseNotify.GROUP_APP);
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

                    HttpURLConnection conn = openApkConnection(context);
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

                    // Never wind a phone back. A different SHA is not "newer": compare the
                    // version code build.sh stamps from the commit count, and only offer
                    // a build that is strictly ahead of what is installed.
                    long remoteCode = archiveVersionCode(context, tempApk);
                    long localCode = installedVersionCode(context);
                    boolean strictlyNewer = remoteCode > 0 && remoteCode > localCode;
                    if (strictlyNewer && downloadedSha != null && !downloadedSha.equalsIgnoreCase(currentAppSha)) {
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
                                    Toast.makeText(context, "Gatehouse is up to date", Toast.LENGTH_SHORT).show();
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
            if (newSha == null || newSha.trim().isEmpty()) return;

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String lastNotifiedSha = prefs.getString(KEY_LAST_NOTIFIED_SHA, "");
            String currentAppSha = prefs.getString(KEY_LAST_SHA, "");

            // If already notified for this exact SHA, or if this SHA is already currently installed, suppress duplicate notification
            if (newSha.equalsIgnoreCase(lastNotifiedSha) || newSha.equalsIgnoreCase(currentAppSha)) {
                return;
            }

            prefs.edit().putString(KEY_LAST_NOTIFIED_SHA, newSha).apply();

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            Uri apkUri = GatehouseFileProvider.getUriForFile(apkFile);
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pi = PendingIntent.getActivity(
                    context, 1089, installIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

            String shaShort = newSha.length() > 8 ? newSha.substring(0, 8) : newSha;
            String versionName = archiveVersionName(context, apkFile);
            String title = versionName != null ? "Update ready · " + versionName : "Update ready";

            android.app.Notification.Builder nb = GatehouseNotify.builder(context, CHANNEL_UPDATES, GatehouseNotify.Tier.NOTICE)
                    .setContentTitle(title)
                    .setContentText("Tap to install. Shift records are kept.")
                    .setStyle(new android.app.Notification.BigTextStyle().bigText(
                            "Build " + shaShort + ". Installing takes under a minute and keeps every record on this phone."))
                    .setContentIntent(pi)
                    .addAction(GatehouseNotify.action(context, "Install", pi));

            nm.notify(NOTIF_ID_UPDATE, nb.build());
        } catch (Exception e) {}
    }

    private static long installedVersionCode(Context context) {
        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String archiveVersionName(Context context, File apk) {
        try {
            android.content.pm.PackageInfo info = context.getPackageManager()
                    .getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            return info != null ? info.versionName : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static long archiveVersionCode(Context context, File apk) {
        try {
            PackageInfo p = context.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
            if (p == null) return 0;
            return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode;
        } catch (Exception e) {
            return 0;
        }
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