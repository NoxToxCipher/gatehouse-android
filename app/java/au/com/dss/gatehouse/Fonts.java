package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

/**
 * Fonts — the three type roles of the instrument design, loaded from
 * app/assets/fonts and cached. Every call falls back to a system face when an
 * asset is missing, so a build without the font files still runs and lays out
 * correctly; it just looks generic until the files land.
 *
 *   display()  Sora            the time and the site name, the two things you glance at
 *   text()     IBM Plex Sans   everything you read
 *   mono()     IBM Plex Mono   everything you measure: times, licence, hash
 */
public final class Fonts {

    private static final Map<String, Typeface> CACHE = new HashMap<>();

    private Fonts() {}

    /** Sora, a variable font; weight 700 when bold, 600 otherwise. */
    public static Typeface display(Context c, boolean bold) {
        String key = "sora:" + (bold ? 700 : 600);
        Typeface t = CACHE.get(key);
        if (t != null) return t;
        t = variable(c, "fonts/Sora[wght].ttf", bold ? 700 : 600,
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        CACHE.put(key, t);
        return t;
    }

    /** IBM Plex Sans (a variable font) at 400, 500 or 600. */
    public static Typeface text(Context c, int weight) {
        int w = weight >= 600 ? 600 : weight >= 500 ? 500 : 400;
        String key = "plexsans:" + w;
        Typeface t = CACHE.get(key);
        if (t != null) return t;
        Typeface fallback = w >= 600
                ? Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) : Typeface.SANS_SERIF;
        t = variable(c, "fonts/IBMPlexSans[wdth,wght].ttf", w, fallback);
        CACHE.put(key, t);
        return t;
    }

    /** IBM Plex Mono, regular or medium. */
    public static Typeface mono(Context c, boolean medium) {
        String file = medium ? "fonts/IBMPlexMono-Medium.ttf" : "fonts/IBMPlexMono-Regular.ttf";
        Typeface fallback = medium
                ? Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) : Typeface.MONOSPACE;
        return asset(c, file, fallback);
    }

    private static Typeface asset(Context c, String path, Typeface fallback) {
        Typeface t = CACHE.get(path);
        if (t != null) return t;
        try {
            t = Typeface.createFromAsset(c.getAssets(), path);
        } catch (Throwable missing) {
            t = null;
        }
        if (t == null) t = fallback;
        CACHE.put(path, t);
        return t;
    }

    private static Typeface variable(Context c, String path, int weight, Typeface fallback) {
        try {
            Typeface t = new Typeface.Builder(c.getAssets(), path)
                    .setFontVariationSettings("'wght' " + weight)
                    .build();
            return t != null ? t : fallback;
        } catch (Throwable missing) {
            return fallback;
        }
    }
}
