package au.com.dss.gatehouse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

/**
 * GatehouseIslandManager — Unified Dynamic Island & Live Capsule Feeder.
 * Feeds real-time punch-hole pills, lock screen Live Activities, and ambient HUDs
 * for Xiaomi HyperOS Smart Island, OxygenOS Fluid Cloud, Realme Mini Capsule,
 * and universal Android ambient displays.
 */
public class GatehouseIslandManager {
    private static final String TAG = "GatehouseIslandManager";
    public static final String CHANNEL_ISLAND = "gatehouse_dynamic_island_v2";
    public static final int NOTIF_ISLAND_ID = 8801;

    public static final String ACTION_DISMISS_ISLAND = "au.com.dss.gatehouse.DISMISS_ISLAND";

    private static GatehouseIslandManager instance;
    private final Context context;
    private MediaSession mediaSession;

    public static synchronized GatehouseIslandManager getInstance(Context ctx) {
        if (instance == null) {
            instance = new GatehouseIslandManager(ctx.getApplicationContext());
        }
        return instance;
    }

    private GatehouseIslandManager(Context ctx) {
        this.context = ctx;
        initChannel();
        initMediaSession();
    }

    private void initChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ISLAND,
                    "Dynamic Island Live Capsule",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chan.setDescription("Live status capsules for Xiaomi HyperOS Smart Island, OxygenOS Fluid Cloud, and ambient locks");
            chan.enableLights(true);
            chan.setLightColor(0xFFF59E0B);
            chan.enableVibration(true);
            chan.setShowBadge(true);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(chan);
        }
    }

    private void initMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (mediaSession != null) {
                    try { mediaSession.release(); } catch (Exception ignored) {}
                }
                mediaSession = new MediaSession(context, "GatehouseIslandSession");
                mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
                mediaSession.setCallback(new MediaSession.Callback() {
                    @Override
                    public void onPlay() {
                        Log.d(TAG, "MediaSession onPlay");
                    }
                    @Override
                    public void onPause() {
                        dismissCapsule();
                    }
                    @Override
                    public void onStop() {
                        dismissCapsule();
                    }
                });
                mediaSession.setActive(true);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing MediaSession for Dynamic Island: " + e.getMessage());
            }
        }
    }

    /**
     * Update the active punch-hole pill and lock screen capsule.
     */
    public void showFuelIsland(double oomPrice, double savingCents, int minsRemaining) {
        String priceStr = String.format(Locale.US, "%.1f¢", oomPrice);
        String title = "⛽ OOM " + priceStr + " (0.8km)";
        String subtitle = "Save " + String.format(Locale.US, "%.1f¢/L", savingCents) + " · Shift ends in " + minsRemaining + "m";
        String bigDetails = "🟢 OOM Kingston: " + priceStr + " (0.8 km) · 🧭 142° SE\n" +
                "⚪ 7-Eleven: 174.9¢ · ⚪ Ampol: 176.9¢\n" +
                "💰 Save $3.60 on a 60L fill vs 7-Eleven";

        Bitmap badge = createPillBadgeBitmap("⛽", "OOM " + priceStr, 0xFFF59E0B);
        publishCapsule(title, subtitle, bigDetails, "Fuel Radar", badge, "https://maps.google.com/?q=" + Uri.encode("OOM Energy Kingston, 122 Kingston Rd, Kingston QLD"), "🗺️ Drive to OOM");
    }

    public void showPatrolIsland(int remainingMins, String guardName) {
        String title = "🛡️ Gatehouse Patrol Active";
        String subtitle = "Next check in " + remainingMins + "m · " + (guardName.contains("Lochran") ? "L. Doherty #41207" : guardName);
        String bigDetails = "✓ Perimeter secure · Booster pressure: 620 kPa\n" +
                "⏱️ Handover transfer prepared for day crew";

        Bitmap badge = createPillBadgeBitmap("🛡️", remainingMins + "m", 0xFF10B981);
        publishCapsule(title, subtitle, bigDetails, "Patrol Monitor", badge, null, null);
    }

    public void showIncidentIsland(String incidentTitle, String summary) {
        String title = "🚨 ALARM: " + incidentTitle;
        String subtitle = summary;
        String bigDetails = "Location: Kingston Facility Perimeter\n" +
                "Attestation: DSS Spark Incident Protocol Engaged";

        Bitmap badge = createPillBadgeBitmap("🚨", "ALERT", 0xFFEF4444);
        publishCapsule(title, subtitle, bigDetails, "Security Alert", badge, null, null);
    }

    public void showDeputyShiftIsland(String shiftTime, String siteName) {
        String title = "📅 Scheduled Shift: " + shiftTime;
        String subtitle = "Site: " + siteName + " · Uniform: Full High-Vis";
        String bigDetails = "Briefing: 12h Pre-shift weather & uniform telemetry ready";

        Bitmap badge = createPillBadgeBitmap("📅", "ROSTER", 0xFF38BDF8);
        publishCapsule(title, subtitle, bigDetails, "Deputy Roster", badge, null, null);
    }

    private void publishCapsule(String title, String subtitle, String bigDetails, String subText, Bitmap iconBitmap, String navUri, String navLabel) {
        try {
            initMediaSession();

            // 1. Update MediaSession Metadata (Primary feeder for Xiaomi Smart Island / ColorOS Fluid Cloud)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
                MediaMetadata.Builder mb = new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, subtitle)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, subText);

                if (iconBitmap != null) {
                    mb.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, iconBitmap);
                    mb.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, iconBitmap);
                    mb.putBitmap(MediaMetadata.METADATA_KEY_ART, iconBitmap);
                }
                mediaSession.setMetadata(mb.build());

                PlaybackState.Builder pb = new PlaybackState.Builder()
                        .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                        .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT);
                mediaSession.setPlaybackState(pb.build());
                mediaSession.setActive(true);
            }

            // 2. Build Ongoing Ambient Notification with MediaStyle
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            Intent appIntent = new Intent(context, MainActivity.class);
            appIntent.setAction(FuelPriceManager.ACTION_OPEN_FUEL);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent appPi = PendingIntent.getActivity(
                    context,
                    8801,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );

            Notification.Builder nb;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nb = new Notification.Builder(context, CHANNEL_ISLAND);
            } else {
                nb = new Notification.Builder(context);
            }

            int smallIcon = context.getResources().getIdentifier("ic_stat_gatehouse", "drawable", context.getPackageName());
            if (smallIcon == 0) smallIcon = context.getApplicationInfo().icon;

            nb.setSmallIcon(smallIcon)
                    .setContentTitle(title)
                    .setContentText(subtitle)
                    .setSubText(subText)
                    .setContentIntent(appPi)
                    .setColor(0xFFF59E0B)
                    .setOngoing(true)
                    .setCategory(Notification.CATEGORY_TRANSPORT)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                nb.setPriority(Notification.PRIORITY_MAX);
            }

            if (iconBitmap != null) {
                nb.setLargeIcon(iconBitmap);
            }

            int actionCount = 0;
            if (navUri != null && navLabel != null) {
                Intent navIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(navUri));
                PendingIntent navPi = PendingIntent.getActivity(
                        context,
                        8802,
                        navIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
                );
                nb.addAction(0, navLabel, navPi);
                actionCount++;
            }

            Intent dismissIntent = new Intent(context, MainActivity.class);
            dismissIntent.setAction(ACTION_DISMISS_ISLAND);
            PendingIntent dismissPi = PendingIntent.getActivity(
                    context,
                    8803,
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            nb.addAction(0, "✕ Dismiss", dismissPi);
            actionCount++;

            // Apply MediaStyle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
                Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
                mediaStyle.setMediaSession(mediaSession.getSessionToken());
                if (actionCount > 1) {
                    mediaStyle.setShowActionsInCompactView(0, 1);
                } else if (actionCount > 0) {
                    mediaStyle.setShowActionsInCompactView(0);
                }
                nb.setStyle(mediaStyle);
            } else {
                nb.setStyle(new Notification.BigTextStyle().bigText(bigDetails));
            }

            nm.notify(NOTIF_ISLAND_ID, nb.build());
        } catch (Exception e) {
            Log.e(TAG, "Error publishing capsule: " + e.getMessage(), e);
        }
    }

    public void dismissCapsule() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaSession != null) {
                PlaybackState.Builder pb = new PlaybackState.Builder()
                        .setState(PlaybackState.STATE_STOPPED, 0, 1.0f);
                mediaSession.setPlaybackState(pb.build());
                mediaSession.setActive(false);
            }
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIF_ISLAND_ID);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing capsule: " + e.getMessage(), e);
        }
    }

    private Bitmap createPillBadgeBitmap(String emoji, String text, int accentColor) {
        int width = 140;
        int height = 140;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFF0F172A);
        canvas.drawRoundRect(new RectF(0, 0, width, height), 28, 28, bgPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6);
        borderPaint.setColor(accentColor);
        canvas.drawRoundRect(new RectF(3, 3, width - 3, height - 3), 28, 28, borderPaint);

        Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emojiPaint.setTextSize(48);
        emojiPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(emoji, width / 2f, 60, emojiPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(accentColor);
        textPaint.setTextSize(22);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, width / 2f, 110, textPaint);

        return bitmap;
    }
}
