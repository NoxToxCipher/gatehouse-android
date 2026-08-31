package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SkyWatchRadarView — Interactive Polar Airspace Radar Scope for Hume Facility.
 * Renders live ADS-B military transports, emergency medevac, and vintage warbirds.
 */
public class SkyWatchRadarView extends View {

    public interface OnAircraftSelectedListener {
        void onSelected(AdsbSkyRadarService.TrackedAircraft ac);
    }

    private final Paint scopeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint vectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetReticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    private final List<AdsbSkyRadarService.TrackedAircraft> aircraftList = new ArrayList<>();
    private AdsbSkyRadarService.TrackedAircraft selectedTarget = null;
    private OnAircraftSelectedListener selectListener;

    private double maxRadiusNm = 25.0;
    private float sweepAngle = 0f;
    private long lastFrameMs = 0;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public SkyWatchRadarView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        ringPaint.setColor(0x3300E5FF);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dpf(1.2f));

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        vectorPaint.setStyle(Paint.Style.STROKE);
        vectorPaint.setStrokeWidth(dpf(1.5f));

        targetReticlePaint.setColor(0xFFFFD166);
        targetReticlePaint.setStyle(Paint.Style.STROKE);
        targetReticlePaint.setStrokeWidth(dpf(1.8f));
        targetReticlePaint.setPathEffect(new DashPathEffect(new float[]{dpf(4), dpf(3)}, 0));
    }

    public void setAircraftList(List<AdsbSkyRadarService.TrackedAircraft> list) {
        this.aircraftList.clear();
        if (list != null) this.aircraftList.addAll(list);
        if (selectedTarget != null) {
            boolean found = false;
            for (AdsbSkyRadarService.TrackedAircraft ac : aircraftList) {
                if (ac.hex.equalsIgnoreCase(selectedTarget.hex)) {
                    selectedTarget = ac;
                    found = true;
                    break;
                }
            }
            if (!found) selectedTarget = null;
        }
        invalidate();
    }

    public void setOnAircraftSelectedListener(OnAircraftSelectedListener l) {
        this.selectListener = l;
    }

    public void setMaxRadiusNm(double nm) {
        this.maxRadiusNm = nm;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float rMax = Math.min(cx, cy) - dpf(16f);

            float touchX = event.getX();
            float touchY = event.getY();

            AdsbSkyRadarService.TrackedAircraft closest = null;
            float minDist = dpf(28f);

            for (AdsbSkyRadarService.TrackedAircraft ac : aircraftList) {
                PointF pt = getAircraftScreenPos(ac, cx, cy, rMax);
                float d = (float) Math.hypot(pt.x - touchX, pt.y - touchY);
                if (d < minDist) {
                    minDist = d;
                    closest = ac;
                }
            }

            if (closest != null) {
                selectedTarget = closest;
                try {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                } catch (Exception ignored) {}
                if (selectListener != null) selectListener.onSelected(selectedTarget);
                invalidate();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private PointF getAircraftScreenPos(AdsbSkyRadarService.TrackedAircraft ac, float cx, float cy, float rMax) {
        double distRatio = Math.min(1.0, ac.distanceNm / maxRadiusNm);
        float radiusPix = (float) (distRatio * rMax);
        double rad = Math.toRadians(ac.bearingDeg - 90.0);
        float px = cx + (float) (Math.cos(rad) * radiusPix);
        float py = cy + (float) (Math.sin(rad) * radiusPix);
        return new PointF(px, py);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float rMax = Math.min(cx, cy) - dpf(16f);

        long now = SystemClock.uptimeMillis();
        if (lastFrameMs == 0) lastFrameMs = now;
        long dt = now - lastFrameMs;
        lastFrameMs = now;
        sweepAngle = (sweepAngle + (dt * 0.09f)) % 360f;

        // 1. Radar Scope Base (Deep Oceanic Obsidian)
        bounds.set(0, 0, w, h);
        scopeBgPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF060D1A, 0xFF0B172A, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(bounds, dpf(16f), dpf(16f), scopeBgPaint);

        // 2. Concentric Range Rings (5, 15, 25 NM)
        double[] rings = {5.0, 15.0, 25.0};
        for (double rNm : rings) {
            float rPix = (float) ((rNm / maxRadiusNm) * rMax);
            ringPaint.setColor(0x2200E5FF);
            canvas.drawCircle(cx, cy, rPix, ringPaint);

            labelPaint.setColor(0x6600E5FF);
            labelPaint.setTextSize(dpf(8f));
            canvas.drawText(String.format(Locale.US, "%.0f NM", rNm), cx, cy - rPix + dpf(10f), labelPaint);
        }

        // Crosshairs
        canvas.drawLine(cx - rMax, cy, cx + rMax, cy, ringPaint);
        canvas.drawLine(cx, cy - rMax, cx, cy + rMax, ringPaint);

        // Cardinal Direction Labels
        labelPaint.setColor(0xFF38BDF8);
        labelPaint.setTextSize(dpf(10f));
        canvas.drawText("N", cx, cy - rMax - dpf(3f), labelPaint);
        canvas.drawText("S", cx, cy + rMax + dpf(11f), labelPaint);
        canvas.drawText("E", cx + rMax + dpf(8f), cy + dpf(3f), labelPaint);
        canvas.drawText("W", cx - rMax - dpf(8f), cy + dpf(3f), labelPaint);

        // 3. Rotating Sweep Glow
        canvas.save();
        canvas.rotate(sweepAngle, cx, cy);
        SweepGradient sweepGrad = new SweepGradient(cx, cy,
            new int[]{0x0000E5FF, 0x0000E5FF, 0x3300E5FF, 0x8800E5FF},
            new float[]{0f, 0.75f, 0.92f, 1.0f}
        );
        sweepPaint.setShader(sweepGrad);
        canvas.drawCircle(cx, cy, rMax, sweepPaint);
        canvas.restore();

        // 4. Center Home Facility Beacon
        Paint beaconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        beaconPaint.setColor(0xFF10B981);
        canvas.drawCircle(cx, cy, dpf(4f), beaconPaint);
        labelPaint.setColor(0xFF10B981);
        labelPaint.setTextSize(dpf(7.5f));
        canvas.drawText("📍 HUME GATE", cx, cy + dpf(12f), labelPaint);

        // 5. Render Tracked Aircraft Blips
        for (AdsbSkyRadarService.TrackedAircraft ac : aircraftList) {
            PointF pt = getAircraftScreenPos(ac, cx, cy, rMax);

            int blipColor = ac.category.color;
            blipPaint.setColor(blipColor);
            blipPaint.setStyle(Paint.Style.FILL);

            // Blip core
            canvas.drawCircle(pt.x, pt.y, ac.isSpecial ? dpf(4.5f) : dpf(3f), blipPaint);

            // Halo for special aircraft (Military, Rescue, Warbirds)
            if (ac.isSpecial) {
                Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
                halo.setColor(blipColor & 0x44FFFFFF);
                halo.setStyle(Paint.Style.FILL);
                canvas.drawCircle(pt.x, pt.y, dpf(9f), halo);
            }

            // Velocity Heading Vector (Direction arrow)
            if (ac.speedKts > 20) {
                vectorPaint.setColor(blipColor);
                double vRad = Math.toRadians(ac.headingDeg - 90.0);
                float vLen = Math.min(dpf(18f), dpf(6f) + (ac.speedKts / 30f));
                float vx = pt.x + (float) (Math.cos(vRad) * vLen);
                float vy = pt.y + (float) (Math.sin(vRad) * vLen);
                canvas.drawLine(pt.x, pt.y, vx, vy, vectorPaint);
            }

            // Callsign & Altitude Text Tag
            labelPaint.setColor(blipColor);
            labelPaint.setTextSize(dpf(8f));
            String tag = ac.callsign;
            String altTag = (ac.altitudeFt > 0) ? (ac.altitudeFt / 1000f < 10 ? String.format(Locale.US, "%.1fk", ac.altitudeFt / 1000f) : ("FL" + (ac.altitudeFt / 100))) : "";
            canvas.drawText(tag, pt.x, pt.y - dpf(6f), labelPaint);
            if (!altTag.isEmpty()) {
                labelPaint.setColor(0xFFE2E8F0);
                labelPaint.setTextSize(dpf(7f));
                canvas.drawText(altTag, pt.x, pt.y + dpf(12f), labelPaint);
            }

            // Target reticle if selected
            if (selectedTarget != null && selectedTarget.hex.equalsIgnoreCase(ac.hex)) {
                canvas.drawCircle(pt.x, pt.y, dpf(14f), targetReticlePaint);
            }
        }

        // Loop animation sweep
        postInvalidateOnAnimation();
    }
}
