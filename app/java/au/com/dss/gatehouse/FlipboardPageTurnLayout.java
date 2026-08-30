package au.com.dss.gatehouse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

public class FlipboardPageTurnLayout extends FrameLayout {

    public interface OnPageTurnListener {
        void onPageFlipped(boolean toCarbon);
    }

    private View underneathView;
    private View topView;
    private OnPageTurnListener pageTurnListener;
    private boolean isCarbonCopyMode = false;

    private boolean isTurning = false;
    private float flipProgress = 0f; // 0.0 (Closed/Flat) to 1.0 (Fully Flipped)
    private float startTouchX = 0f;
    private float startTouchY = 0f;
    private VelocityTracker velocityTracker;
    private ValueAnimator flipAnimator;

    private final Camera camera3D = new Camera();
    private final Matrix matrix3D = new Matrix();
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backSheetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spineShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint binderHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint binderHoleInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dogEarHintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dogEarTabPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap topViewBitmap = null;
    private Bitmap underneathViewBitmap = null;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public FlipboardPageTurnLayout(Context context) {
        super(context);
        setWillNotDraw(false);
        setClipChildren(false);

        shadowPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setStyle(Paint.Style.FILL);
        backSheetPaint.setStyle(Paint.Style.FILL);
        spineShadowPaint.setStyle(Paint.Style.FILL);

        binderHolePaint.setColor(0xFF070A12);
        binderHolePaint.setStyle(Paint.Style.FILL);

        binderHoleInner.setColor(0x44FFFFFF);
        binderHoleInner.setStyle(Paint.Style.STROKE);
        binderHoleInner.setStrokeWidth(dpf(1f));

        dogEarHintPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        dogEarTabPaint.setStyle(Paint.Style.FILL);
    }

    public void setPageTurnListener(OnPageTurnListener listener) {
        this.pageTurnListener = listener;
    }

    public void setPages(View under, View top, boolean isCarbon) {
        this.underneathView = under;
        this.topView = top;
        this.isCarbonCopyMode = isCarbon;
        removeAllViews();
        if (underneathView != null) addView(underneathView);
        if (topView != null) addView(topView);
        this.flipProgress = 0f;
        this.isTurning = false;
        recycleBitmaps();
        invalidate();
    }

    private void recycleBitmaps() {
        if (topViewBitmap != null && !topViewBitmap.isRecycled()) {
            topViewBitmap.recycle();
            topViewBitmap = null;
        }
        if (underneathViewBitmap != null && !underneathViewBitmap.isRecycled()) {
            underneathViewBitmap.recycle();
            underneathViewBitmap = null;
        }
    }

    private void capturePageBitmaps() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        try {
            if (topView != null) {
                if (topViewBitmap == null || topViewBitmap.getWidth() != w || topViewBitmap.getHeight() != h) {
                    topViewBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                }
                Canvas c = new Canvas(topViewBitmap);
                topView.draw(c);
            }
            if (underneathView != null) {
                if (underneathViewBitmap == null || underneathViewBitmap.getWidth() != w || underneathViewBitmap.getHeight() != h) {
                    underneathViewBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                }
                Canvas c = new Canvas(underneathViewBitmap);
                underneathView.draw(c);
            }
        } catch (Exception e) {}
    }

    public void triggerFlipAnimation(final boolean toCarbon) {
        if (flipAnimator != null && flipAnimator.isRunning()) flipAnimator.cancel();
        capturePageBitmaps();
        isTurning = true;

        flipAnimator = ValueAnimator.ofFloat(0f, 1f);
        flipAnimator.setDuration(380);
        flipAnimator.setInterpolator(new DecelerateInterpolator(1.4f));
        flipAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator va) {
                flipProgress = (Float) va.getAnimatedValue();
                invalidate();
            }
        });
        flipAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isTurning = false;
                flipProgress = 0f;
                if (pageTurnListener != null) {
                    pageTurnListener.onPageFlipped(toCarbon);
                }
            }
        });
        flipAnimator.start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        float w = getWidth();
        if (w <= 0) return false;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startTouchX = ev.getX();
                startTouchY = ev.getY();
                if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
                velocityTracker.clear();
                velocityTracker.addMovement(ev);
                isTurning = false;
                break;

            case MotionEvent.ACTION_MOVE:
                if (velocityTracker != null) velocityTracker.addMovement(ev);
                float dx = ev.getX() - startTouchX;
                float dy = Math.abs(ev.getY() - startTouchY);

                if (!isCarbonCopyMode && dx < -dpf(10) && Math.abs(dx) > dy * 0.65f) {
                    isTurning = true;
                    capturePageBitmaps();
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                if (isCarbonCopyMode && dx > dpf(10) && Math.abs(dx) > dy * 0.65f) {
                    isTurning = true;
                    capturePageBitmaps();
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final float w = getWidth();
        final float h = getHeight();
        if (w <= 0 || h <= 0) return super.onTouchEvent(ev);

        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startTouchX = ev.getX();
                startTouchY = ev.getY();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = ev.getX() - startTouchX;
                float dy = Math.abs(ev.getY() - startTouchY);

                if (!isTurning) {
                    if (!isCarbonCopyMode && dx < -dpf(8) && Math.abs(dx) > dy * 0.65f) {
                        isTurning = true;
                        capturePageBitmaps();
                        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    } else if (isCarbonCopyMode && dx > dpf(8) && Math.abs(dx) > dy * 0.65f) {
                        isTurning = true;
                        capturePageBitmaps();
                        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }

                if (isTurning) {
                    if (!isCarbonCopyMode) {
                        flipProgress = Math.max(0f, Math.min(1f, -dx / w));
                    } else {
                        flipProgress = Math.max(0f, Math.min(1f, dx / w));
                    }
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isTurning) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float vx = velocityTracker.getXVelocity();
                    final boolean shouldComplete;

                    if (!isCarbonCopyMode) {
                        // Original sheet: swipe left turns to Carbon
                        shouldComplete = vx < -dpf(400) || flipProgress > 0.42f;
                    } else {
                        // Carbon sheet: swipe right turns to Original
                        shouldComplete = vx > dpf(400) || flipProgress > 0.42f;
                    }

                    if (flipAnimator != null && flipAnimator.isRunning()) flipAnimator.cancel();
                    final float fromP = flipProgress;
                    final float targetP = shouldComplete ? 1f : 0f;

                    flipAnimator = ValueAnimator.ofFloat(fromP, targetP);
                    flipAnimator.setDuration(Math.max(160, (int) (320 * Math.abs(targetP - fromP))));
                    flipAnimator.setInterpolator(shouldComplete ? new DecelerateInterpolator(1.3f) : new OvershootInterpolator(1.08f));
                    flipAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator va) {
                            flipProgress = (Float) va.getAnimatedValue();
                            invalidate();
                        }
                    });
                    flipAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            isTurning = false;
                            flipProgress = 0f;
                            if (shouldComplete && pageTurnListener != null) {
                                pageTurnListener.onPageFlipped(!isCarbonCopyMode);
                            }
                        }
                    });
                    flipAnimator.start();

                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    return true;
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!isTurning || topViewBitmap == null || underneathViewBitmap == null) {
            super.dispatchDraw(canvas);
            drawBinderPunchHoles(canvas);
            drawCornerHints(canvas);
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Draw Revealed Base Page Underneath
        canvas.drawBitmap(underneathViewBitmap, 0, 0, null);

        // 2. Compute 3D Flipboard Rotation Angle (0 deg = flat, 90 deg = upright, 180 deg = flipped)
        float rotationAngle = (!isCarbonCopyMode ? -1f : 1f) * (flipProgress * 180f);
        float sinAngle = (float) Math.sin(flipProgress * Math.PI);

        // 3. Dynamic Drop Shadow Cast onto Underneath Page
        if (sinAngle > 0.01f) {
            int shadowAlpha = (int) (sinAngle * 160);
            float shadowW = w * (0.15f + sinAngle * 0.45f);
            if (!isCarbonCopyMode) {
                Shader shadowShader = new LinearGradient(
                        0, 0, shadowW, 0,
                        new int[]{(shadowAlpha << 24) | 0x000000, 0x00000000},
                        new float[]{0f, 1f},
                        Shader.TileMode.CLAMP);
                shadowPaint.setShader(shadowShader);
                canvas.drawRect(0, 0, shadowW, h, shadowPaint);
            } else {
                Shader shadowShader = new LinearGradient(
                        w - shadowW, 0, w, 0,
                        new int[]{0x00000000, (shadowAlpha << 24) | 0x000000},
                        new float[]{0f, 1f},
                        Shader.TileMode.CLAMP);
                shadowPaint.setShader(shadowShader);
                canvas.drawRect(w - shadowW, 0, w, h, shadowPaint);
            }
        }

        // 4. 3D Turning Sheet Transformation
        canvas.save();
        camera3D.save();

        float cameraDist = -dpf(12000);
        camera3D.setLocation(0, 0, cameraDist / getResources().getDisplayMetrics().density);

        if (flipProgress <= 0.5f) {
            // FRONT OF SHEET VISIBLE (0 deg to 90 deg)
            camera3D.rotateY(rotationAngle);
            camera3D.getMatrix(matrix3D);

            float pivotX = !isCarbonCopyMode ? 0 : w;
            matrix3D.preTranslate(-pivotX, -h * 0.5f);
            matrix3D.postTranslate(pivotX, h * 0.5f);

            canvas.concat(matrix3D);

            // Draw front bitmap
            canvas.drawBitmap(topViewBitmap, 0, 0, null);

            // Diffuse Lighting Falloff on turning front sheet
            if (sinAngle > 0.01f) {
                int lightDim = (int) (sinAngle * 110);
                highlightPaint.setColor((lightDim << 24) | 0x000000);
                canvas.drawRect(0, 0, w, h, highlightPaint);

                // Moving Specular Ridge Highlight along the curved lift
                float specX = !isCarbonCopyMode ? (w * (1f - flipProgress * 1.5f)) : (w * flipProgress * 1.5f);
                Shader specShader = new LinearGradient(
                        specX - dpf(40), 0, specX + dpf(40), 0,
                        new int[]{0x00FFFFFF, (int) (sinAngle * 70) << 24 | 0xFFFFFF, 0x00FFFFFF},
                        new float[]{0f, 0.5f, 1f},
                        Shader.TileMode.CLAMP);
                shadowPaint.setShader(specShader);
                canvas.drawRect(0, 0, w, h, shadowPaint);
            }
        } else {
            // BACK OF SHEET VISIBLE (90 deg to 180 deg)
            camera3D.rotateY(rotationAngle + (!isCarbonCopyMode ? 180f : -180f));
            camera3D.getMatrix(matrix3D);

            float pivotX = !isCarbonCopyMode ? 0 : w;
            matrix3D.preTranslate(-pivotX, -h * 0.5f);
            matrix3D.postTranslate(pivotX, h * 0.5f);

            canvas.concat(matrix3D);

            // Draw authentic reverse carbon/security backing
            backSheetPaint.setColor(!isCarbonCopyMode ? 0xFF1E293B : 0xFF2A220A);
            canvas.drawRoundRect(new RectF(0, 0, w, h), dpf(16), dpf(16), backSheetPaint);

            // Feint reverse ruled lines on back
            shadowPaint.setColor(!isCarbonCopyMode ? 0x2238BDF8 : 0x33FDE047);
            shadowPaint.setShader(null);
            shadowPaint.setStrokeWidth(dpf(1.2f));
            for (float y = dpf(40); y < h - dpf(30); y += dpf(26)) {
                canvas.drawLine(dpf(20), y, w - dpf(20), y, shadowPaint);
            }

            // Authentic DSS Watermark Stamp on reverse
            dogEarHintPaint.setColor(!isCarbonCopyMode ? 0x4494A3B8 : 0x55FEF08A);
            dogEarHintPaint.setTextSize(dpf(14));
            dogEarHintPaint.setTextAlign(Paint.Align.CENTER);
            String watermark = !isCarbonCopyMode ? "DSS SECURITY LOGBOOK · OFFICIAL ORIGINAL" : "DSS SECURITY LOGBOOK · CANARY CARBON DUPLICATE";
            canvas.drawText(watermark, w * 0.5f, h * 0.5f, dogEarHintPaint);

            // Shading as it lands on opposite side
            int landingShade = (int) (sinAngle * 120);
            highlightPaint.setColor((landingShade << 24) | 0x000000);
            canvas.drawRect(0, 0, w, h, highlightPaint);
        }

        camera3D.restore();
        canvas.restore();

        // 5. Left Spine Binding Shadow
        Shader spineShader = new LinearGradient(
                0, 0, dpf(18), 0,
                new int[]{0xAA000000, 0x00000000},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP);
        spineShadowPaint.setShader(spineShader);
        canvas.drawRect(0, 0, dpf(18), h, spineShadowPaint);

        // 6. Draw Binder Punch Holes
        drawBinderPunchHoles(canvas);
    }

    private void drawBinderPunchHoles(Canvas canvas) {
        float h = getHeight();
        if (h <= 0) return;

        // Realistic punched binder oval holes along the left spine
        float holeX = dpf(6f);
        float holeW = dpf(5f);
        float holeH = dpf(14f);

        float[] holeYs = {h * 0.18f, h * 0.50f, h * 0.82f};
        for (float y : holeYs) {
            RectF holeRect = new RectF(holeX - holeW / 2f, y - holeH / 2f, holeX + holeW / 2f, y + holeH / 2f);
            canvas.drawRoundRect(holeRect, dpf(2.5f), dpf(2.5f), binderHolePaint);
            canvas.drawRoundRect(holeRect, dpf(2.5f), dpf(2.5f), binderHoleInner);
        }
    }

    private void drawCornerHints(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (!isCarbonCopyMode) {
            // Dog ear on bottom right: "🟡 FLIP PAGE (R→L)"
            float earSize = dpf(38f);
            Path ear = new Path();
            ear.moveTo(w - earSize, h);
            ear.lineTo(w, h - earSize);
            ear.lineTo(w, h);
            ear.close();
            dogEarTabPaint.setColor(0xFF0284C7);
            canvas.drawPath(ear, dogEarTabPaint);

            dogEarHintPaint.setTextAlign(Paint.Align.RIGHT);
            dogEarHintPaint.setTextSize(dpf(9.5f));
            dogEarHintPaint.setColor(0xFF38BDF8);
            canvas.drawText("🟡 FLIP PAGE (R→L) ◂", w - dpf(14), h - dpf(10), dogEarHintPaint);
        } else {
            // Dog ear on bottom left: "▸ (L→R) ORIGINAL SHEET"
            float earSize = dpf(38f);
            Path ear = new Path();
            ear.moveTo(0, h - earSize);
            ear.lineTo(earSize, h);
            ear.lineTo(0, h);
            ear.close();
            dogEarTabPaint.setColor(0xFFCA8A04);
            canvas.drawPath(ear, dogEarTabPaint);

            dogEarHintPaint.setTextAlign(Paint.Align.LEFT);
            dogEarHintPaint.setTextSize(dpf(9.5f));
            dogEarHintPaint.setColor(0xFFFEF08A);
            canvas.drawText("▸ (L→R) ORIGINAL SHEET", dpf(14), h - dpf(10), dogEarHintPaint);
        }
    }
}