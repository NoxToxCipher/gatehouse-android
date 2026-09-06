package au.com.dss.gatehouse;

import android.content.Context;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The fall detector's front door: whether this phone should run it, how to
 * start and stop it, and the small amount of state the service, the check-in
 * screen and Settings share.
 *
 * It runs only on the hut phones, the site-issued handsets every guard
 * carries, because it holds the accelerometer open all shift and that costs
 * battery a personal phone should not pay.
 *
 * Staffing rule: with one guard on site, Hut Phone #1 carries fall detection
 * and Hut Phone #2 rests in the hut as the beacon. With two guards rostered
 * on site, both phones watch. Hut Phone #2 can be told to keep watching
 * regardless (mode "on"), and either phone can be switched off.
 */
final class FallDetection {
    private FallDetection() {}

    static final String PREFS = "gatehouse_fall";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_MODE = "mode";
    static final String MODE_AUTO = "auto", MODE_ON = "on", MODE_OFF = "off";
    /** A roster older than this says nothing about who is on site now. */
    private static final long ROSTER_FRESH_MS = 36L * 60 * 60 * 1000;
    private static final String KEY_OFFICER = "officer_name";
    private static final String KEY_LAST_EVENT = "last_event";
    private static final String KEY_PENDING = "pending_record_lines";

    /** The freshest fix the service found when a fall was detected. */
    static volatile Location lastFix;

    static boolean isHutPhone() {
        try {
            return MainActivity.getHutPhoneHardwareTag().startsWith("Hut Phone");
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean isHutPhoneOne() {
        try {
            return MainActivity.getHutPhoneHardwareTag().endsWith("#1");
        } catch (Throwable t) {
            return false;
        }
    }

    /** auto (the roster decides, Hut Phone #2 only), on, or off. Hut Phone #1 treats auto as on. */
    static String mode(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String m = p.getString(KEY_MODE, null);
        if (m == null) m = p.getBoolean(KEY_ENABLED, true) ? MODE_AUTO : MODE_OFF;
        return m;
    }

    static void setMode(Context ctx, String mode) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode).apply();
        ensureRunning(ctx);
    }

    static boolean isEnabled(Context ctx) {
        return !MODE_OFF.equals(mode(ctx));
    }

    /** True when the detector should be running on this phone right now. */
    static boolean shouldRun(Context ctx) {
        if (!isHutPhone()) return false;
        String m = mode(ctx);
        if (MODE_OFF.equals(m)) return false;
        if (isHutPhoneOne() || MODE_ON.equals(m)) return true;
        return guardsOnSiteNow(ctx) >= 2;
    }

    /** One line for Settings: what this phone is doing and why. */
    static String statusLine(Context ctx) {
        if (!isHutPhone()) return "Not a hut phone. Fall detection runs only on Hut Phone #1 and #2, which every guard carries.";
        String m = mode(ctx);
        String tag = MainActivity.getHutPhoneHardwareTag();
        if (MODE_OFF.equals(m)) return "Off on this phone.";
        if (isHutPhoneOne()) return "On · " + tag + " · a hard impact followed by stillness opens a check-in at full volume; thirty seconds unanswered texts control.";
        if (MODE_ON.equals(m)) return "On · " + tag + " · kept watching even when one guard is on site.";
        int n = guardsOnSiteNow(ctx);
        if (n < 0) return "Auto · no recent roster, so treated as one guard on site: Hut Phone #1 carries fall detection and this phone rests as the hut beacon.";
        if (n >= 2) return "Auto · " + n + " guards rostered on site now (" + joinNames(liveGuardNames(ctx)) + ") · watching.";
        return "Auto · one guard on site, so Hut Phone #1 carries fall detection and this phone rests as the hut beacon. Choose On to watch here anyway.";
    }

    // ------------------------------------------------------------- roster

    /** Distinct guards whose rostered shift covers this moment, from the cached roster. Empty when none or unknown. */
    static List<String> liveGuardNames(Context ctx) {
        List<String> out = new ArrayList<>();
        try {
            RosterProvider.Result r = Rostering.create(ctx).loadCachedResult();
            if (r == null) return out;
            long now = System.currentTimeMillis();
            if (r.syncTimestamp > 0 && now - r.syncTimestamp > ROSTER_FRESH_MS) return out;
            Set<String> names = new LinkedHashSet<>();
            long nowSec = now / 1000L;
            for (RosterProvider.Shift sh : r.weekShifts) {
                if (sh == null || sh.startTs <= 0 || sh.endTs <= 0) continue;
                if (sh.startTs <= nowSec && nowSec < sh.endTs && sh.guardName != null && !sh.guardName.trim().isEmpty()) {
                    names.add(sh.guardName.trim());
                }
            }
            out.addAll(names);
        } catch (Throwable ignored) {}
        return out;
    }

    /** How many guards are rostered on site now; -1 when there is no usable roster. */
    static int guardsOnSiteNow(Context ctx) {
        try {
            RosterProvider.Result r = Rostering.create(ctx).loadCachedResult();
            if (r == null || r.weekShifts == null || r.weekShifts.isEmpty()) return -1;
            if (r.syncTimestamp > 0 && System.currentTimeMillis() - r.syncTimestamp > ROSTER_FRESH_MS) return -1;
        } catch (Throwable t) {
            return -1;
        }
        return liveGuardNames(ctx).size();
    }

    /**
     * The other guard rostered on site now, for the text: a name, "" when the
     * roster says the officer is alone, null when there is no usable roster.
     */
    static String otherGuardOnSite(Context ctx) {
        int n = guardsOnSiteNow(ctx);
        if (n < 0) return null;
        String me = officerName(ctx);
        for (String name : liveGuardNames(ctx)) {
            if (!name.equalsIgnoreCase(me)) return name;
        }
        return "";
    }

    private static String joinNames(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(n);
        }
        return sb.toString();
    }

    /** Re-checks the staffing rule every fifteen minutes, on hut phones only. */
    static void scheduleTicks(Context ctx) {
        if (!isHutPhone()) return;
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 0x5A12,
                    new Intent(ctx, FallDetectionService.TickReceiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            long period = 15L * 60 * 1000;
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + period, period, pi);
        } catch (Throwable ignored) {}
    }

    /** Starts the service on a hut phone that wants it, stops it anywhere else. */
    static void ensureRunning(Context ctx) {
        Intent svc = new Intent(ctx, FallDetectionService.class);
        try {
            if (shouldRun(ctx)) ctx.startForegroundService(svc);
            else ctx.stopService(svc);
        } catch (Throwable ignored) {}
    }

    /** MainActivity keeps this fresh; the check-in screen reads it with no activity around. */
    static void setOfficerName(Context ctx, String name) {
        if (name == null || name.trim().isEmpty()) return;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_OFFICER, name.trim()).apply();
    }

    /** The roster's one live guard when there is exactly one; otherwise the name the app last held. */
    static String officerName(Context ctx) {
        String signed = GuardSession.name(ctx);
        if (!signed.isEmpty()) return signed;
        List<String> live = liveGuardNames(ctx);
        if (live.size() == 1) return live.get(0);
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_OFFICER, "the guard on shift");
    }

    static String locationLink() {
        Location l = lastFix;
        if (l == null) return "";
        return String.format(Locale.US, "https://maps.google.com/?q=%.5f,%.5f", l.getLatitude(), l.getLongitude());
    }

    static String locationNote() {
        Location l = lastFix;
        if (l == null) return "";
        long ageMin = (SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos()) / 60_000_000_000L;
        String when = ageMin < 1 ? "just now" : ageMin + " min ago";
        return String.format(Locale.US, "within %.0f m, %s", l.getAccuracy(), when);
    }

    static String clock(long wallMs) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(wallMs));
    }

    // ------------------------------------------------------------- outcomes

    /** One line for the Settings row, so tuning can be done from evidence. */
    static void setLastEvent(Context ctx, String line) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST_EVENT, line).apply();
    }

    static String lastEvent(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_EVENT, "");
    }

    /**
     * Queues a line for the sealed record. The record lives in MainActivity,
     * so the service and the check-in screen leave lines here and the next
     * resume writes them in.
     */
    static synchronized void queueForRecord(Context ctx, String line) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String cur = p.getString(KEY_PENDING, "");
        p.edit().putString(KEY_PENDING, cur.isEmpty() ? line : cur + "" + line).apply();
        setLastEvent(ctx, line);
    }

    static synchronized List<String> drainForRecord(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String cur = p.getString(KEY_PENDING, "");
        List<String> out = new ArrayList<>();
        if (!cur.isEmpty()) {
            for (String s : cur.split("")) if (!s.isEmpty()) out.add(s);
            p.edit().remove(KEY_PENDING).apply();
        }
        return out;
    }
}
