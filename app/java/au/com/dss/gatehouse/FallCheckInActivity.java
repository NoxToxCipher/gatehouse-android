package au.com.dss.gatehouse;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * The question asked after a hard fall, over the lock screen, at full
 * volume: are you all right? One tap answers it. Thirty seconds without
 * one texts the control pair, and the screen then says so and offers 000.
 */
public class FallCheckInActivity extends Activity {
    static final String EXTRA_IMPACT_AT = "impact_at";
    static final String EXTRA_DETAIL = "detail";
    static final String EXTRA_TEST = "test";
    static final int COUNTDOWN_S = 30;

    private static final int BG = 0xFF000000;
    private static final int PANEL = 0xFF0F1216;
    private static final int PALE = 0xFFE8E4DA;
    private static final int MUTED = 0xFF8A9099;
    private static final int BRASS = GatehouseNotify.BRASS;
    private static final int CRIMSON = GatehouseNotify.CRIMSON;

    private final Handler handler = new Handler();
    private long impactAt;
    private String detail = "";
    private boolean test;
    private long shownAt;
    private boolean answered, escalated;

    private TextView eyebrow, title, body, count, primary, secondary;
    private MediaPlayer player;
    private AudioManager audio;
    private int savedAlarmVolume = -1;
    private Vibrator vibrator;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (answered || escalated) return;
            long left = COUNTDOWN_S - (SystemClock.elapsedRealtime() - shownAt) / 1000L;
            if (left <= 0) { escalate(); return; }
            count.setText(String.valueOf(left));
            count.setTextColor(left <= 10 ? CRIMSON : BRASS);
            handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Intent in = getIntent();
        impactAt = in.getLongExtra(EXTRA_IMPACT_AT, System.currentTimeMillis());
        detail = in.getStringExtra(EXTRA_DETAIL) != null ? in.getStringExtra(EXTRA_DETAIL) : "";
        test = in.getBooleanExtra(EXTRA_TEST, false);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(FallDetectionService.NOTIF_CHECKIN);

        setContentView(buildScreen());
        shownAt = SystemClock.elapsedRealtime();
        startAlarm();
        handler.post(tick);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopAlarm();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // The big button is the answer. Back is not.
        if (answered || escalated) super.onBackPressed();
    }

    // ------------------------------------------------------------- screen

    private View buildScreen() {
        String tag = MainActivity.getHutPhoneHardwareTag();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(BG);
        col.setPadding(dp(28), dp(56), dp(28), dp(32));
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        eyebrow = label((test ? "TEST CHECK-IN · " : "FALL DETECTED · ") + tag.toUpperCase(Locale.US), 10f, MUTED, Fonts.mono(this, true));
        eyebrow.setLetterSpacing(0.14f);
        col.addView(eyebrow);

        title = label("Are you all right?", 34f, PALE, Fonts.display(this, true));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = params();
        tlp.topMargin = dp(18);
        title.setLayoutParams(tlp);
        col.addView(title);

        body = label(tag + " felt a hard fall at " + FallDetection.clock(impactAt) + ".", 16f, MUTED, Fonts.text(this, 400));
        body.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams blp = params();
        blp.topMargin = dp(10);
        body.setLayoutParams(blp);
        col.addView(body);

        count = label(String.valueOf(COUNTDOWN_S), 96f, BRASS, Fonts.display(this, true));
        count.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clp = params();
        clp.topMargin = dp(28);
        clp.bottomMargin = dp(8);
        count.setLayoutParams(clp);
        col.addView(count);

        TextView hint = label("Tap below within " + COUNTDOWN_S + " seconds, or Petrea and Lochran are texted.", 14f, MUTED, Fonts.text(this, 400));
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hlp = params();
        hlp.bottomMargin = dp(28);
        hint.setLayoutParams(hlp);
        col.addView(hint);

        primary = button("I'm all right", BRASS, BG, true);
        primary.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { answer(); }
        });
        col.addView(primary);

        secondary = button("Call 000", PANEL, PALE, false);
        LinearLayout.LayoutParams slp = params();
        slp.topMargin = dp(12);
        secondary.setLayoutParams(slp);
        secondary.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dial000(); }
        });
        col.addView(secondary);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        sv.addView(col);
        return sv;
    }

    private TextView label(String text, float sp, int color, Typeface face) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(face);
        t.setLayoutParams(params());
        return t;
    }

    private TextView button(String text, int fill, int ink, boolean filled) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f);
        b.setTextColor(ink);
        b.setTypeface(Fonts.text(this, 600));
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(20), dp(22), dp(20), dp(22));
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(14));
        d.setColor(fill);
        if (!filled) d.setStroke(dp(1), 0x33E8E4DA);
        b.setBackground(d);
        b.setLayoutParams(params());
        b.setClickable(true);
        return b;
    }

    private LinearLayout.LayoutParams params() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------- outcomes

    private void answer() {
        if (answered) return;
        answered = true;
        stopAlarm();
        long secs = (SystemClock.elapsedRealtime() - shownAt) / 1000L;
        if (escalated) {
            String status = AlertDispatcher.sendFallAllClear(this, FallDetection.officerName(this),
                    FallDetection.clock(impactAt), test);
            FallDetection.queueForRecord(this, (test ? "[FALL TEST] " : "[FALL] ")
                    + "Officer answered " + secs + " s after the check-in; all-clear texted (" + status + ")");
        } else {
            FallDetection.queueForRecord(this, (test ? "[FALL TEST] " : "[FALL] ")
                    + "Check-in answered in " + secs + " s; no text sent. " + detail);
        }
        finish();
    }

    private void escalate() {
        if (escalated) return;
        escalated = true;
        String status = AlertDispatcher.sendFallAlert(this, FallDetection.officerName(this),
                MainActivity.getHutPhoneHardwareTag(), FallDetection.clock(impactAt), COUNTDOWN_S,
                FallDetection.locationLink(), FallDetection.locationNote(),
                FallDetection.otherGuardOnSite(this), test);
        FallDetection.queueForRecord(this, (test ? "[FALL TEST] " : "[FALL] ")
                + "Check-in unanswered after " + COUNTDOWN_S + " s; control texted (" + status + "). " + detail);

        title.setText("Control has been texted");
        body.setText("Petrea and Lochran were texted at " + FallDetection.clock(System.currentTimeMillis())
                + ". Ring 000 if you need help.");
        count.setText("");
        count.setVisibility(View.GONE);
        primary.setText("I'm all right now");
        // The alarm keeps sounding until someone answers; that is the point of it.
    }

    private void dial000() {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:000")));
        } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------- alarm

    private void startAlarm() {
        try {
            audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                savedAlarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM);
                try {
                    audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
                } catch (Throwable ignored) {}
            }
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, GatehouseSounds.alert(this));
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Throwable t) {
            player = null;
        }
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 400, 300, 400, 900}, 0));
        } catch (Throwable ignored) {}
    }

    private void stopAlarm() {
        try { if (player != null) { player.stop(); player.release(); } } catch (Throwable ignored) {}
        player = null;
        try { if (vibrator != null) vibrator.cancel(); } catch (Throwable ignored) {}
        try {
            if (audio != null && savedAlarmVolume >= 0) audio.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0);
        } catch (Throwable ignored) {}
        savedAlarmVolume = -1;
    }
}
