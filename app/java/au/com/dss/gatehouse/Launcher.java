package au.com.dss.gatehouse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The home screen's front door: whether this device is offered it, the
 * small state the app shares with it, and first light for the site.
 *
 * It is offered on Hut Phone #2 by default, the phone used least, and on
 * any device where Settings turns it on for testing. Hut Phone #1 is left
 * alone for now. The HOME activity ships disabled and is enabled here, so
 * a phone that is not offered it never sees a "choose your home app" prompt.
 */
final class Launcher {
    private Launcher() {}

    static final String PREFS = "gatehouse_launcher";
    private static final String KEY_OFFER = "offer";
    private static final String KEY_WELFARE_RESET = "welfare_reset_ms";
    private static final String KEY_TEMP = "temp_c";
    /** Opens the app on a tab: 0 patrol, 1 contacts, 2 tools, 3 settings. */
    static final String EXTRA_TAB = "gatehouse.tab";

    /** Hume Doors & Timber, Kingston, for first light. */
    private static final double SITE_LAT = -27.6534, SITE_LON = 153.1165;

    static boolean offered(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean dflt = false;
        try { dflt = MainActivity.getHutPhoneHardwareTag().endsWith("#2"); } catch (Throwable ignored) {}
        return p.getBoolean(KEY_OFFER, dflt);
    }

    static void setOffered(Context ctx, boolean on) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_OFFER, on).apply();
        apply(ctx);
    }

    /** Enables or disables the HOME activity to match the offer. */
    static void apply(Context ctx) {
        try {
            ComponentName cn = new ComponentName(ctx, GatehouseHomeActivity.class);
            int want = offered(ctx)
                    ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            PackageManager pm = ctx.getPackageManager();
            if (pm.getComponentEnabledSetting(cn) != want) {
                pm.setComponentEnabledSetting(cn, want, PackageManager.DONT_KILL_APP);
            }
        } catch (Throwable ignored) {}
    }

    /** True when this app is the device's current home screen. */
    static boolean isDefaultHome(Context ctx) {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri = ctx.getPackageManager().resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            return ri != null && ri.activityInfo != null && ctx.getPackageName().equals(ri.activityInfo.packageName);
        } catch (Throwable t) {
            return false;
        }
    }

    static String statusLine(Context ctx) {
        if (!offered(ctx)) return "Not offered on this device. Hut Phone #2 has it by default; turn it on here to test it.";
        if (isDefaultHome(ctx)) return "This device's home screen.";
        return "Offered on this device, not yet chosen as the home screen. Choose it below, or from the system's home settings.";
    }

    // ------------------------------------------------------------- shared state

    /** The app calls this whenever the guard does something the welfare timer counts. */
    static void noteWelfareReset(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_WELFARE_RESET, System.currentTimeMillis()).apply();
    }

    /** Minutes left on the ninety-minute welfare clock, or -1 when it has never been set. */
    static long welfareMinutesLeft(Context ctx) {
        long reset = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_WELFARE_RESET, 0L);
        if (reset <= 0) return -1;
        long left = 90L * 60_000L - (System.currentTimeMillis() - reset);
        return Math.max(0, left / 60_000L);
    }

    static void noteWeather(Context ctx, double tempC) {
        if (Double.isNaN(tempC)) return;
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY_TEMP, (float) tempC).apply();
    }

    /** The last temperature the app showed, or NaN. */
    static double tempC(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.contains(KEY_TEMP) ? p.getFloat(KEY_TEMP, 0f) : Double.NaN;
    }

    // ------------------------------------------------------------- first light

    /** Civil dawn at the site for the given day, as "HH:mm" local. */
    static String firstLight(Calendar day) {
        double minutes = twilightMinutesUtc(day, true);
        if (Double.isNaN(minutes)) return "--:--";
        Calendar c = (Calendar) day.clone();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MINUTE, (int) Math.round(minutes));
        c.setTimeZone(TimeZone.getDefault());
        return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    /** NOAA solar position, zenith 96° for civil twilight. Minutes after 00:00 UTC. */
    private static double twilightMinutesUtc(Calendar day, boolean dawn) {
        int y = day.get(Calendar.YEAR), m = day.get(Calendar.MONTH) + 1, d = day.get(Calendar.DAY_OF_MONTH);
        double jd = julianDay(y, m, d);
        double t = (jd - 2451545.0) / 36525.0;
        double l0 = (280.46646 + t * (36000.76983 + t * 0.0003032)) % 360.0;
        double mAnom = 357.52911 + t * (35999.05029 - 0.0001537 * t);
        double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);
        double mr = Math.toRadians(mAnom);
        double c = Math.sin(mr) * (1.914602 - t * (0.004817 + 0.000014 * t))
                + Math.sin(2 * mr) * (0.019993 - 0.000101 * t) + Math.sin(3 * mr) * 0.000289;
        double trueLong = l0 + c;
        double omega = 125.04 - 1934.136 * t;
        double lambda = trueLong - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));
        double eps0 = 23.0 + (26.0 + ((21.448 - t * (46.815 + t * (0.00059 - t * 0.001813)))) / 60.0) / 60.0;
        double eps = eps0 + 0.00256 * Math.cos(Math.toRadians(omega));
        double decl = Math.toDegrees(Math.asin(Math.sin(Math.toRadians(eps)) * Math.sin(Math.toRadians(lambda))));
        double yv = Math.tan(Math.toRadians(eps / 2)); yv *= yv;
        double l0r = Math.toRadians(l0);
        double eqTime = 4 * Math.toDegrees(yv * Math.sin(2 * l0r) - 2 * e * Math.sin(mr)
                + 4 * e * yv * Math.sin(mr) * Math.cos(2 * l0r) - 0.5 * yv * yv * Math.sin(4 * l0r)
                - 1.25 * e * e * Math.sin(2 * mr));
        double latR = Math.toRadians(SITE_LAT), declR = Math.toRadians(decl);
        double cosHa = (Math.cos(Math.toRadians(96.0)) - Math.sin(latR) * Math.sin(declR)) / (Math.cos(latR) * Math.cos(declR));
        if (cosHa < -1 || cosHa > 1) return Double.NaN;
        double ha = Math.toDegrees(Math.acos(cosHa));
        double solarNoon = 720 - 4 * SITE_LON - eqTime;
        return dawn ? solarNoon - 4 * ha : solarNoon + 4 * ha;
    }

    private static double julianDay(int y, int m, int d) {
        if (m <= 2) { y -= 1; m += 12; }
        int a = y / 100;
        int b = 2 - a + a / 4;
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }
}
