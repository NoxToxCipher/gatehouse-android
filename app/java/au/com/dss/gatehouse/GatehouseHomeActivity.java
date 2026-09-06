package au.com.dss.gatehouse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The hut phone's home screen: a phone first, a gatehouse underneath.
 *
 * The ordinary dock sits where every phone keeps it, so whoever is handed
 * the phone can dial. The shift sits in a small dial beside the clock; the
 * gatehouse is one row; a live stand-down pins to the top and cannot be
 * covered. A long press anywhere opens everything else. In landscape, on
 * the hut tablet, the clock and the rows sit side by side.
 */
public class GatehouseHomeActivity extends Activity {
    private static final int BG = 0xFF000000;
    private static final int PANEL = 0xFF0F1216;
    private static final int POD = 0xFF1A1F26;
    private static final int HAIR = 0x22E8E4DA;
    private static final int PALE = 0xFFE8E4DA;
    private static final int MUTED = 0xFF8A9099;
    private static final int QUIET = 0xFF5C636C;
    private static final int BRASS = 0xFFE5A93C;
    private static final int GREEN = 0xFF22C55E;
    private static final int CRIMSON = 0xFFE5484D;

    private final Handler ticker = new Handler();
    private FrameLayout root;
    private FrameLayout drawer;
    private TextView stripLeft, stripTemp, stripRight, timeView, zoneView;
    private View stripDot;
    private TextView statA, statB, statC, siteName, siteLine;
    private DialView dial;
    private FrameLayout alertSlot;
    private boolean landscape;
    /** Landscape on a phone: wide and shallow, so the console is compact and the shelf lives in the drawer. */
    private boolean compact;
    private BoardView board;
    private RosterProvider.Result roster;
    private long rosterLoadedAt;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            ticker.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(BG));
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        ticker.removeCallbacks(tick);
        ticker.postDelayed(tick, 1000);
    }

    @Override
    protected void onPause() {
        ticker.removeCallbacks(tick);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        closeDrawer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        build();
        refresh();
    }

    @Override
    public void onBackPressed() {
        // A home screen has nowhere to go back to.
        if (drawer != null) closeDrawer();
    }

    // ------------------------------------------------------------- build

    private void build() {
        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        compact = landscape && getResources().getConfiguration().smallestScreenWidthDp < 600;
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) { openDrawer(); return true; }
        });
        // Keep clear of the status bar and the navigation bar on every version.
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int l, t, r, b;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                    l = bars.left; t = bars.top; r = bars.right; b = bars.bottom;
                } else {
                    l = insets.getSystemWindowInsetLeft(); t = insets.getSystemWindowInsetTop();
                    r = insets.getSystemWindowInsetRight(); b = insets.getSystemWindowInsetBottom();
                }
                v.setPadding(l, t, r, b);
                return insets;
            }
        });

        // status strip
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        stripLeft = label("All clear", 13f, PALE, Fonts.text(this, 600));
        stripTemp = label("", 13f, MUTED, Fonts.text(this, 400));
        stripRight = label("", 11.5f, QUIET, Fonts.mono(this, false));
        stripDot = new View(this);
        stripDot.setBackground(circle(GREEN));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(7), dp(7));
        dlp.rightMargin = dp(8);
        strip.addView(stripDot, dlp);
        strip.addView(stripLeft);
        LinearLayout.LayoutParams tlp = wrap();
        tlp.leftMargin = dp(8);
        tlp.weight = 1f;
        tlp.width = 0;
        strip.addView(stripTemp, tlp);
        strip.addView(stripRight);

        // clock row: the dial beside the time
        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.HORIZONTAL);
        clock.setGravity(Gravity.CENTER_VERTICAL);
        dial = new DialView(this);
        int dialPx = dp(compact ? 118 : (landscape ? 190 : 108));
        LinearLayout.LayoutParams dialLp = new LinearLayout.LayoutParams(dialPx, dialPx);
        dialLp.rightMargin = dp(compact ? 18 : (landscape ? 28 : 20));
        clock.addView(dial, dialLp);
        LinearLayout timeCol = new LinearLayout(this);
        timeCol.setOrientation(LinearLayout.VERTICAL);
        timeView = label("00:00", compact ? 58f : (landscape ? 92f : 54f), PALE, Fonts.display(this, true));
        timeView.setLetterSpacing(-0.02f);
        zoneView = label("", compact ? 10f : (landscape ? 11.5f : 10f), QUIET, Fonts.mono(this, false));
        zoneView.setLetterSpacing(0.14f);
        LinearLayout.LayoutParams zlp = wrap();
        zlp.topMargin = dp(8);
        timeCol.addView(timeView);
        timeCol.addView(zoneView, zlp);
        clock.addView(timeCol);

        // stats on a hairline
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, dp(compact ? 10 : 14), 0, 0);
        stats.setBackground(topHairline());
        statA = stat(stats, "SHIFT");
        statB = stat(stats, "WELFARE");
        statC = stat(stats, "FIRST LIGHT");

        // site
        siteName = label("Hume Doors & Timber, Kingston", compact ? 13f : 14f, PALE, Fonts.display(this, true));
        siteLine = label("", 11f, MUTED, Fonts.mono(this, false));
        if (compact) {
            siteLine.setSingleLine(true);
            siteLine.setEllipsize(android.text.TextUtils.TruncateAt.END);
        }

        // the gatehouse, one row
        LinearLayout gh = new LinearLayout(this);
        gh.setOrientation(LinearLayout.VERTICAL);
        gh.setBackground(outline(16));
        gh.setPadding(dp(6), dp(10), dp(6), dp(8));
        TextView ghK = label("GATEHOUSE", 9f, QUIET, Fonts.mono(this, false));
        ghK.setLetterSpacing(0.14f);
        ghK.setPadding(dp(8), 0, dp(8), dp(8));
        if (!landscape) gh.addView(ghK);
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.addView(tab(GlyphView.SHIELD, "PATROL", new Runnable() { public void run() { openTab(0); } }));
        tabs.addView(tab(GlyphView.CALENDAR, "ROSTER", new Runnable() { public void run() { openRoster(); } }));
        tabs.addView(tab(GlyphView.PEOPLE, "CONTACTS", new Runnable() { public void run() { openTab(1); } }));
        tabs.addView(tab(GlyphView.SLIDERS, "TOOLS", new Runnable() { public void run() { openTab(2); } }));
        gh.addView(tabs);

        alertSlot = new FrameLayout(this);

        // shelf and dock
        LinearLayout shelf = new LinearLayout(this);
        shelf.setOrientation(LinearLayout.HORIZONTAL);
        shelf.setPadding(dp(6), 0, dp(6), 0);
        int shelfPod = landscape && !compact ? 54 : 42;
        addPod(shelf, GlyphView.PLAY, "YOUTUBE", shelfPod, POD_SHELF, firstOf(launchAny("com.google.android.youtube", "app.revanced.android.youtube"),
                view("https://www.youtube.com")), false);
        addPod(shelf, GlyphView.NEWS, "NEWS", shelfPod, POD_SHELF, newsIntent(), false);
        addPod(shelf, GlyphView.PIN, "MAPS", shelfPod, POD_SHELF, firstOf(launchAny("com.google.android.apps.maps"), selector(Intent.CATEGORY_APP_MAPS),
                resolves(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:-27.6534,153.1165"))) ? new Intent(Intent.ACTION_VIEW, Uri.parse("geo:-27.6534,153.1165")) : null), false);
        addPod(shelf, GlyphView.GALLERY, "GALLERY", shelfPod, POD_SHELF, firstOf(selector(Intent.CATEGORY_APP_GALLERY),
                launchAny("com.sec.android.gallery3d", "com.google.android.apps.photos")), false);

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setBackground(panel(26));
        dock.setPadding(dp(8), dp(12), dp(8), dp(12));
        int dockPod = compact ? 44 : 54;
        addPod(dock, GlyphView.PHONE, "PHONE", dockPod, POD, new Intent(Intent.ACTION_DIAL), true);
        addPod(dock, GlyphView.BUBBLE, "MESSAGES", dockPod, POD, firstOf(smsApp(), selector(Intent.CATEGORY_APP_MESSAGING)), true);
        addPod(dock, GlyphView.CAMERA, "CAMERA", dockPod, POD, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), true);
        Intent browser = firstOf(launchAny("com.android.chrome", "com.sec.android.app.sbrowser", "org.mozilla.firefox"),
                selector(Intent.CATEGORY_APP_BROWSER), view("https://www.abc.net.au"));
        addPod(dock, GlyphView.GLOBE, browser != null && "com.android.chrome".equals(browser.getPackage()) ? "CHROME" : "BROWSER", dockPod, POD, browser, true);

        TextView hint = label("HOLD ANYWHERE FOR EVERYTHING ELSE", 9f, QUIET, Fonts.mono(this, false));
        hint.setLetterSpacing(0.12f);
        hint.setGravity(Gravity.CENTER);

        int padH = dp(landscape ? 36 : 22);
        if (!landscape) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(padH, dp(14), padH, dp(10));
            col.addView(strip, top(0));
            col.addView(alertSlot, top(14));
            col.addView(clock, top(28));
            col.addView(stats, top(20));
            col.addView(siteName, top(14));
            col.addView(siteLine, top(4));
            col.addView(gh, top(18));
            board = new BoardView(this);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            blp.topMargin = dp(10);
            blp.bottomMargin = dp(4);
            col.addView(board, blp);
            col.addView(shelf, top(8));
            col.addView(dock, top(14));
            col.addView(hint, top(10));
            root.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            // A wall instrument: one status line across the top, the clock and dial
            // anchoring the left, the roster as a panel on the right, and one console
            // bar across the bottom. A live stand-down spans both columns.
            int gutter = dp(compact ? 18 : 28);
            if (compact) padH = dp(16);
            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            page.setPadding(padH, dp(compact ? 6 : 14), padH, dp(compact ? 6 : 14));
            page.addView(strip, top(0));
            page.addView(alertSlot, top(12));

            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(clock, top(compact ? 6 : 22));
            left.addView(stats, top(compact ? 10 : 26));
            left.addView(siteName, top(compact ? 8 : 18));
            left.addView(siteLine, top(4));
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.42f);
            llp.rightMargin = gutter;
            body.addView(left, llp);

            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setBackground(outline(16));
            panel.setPadding(dp(6), dp(4), dp(6), dp(4));
            board = new BoardView(this);
            panel.addView(board, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.58f);
            plp.topMargin = dp(compact ? 10 : 22);
            body.addView(panel, plp);
            page.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            // the console bar: gatehouse, shelf, dock, on hairlines
            LinearLayout bar = new LinearLayout(this);
            bar.setOrientation(LinearLayout.HORIZONTAL);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setBackground(panel(26));
            bar.setPadding(dp(10), dp(compact ? 6 : 10), dp(10), dp(compact ? 6 : 10));
            gh.setBackground(null);
            gh.setPadding(dp(4), 0, dp(4), 0);
            shelf.setPadding(0, 0, 0, 0);
            dock.setBackground(null);
            dock.setPadding(0, 0, 0, 0);
            bar.addView(gh, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.05f));
            bar.addView(vrule(), new LinearLayout.LayoutParams(dp(1), dp(44)));
            if (!compact) {
                // On a phone sideways the shelf lives in the drawer; the bar keeps the gatehouse and the dock.
                bar.addView(shelf, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                bar.addView(vrule(), new LinearLayout.LayoutParams(dp(1), dp(44)));
            }
            bar.addView(dock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));
            page.addView(bar, top(compact ? 10 : 22));
            if (!compact) page.addView(hint, top(10));
            root.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        setContentView(root);
        drawer = null;
    }

    // ------------------------------------------------------------- refresh

    private void refresh() {
        Calendar now = Calendar.getInstance();
        timeView.setText(new SimpleDateFormat("HH:mm", Locale.US).format(now.getTime()));
        zoneView.setText((compact ? "AEST · " : "AEST · BRISBANE · ") + new SimpleDateFormat("EEE d MMM", Locale.US).format(now.getTime()).toUpperCase(Locale.US));

        // roster, re-read once a minute
        RosterProvider.Shift live = null, next = null;
        try {
            if (roster == null || System.currentTimeMillis() - rosterLoadedAt > 60_000L) {
                roster = Rostering.create(this).loadCachedResult();
                rosterLoadedAt = System.currentTimeMillis();
                if (board != null) board.setRoster(roster);
            }
            RosterProvider.Result r = roster;
            if (r != null && r.weekShifts != null) {
                List<RosterProvider.Shift> shifts = new ArrayList<>(r.weekShifts);
                Collections.sort(shifts, new Comparator<RosterProvider.Shift>() {
                    @Override public int compare(RosterProvider.Shift a, RosterProvider.Shift b) { return Long.compare(a.startTs, b.startTs); }
                });
                long nowSec = System.currentTimeMillis() / 1000L;
                for (RosterProvider.Shift s : shifts) {
                    if (s == null || s.startTs <= 0) continue;
                    if (s.startTs <= nowSec && nowSec < s.endTs) { if (live == null) live = s; }
                    else if (s.startTs > nowSec && next == null) next = s;
                }
            }
        } catch (Throwable ignored) {}

        String tag = "";
        try { tag = MainActivity.getHutPhoneHardwareTag(); } catch (Throwable ignored) {}
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.US);
        if (live != null) {
            long total = Math.max(1, live.endTs - live.startTs);
            float progress = Math.min(1f, Math.max(0f, (System.currentTimeMillis() / 1000L - live.startTs) / (float) total));
            dial.set(progress, true, "SHIFT", Math.round(progress * 100) + "%",
                    hm.format(new Date(live.startTs * 1000L)).substring(0, 2) + " – " + hm.format(new Date(live.endTs * 1000L)).substring(0, 2));
            statA.setText(Math.round(progress * 100) + "%");
            siteLine.setText(live.guardName + (compact ? "" : " · " + tag) + " · until " + hm.format(new Date(live.endTs * 1000L)));
        } else if (next != null) {
            dial.set(0f, false, "NEXT SHIFT", hm.format(new Date(next.startTs * 1000L)), initials(next.guardName));
            statA.setText("—");
            siteLine.setText("No guard rostered · the gatehouse rests until " + hm.format(new Date(next.startTs * 1000L)) + " · " + tag);
        } else {
            dial.set(0f, false, "NO ROSTER", "", "");
            statA.setText("—");
            siteLine.setText("No roster on this device · " + tag);
        }

        long welfare = Launcher.welfareMinutesLeft(this);
        statB.setText(welfare < 0 ? "—" : welfare + "m");
        statC.setText(Launcher.firstLight(now));

        // the strip
        boolean standDown = FireRadarManager.isStandDownActive(this);
        stripLeft.setText(standDown ? "Stand-down" : (live != null ? "All clear" : "Resting"));
        stripDot.setBackground(circle(standDown ? CRIMSON : (live != null ? GREEN : QUIET)));
        double t = Launcher.tempC(this);
        stripTemp.setText(Double.isNaN(t) ? "" : String.format(Locale.US, "%.1f°C", t));
        stripRight.setText(battery());

        // the pinned alert
        alertSlot.removeAllViews();
        if (standDown) alertSlot.addView(alertCard());
    }

    private String battery() {
        try {
            BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            int pct = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
            Intent st = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int status = st != null ? st.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            return (pct >= 0 ? pct + "%" : "") + (charging ? " · charging" : "");
        } catch (Throwable t) {
            return "";
        }
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "";
        String[] p = name.trim().split(" +");
        if (p.length == 1) return p[0].toUpperCase(Locale.US);
        return (p[0].substring(0, 1) + ". " + p[p.length - 1]).toUpperCase(Locale.US);
    }

    private View alertCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(16));
        d.setColor(BG);
        d.setStroke(dp(1), 0x80E5484D);
        card.setBackground(d);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        FireRadarManager.StandDown sd = FireRadarManager.standDown(this);
        TextView k = label("STAND-DOWN · LIGHTNING", 9.5f, CRIMSON, Fonts.mono(this, false));
        k.setLetterSpacing(0.14f);
        String headline = sd != null && sd.closestKm > 0
                ? String.format(Locale.US, "Lightning %.1f km %s", sd.closestKm, sd.direction)
                : "Lightning stand-down is live";
        String detail = "Stand down under hard cover until it clears."
                + (sd != null && sd.strikes > 0 ? " " + sd.strikes + " strikes within 10 km." : "")
                + " Petrea and Lochran have been texted.";
        TextView h = label(headline, 18f, PALE, Fonts.display(this, true));
        TextView b = label(detail, 13f, MUTED, Fonts.text(this, 400));
        LinearLayout actRow = new LinearLayout(this);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        String since = "";
        if (sd != null && sd.sinceMs > 0) {
            long mins = Math.max(0, (System.currentTimeMillis() - sd.sinceMs) / 60000L);
            since = "SINCE " + FallDetection.clock(sd.sinceMs) + " · " + (mins >= 60 ? (mins / 60) + " H " + (mins % 60) + " MIN" : mins + " MIN");
        }
        TextView sinceView = label(since, 11f, QUIET, Fonts.mono(this, false));
        TextView act = label("OPEN ›", 11f, PALE, Fonts.mono(this, true));
        act.setGravity(Gravity.END);
        actRow.addView(sinceView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actRow.addView(act);
        card.addView(k);
        card.addView(h, top(4));
        card.addView(b, top(4));
        card.addView(actRow, top(8));
        card.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { openTab(0); } });
        LinearLayout.LayoutParams lp = wrap();
        lp.bottomMargin = dp(landscape ? 14 : 0);
        card.setLayoutParams(lp);
        return card;
    }

    // ------------------------------------------------------------- launches

    private void openTab(int tab) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(Launcher.EXTRA_TAB, tab);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { startActivity(i); } catch (Throwable ignored) {}
    }

    private void openRoster() {
        Intent i = new Intent(this, MainActivity.class);
        i.setAction(DeputyNotifier.ACTION_OPEN_DEPUTY);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try { startActivity(i); } catch (Throwable ignored) {}
    }

    private Intent selector(String category) {
        Intent i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category);
        return resolves(i) ? i : null;
    }

    private Intent firstOf(Intent... options) {
        for (Intent i : options) if (i != null) return i;
        return null;
    }

    private Intent view(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        return resolves(i) ? i : null;
    }

    private Intent smsApp() {
        try {
            String pkg = Telephony.Sms.getDefaultSmsPackage(this);
            return pkg != null ? getPackageManager().getLaunchIntentForPackage(pkg) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private Intent launchAny(String... packages) {
        for (String p : packages) {
            Intent i = getPackageManager().getLaunchIntentForPackage(p);
            if (i != null) return i;
        }
        return null;
    }

    private Intent newsIntent() {
        Intent app = launchAny("au.net.abc.abcnews", "com.abc.abcnews", "com.google.android.apps.magazines", "au.com.news.dailytelegraph", "com.bbc.news");
        if (app != null) return app;
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.abc.net.au/news"));
        return resolves(web) ? web : null;
    }

    private boolean resolves(Intent i) {
        try {
            return i != null && getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private void launch(Intent i) {
        if (i == null) return;
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------- the drawer

    private void openDrawer() {
        if (drawer != null) return;
        drawer = new FrameLayout(this);
        drawer.setBackgroundColor(0x99000000);
        drawer.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { closeDrawer(); } });

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable d = new GradientDrawable();
        d.setColor(PANEL);
        float r = dp(26);
        d.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheet.setBackground(d);
        sheet.setPadding(dp(20), dp(12), dp(20), 0);
        sheet.setClickable(true);

        View grab = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(2));
        g.setColor(0x40E8E4DA);
        grab.setBackground(g);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(dp(40), dp(4));
        glp.gravity = Gravity.CENTER_HORIZONTAL;
        glp.bottomMargin = dp(14);
        sheet.addView(grab, glp);

        List<ResolveInfo> apps = new ArrayList<>();
        try {
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            apps.addAll(getPackageManager().queryIntentActivities(main, 0));
        } catch (Throwable ignored) {}
        final PackageManager pm = getPackageManager();
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm)));
            }
        });

        TextView k = label("EVERYTHING ELSE · " + apps.size() + " APPS", 9.5f, QUIET, Fonts.mono(this, false));
        k.setLetterSpacing(0.14f);
        sheet.addView(k);

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(6), 0, dp(24));
        for (final ResolveInfo ri : apps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(11), 0, dp(11));
            row.setBackground(bottomHairline());
            ImageView icon = new ImageView(this);
            try {
                Drawable ic = ri.loadIcon(pm);
                icon.setImageDrawable(ic);
            } catch (Throwable ignored) {}
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(24), dp(24));
            ilp.rightMargin = dp(14);
            row.addView(icon, ilp);
            TextView name = label(String.valueOf(ri.loadLabel(pm)), 14.5f, PALE, Fonts.text(this, 400));
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(name, nlp);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    closeDrawer();
                    Intent i = pm.getLaunchIntentForPackage(ri.activityInfo.packageName);
                    if (i == null) {
                        i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                                .setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                    }
                    launch(i);
                }
            });
            list.addView(row);
        }
        sv.addView(list);
        sheet.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        slp.topMargin = dp(landscape ? 40 : 110);
        if (landscape) { slp.leftMargin = dp(120); slp.rightMargin = dp(120); }
        drawer.addView(sheet, slp);
        root.addView(drawer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void closeDrawer() {
        if (drawer == null) return;
        root.removeView(drawer);
        drawer = null;
    }

    // ------------------------------------------------------------- pieces

    private static final int POD_SHELF = 0xFF0F1216;

    private void addPod(LinearLayout rowView, int glyph, String cap, int podDp, int podColor, final Intent target, boolean dock) {
        if (target == null) return;
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout pod = new FrameLayout(this);
        pod.setBackground(circle(landscape && podDp >= 44 ? POD : podColor));
        boolean big = dock || podDp >= 44;
        GlyphView g = new GlyphView(this, glyph, big ? PALE : MUTED, big ? 1.6f : 1.5f);
        int gs = dp(big ? 24 : 19);
        FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(gs, gs);
        glp.gravity = Gravity.CENTER;
        pod.addView(g, glp);
        item.addView(pod, new LinearLayout.LayoutParams(dp(podDp), dp(podDp)));
        TextView c = label(cap, big ? 9f : 8.5f, big ? MUTED : QUIET, Fonts.mono(this, false));
        c.setLetterSpacing(0.1f);
        c.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clp = wrap();
        clp.topMargin = dp(5);
        item.addView(c, clp);
        item.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { launch(target); } });
        rowView.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private View tab(int glyph, String cap, final Runnable go) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setPadding(dp(2), landscape ? 0 : dp(6), dp(2), landscape ? 0 : dp(6));
        GlyphView g = new GlyphView(this, glyph, landscape ? PALE : MUTED, 1.5f);
        if (landscape) {
            // In the console bar the gatehouse four sit in outlined pods, against the apps' filled ones.
            FrameLayout pod = new FrameLayout(this);
            GradientDrawable ring = new GradientDrawable();
            ring.setShape(GradientDrawable.OVAL);
            ring.setColor(BG);
            ring.setStroke(dp(1), HAIR);
            pod.setBackground(ring);
            FrameLayout.LayoutParams glp = new FrameLayout.LayoutParams(dp(22), dp(22));
            glp.gravity = Gravity.CENTER;
            pod.addView(g, glp);
            item.addView(pod, new LinearLayout.LayoutParams(dp(compact ? 44 : 54), dp(compact ? 44 : 54)));
        } else {
            item.addView(g, new LinearLayout.LayoutParams(dp(20), dp(20)));
        }
        TextView c = label(cap, 9f, MUTED, Fonts.mono(this, false));
        c.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams clp = wrap();
        clp.topMargin = dp(landscape ? 5 : 6);
        item.addView(c, clp);
        item.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { go.run(); } });
        item.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return item;
    }

    private TextView stat(LinearLayout rowView, String key) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView k = label(key, 9f, QUIET, Fonts.mono(this, false));
        k.setLetterSpacing(0.14f);
        TextView v = label("—", 16f, PALE, Fonts.mono(this, true));
        LinearLayout.LayoutParams vlp = wrap();
        vlp.topMargin = dp(5);
        col.addView(k);
        col.addView(v, vlp);
        rowView.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return v;
    }

    private TextView label(String text, float sp, int color, android.graphics.Typeface face) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(face);
        t.setLayoutParams(wrap());
        return t;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int dpTop) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(dpTop);
        return lp;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

    private GradientDrawable panel(int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(radiusDp));
        d.setColor(PANEL);
        return d;
    }

    private GradientDrawable outline(int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(radiusDp));
        d.setColor(BG);
        d.setStroke(dp(1), HAIR);
        return d;
    }

    private View vrule() {
        View v = new View(this);
        v.setBackgroundColor(HAIR);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(1), dp(44));
        lp.leftMargin = dp(8);
        lp.rightMargin = dp(8);
        v.setLayoutParams(lp);
        return v;
    }

    private Drawable topHairline() {
        return new HairlineDrawable(true, dp(1));
    }

    private Drawable bottomHairline() {
        return new HairlineDrawable(false, dp(1));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** A one-pixel rule along the top or bottom edge. */
    private static class HairlineDrawable extends Drawable {
        private final boolean top;
        private final int px;
        private final Paint p = new Paint();
        HairlineDrawable(boolean top, int px) { this.top = top; this.px = px; p.setColor(HAIR); }
        @Override public void draw(Canvas c) {
            android.graphics.Rect b = getBounds();
            if (top) c.drawRect(b.left, b.top, b.right, b.top + px, p);
            else c.drawRect(b.left, b.bottom - px, b.right, b.bottom, p);
        }
        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(android.graphics.ColorFilter cf) {}
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    /** The coming shifts on hairlines, drawn by the same painter as the widget. Tapping opens the roster. */
    private class BoardView extends View {
        private RosterProvider.Result data;
        BoardView(Context c) {
            super(c);
            setOnClickListener(new OnClickListener() { public void onClick(View v) { openRoster(); } });
        }
        void setRoster(RosterProvider.Result r) { data = r; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            if (getWidth() < 10 || getHeight() < 10) return;
            RosterBoard.draw(getContext(), c, getWidth(), getHeight(), data, false);
        }
    }

    /** The shift as a small dial: a hairline track, a brass arc, three lines in the middle. */
    private static class DialView extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF box = new RectF();
        private float progress;
        private boolean live;
        private String l1 = "", l2 = "", l3 = "";

        DialView(Context c) {
            super(c);
            float d = getResources().getDisplayMetrics().density;
            track.setStyle(Paint.Style.STROKE); track.setStrokeWidth(3 * d); track.setColor(0xFF1A1F26);
            arc.setStyle(Paint.Style.STROKE); arc.setStrokeWidth(3 * d); arc.setStrokeCap(Paint.Cap.ROUND); arc.setColor(BRASS);
            dot.setColor(0xFFF4E6C8);
            small.setTypeface(Fonts.mono(c, false)); small.setTextSize(9.5f * d); small.setLetterSpacing(0.12f); small.setColor(QUIET); small.setTextAlign(Paint.Align.CENTER);
            big.setTypeface(Fonts.mono(c, true)); big.setTextSize(14f * d); big.setColor(BRASS); big.setTextAlign(Paint.Align.CENTER);
        }

        void set(float progress, boolean live, String l1, String l2, String l3) {
            this.progress = progress; this.live = live; this.l1 = l1; this.l2 = l2; this.l3 = l3;
            big.setColor(live ? BRASS : MUTED);
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float d = getResources().getDisplayMetrics().density;
            float k = Math.max(1f, w / (108f * d));
            small.setTextSize(9.5f * d * k);
            big.setTextSize(14f * d * k);
            track.setStrokeWidth(3f * d * Math.min(k, 1.6f));
            arc.setStrokeWidth(3f * d * Math.min(k, 1.6f));
            float pad = track.getStrokeWidth() * 1.5f;
            box.set(pad, pad, w - pad, h - pad);
            c.drawArc(box, 135f, 270f, false, track);
            if (live && progress > 0f) {
                c.drawArc(box, 135f, 270f * progress, false, arc);
                double a = Math.toRadians(135f + 270f * progress);
                float r = (w - 2 * pad) / 2f;
                c.drawCircle(w / 2f + (float) Math.cos(a) * r, h / 2f + (float) Math.sin(a) * r, track.getStrokeWidth(), dot);
            }
            float cy = h / 2f;
            float lh = small.getTextSize() * 1.5f;
            c.drawText(l1, w / 2f, cy - lh * 0.55f, small);
            if (!l2.isEmpty()) c.drawText(l2, w / 2f, cy + big.getTextSize() * 0.55f, big);
            if (!l3.isEmpty()) c.drawText(l3, w / 2f, cy + big.getTextSize() * 0.55f + lh, small);
        }
    }
}
