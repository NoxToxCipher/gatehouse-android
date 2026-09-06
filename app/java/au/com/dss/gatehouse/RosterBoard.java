package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The roster as a board: what is happening now, then what is next, on
 * hairlines, in the instrument's own type. One painter serves the home
 * screen widget (with the hero) and the launcher (rows only, since the
 * dial and the site line already say what is on now).
 */
final class RosterBoard {
    private RosterBoard() {}

    static final int PALE = 0xFFE8E4DA;
    static final int MUTED = 0xFF8A9099;
    static final int QUIET = 0xFF5C636C;
    static final int HAIR = 0x22E8E4DA;
    static final int BRASS_HAIR = 0x66E5A93C;
    static final int GREEN = 0xFF22C55E;
    static final int AMBER = 0xFFF59E0B;

    /** A bitmap of the full board, for the widget. */
    static Bitmap render(Context ctx, int w, int h, RosterProvider.Result roster) {
        try {
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            draw(ctx, new Canvas(bmp), w, h, roster, true);
            return bmp;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Draws the board into the given space. Scales with the width, so it reads the same at any size. */
    static void draw(Context ctx, Canvas c, int w, int h, RosterProvider.Result roster, boolean withHero) {
        float u = w / 340f;
        float pad = 14f * u;

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint display = new Paint(Paint.ANTI_ALIAS_FLAG);
        mono.setTypeface(Fonts.mono(ctx, false));
        text.setTypeface(Fonts.text(ctx, 400));
        display.setTypeface(Fonts.display(ctx, true));
        line.setStrokeWidth(Math.max(1f, u));
        line.setStyle(Paint.Style.STROKE);

        long nowSec = System.currentTimeMillis() / 1000L;
        Calendar now = Calendar.getInstance();

        // header: ROSTER · SUN 7 SEP                               synced 12 min ago
        float y = pad + 10f * u;
        mono.setTextSize(9.5f * u);
        mono.setLetterSpacing(0.14f);
        mono.setColor(QUIET);
        mono.setTextAlign(Paint.Align.LEFT);
        String today = new SimpleDateFormat("EEE d MMM", Locale.US).format(now.getTime()).toUpperCase(Locale.US);
        c.drawText("ROSTER · " + today, pad, y, mono);
        if (roster != null) {
            boolean stale = roster.syncTimestamp > 0 && System.currentTimeMillis() - roster.syncTimestamp > 36L * 3600_000L;
            mono.setLetterSpacing(0.02f);
            mono.setColor(stale ? AMBER : QUIET);
            mono.setTextAlign(Paint.Align.RIGHT);
            c.drawText(age(roster.syncTimestamp), w - pad, y, mono);
            mono.setTextAlign(Paint.Align.LEFT);
        }

        // shifts in order: the live one, then what is next
        List<RosterProvider.Shift> shifts = new ArrayList<>();
        if (roster != null && roster.weekShifts != null) shifts.addAll(roster.weekShifts);
        Collections.sort(shifts, new Comparator<RosterProvider.Shift>() {
            @Override public int compare(RosterProvider.Shift a, RosterProvider.Shift b) {
                return Long.compare(a.startTs, b.startTs);
            }
        });
        RosterProvider.Shift live = null;
        List<RosterProvider.Shift> upcoming = new ArrayList<>();
        for (RosterProvider.Shift s : shifts) {
            if (s == null || s.startTs <= 0) continue;
            if (s.startTs <= nowSec && nowSec < s.endTs) { if (live == null) live = s; }
            else if (s.startTs > nowSec) upcoming.add(s);
        }

        if (withHero) {
            y += 24f * u;
            RosterProvider.Shift hero = live != null ? live : (upcoming.isEmpty() ? null : upcoming.remove(0));
            if (hero != null) {
                mono.setTextSize(9.5f * u);
                mono.setLetterSpacing(0.14f);
                mono.setColor(MUTED);
                float labelX = pad;
                if (live != null) {
                    fill.setColor(GREEN);
                    c.drawCircle(pad + 3.5f * u, y - 3.5f * u, 3.5f * u, fill);
                    labelX = pad + 12f * u;
                    c.drawText("ON SHIFT NOW", labelX, y, mono);
                } else {
                    c.drawText("NEXT SHIFT · " + dayLabel(hero.startTs, now), labelX, y, mono);
                }
                mono.setLetterSpacing(0.02f);
                mono.setTextSize(11f * u);
                mono.setColor(QUIET);
                mono.setTextAlign(Paint.Align.RIGHT);
                c.drawText(live != null ? left(live.endTs - nowSec) + " left" : "in " + left(hero.startTs - nowSec), w - pad, y, mono);
                mono.setTextAlign(Paint.Align.LEFT);

                y += 32f * u;
                display.setTextSize(28f * u);
                display.setColor(PALE);
                c.drawText(range(hero), pad, y, display);

                y += 20f * u;
                text.setTextSize(13f * u);
                text.setColor(MUTED);
                c.drawText(ellipsize(hero.guardName + " · " + unitOf(hero), text, w - 2 * pad), pad, y, text);
            } else {
                mono.setTextSize(9.5f * u);
                mono.setLetterSpacing(0.14f);
                mono.setColor(MUTED);
                c.drawText(roster == null ? "NO ROSTER ON THIS PHONE" : "NOTHING ROSTERED", pad, y, mono);
                y += 30f * u;
                display.setTextSize(22f * u);
                display.setColor(PALE);
                c.drawText(roster == null ? "Open Gatehouse and sync" : "No shifts this week", pad, y, display);
                y += 20f * u;
                text.setTextSize(13f * u);
                text.setColor(MUTED);
                c.drawText(roster == null ? "The roster shows here once it has been fetched." : "The week's roster is empty.", pad, y, text);
            }
            y += 16f * u;
        } else {
            y += 10f * u;
            if (roster == null || upcoming.isEmpty()) {
                y += 22f * u;
                text.setTextSize(13f * u);
                text.setColor(MUTED);
                c.drawText(roster == null ? "No roster on this device yet." : "Nothing more rostered this week.", pad, y, text);
                return;
            }
        }

        // a brass hairline, then the coming shifts on hairlines, the day named once per day
        line.setColor(BRASS_HAIR);
        c.drawLine(pad, y, w - pad, y, line);

        float rowH = 30f * u;
        float bottomRoom = pad + 10f * u;
        int rowsFit = Math.max(0, (int) Math.floor((h - bottomRoom - y) / rowH));
        int shown = 0;
        String lastDay = "";
        float colDay = pad, colTime = pad + 76f * u, colName = pad + 194f * u;
        for (RosterProvider.Shift s : upcoming) {
            if (shown >= rowsFit) break;
            float base = y + rowH * shown + 20f * u;
            String day = dayLabel(s.startTs, now);
            if (!day.equals(lastDay)) {
                mono.setTextSize(9.5f * u);
                mono.setLetterSpacing(0.14f);
                mono.setColor(QUIET);
                c.drawText(day, colDay, base, mono);
                lastDay = day;
            }
            mono.setLetterSpacing(0f);
            mono.setTextSize(13f * u);
            mono.setColor(PALE);
            c.drawText(range(s), colTime, base, mono);
            text.setTextSize(13f * u);
            text.setColor(MUTED);
            c.drawText(ellipsize(s.guardName, text, w - pad - colName), colName, base, text);
            if (shown + 1 < Math.min(rowsFit, upcoming.size())) {
                line.setColor(HAIR);
                float sep = y + rowH * (shown + 1);
                c.drawLine(pad, sep, w - pad, sep, line);
            }
            shown++;
        }
        int more = upcoming.size() - shown;
        if (more > 0 && rowsFit > 0) {
            mono.setTextSize(10f * u);
            mono.setLetterSpacing(0.02f);
            mono.setColor(QUIET);
            mono.setTextAlign(Paint.Align.RIGHT);
            c.drawText("+" + more + " more this week", w - pad, h - pad + 4f * u, mono);
        }
    }

    // ------------------------------------------------------------------ words

    static String range(RosterProvider.Shift s) {
        String r = s.getFormattedHoursRange();
        int p = r.indexOf(" (");
        return p > 0 ? r.substring(0, p) : r;
    }

    static String dayLabel(long startTs, Calendar now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startTs * 1000L);
        if (sameDay(c, now)) return "TODAY";
        Calendar t = (Calendar) now.clone();
        t.add(Calendar.DAY_OF_YEAR, 1);
        if (sameDay(c, t)) return "TOMORROW";
        return new SimpleDateFormat("EEE d MMM", Locale.US).format(c.getTime()).toUpperCase(Locale.US);
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    static String left(long secs) {
        long m = Math.max(0, secs) / 60;
        long h = m / 60;
        m = m % 60;
        return h > 0 ? h + " h " + m + " m" : m + " m";
    }

    static String age(long syncTs) {
        if (syncTs <= 0) return "";
        long mins = (System.currentTimeMillis() - syncTs) / 60000L;
        if (mins < 1) return "synced just now";
        if (mins < 60) return "synced " + mins + " min ago";
        long hours = mins / 60;
        if (hours < 36) return "synced " + hours + " h ago";
        return "roster " + (hours / 24) + " days old";
    }

    private static String ellipsize(String s, Paint p, float maxW) {
        if (s == null) return "";
        if (p.measureText(s) <= maxW) return s;
        int n = s.length();
        while (n > 1 && p.measureText(s.substring(0, n) + "…") > maxW) n--;
        return s.substring(0, n).trim() + "…";
    }

    private static String unitOf(RosterProvider.Shift s) {
        return s.operationalUnit != null && !s.operationalUnit.isEmpty() ? s.operationalUnit : "Hume Doors & Timber";
    }
}
