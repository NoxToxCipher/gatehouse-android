package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public class ModernDockIconView extends View {

    public static final int TYPE_INCIDENT = 0;
    public static final int TYPE_NOTES = 1;
    public static final int TYPE_PHOTO = 2;
    public static final int TYPE_VOICE = 3;
    // Tools tile icons: the instrument set, drawn in the same line weight.
    public static final int TYPE_GEAR = 4;
    public static final int TYPE_BOLT = 5;
    public static final int TYPE_TORCH = 6;
    public static final int TYPE_RADIO = 7;
    public static final int TYPE_SIREN = 8;
    public static final int TYPE_GAUGE = 9;
    public static final int TYPE_COMPASS = 10;
    public static final int TYPE_WEATHER = 11;
    public static final int TYPE_TELESCOPE = 12;
    public static final int TYPE_DISH = 13;
    public static final int TYPE_SPARK = 14;
    public static final int TYPE_FUEL = 15;
    public static final int TYPE_IDCARD = 16;
    public static final int TYPE_SCALES = 17;
    public static final int TYPE_BOOKS = 18;

    /** When false the icon draws with no pod of its own (for use inside a tile's box). */
    private boolean drawPod = true;

    public void setDrawPod(boolean draw) {
        this.drawPod = draw;
        invalidate();
    }

    private int iconType = TYPE_INCIDENT;
    private int primaryColor = 0xFFEF4444;
    private int accentColor = 0xFFFF6B6B;
    private int podBgColor = 0x22EF4444;
    private int podBorderColor = 0x55EF4444;

    private final Paint podBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint podBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF podRect = new RectF();
    private final Path iconPath = new Path();
    private final Path subPath = new Path();

    public ModernDockIconView(Context context) {
        super(context);
        init();
    }

    public ModernDockIconView(Context context, int type, int primaryCol, int accentCol) {
        super(context);
        this.iconType = type;
        this.primaryColor = primaryCol;
        this.accentColor = accentCol;
        initColors();
        init();
    }

    public void setType(int type, int primaryCol, int accentCol) {
        this.iconType = type;
        this.primaryColor = primaryCol;
        this.accentColor = accentCol;
        initColors();
        invalidate();
    }

    private void initColors() {
        this.podBgColor = (primaryColor & 0x00FFFFFF) | 0x20000000;
        this.podBorderColor = (primaryColor & 0x00FFFFFF) | 0x66000000;
    }

    private void init() {
        podBgPaint.setStyle(Paint.Style.FILL);
        podBorderPaint.setStyle(Paint.Style.STROKE);
        podBorderPaint.setStrokeWidth(dpf(1.2f));

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);
    }

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int) dpf(44);
        int w = resolveSize(defaultSize, widthMeasureSpec);
        int h = resolveSize(defaultSize, heightMeasureSpec);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float podSize = Math.min(w, h) - dpf(4);
        float podLeft = (w - podSize) / 2f;
        float podTop = (h - podSize) / 2f;
        podRect.set(podLeft, podTop, podLeft + podSize, podTop + podSize);
        float podRadius = dpf(12);

        // 1. Frosted Pod Background & Subtle Border (skipped inside a tile box)
        if (drawPod) {
            podBgPaint.setColor(podBgColor);
            canvas.drawRoundRect(podRect, podRadius, podRadius, podBgPaint);

            podBorderPaint.setColor(podBorderColor);
            canvas.drawRoundRect(podRect, podRadius, podRadius, podBorderPaint);
        }

        float cx = w / 2f;
        float cy = h / 2f;
        float r = podSize * 0.44f;

        strokePaint.setColor(primaryColor);
        strokePaint.setStrokeWidth(dpf(1.8f));
        fillPaint.setColor(accentColor);

        switch (iconType) {
            case TYPE_INCIDENT:
                drawIncident(canvas, cx, cy, r);
                break;
            case TYPE_NOTES:
                drawNotes(canvas, cx, cy, r);
                break;
            case TYPE_PHOTO:
                drawPhoto(canvas, cx, cy, r);
                break;
            case TYPE_VOICE:
                drawVoice(canvas, cx, cy, r);
                break;
            case TYPE_GEAR:      drawGear(canvas, cx, cy, r); break;
            case TYPE_BOLT:      drawBolt(canvas, cx, cy, r); break;
            case TYPE_TORCH:     drawTorch(canvas, cx, cy, r); break;
            case TYPE_RADIO:     drawRadio(canvas, cx, cy, r); break;
            case TYPE_SIREN:     drawSiren(canvas, cx, cy, r); break;
            case TYPE_GAUGE:     drawGauge(canvas, cx, cy, r); break;
            case TYPE_COMPASS:   drawCompass(canvas, cx, cy, r); break;
            case TYPE_WEATHER:   drawWeather(canvas, cx, cy, r); break;
            case TYPE_TELESCOPE: drawTelescope(canvas, cx, cy, r); break;
            case TYPE_DISH:      drawDish(canvas, cx, cy, r); break;
            case TYPE_SPARK:     drawSpark(canvas, cx, cy, r); break;
            case TYPE_FUEL:      drawFuel(canvas, cx, cy, r); break;
            case TYPE_IDCARD:    drawIdCard(canvas, cx, cy, r); break;
            case TYPE_SCALES:    drawScales(canvas, cx, cy, r); break;
            case TYPE_BOOKS:     drawBooks(canvas, cx, cy, r); break;
        }
    }

    /** Map a tile's emoji glyph to a drawn type, or -1 to keep the glyph. */
    public static int typeForGlyph(String glyph) {
        if (glyph == null) return -1;
        String g = glyph.replace("️", "").trim();
        switch (g) {
            case "⚙":             return TYPE_GEAR;       // gear
            case "⚡":             return TYPE_BOLT;       // high voltage
            case "🔦":       return TYPE_TORCH;      // flashlight
            case "📻":       return TYPE_RADIO;      // radio
            case "🚨":       return TYPE_SIREN;      // police light
            case "🎛":       return TYPE_GAUGE;      // control knobs
            case "🧭":       return TYPE_COMPASS;    // compass
            case "🌤":       return TYPE_WEATHER;    // sun behind small cloud
            case "🔭":       return TYPE_TELESCOPE;  // telescope
            case "📡":       return TYPE_DISH;       // satellite antenna
            case "✨":             return TYPE_SPARK;      // sparkles
            case "⛽":             return TYPE_FUEL;       // fuel pump
            case "🪪":       return TYPE_IDCARD;     // identification card
            case "⚖":             return TYPE_SCALES;     // scales
            case "📚":       return TYPE_BOOKS;      // books
            default:                   return -1;
        }
    }

    private void prep() {
        strokePaint.setColor(primaryColor);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpf(1.8f));
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    private void drawGear(Canvas c, float cx, float cy, float r) {
        prep();
        c.drawCircle(cx, cy, r * 0.5f, strokePaint);
        c.drawCircle(cx, cy, r * 0.18f, strokePaint);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            float x1 = cx + (float) Math.cos(a) * r * 0.58f, y1 = cy + (float) Math.sin(a) * r * 0.58f;
            float x2 = cx + (float) Math.cos(a) * r * 0.86f, y2 = cy + (float) Math.sin(a) * r * 0.86f;
            c.drawLine(x1, y1, x2, y2, strokePaint);
        }
    }

    private void drawBolt(Canvas c, float cx, float cy, float r) {
        prep();
        iconPath.reset();
        iconPath.moveTo(cx + r * 0.18f, cy - r * 0.85f);
        iconPath.lineTo(cx - r * 0.38f, cy + r * 0.08f);
        iconPath.lineTo(cx + r * 0.04f, cy + r * 0.08f);
        iconPath.lineTo(cx - r * 0.18f, cy + r * 0.85f);
        iconPath.lineTo(cx + r * 0.38f, cy - r * 0.08f);
        iconPath.lineTo(cx - r * 0.04f, cy - r * 0.08f);
        iconPath.close();
        c.drawPath(iconPath, strokePaint);
    }

    private void drawTorch(Canvas c, float cx, float cy, float r) {
        prep();
        iconPath.reset();
        iconPath.moveTo(cx - r * 0.42f, cy - r * 0.75f);
        iconPath.lineTo(cx + r * 0.42f, cy - r * 0.75f);
        iconPath.lineTo(cx + r * 0.22f, cy - r * 0.15f);
        iconPath.lineTo(cx - r * 0.22f, cy - r * 0.15f);
        iconPath.close();
        c.drawPath(iconPath, strokePaint);
        RectF body = new RectF(cx - r * 0.22f, cy - r * 0.15f, cx + r * 0.22f, cy + r * 0.85f);
        c.drawRoundRect(body, r * 0.1f, r * 0.1f, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawLine(cx, cy - r * 0.92f, cx, cy - r * 1.0f, strokePaint);
        c.drawLine(cx - r * 0.3f, cy - r * 0.9f, cx - r * 0.36f, cy - r * 0.98f, strokePaint);
        c.drawLine(cx + r * 0.3f, cy - r * 0.9f, cx + r * 0.36f, cy - r * 0.98f, strokePaint);
    }

    private void drawRadio(Canvas c, float cx, float cy, float r) {
        prep();
        RectF box = new RectF(cx - r * 0.85f, cy - r * 0.3f, cx + r * 0.85f, cy + r * 0.7f);
        c.drawRoundRect(box, r * 0.15f, r * 0.15f, strokePaint);
        c.drawLine(cx + r * 0.3f, cy - r * 0.3f, cx + r * 0.8f, cy - r * 0.85f, strokePaint);
        c.drawCircle(cx - r * 0.4f, cy + r * 0.2f, r * 0.24f, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawLine(cx + r * 0.15f, cy + r * 0.05f, cx + r * 0.6f, cy + r * 0.05f, strokePaint);
        c.drawLine(cx + r * 0.15f, cy + r * 0.35f, cx + r * 0.6f, cy + r * 0.35f, strokePaint);
    }

    private void drawSiren(Canvas c, float cx, float cy, float r) {
        prep();
        RectF dome = new RectF(cx - r * 0.55f, cy - r * 0.5f, cx + r * 0.55f, cy + r * 0.6f);
        c.drawArc(dome, 180, 180, false, strokePaint);
        c.drawLine(cx - r * 0.55f, cy + r * 0.05f, cx - r * 0.55f, cy + r * 0.45f, strokePaint);
        c.drawLine(cx + r * 0.55f, cy + r * 0.05f, cx + r * 0.55f, cy + r * 0.45f, strokePaint);
        c.drawLine(cx - r * 0.8f, cy + r * 0.45f, cx + r * 0.8f, cy + r * 0.45f, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawLine(cx, cy - r * 0.7f, cx, cy - r * 0.95f, strokePaint);
        c.drawLine(cx - r * 0.5f, cy - r * 0.55f, cx - r * 0.68f, cy - r * 0.75f, strokePaint);
        c.drawLine(cx + r * 0.5f, cy - r * 0.55f, cx + r * 0.68f, cy - r * 0.75f, strokePaint);
    }

    private void drawGauge(Canvas c, float cx, float cy, float r) {
        prep();
        RectF arc = new RectF(cx - r * 0.85f, cy - r * 0.6f, cx + r * 0.85f, cy + r * 1.1f);
        c.drawArc(arc, 200, 140, false, strokePaint);
        c.drawLine(cx, cy + r * 0.25f, cx + r * 0.45f, cy - r * 0.3f, strokePaint);
        fillPaint.setColor(accentColor);
        c.drawCircle(cx, cy + r * 0.25f, r * 0.1f, fillPaint);
    }

    private void drawCompass(Canvas c, float cx, float cy, float r) {
        prep();
        c.drawCircle(cx, cy, r * 0.82f, strokePaint);
        iconPath.reset();
        iconPath.moveTo(cx, cy - r * 0.55f);
        iconPath.lineTo(cx + r * 0.2f, cy);
        iconPath.lineTo(cx, cy + r * 0.55f);
        iconPath.lineTo(cx - r * 0.2f, cy);
        iconPath.close();
        c.drawPath(iconPath, strokePaint);
        subPath.reset();
        subPath.moveTo(cx, cy - r * 0.55f);
        subPath.lineTo(cx + r * 0.2f, cy);
        subPath.lineTo(cx - r * 0.2f, cy);
        subPath.close();
        fillPaint.setColor(accentColor);
        c.drawPath(subPath, fillPaint);
    }

    private void drawWeather(Canvas c, float cx, float cy, float r) {
        prep();
        float sx = cx - r * 0.3f, sy = cy - r * 0.3f;
        c.drawCircle(sx, sy, r * 0.28f, strokePaint);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(-90 + i * 45 - 45);
            c.drawLine(sx + (float) Math.cos(a) * r * 0.4f, sy + (float) Math.sin(a) * r * 0.4f,
                       sx + (float) Math.cos(a) * r * 0.58f, sy + (float) Math.sin(a) * r * 0.58f, strokePaint);
        }
        strokePaint.setColor(accentColor);
        RectF cloud = new RectF(cx - r * 0.35f, cy + r * 0.05f, cx + r * 0.85f, cy + r * 0.6f);
        c.drawRoundRect(cloud, r * 0.28f, r * 0.28f, strokePaint);
        c.drawArc(new RectF(cx - r * 0.05f, cy - r * 0.25f, cx + r * 0.55f, cy + r * 0.35f), 180, 180, false, strokePaint);
    }

    private void drawTelescope(Canvas c, float cx, float cy, float r) {
        prep();
        strokePaint.setStrokeWidth(dpf(3.4f));
        c.drawLine(cx - r * 0.55f, cy + r * 0.2f, cx + r * 0.6f, cy - r * 0.55f, strokePaint);
        strokePaint.setStrokeWidth(dpf(1.8f));
        c.drawLine(cx, cy - r * 0.1f, cx - r * 0.35f, cy + r * 0.85f, strokePaint);
        c.drawLine(cx, cy - r * 0.1f, cx + r * 0.35f, cy + r * 0.85f, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawCircle(cx + r * 0.68f, cy - r * 0.6f, r * 0.14f, strokePaint);
    }

    private void drawDish(Canvas c, float cx, float cy, float r) {
        prep();
        RectF bowl = new RectF(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f);
        c.drawArc(bowl, 130, 160, false, strokePaint);
        c.drawLine(cx, cy + r * 0.2f, cx, cy + r * 0.8f, strokePaint);
        c.drawLine(cx - r * 0.35f, cy + r * 0.8f, cx + r * 0.35f, cy + r * 0.8f, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawLine(cx, cy + r * 0.1f, cx + r * 0.4f, cy - r * 0.45f, strokePaint);
        fillPaint.setColor(accentColor);
        c.drawCircle(cx + r * 0.45f, cy - r * 0.52f, r * 0.1f, fillPaint);
    }

    private void drawSpark(Canvas c, float cx, float cy, float r) {
        prep();
        RectF body = new RectF(cx - r * 0.22f, cy - r * 0.22f, cx + r * 0.22f, cy + r * 0.22f);
        c.drawRoundRect(body, r * 0.06f, r * 0.06f, strokePaint);
        c.drawLine(cx - r * 0.22f, cy, cx - r * 0.38f, cy, strokePaint);
        c.drawLine(cx + r * 0.22f, cy, cx + r * 0.38f, cy, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawRect(cx - r * 0.9f, cy - r * 0.18f, cx - r * 0.38f, cy + r * 0.18f, strokePaint);
        c.drawRect(cx + r * 0.38f, cy - r * 0.18f, cx + r * 0.9f, cy + r * 0.18f, strokePaint);
    }

    private void drawFuel(Canvas c, float cx, float cy, float r) {
        prep();
        RectF pump = new RectF(cx - r * 0.6f, cy - r * 0.8f, cx + r * 0.2f, cy + r * 0.8f);
        c.drawRoundRect(pump, r * 0.1f, r * 0.1f, strokePaint);
        c.drawLine(cx - r * 0.75f, cy + r * 0.8f, cx + r * 0.35f, cy + r * 0.8f, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawRect(cx - r * 0.42f, cy - r * 0.6f, cx + r * 0.02f, cy - r * 0.2f, strokePaint);
        iconPath.reset();
        iconPath.moveTo(cx + r * 0.2f, cy - r * 0.35f);
        iconPath.lineTo(cx + r * 0.55f, cy - r * 0.35f);
        iconPath.lineTo(cx + r * 0.55f, cy + r * 0.3f);
        iconPath.lineTo(cx + r * 0.7f, cy + r * 0.3f);
        c.drawPath(iconPath, strokePaint);
    }

    private void drawIdCard(Canvas c, float cx, float cy, float r) {
        prep();
        RectF card = new RectF(cx - r * 0.88f, cy - r * 0.55f, cx + r * 0.88f, cy + r * 0.55f);
        c.drawRoundRect(card, r * 0.12f, r * 0.12f, strokePaint);
        c.drawCircle(cx - r * 0.45f, cy - r * 0.12f, r * 0.16f, strokePaint);
        c.drawArc(new RectF(cx - r * 0.72f, cy + r * 0.05f, cx - r * 0.18f, cy + r * 0.5f), 180, 180, false, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawLine(cx + r * 0.05f, cy - r * 0.15f, cx + r * 0.62f, cy - r * 0.15f, strokePaint);
        c.drawLine(cx + r * 0.05f, cy + r * 0.15f, cx + r * 0.45f, cy + r * 0.15f, strokePaint);
    }

    private void drawScales(Canvas c, float cx, float cy, float r) {
        prep();
        c.drawLine(cx, cy - r * 0.7f, cx, cy + r * 0.6f, strokePaint);
        c.drawLine(cx - r * 0.4f, cy + r * 0.75f, cx + r * 0.4f, cy + r * 0.75f, strokePaint);
        c.drawLine(cx - r * 0.7f, cy - r * 0.45f, cx + r * 0.7f, cy - r * 0.45f, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawLine(cx - r * 0.7f, cy - r * 0.45f, cx - r * 0.7f, cy - r * 0.05f, strokePaint);
        c.drawLine(cx + r * 0.7f, cy - r * 0.45f, cx + r * 0.7f, cy - r * 0.05f, strokePaint);
        c.drawArc(new RectF(cx - r * 0.95f, cy - r * 0.35f, cx - r * 0.45f, cy + r * 0.25f), 0, 180, false, strokePaint);
        c.drawArc(new RectF(cx + r * 0.45f, cy - r * 0.35f, cx + r * 0.95f, cy + r * 0.25f), 0, 180, false, strokePaint);
    }

    private void drawBooks(Canvas c, float cx, float cy, float r) {
        prep();
        c.drawRoundRect(new RectF(cx - r * 0.75f, cy - r * 0.55f, cx - r * 0.32f, cy + r * 0.75f), r * 0.05f, r * 0.05f, strokePaint);
        c.drawRoundRect(new RectF(cx - r * 0.2f, cy - r * 0.78f, cx + r * 0.2f, cy + r * 0.75f), r * 0.05f, r * 0.05f, strokePaint);
        c.drawRoundRect(new RectF(cx + r * 0.32f, cy - r * 0.5f, cx + r * 0.75f, cy + r * 0.75f), r * 0.05f, r * 0.05f, strokePaint);
        strokePaint.setColor(accentColor);
        c.drawLine(cx - r * 0.2f, cy - r * 0.45f, cx + r * 0.2f, cy - r * 0.45f, strokePaint);
        c.drawLine(cx - r * 0.75f, cy - r * 0.25f, cx - r * 0.32f, cy - r * 0.25f, strokePaint);
    }

    private void drawIncident(Canvas canvas, float cx, float cy, float r) {
        iconPath.reset();
        float topY = cy - r * 0.72f;
        float botY = cy + r * 0.65f;
        float halfBase = r * 0.78f;

        iconPath.moveTo(cx, topY);
        iconPath.lineTo(cx + halfBase, botY);
        iconPath.lineTo(cx - halfBase, botY);
        iconPath.close();

        strokePaint.setStrokeWidth(dpf(2.0f));
        canvas.drawPath(iconPath, strokePaint);

        strokePaint.setColor(accentColor);
        strokePaint.setStrokeWidth(dpf(2.2f));
        canvas.drawLine(cx, cy - r * 0.28f, cx, cy + r * 0.14f, strokePaint);

        canvas.drawCircle(cx, cy + r * 0.38f, dpf(1.4f), fillPaint);
    }

    private void drawNotes(Canvas canvas, float cx, float cy, float r) {
        float docLeft = cx - r * 0.62f;
        float docTop = cy - r * 0.72f;
        float docRight = cx + r * 0.38f;
        float docBottom = cy + r * 0.72f;
        float foldSize = r * 0.32f;

        iconPath.reset();
        iconPath.moveTo(docLeft, docTop);
        iconPath.lineTo(docRight - foldSize, docTop);
        iconPath.lineTo(docRight, docTop + foldSize);
        iconPath.lineTo(docRight, docBottom);
        iconPath.lineTo(docLeft, docBottom);
        iconPath.close();

        strokePaint.setStrokeWidth(dpf(1.7f));
        canvas.drawPath(iconPath, strokePaint);

        subPath.reset();
        subPath.moveTo(docRight - foldSize, docTop);
        subPath.lineTo(docRight - foldSize, docTop + foldSize);
        subPath.lineTo(docRight, docTop + foldSize);
        canvas.drawPath(subPath, strokePaint);

        strokePaint.setColor(accentColor);
        strokePaint.setStrokeWidth(dpf(1.4f));
        float lineX1 = docLeft + dpf(3.5f);
        float lineX2 = docRight - dpf(4f);
        canvas.drawLine(lineX1, cy - r * 0.18f, lineX2, cy - r * 0.18f, strokePaint);
        canvas.drawLine(lineX1, cy + r * 0.08f, lineX2, cy + r * 0.08f, strokePaint);
        canvas.drawLine(lineX1, cy + r * 0.34f, lineX1 + (lineX2 - lineX1) * 0.55f, cy + r * 0.34f, strokePaint);

        strokePaint.setColor(primaryColor);
        strokePaint.setStrokeWidth(dpf(1.8f));
        canvas.drawLine(cx + r * 0.68f, cy + r * 0.15f, cx + r * 0.22f, cy + r * 0.65f, strokePaint);
        canvas.drawCircle(cx + r * 0.20f, cy + r * 0.67f, dpf(1.2f), fillPaint);
    }

    private void drawPhoto(Canvas canvas, float cx, float cy, float r) {
        float camW = r * 1.55f;
        float camH = r * 1.05f;
        float camLeft = cx - camW / 2f;
        float camTop = cy - camH / 2f + dpf(1.5f);
        RectF camBody = new RectF(camLeft, camTop, camLeft + camW, camTop + camH);

        strokePaint.setStrokeWidth(dpf(1.8f));
        canvas.drawRoundRect(camBody, dpf(4), dpf(4), strokePaint);

        float notchW = r * 0.52f;
        float notchH = dpf(3.2f);
        RectF notch = new RectF(cx - notchW / 2f, camTop - notchH, cx + notchW / 2f, camTop + dpf(1));
        canvas.drawRoundRect(notch, dpf(2), dpf(2), strokePaint);

        float lensR = r * 0.38f;
        strokePaint.setColor(accentColor);
        strokePaint.setStrokeWidth(dpf(1.8f));
        canvas.drawCircle(cx, cy + dpf(1.5f), lensR, strokePaint);

        canvas.drawCircle(cx + lensR * 0.38f, cy + dpf(1.5f) - lensR * 0.38f, dpf(1.4f), fillPaint);

        fillPaint.setColor(accentColor);
        canvas.drawCircle(camLeft + dpf(4f), camTop + dpf(4f), dpf(1.3f), fillPaint);
    }

    private void drawVoice(Canvas canvas, float cx, float cy, float r) {
        float micW = r * 0.44f;
        float micH = r * 0.85f;
        float micLeft = cx - micW / 2f;
        float micTop = cy - r * 0.65f;
        RectF micCap = new RectF(micLeft, micTop, micLeft + micW, micTop + micH);

        strokePaint.setStrokeWidth(dpf(1.8f));
        canvas.drawRoundRect(micCap, micW / 2f, micW / 2f, strokePaint);

        strokePaint.setColor(accentColor);
        strokePaint.setStrokeWidth(dpf(1.2f));
        canvas.drawLine(micLeft + dpf(1), micTop + micH * 0.42f, micLeft + micW - dpf(1), micTop + micH * 0.42f, strokePaint);

        strokePaint.setColor(primaryColor);
        strokePaint.setStrokeWidth(dpf(1.8f));
        float cradleR = r * 0.42f;
        RectF cradleRect = new RectF(cx - cradleR, cy - r * 0.15f, cx + cradleR, cy + r * 0.45f);
        canvas.drawArc(cradleRect, 0, 180, false, strokePaint);

        canvas.drawLine(cx, cy + r * 0.45f, cx, cy + r * 0.72f, strokePaint);
        canvas.drawLine(cx - r * 0.32f, cy + r * 0.72f, cx + r * 0.32f, cy + r * 0.72f, strokePaint);

        strokePaint.setColor(accentColor);
        strokePaint.setStrokeWidth(dpf(1.4f));

        RectF leftWave = new RectF(cx - r * 0.88f, cy - r * 0.52f, cx - r * 0.38f, cy + r * 0.08f);
        canvas.drawArc(leftWave, 120, 120, false, strokePaint);

        RectF rightWave = new RectF(cx + r * 0.38f, cy - r * 0.52f, cx + r * 0.88f, cy + r * 0.08f);
        canvas.drawArc(rightWave, 300, 120, false, strokePaint);
    }
}