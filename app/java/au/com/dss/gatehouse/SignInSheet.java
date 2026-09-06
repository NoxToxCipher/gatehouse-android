package au.com.dss.gatehouse;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The gate: who is on the phone, confirmed by a PIN, and on a guard's first
 * sign-in the theme they want to work in. Laid over the app in the current
 * theme's tokens, so a light-mode guard is not blinded by an obsidian sheet.
 * Sideways it is two columns: the greeting and the name on the left, the pad on
 * the right.
 */
final class SignInSheet extends FrameLayout {

    interface Host {
        String hutTag();
        String rosteredGuardName();
        void previewTheme(int themeId);
        void onSignedIn(SiteBook.Guard guard, boolean firstTime);
        void onSkipped();
    }

    /** The theme tokens the sheet paints with. */
    static final class Look {
        int bg, panel, pale, muted, quiet, accent, accentSoft, crimson, hair;
    }

    private static final int PIN_LENGTH = 4;
    private static final String[] THEME_NAMES = {"OLED Gold", "0-Lux Red", "NVG Green", "Cyber Violet", "Daylight", "Desert Sand"};
    private static final String[] THEME_KINDS = {"NIGHT · BLACK", "NIGHT · RED", "NIGHT · GREEN", "INDOOR", "DAY · WHITE", "DAY · LINEN"};

    private final Host host;
    private final Look look;
    private final SiteBook book;
    private final int[][] palettes;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final float density;
    private final boolean landscape;

    private SiteBook.Guard chosen;
    private final StringBuilder pin = new StringBuilder();
    private TextView greeting, nameView, notYou, feedback;
    private LinearLayout dotsRow, pad;
    private View[] dots;
    private TextView[] keys;
    private boolean busy;
    private int previewTheme = -1;

    SignInSheet(Context ctx, Host host, Look look, SiteBook book, int[][] palettes, int currentTheme) {
        super(ctx);
        this.host = host;
        this.look = look;
        this.book = book;
        this.palettes = palettes;
        this.previewTheme = currentTheme;
        density = getResources().getDisplayMetrics().density;
        landscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        setBackgroundColor(look.bg);
        setClickable(true);
        setFocusable(true);
        chosen = book.guardForName(host.rosteredGuardName());
        buildPinPage();
    }

    @Override public boolean onTouchEvent(MotionEvent e) { return true; }

    private int dp(int v) { return Math.round(v * density); }

    private LinearLayout.LayoutParams top(int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(marginDp);
        return lp;
    }

    private TextView label(String text, float sp, int color, android.graphics.Typeface face) {
        TextView t = new TextView(getContext());
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(face);
        t.setIncludeFontPadding(false);
        return t;
    }

    private TextView micro(String text, int color) {
        TextView t = label(text, 9.5f, color, Fonts.mono(getContext(), false));
        t.setLetterSpacing(0.14f);
        return t;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private StateListDrawable pressable(int radiusDp) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, rounded(look.accentSoft, look.accent, radiusDp));
        s.addState(new int[]{}, rounded(Color.TRANSPARENT, look.hair, radiusDp));
        return s;
    }

    // ------------------------------------------------------------------ the PIN page

    private void buildPinPage() {
        removeAllViews();
        pin.setLength(0);

        LinearLayout head = new LinearLayout(getContext());
        head.setOrientation(LinearLayout.VERTICAL);
        String tag = host.hutTag();
        head.addView(micro("GATEHOUSE" + (tag == null || tag.isEmpty() ? "" : " · " + tag.toUpperCase(Locale.US)), look.accent));
        if (!book.site.isEmpty()) head.addView(label(book.site, 12.5f, look.muted, Fonts.mono(getContext(), false)), top(6));

        greeting = label("", landscape ? 26f : 30f, look.pale, Fonts.display(getContext(), true));
        head.addView(greeting, top(landscape ? 16 : 28));

        LinearLayout nameRow = new LinearLayout(getContext());
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(Gravity.CENTER_VERTICAL);
        nameView = label("", 14f, look.pale, Fonts.text(getContext(), 500));
        nameView.setBackground(rounded(Color.TRANSPARENT, look.hair, 12));
        nameView.setPadding(dp(14), dp(8), dp(14), dp(8));
        notYou = label("Not you?", 12.5f, look.accent, Fonts.text(getContext(), 500));
        notYou.setPadding(dp(14), dp(8), dp(4), dp(8));
        nameRow.addView(nameView);
        nameRow.addView(notYou);
        head.addView(nameRow, top(14));
        View.OnClickListener pick = new View.OnClickListener() {
            public void onClick(View v) { buildNamePage(); }
        };
        nameView.setOnClickListener(pick);
        notYou.setOnClickListener(pick);

        LinearLayout entry = new LinearLayout(getContext());
        entry.setOrientation(LinearLayout.VERTICAL);
        entry.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView pinLabel = micro("PIN", look.quiet);
        pinLabel.setGravity(Gravity.CENTER);
        entry.addView(pinLabel, top(0));

        dotsRow = new LinearLayout(getContext());
        dotsRow.setOrientation(LinearLayout.HORIZONTAL);
        dotsRow.setGravity(Gravity.CENTER);
        dots = new View[PIN_LENGTH];
        for (int i = 0; i < PIN_LENGTH; i++) {
            View d = new View(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(12), dp(12));
            lp.setMargins(dp(7), 0, dp(7), 0);
            dotsRow.addView(d, lp);
            dots[i] = d;
        }
        entry.addView(dotsRow, top(10));

        feedback = label("", 12.5f, look.muted, Fonts.text(getContext(), 400));
        feedback.setGravity(Gravity.CENTER);
        feedback.setMinHeight(dp(20));
        entry.addView(feedback, top(10));

        pad = new LinearLayout(getContext());
        pad.setOrientation(LinearLayout.VERTICAL);
        keys = new TextView[12];
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"", "0", "⌫"}};
        int keyH = landscape ? 42 : 56;
        int idx = 0;
        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(getContext());
            r.setOrientation(LinearLayout.HORIZONTAL);
            for (final String k : row) {
                TextView key = label(k, k.equals("⌫") ? 18f : 22f, look.pale, Fonts.display(getContext(), true));
                key.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(keyH), 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                if (!k.isEmpty()) {
                    key.setBackground(pressable(14));
                    key.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) { onKey(k); }
                    });
                }
                r.addView(key, lp);
                keys[idx++] = key;
            }
            pad.addView(r);
        }
        entry.addView(pad, top(6));

        TextView skip = label("Continue without signing in", 12.5f, look.quiet, Fonts.text(getContext(), 400));
        skip.setGravity(Gravity.CENTER);
        skip.setPadding(dp(12), dp(12), dp(12), dp(12));
        skip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GuardSession.noteSkipped(getContext());
                host.onSkipped();
            }
        });

        int padH = dp(landscape ? 28 : 28);
        if (landscape) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(padH, dp(18), padH, dp(10));
            LinearLayout left = new LinearLayout(getContext());
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(head);
            left.addView(new View(getContext()), new LinearLayout.LayoutParams(0, 0, 1f));
            skip.setGravity(Gravity.START);
            skip.setPadding(0, dp(12), dp(12), dp(6));
            left.addView(skip);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.1f);
            llp.rightMargin = dp(28);
            row.addView(left, llp);
            LinearLayout right = new LinearLayout(getContext());
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.CENTER_VERTICAL);
            right.addView(entry);
            row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            addView(row, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            LinearLayout col = new LinearLayout(getContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(padH, dp(28), padH, dp(16));
            col.addView(head);
            col.addView(new View(getContext()), new LinearLayout.LayoutParams(0, 0, 1f));
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            col.addView(entry, elp);
            col.addView(skip, top(6));
            ScrollView sv = new ScrollView(getContext());
            sv.setFillViewport(true);
            sv.addView(col, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            addView(sv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        refreshChosen();
        refreshDots();
        refreshWait();
    }

    private void refreshChosen() {
        if (chosen != null) {
            greeting.setText(greetingFor(chosen.firstName()));
            nameView.setText(chosen.name);
            notYou.setVisibility(View.VISIBLE);
        } else {
            greeting.setText("Who is on the gate?");
            nameView.setText("Choose your name");
            notYou.setVisibility(View.GONE);
        }
    }

    private static String greetingFor(String first) {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String part = h < 5 ? "Good night" : h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
        return part + ", " + first + ".";
    }

    private void refreshDots() {
        for (int i = 0; i < PIN_LENGTH; i++) {
            boolean on = i < pin.length();
            dots[i].setBackground(rounded(on ? look.accent : Color.TRANSPARENT, on ? look.accent : look.quiet, 6));
        }
    }

    private void onKey(String k) {
        if (busy || GuardSession.waitMs(getContext()) > 0) return;
        if (chosen == null) { buildNamePage(); return; }
        if (k.equals("⌫")) {
            if (pin.length() > 0) pin.setLength(pin.length() - 1);
            feedback.setText("");
            refreshDots();
            return;
        }
        if (pin.length() >= PIN_LENGTH) return;
        pin.append(k);
        refreshDots();
        if (pin.length() == PIN_LENGTH) {
            busy = true;
            final String entered = pin.toString();
            handler.postDelayed(new Runnable() {
                public void run() { verify(entered); }
            }, 120);
        }
    }

    private void verify(String entered) {
        boolean ok = book.verifyPin(chosen, entered);
        busy = false;
        if (ok) {
            GuardSession.signIn(getContext(), chosen);
            boolean first = !GuardSession.themePicked(getContext(), chosen.key);
            if (first) buildThemePage();
            else host.onSignedIn(chosen, false);
            return;
        }
        pin.setLength(0);
        int left = GuardSession.noteFailure(getContext());
        refreshDots();
        ObjectAnimator shake = ObjectAnimator.ofFloat(dotsRow, "translationX", 0f, dp(10));
        shake.setDuration(420);
        shake.setInterpolator(new CycleInterpolator(3));
        shake.start();
        if (left > 0) {
            feedback.setTextColor(look.crimson);
            feedback.setText(left == 1 ? "That PIN doesn't match. One try left." : "That PIN doesn't match. " + left + " tries left.");
        } else {
            refreshWait();
        }
    }

    private void refreshWait() {
        long wait = GuardSession.waitMs(getContext());
        boolean locked = wait > 0;
        for (TextView k : keys) if (k != null) k.setAlpha(locked ? 0.35f : 1f);
        if (locked) {
            feedback.setTextColor(look.crimson);
            feedback.setText("Five wrong PINs. Wait " + ((wait + 999) / 1000) + " s.");
            handler.postDelayed(new Runnable() {
                public void run() { if (isAttachedToWindow()) refreshWait(); }
            }, 1000);
        } else if (feedback.getText().toString().startsWith("Five wrong")) {
            feedback.setText("");
        }
    }

    // ------------------------------------------------------------------ the name page

    private void buildNamePage() {
        removeAllViews();
        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(28);
        col.setPadding(padH, dp(28), padH, dp(24));
        col.addView(micro("GATEHOUSE · SIGN IN", look.accent));
        col.addView(label("Who is on the gate?", 26f, look.pale, Fonts.display(getContext(), true)), top(16));

        List<SiteBook.Guard> list = book.guardsBySurname();
        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        int cols = landscape ? 3 : 1;
        LinearLayout row = null;
        for (int i = 0; i < list.size(); i++) {
            final SiteBook.Guard g = list.get(i);
            if (i % cols == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, top(0));
            }
            LinearLayout item = new LinearLayout(getContext());
            item.setOrientation(LinearLayout.VERTICAL);
            item.setBackground(pressable(14));
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.addView(label(g.name, 15f, look.pale, Fonts.text(getContext(), 500)));
            String sub = !g.role.isEmpty() ? g.role : (g.licence.isEmpty() ? "Security officer" : "Licence " + g.licence);
            item.addView(label(sub, 11f, look.muted, Fonts.mono(getContext(), false)), top(3));
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chosen = g;
                    buildPinPage();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cols == 1 ? ViewGroup.LayoutParams.MATCH_PARENT : 0, ViewGroup.LayoutParams.WRAP_CONTENT, cols == 1 ? 0f : 1f);
            lp.setMargins(dp(cols == 1 ? 0 : 4), dp(4), dp(cols == 1 ? 0 : 4), dp(4));
            row.addView(item, lp);
        }
        if (row != null) {
            while (row.getChildCount() < cols) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 0, 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(new View(getContext()), lp);
            }
        }
        ScrollView sv = new ScrollView(getContext());
        sv.addView(grid);
        col.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        if (chosen != null) {
            TextView back = label("‹ Back", 13f, look.accent, Fonts.text(getContext(), 500));
            back.setPadding(dp(4), dp(12), dp(12), dp(4));
            back.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { buildPinPage(); }
            });
            col.addView(back, top(8));
        }
        addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // ------------------------------------------------------------------ the theme page

    private LinearLayout[] swatches;
    private boolean themeSeeded, relooking;

    private void buildThemePage() {
        removeAllViews();
        if (!themeSeeded) {
            themeSeeded = true;
            if (chosen.theme >= 0) previewTheme = chosen.theme;
        }
        if (!relooking) host.previewTheme(previewTheme);

        LinearLayout col = new LinearLayout(getContext());
        col.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(28);
        col.setPadding(padH, dp(28), padH, dp(20));
        col.addView(micro("WELCOME · " + chosen.name.toUpperCase(Locale.US), look.accent));
        col.addView(label("Pick the look you work in.", 26f, look.pale, Fonts.display(getContext(), true)), top(16));
        col.addView(label("This hut phone changes to it whenever you sign in. Settings can change it any time.", 13f, look.muted, Fonts.text(getContext(), 400)), top(8));

        LinearLayout grid = new LinearLayout(getContext());
        grid.setOrientation(LinearLayout.VERTICAL);
        int cols = landscape ? 3 : 2;
        swatches = new LinearLayout[palettes.length];
        LinearLayout row = null;
        for (int i = 0; i < palettes.length; i++) {
            if (i % cols == 0) {
                row = new LinearLayout(getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, top(0));
            }
            final int id = i;
            int[] pal = palettes[i];
            int bg = pal[17], panel = pal[0], accent = pal[5], pale = pal[8], muted = pal[9];
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            View dot = new View(getContext());
            dot.setBackground(rounded(accent, 0, 7));
            card.addView(dot, new LinearLayout.LayoutParams(dp(14), dp(14)));
            View strip = new View(getContext());
            strip.setBackground(rounded(panel, muted & 0x66FFFFFF, 4));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
            slp.topMargin = dp(10);
            card.addView(strip, slp);
            card.addView(label(THEME_NAMES[i], 14f, pale, Fonts.display(getContext(), true)), top(12));
            TextView kind = label(THEME_KINDS[i], 9.5f, muted, Fonts.mono(getContext(), false));
            kind.setLetterSpacing(0.14f);
            card.addView(kind, top(4));
            card.setTag(new int[]{bg, muted});
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    previewTheme = id;
                    host.previewTheme(id);
                    paintSwatches();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(card, lp);
            swatches[i] = card;
        }
        if (row != null) {
            while (row.getChildCount() < cols) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 0, 1f);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(new View(getContext()), lp);
            }
        }
        paintSwatches();
        ScrollView sv = new ScrollView(getContext());
        sv.addView(grid);
        col.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView keep = label("Keep this look", 15f, Color.BLACK, Fonts.text(getContext(), 600));
        keep.setGravity(Gravity.CENTER);
        keep.setPadding(dp(16), dp(14), dp(16), dp(14));
        keep.setBackground(rounded(look.accent, 0, 14));
        keep.setTextColor(luminance(look.accent) > 0.5f ? Color.BLACK : Color.WHITE);
        keep.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GuardSession.setThemeFor(getContext(), chosen.key, previewTheme);
                host.onSignedIn(chosen, true);
            }
        });
        col.addView(keep, top(12));
        addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void paintSwatches() {
        if (swatches == null) return;
        for (int i = 0; i < swatches.length; i++) {
            int[] c = (int[]) swatches[i].getTag();
            boolean on = i == previewTheme;
            GradientDrawable d = new GradientDrawable();
            d.setColor(c[0]);
            d.setCornerRadius(dp(16));
            d.setStroke(dp(on ? 2 : 1), on ? look.accent : (c[1] & 0x55FFFFFF));
            swatches[i].setBackground(d);
        }
    }

    private static float luminance(int c) {
        return (0.2126f * Color.red(c) + 0.7152f * Color.green(c) + 0.0722f * Color.blue(c)) / 255f;
    }

    /** The sheet repaints itself when the app's theme changes under it. */
    void relook(Look l) {
        look.bg = l.bg; look.panel = l.panel; look.pale = l.pale; look.muted = l.muted; look.quiet = l.quiet;
        look.accent = l.accent; look.accentSoft = l.accentSoft; look.crimson = l.crimson; look.hair = l.hair;
        setBackgroundColor(look.bg);
        if (swatches != null && !relooking) {
            relooking = true;
            try { buildThemePage(); } finally { relooking = false; }
        }
    }

    static String sinceLine(long sinceMs) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(sinceMs));
    }
}
