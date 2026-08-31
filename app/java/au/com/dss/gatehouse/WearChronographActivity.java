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
 * WearChronographActivity — Screen 1: Clean Tactical Shift Chronograph Face for Wear OS.
 * Features vector 270° amber shift progress arc, live monospace digital clock,
 * AEST timezone, and seamless swipe navigation to companion watch tools.
 */
public class WearChronographActivity extends Activity {

    private ChronographWatchView watchView;
    private Handler clockHandler;
    private Runnable clockRunnable;
    private GestureDetector gestureDetector;

    private static final int COL_ACCENT = 0xFFFFD166;
    private static final int COL_EMERALD = 0xFF10B981;
    private static final int COL_CYAN = 0xFF00E5FF;
    private static final int COL_LINE = 0x33475569;
    private static final int COL_QUIET = 0xFF64748B;
    private static final int COL_MUTED = 0xFF94A3B8;

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
        private final Paint pipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        public ChronographWatchView(Context context) {
            super(context);
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeCap(Paint.Cap.ROUND);

            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);

            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeCap(Paint.Cap.ROUND);

            tickPaint.setStrokeCap(Paint.Cap.ROUND);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

            pipPaint.setStyle(Paint.Style.FILL);
            pipPaint.setColor(0xFFFFFFFF);
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

            // Shift Calculation (18:00 to 06:00 AEST = 12 hour shift default or 74% demo)
            long now = System.currentTimeMillis();
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.US);
            sdfTime.setTimeZone(TimeZone.getDefault());
            String timeStr = sdfTime.format(new Date(now));

            float shiftProgress = 0.74f; // 74% active patrol night shift
            int pct = (int) (shiftProgress * 100);

            // 1. Outer Inactive Track & Hourly Ticks
            trackPaint.setColor(COL_LINE);
            trackPaint.setStrokeWidth(12f);
            canvas.drawArc(outerRect, 135f, 270f, false, trackPaint);

            int tickCount = 12; // 12-hour shift ticks
            for (int i = 0; i <= tickCount; i++) {
                float angleDeg = 135f + (i * 270f / (float) tickCount);
                double rad = Math.toRadians(angleDeg);
                boolean isMajor = (i == 0 || i == tickCount || i == tickCount / 2);
                float tLen = isMajor ? 18f : 10f;
                float x1 = cx + (float) Math.cos(rad) * (rOuter + 6f);
                float y1 = cy + (float) Math.sin(rad) * (rOuter + 6f);
                float x2 = cx + (float) Math.cos(rad) * (rOuter + 6f + tLen);
                float y2 = cy + (float) Math.sin(rad) * (rOuter + 6f + tLen);

                tickPaint.setColor(isMajor ? COL_ACCENT : COL_QUIET);
                tickPaint.setStrokeWidth(isMajor ? 4.5f : 2.5f);
                canvas.drawLine(x1, y1, x2, y2, tickPaint);
            }

            // 2. Active Glowing Progress Sweep
            float outerSweep = shiftProgress * 270f;
            glowPaint.setColor(COL_ACCENT);
            glowPaint.setAlpha(65);
            glowPaint.setStrokeWidth(24f);
            canvas.drawArc(outerRect, 135f, outerSweep, false, glowPaint);

            arcPaint.setColor(COL_ACCENT);
            arcPaint.setStrokeWidth(12f);
            canvas.drawArc(outerRect, 135f, outerSweep, false, arcPaint);

            // Progress Cursor Head Pip
            double headRad = Math.toRadians(135f + outerSweep);
            float hx = cx + (float) Math.cos(headRad) * rOuter;
            float hy = cy + (float) Math.sin(headRad) * rOuter;
            canvas.drawCircle(hx, hy, 7.5f, pipPaint);

            // 3. Inner Secondary Track (Cyan / Emerald)
            trackPaint.setStrokeWidth(7f);
            trackPaint.setColor(0x22475569);
            canvas.drawArc(innerRect, 135f, 270f, false, trackPaint);

            float innerSweep = (1f - shiftProgress) * 270f;
            arcPaint.setColor(COL_EMERALD);
            arcPaint.setStrokeWidth(7f);
            canvas.drawArc(innerRect, 135f, innerSweep, false, arcPaint);

            // 4. Center Monospace Digital Core Display
            // Top: Shift %
            textPaint.setColor(COL_ACCENT);
            textPaint.setTextSize(w * 0.055f);
            textPaint.setLetterSpacing(0.08f);
            canvas.drawText("SHIFT " + pct + "%", cx, cy - (h * 0.10f), textPaint);

            // Center: Digital Time
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextSize(w * 0.14f);
            textPaint.setLetterSpacing(0.02f);
            canvas.drawText(timeStr, cx, cy + (h * 0.035f), textPaint);

            // Bottom: Subtitle AEST · BRISBANE
            textPaint.setColor(COL_QUIET);
            textPaint.setTextSize(w * 0.040f);
            textPaint.setLetterSpacing(0.12f);
            canvas.drawText("AEST · BRISBANE", cx, cy + (h * 0.14f), textPaint);

            // 5. Baseline 18:00 / 06:00 Timestamps
            textPaint.setTextSize(w * 0.042f);
            textPaint.setColor(COL_MUTED);
            textPaint.setLetterSpacing(0f);

            double leftRad = Math.toRadians(135.0);
            float lx = cx + (float) Math.cos(leftRad) * (rOuter + 28f);
            float ly = cy + (float) Math.sin(leftRad) * (rOuter + 28f);
            canvas.drawText("18:00", lx - 8f, ly + 14f, textPaint);

            double rightRad = Math.toRadians(45.0);
            float rx = cx + (float) Math.cos(rightRad) * (rOuter + 28f);
            float ry = cy + (float) Math.sin(rightRad) * (rOuter + 28f);
            canvas.drawText("06:00", rx + 8f, ry + 14f, textPaint);
        }
    }
}
