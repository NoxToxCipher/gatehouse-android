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
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BackgammonGameView — Classic 24-point Backgammon Board.
 * Museum-grade Lacquered Walnut & Inlaid Ivory Canvas with 3D Stacked Checkers,
 * Central Bar Dividers, Pip Equity Counter, and 3D Ivory Dice.
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
    private final Paint goldDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moveBeaconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkerShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint latheRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diceFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dicePipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trayBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

    // Animation States
    private boolean isRolling = false;
    private int tumbleDie1 = 1, tumbleDie2 = 1;
    private float tumbleRot1 = 0f, tumbleRot2 = 0f;

    // Checker Sliding Animation
    private boolean isCheckerAnimating = false;
    private float animStartX, animStartY;
    private float animEndX, animEndY;
    private boolean animIsGold;
    private long animStartTime = 0;
    private static final long CHECKER_SLIDE_DURATION_MS = 220;

    // Blot Hit Shockwave Ripple
    private float rippleX = -1, rippleY = -1;
    private long rippleStartTime = 0;
    private static final long RIPPLE_DURATION_MS = 380;
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Bearing-off Particles
    private final List<Particle> particles = new ArrayList<>();
    private static class Particle {
        float x, y, vx, vy, alpha, size;
        int color;
    }

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public BackgammonGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(2f));

        goldDetailPaint.setColor(0xFFCA8A04);
        goldDetailPaint.setStyle(Paint.Style.STROKE);
        goldDetailPaint.setStrokeWidth(dpf(1f));

        moveBeaconPaint.setColor(0xCCFDE047);
        moveBeaconPaint.setStyle(Paint.Style.STROKE);
        moveBeaconPaint.setStrokeWidth(dpf(2.4f));

        shadowPaint.setColor(0x99000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        checkerRimPaint.setColor(0xFFFFFFFF);
        checkerRimPaint.setStyle(Paint.Style.STROKE);
        checkerRimPaint.setStrokeWidth(dpf(1.2f));

        latheRingPaint.setStyle(Paint.Style.STROKE);
        latheRingPaint.setStrokeWidth(dpf(1f));

        checkerShinePaint.setColor(0xAAFFFFFF);
        checkerShinePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        diceFacePaint.setColor(0xFFFFFBEB);
        diceFacePaint.setStyle(Paint.Style.FILL);

        dicePipPaint.setColor(0xFF0F172A);
        dicePipPaint.setStyle(Paint.Style.FILL);

        trayBgPaint.setColor(0xFF131B2B);
        trayBgPaint.setStyle(Paint.Style.FILL);

        resetGame();
    }

    private static class HistoryState {
        final int[] points = new int[24];
        final int whiteBar, blackBar, whiteOff, blackOff;
        final boolean whiteTurn, waitingForRoll;
        final List<Integer> availableDice = new ArrayList<>();
        final int lastDie1, lastDie2;

        HistoryState(int[] pts, int wb, int bb, int wo, int bo, boolean wt, boolean wfr, List<Integer> dice, int d1, int d2) {
            System.arraycopy(pts, 0, points, 0, 24);
            this.whiteBar = wb;
            this.blackBar = bb;
            this.whiteOff = wo;
            this.blackOff = bo;
            this.whiteTurn = wt;
            this.waitingForRoll = wfr;
            this.availableDice.addAll(dice);
            this.lastDie1 = d1;
            this.lastDie2 = d2;
        }
    }

    private final List<HistoryState> history = new ArrayList<>();

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

        history.clear();
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

    public void undoMove() {
        if (history.isEmpty()) return;
        HistoryState prev = history.remove(history.size() - 1);
        if (!prev.whiteTurn && !history.isEmpty()) {
            prev = history.remove(history.size() - 1);
        }
        System.arraycopy(prev.points, 0, points, 0, 24);
        this.whiteBar = prev.whiteBar;
        this.blackBar = prev.blackBar;
        this.whiteOff = prev.whiteOff;
        this.blackOff = prev.blackOff;
        this.whiteTurn = prev.whiteTurn;
        this.waitingForRoll = prev.waitingForRoll;
        this.availableDice.clear();
        this.availableDice.addAll(prev.availableDice);
        this.lastDie1 = prev.lastDie1;
        this.lastDie2 = prev.lastDie2;
        this.selectedPoint = -1;
        updateStatus();
        invalidate();
    }

    public void rollDice() {
        if (!waitingForRoll || isRolling) return;
        isRolling = true;
        history.add(new HistoryState(points, whiteBar, blackBar, whiteOff, blackOff, whiteTurn, waitingForRoll, availableDice, lastDie1, lastDie2));
        try {
            RecreationAudioSynth.playDiceRoll();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        animateDiceTumble(0);
    }

    private void animateDiceTumble(final int step) {
        if (step < 6) {
            tumbleDie1 = rand.nextInt(6) + 1;
            tumbleDie2 = rand.nextInt(6) + 1;
            tumbleRot1 = (rand.nextFloat() - 0.5f) * 40f;
            tumbleRot2 = (rand.nextFloat() - 0.5f) * 40f;
            invalidate();
            postDelayed(new Runnable() {
                public void run() {
                    animateDiceTumble(step + 1);
                }
            }, 45);
        } else {
            int d1 = rand.nextInt(6) + 1;
            int d2 = rand.nextInt(6) + 1;
            lastDie1 = d1;
            lastDie2 = d2;
            tumbleDie1 = d1;
            tumbleDie2 = d2;
            tumbleRot1 = 0f;
            tumbleRot2 = 0f;
            isRolling = false;

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
        history.add(new HistoryState(points, whiteBar, blackBar, whiteOff, blackOff, whiteTurn, waitingForRoll, availableDice, lastDie1, lastDie2));

        if (whiteTurn) {
            int toPoint = (fromPoint == -1) ? 24 - dieVal : fromPoint - dieVal;
            if (toPoint >= 0) {
                if (points[toPoint] < -1) return false;

                if (points[toPoint] == -1) {
                    points[toPoint] = 0;
                    blackBar++;
                    triggerRipple(toPoint, false);
                    try {
                        RecreationAudioSynth.playChessPieceThud(true);
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    } catch (Exception ignored) {}
                } else {
                    try { RecreationAudioSynth.playChessPieceThud(false); } catch (Exception ignored) {}
                }
                points[toPoint]++;
            } else {
                if (!canBearOff(true)) return false;
                whiteOff++;
                spawnBearingOffParticles(true);
                try { RecreationAudioSynth.playBadukStoneClack(); } catch (Exception ignored) {}
            }

            if (fromPoint == -1) whiteBar--;
            else points[fromPoint]--;

            triggerSlide(fromPoint, toPoint, true);

        } else {
            int toPoint = (fromPoint == -1) ? dieVal - 1 : fromPoint + dieVal;
            if (toPoint < 24) {
                if (points[toPoint] > 1) return false;

                if (points[toPoint] == 1) {
                    points[toPoint] = 0;
                    whiteBar++;
                    triggerRipple(toPoint, true);
                    try {
                        RecreationAudioSynth.playChessPieceThud(true);
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    } catch (Exception ignored) {}
                } else {
                    try { RecreationAudioSynth.playChessPieceThud(false); } catch (Exception ignored) {}
                }
                points[toPoint]--;
            } else {
                if (!canBearOff(false)) return false;
                blackOff++;
                spawnBearingOffParticles(false);
                try { RecreationAudioSynth.playBadukStoneClack(); } catch (Exception ignored) {}
            }

            if (fromPoint == -1) blackBar--;
            else points[fromPoint]++;

            triggerSlide(fromPoint, toPoint, false);
        }

        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

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
            statusListener.onStatusChanged("💀 BOT WINS! Backgammon Grandmaster.", 0xFFEF4444);
            return;
        }

        String turn = whiteTurn ? "🟡 Your Turn (Gold)" : "🔵 Bot's Turn (Cyan)";
        String dice = waitingForRoll ? "· Tap to Roll" : ("· Dice: [" + lastDie1 + ", " + lastDie2 + "]");
        statusListener.onStatusChanged(turn + " " + dice + " (Off: " + whiteOff + " - " + blackOff + ")", 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (action == MotionEvent.ACTION_DOWN) {
            if (event.getY() > getHeight() - dpf(48f)) {
                if (waitingForRoll && whiteTurn) {
                    rollDice();
                    return true;
                }
            }

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

    private boolean isPointMovable(int fromPoint) {
        if (!whiteTurn || waitingForRoll || availableDice.isEmpty()) return false;
        if (whiteBar > 0) return (fromPoint == -1);
        if (fromPoint < 0 || fromPoint >= 24 || points[fromPoint] <= 0) return false;

        for (int die : availableDice) {
            int toPoint = fromPoint - die;
            if (toPoint >= 0) {
                if (points[toPoint] >= -1) return true;
            } else {
                if (canBearOff(true)) return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Moroccan Walnut Board Bed
        rect.set(0, 0, w, h);
        boardWoodPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF2C160B, 0xFF190C06, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardWoodPaint);

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        float pad = dpf(10f);
        float barW = dpf(16f);
        float colW = (w - pad * 2 - barW) / 12f;
        float triH = (h - dpf(46f)) * 0.42f;

        // Draw Central Bar (Solid Ebony Divider with Brass Hinges)
        float barLeft = pad + 6 * colW;
        rect.set(barLeft, dpf(8f), barLeft + barW, h - dpf(36f));
        Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(0xFF140D09);
        canvas.drawRoundRect(rect, dpf(4f), dpf(4f), barPaint);
        canvas.drawRoundRect(rect, dpf(4f), dpf(4f), goldDetailPaint);

        // Draw 24 Points with Inlaid Ivory & Rosewood
        pointLightPaint.setColor(0xFFECE1CA); // Inlaid Cream Ivory
        pointDarkPaint.setColor(0xFF85371A);  // Inlaid Burnt Rosewood

        Paint pointBevel = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointBevel.setColor(0x44000000);
        pointBevel.setStyle(Paint.Style.STROKE);
        pointBevel.setStrokeWidth(dpf(1f));

        for (int i = 0; i < 12; i++) {
            float left = pad + (i < 6 ? i * colW : i * colW + barW);
            boolean isDark = (i % 2 == 0);

            // Top row points (12..23)
            triPath.reset();
            triPath.moveTo(left, dpf(8f));
            triPath.lineTo(left + colW, dpf(8f));
            triPath.lineTo(left + colW / 2f, dpf(8f) + triH);
            triPath.close();
            canvas.drawPath(triPath, isDark ? pointDarkPaint : pointLightPaint);
            canvas.drawPath(triPath, pointBevel);

            // Bottom row points (11..0)
            triPath.reset();
            triPath.moveTo(left, h - dpf(36f));
            triPath.lineTo(left + colW, h - dpf(36f));
            triPath.lineTo(left + colW / 2f, h - dpf(36f) - triH);
            triPath.close();
            canvas.drawPath(triPath, !isDark ? pointDarkPaint : pointLightPaint);
            canvas.drawPath(triPath, pointBevel);
        }

        // Draw Checkers on Central Bar if any
        float checkerR = Math.min(colW * 0.44f, dpf(11f));
        if (whiteBar > 0) {
            float bcx = barLeft + barW / 2f;
            float bcy = (h - dpf(36f)) / 2f + dpf(14f);
            drawBackgammonChecker(canvas, bcx, bcy, checkerR, true, isPointMovable(-1));
            if (whiteBar > 1) {
                textPaint.setTextSize(dpf(9f));
                canvas.drawText("×" + whiteBar, bcx, bcy + dpf(3f), textPaint);
            }
        }
        if (blackBar > 0) {
            float bcx = barLeft + barW / 2f;
            float bcy = (h - dpf(36f)) / 2f - dpf(14f);
            drawBackgammonChecker(canvas, bcx, bcy, checkerR, false, false);
            if (blackBar > 1) {
                textPaint.setTextSize(dpf(9f));
                canvas.drawText("×" + blackBar, bcx, bcy + dpf(3f), textPaint);
            }
        }

        // Draw Checkers on 24 Points
        for (int p = 0; p < 24; p++) {
            int count = points[p];
            if (count == 0) continue;

            boolean isGold = (count > 0);
            int absCount = Math.abs(count);
            boolean isMovable = isGold && isPointMovable(p);

            float cx;
            float cyStart;
            float cyStep;

            if (p < 12) {
                int col = 11 - p;
                cx = pad + (col < 6 ? col * colW : col * colW + barW) + colW / 2f;
                cyStart = h - dpf(36f) - checkerR;
                cyStep = -checkerR * 1.8f;
            } else {
                int col = p - 12;
                cx = pad + (col < 6 ? col * colW : col * colW + barW) + colW / 2f;
                cyStart = dpf(8f) + checkerR;
                cyStep = checkerR * 1.8f;
            }

            int drawCount = Math.min(absCount, 5);
            for (int k = 0; k < drawCount; k++) {
                boolean isTop = (k == drawCount - 1);
                drawBackgammonChecker(canvas, cx, cyStart + k * cyStep, checkerR, isGold, isTop && isMovable);
            }
            if (absCount > 5) {
                textPaint.setTextSize(dpf(9f));
                canvas.drawText("+" + (absCount - 5), cx, cyStart + 4 * cyStep + dpf(3f), textPaint);
            }
        }

        // Render Shockwave Ripple on Blot Hits
        long rippleElapsed = System.currentTimeMillis() - rippleStartTime;
        if (rippleX >= 0 && rippleElapsed < RIPPLE_DURATION_MS) {
            float rp = (float) rippleElapsed / RIPPLE_DURATION_MS;
            float rR = checkerR * (1.0f + 2.2f * rp);
            ripplePaint.setColor(0xFFFFD166);
            ripplePaint.setStyle(Paint.Style.STROKE);
            ripplePaint.setStrokeWidth(dpf(2.4f) * (1f - rp));
            ripplePaint.setAlpha((int) (220 * (1f - rp)));
            canvas.drawCircle(rippleX, rippleY, rR, ripplePaint);
            postInvalidateOnAnimation();
        }

        // Render Animated Sliding Active Checker
        long slideElapsed = System.currentTimeMillis() - animStartTime;
        if (isCheckerAnimating && slideElapsed < CHECKER_SLIDE_DURATION_MS) {
            float sp = (float) slideElapsed / CHECKER_SLIDE_DURATION_MS;
            float t = 1f - (1f - sp) * (1f - sp); // Ease-out quad
            float curX = animStartX + (animEndX - animStartX) * t;
            float curY = animStartY + (animEndY - animStartY) * t - (float) Math.sin(sp * Math.PI) * dpf(14f);
            drawBackgammonChecker(canvas, curX, curY, checkerR * 1.08f, animIsGold, false);
            postInvalidateOnAnimation();
        } else if (isCheckerAnimating) {
            isCheckerAnimating = false;
        }

        // Render Bearing-Off Golden Particles
        if (!particles.isEmpty()) {
            Paint pPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pPaint.setStyle(Paint.Style.FILL);
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.x += p.vx;
                p.y += p.vy;
                p.alpha -= 0.045f;
                if (p.alpha <= 0) {
                    particles.remove(i);
                } else {
                    pPaint.setColor(p.color);
                    pPaint.setAlpha((int) (255 * p.alpha));
                    canvas.drawCircle(p.x, p.y, p.size * p.alpha, pPaint);
                }
            }
            postInvalidateOnAnimation();
        }

        // Bottom Dashboard: Checkers Borne Off & 3D Rolling Dice
        float trayTop = h - dpf(32f);
        rect.set(pad, trayTop, w - pad, h - dpf(4f));
        canvas.drawRoundRect(rect, dpf(6f), dpf(6f), trayBgPaint);
        canvas.drawRoundRect(rect, dpf(6f), dpf(6f), goldBorderPaint);

        textPaint.setTextSize(dpf(10f));
        canvas.drawText("🟡 Off: " + whiteOff + " (Bar " + whiteBar + ") | 🔵 Off: " + blackOff + " (Bar " + blackBar + ")", w * 0.38f, h - dpf(12f), textPaint);

        if (isRolling || (lastDie1 > 0 && lastDie2 > 0)) {
            int d1 = isRolling ? tumbleDie1 : lastDie1;
            int d2 = isRolling ? tumbleDie2 : lastDie2;
            float rot1 = isRolling ? tumbleRot1 : 0f;
            float rot2 = isRolling ? tumbleRot2 : 0f;
            draw3DDie(canvas, w - dpf(65f), h - dpf(18f), dpf(10.5f), d1, rot1);
            draw3DDie(canvas, w - dpf(35f), h - dpf(18f), dpf(10.5f), d2, rot2);
            if (isRolling) postInvalidateOnAnimation();
        }
    }

    private float[] getPointCoordinates(int p, boolean isBar, boolean isGold, float w, float h) {
        float pad = dpf(10f);
        float barW = dpf(16f);
        float colW = (w - pad * 2 - barW) / 12f;
        float checkerR = Math.min(colW * 0.44f, dpf(11f));
        float barLeft = pad + 6 * colW;

        if (isBar || p == -1) {
            float bcx = barLeft + barW / 2f;
            float bcy = isGold ? ((h - dpf(36f)) / 2f + dpf(14f)) : ((h - dpf(36f)) / 2f - dpf(14f));
            return new float[]{bcx, bcy};
        }
        if (p < 0 || p >= 24) { // Borne off tray
            return new float[]{isGold ? (w * 0.35f) : (w * 0.65f), h - dpf(18f)};
        }

        int count = Math.abs(points[p]);
        float cx, cy;
        if (p < 12) {
            int col = 11 - p;
            cx = pad + (col < 6 ? col * colW : col * colW + barW) + colW / 2f;
            cy = h - dpf(36f) - checkerR - Math.min(count, 4) * checkerR * 1.8f;
        } else {
            int col = p - 12;
            cx = pad + (col < 6 ? col * colW : col * colW + barW) + colW / 2f;
            cy = dpf(8f) + checkerR + Math.min(count, 4) * checkerR * 1.8f;
        }
        return new float[]{cx, cy};
    }

    private void triggerSlide(int fromPt, int toPt, boolean isGold) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float[] start = getPointCoordinates(fromPt, fromPt == -1, isGold, w, h);
        float[] end = getPointCoordinates(toPt, false, isGold, w, h);
        animStartX = start[0];
        animStartY = start[1];
        animEndX = end[0];
        animEndY = end[1];
        animIsGold = isGold;
        animStartTime = System.currentTimeMillis();
        isCheckerAnimating = true;
        invalidate();
    }

    private void triggerRipple(int pt, boolean isGold) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float[] pos = getPointCoordinates(pt, false, isGold, w, h);
        rippleX = pos[0];
        rippleY = pos[1];
        rippleStartTime = System.currentTimeMillis();
        invalidate();
    }

    private void spawnBearingOffParticles(boolean isGold) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;
        float ox = isGold ? (w * 0.35f) : (w * 0.65f);
        float oy = h - dpf(18f);
        for (int i = 0; i < 14; i++) {
            Particle p = new Particle();
            p.x = ox + (rand.nextFloat() - 0.5f) * dpf(24f);
            p.y = oy + (rand.nextFloat() - 0.5f) * dpf(10f);
            float angle = (float) (rand.nextFloat() * Math.PI * 2);
            float speed = dpf(1.2f + rand.nextFloat() * 2.5f);
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed - dpf(1.5f);
            p.alpha = 1.0f;
            p.size = dpf(2f + rand.nextFloat() * 2.5f);
            p.color = isGold ? 0xFFFDE047 : 0xFF38BDF8;
            particles.add(p);
        }
        invalidate();
    }

    private void drawBackgammonChecker(Canvas canvas, float cx, float cy, float r, boolean isGold, boolean isMovable) {
        // Deep drop shadow
        canvas.drawCircle(cx + dpf(1.2f), cy + dpf(1.8f), r, shadowPaint);

        // Movable beacon glow halo
        if (isMovable) {
            canvas.drawCircle(cx, cy, r + dpf(2.4f), moveBeaconPaint);
        }

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            isGold ? new int[]{0xFFFFFDF5, 0xFFFDE047, 0xFFD97706, 0xFF78350F}
                   : new int[]{0xFFE0F2FE, 0xFF0284C7, 0xFF082F49, 0xFF031628},
            null, Shader.TileMode.CLAMP
        );
        checkerPaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, checkerPaint);
        canvas.drawCircle(cx, cy, r, checkerRimPaint);

        // Concentric Turned Lathe Rings
        latheRingPaint.setColor(isGold ? 0x6678350F : 0x55000000);
        canvas.drawCircle(cx, cy, r * 0.65f, latheRingPaint);
        canvas.drawCircle(cx, cy, r * 0.35f, latheRingPaint);

        // Center Brass/Ivory Stud
        Paint studPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        studPaint.setColor(isGold ? 0xFFFDE047 : 0xFF38BDF8);
        canvas.drawCircle(cx, cy, dpf(1.6f), studPaint);

        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.28f, checkerShinePaint);
    }

    private void draw3DDie(Canvas canvas, float cx, float cy, float s, int val, float rot) {
        canvas.save();
        if (rot != 0f) {
            canvas.rotate(rot, cx, cy);
        }
        // Deep drop shadow
        rect.set(cx - s + dpf(1f), cy - s + dpf(1.5f), cx + s + dpf(1f), cy + s + dpf(1.5f));
        canvas.drawRoundRect(rect, dpf(3.5f), dpf(3.5f), shadowPaint);

        rect.set(cx - s, cy - s, cx + s, cy + s);
        canvas.drawRoundRect(rect, dpf(3.5f), dpf(3.5f), diceFacePaint);
        canvas.drawRoundRect(rect, dpf(3.5f), dpf(3.5f), goldBorderPaint);

        if (val == 1 || val == 3 || val == 5) canvas.drawCircle(cx, cy, dpf(1.8f), dicePipPaint);
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
        canvas.restore();
    }
}
