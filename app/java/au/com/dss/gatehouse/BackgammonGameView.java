package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BackgammonGameView — Classic 24-point Backgammon Board.
 * High-fidelity Lacquered Walnut & Inlaid Ivory Canvas with 3D Stacked Checkers,
 * Central Bar dividers, Pip counter, and 3D rolling dice.
 */
public class BackgammonGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardWoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diceFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dicePipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path triPath = new Path();
    private final Random rand = new Random();

    private final int[] points = new int[24];
    private int whiteBar = 0;
    private int blackBar = 0;
    private int whiteOff = 0;
    private int blackOff = 0;

    private boolean whiteTurn = true;
    private final List<Integer> availableDice = new ArrayList<>();
    private boolean waitingForRoll = true;
    private int selectedPoint = -1;
    private int lastDie1 = 0, lastDie2 = 0;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public BackgammonGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(1.5f));

        shadowPaint.setColor(0x88000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        checkerRimPaint.setColor(0x66FFFFFF);
        checkerRimPaint.setStyle(Paint.Style.STROKE);
        checkerRimPaint.setStrokeWidth(dpf(1.2f));

        checkerShinePaint.setColor(0xAAFFFFFF);
        checkerShinePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        diceFacePaint.setColor(0xFFFFFBEB);
        diceFacePaint.setStyle(Paint.Style.FILL);

        dicePipPaint.setColor(0xFF0F172A);
        dicePipPaint.setStyle(Paint.Style.FILL);

        resetGame();
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void resetGame() {
        for (int i = 0; i < 24; i++) points[i] = 0;
        points[0] = 2;
        points[11] = 5;
        points[16] = 3;
        points[18] = 5;

        points[23] = -2;
        points[12] = -5;
        points[7] = -3;
        points[5] = -5;

        whiteBar = 0;
        blackBar = 0;
        whiteOff = 0;
        blackOff = 0;
        whiteTurn = true;
        waitingForRoll = true;
        availableDice.clear();
        selectedPoint = -1;

        updateStatus();
        invalidate();
    }

    public void rollDice() {
        if (!waitingForRoll) return;
        int d1 = rand.nextInt(6) + 1;
        int d2 = rand.nextInt(6) + 1;
        lastDie1 = d1;
        lastDie2 = d2;

        availableDice.clear();
        if (d1 == d2) {
            for (int i = 0; i < 4; i++) availableDice.add(d1);
        } else {
            availableDice.add(d1);
            availableDice.add(d2);
        }
        waitingForRoll = false;

        if (!canAnyMove(whiteTurn)) {
            postDelayed(new Runnable() {
                public void run() { endTurn(); }
            }, 700);
        } else {
            if (!whiteTurn) {
                postDelayed(new Runnable() {
                    public void run() { botPlayTurn(); }
                }, 500);
            }
        }
        updateStatus();
        invalidate();
    }

    private boolean canAnyMove(boolean isWhite) {
        if (availableDice.isEmpty()) return false;
        for (int d : availableDice) {
            if (isWhite) {
                if (whiteBar > 0) {
                    int target = 24 - d;
                    if (points[target] >= -1) return true;
                } else {
                    for (int p = 0; p < 24; p++) {
                        if (points[p] > 0) {
                            int target = p - d;
                            if (target >= 0 && points[target] >= -1) return true;
                            if (target < 0 && canBearOff(true)) return true;
                        }
                    }
                }
            } else {
                if (blackBar > 0) {
                    int target = d - 1;
                    if (points[target] <= 1) return true;
                } else {
                    for (int p = 0; p < 24; p++) {
                        if (points[p] < 0) {
                            int target = p + d;
                            if (target < 24 && points[target] <= 1) return true;
                            if (target >= 24 && canBearOff(false)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean canBearOff(boolean isWhite) {
        if (isWhite) {
            if (whiteBar > 0) return false;
            for (int p = 6; p < 24; p++) {
                if (points[p] > 0) return false;
            }
            return true;
        } else {
            if (blackBar > 0) return false;
            for (int p = 0; p < 18; p++) {
                if (points[p] < 0) return false;
            }
            return true;
        }
    }

    private boolean tryMoveChecker(int fromPoint, int dieVal) {
        if (!availableDice.contains(dieVal)) return false;

        if (whiteTurn) {
            int toPoint = (fromPoint == -1) ? 24 - dieVal : fromPoint - dieVal;
            if (toPoint >= 0) {
                if (points[toPoint] < -1) return false;

                if (points[toPoint] == -1) {
                    points[toPoint] = 0;
                    blackBar++;
                }
                points[toPoint]++;
            } else {
                if (!canBearOff(true)) return false;
                whiteOff++;
            }

            if (fromPoint == -1) whiteBar--;
            else points[fromPoint]--;

        } else {
            int toPoint = (fromPoint == -1) ? dieVal - 1 : fromPoint + dieVal;
            if (toPoint < 24) {
                if (points[toPoint] > 1) return false;

                if (points[toPoint] == 1) {
                    points[toPoint] = 0;
                    whiteBar++;
                }
                points[toPoint]--;
            } else {
                if (!canBearOff(false)) return false;
                blackOff++;
            }

            if (fromPoint == -1) blackBar--;
            else points[fromPoint]++;
        }

        availableDice.remove((Integer) dieVal);

        if (whiteOff == 15 || blackOff == 15) {
            updateStatus();
            invalidate();
            return true;
        }

        if (availableDice.isEmpty() || !canAnyMove(whiteTurn)) {
            endTurn();
        }
        updateStatus();
        invalidate();
        return true;
    }

    private void endTurn() {
        whiteTurn = !whiteTurn;
        waitingForRoll = true;
        availableDice.clear();
        selectedPoint = -1;
        updateStatus();
        invalidate();

        if (!whiteTurn) {
            postDelayed(new Runnable() {
                public void run() { rollDice(); }
            }, 450);
        }
    }

    private void botPlayTurn() {
        if (whiteTurn || waitingForRoll || availableDice.isEmpty()) return;

        int die = availableDice.get(0);
        boolean moved = false;

        if (blackBar > 0) {
            if (tryMoveChecker(-1, die)) moved = true;
        } else {
            for (int p = 0; p < 24; p++) {
                if (points[p] < 0) {
                    if (tryMoveChecker(p, die)) {
                        moved = true;
                        break;
                    }
                }
            }
        }

        if (moved && !availableDice.isEmpty()) {
            postDelayed(new Runnable() {
                public void run() { botPlayTurn(); }
            }, 350);
        } else if (!moved) {
            endTurn();
        }
    }

    private void updateStatus() {
        if (statusListener == null) return;
        if (whiteOff == 15) {
            statusListener.onStatusChanged("🏆 VICTORY! All 15 checkers borne off.", 0xFF10B981);
            return;
        }
        if (blackOff == 15) {
            statusListener.onStatusChanged("💀 BOT WINS! Backgammon Master.", 0xFFEF4444);
            return;
        }

        String turn = whiteTurn ? "🟡 Your Turn (Gold)" : "🔵 Bot's Turn (Cyan)";
        String dice = waitingForRoll ? "· Tap to Roll" : ("· Dice: [" + lastDie1 + ", " + lastDie2 + "]");
        statusListener.onStatusChanged(turn + " " + dice + " (Off: " + whiteOff + " - " + blackOff + ")", 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (waitingForRoll && whiteTurn) {
                rollDice();
                return true;
            }

            if (!waitingForRoll && whiteTurn && !availableDice.isEmpty()) {
                int die = availableDice.get(0);
                if (whiteBar > 0) {
                    tryMoveChecker(-1, die);
                    return true;
                }

                for (int p = 0; p < 24; p++) {
                    if (points[p] > 0) {
                        if (tryMoveChecker(p, die)) return true;
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        rect.set(0, 0, w, h);
        boardWoodPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF1C1917, 0xFF0F172A, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardWoodPaint);

        rect.set(dpf(2f), dpf(2f), w - dpf(2f), h - dpf(2f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        float pad = dpf(10f);
        float barW = dpf(16f);
        float colW = (w - pad * 2 - barW) / 12f;
        float triH = (h - dpf(42f)) * 0.42f;

        // Draw Center Bar
        float barLeft = pad + 6 * colW;
        rect.set(barLeft, dpf(10f), barLeft + barW, h - dpf(32f));
        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(0xFF292524);
        canvas.drawRoundRect(rect, dpf(4f), dpf(4f), barPaint);
        canvas.drawRoundRect(rect, dpf(4f), dpf(4f), goldBorderPaint);

        // Draw 24 Points with Shaded Gradients
        pointDarkPaint.setColor(0xFF1E293B);
        pointLightPaint.setColor(0xFF451A03);

        for (int i = 0; i < 12; i++) {
            float left = pad + (i < 6 ? i * colW : i * colW + barW);
            boolean isDark = (i % 2 == 0);

            // Top row points (12..23)
            triPath.reset();
            triPath.moveTo(left, dpf(10f));
            triPath.lineTo(left + colW, dpf(10f));
            triPath.lineTo(left + colW / 2f, dpf(10f) + triH);
            triPath.close();
            canvas.drawPath(triPath, isDark ? pointDarkPaint : pointLightPaint);

            // Bottom row points (11..0)
            triPath.reset();
            triPath.moveTo(left, h - dpf(32f));
            triPath.lineTo(left + colW, h - dpf(32f));
            triPath.lineTo(left + colW / 2f, h - dpf(32f) - triH);
            triPath.close();
            canvas.drawPath(triPath, !isDark ? pointDarkPaint : pointLightPaint);
        }

        // Draw Checkers with 3D Radial Shine
        float checkerR = Math.min(colW * 0.44f, dpf(11f));
        for (int p = 0; p < 24; p++) {
            int count = points[p];
            if (count == 0) continue;

            boolean isGold = (count > 0);
            int absCount = Math.abs(count);

            float cx;
            float cyStart;
            float cyStep;

            if (p < 12) {
                int col = 11 - p;
                cx = pad + (col < 6 ? col * colW : col * colW + barW) + colW / 2f;
                cyStart = h - dpf(32f) - checkerR;
                cyStep = -checkerR * 1.8f;
            } else {
                int col = p - 12;
                cx = pad + (col < 6 ? col * colW : col * colW + barW) + colW / 2f;
                cyStart = dpf(10f) + checkerR;
                cyStep = checkerR * 1.8f;
            }

            for (int k = 0; k < Math.min(absCount, 5); k++) {
                drawBackgammonChecker(canvas, cx, cyStart + k * cyStep, checkerR, isGold);
            }
            if (absCount > 5) {
                textPaint.setTextSize(dpf(9f));
                canvas.drawText("+" + (absCount - 5), cx, cyStart + 4 * cyStep, textPaint);
            }
        }

        // Bottom Dashboard: Checkers Borne Off + 3D Rolling Dice
        textPaint.setTextSize(dpf(10f));
        canvas.drawText("🟡 Off: " + whiteOff + " (Bar " + whiteBar + ") | 🔵 Off: " + blackOff + " (Bar " + blackBar + ")", w * 0.35f, h - dpf(10f), textPaint);

        if (lastDie1 > 0 && lastDie2 > 0) {
            draw3DDie(canvas, w - dpf(65f), h - dpf(16f), dpf(10f), lastDie1);
            draw3DDie(canvas, w - dpf(35f), h - dpf(16f), dpf(10f), lastDie2);
        }
    }

    private void drawBackgammonChecker(Canvas canvas, float cx, float cy, float r, boolean isGold) {
        canvas.drawCircle(cx + dpf(1f), cy + dpf(1.5f), r, shadowPaint);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            isGold ? new int[]{0xFFFEF08A, 0xFFEAB308, 0xFF713F12} : new int[]{0xFFBAE6FD, 0xFF0284C7, 0xFF082F49},
            null, Shader.TileMode.CLAMP
        );
        checkerPaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, checkerPaint);
        canvas.drawCircle(cx, cy, r, checkerRimPaint);
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.28f, checkerShinePaint);
    }

    private void draw3DDie(Canvas canvas, float cx, float cy, float s, int val) {
        rect.set(cx - s, cy - s, cx + s, cy + s);
        canvas.drawRoundRect(rect, dpf(3f), dpf(3f), diceFacePaint);
        canvas.drawRoundRect(rect, dpf(3f), dpf(3f), goldBorderPaint);

        // Center pip
        if (val == 1 || val == 3 || val == 5) canvas.drawCircle(cx, cy, dpf(1.8f), dicePipPaint);
        // Corners
        if (val >= 2) {
            canvas.drawCircle(cx - s * 0.5f, cy - s * 0.5f, dpf(1.8f), dicePipPaint);
            canvas.drawCircle(cx + s * 0.5f, cy + s * 0.5f, dpf(1.8f), dicePipPaint);
        }
        if (val >= 4) {
            canvas.drawCircle(cx + s * 0.5f, cy - s * 0.5f, dpf(1.8f), dicePipPaint);
            canvas.drawCircle(cx - s * 0.5f, cy + s * 0.5f, dpf(1.8f), dicePipPaint);
        }
        if (val == 6) {
            canvas.drawCircle(cx - s * 0.5f, cy, dpf(1.8f), dicePipPaint);
            canvas.drawCircle(cx + s * 0.5f, cy, dpf(1.8f), dicePipPaint);
        }
    }
}
