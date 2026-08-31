package au.com.dss.gatehouse;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * WearRadarActivity — Screen 3: 10km Bushfire & Threat Radar HUD for Wear OS.
 * 
 * Renders concentric 2.5km, 5km, 7.5km, 10km range rings, 360° rotating phosphor sweep beam,
 * active QFES bushfire hotspot blips with wind vector arrows, and real-time lightning strike proximity.
 */
public class WearRadarActivity extends Activity {

    private CircularRadarView radarView;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        radarView = new CircularRadarView(this);
        root.addView(radarView);

        // Bottom Warning Banner: ⚠️ FIRE & LIGHTNING RADAR
        TextView banner = new TextView(this);
        banner.setText("⚠️ FIRE & LIGHTNING RADAR");
        banner.setTextColor(0xFFFFD166);
        banner.setTextSize(10f);
        banner.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        banner.setGravity(Gravity.CENTER);
        banner.setBackgroundColor(0xDD1E293B);
        banner.setPadding(0, 8, 0, 8);

        FrameLayout.LayoutParams lpBanner = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpBanner.gravity = Gravity.BOTTOM;
        lpBanner.bottomMargin = 18;
        lpBanner.leftMargin = 40;
        lpBanner.rightMargin = 40;
        root.addView(banner, lpBanner);

        setContentView(root);

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null) {
                    float diffX = e2.getX() - e1.getX();
                    if (diffX > 80) { // Swipe Right -> Pressure Gauge
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        return true;
                    } else if (diffX < -80) { // Swipe Left -> PTT Radio
                        Intent intent = new Intent(WearRadarActivity.this, WearPttActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        return true;
                    }
                }
                return false;
            }
        });

        radarView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (radarView != null) radarView.startAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (radarView != null) radarView.stopAnimation();
    }

    private class CircularRadarView extends View {
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint blipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        private float sweepAngle = 0f;
        private ValueAnimator sweepAnim;

        public CircularRadarView(Context context) {
            super(context);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setColor(0x4410B981); // Emerald Ring
            ringPaint.setStrokeWidth(2f);

            axisPaint.setStyle(Paint.Style.STROKE);
            axisPaint.setColor(0x2210B981);
            axisPaint.setStrokeWidth(1.5f);

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

            blipPaint.setStyle(Paint.Style.FILL);
            pulsePaint.setStyle(Paint.Style.STROKE);
        }

        public void startAnimation() {
            if (sweepAnim == null) {
                sweepAnim = ValueAnimator.ofFloat(0f, 360f);
                sweepAnim.setDuration(4000);
                sweepAnim.setRepeatCount(ValueAnimator.INFINITE);
                sweepAnim.setInterpolator(new LinearInterpolator());
                sweepAnim.addUpdateListener(animation -> {
                    sweepAngle = (float) animation.getAnimatedValue();
                    invalidate();
                });
            }
            if (!sweepAnim.isRunning()) sweepAnim.start();
        }

        public void stopAnimation() {
            if (sweepAnim != null && sweepAnim.isRunning()) {
                sweepAnim.cancel();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;

            float cx = w / 2f;
            float cy = h / 2f - 10f;
            float maxR = Math.min(w, h) * 0.42f;

            // 1. Concentric Range Rings (2.5km, 5km, 10km)
            float[] radii = { maxR * 0.25f, maxR * 0.50f, maxR * 0.75f, maxR };
            String[] labels = { "2.5km", "5km", "7.5km", "10km" };

            for (int i = 0; i < radii.length; i++) {
                canvas.drawCircle(cx, cy, radii[i], ringPaint);
                textPaint.setColor(0x8810B981);
                textPaint.setTextSize(w * 0.026f);
                canvas.drawText(labels[i], cx, cy - radii[i] + 12f, textPaint);
            }

            // Crosshair Axes
            canvas.drawLine(cx - maxR, cy, cx + maxR, cy, axisPaint);
            canvas.drawLine(cx, cy - maxR, cx, cy + maxR, axisPaint);

            // 2. Rotating Phosphor Sweep Cone
            canvas.save();
            canvas.rotate(sweepAngle, cx, cy);
            int[] sweepColors = { 0x0010B981, 0x0010B981, 0x5510B981 };
            float[] sweepPositions = { 0f, 0.75f, 1f };
            SweepGradient sg = new SweepGradient(cx, cy, sweepColors, sweepPositions);
            sweepPaint.setShader(sg);
            canvas.drawCircle(cx, cy, maxR, sweepPaint);
            sweepPaint.setShader(null);
            canvas.restore();

            // 3. Center Yard Marker (Hume Doors Kingston)
            blipPaint.setColor(0xFF00E5FF);
            canvas.drawCircle(cx, cy, 5f, blipPaint);

            // 4. 🔥 QFES Bushfire Blip (7.8km NW = angle 315°, distance 7.8/10 * maxR)
            double fireRad = Math.toRadians(315.0);
            float fireDist = (7.8f / 10f) * maxR;
            float fx = cx + (float) Math.cos(fireRad) * fireDist;
            float fy = cy + (float) Math.sin(fireRad) * fireDist;

            // Fire Thermal Glow & Blip
            blipPaint.setColor(0xFFF97316); // Vibrant Orange Flame
            canvas.drawCircle(fx, fy, 8f, blipPaint);
            pulsePaint.setColor(0xFFF97316);
            pulsePaint.setStrokeWidth(2f);
            canvas.drawCircle(fx, fy, 14f, pulsePaint);

            textPaint.setColor(0xFFFFD166);
            textPaint.setTextSize(w * 0.032f);
            canvas.drawText("🔥 QFES FIRE 7.8km NW", fx + 30f, fy - 6f, textPaint);

            // Wind Vector Arrow (blowing SE from Fire)
            Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setColor(0xFFFFD166);
            arrowPaint.setStrokeWidth(3f);
            canvas.drawLine(fx, fy, fx + 16f, fy + 16f, arrowPaint);

            // 5. ⚡ Lightning Strike Blip (4.2km SE = angle 135°, distance 4.2/10 * maxR)
            double lightRad = Math.toRadians(135.0);
            float lightDist = (4.2f / 10f) * maxR;
            float lx = cx + (float) Math.cos(lightRad) * lightDist;
            float ly = cy + (float) Math.sin(lightRad) * lightDist;

            blipPaint.setColor(0xFFFFD166); // Amber Lightning
            canvas.drawCircle(lx, ly, 7f, blipPaint);
            pulsePaint.setColor(0xFFFFD166);
            canvas.drawCircle(lx, ly, 12f, pulsePaint);

            textPaint.setColor(0xFFFFD166);
            textPaint.setTextSize(w * 0.032f);
            canvas.drawText("⚡ 4.2km SE", lx + 22f, ly + 4f, textPaint);
        }
    }
}
