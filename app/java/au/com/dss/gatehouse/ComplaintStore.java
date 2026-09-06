package au.com.dss.gatehouse;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The complaint book: what a guard raised, and the firm's answer, kept by ticket
 * and never by name. A complaint carries no author. The only record of who filed
 * a ticket is a private list on the phone it was filed from, kept so that guard
 * can read the answer; it is never written into a complaint, never sealed into
 * the shift record, and never shown to control.
 *
 * The book lives at files/complaints.json on the phone. Today that means a
 * complaint and its answer are shared on one phone, or across the shared hut
 * phones. A sync transport (SMS between hut phones, or the firm's server) can
 * later carry the same book between devices without changing a screen.
 */
public final class ComplaintStore {

    private static final String FILE = "complaints.json";
    private static final String PREFS = "gatehouse_complaints";
    /** No look-alike characters, so a ticket read aloud is unambiguous. */
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RNG = new SecureRandom();

    public static final String LODGED = "LODGED";
    public static final String ANSWERED = "ANSWERED";
    public static final String RESOLVED = "RESOLVED";

    public static final String[][] CATEGORIES = {
            {"pay", "Pay & hours"},
            {"roster", "Roster & shifts"},
            {"safety", "Safety on site"},
            {"conduct", "Someone's conduct"},
            {"equipment", "Equipment & the hut"},
            {"other", "Something else"},
    };

    public static final class Complaint {
        public String id = "";
        public String category = "";
        public String categoryLabel = "";
        public String text = "";
        public long createdAt;
        public String status = LODGED;
        public String response = "";
        public String respondedBy = "";
        public long respondedAt;

        public boolean answered() { return !response.isEmpty(); }
    }

    private ComplaintStore() {}

    /** Wipes the complaint book and the per-phone "mine" lists. For test resets. */
    public static void clear(Context ctx) {
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            if (f.exists()) f.delete();
        } catch (Throwable ignored) {}
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------------ reading

    /** Every complaint, newest first. */
    public static List<Complaint> all(Context ctx) {
        List<Complaint> out = new ArrayList<>();
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            if (!f.exists()) return out;
            JSONArray a = new JSONArray(readAll(f));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                Complaint c = new Complaint();
                c.id = o.optString("id", "");
                c.category = o.optString("category", "");
                c.categoryLabel = o.optString("categoryLabel", labelFor(c.category));
                c.text = o.optString("text", "");
                c.createdAt = o.optLong("createdAt", 0L);
                c.status = o.optString("status", LODGED);
                c.response = o.optString("response", "");
                c.respondedBy = o.optString("respondedBy", "");
                c.respondedAt = o.optLong("respondedAt", 0L);
                out.add(c);
            }
        } catch (Throwable ignored) {}
        Collections.sort(out, new Comparator<Complaint>() {
            public int compare(Complaint a, Complaint b) { return Long.compare(b.createdAt, a.createdAt); }
        });
        return out;
    }

    public static Complaint byId(Context ctx, String id) {
        if (id == null) return null;
        for (Complaint c : all(ctx)) if (c.id.equals(id)) return c;
        return null;
    }

    /** The tickets filed on this phone by whoever is signed in (or nobody), newest first. */
    public static List<Complaint> mine(Context ctx) {
        List<String> ids = mineIds(ctx);
        List<Complaint> out = new ArrayList<>();
        for (Complaint c : all(ctx)) if (ids.contains(c.id)) out.add(c);
        return out;
    }

    /** Open first (oldest open first, so the longest wait is on top), then answered, then resolved. */
    public static List<Complaint> forControl(Context ctx) {
        List<Complaint> out = all(ctx);
        Collections.sort(out, new Comparator<Complaint>() {
            public int compare(Complaint a, Complaint b) {
                int ra = rank(a), rb = rank(b);
                if (ra != rb) return Integer.compare(ra, rb);
                if (ra == 0) return Long.compare(a.createdAt, b.createdAt); // oldest unanswered first
                return Long.compare(b.createdAt, a.createdAt);
            }
            int rank(Complaint c) { return RESOLVED.equals(c.status) ? 2 : c.answered() ? 1 : 0; }
        });
        return out;
    }

    public static int openCount(Context ctx) {
        int n = 0;
        for (Complaint c : all(ctx)) if (!c.answered() && !RESOLVED.equals(c.status)) n++;
        return n;
    }

    // ------------------------------------------------------------------ writing

    /** Files a complaint, name-free, and remembers the ticket on this phone. Returns the ticket. */
    public static Complaint lodge(Context ctx, String categoryKey, String text) {
        return lodge(ctx, categoryKey, text, false);
    }

    public static Complaint lodge(Context ctx, String categoryKey, String text, boolean anonymous) {
        Complaint c = new Complaint();
        c.id = newTicket(ctx);
        c.category = categoryKey == null ? "other" : categoryKey;
        c.categoryLabel = labelFor(c.category);
        c.text = text == null ? "" : text.trim();
        c.createdAt = System.currentTimeMillis();
        c.status = LODGED;
        List<Complaint> list = all(ctx);
        list.add(0, c);
        save(ctx, list);
        rememberMine(ctx, c.id, anonymous);
        return c;
    }

    /** Control's answer against a ticket. The answerer's name is kept; the asker's is not. */
    public static boolean answer(Context ctx, String id, String response, String byName) {
        List<Complaint> list = all(ctx);
        boolean hit = false;
        for (Complaint c : list) {
            if (c.id.equals(id)) {
                c.response = response == null ? "" : response.trim();
                c.respondedBy = byName == null ? "" : byName;
                c.respondedAt = System.currentTimeMillis();
                if (LODGED.equals(c.status)) c.status = ANSWERED;
                hit = true;
                break;
            }
        }
        if (hit) save(ctx, list);
        return hit;
    }

    public static boolean setResolved(Context ctx, String id, boolean resolved) {
        List<Complaint> list = all(ctx);
        boolean hit = false;
        for (Complaint c : list) {
            if (c.id.equals(id)) {
                c.status = resolved ? RESOLVED : (c.answered() ? ANSWERED : LODGED);
                hit = true;
                break;
            }
        }
        if (hit) save(ctx, list);
        return hit;
    }

    // ------------------------------------------------------------------ helpers

    public static String labelFor(String key) {
        for (String[] c : CATEGORIES) if (c[0].equals(key)) return c[1];
        return "Something else";
    }

    private static String newTicket(Context ctx) {
        List<String> existing = new ArrayList<>();
        for (Complaint c : all(ctx)) existing.add(c.id);
        for (int attempt = 0; attempt < 50; attempt++) {
            StringBuilder sb = new StringBuilder(9);
            for (int i = 0; i < 8; i++) {
                if (i == 4) sb.append('-');
                sb.append(ALPHABET[RNG.nextInt(ALPHABET.length)]);
            }
            String t = sb.toString();
            if (!existing.contains(t)) return t;
        }
        return "T" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.US);
    }

    private static List<String> mineIds(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<String> out = new ArrayList<>();
        for (String key : new String[]{"mine_anon", "mine_" + guardKey(ctx)}) {
            String raw = p.getString(key, "");
            if (raw.isEmpty()) continue;
            for (String s : raw.split(",")) if (!s.isEmpty() && !out.contains(s)) out.add(s);
        }
        return out;
    }

    private static void rememberMine(Context ctx, String id) {
        rememberMine(ctx, id, false);
    }

    private static void rememberMine(Context ctx, String id, boolean anonymous) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = "mine_" + (anonymous ? "anon" : guardKey(ctx));
        String raw = p.getString(key, "");
        p.edit().putString(key, raw.isEmpty() ? id : raw + "," + id).apply();
    }

    private static String guardKey(Context ctx) {
        String k = GuardSession.key(ctx);
        return k != null ? k : "anon";
    }

    private static void save(Context ctx, List<Complaint> list) {
        JSONArray a = new JSONArray();
        for (Complaint c : list) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("category", c.category);
                o.put("categoryLabel", c.categoryLabel);
                o.put("text", c.text);
                o.put("createdAt", c.createdAt);
                o.put("status", c.status);
                o.put("response", c.response);
                o.put("respondedBy", c.respondedBy);
                o.put("respondedAt", c.respondedAt);
                a.put(o);
            } catch (JSONException ignored) {}
        }
        try {
            File f = new File(ctx.getFilesDir(), FILE);
            File tmp = new File(ctx.getFilesDir(), FILE + ".tmp");
            FileOutputStream out = new FileOutputStream(tmp);
            try {
                out.write(a.toString().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            } finally {
                out.close();
            }
            if (!tmp.renameTo(f)) {
                if (f.exists()) f.delete();
                tmp.renameTo(f);
            }
        } catch (IOException ignored) {}
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

    // ------------------------------------------------------------------ time words

    /** "just now", "14 min", "3 h", "2 days" for a duration in ms. */
    public static String elapsedWords(long fromMs, long toMs) {
        long ms = Math.max(0, toMs - fromMs);
        long min = ms / 60000L;
        if (min < 1) return "just now";
        if (min < 60) return min + " min";
        long h = min / 60;
        if (h < 24) return h + (h == 1 ? " hour" : " hours");
        long d = h / 24;
        return d + (d == 1 ? " day" : " days");
    }
}
