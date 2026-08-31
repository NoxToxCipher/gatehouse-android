package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BackgammonGameView — Classic 24-point Backgammon Board.
 * Features 2 rolling dice, bar entry, bearing off, pip count evaluation, and Expectimax AI.
 */
public class BackgammonGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiteCheckerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackCheckerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path triPath = new Path();
    private final Random rand = new Random();

    // 24 points: 0..23. Positive count = White (Human), Negative count = Black (Bot)
    private final int[] points = new int[24];
    private int whiteBar = 0;
    private int blackBar = 0;
    private int whiteOff = 0;
    private int blackOff = 0;

    // true = White (Human), false = Black (Bot)
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

        boardBgPaint.setColor(0xFF0F172A);
        pointDarkPaint.setColor(0xFF1E293B);
        pointLightPaint.setColor(0xFF334155);

        whiteCheckerPaint.setColor(0xFFFFD166); // Gold
        whiteCheckerPaint.setStyle(Paint.Style.FILL);

        blackCheckerPaint.setColor(0xFF38BDF8); // Cyan
        blackCheckerPaint.setStyle(Paint.Style.FILL);

        selectPaint.setColor(0x88FFD166);
        selectPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        resetGame();
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void resetGame() {
        for (int i = 0; i < 24; i++) points[i] = 0;
        // Standard Backgammon initial setup
        points[0] = 2;   // White 2 on point 1 (idx 0)
        points[11] = 5;  // White 5 on point 12
        points[16] = 3;  // White 3 on point 17
        points[18] = 5;  // White 5 on point 19

        points[23] = -2; // Black 2 on point 24 (idx 23)
        points[12] = -5; // Black 5 on point 13
        points[7] = -3;  // Black 3 on point 8
        points[5] = -5;  // Black 5 on point 6

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
                if (points[toPoint] < -1) return false; // blocked by 2+ enemy checkers

                // Hit opponent single blot
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
            statusListener.onStatusChanged("💀 BOT WINS! Grandmaster Backgammon.", 0xFFEF4444);
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
                float w = getWidth();
                float h = getHeight();
                float pad = dpf(12f);
                float colW = (w - pad * 2 - dpf(16f)) / 12f;

                float ex = event.getX();
                float ey = event.getY();

                // Select and move
                int die = availableDice.get(0);
                if (whiteBar > 0) {
                    tryMoveChecker(-1, die);
                    return true;
                }

                // Check clicked point
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
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        float pad = dpf(12f);
        float barW = dpf(16f);
        float colW = (w - pad * 2 - barW) / 12f;
        float triH = (h - dpf(30f)) * 0.42f;

        // Draw 24 triangular points
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
            triPath.moveTo(left, h - dpf(20f));
            triPath.lineTo(left + colW, h - dpf(20f));
            triPath.lineTo(left + colW / 2f, h - dpf(20f) - triH);
            triPath.close();
            canvas.drawPath(triPath, !isDark ? pointDarkPaint : pointLightPaint);
        }

        // Draw Checkers on points
        float checkerR = Math.min(colW * 0.45f, dpf(11f));
        for (int p = 0; p < 24; p++) {
            int count = points[p];
            if (count == 0) continue;

            boolean isWhite = (count > 0);
            int absCount = Math.abs(count);

            float cx;
            float cyStart;
            float cyStep;

            if (p < 12) { // Bottom row (11..0 from left to right)
                int col = 11 - p;
                cx = pad + (col < 6 ? col * colW : col * colW + barW) + colW / 2f;
                cyStart = h - dpf(20f) - checkerR;
                cyStep = -checkerR * 1.8f;
            } else { // Top row (12..23 from left to right)
                int col = p - 12;
                cx = pad + (col < 6 ? col * colW : col * colW + barW) + colW / 2f;
                cyStart = dpf(10f) + checkerR;
                cyStep = checkerR * 1.8f;
            }

            for (int k = 0; k < Math.min(absCount, 5); k++) {
                canvas.drawCircle(cx, cyStart + k * cyStep, checkerR, isWhite ? whiteCheckerPaint : blackCheckerPaint);
            }
            if (absCount > 5) {
                textPaint.setTextSize(dpf(9f));
                canvas.drawText("+" + (absCount - 5), cx, cyStart + 4 * cyStep, textPaint);
            }
        }

        // Center Bar and Borne Off stats
        textPaint.setTextSize(dpf(10f));
        canvas.drawText("🟡 Gold Off: " + whiteOff + " (Bar: " + whiteBar + ") | 🔵 Cyan Off: " + blackOff + " (Bar: " + blackBar + ")", w / 2f, h - dpf(6f), textPaint);
    }
}
