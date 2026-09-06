package au.com.dss.gatehouse;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Who is signed in on this hut phone. A session lasts a long shift and a little
 * more, or until the guard signs out, so a phone opened at the start of the next
 * shift asks again. Wrong PINs are counted here too, and five in a row make the
 * phone wait.
 */
final class GuardSession {

    static final String PREFS = "gatehouse_session";
    private static final String KEY_GUARD = "guard_key";
    private static final String KEY_NAME = "guard_name";
    private static final String KEY_SINCE = "signed_in_at";
    private static final String KEY_FAILS = "pin_failures";
    private static final String KEY_LOCK = "pin_locked_until";
    private static final String KEY_SKIPPED = "skipped_at";

    /** A double shift and an hour of handover. */
    static final long MAX_MS = 14L * 60L * 60L * 1000L;
    /** A skipped sign-in is not asked again for this long. */
    static final long SKIP_MS = 60L * 60L * 1000L;
    static final int FAILS_BEFORE_WAIT = 5;
    static final long WAIT_MS = 30_000L;

    private GuardSession() {}

    private static SharedPreferences p(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The signed-in guard's book key, or null when nobody is, or the session has lapsed. */
    static String key(Context ctx) {
        SharedPreferences p = p(ctx);
        String k = p.getString(KEY_GUARD, null);
        if (k == null || k.isEmpty()) return null;
        long since = p.getLong(KEY_SINCE, 0L);
        if (System.currentTimeMillis() - since > MAX_MS) return null;
        return k;
    }

    static String name(Context ctx) {
        return key(ctx) != null ? p(ctx).getString(KEY_NAME, "") : "";
    }

    static long since(Context ctx) {
        return key(ctx) != null ? p(ctx).getLong(KEY_SINCE, 0L) : 0L;
    }

    static boolean active(Context ctx) {
        return key(ctx) != null;
    }

    static void signIn(Context ctx, SiteBook.Guard g) {
        p(ctx).edit()
                .putString(KEY_GUARD, g.key)
                .putString(KEY_NAME, g.name)
                .putLong(KEY_SINCE, System.currentTimeMillis())
                .remove(KEY_SKIPPED)
                .putInt(KEY_FAILS, 0)
                .remove(KEY_LOCK)
                .apply();
    }

    static void signOut(Context ctx) {
        p(ctx).edit().remove(KEY_GUARD).remove(KEY_NAME).remove(KEY_SINCE).remove(KEY_SKIPPED).apply();
    }

    /** The guard chose to carry on without signing in; the phone does not nag for an hour. */
    static void noteSkipped(Context ctx) {
        p(ctx).edit().putLong(KEY_SKIPPED, System.currentTimeMillis()).apply();
    }

    static void clearSkipped(Context ctx) {
        p(ctx).edit().remove(KEY_SKIPPED).apply();
    }

    static boolean recentlySkipped(Context ctx) {
        return System.currentTimeMillis() - p(ctx).getLong(KEY_SKIPPED, 0L) < SKIP_MS;
    }

    /** Whether the app should put the sign-in sheet up now. */
    static boolean shouldAsk(Context ctx) {
        if (!SiteBook.present(ctx)) return false;
        if (active(ctx)) return false;
        return !recentlySkipped(ctx);
    }

    // ------------------------------------------------------------------ themes

    static int themeFor(Context ctx, String guardKey) {
        return p(ctx).getInt("theme_" + guardKey, -1);
    }

    static void setThemeFor(Context ctx, String guardKey, int themeId) {
        p(ctx).edit().putInt("theme_" + guardKey, themeId).putBoolean("theme_picked_" + guardKey, true).apply();
    }

    static void forgetThemes(Context ctx) {
        SharedPreferences.Editor e = p(ctx).edit();
        for (String k : p(ctx).getAll().keySet()) if (k.startsWith("theme_")) e.remove(k);
        e.apply();
    }

    static boolean themePicked(Context ctx, String guardKey) {
        return p(ctx).getBoolean("theme_picked_" + guardKey, false);
    }

    // ------------------------------------------------------------------ wrong PINs

    /** Milliseconds the pad must still wait, or 0. */
    static long waitMs(Context ctx) {
        long until = p(ctx).getLong(KEY_LOCK, 0L);
        return Math.max(0L, until - System.currentTimeMillis());
    }

    static int failures(Context ctx) {
        return p(ctx).getInt(KEY_FAILS, 0);
    }

    /** Counts a wrong PIN; returns the tries left before the wait, 0 when the wait has begun. */
    static int noteFailure(Context ctx) {
        int fails = failures(ctx) + 1;
        SharedPreferences.Editor e = p(ctx).edit().putInt(KEY_FAILS, fails);
        if (fails >= FAILS_BEFORE_WAIT) {
            e.putLong(KEY_LOCK, System.currentTimeMillis() + WAIT_MS).putInt(KEY_FAILS, 0);
            e.apply();
            return 0;
        }
        e.apply();
        return FAILS_BEFORE_WAIT - fails;
    }
}
