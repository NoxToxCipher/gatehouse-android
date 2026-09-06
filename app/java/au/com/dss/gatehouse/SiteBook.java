package au.com.dss.gatehouse;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The site's people and numbers: the guards with their PIN hashes, the hut phones,
 * who control and emergency texts go to, and the site's own after-hours contacts.
 *
 * It lives only on the phone, at files/sitebook.json, put there by the owner (over
 * adb through {@link SiteBookReceiver}, or imported from a file in Settings). The
 * source ships none of it, so the public repository and the APK it publishes carry
 * no name, number or PIN. When this software is sold, the book comes from the
 * owner's server instead; nothing else in the app has to change.
 */
public final class SiteBook {

    public static final String FILE = "sitebook.json";
    private static final int PIN_ITER_DEFAULT = 20000;

    public static final class Guard {
        public String key = "";
        public String name = "";
        public String mobile = "";
        public String role = "";
        public String licence = "";
        /** A theme id seeded by the owner, or -1 for "let them pick". */
        public int theme = -1;
        String pinSalt = "";
        String pinHash = "";
        int pinIter = PIN_ITER_DEFAULT;

        public boolean hasPin() { return !pinSalt.isEmpty() && !pinHash.isEmpty(); }
        public String firstName() {
            String[] p = name.trim().split(" +");
            return p.length > 0 ? p[0] : name;
        }
    }

    public static final class Contact {
        public String name = "";
        public String role = "";
        public String mobile = "";
        public String badge = "";
        public String group = "";
    }

    public int version;
    public String otaToken = "";
    public String site = "";
    public String company = "";
    public String issued = "";
    public final Map<String, String> hut = new LinkedHashMap<>();
    public final List<String> control = new ArrayList<>();
    public final List<String> emergency = new ArrayList<>();
    public final List<Guard> guards = new ArrayList<>();
    public final List<Contact> contacts = new ArrayList<>();
    public final Map<String, String> groupTitles = new LinkedHashMap<>();

    private static final SiteBook EMPTY = new SiteBook();
    private static SiteBook cached;
    private static long cachedStamp = -1L;

    private SiteBook() {}

    // ------------------------------------------------------------------ loading

    /** The book on this phone, or an empty one. Cached until the file changes. */
    public static synchronized SiteBook get(Context ctx) {
        try {
            File f = file(ctx);
            if (!f.exists()) { cached = null; cachedStamp = -1L; return EMPTY; }
            long stamp = f.lastModified() ^ (f.length() << 20);
            if (cached != null && stamp == cachedStamp) return cached;
            cached = parse(readAll(f));
            cachedStamp = stamp;
            return cached;
        } catch (Throwable t) {
            return EMPTY;
        }
    }

    /** The last book loaded, for callers with no context to hand; empty until {@link #get} has run. */
    public static synchronized SiteBook cachedOrEmpty() {
        return cached != null ? cached : EMPTY;
    }

    /** The OTA read token for a private repo, or empty to fetch the public URL. */
    public static String otaToken(Context ctx) {
        return get(ctx).otaToken;
    }

    public static boolean present(Context ctx) {
        return file(ctx).exists();
    }

    public boolean isEmpty() { return guards.isEmpty() && contacts.isEmpty() && hut.isEmpty(); }

    /**
     * Installs a book from its JSON text after checking it, atomically. Returns a
     * one-line summary; throws IllegalArgumentException with a plain message when
     * the text is not a site book.
     */
    public static synchronized String install(Context ctx, String json) {
        SiteBook book;
        try {
            book = parse(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException("That file is not a site book: " + e.getMessage());
        }
        if (book.version != 1) throw new IllegalArgumentException("Site book version " + book.version + " is not one this app reads.");
        if (book.guards.isEmpty()) throw new IllegalArgumentException("The site book lists no guards.");
        for (Guard g : book.guards) {
            if (g.key.isEmpty() || g.name.isEmpty()) throw new IllegalArgumentException("A guard entry is missing its key or name.");
        }
        File f = file(ctx);
        File tmp = new File(f.getParentFile(), FILE + ".tmp");
        try {
            FileOutputStream out = new FileOutputStream(tmp);
            try {
                out.write(json.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            } finally {
                out.close();
            }
            if (!tmp.renameTo(f)) {
                if (f.exists() && !f.delete()) throw new IOException("could not replace the old book");
                if (!tmp.renameTo(f)) throw new IOException("could not write the book");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not save the site book: " + e.getMessage());
        }
        cached = book;
        cachedStamp = f.lastModified() ^ (f.length() << 20);
        return book.summary();
    }

    public static synchronized void remove(Context ctx) {
        File f = file(ctx);
        if (f.exists()) f.delete();
        cached = null;
        cachedStamp = -1L;
    }

    public String summary() {
        if (isEmpty()) return "No site book on this phone";
        return guards.size() + (guards.size() == 1 ? " guard" : " guards")
                + " · " + contacts.size() + (contacts.size() == 1 ? " contact" : " contacts")
                + (issued.isEmpty() ? "" : " · issued " + issued);
    }

    static SiteBook parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        SiteBook b = new SiteBook();
        b.version = o.optInt("version", 0);
        b.site = o.optString("site", "");
        b.company = o.optString("company", "");
        b.issued = o.optString("issued", "");
        JSONObject ota = o.optJSONObject("ota");
        if (ota != null) b.otaToken = ota.optString("token", "");
        JSONObject hut = o.optJSONObject("hut");
        if (hut != null) {
            Iterator<String> it = hut.keys();
            while (it.hasNext()) { String k = it.next(); b.hut.put(k, hut.optString(k, "")); }
        }
        JSONArray ctl = o.optJSONArray("control");
        if (ctl != null) for (int i = 0; i < ctl.length(); i++) b.control.add(ctl.optString(i));
        JSONArray em = o.optJSONArray("emergency");
        if (em != null) for (int i = 0; i < em.length(); i++) b.emergency.add(em.optString(i));
        JSONArray gs = o.optJSONArray("guards");
        if (gs != null) {
            for (int i = 0; i < gs.length(); i++) {
                JSONObject g = gs.optJSONObject(i);
                if (g == null) continue;
                Guard guard = new Guard();
                guard.key = g.optString("key", "").trim();
                guard.name = g.optString("name", "").trim();
                guard.mobile = g.optString("mobile", "").trim();
                guard.role = g.optString("role", "").trim();
                guard.licence = g.optString("licence", "").trim();
                guard.theme = themeId(g.optString("theme", ""));
                JSONObject pin = g.optJSONObject("pin");
                if (pin != null) {
                    guard.pinSalt = pin.optString("salt", "");
                    guard.pinHash = pin.optString("hash", "");
                    guard.pinIter = pin.optInt("iter", PIN_ITER_DEFAULT);
                }
                b.guards.add(guard);
            }
        }
        JSONObject groups = o.optJSONObject("groups");
        if (groups != null) {
            Iterator<String> it = groups.keys();
            while (it.hasNext()) { String k = it.next(); b.groupTitles.put(k, groups.optString(k, "")); }
        }
        JSONArray cs = o.optJSONArray("contacts");
        if (cs != null) {
            for (int i = 0; i < cs.length(); i++) {
                JSONObject c = cs.optJSONObject(i);
                if (c == null) continue;
                Contact contact = new Contact();
                contact.name = c.optString("name", "").trim();
                contact.role = c.optString("role", "").trim();
                contact.mobile = c.optString("mobile", "").trim();
                contact.badge = c.optString("badge", "").trim();
                contact.group = c.optString("group", "").trim();
                b.contacts.add(contact);
            }
        }
        return b;
    }

    /** Theme names as the owner writes them in the book. */
    private static int themeId(String s) {
        switch (s.trim().toLowerCase(Locale.US)) {
            case "gold": case "oled gold": return MainActivity.THEME_GOLD;
            case "red": case "0-lux red": return MainActivity.THEME_RED;
            case "nvg": case "green": return MainActivity.THEME_NVG;
            case "violet": return MainActivity.THEME_VIOLET;
            case "daylight": case "day": case "light": return MainActivity.THEME_DAYLIGHT;
            case "sand": case "desert sand": return MainActivity.THEME_DESERT_SAND;
            default: return -1;
        }
    }

    // ------------------------------------------------------------------ lookups

    public Guard guard(String key) {
        if (key == null) return null;
        for (Guard g : guards) if (g.key.equals(key)) return g;
        return null;
    }

    /** The number for a guard key or a hut key ("hut1", "hut2"); empty when none. */
    public String numberFor(String key) {
        if (key == null) return "";
        Guard g = guard(key);
        if (g != null) return g.mobile;
        String h = hut.get(key);
        return h != null ? h : "";
    }

    public String displayName(String key) {
        Guard g = guard(key);
        if (g != null) return g.name;
        if ("hut1".equals(key)) return "Hut Phone #1";
        if ("hut2".equals(key)) return "Hut Phone #2";
        return key != null ? key : "";
    }

    /**
     * The guard for a roster name, or null. Rosters abbreviate ("Chris Ireton",
     * "Josh Edwards", "William NEWMAN"), so a match is the same surname plus a
     * first name that equals or begins the other.
     */
    public Guard guardForName(String rosterName) {
        if (rosterName == null) return null;
        String[] r = rosterName.trim().toLowerCase(Locale.US).split(" +");
        if (r.length == 0 || r[0].isEmpty()) return null;
        String rFirst = r[0], rLast = r[r.length - 1];
        for (Guard g : guards) {
            String[] b = g.name.toLowerCase(Locale.US).split(" +");
            if (b.length == 0 || b[0].isEmpty()) continue;
            String bFirst = b[0], bLast = b[b.length - 1];
            boolean firstFits = rFirst.equals(bFirst) || rFirst.startsWith(bFirst) || bFirst.startsWith(rFirst);
            if (b.length == 1) {
                if (rFirst.equals(bFirst)) return g;
            } else if (rLast.equals(bLast) && firstFits) {
                return g;
            }
        }
        return null;
    }

    /** Guards in surname order, for a name list. */
    public List<Guard> guardsBySurname() {
        List<Guard> out = new ArrayList<>(guards);
        java.util.Collections.sort(out, new java.util.Comparator<Guard>() {
            public int compare(Guard a, Guard b) {
                String[] x = a.name.split(" +"), y = b.name.split(" +");
                int c = x[x.length - 1].compareToIgnoreCase(y[y.length - 1]);
                return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
            }
        });
        return out;
    }

    // ------------------------------------------------------------------ PINs

    /** True when the PIN matches the guard's salted PBKDF2-HMAC-SHA256 hash. */
    public boolean verifyPin(Guard g, String pin) {
        if (g == null || !g.hasPin() || pin == null) return false;
        try {
            byte[] salt = hex(g.pinSalt);
            byte[] want = hex(g.pinHash);
            byte[] got = pbkdf2(pin.getBytes(StandardCharsets.UTF_8), salt, g.pinIter, want.length);
            int diff = 0;
            for (int i = 0; i < want.length; i++) diff |= want[i] ^ got[i];
            return diff == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /** PBKDF2 with HMAC-SHA256, written out so no platform provider is assumed. */
    static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int dkLen) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(password.length == 0 ? new byte[1] : password, "HmacSHA256"));
        int hLen = mac.getMacLength();
        int blocks = (dkLen + hLen - 1) / hLen;
        byte[] out = new byte[blocks * hLen];
        for (int block = 1; block <= blocks; block++) {
            mac.update(salt);
            mac.update(new byte[]{(byte) (block >>> 24), (byte) (block >>> 16), (byte) (block >>> 8), (byte) block});
            byte[] u = mac.doFinal();
            byte[] t = u.clone();
            for (int i = 1; i < iterations; i++) {
                u = mac.doFinal(u);
                for (int j = 0; j < t.length; j++) t[j] ^= u[j];
            }
            System.arraycopy(t, 0, out, (block - 1) * hLen, hLen);
        }
        byte[] dk = new byte[dkLen];
        System.arraycopy(out, 0, dk, 0, dkLen);
        return dk;
    }

    private static byte[] hex(String s) {
        int n = s.length() / 2;
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        return out;
    }

    // ------------------------------------------------------------------ files

    static File file(Context ctx) {
        return new File(ctx.getFilesDir(), FILE);
    }

    private static String readAll(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            byte[] buf = new byte[(int) Math.min(f.length(), 1 << 20)];
            int off = 0;
            while (off < buf.length) {
                int r = in.read(buf, off, buf.length - off);
                if (r < 0) break;
                off += r;
            }
            return new String(buf, 0, off, StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
