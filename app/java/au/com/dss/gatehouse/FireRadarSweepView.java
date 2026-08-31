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
 * FireRadarSweepView — Precision 10km Concentric Fire Radar HUD.
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

    public static final int MODE_FIRE_WEATHER = 0;
    public static final int MODE_AIRSPACE = 1;

    private int radarMode = MODE_FIRE_WEATHER;

    private FireRadarManager.FireRadarSnapshot snapshot = new FireRadarManager.FireRadarSnapshot();
    private FireRadarManager.FireIncident selectedIncident = null;
    private FireRadarManager.LightningStrike selectedLightning = null;

    private AirspaceRadarManager.AirspaceSnapshot airspaceSnapshot = new AirspaceRadarManager.AirspaceSnapshot();
    private AirspaceRadarManager.AirTrack selectedAirTrack = null;

    public FireRadarSweepView(Context context) {
        super(context);
        init();
    }

    public FireRadarSweepView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setRadarMode(int mode) {
        this.radarMode = mode;
        invalidate();
    }

    public int getRadarMode() {
        return this.radarMode;
    }

    public void setAirspaceSnapshot(AirspaceRadarManager.AirspaceSnapshot snap) {
        if (snap != null) {
            this.airspaceSnapshot = snap;
            if (!snap.tracksWithin10Km.isEmpty()) {
                this.selectedAirTrack = snap.getNearestTrack();
            }
            invalidate();
        }
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
        int coneHighlight = (radarMode == MODE_AIRSPACE) ? 0x9900E5FF : 0x9922D3EE;
        int coneMid = (radarMode == MODE_AIRSPACE) ? 0x4400E5FF : 0x4406B6D4;
        SweepGradient sweepGrad = new SweepGradient(cx, cy,
                new int[]{0x00000000, 0x0006B6D4, 0x1106B6D4, coneMid, coneHighlight},
                new float[]{0f, 0.70f, 0.85f, 0.96f, 1.0f});
        sweepPaint.setShader(sweepGrad);
        canvas.drawCircle(cx, cy, maxRadius, sweepPaint);
        canvas.restore();

        // 5. Center Guard Hut Beacon (Kingston Hume Site)
        glowPaint.setColor(0x3338BDF8);
        canvas.drawCircle(cx, cy, dp(8), glowPaint);
        canvas.drawCircle(cx, cy, dp(3.5f), centerPaint);

        Paint hutLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hutLabelPaint.setColor(0xBB38BDF8);
        hutLabelPaint.setTextSize(dp(7f));
        hutLabelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        hutLabelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GUARD HUT", cx, cy + dp(11), hutLabelPaint);
        hutLabelPaint.setTextAlign(Paint.Align.LEFT);

        if (radarMode == MODE_AIRSPACE) {
            // =================================================================
            // AIRSPACE & POLAIR RADAR MODE
            // =================================================================
            drawAirspaceHeader(canvas, dp(14), dp(18));
            drawAirspaceBadge(canvas, w - dp(14), dp(10));

            // Render Aircraft & Drone Blips
            for (AirspaceRadarManager.AirTrack t : airspaceSnapshot.tracksWithin10Km) {
                float distFraction = (float) (t.distanceKm / AirspaceRadarManager.RADAR_RADIUS_KM);
                distFraction = Math.min(1.0f, Math.max(0.05f, distFraction));
                float blipDist = maxRadius * distFraction;

                double rad = Math.toRadians(t.bearingDeg - 90.0);
                float ax = (float) (cx + blipDist * Math.cos(rad));
                float ay = (float) (cy + blipDist * Math.sin(rad));

                if (t.category == AirspaceRadarManager.AircraftCategory.POLAIR_QPS) {
                    // Pulsing Cyan Ring
                    float pr = dp(6) + (1f - pulsePhase) * dp(14);
                    pulsePaint.setColor(((int) ((1f - pulsePhase) * 220) << 24) | 0x00E5FF);
                    pulsePaint.setStrokeWidth(dp(1.8f));
                    canvas.drawCircle(ax, ay, pr, pulsePaint);

                    // Rotating Rotor Blades Indicator
                    canvas.save();
                    canvas.rotate(sweepAngle * 3f, ax, ay);
                    Paint rotorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    rotorPaint.setColor(0xCC00E5FF);
                    rotorPaint.setStrokeWidth(dp(1.5f));
                    canvas.drawLine(ax - dp(8), ay, ax + dp(8), ay, rotorPaint);
                    canvas.drawLine(ax, ay - dp(8), ax, ay + dp(8), rotorPaint);
                    canvas.restore();

                    // Core Blip
                    blipPaint.setColor(0xFF00E5FF);
                    canvas.drawCircle(ax, ay, dp(5f), blipPaint);

                    // Label Pill
                    String lbl = String.format(Locale.US, "🚁 %s · %dft (%.1fkm)", t.callsign, t.altitudeFt, t.distanceKm);
                    Paint blipPill = new Paint(Paint.ANTI_ALIAS_FLAG);
                    blipPill.setColor(0xE6081B2A);
                    float lblW = textPaint.measureText(lbl);
                    canvas.drawRoundRect(new RectF(ax + dp(7), ay - dp(10), ax + dp(11) + lblW, ay + dp(4)), dp(4), dp(4), blipPill);

                    textPaint.setColor(0xFF00E5FF);
                    textPaint.setTextSize(dp(8.5f));
                    canvas.drawText(lbl, ax + dp(9), ay - dp(1), textPaint);

                } else if (t.category == AirspaceRadarManager.AircraftCategory.AEROMEDICAL_RESCUE) {
                    // LifeFlight Emerald Pulse
                    float pr = dp(6) + pulsePhase * dp(12);
                    pulsePaint.setColor(((int) ((1f - pulsePhase) * 180) << 24) | 0x10B981);
                    pulsePaint.setStrokeWidth(dp(1.5f));
                    canvas.drawCircle(ax, ay, pr, pulsePaint);

                    // Core Blip
                    blipPaint.setColor(0xFF10B981);
                    canvas.drawCircle(ax, ay, dp(4.5f), blipPaint);

                    // Label Pill
                    String lbl = String.format(Locale.US, "🚑 %s · %dft", t.callsign, t.altitudeFt);
                    Paint blipPill = new Paint(Paint.ANTI_ALIAS_FLAG);
                    blipPill.setColor(0xE606231B);
                    float lblW = textPaint.measureText(lbl);
                    canvas.drawRoundRect(new RectF(ax + dp(6), ay - dp(10), ax + dp(10) + lblW, ay + dp(4)), dp(4), dp(4), blipPill);

                    textPaint.setColor(0xFF34D399);
                    textPaint.setTextSize(dp(8.5f));
                    canvas.drawText(lbl, ax + dp(8), ay - dp(1), textPaint);

                } else if (t.category == AirspaceRadarManager.AircraftCategory.DRONE_UAS) {
                    // Drone Purple Pulsing Ring
                    float pr = dp(5) + pulsePhase * dp(10);
                    pulsePaint.setColor(((int) ((1f - pulsePhase) * 200) << 24) | 0xA855F7);
                    pulsePaint.setStrokeWidth(dp(1.5f));
                    canvas.drawCircle(ax, ay, pr, pulsePaint);

                    blipPaint.setColor(0xFFA855F7);
                    canvas.drawCircle(ax, ay, dp(4.5f), blipPaint);

                    String lbl = String.format(Locale.US, "🛸 DRONE · %dft AGL", t.altitudeFt);
                    Paint blipPill = new Paint(Paint.ANTI_ALIAS_FLAG);
                    blipPill.setColor(0xE6260F38);
                    float lblW = textPaint.measureText(lbl);
                    canvas.drawRoundRect(new RectF(ax + dp(6), ay - dp(10), ax + dp(10) + lblW, ay + dp(4)), dp(4), dp(4), blipPill);

                    textPaint.setColor(0xFFD8B4FE);
                    textPaint.setTextSize(dp(8.5f));
                    canvas.drawText(lbl, ax + dp(8), ay - dp(1), textPaint);

                } else {
                    // Civil / General Aviation
                    blipPaint.setColor(0xFF94A3B8);
                    canvas.drawCircle(ax, ay, dp(4f), blipPaint);

                    // Heading Vector Arrow
                    float arrowLen = dp(10);
                    double headRad = Math.toRadians(t.headingDeg - 90.0);
                    float hx = (float) (ax + arrowLen * Math.cos(headRad));
                    float hy = (float) (ay + arrowLen * Math.sin(headRad));
                    Paint headPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    headPaint.setColor(0xFF64748B);
                    headPaint.setStrokeWidth(dp(1.5f));
                    canvas.drawLine(ax, ay, hx, hy, headPaint);

                    String lbl = String.format(Locale.US, "✈️ %s · %dft", t.callsign, t.altitudeFt);
                    Paint blipPill = new Paint(Paint.ANTI_ALIAS_FLAG);
                    blipPill.setColor(0xD90F172A);
                    float lblW = textPaint.measureText(lbl);
                    canvas.drawRoundRect(new RectF(ax + dp(6), ay - dp(10), ax + dp(10) + lblW, ay + dp(4)), dp(4), dp(4), blipPill);

                    textPaint.setColor(0xFFCBD5E1);
                    textPaint.setTextSize(dp(8f));
                    canvas.drawText(lbl, ax + dp(8), ay - dp(1), textPaint);
                }
            }

            drawSelectedAirspaceTelemetry(canvas, w, h, cx, cy);

        } else {
            // =================================================================
            // FIRE & LIGHTNING WEATHER RADAR MODE
            // =================================================================
            drawWindVector(canvas, dp(14), dp(18), snapshot.windDirDeg, snapshot.windSpeedKmh, snapshot.windDir);
            drawDangerRatingBadge(canvas, w - dp(14), dp(10), snapshot.dangerRating);

            // Proximity Safety Danger Ring
            float threshFraction = (float) (Math.min(10.0, Math.max(1.0, snapshot.proximityThresholdKm)) / FireRadarManager.RADAR_RADIUS_KM);
            float threshRadius = maxRadius * threshFraction;
            Paint threshPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            threshPaint.setStyle(Paint.Style.STROKE);
            threshPaint.setStrokeWidth(dp(1.2f));
            threshPaint.setColor(snapshot.isLightningStandDownActive ? 0x88EF4444 : 0x66F59E0B);
            threshPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(4), dp(4)}, 0));
            canvas.drawCircle(cx, cy, threshRadius, threshPaint);

            // Lightning Strike Blips
            for (FireRadarManager.LightningStrike s : snapshot.lightningWithin10Km) {
                float distFraction = (float) (s.distanceKm / FireRadarManager.RADAR_RADIUS_KM);
                distFraction = Math.min(1.0f, Math.max(0.05f, distFraction));
                float blipDist = maxRadius * distFraction;

                double rad = Math.toRadians(s.bearingDeg - 90.0);
                float lx = (float) (cx + blipDist * Math.cos(rad));
                float ly = (float) (cy + blipDist * Math.sin(rad));

                float pr = dp(6) + (1f - pulsePhase) * dp(12);
                int pulseAlpha = (int) ((1f - pulsePhase) * 220);
                pulsePaint.setColor((pulseAlpha << 24) | 0x00E5FF);
                pulsePaint.setStrokeWidth(dp(1.8f));
                canvas.drawCircle(lx, ly, pr, pulsePaint);

                blipPaint.setColor(s.statusColor);
                canvas.drawCircle(lx, ly, dp(4.5f), blipPaint);

                String lbl = String.format(Locale.US, "⚡ %.1fkm (%dkA)", s.distanceKm, s.kiloAmps);
                Paint blipPill = new Paint(Paint.ANTI_ALIAS_FLAG);
                blipPill.setColor(0xD90B192C);
                float lblW = textPaint.measureText(lbl);
                canvas.drawRoundRect(new RectF(lx + dp(6), ly - dp(10), lx + dp(10) + lblW, ly + dp(4)), dp(4), dp(4), blipPill);

                textPaint.setColor(0xFF38BDF8);
                textPaint.setTextSize(dp(8.5f));
                canvas.drawText(lbl, lx + dp(8), ly - dp(1), textPaint);
            }

            // Fire Incident Blips
            for (FireRadarManager.FireIncident inc : snapshot.incidentsWithin10Km) {
                float distFraction = (float) (inc.distanceKm / FireRadarManager.RADAR_RADIUS_KM);
                distFraction = Math.min(1.0f, Math.max(0.05f, distFraction));
                float blipDist = maxRadius * distFraction;

                double rad = Math.toRadians(inc.bearingDeg - 90.0);
                float bx = (float) (cx + blipDist * Math.cos(rad));
                float by = (float) (cy + blipDist * Math.sin(rad));

                int blipColor = inc.statusColor;

                float pr = dp(5) + pulsePhase * dp(14);
                int pulseAlpha = (int) ((1f - pulsePhase) * 180);
                pulsePaint.setColor((pulseAlpha << 24) | (blipColor & 0x00FFFFFF));
                pulsePaint.setStrokeWidth(dp(1.5f));
                canvas.drawCircle(bx, by, pr, pulsePaint);

                blipPaint.setColor(blipColor);
                canvas.drawCircle(bx, by, dp(4.5f), blipPaint);

                String lbl = String.format(Locale.US, "🔥 %.1fkm %s", inc.distanceKm, inc.compassDir);
                Paint blipPill = new Paint(Paint.ANTI_ALIAS_FLAG);
                blipPill.setColor(0xD90F172A);
                float lblW = textPaint.measureText(lbl);
                canvas.drawRoundRect(new RectF(bx + dp(6), by - dp(10), bx + dp(10) + lblW, by + dp(4)), dp(4), dp(4), blipPill);

                textPaint.setColor(0xFFF1F5F9);
                textPaint.setTextSize(dp(8.5f));
                canvas.drawText(lbl, bx + dp(8), by - dp(1), textPaint);
            }

            drawSelectedTelemetry(canvas, w, h, cx, cy);
        }
    }

    private void drawAirspaceHeader(Canvas canvas, float x, float y) {
        Paint wp = new Paint(Paint.ANTI_ALIAS_FLAG);
        wp.setColor(0xFF00E5FF);
        wp.setTextSize(dp(8.5f));
        wp.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        String timeStr = new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new java.util.Date(airspaceSnapshot.lastUpdatedTs));
        canvas.drawText("🛰️ ADS-B SCAN · " + timeStr + " AEST", x, y - dp(2), wp);

        wp.setColor(airspaceSnapshot.hasPolairNearby ? 0xFF00E5FF : 0xFF38BDF8);
        wp.setTextSize(dp(9.5f));
        String status = airspaceSnapshot.hasPolairNearby
                ? "🚁 POLAIR PATROL IN SECTOR"
                : (airspaceSnapshot.hasDroneNearby ? "🛸 DRONE SIGHTING ACTIVE" : "✈️ " + airspaceSnapshot.totalTracks + " ACTIVE CONTACTS");
        canvas.drawText(status, x, y + dp(12), wp);
    }

    private void drawAirspaceBadge(Canvas canvas, float rightX, float topY) {
        Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        bp.setTextSize(dp(9.5f));
        bp.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        String text = "🚁 AIRSPACE HUD";
        float textWidth = bp.measureText(text);
        float boxW = textWidth + dp(16);
        float boxH = dp(22);
        float left = rightX - boxW;

        Paint boxBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        boxBg.setColor(0x3300E5FF);
        canvas.drawRoundRect(new RectF(left, topY, rightX, topY + boxH), dp(6), dp(6), boxBg);

        bp.setColor(0xFF00E5FF);
        canvas.drawText(text, left + dp(8), topY + dp(15), bp);
    }

    private void drawSelectedAirspaceTelemetry(Canvas canvas, float w, float h, float cx, float cy) {
        float boxH = dp(38);
        float boxY = h - boxH - dp(8);
        RectF r = new RectF(dp(12), boxY, w - dp(12), boxY + boxH);

        Paint panelBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        panelBg.setColor(0xEE0F172A);
        canvas.drawRoundRect(r, dp(10), dp(10), panelBg);

        if (selectedAirTrack != null) {
            Paint titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
            titleP.setColor(selectedAirTrack.statusColor);
            titleP.setTextSize(dp(10f));
            titleP.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(selectedAirTrack.callsign + " · " + selectedAirTrack.aircraftModel + " (" + String.format(Locale.US, "%.1f km %s", selectedAirTrack.distanceKm, selectedAirTrack.compassDir) + ")", dp(20), boxY + dp(16), titleP);

            Paint subP = new Paint(Paint.ANTI_ALIAS_FLAG);
            subP.setColor(0xFFF1F5F9);
            subP.setTextSize(dp(8.5f));
            subP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("ALT: " + selectedAirTrack.altitudeFt + " ft · SPD: " + selectedAirTrack.speedKmh + " km/h · " + selectedAirTrack.statusText, dp(20), boxY + dp(30), subP);
            return;
        }

        if (airspaceSnapshot.hasPolairNearby && airspaceSnapshot.nearestPolair != null) {
            AirspaceRadarManager.AirTrack p = airspaceSnapshot.nearestPolair;
            Paint titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
            titleP.setColor(0xFF00E5FF);
            titleP.setTextSize(dp(10f));
            titleP.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("🚁 " + p.callsign + " (QPS POLAIR) · " + String.format(Locale.US, "%.1f km %s", p.distanceKm, p.compassDir), dp(20), boxY + dp(16), titleP);

            Paint subP = new Paint(Paint.ANTI_ALIAS_FLAG);
            subP.setColor(0xFFF59E0B);
            subP.setTextSize(dp(8.5f));
            subP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("ALT: " + p.altitudeFt + " ft · " + p.statusText, dp(20), boxY + dp(30), subP);
            return;
        }

        Paint okPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        okPaint.setColor(0xFF10B981);
        okPaint.setTextSize(dp(10f));
        okPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        String msg = "✓ 10KM AIRSPACE CLEAR · NO LOW-ALTITUDE THREATS";
        float tw = okPaint.measureText(msg);
        canvas.drawText(msg, (w - tw) * 0.5f, boxY + dp(23), okPaint);
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
        float boxH = dp(38);
        float boxY = h - boxH - dp(8);
        RectF r = new RectF(dp(12), boxY, w - dp(12), boxY + boxH);

        Paint panelBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        panelBg.setColor(0xEE0F172A);
        canvas.drawRoundRect(r, dp(10), dp(10), panelBg);

        if (selectedLightning != null) {
            Paint titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
            titleP.setColor(0xFF00E5FF);
            titleP.setTextSize(dp(10f));
            titleP.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("⚡ STRIKE #" + selectedLightning.id + " (" + String.format(Locale.US, "%.1f km %s · %s", selectedLightning.distanceKm, selectedLightning.compassDir, selectedLightning.locationName) + ")", dp(20), boxY + dp(16), titleP);

            Paint subP = new Paint(Paint.ANTI_ALIAS_FLAG);
            subP.setColor(0xFFF59E0B);
            subP.setTextSize(dp(8.5f));
            subP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("PEAK CURRENT: " + selectedLightning.kiloAmps + " kA · " + (selectedLightning.isGroundStrike ? "CLOUD-TO-GROUND" : "INTRA-CLOUD"), dp(20), boxY + dp(30), subP);
            return;
        }

        if (selectedIncident != null) {
            Paint titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
            titleP.setColor(0xFFF1F5F9);
            titleP.setTextSize(dp(10f));
            titleP.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("🚨 " + selectedIncident.name + " (" + String.format(Locale.US, "%.1f km %s", selectedIncident.distanceKm, selectedIncident.compassDir) + ")", dp(20), boxY + dp(16), titleP);

            Paint subP = new Paint(Paint.ANTI_ALIAS_FLAG);
            subP.setColor(0xFFF59E0B);
            subP.setTextSize(dp(8.5f));
            subP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText(selectedIncident.hazardPotential, dp(20), boxY + dp(30), subP);
            return;
        }

        if (snapshot.hasHailWarning) {
            Paint titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
            titleP.setColor(0xFF38BDF8);
            titleP.setTextSize(dp(10f));
            titleP.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("🧊 SEVERE HAIL WARNING: " + snapshot.hailRiskLevel + " (~" + String.format(Locale.US, "%.0fmm", snapshot.estimatedHailSizeMm) + " · " + snapshot.hailProbabilityPercent + "%)", dp(20), boxY + dp(16), titleP);

            Paint subP = new Paint(Paint.ANTI_ALIAS_FLAG);
            subP.setColor(0xFFF59E0B);
            subP.setTextSize(dp(8.5f));
            subP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("WHS: MOVE PATROL VEHICLE UNDER COVER · SHELTER IN GUARD HUT", dp(20), boxY + dp(30), subP);
            return;
        }

        if (snapshot.isLightningStandDownActive) {
            Paint titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
            titleP.setColor(0xFFEF4444);
            titleP.setTextSize(dp(10f));
            titleP.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(snapshot.lightningStandDownReason, dp(20), boxY + dp(16), titleP);

            Paint subP = new Paint(Paint.ANTI_ALIAS_FLAG);
            subP.setColor(0xFF38BDF8);
            subP.setTextSize(dp(8.5f));
            subP.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("WHS ADVISORY: CEASE OUTDOOR TIMBER ROUNDS · SHELTER IN GUARD HUT", dp(20), boxY + dp(30), subP);
            return;
        }

        Paint okPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        okPaint.setColor(0xFF10B981);
        okPaint.setTextSize(dp(10f));
        okPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        String msg = "✓ 10KM RADAR CLEAR · 0 ACTIVE THREATS";
        float tw = okPaint.measureText(msg);
        canvas.drawText(msg, (w - tw) * 0.5f, boxY + dp(23), okPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();
            float cx = getWidth() * 0.5f;
            float cy = getHeight() * 0.5f;
            float maxRadius = Math.min(getWidth(), getHeight()) * 0.44f;

            if (radarMode == MODE_AIRSPACE) {
                // Check aircraft track touch
                for (AirspaceRadarManager.AirTrack t : airspaceSnapshot.tracksWithin10Km) {
                    float distFraction = (float) (t.distanceKm / AirspaceRadarManager.RADAR_RADIUS_KM);
                    float blipDist = maxRadius * Math.min(1.0f, Math.max(0.05f, distFraction));
                    double rad = Math.toRadians(t.bearingDeg - 90.0);
                    float ax = (float) (cx + blipDist * Math.cos(rad));
                    float ay = (float) (cy + blipDist * Math.sin(rad));

                    float d = (float) Math.hypot(tx - ax, ty - ay);
                    if (d < dp(28)) {
                        selectedAirTrack = t;
                        invalidate();
                        return true;
                    }
                }
            } else {
                // Check lightning strike blip touch first
                for (FireRadarManager.LightningStrike s : snapshot.lightningWithin10Km) {
                    float distFraction = (float) (s.distanceKm / FireRadarManager.RADAR_RADIUS_KM);
                    float blipDist = maxRadius * Math.min(1.0f, Math.max(0.05f, distFraction));
                    double rad = Math.toRadians(s.bearingDeg - 90.0);
                    float lx = (float) (cx + blipDist * Math.cos(rad));
                    float ly = (float) (cy + blipDist * Math.sin(rad));

                    float d = (float) Math.hypot(tx - lx, ty - ly);
                    if (d < dp(28)) {
                        selectedLightning = s;
                        selectedIncident = null;
                        invalidate();
                        return true;
                    }
                }

                // Check fire incident blip touch
                for (FireRadarManager.FireIncident inc : snapshot.incidentsWithin10Km) {
                    float distFraction = (float) (inc.distanceKm / FireRadarManager.RADAR_RADIUS_KM);
                    float blipDist = maxRadius * Math.min(1.0f, Math.max(0.05f, distFraction));
                    double rad = Math.toRadians(inc.bearingDeg - 90.0);
                    float bx = (float) (cx + blipDist * Math.cos(rad));
                    float by = (float) (cy + blipDist * Math.sin(rad));

                    float d = (float) Math.hypot(tx - bx, ty - by);
                    if (d < dp(28)) {
                        selectedIncident = inc;
                        selectedLightning = null;
                        invalidate();
                        return true;
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}