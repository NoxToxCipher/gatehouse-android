package au.com.dss.gatehouse;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

import java.util.Arrays;
import java.util.Locale;

/**
 * One voice for every notification Gatehouse posts.
 *
 * Each phone brand draws its own notification chrome, so what survives
 * across all of them is the standard template plus a few constants held
 * steady here: the gatehouse mark in the status bar, one accent, sentence
 * case copy that states the fact first, and a weight that matches the tier.
 * Custom layouts, emoji and per-notification colours are deliberately absent;
 * they are what makes the same alert look like four different apps on four
 * phones.
 *
 * Tiers match {@link GatehouseSounds}:
 *   NOTICE  worth knowing, never a banner (satellite passes, fuel, sky, updates)
 *   CHIME   worth a look (roster changes, shift reminders, licence)
 *   ALERT   act now (fire, lightning, hail): crimson, heads-up, readable on the lock screen
 */
final class GatehouseNotify {
    private GatehouseNotify() {}

    /** Accent for the notice and chime tiers. The default instrument brass. */
    static final int BRASS = 0xFFE5A93C;
    /** Accent for the alert tier only. */
    static final int CRIMSON = 0xFFE5484D;

    enum Tier { NOTICE, CHIME, ALERT }

    /** Channel groups: how the notification settings page of every brand organises Gatehouse. */
    static final String GROUP_SAFETY = "gatehouse.safety";
    static final String GROUP_ROSTER = "gatehouse.roster";
    static final String GROUP_SKY = "gatehouse.sky";
    static final String GROUP_APP = "gatehouse.app";

    /** Channel ids from earlier builds. Deleted so the settings page shows only the live set. */
    private static final String[] RETIRED_CHANNELS = {
            "satellite_sky_passes", "satellite_sky_passes_v2",
            "fuel_shift_alerts", "fuel_shift_alerts_v2",
            "gatehouse_skywatch_alerts", "gatehouse_skywatch_alerts_v2",
            "airspace_flight_alerts", "airspace_flight_alerts_v2",
            "gatehouse_updates", "gatehouse_updates_v2",
            "deputy_roster_updates", "deputy_shift_weather", "security_licence_alerts",
            "fire_hazard_alerts", "lightning_proximity_alerts", "hail_severe_alerts",
            "dss_deputy_roster_channel",
    };

    private static boolean prepared = false;

    // ------------------------------------------------------------------ channels

    /** Creates the channel groups once per process and retires channel ids from earlier builds. */
    static synchronized void prepare(Context ctx) {
        if (prepared) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        try {
            nm.createNotificationChannelGroups(Arrays.asList(
                    new NotificationChannelGroup(GROUP_SAFETY, "Safety"),
                    new NotificationChannelGroup(GROUP_ROSTER, "Roster"),
                    new NotificationChannelGroup(GROUP_SKY, "Sky and fuel"),
                    new NotificationChannelGroup(GROUP_APP, "App")));
            for (String id : RETIRED_CHANNELS) {
                try { nm.deleteNotificationChannel(id); } catch (Throwable ignored) {}
            }
            prepared = true;
        } catch (Throwable ignored) {}
    }

    /**
     * Creates or refreshes a channel in the house style. Names and descriptions
     * update in place; importance only ever moves down, which Android permits
     * without a new id as long as the user has not touched the channel.
     */
    static void createChannel(Context ctx, String id, String name, String description, Tier tier, String group) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        prepare(ctx);
        int importance = tier == Tier.ALERT
                ? NotificationManager.IMPORTANCE_HIGH
                : NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel c = new NotificationChannel(id, name, importance);
        c.setDescription(description);
        try { c.setGroup(group); } catch (Throwable ignored) {}
        c.enableLights(true);
        c.setLightColor(tier == Tier.ALERT ? CRIMSON : BRASS);
        c.enableVibration(true);
        c.setVibrationPattern(vibration(tier));
        c.setShowBadge(tier != Tier.NOTICE);
        switch (tier) {
            case ALERT: GatehouseSounds.applyAlert(c, ctx); break;
            case CHIME: GatehouseSounds.applyChime(c, ctx); break;
            default:    GatehouseSounds.applyNotice(c, ctx); break;
        }
        nm.createNotificationChannel(c);
    }

    private static long[] vibration(Tier tier) {
        switch (tier) {
            case ALERT: return new long[]{0, 240, 120, 240, 120, 420};
            case CHIME: return new long[]{0, 110, 90, 110};
            default:    return new long[]{0, 60};
        }
    }

    // ------------------------------------------------------------------ builders

    /** A builder carrying everything the house style holds constant. Add copy, intent and style. */
    static Notification.Builder builder(Context ctx, String channelId, Tier tier) {
        Notification.Builder b = new Notification.Builder(ctx, channelId)
                .setSmallIcon(smallIcon(ctx))
                .setColor(tier == Tier.ALERT ? CRIMSON : BRASS)
                .setShowWhen(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);
        switch (tier) {
            case ALERT:
                // Alarm category: heads-up everywhere, and it sounds through Do Not
                // Disturb wherever the guard's own DND rules let alarms through.
                b.setCategory(Notification.CATEGORY_ALARM)
                 .setVisibility(Notification.VISIBILITY_PUBLIC);
                break;
            case CHIME:
                b.setCategory(Notification.CATEGORY_EVENT);
                break;
            default:
                b.setCategory(Notification.CATEGORY_STATUS);
                break;
        }
        return b;
    }

    /** The flat gatehouse mark. Every brand tints it with the accent; none of them draw it differently. */
    static int smallIcon(Context ctx) {
        int id = ctx.getResources().getIdentifier("ic_stat_gatehouse", "drawable", ctx.getPackageName());
        return id != 0 ? id : ctx.getApplicationInfo().icon;
    }

    static PendingIntent open(Context ctx, int requestCode, Intent intent) {
        return PendingIntent.getActivity(ctx, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** An action with a plain verb for a label. Watches show the icon; phones show the words. */
    static Notification.Action action(Context ctx, String label, PendingIntent pi) {
        return new Notification.Action.Builder(Icon.createWithResource(ctx, smallIcon(ctx)), label, pi).build();
    }

    /** Joins body lines for a big-text style, skipping blanks. */
    static String lines(String... ls) {
        StringBuilder sb = new StringBuilder();
        for (String l : ls) {
            if (l == null || l.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(l.trim());
        }
        return sb.toString();
    }

    /** "CATASTROPHIC" to "Catastrophic": labels that are caps in the instrument read as words here. */
    static String sentence(String label) {
        if (label == null || label.isEmpty()) return "";
        String s = label.trim().toLowerCase(Locale.US);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static final long MINUTE = 60_000L;
    static final long HOUR = 60 * MINUTE;

    // ------------------------------------------------------------------ samples

    /**
     * Posts one sample of a tier on the real channel, so the way this phone
     * draws it can be checked from Settings without waiting for a storm.
     * Samples clear themselves after three minutes.
     */
    static void postSample(Context ctx, Tier tier) {
        try {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            Intent intent = new Intent(ctx, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = open(ctx, 0x5A00 + tier.ordinal(), intent);
            Notification.Builder b;
            switch (tier) {
                case ALERT: {
                    FireRadarManager.initChannels(ctx);
                    b = builder(ctx, FireRadarManager.CHANNEL_LIGHTNING_ALERTS, tier)
                            .setContentTitle("Sample · Lightning 2.4 km SW")
                            .setContentText("Stand down under hard cover until it clears.")
                            .setStyle(new Notification.BigTextStyle().bigText(lines(
                                    "Closest strike 2.4 km SW · 5 strikes within 10 km",
                                    "Trigger: closer than 5 km, or 3 or more strikes.",
                                    "Cease yard rounds. Stay in the guard hut until it clears.")));
                    break;
                }
                case CHIME: {
                    DeputyNotifier.initChannels(ctx);
                    b = builder(ctx, DeputyNotifier.CHANNEL_ROSTER_CHANGES, tier)
                            .setContentTitle("Sample · Roster changed · 2 shifts")
                            .setContentText("Added · Sat 20:00 – 04:00 · B. Rush")
                            .setStyle(new Notification.InboxStyle()
                                    .addLine("Added · Sat 20:00 – 04:00 · B. Rush")
                                    .addLine("Changed · Sun 18:00 – 06:00 · P. Doherty"))
                            .addAction(action(ctx, "Open roster", pi));
                    break;
                }
                default: {
                    SatelliteTrackerManager.initChannels(ctx);
                    b = builder(ctx, SatelliteTrackerManager.CHANNEL_SATELLITE_ALERTS, tier)
                            .setContentTitle("Sample · ISS in 2 min")
                            .setContentText("Rises SW, peaks 68° at 19:42 · 6 min")
                            .setStyle(new Notification.BigTextStyle().bigText(lines(
                                    "Rises 19:38 in the SW (225°)",
                                    "Peaks 19:42 at 68°, N",
                                    "Sets in the NE (40°) · 6 min 12 s · magnitude -2.1",
                                    "Look to the SW horizon. It crosses steadily without blinking.")))
                            .addAction(action(ctx, "Track pass", pi));
                    break;
                }
            }
            b.setContentIntent(pi).setTimeoutAfter(3 * MINUTE);
            nm.notify(0x5A00 + tier.ordinal(), b.build());
        } catch (Throwable ignored) {}
    }
}
