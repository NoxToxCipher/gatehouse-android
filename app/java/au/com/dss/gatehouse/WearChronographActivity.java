package au.com.dss.gatehouse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * WearChronographActivity — Screen 1: Clean Shift Chronograph Face for Wear OS.
 * Features vector 270° amber shift progress arc, live monospace digital clock,
 * AEST timezone, and seamless swipe navigation to companion watch tools.
 */
public class WearChronographActivity extends Activity {

    private ChronographWatchView watchView;
    private Handler clockHandler;
    private Runnable clockRunnable;
    private GestureDetector gestureDetector;

    // The same tokens the phone uses, so the watch is the same instrument.
    private static final int COL_ACCENT = 0xFFE5A93C;
    private static final int COL_EMERALD = 0xFF10B981;
    private static final int COL_AMBER = 0xFFF59E0B;
    private static final int COL_CRIMSON = 0xFFEF4444;
    private static final int COL_CYAN = 0xFF06B6D4;
    private static final int COL_LINE = 0xFF1E2B40;
    private static final int COL_LINE_SUBTLE = 0xFF121B28;
    private static final int COL_QUIET = 0xFF5B6B82;
    private static final int COL_MUTED = 0xFF94A3B8;
    private static final int COL_PALE = 0xFFF3F6FA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        watchView = new ChronographWatchView(this);
        root.addView(watchView);
        setContentView(root);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    float diffX = e2.getX() - e1.getX();
                    if (diffX < -80) { // Swipe Left -> Open Pressure Gauge
                        Intent intent = new Intent(WearChronographActivity.this, WearPressureGaugeActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        return true;
                    }
                }
                return false;
            }
        });

        watchView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        clockHandler = new Handler(Looper.getMainLooper());
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (watchView != null) watchView.invalidate();
                clockHandler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.post(clockRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }

    private class ChronographWatchView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Read once. Re-reading display metrics every frame stutters on real hardware.
        private final float density;

        private RosterProvider.Result roster;
        private long rosterLoadedAt;

        public ChronographWatchView(Context context) {
            super(context);
            density = getResources().getDisplayMetrics().density;

            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeCap(Paint.Cap.ROUND);
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeCap(Paint.Cap.ROUND);
            tickPaint.setStrokeCap(Paint.Cap.ROUND);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Fonts.mono(context, true));
            timePaint.setTextAlign(Paint.Align.CENTER);
            timePaint.setTypeface(Fonts.display(context, true));

            pipPaint.setStyle(Paint.Style.FILL);
            pipPaint.setColor(0xFFFFFFFF);
        }

        private float dpf(float v) {
            return v * density;
        }

        /** The live rostered shift, or null when nobody is on. Cached for a minute. */
        private RosterProvider.Shift liveShift() {
            try {
                if (roster == null || System.currentTimeMillis() - rosterLoadedAt > 60000L) {
                    roster = Rostering.create(getContext()).loadCachedResult();
                    rosterLoadedAt = System.currentTimeMillis();
                }
                if (roster != null && roster.weekShifts != null) {
                    long nowSec = System.currentTimeMillis() / 1000L;
                    for (RosterProvider.Shift s : roster.weekShifts) {
                        if (s == null || s.startTs <= 0) continue;
                        if (s.startTs <= nowSec && nowSec < s.endTs) return s;
                    }
                }
            } catch (Throwable ignored) {}
            return null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            float cx = w / 2f;
            float cy = h / 2f;
            float rOuter = Math.min(w, h) * 0.40f;
            float rInner = rOuter * 0.84f;

            RectF outerRect = new RectF(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter);
            RectF innerRect = new RectF(cx - rInner, cy - rInner, cx + rInner, cy + rInner);

            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.US);
            sdfTime.setTimeZone(TimeZone.getDefault());
            String timeStr = sdfTime.format(new Date(System.currentTimeMillis()));

            // Real rostered shift. With nobody on, the dial says so rather than
            // inventing progress.
            RosterProvider.Shift live = liveShift();
            boolean onShift = live != null;
            float shiftProgress = 0f;
            String startLabel = "";
            String endLabel = "";
            if (onShift) {
                long total = Math.max(1, live.endTs - live.startTs);
                shiftProgress = Math.min(1f, Math.max(0f,
                        (System.currentTimeMillis() / 1000L - live.startTs) / (float) total));
                SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.US);
                startLabel = hm.format(new Date(live.startTs * 1000L));
                endLabel = hm.format(new Date(live.endTs * 1000L));
            }

            // Outer track and notches
            trackPaint.setColor(COL_LINE);
            trackPaint.setStrokeWidth(dpf(6f));
            canvas.drawArc(outerRect, 135f, 270f, false, trackPaint);

            int tickCount = 12;
            for (int i = 0; i <= tickCount; i++) {
                double rad = Math.toRadians(135f + (i * 270f / (float) tickCount));
                boolean isMajor = (i % 3 == 0);
                boolean isEdge = (i == 0 || i == tickCount || i == tickCount / 2);
                float tLen = isMajor ? dpf(6f) : dpf(3.5f);
                float rIn = rOuter + dpf(5f);
                tickPaint.setColor(isEdge ? COL_ACCENT : COL_QUIET);
                tickPaint.setStrokeWidth(isMajor ? dpf(1.8f) : dpf(1.0f));
                canvas.drawLine(
                        cx + (float) Math.cos(rad) * rIn,
                        cy + (float) Math.sin(rad) * rIn,
                        cx + (float) Math.cos(rad) * (rIn + tLen),
                        cy + (float) Math.sin(rad) * (rIn + tLen), tickPaint);
            }

            // Shift sweep, with the phone glow pass and head dot
            if (onShift && shiftProgress > 0f) {
                float outerSweep = Math.max(0.01f, shiftProgress * 270f);
                glowPaint.setColor(COL_ACCENT);
                glowPaint.setAlpha(60);
                glowPaint.setStrokeWidth(dpf(11f));
                canvas.drawArc(outerRect, 135f, outerSweep, false, glowPaint);

                arcPaint.setColor(COL_ACCENT);
                arcPaint.setStrokeWidth(dpf(6f));
                canvas.drawArc(outerRect, 135f, outerSweep, false, arcPaint);

                double headRad = Math.toRadians(135f + outerSweep);
                canvas.drawCircle(cx + (float) Math.cos(headRad) * rOuter,
                        cy + (float) Math.sin(headRad) * rOuter, dpf(2.5f), pipPaint);
            }

            // Inner ring is the welfare countdown, exactly as on the phone
            trackPaint.setStrokeWidth(dpf(4f));
            trackPaint.setColor(COL_LINE_SUBTLE);
            canvas.drawArc(innerRect, 135f, 270f, false, trackPaint);

            long minsLeft = 0;
            try {
                minsLeft = Launcher.welfareMinutesLeft(getContext());
            } catch (Throwable ignored) {}
            float welfareLeft = Math.max(0f, Math.min(1f, minsLeft / 90f));
            float welfareFrac = 1f - welfareLeft;
            int welfareCol = welfareFrac > 0.85f
                    ? COL_CRIMSON : (welfareFrac > 0.60f ? COL_AMBER : COL_EMERALD);
            if (welfareLeft > 0f) {
                arcPaint.setColor(welfareCol);
                arcPaint.setStrokeWidth(dpf(4f));
                canvas.drawArc(innerRect, 135f, Math.max(0.01f, welfareLeft * 270f), false, arcPaint);
            }

            // Centre, set like the phone chronograph
            textPaint.setColor(COL_ACCENT);
            textPaint.setTextSize(w * 0.055f);
            textPaint.setLetterSpacing(0.12f);
            canvas.drawText(onShift ? "SHIFT " + Math.round(shiftProgress * 100) + "%" : "OFF SHIFT",
                    cx, cy - (h * 0.10f), textPaint);

            // HH:mm:ss is eight glyphs wide. Above about 0.125w the string runs
            // past the inner welfare ring on a round watch, so keep it under that.
            timePaint.setColor(COL_PALE);
            timePaint.setTextSize(w * 0.118f);
            canvas.drawText(timeStr, cx, cy + (h * 0.040f), timePaint);

            textPaint.setColor(COL_QUIET);
            textPaint.setTextSize(w * 0.040f);
            textPaint.setLetterSpacing(0.14f);
            canvas.drawText("AEST \u00b7 BRISBANE", cx, cy + (h * 0.145f), textPaint);
            textPaint.setLetterSpacing(0f);

            // The shift real ends, not a fixed 18:00 / 06:00
            if (onShift) {
                textPaint.setTextSize(w * 0.042f);
                textPaint.setColor(COL_MUTED);
                double leftRad = Math.toRadians(135.0);
                canvas.drawText(startLabel,
                        cx + (float) Math.cos(leftRad) * (rOuter + dpf(20f)) - dpf(4f),
                        cy + (float) Math.sin(leftRad) * (rOuter + dpf(20f)) + dpf(12f), textPaint);
                double rightRad = Math.toRadians(45.0);
                canvas.drawText(endLabel,
                        cx + (float) Math.cos(rightRad) * (rOuter + dpf(20f)) + dpf(4f),
                        cy + (float) Math.sin(rightRad) * (rOuter + dpf(20f)) + dpf(12f), textPaint);
            }
        }
    }
}
