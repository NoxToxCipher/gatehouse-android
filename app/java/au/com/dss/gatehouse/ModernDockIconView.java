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

        // 1. Frosted Pod Background & Subtle Border
        podBgPaint.setColor(podBgColor);
        canvas.drawRoundRect(podRect, podRadius, podRadius, podBgPaint);

        podBorderPaint.setColor(podBorderColor);
        canvas.drawRoundRect(podRect, podRadius, podRadius, podBorderPaint);

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
        }
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