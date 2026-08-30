package au.com.dss.gatehouse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * FireRadarSweepView — Tactical 10km Concentric Fire Radar HUD.
 * Renders 2.5km, 5km, 7.5km, 10km range rings, 360° rotating phosphor sweep beam,
 * active fire incident blips with pulsating thermal rings, wind vectors, and hazard trajectory overlays.
 */
public class FireRadarSweepView extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint windPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float sweepAngle = 0f;
    private float pulsePhase = 0f;
    private ValueAnimator sweepAnimator;
    private ValueAnimator pulseAnimator;

    private FireRadarManager.FireRadarSnapshot snapshot = new FireRadarManager.FireRadarSnapshot();
    private FireRadarManager.FireIncident selectedIncident = null;

    public FireRadarSweepView(Context context) {
        super(context);
        init();
    }

    public FireRadarSweepView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(0x3306B6D4);
        ringPaint.setStrokeWidth(dp(1.2f));

        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setColor(0x2206B6D4);
        axisPaint.setStrokeWidth(dp(1f));

        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(dp(9.5f));
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        blipPaint.setStyle(Paint.Style.FILL);
        pulsePaint.setStyle(Paint.Style.STROKE);

        windPaint.setStyle(Paint.Style.STROKE);
        windPaint.setStrokeCap(Paint.Cap.ROUND);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(0xFF38BDF8);

        glowPaint.setStyle(Paint.Style.FILL);

        // Rotating radar sweep animator (smooth 4.0s continuous rotation)
        sweepAnimator = ValueAnimator.ofFloat(0f, 360f);
        sweepAnimator.setDuration(4000);
        sweepAnimator.setRepeatCount(ValueAnimator.INFINITE);
        sweepAnimator.setInterpolator(new LinearInterpolator());
        sweepAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                sweepAngle = (Float) va.getAnimatedValue();
                invalidate();
            }
        });

        // Pulsing thermal ring animator
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1600);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator va) {
                pulsePhase = (Float) va.getAnimatedValue();
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sweepAnimator != null && !sweepAnimator.isRunning()) sweepAnimator.start();
        if (pulseAnimator != null && !pulseAnimator.isRunning()) pulseAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sweepAnimator != null) sweepAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }

    public void setSnapshot(FireRadarManager.FireRadarSnapshot snapshot) {
        if (snapshot != null) {
            this.snapshot = snapshot;
            if (snapshot.hasFiresWithin10Km()) {
                selectedIncident = snapshot.getNearestIncident();
            }
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Dedicated Top Header (h <= dp(38)) & Bottom Telemetry (h >= h - dp(44))
        final float topHeaderH = dp(38);
        final float bottomFooterH = dp(44);
        final float usableH = Math.max(dp(100), h - topHeaderH - bottomFooterH);

        final float cx = w * 0.5f;
        final float cy = topHeaderH + usableH * 0.5f;
        final float maxRadius = Math.min(w * 0.45f, usableH * 0.48f);

        // 1. Radar Screen Dark Gradient Backdrop
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, 0, h, 0xFF050B14, 0xFF0B132B, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(new RectF(0, 0, w, h), dp(16), dp(16), bgPaint);

        // 2. Concentric Range Rings: 2.5km, 5.0km, 7.5km, 10.0km
        float[] ringFractions = {0.25f, 0.50f, 0.75f, 1.00f};
        String[] ringLabels = {"2.5k", "5.0k", "7.5k", "10k"};

        for (int i = 0; i < ringFractions.length; i++) {
            float r = maxRadius * ringFractions[i];
            ringPaint.setColor(i == 3 ? 0x6606B6D4 : 0x2A06B6D4);
            ringPaint.setStrokeWidth(i == 3 ? dp(1.8f) : dp(1f));
            canvas.drawCircle(cx, cy, r, ringPaint);

            // Ring distance label along 45° NE diagonal with subtle dark pill background
            float diagX = (float) (cx + r * Math.cos(Math.toRadians(45)));
            float diagY = (float) (cy - r * Math.sin(Math.toRadians(45)));
            
            Paint labelBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            labelBg.setColor(0xD9050B14);
            canvas.drawRoundRect(new RectF(diagX - dp(10), diagY - dp(6), diagX + dp(10), diagY + dp(6)), dp(3), dp(3), labelBg);

            textPaint.setColor(0xFF64748B);
            textPaint.setTextSize(dp(8f));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(ringLabels[i], diagX, diagY + dp(2.8f), textPaint);
        }

        // 3. Crosshairs & Radial Cardinal Ticks
        canvas.drawLine(cx - maxRadius, cy, cx + maxRadius, cy, axisPaint);
        canvas.drawLine(cx, cy - maxRadius, cx, cy + maxRadius, axisPaint);

        textPaint.setColor(0xFF38BDF8);
        textPaint.setTextSize(dp(9.5f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("N", cx, cy - maxRadius - dp(4), textPaint);
        canvas.drawText("S", cx, cy + maxRadius + dp(11), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("E", cx + maxRadius + dp(4), cy + dp(3.5f), textPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("W", cx - maxRadius - dp(4), cy + dp(3.5f), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // 4. Rotating Phosphor Sweep Cone
        canvas.save();
        canvas.rotate(sweepAngle, cx, cy);
        SweepGradient sweepGrad = new SweepGradient(cx, cy,
                new int[]{0x00000000, 0x0006B6D4, 0x1106B6D4, 0x4406B6D4, 0x9922D3EE},
                new float[]{0f, 0.70f, 0.85f, 0.96f, 1.0f});
        sweepPaint.setShader(sweepGrad);
        canvas.drawCircle(cx, cy, maxRadius, sweepPaint);
        canvas.restore();

        // 5. Ambient Wind Direction Vector Overlay (Top-Left Bar — Fully Outside Radar Circle)
        drawWindVector(canvas, dp(14), dp(18), snapshot.windDirDeg, snapshot.windSpeedKmh, snapshot.windDir);

        // 6. AFDRS Fire Danger Rating Badge (Top-Right Bar — Fully Outside Radar Circle)
        drawDangerRatingBadge(canvas, w - dp(14), dp(10), snapshot.dangerRating);

        // 7. Center Gatehouse Beacon (Kingston Post 01)
        glowPaint.setColor(0x3338BDF8);
        canvas.drawCircle(cx, cy, dp(8), glowPaint);
        canvas.drawCircle(cx, cy, dp(3.5f), centerPaint);

        // 8. Fire Incident Blips on Radar
        for (FireRadarManager.FireIncident inc : snapshot.incidentsWithin10Km) {
            float distFraction = (float) (inc.distanceKm / FireRadarManager.RADAR_RADIUS_KM);
            distFraction = Math.min(1.0f, Math.max(0.05f, distFraction));
            float blipDist = maxRadius * distFraction;

            double rad = Math.toRadians(inc.bearingDeg - 90.0);
            float bx = (float) (cx + blipDist * Math.cos(rad));
            float by = (float) (cy + blipDist * Math.sin(rad));

            int blipColor = inc.statusColor;

            // Thermal pulsing ripple ring
            float pr = dp(5) + pulsePhase * dp(14);
            int pulseAlpha = (int) ((1f - pulsePhase) * 180);
            pulsePaint.setColor((pulseAlpha << 24) | (blipColor & 0x00FFFFFF));
            pulsePaint.setStrokeWidth(dp(1.5f));
            canvas.drawCircle(bx, by, pr, pulsePaint);

            // Core Fire Blip
            blipPaint.setColor(blipColor);
            canvas.drawCircle(bx, by, dp(4.5f), blipPaint);

            // Blip Label Pill with Translucent Backdrop
            String lbl = String.format(Locale.US, "🔥 %.1fkm %s", inc.distanceKm, inc.compassDir);
            Paint blipPill = new Paint(Paint.ANTI_ALIAS_FLAG);
            blipPill.setColor(0xD90F172A);
            float lblW = textPaint.measureText(lbl);
            canvas.drawRoundRect(new RectF(bx + dp(6), by - dp(10), bx + dp(10) + lblW, by + dp(4)), dp(4), dp(4), blipPill);

            textPaint.setColor(0xFFF1F5F9);
            textPaint.setTextSize(dp(8.5f));
            canvas.drawText(lbl, bx + dp(8), by - dp(1), textPaint);
        }

        // 9. Bottom Selected Incident Telemetry Bar (Dedicated Footer Strip)
        drawSelectedTelemetry(canvas, w, h, cx, cy);
    }

    private void drawWindVector(Canvas canvas, float x, float y, double windDeg, double windSpeed, String windDir) {
        Paint wp = new Paint(Paint.ANTI_ALIAS_FLAG);
        wp.setColor(0xFF00E5FF);
        wp.setTextSize(dp(8.5f));
        wp.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        String timeStr = new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new java.util.Date(snapshot.lastUpdatedTs));
        canvas.drawText("🛰️ LIVE SCAN · " + timeStr + " AEST", x, y - dp(2), wp);

        wp.setColor(0xFF38BDF8);
        wp.setTextSize(dp(9.5f));
        canvas.drawText("💨 WIND " + windDir + " · " + String.format(Locale.US, "%.1f km/h", windSpeed), x, y + dp(12), wp);

        // Mini wind arrow pointing in direction wind travels
        float arrowLen = dp(14);
        float ax = x + dp(6);
        float ay = y + dp(22);
        double travelRad = Math.toRadians(windDeg + 180.0 - 90.0);
        float ex = (float) (ax + arrowLen * Math.cos(travelRad));
        float ey = (float) (ay + arrowLen * Math.sin(travelRad));

        windPaint.setColor(0xFF38BDF8);
        windPaint.setStrokeWidth(dp(2f));
        canvas.drawLine(ax, ay, ex, ey, windPaint);
        canvas.drawCircle(ex, ey, dp(2f), centerPaint);
    }

    private void drawDangerRatingBadge(Canvas canvas, float rightX, float topY, FireRadarManager.FireDangerRating rating) {
        if (rating == null) rating = FireRadarManager.FireDangerRating.MODERATE;
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setTextSize(dp(9.5f));
        bp.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        String text = "🔥 " + rating.label;
        float textWidth = bp.measureText(text);
        float boxW = textWidth + dp(16);
        float boxH = dp(22);
        float left = rightX - boxW;

        Paint boxBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxBg.setColor(rating.bgColor);
        canvas.drawRoundRect(new RectF(left, topY, rightX, topY + boxH), dp(6), dp(6), boxBg);

        bp.setColor(rating.color);
        canvas.drawText(text, left + dp(8), topY + dp(15), bp);
    }

    private void drawSelectedTelemetry(Canvas canvas, float w, float h, float cx, float cy) {
        if (snapshot.incidentsWithin10Km.isEmpty()) {
            Paint okPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            okPaint.setColor(0xFF10B981);
            okPaint.setTextSize(dp(10f));
            okPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            String msg = "✓ 10KM RADAR CLEAR · 0 ACTIVE THREATS";
            float tw = okPaint.measureText(msg);
            canvas.drawText(msg, (w - tw) * 0.5f, h - dp(12), okPaint);
            return;
        }

        FireRadarManager.FireIncident inc = selectedIncident != null ? selectedIncident : snapshot.getNearestIncident();
        if (inc == null) return;

        float boxH = dp(38);
        float boxY = h - boxH - dp(8);
        RectF r = new RectF(dp(12), boxY, w - dp(12), boxY + boxH);

        Paint panelBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        panelBg.setColor(0xEE0F172A);
        canvas.drawRoundRect(r, dp(10), dp(10), panelBg);

        Paint titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
        titleP.setColor(0xFFF1F5F9);
        titleP.setTextSize(dp(10f));
        titleP.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("🚨 " + inc.name + " (" + String.format(Locale.US, "%.1f km %s", inc.distanceKm, inc.compassDir) + ")", dp(20), boxY + dp(16), titleP);

        Paint subP = new Paint(Paint.ANTI_ALIAS_FLAG);
        subP.setColor(0xFFF59E0B);
        subP.setTextSize(dp(8.5f));
        subP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        canvas.drawText(inc.hazardPotential, dp(20), boxY + dp(30), subP);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();
            float cx = getWidth() * 0.5f;
            float cy = getHeight() * 0.5f;
            float maxRadius = Math.min(getWidth(), getHeight()) * 0.44f;

            for (FireRadarManager.FireIncident inc : snapshot.incidentsWithin10Km) {
                float distFraction = (float) (inc.distanceKm / FireRadarManager.RADAR_RADIUS_KM);
                float blipDist = maxRadius * Math.min(1.0f, Math.max(0.05f, distFraction));
                double rad = Math.toRadians(inc.bearingDeg - 90.0);
                float bx = (float) (cx + blipDist * Math.cos(rad));
                float by = (float) (cy + blipDist * Math.sin(rad));

                float d = (float) Math.hypot(tx - bx, ty - by);
                if (d < dp(28)) {
                    selectedIncident = inc;
                    invalidate();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}