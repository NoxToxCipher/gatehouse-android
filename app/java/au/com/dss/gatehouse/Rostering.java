package au.com.dss.gatehouse;

import android.content.Context;

/**
 * Rostering — the one place that decides which {@link RosterProvider} the app
 * runs against. Everything else in Gatehouse depends only on the interface, so
 * switching back-ends is a stored preference, not edits scattered through the UI.
 *
 * Providers: Deputy (default), Tanda, and a no-server Manual/CSV roster. Set the
 * active one with {@link #setActiveProvider}; a settings picker can drive that.
 */
public final class Rostering {

    private static final String PREFS = "gatehouse_rostering";
    private static final String KEY_PROVIDER = "active_provider";

    public static final String DEPUTY = "deputy";
    public static final String TANDA = "tanda";
    public static final String MANUAL = "manual";

    private Rostering() {}

    /** The active provider for this device. */
    public static RosterProvider create(Context context) {
        switch (getActiveProviderKey(context)) {
            case TANDA:  return new TandaApi(context);
            case MANUAL: return new ManualRoster(context);
            case DEPUTY:
            default:     return new DeputyApi(context);
        }
    }

    public static String getActiveProviderKey(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PROVIDER, DEPUTY);
    }

    public static void setActiveProvider(Context context, String key) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_PROVIDER, key).apply();
    }

    /** Display label for a provider key, for a settings picker. */
    public static String labelFor(String key) {
        if (TANDA.equals(key)) return "Tanda";
        if (MANUAL.equals(key)) return "Manual / CSV (no server)";
        return "Deputy";
    }

    public static String[] availableProviders() {
        return new String[]{DEPUTY, TANDA, MANUAL};
    }
}
