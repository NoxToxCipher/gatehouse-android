package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Line icons for the home screen, drawn in a 24-unit box so they match the
 * dock's drawn icons and need no image assets. One stroke weight, round caps.
 */
public class GlyphView extends View {
    public static final int PHONE = 0, BUBBLE = 1, CAMERA = 2, GLOBE = 3, PLAY = 4, NEWS = 5,
            PIN = 6, GALLERY = 7, SHIELD = 8, CALENDAR = 9, PEOPLE = 10, SLIDERS = 11, GEAR = 12;

    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private int kind = PHONE;

    public GlyphView(Context c, int kind, int color, float strokeDp) {
        super(c);
        this.kind = kind;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setColor(color);
        stroke.setStrokeWidth(strokeDp * getResources().getDisplayMetrics().density);
    }

    public void setColor(int color) { stroke.setColor(color); invalidate(); }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        float ox = (getWidth() - 24f * s) / 2f, oy = (getHeight() - 24f * s) / 2f;
        c.save();
        c.translate(ox, oy);
        c.scale(s, s);
        stroke.setStrokeWidth(stroke.getStrokeWidth() / s);
        path.reset();
        switch (kind) {
            case PHONE:
                path.moveTo(6, 4); path.lineTo(9, 4); path.lineTo(11, 9); path.lineTo(8.5f, 10.5f);
                path.quadTo(10, 14, 13.5f, 15.5f); path.lineTo(15, 13); path.lineTo(20, 15); path.lineTo(20, 18);
                path.quadTo(20, 20, 18, 20); path.quadTo(6, 19, 4, 6); path.quadTo(4, 4, 6, 4);
                c.drawPath(path, stroke);
                break;
            case BUBBLE:
                path.moveTo(4, 6); path.quadTo(4, 4, 6, 4); path.lineTo(18, 4); path.quadTo(20, 4, 20, 6);
                path.lineTo(20, 14); path.quadTo(20, 16, 18, 16); path.lineTo(9, 16); path.lineTo(4, 20); path.close();
                c.drawPath(path, stroke);
                break;
            case CAMERA:
                path.moveTo(4, 8); path.lineTo(7, 8); path.lineTo(9, 5); path.lineTo(15, 5); path.lineTo(17, 8); path.lineTo(20, 8);
                path.lineTo(20, 19); path.lineTo(4, 19); path.close();
                c.drawPath(path, stroke);
                c.drawCircle(12, 13, 3.2f, stroke);
                break;
            case GLOBE:
                c.drawCircle(12, 12, 8.5f, stroke);
                c.drawLine(3.5f, 12, 20.5f, 12, stroke);
                path.moveTo(12, 3.5f); path.cubicTo(16, 7, 16, 17, 12, 20.5f); path.cubicTo(8, 17, 8, 7, 12, 3.5f);
                c.drawPath(path, stroke);
                break;
            case PLAY:
                c.drawRoundRect(new RectF(3, 6, 21, 18), 4, 4, stroke);
                path.moveTo(10, 9); path.lineTo(15, 12); path.lineTo(10, 15); path.close();
                c.drawPath(path, stroke);
                break;
            case NEWS:
                path.moveTo(4, 5); path.lineTo(17, 5); path.lineTo(17, 19); path.lineTo(6, 19); path.quadTo(4, 19, 4, 17); path.close();
                path.moveTo(17, 9); path.lineTo(20, 9); path.lineTo(20, 17); path.quadTo(20, 19, 18, 19);
                c.drawPath(path, stroke);
                c.drawLine(7, 9, 14, 9, stroke); c.drawLine(7, 12, 14, 12, stroke); c.drawLine(7, 15, 11, 15, stroke);
                break;
            case PIN:
                path.moveTo(12, 21); path.cubicTo(8, 16, 6, 13, 6, 10); path.cubicTo(6, 6.7f, 8.7f, 4, 12, 4);
                path.cubicTo(15.3f, 4, 18, 6.7f, 18, 10); path.cubicTo(18, 13, 16, 16, 12, 21); path.close();
                c.drawPath(path, stroke);
                c.drawCircle(12, 10, 2.2f, stroke);
                break;
            case GALLERY:
                c.drawRoundRect(new RectF(4, 5, 20, 19), 2, 2, stroke);
                path.moveTo(4, 16); path.lineTo(9, 11); path.lineTo(13, 15); path.lineTo(16, 12); path.lineTo(20, 16);
                c.drawPath(path, stroke);
                c.drawCircle(16, 9, 1.5f, stroke);
                break;
            case SHIELD:
                path.moveTo(12, 3); path.lineTo(5, 6); path.lineTo(5, 12); path.quadTo(5, 18, 12, 21); path.quadTo(19, 18, 19, 12); path.lineTo(19, 6); path.close();
                c.drawPath(path, stroke);
                path.reset(); path.moveTo(9.5f, 12); path.lineTo(11.3f, 13.8f); path.lineTo(15, 10);
                c.drawPath(path, stroke);
                break;
            case CALENDAR:
                c.drawRoundRect(new RectF(4, 5, 20, 20), 2, 2, stroke);
                c.drawLine(4, 10, 20, 10, stroke); c.drawLine(9, 3, 9, 7, stroke); c.drawLine(15, 3, 15, 7, stroke);
                break;
            case PEOPLE:
                c.drawCircle(10, 9, 3.2f, stroke);
                path.moveTo(4.5f, 19); path.cubicTo(5, 15.5f, 7.5f, 14.4f, 10, 14.4f); path.cubicTo(12.5f, 14.4f, 15, 15.5f, 15.5f, 19);
                c.drawPath(path, stroke);
                path.reset(); path.moveTo(17, 8); path.quadTo(18.5f, 10.5f, 17, 13); c.drawPath(path, stroke);
                path.reset(); path.moveTo(19.5f, 6); path.quadTo(22, 10.5f, 19.5f, 15); c.drawPath(path, stroke);
                break;
            case SLIDERS:
                c.drawLine(4, 8, 20, 8, stroke); c.drawLine(4, 16, 20, 16, stroke);
                Paint fillDark = new Paint(Paint.ANTI_ALIAS_FLAG); fillDark.setColor(0xFF000000);
                c.drawCircle(9, 8, 2.2f, fillDark); c.drawCircle(9, 8, 2.2f, stroke);
                c.drawCircle(15, 16, 2.2f, fillDark); c.drawCircle(15, 16, 2.2f, stroke);
                break;
            case GEAR:
                c.drawCircle(12, 12, 3, stroke);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    c.drawLine((float) (12 + 6.5 * Math.cos(a)), (float) (12 + 6.5 * Math.sin(a)),
                            (float) (12 + 9 * Math.cos(a)), (float) (12 + 9 * Math.sin(a)), stroke);
                }
                break;
        }
        stroke.setStrokeWidth(stroke.getStrokeWidth() * s);
        c.restore();
    }
}
