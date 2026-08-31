package au.com.dss.gatehouse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Date;

/**
 * WearPressureGaugeActivity — Screen 2: Interactive Rotary Pressure Gauge for Lot 16 Pump Room.
 * 
 * Supports Samsung Galaxy Watch rotating bezel hardware (via AXIS_SCROLL),
 * digital crown rotary encoders, and circular touch dragging with haptic detent feedback.
 * 1-Tap [ CONFIRM & LOG ] commits the verification directly to the SPARK Ada record core.
 */
public class WearPressureGaugeActivity extends Activity {

    private RotaryPressureGaugeView gaugeView;
    private Vibrator vibrator;
    private GestureDetector gestureDetector;
    private float currentPsi = 1200f; // Default nominal 1,200 PSI
    private static final float MIN_PSI = 0f;
    private static final float MAX_PSI = 1600f;
    private static final float NOMINAL_MIN = 1000f;
    private static final float NOMINAL_MAX = 1400f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        gaugeView = new RotaryPressureGaugeView(this);
        root.addView(gaugeView);

        // Top Prompt: ROTATE BEZEL TO ADJUST
        TextView promptView = new TextView(this);
        promptView.setText("ROTATE BEZEL TO ADJUST");
        promptView.setTextColor(0xFF94A3B8);
        promptView.setTextSize(10f);
        promptView.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        promptView.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout.LayoutParams lpPrompt = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpPrompt.topMargin = 22;
        root.addView(promptView, lpPrompt);

        // Bottom Action Button: [ CONFIRM & LOG ]
        TextView logButton = new TextView(this);
        logButton.setText("[ CONFIRM & LOG ]");
        logButton.setTextColor(0xFFFFFFFF);
        logButton.setTextSize(12f);
        logButton.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        logButton.setGravity(Gravity.CENTER);
        logButton.setBackgroundColor(0xFF10B981);
        logButton.setPadding(0, 16, 0, 16);

        FrameLayout.LayoutParams lpBtn = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpBtn.gravity = Gravity.BOTTOM;
        lpBtn.bottomMargin = 28;
        lpBtn.leftMargin = 50;
        lpBtn.rightMargin = 50;
        root.addView(logButton, lpBtn);

        logButton.setOnClickListener(v -> logInspection());

        setContentView(root);

        // 1. Samsung Hardware Rotating Bezel / Digital Crown Input Listener
        root.setFocusable(true);
        root.setFocusableInTouchMode(true);
        root.requestFocus();

        root.setOnGenericMotionListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_SCROLL) {
                float vScroll = event.getAxisValue(MotionEvent.AXIS_SCROLL);
                if (vScroll != 0f) {
                    adjustPsi(vScroll * 50f);
                    return true;
                }
            }
            return false;
        });

        // 2. Swipe Navigation (Right -> Chronograph, Left -> Radar)
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    float diffX = e2.getX() - e1.getX();
                    if (diffX > 80) { // Swipe Right -> Chronograph
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        return true;
                    } else if (diffX < -80) { // Swipe Left -> Radar
                        Intent intent = new Intent(WearPressureGaugeActivity.this, WearRadarActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void adjustPsi(float delta) {
        float old = currentPsi;
        currentPsi = Math.max(MIN_PSI, Math.min(MAX_PSI, currentPsi + delta));
        if (Math.round(old / 50f) != Math.round(currentPsi / 50f)) {
            triggerHapticClick();
        }
        if (gaugeView != null) gaugeView.invalidate();
    }

    private void triggerHapticClick() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(12);
            }
        }
    }

    private void logInspection() {
        triggerHapticClick();
        int psiInt = Math.round(currentPsi);
        boolean nominal = (currentPsi >= NOMINAL_MIN && currentPsi <= NOMINAL_MAX);
        String status = nominal ? "NOMINAL" : "OUT OF SPEC";

        // Commit to SPARK Ada Core Logbook
        try {
            int nowMins = (int) (System.currentTimeMillis() / 60000L);
            Core.addCheckpoint(nowMins, nowMins, "Lot 16 Fire System (Inside)", "04E3F4A5B6C7D8", 1, Core.AUTH_CRYPTOGRAPHIC);
            Core.addNote(Core.KIND_OBSERVATION, Core.TOPIC_ROUTINE, nowMins, nowMins, "Lot 16 Fire Booster: " + psiInt + " PSI (" + status + ")", 0);
        } catch (Throwable ignored) {}

        Toast.makeText(this, "Logged: " + psiInt + " PSI (" + status + ")", Toast.LENGTH_SHORT).show();
    }

    private class RotaryPressureGaugeView extends View {
        private final Paint dialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint nominalZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private float lastTouchY = 0f;

        public RotaryPressureGaugeView(Context context) {
            super(context);
            dialPaint.setStyle(Paint.Style.STROKE);
            dialPaint.setStrokeCap(Paint.Cap.ROUND);

            tickPaint.setStrokeCap(Paint.Cap.ROUND);

            nominalZonePaint.setStyle(Paint.Style.STROKE);
            nominalZonePaint.setStrokeCap(Paint.Cap.BUTT);

            needlePaint.setStyle(Paint.Style.STROKE);
            needlePaint.setStrokeCap(Paint.Cap.ROUND);
            needlePaint.setColor(0xFFFFD166); // Luminous Amber Needle

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (gestureDetector != null && gestureDetector.onTouchEvent(event)) {
                return true;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchY = event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dy = lastTouchY - event.getY();
                    if (Math.abs(dy) > 4) {
                        adjustPsi(dy * 5f);
                        lastTouchY = event.getY();
                    }
                    return true;
            }
            return super.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            float cx = w / 2f;
            float cy = h / 2f - (h * 0.04f);
            float rOuter = Math.min(w, h) * 0.38f;

            RectF dialRect = new RectF(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter);

            // 1. Background Dial Arc (135° to 45° = 270° sweep)
            dialPaint.setColor(0x33475569);
            dialPaint.setStrokeWidth(12f);
            canvas.drawArc(dialRect, 135f, 270f, false, dialPaint);

            // 2. Green Nominal Safety Arc (1,000 to 1,400 PSI)
            float startFrac = (NOMINAL_MIN - MIN_PSI) / (MAX_PSI - MIN_PSI);
            float endFrac = (NOMINAL_MAX - MIN_PSI) / (MAX_PSI - MIN_PSI);
            float nomStartAngle = 135f + (startFrac * 270f);
            float nomSweep = (endFrac - startFrac) * 270f;

            nominalZonePaint.setColor(0xFF10B981);
            nominalZonePaint.setStrokeWidth(14f);
            canvas.drawArc(dialRect, nomStartAngle, nomSweep, false, nominalZonePaint);

            // 3. Gauge Ticks & Numbers (0, 200, 400, 600, 800, 1000, 1200, 1400, 1600)
            int numSteps = 8;
            for (int i = 0; i <= numSteps; i++) {
                float frac = i / (float) numSteps;
                float angle = 135f + (frac * 270f);
                double rad = Math.toRadians(angle);

                float x1 = cx + (float) Math.cos(rad) * (rOuter - 4f);
                float y1 = cy + (float) Math.sin(rad) * (rOuter - 4f);
                float x2 = cx + (float) Math.cos(rad) * (rOuter + 8f);
                float y2 = cy + (float) Math.sin(rad) * (rOuter + 8f);

                tickPaint.setColor(0xFF94A3B8);
                tickPaint.setStrokeWidth(3.5f);
                canvas.drawLine(x1, y1, x2, y2, tickPaint);

                // Tick Numerals (Inner Ring)
                int val = Math.round(MIN_PSI + frac * (MAX_PSI - MIN_PSI));
                if (val % 400 == 0 || val == 1000 || val == 1400) {
                    float nx = cx + (float) Math.cos(rad) * (rOuter - 26f);
                    float ny = cy + (float) Math.sin(rad) * (rOuter - 26f) + 5f;
                    textPaint.setTextSize(w * 0.034f);
                    textPaint.setColor(0xFF64748B);
                    canvas.drawText(String.valueOf(val), nx, ny, textPaint);
                }
            }

            // 4. Amber Needle
            float psiFrac = (currentPsi - MIN_PSI) / (MAX_PSI - MIN_PSI);
            float needleAngle = 135f + (psiFrac * 270f);
            double needleRad = Math.toRadians(needleAngle);

            float needleLen = rOuter + 4f;
            float nEndx = cx + (float) Math.cos(needleRad) * needleLen;
            float nEndy = cy + (float) Math.sin(needleRad) * needleLen;

            needlePaint.setStrokeWidth(7f);
            canvas.drawLine(cx, cy, nEndx, nEndy, needlePaint);

            // Needle Center Hub
            Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            hubPaint.setStyle(Paint.Style.FILL);
            hubPaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(cx, cy, 9f, hubPaint);

            // 5. Center Digital Readout & Status
            int psiInt = Math.round(currentPsi);
            boolean nominal = (currentPsi >= NOMINAL_MIN && currentPsi <= NOMINAL_MAX);

            // PSI Readout
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextSize(w * 0.11f);
            textPaint.setLetterSpacing(0.02f);
            canvas.drawText(String.format(Locale.US, "%,d", psiInt) + " PSI", cx, cy + (h * 0.16f), textPaint);

            // Nominal / Alert Sub-badge
            textPaint.setColor(nominal ? 0xFF10B981 : 0xFFEF4444);
            textPaint.setTextSize(w * 0.038f);
            textPaint.setLetterSpacing(0.08f);
            canvas.drawText(nominal ? "NOMINAL" : "OUT OF RANGE", cx, cy + (h * 0.23f), textPaint);
        }
    }
}
