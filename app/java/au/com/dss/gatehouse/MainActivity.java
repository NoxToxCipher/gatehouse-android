package au.com.dss.gatehouse;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.TimeZone;

/** A screen over the record core.
 *
 * Placeholder in almost every way that matters: the site and the guard are
 * hardcoded, a tap is a button rather than an NFC tag, and there is no
 * storage, so closing the app loses the night. What is not placeholder is the
 * record underneath. Every entry below goes through the same Ada library that
 * is proved and tested on the desktop, the rules that refuse an entry are its
 * rules, the sentence explaining a refusal is its sentence, and the handover
 * page is rendered by it rather than assembled here.
 */
public class MainActivity extends Activity {

    private static final int NAVY = 0xFF14213D;
    private static final int AMBER = 0xFFE8A33D;
    private static final int PALE = 0xFFDCE3F0;
    private static final int MUTED = 0xFF8FA0C4;
    private static final int PANEL = 0xFF1C2A4A;

    private TextView status;
    private TextView reason;
    private TextView page;
    private int taps = 100;

    /** Minutes since 1970-01-01 00:00 in the zone this site keeps, which is
     *  what the core means by a time. Local rather than UTC on purpose: see
     *  Gatehouse.Clock. */
    private static int nowMinutes() {
        long ms = System.currentTimeMillis();
        long local = ms + TimeZone.getDefault().getOffset(ms);
        return (int) (local / 60000L);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(NAVY);
        root.setPadding(dp(16), dp(20), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("Gatehouse");
        title.setTextColor(AMBER);
        title.setTextSize(22);
        root.addView(title);

        TextView site = new TextView(this);
        site.setText("Northgate Rise, stage 2");
        site.setTextColor(PALE);
        site.setTextSize(15);
        site.setPadding(0, dp(2), 0, 0);
        root.addView(site);

        status = new TextView(this);
        status.setTextColor(MUTED);
        status.setTextSize(12);
        status.setPadding(0, dp(6), 0, dp(12));
        root.addView(status);

        root.addView(button("tap gate A", new View.OnClickListener() {
            public void onClick(View v) { tap("gate A", "04A2B7C1D3E580"); }
        }));
        root.addView(button("tap compound", new View.OnClickListener() {
            public void onClick(View v) { tap("compound", "04B1C2D3E4F590"); }
        }));
        root.addView(button("note for the day crew", new View.OnClickListener() {
            public void onClick(View v) {
                int t = nowMinutes();
                check(Core.addNote(Core.KIND_OBSERVATION, Core.TOPIC_FOR_DAY_CREW,
                                   t, t, "floodlight out over the east stack", 0));
            }
        }));
        root.addView(button("try a note with a line break", new View.OnClickListener() {
            public void onClick(View v) {
                int t = nowMinutes();
                check(Core.addNote(Core.KIND_OBSERVATION, Core.TOPIC_ROUTINE,
                                   t, t, "vehicles:\nute, van", 0));
            }
        }));
        root.addView(button("seal and read the handover", new View.OnClickListener() {
            public void onClick(View v) { sealAndShow(); }
        }));

        reason = new TextView(this);
        reason.setTextColor(AMBER);
        reason.setTextSize(13);
        reason.setPadding(dp(2), dp(8), dp(2), dp(8));
        root.addView(reason);

        page = new TextView(this);
        page.setTextColor(PALE);
        page.setTextSize(10);
        page.setTypeface(Typeface.MONOSPACE);
        page.setBackgroundColor(PANEL);
        page.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(page);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(NAVY);
        scroll.addView(root);
        setContentView(scroll);

        startShift();
    }

    private Button button(String label, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(NAVY);
        b.setBackgroundColor(AMBER);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private void startShift() {
        int t = nowMinutes();
        Core.siteBegin("Northgate Rise, stage 2");
        Core.siteAddPoint("gate A", "04A2B7C1D3E580");
        Core.siteAddPoint("compound", "04B1C2D3E4F590");
        Core.siteAddPoint("east fence", "04C9D8E7F6A5B4");
        Core.siteAddPoint("crane base", "04D3E2F1A0B9C8");
        Core.sitePolicy(1, 240, 0);
        Core.setAttribution(Core.DEVICE_PERSONAL, Core.METHOD_SESSION);
        Core.setGuard("g-kelso", "R. Kelso", "SAMPLE-LIC", "typed", "");
        check(Core.openShift(Core.genesis(), Core.siteHash(), t, t,
                             "on site, handover from day crew taken"));
        page.setText("The record is open. Tap, write, then seal, and the "
                     + "handover page appears here.\n\nentry encoding v"
                     + Core.encodingVersion() + "   archive format v"
                     + Core.archiveVersion());
    }

    private void tap(String label, String uid) {
        int t = nowMinutes();
        taps++;
        check(Core.addCheckpoint(t, t, label, uid, taps, Core.AUTH_CRYPTOGRAPHIC));
    }

    private void sealAndShow() {
        int t = nowMinutes();
        int r = Core.seal(t, t, "off site");
        check(r);
        String text = Core.report(t - 840, t);
        if (text.length() > 0) {
            page.setText(text);
        }
    }

    /** The core answered. If it refused, show its words, not ours. */
    private void check(int result) {
        String why = Core.lastReason();
        if (result == Core.OK) {
            reason.setText("");
        } else if (why.length() > 0) {
            reason.setText(why);
        } else {
            reason.setText("refused, status " + result);
        }
        refresh();
    }

    private void refresh() {
        status.setText(Core.entryCount() + " entries"
                       + (Core.isSealed() == 1 ? " · sealed" : " · open")
                       + (Core.verified() == 1 ? " · verifies" : " · BROKEN"));
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
