package au.com.dss.gatehouse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.TimeZone;

/** A screen over the record core.
 *
 * Placeholder in the obvious ways: the site and the guard are hardcoded, and
 * a tap is a button rather than an NFC tag. A sealed record is written to a
 * file before the next one opens, so a night that finished is kept; a night
 * still running is not, and force-closing mid-shift still loses it. What is
 * not placeholder is the record
 * underneath. Every entry goes through the same Ada library that is proved
 * and tested on the desktop, the rules that refuse an entry are its rules,
 * the sentence explaining a refusal is its sentence, and the handover page is
 * rendered by it rather than assembled here.
 *
 * The screen offers what the record will actually take next. A sealed shift
 * accepts nothing, so the tiles go quiet and the one thing left to do is
 * start the next record. Buttons that do nothing when pressed are how an app
 * teaches somebody to stop trusting it.
 */
public class MainActivity extends Activity {

    private static final int NAVY = 0xFF14213D;
    private static final int PANEL = 0xFF1C2A4A;
    private static final int LINE = 0xFF2C3A5C;
    private static final int AMBER = 0xFFE8A33D;
    private static final int AMBER_INK = 0xFF14213D;
    private static final int PALE = 0xFFF2F4F8;
    private static final int MUTED = 0xFF8FA0C4;
    private static final int QUIET = 0xFF54648A;

    private static final String[] POINTS = {
        "gate A", "04A2B7C1D3E580",
        "compound", "04B1C2D3E4F590",
        "east fence", "04C9D8E7F6A5B4",
        "crane base", "04D3E2F1A0B9C8",
    };

    private LinearLayout pills;
    private LinearLayout tiles;
    private LinearLayout notes;
    private LinearLayout tonight;
    private TextView tonightTitle;
    private TextView banner;
    private TextView primary;
    private TextView pageTitle;
    private TextView page;

    private int taps = 100;
    private int openedAt;

    /** Minutes since 1970-01-01 00:00 in the zone this site keeps, which is
     *  what the core means by a time. Local rather than UTC: see
     *  Gatehouse.Clock. */
    private static int nowMinutes() {
        long ms = System.currentTimeMillis();
        return (int) ((ms + TimeZone.getDefault().getOffset(ms)) / 60000L);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(NAVY);
        root.setPadding(dp(20), dp(16), dp(20), dp(28));

        root.addView(header());

        pills = new LinearLayout(this);
        pills.setOrientation(LinearLayout.HORIZONTAL);
        pills.setPadding(0, dp(14), 0, dp(4));
        root.addView(pills);

        // Directly under the status pills, where a refusal is on screen
        // without scrolling. It used to sit below the seal button, off the
        // bottom of a phone: the record refused an entry and said why, and
        // the guard never saw the sentence.
        banner = new TextView(this);
        banner.setTextSize(14);
        banner.setTextColor(AMBER);
        banner.setPadding(dp(14), dp(12), dp(14), dp(12));
        banner.setBackground(rounded(PANEL, dp(12)));
        banner.setVisibility(View.GONE);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bl.topMargin = dp(14);
        bl.bottomMargin = dp(4);
        banner.setLayoutParams(bl);
        root.addView(banner);

        root.addView(label("checkpoints"));
        tiles = new LinearLayout(this);
        tiles.setOrientation(LinearLayout.VERTICAL);
        tiles.setPadding(0, dp(10), 0, dp(20));
        for (int i = 0; i < POINTS.length; i += 4) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(tile(POINTS[i], POINTS[i + 1], true));
            row.addView(tile(POINTS[i + 2], POINTS[i + 3], false));
            tiles.addView(row);
        }
        root.addView(tiles);

        root.addView(tonightLabel());
        tonight = new LinearLayout(this);
        tonight.setOrientation(LinearLayout.VERTICAL);
        tonight.setPadding(0, dp(10), 0, dp(22));
        root.addView(tonight);

        root.addView(label("write up"));
        notes = new LinearLayout(this);
        notes.setOrientation(LinearLayout.VERTICAL);
        notes.setPadding(0, dp(10), 0, dp(22));
        notes.addView(ghost("a note for the day crew", new View.OnClickListener() {
            public void onClick(View v) {
                note(Core.TOPIC_FOR_DAY_CREW,
                     "floodlight out over the east stack");
            }
        }));
        notes.addView(ghost("an incident", new View.OnClickListener() {
            public void onClick(View v) {
                note(Core.TOPIC_INCIDENT,
                     "two people at the east fence, moved off north when the "
                     + "torch was put on them");
            }
        }));
        notes.addView(ghost("a note the record will refuse",
                            new View.OnClickListener() {
            public void onClick(View v) {
                note(Core.TOPIC_ROUTINE, "vehicles:\nute, van");
            }
        }));
        root.addView(notes);

        primary = new TextView(this);
        primary.setTextSize(16);
        primary.setGravity(Gravity.CENTER);
        primary.setPadding(dp(16), dp(17), dp(16), dp(17));
        root.addView(primary);

        pageTitle = label("the handover");
        pageTitle.setPadding(0, dp(26), 0, dp(10));
        pageTitle.setVisibility(View.GONE);
        root.addView(pageTitle);

        page = new TextView(this);
        page.setTextColor(PALE);
        page.setTextSize(9);
        page.setTypeface(Typeface.MONOSPACE);
        page.setBackground(rounded(PANEL, dp(12)));
        page.setPadding(dp(12), dp(12), dp(12), dp(12));
        page.setVisibility(View.GONE);
        root.addView(page);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(NAVY);
        scroll.setFitsSystemWindows(true);
        scroll.addView(root);
        setContentView(scroll);

        startShift();
    }

    // ---- the pieces ------------------------------------------------------

    private LinearLayout header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);

        TextView word = new TextView(this);
        word.setText("GATEHOUSE");
        word.setTextColor(AMBER);
        word.setTextSize(12);
        word.setLetterSpacing(0.18f);
        h.addView(word);

        TextView site = new TextView(this);
        site.setText("Northgate Rise, stage 2");
        site.setTextColor(PALE);
        site.setTextSize(25);
        site.setPadding(0, dp(4), 0, 0);
        h.addView(site);

        TextView who = new TextView(this);
        who.setText("R. Kelso");
        who.setTextColor(MUTED);
        who.setTextSize(14);
        h.addView(who);
        return h;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(QUIET);
        t.setTextSize(12);
        t.setLetterSpacing(0.12f);
        return t;
    }

    private TextView pill(String text, boolean strong) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(strong ? AMBER_INK : MUTED);
        t.setPadding(dp(11), dp(6), dp(11), dp(6));
        t.setBackground(strong ? rounded(AMBER, dp(20))
                               : outlined(LINE, dp(20)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        t.setLayoutParams(lp);
        return t;
    }

    /** A checkpoint. Four of them, none singled out as next: the patrol is
     *  meant to be walked in whatever order the guard chooses. */
    /** A checkpoint tile, showing what the record holds for that point.
     *
     *  It used to show the name alone, and the only sign a tap had landed was
     *  a small count changing elsewhere on the page. A guard who is not sure
     *  taps again, and a second tap is a second visit in the record on a point
     *  walked once: the screen was quietly inviting the record to say
     *  something untrue. The count and the time under the name come from the
     *  core, so what the tile claims is what the record holds. */
    private TextView tile(final String name, final String uid, boolean left) {
        TextView t = new TextView(this);
        t.setTag(name);
        t.setText(name);
        t.setTextSize(16);
        t.setTextColor(PALE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(18), dp(12), dp(18));
        t.setBackground(pressable(PANEL, dp(14)));
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { tap(name, uid); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = left ? dp(5) : 0;
        lp.leftMargin = left ? 0 : dp(5);
        lp.bottomMargin = dp(10);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView ghost(String text, View.OnClickListener onClick) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(15);
        t.setTextColor(PALE);
        t.setPadding(dp(16), dp(15), dp(16), dp(15));
        t.setBackground(pressableOutline(LINE, dp(12)));
        t.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView tonightLabel() {
        tonightTitle = label("tonight so far");
        return tonightTitle;
    }

    /** The record, read back out of the record.
     *
     *  Every line here comes from the core, one call per entry, rather than
     *  from anything this screen remembered doing. That matters more than it
     *  looks: an app listing what it believes it sent would go on showing an
     *  entry the record refused, and would be the last thing to find out it
     *  had drifted. Walking until the core returns nothing also means a
     *  restored record fills the list in correctly with no extra work. */
    private void fillTonight() {
        tonight.removeAllViews();
        int shown = 0;
        for (int i = 1; ; i++) {
            String line = Core.entryLine(i);
            if (line.length() == 0) {
                break;
            }
            tonight.addView(entryRow(line, i));
            shown++;
        }
        boolean any = shown > 0;
        tonight.setVisibility(any ? View.VISIBLE : View.GONE);
        tonightTitle.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private TextView entryRow(String line, int seq) {
        TextView t = new TextView(this);
        t.setText(line);
        t.setTextSize(14);
        t.setTextColor(PALE);
        t.setTypeface(Typeface.MONOSPACE);
        t.setPadding(dp(14), dp(11), dp(14), dp(11));
        t.setBackground(rounded(PANEL, dp(10)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        t.setLayoutParams(lp);
        return t;
    }

    private GradientDrawable rounded(int fill, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        return g;
    }

    private GradientDrawable outlined(int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x00000000);
        g.setStroke(dp(1), stroke);
        g.setCornerRadius(radius);
        return g;
    }

    private RippleDrawable pressable(int fill, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x44E8A33D),
                                  rounded(fill, radius), null);
    }

    private RippleDrawable pressableOutline(int stroke, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(0x44E8A33D),
                                  outlined(stroke, radius), null);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ---- what the record is asked to do ----------------------------------

    private void startShift() {
        int t = nowMinutes();
        openedAt = t;
        Core.siteBegin("Northgate Rise, stage 2");
        for (int i = 0; i < POINTS.length; i += 2) {
            Core.siteAddPoint(POINTS[i], POINTS[i + 1]);
        }
        Core.sitePolicy(1, 240, 0);
        Core.setAttribution(Core.DEVICE_PERSONAL, Core.METHOD_SESSION);
        Core.setGuard("g-kelso", "R. Kelso", "SAMPLE-LIC", "typed", "");
        answer(Core.openShift(Core.genesis(), Core.siteHash(), t, t,
                              "on site, handover from day crew taken"));
        hidePage();
    }

    private void tap(String name, String uid) {
        int t = nowMinutes();
        taps++;
        answer(Core.addCheckpoint(t, t, name, uid, taps,
                                  Core.AUTH_CRYPTOGRAPHIC));
    }

    private void note(int topic, String text) {
        int t = nowMinutes();
        answer(Core.addNote(Core.KIND_OBSERVATION, topic, t, t, text, 0));
    }

    private void sealAndShow() {
        int t = nowMinutes();
        answer(Core.seal(t, t, "off site"));
        String text = Core.report(openedAt, t);
        if (text.length() > 0) {
            page.setText(text);
            page.setVisibility(View.VISIBLE);
            pageTitle.setVisibility(View.VISIBLE);
        }
    }

    /** Writes the archive to a file, then tells the core it was stored.
     *
     *  Two calls, not one, and the second is the honest half. The core hands
     *  over the bytes; only this side knows whether they reached anywhere
     *  that survives the app being killed. Saying Kept without writing them
     *  would be a lie the core cannot catch, which is exactly what this app
     *  was doing before the two were separated. */
    private boolean keepArchive() {
        String text = Core.archive();
        if (text.length() == 0) {
            return false;
        }
        try {
            File out = new File(getFilesDir(),
                                "record-" + Core.head().substring(0, 16) + ".txt");
            FileOutputStream f = new FileOutputStream(out);
            f.write(text.getBytes("UTF-8"));
            f.close();
        } catch (Exception e) {
            return false;
        }
        int r = Core.kept();
        return r == Core.OK;
    }

    /** The next record, opened on this one's head so the two read as one run.
     *  The core will not start it until this one has been stored, which is
     *  the whole point: opening the next chain throws this one away, and a
     *  night nobody kept is gone. */
    private void nextShift() {
        int t = nowMinutes();
        if (!keepArchive()) {
            banner.setText("could not write the record out; it has not been kept");
            banner.setVisibility(View.VISIBLE);
            return;
        }
        int r = Core.continueShift(t, t, "on site, continuation");
        if (r == Core.OK) {
            openedAt = t;
            hidePage();
        }
        answer(r);
    }

    private void hidePage() {
        page.setVisibility(View.GONE);
        pageTitle.setVisibility(View.GONE);
    }

    /** The core answered. If it refused, show its words, never ours. */
    private void answer(int result) {
        String why = Core.lastReason();
        if (result == Core.OK || why.length() == 0) {
            banner.setVisibility(View.GONE);
        } else {
            banner.setText(why);
            banner.setVisibility(View.VISIBLE);
        }
        refresh();
    }

    /** Offer what the record will take. A sealed shift accepts nothing, so
     *  the tiles and the write-up buttons go quiet instead of sitting there
     *  refusing every press. */
    private void refresh() {
        boolean isSealed = Core.isSealed() == 1;
        int n = Core.entryCount();

        pills.removeAllViews();
        pills.addView(pill(n + (n == 1 ? " entry" : " entries"), false));
        pills.addView(pill(isSealed ? "sealed" : "open", isSealed));
        pills.addView(pill(Core.verified() == 1 ? "verifies" : "BROKEN", false));

        for (int r = 0; r < tiles.getChildCount(); r++) {
            LinearLayout row = (LinearLayout) tiles.getChildAt(r);
            for (int k = 0; k < row.getChildCount(); k++) {
                TextView t = (TextView) row.getChildAt(k);
                markTile(t, (String) t.getTag());
            }
        }
        fillTonight();

        setLive(tiles, !isSealed);
        setLive(notes, !isSealed);

        if (isSealed) {
            primary.setText("start the next record");
            primary.setTextColor(PALE);
            primary.setBackground(pressableOutline(LINE, dp(14)));
            primary.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { nextShift(); }
            });
        } else {
            primary.setText("seal and read the handover");
            primary.setTextColor(AMBER_INK);
            primary.setBackground(pressable(AMBER, dp(14)));
            primary.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { askThenSeal(); }
            });
        }
    }

    /** Sealing is final: nothing can be added to a sealed record, by design.
     *  It was one tap, in the largest control on the screen, with the tiles
     *  right above it. A guard reaching for a checkpoint and catching this
     *  instead ended the night, and no part of the record can undo that.
     *
     *  The count is in the question because it is the thing worth checking:
     *  a guard who reads "seal 3 entries" at 05:00 knows something is wrong
     *  before the record says so. */
    private void askThenSeal() {
        int n = Core.entryCount();
        new AlertDialog.Builder(this)
            .setTitle("Seal the record?")
            .setMessage("Sealing ends tonight's record with " + n
                        + (n == 1 ? " entry" : " entries")
                        + ". Nothing can be added to it afterwards.")
            .setNegativeButton("not yet", null)
            .setPositiveButton("seal", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    sealAndShow();
                }
            })
            .show();
    }

    /** What the record holds for this point, under its name. Never walked
     *  reads as exactly that rather than as a zero, because "0 visits" and
     *  "not yet" are the same fact and only one of them is a sentence. */
    private void markTile(TextView t, String name) {
        int visits = Core.pointVisits(name);
        if (visits <= 0) {
            t.setText(name + "\nnot yet");
            t.setTextColor(PALE);
            return;
        }
        int at = Core.pointLast(name);
        t.setText(name + "\n" + clock(at)
                  + (visits > 1 ? "  x" + visits : ""));
        t.setTextColor(AMBER);
    }

    private String clock(int minutes) {
        int h = (minutes / 60) % 24;
        int m = minutes % 60;
        return (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
    }

    private void setLive(LinearLayout group, boolean on) {
        group.setAlpha(on ? 1f : 0.3f);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setEnabled(on);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int k = 0; k < row.getChildCount(); k++) {
                    row.getChildAt(k).setEnabled(on);
                }
            }
        }
    }
}
