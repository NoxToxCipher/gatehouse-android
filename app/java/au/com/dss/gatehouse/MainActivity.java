package au.com.dss.gatehouse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputType;
import android.widget.EditText;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
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

    /** How long an entry waits before it becomes part of the record.
     *
     *  Nothing is ever removed from a chain -- there is no delete, by design,
     *  and the postcondition on Append proves every earlier entry is
     *  unchanged. So an entry a guard can take back is one that has not been
     *  written yet. It sits here, on this side of the record, and the record
     *  begins for it when it commits, the same as a guard deciding not to
     *  write a line on paper.
     *
     *  The time it happened is kept from the moment of the tap, so holding it
     *  does not move it: the record carries when it happened and when it was
     *  written down, and those being different is ordinary. */
    private static final long HOLD_MS = 2 * 60 * 1000L;

    /** How many entries may be held at once.
     *
     *  The window is the weak point of this whole idea and the cap is most of
     *  what makes it safe. A guard who can hold one entry for two minutes has
     *  a way to undo a fumble. A guard who can hold twelve until 05:00 has a
     *  way to decide, at the end of the night, which parts of it happened.
     *  Past three, the oldest is written before the newest is held. */
    private static final int MAX_HELD = 3;

    private static class Pending {
        boolean checkpoint;
        String label = "";
        String uid = "";
        int taps;
        int topic;
        String text = "";
        int occurred;
        long created;
    }

    private final ArrayList<Pending> pending = new ArrayList<Pending>();
    private final Handler ticker = new Handler();

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
        notes.addView(ghost("a note for the day crew",
                            new View.OnClickListener() {
            public void onClick(View v) {
                askNote(Core.TOPIC_FOR_DAY_CREW, "For the day crew",
                        "floodlight out over the east stack", "");
            }
        }));
        notes.addView(ghost("an incident", new View.OnClickListener() {
            public void onClick(View v) {
                askNote(Core.TOPIC_INCIDENT, "Incident",
                        "what happened, where, and what you did", "");
            }
        }));
        notes.addView(ghost("anything else", new View.OnClickListener() {
            public void onClick(View v) {
                askNote(Core.TOPIC_ROUTINE, "Note", "in your own words", "");
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

        loadPending();
        startShift();
        // Anything that survived a restart is written straight in rather than
        // resuming its window. It stops the entries being lost, which is what
        // the file is for, without letting a restart be a way to keep them
        // hanging: a held entry that outlives the app has stopped being a
        // fumble a guard is about to correct.
        commitAll();
        ticker.postDelayed(tick, 1000);
    }

    private final Runnable tick = new Runnable() {
        public void run() {
            commitDue();
            if (!pending.isEmpty()) {
                refresh();
            }
            ticker.postDelayed(this, 1000);
        }
    };

    /** Putting the phone away ends the window.
     *
     *  The point of holding an entry is the few seconds in which a guard
     *  realises they hit the wrong tile. Once the app is off the screen that
     *  moment is over, and anything still held is just an entry waiting to be
     *  chosen or discarded later. So it gets written. */
    @Override
    protected void onPause() {
        super.onPause();
        commitAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ticker.removeCallbacks(tick);
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
        // Held entries below the written ones, looking different, because
        // the difference between them is the whole point: one lot is the
        // record and the other is not yet anything.
        for (int i = 0; i < pending.size(); i++) {
            tonight.addView(pendingRow(pending.get(i)));
        }
        shown += pending.size();
        boolean any = shown > 0;
        tonight.setVisibility(any ? View.VISIBLE : View.GONE);
        tonightTitle.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    /** A held entry: what it will say, and that it can still be taken back.
     *
     *  Dragged sideways past a threshold it is discarded, and nothing about
     *  it ever reaches the record. Held to a threshold rather than any
     *  movement because a guard scrolling a list with cold hands should not
     *  be able to lose an entry by brushing it. */
    private TextView pendingRow(final Pending p) {
        final TextView t = new TextView(this);
        long left = HOLD_MS - (SystemClock.elapsedRealtime() - p.created);
        int secs = (int) Math.max(0, (left + 999) / 1000);
        t.setText(clock(p.occurred) + "  "
                  + (p.checkpoint ? p.label : p.text)
                  + "\nheld for " + secs
                  + "s  ·  swipe to take it back");
        t.setTextSize(14);
        t.setTextColor(MUTED);
        t.setPadding(dp(14), dp(11), dp(14), dp(11));
        t.setBackground(outlined(LINE, dp(10)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6);
        t.setLayoutParams(lp);

        t.setOnTouchListener(new View.OnTouchListener() {
            private float from;
            private boolean dragging;

            public boolean onTouch(View v, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    from = e.getRawX();
                    dragging = true;
                    return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (dragging) {
                        float dx = e.getRawX() - from;
                        v.setTranslationX(dx);
                        v.setAlpha(Math.max(0.25f,
                                   1f - Math.abs(dx) / (dp(200) * 1f)));
                    }
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    float dx2 = e.getRawX() - from;
                    dragging = false;
                    if (Math.abs(dx2) > dp(110)) {
                        // Posted, not called. Taking it back rebuilds this
                        // list, and removing a view while the framework is
                        // still dispatching touch to that very view leaves a
                        // hole in the parent's children and brings the app
                        // down on the next layout pass. It did, twice.
                        v.post(new Runnable() {
                            public void run() { takeBack(p); }
                        });
                    } else {
                        v.setTranslationX(0f);
                        v.setAlpha(1f);
                    }
                    return true;
                default:
                    return false;
                }
            }
        });
        return t;
    }

    /** Discarded before it was ever a record, which is the only kind of
     *  taking back this product has. */
    private void takeBack(Pending p) {
        pending.remove(p);
        savePending();
        banner.setText("taken back, and never written to the record");
        banner.setVisibility(View.VISIBLE);
        refresh();
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
        Pending p = new Pending();
        p.checkpoint = true;
        p.label = name;
        p.uid = uid;
        p.taps = ++taps;
        p.occurred = nowMinutes();
        p.created = SystemClock.elapsedRealtime();
        hold(p);
    }

    private void note(int topic, String text) {
        Pending p = new Pending();
        p.checkpoint = false;
        p.topic = topic;
        p.text = text;
        p.occurred = nowMinutes();
        p.created = SystemClock.elapsedRealtime();
        hold(p);
    }

    /** The guard writes the note.
     *
     *  These were canned examples that wrote the same sentence every time,
     *  which was fine for showing the record working and no use to anybody on
     *  a site. The example is the hint now, so the field starts empty and
     *  what goes into the record is what the guard actually typed.
     *
     *  The field takes line breaks even though rule 13 refuses them. Hiding
     *  the possibility would hide the rule; losing what they typed would be
     *  worse than either, so the text comes back in the box to be fixed. */
    private void askNote(final int topic, final String title, String hint,
                         String prefill) {
        final EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(prefill);
        field.setTextColor(PALE);
        field.setHintTextColor(QUIET);
        field.setInputType(InputType.TYPE_CLASS_TEXT
                           | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                           | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        field.setMinLines(3);
        field.setGravity(Gravity.TOP);
        field.setPadding(dp(20), dp(12), dp(20), dp(12));
        field.setSelection(prefill.length());

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(field)
            .setNegativeButton("cancel", null)
            .setPositiveButton("add", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) {
                    String text = field.getText().toString().trim();
                    if (text.length() == 0) {
                        return;
                    }
                    if (!oneLine(text)) {
                        banner.setText("an entry is one line; "
                                       + "take out the line break");
                        banner.setVisibility(View.VISIBLE);
                        askNote(topic, title, "", text);
                        return;
                    }
                    note(topic, text);
                }
            })
            .show();
    }

    /** Asked here only so the typed text can be handed straight back to be
     *  fixed. The record still decides: this entry goes through the same
     *  core call every other one does, and would be refused there too. */
    private boolean oneLine(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < ' ' || c == 127) {
                return false;
            }
        }
        return true;
    }

    private void hold(Pending p) {
        pending.add(p);
        while (pending.size() > MAX_HELD) {
            commit(pending.remove(0));
        }
        savePending();
        banner.setVisibility(View.GONE);
        refresh();
    }

    /** Commit anything that has waited long enough. */
    private void commitDue() {
        long now = SystemClock.elapsedRealtime();
        boolean any = false;
        for (int i = 0; i < pending.size(); ) {
            Pending p = pending.get(i);
            if (now - p.created >= HOLD_MS) {
                pending.remove(i);
                commit(p);
                any = true;
            } else {
                i++;
            }
        }
        if (any) {
            savePending();
            refresh();
        }
    }

    /** Everything still waiting, now. Sealing has to do this first: a record
     *  sealed with entries still held on this side would drop them without a
     *  word, which is the failure this whole buffer exists to avoid. */
    private void commitAll() {
        while (!pending.isEmpty()) {
            commit(pending.remove(0));
        }
        savePending();
    }

    /** Recorded is the later of the clock and the last entry's time, because
     *  recorded times may not go backwards and a phone's clock can. The core
     *  exports last_recorded for exactly this. */
    private void commit(Pending p) {
        int t = Math.max(nowMinutes(), Core.lastRecorded());
        if (p.checkpoint) {
            answer(Core.addCheckpoint(p.occurred, t, p.label, p.uid, p.taps,
                                      Core.AUTH_CRYPTOGRAPHIC));
        } else {
            answer(Core.addNote(Core.KIND_OBSERVATION, p.topic,
                                p.occurred, t, p.text, 0));
        }
    }

    private void sealAndShow() {
        commitAll();
        int t = Math.max(nowMinutes(), Core.lastRecorded());
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

    /** Held entries outlive the app being killed.
     *
     *  Without this the buffer would trade one silent loss for another: a
     *  guard taps a checkpoint, the phone is killed inside the window, and a
     *  visit that really happened is gone with nothing to show it ever did.
     *
     *  Text last and its length in front of it, the same discipline the
     *  archive uses, so a note containing the separator reads back whole. */
    private void savePending() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < pending.size(); i++) {
            Pending p = pending.get(i);
            String body = p.checkpoint ? p.label : p.text;
            b.append(p.checkpoint ? 1 : 0).append('|')
             .append(p.topic).append('|')
             .append(p.taps).append('|')
             .append(p.occurred).append('|')
             .append(p.created).append('|')
             .append(p.uid).append('|')
             .append(body.length()).append('|')
             .append(body).append('\n');
        }
        try {
            FileOutputStream f = new FileOutputStream(
                    new File(getFilesDir(), "pending.txt"));
            f.write(b.toString().getBytes("UTF-8"));
            f.close();
        } catch (Exception e) {
            // Nothing useful to do: the entries are still held in memory.
        }
    }

    private void loadPending() {
        pending.clear();
        File in = new File(getFilesDir(), "pending.txt");
        if (!in.exists()) {
            return;
        }
        try {
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(new FileInputStream(in), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                Pending p = parsePending(line);
                if (p != null) {
                    pending.add(p);
                }
            }
            r.close();
        } catch (Exception e) {
            // A file that will not read is not worth guessing at.
        }
    }

    private Pending parsePending(String line) {
        try {
            int[] at = new int[7];
            int n = 0;
            for (int i = 0; i < line.length() && n < 7; i++) {
                if (line.charAt(i) == '|') {
                    at[n++] = i;
                }
            }
            if (n < 7) {
                return null;
            }
            Pending p = new Pending();
            p.checkpoint = line.charAt(0) == '1';
            p.topic = Integer.parseInt(line.substring(at[0] + 1, at[1]));
            p.taps = Integer.parseInt(line.substring(at[1] + 1, at[2]));
            p.occurred = Integer.parseInt(line.substring(at[2] + 1, at[3]));
            p.created = Long.parseLong(line.substring(at[3] + 1, at[4]));
            p.uid = line.substring(at[4] + 1, at[5]);
            int len = Integer.parseInt(line.substring(at[5] + 1, at[6]));
            if (at[6] + 1 + len > line.length()) {
                return null;
            }
            String body = line.substring(at[6] + 1, at[6] + 1 + len);
            if (p.checkpoint) {
                p.label = body;
            } else {
                p.text = body;
            }
            if (p.taps > taps) {
                taps = p.taps;
            }
            return p;
        } catch (Exception e) {
            return null;
        }
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
        int n = Core.entryCount() + pending.size();
        String held = pending.isEmpty() ? ""
            : " That includes " + pending.size()
              + (pending.size() == 1 ? " entry" : " entries")
              + " still held, which will be written in first.";
        new AlertDialog.Builder(this)
            .setTitle("Seal the record?")
            .setMessage("Sealing ends tonight's record with " + n
                        + (n == 1 ? " entry" : " entries")
                        + ". Nothing can be added to it afterwards." + held)
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
