package au.com.dss.gatehouse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Locale;

/**
 * NightSkyDomeView — Polar Sky Dome & Orbital Track HUD.
 * Renders local 0°–90° elevation horizon rings, cardinal compass bearings,
 * and orbital trajectory arcs for the ISS and Starlink train passes visible from Kingston.
 */
public class NightSkyDomeView extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint satPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path trackPath = new Path();
    private final RectF domeRect = new RectF();

    private SatelliteTrackerManager.VisualPass currentPass;
    private float pulsePhase = 0f;
    private ValueAnimator pulseAnimator;

    // Synthetic starfield coordinates (normalized -1..1)
    private static final float[][] STARS = {
            {-0.4f, -0.6f, 1.5f}, {0.3f, -0.7f, 2.0f}, {0.7f, -0.3f, 1.2f},
            {-0.6f, 0.4f, 1.8f}, {-0.2f, 0.5f, 1.0f}, {0.5f, 0.6f, 2.2f},
            {0.1f, -0.2f, 2.5f}, {-0.5f, -0.1f, 1.6f}, {0.6f, 0.2f, 1.4f},
            {-0.3f, 0.7f, 1.7f}, {0.4f, -0.4f, 1.9f}, {-0.7f, -0.5f, 1.1f}
    };

    public NightSkyDomeView(Context context) {
        super(context);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(0x3300E5FF);
        ringPaint.setStrokeWidth(dp(1.2f));

        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setColor(0x2200E5FF);
        axisPaint.setStrokeWidth(dp(1.0f));
        axisPaint.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));

        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setStrokeJoin(Paint.Join.ROUND);

        satPaint.setStyle(Paint.Style.FILL);
        glowPaint.setStyle(Paint.Style.FILL);
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setColor(0x88FFFFFF);

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2400);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                pulsePhase = (float) a.getAnimatedValue();
                invalidate();
            }
        });
        pulseAnimator.start();
    }

    public void setVisualPass(SatelliteTrackerManager.VisualPass pass) {
        this.currentPass = pass;
        invalidate();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float maxRadius = Math.min(w, h) / 2f - dp(24);
        if (maxRadius <= 10) return;

        // 1. Dark Sky Dome Background
        domeRect.set(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius);
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xFF070C18);
        canvas.drawCircle(cx, cy, maxRadius, bgPaint);

        // 2. Ambient Starfield
        for (float[] s : STARS) {
            float sx = cx + s[0] * maxRadius * 0.9f;
            float sy = cy + s[1] * maxRadius * 0.9f;
            float sr = dp(s[2] * 0.6f);
            canvas.drawCircle(sx, sy, sr, starPaint);
        }

        // 3. Polar Elevation Rings (0° Horizon, 30°, 60°, Zenith 90°)
        ringPaint.setColor(0x3300E5FF);
        canvas.drawCircle(cx, cy, maxRadius, ringPaint);           // 0° Horizon
        ringPaint.setColor(0x2200E5FF);
        canvas.drawCircle(cx, cy, maxRadius * 0.66f, ringPaint);   // 30° Elevation
        canvas.drawCircle(cx, cy, maxRadius * 0.33f, ringPaint);   // 60° Elevation

        // Zenith Crosshair (90° directly overhead)
        canvas.drawLine(cx - dp(6), cy, cx + dp(6), cy, ringPaint);
        canvas.drawLine(cx, cy - dp(6), cx, cy + dp(6), ringPaint);

        // Elevation Text Labels
        textPaint.setTextSize(dp(8.5f));
        textPaint.setColor(0x6600E5FF);
        canvas.drawText("ZENITH 90°", cx, cy - dp(8), textPaint);
        canvas.drawText("60°", cx, cy - maxRadius * 0.33f + dp(3), textPaint);
        canvas.drawText("30°", cx, cy - maxRadius * 0.66f + dp(3), textPaint);
        canvas.drawText("HORIZON 0°", cx, cy - maxRadius + dp(10), textPaint);

        // 4. Compass Cardinal Axis Lines & Labels
        canvas.drawLine(cx, cy - maxRadius, cx, cy + maxRadius, axisPaint);
        canvas.drawLine(cx - maxRadius, cy, cx + maxRadius, cy, axisPaint);

        textPaint.setTextSize(dp(11f));
        textPaint.setColor(0xFF00E5FF);
        canvas.drawText("N", cx, cy - maxRadius - dp(6), textPaint);
        textPaint.setColor(0xFF94A3B8);
        canvas.drawText("S", cx, cy + maxRadius + dp(15), textPaint);
        canvas.drawText("E", cx + maxRadius + dp(12), cy + dp(4), textPaint);
        canvas.drawText("W", cx - maxRadius - dp(12), cy + dp(4), textPaint);

        // 5. Draw Satellite Trajectory Arc
        if (currentPass != null) {
            int trackColor = currentPass.category != null ? currentPass.category.color : 0xFF00E5FF;
            trackPaint.setColor(trackColor);
            trackPaint.setStrokeWidth(dp(3f));

            // Convert Rise, Peak, Set (Azimuth, Elevation) to Polar Coordinates (x, y)
            // Elevation: 0° = maxRadius, 90° = 0 (center) -> r = maxRadius * (1 - el / 90)
            // Azimuth: 0° = North (up, -90 deg from standard math 0)
            float rRise = maxRadius * (1f - (float) Math.max(0, currentPass.startEl) / 90f);
            double radRise = Math.toRadians(currentPass.startAz - 90.0);
            float xRise = (float) (cx + rRise * Math.cos(radRise));
            float yRise = (float) (cy + rRise * Math.sin(radRise));

            float rPeak = maxRadius * (1f - (float) Math.max(0, currentPass.maxEl) / 90f);
            double radPeak = Math.toRadians(currentPass.maxAz - 90.0);
            float xPeak = (float) (cx + rPeak * Math.cos(radPeak));
            float yPeak = (float) (cy + rPeak * Math.sin(radPeak));

            float rSet = maxRadius * (1f - (float) Math.max(0, currentPass.endEl) / 90f);
            double radSet = Math.toRadians(currentPass.endAz - 90.0);
            float xSet = (float) (cx + rSet * Math.cos(radSet));
            float ySet = (float) (cy + rSet * Math.sin(radSet));

            // Quadratic Bezier curve arc representing celestial ground-track
            trackPath.reset();
            trackPath.moveTo(xRise, yRise);
            trackPath.quadTo(xPeak, yPeak, xSet, ySet);
            canvas.drawPath(trackPath, trackPaint);

            // Rise & Set Endpoint Dots
            satPaint.setColor(trackColor);
            canvas.drawCircle(xRise, yRise, dp(4), satPaint);
            canvas.drawCircle(xSet, ySet, dp(4), satPaint);

            // Trajectory labels
            textPaint.setTextSize(dp(8f));
            textPaint.setColor(0xFFFFFFFF);
            canvas.drawText("RISE " + currentPass.startAzCompass, xRise, yRise - dp(6), textPaint);
            canvas.drawText("SET " + currentPass.endAzCompass, xSet, ySet + dp(12), textPaint);

            // 6. Real-time / Simulated Satellite Position Marker
            long now = System.currentTimeMillis();
            float tFrac = 0.5f; // Default to peak
            if (currentPass.durationSec > 0 && currentPass.startUtcMillis > 0) {
                if (now >= currentPass.startUtcMillis && now <= currentPass.endUtcMillis) {
                    tFrac = (float) (now - currentPass.startUtcMillis) / (float) (currentPass.endUtcMillis - currentPass.startUtcMillis);
                } else {
                    // Cyclic demonstration sweep when outside pass window
                    tFrac = pulsePhase;
                }
            }

            // Interpolate position along quadratic curve: B(t) = (1-t)^2*P0 + 2(1-t)t*P1 + t^2*P2
            float u = 1f - tFrac;
            float curX = u * u * xRise + 2 * u * tFrac * xPeak + tFrac * tFrac * xSet;
            float curY = u * u * yRise + 2 * u * tFrac * yPeak + tFrac * tFrac * ySet;

            if (currentPass.isStarlinkTrain) {
                // Render Starlink Train: chain of 12 luminous satellite nodes
                satPaint.setColor(0xFFFFFFFF);
                int count = Math.min(15, Math.max(6, currentPass.trainSatCount));
                for (int i = 0; i < count; i++) {
                    float offsetFrac = tFrac - (i * 0.022f);
                    if (offsetFrac >= 0f && offsetFrac <= 1f) {
                        float ou = 1f - offsetFrac;
                        float sx = ou * ou * xRise + 2 * ou * offsetFrac * xPeak + offsetFrac * offsetFrac * xSet;
                        float sy = ou * ou * yRise + 2 * ou * offsetFrac * yPeak + offsetFrac * offsetFrac * xSet;
                        float nodeSize = (i == 0) ? dp(4f) : dp(2.2f);
                        canvas.drawCircle(sx, sy, nodeSize, satPaint);
                    }
                }
                // Pulsing Lead Sat Glow Ring
                glowPaint.setColor(0x5510B981);
                canvas.drawCircle(curX, curY, dp(12) + (1f - pulsePhase) * dp(10), glowPaint);
            } else {
                // Single Satellite / ISS Glow Node
                glowPaint.setColor((trackColor & 0x00FFFFFF) | 0x44000000);
                canvas.drawCircle(curX, curY, dp(14) + (1f - pulsePhase) * dp(12), glowPaint);
                satPaint.setColor(0xFFFFFFFF);
                canvas.drawCircle(curX, curY, dp(5), satPaint);
                satPaint.setColor(trackColor);
                canvas.drawCircle(curX, curY, dp(3), satPaint);
            }

            // Satellite Tag Header overlay
            textPaint.setTextSize(dp(9.5f));
            textPaint.setColor(trackColor);
            String satLabel = currentPass.satName + " · Mag " + String.format(Locale.US, "%.1f", currentPass.visualMag);
            canvas.drawText(satLabel, curX, curY - dp(14), textPaint);
        }
    }
}
