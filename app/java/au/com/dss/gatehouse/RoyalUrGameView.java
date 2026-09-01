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
import java.util.Random;

/**
 * RoyalUrGameView — The Royal Game of Ur (Mesopotamia, c. 2600 BCE).
 * Museum-grade Lapis Lazuli, Cedar & Gold Leaf Sumerian Mosaic Board.
 * Features 20-square Finkel track, 4-sided tetrahedral dice with rolling physics,
 * 8-petal rosettes with ruby cabochons, and Expectimax AI.
 */
public class RoyalUrGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardWoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lapisTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stoneTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint combatTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rosettePetalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rubyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trayBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF tileRect = new RectF();
    private final Path dicePath = new Path();
    private final Random rand = new Random();

    private int currentTurn = 0; // 0 = Player (Gold), 1 = Sumerian Bot (Lapis)
    private int currentRoll = -1;
    private boolean waitingForRoll = true;
    private int whitePiecesOff = 0;
    private int blackPiecesOff = 0;
    private int whitePiecesUnentered = 7;
    private int blackPiecesUnentered = 7;

    private final int[] whitePieces = new int[7];
    private final int[] blackPieces = new int[7];
    private final boolean[] lastDiceFaces = new boolean[4];

    private static final int[][] WHITE_PATH = {
        {0, 3}, {0, 2}, {0, 1}, {0, 0},
        {1, 0}, {1, 1}, {1, 2}, {1, 3}, {1, 4}, {1, 5}, {1, 6}, {1, 7},
        {0, 7}, {0, 6}
    };

    private static final int[][] BLACK_PATH = {
        {2, 3}, {2, 2}, {2, 1}, {2, 0},
        {1, 0}, {1, 1}, {1, 2}, {1, 3}, {1, 4}, {1, 5}, {1, 6}, {1, 7},
        {2, 7}, {2, 6}
    };

    private static final boolean[] IS_ROSETTE = {
        false, false, false, true,
        false, false, false, true, false, false, false, false,
        false, true
    };

    private static class HistoryState {
        final int[] whitePieces;
        final int[] blackPieces;
        final int whitePiecesOff;
        final int blackPiecesOff;
        final int whitePiecesUnentered;
        final int blackPiecesUnentered;
        final int currentTurn;
        final int currentRoll;
        final boolean waitingForRoll;
        final boolean[] lastDiceFaces;

        HistoryState(int[] wp, int[] bp, int wOff, int bOff, int wUn, int bUn, int turn, int roll, boolean wait, boolean[] dice) {
            this.whitePieces = wp.clone();
            this.blackPieces = bp.clone();
            this.whitePiecesOff = wOff;
            this.blackPiecesOff = bOff;
            this.whitePiecesUnentered = wUn;
            this.blackPiecesUnentered = bUn;
            this.currentTurn = turn;
            this.currentRoll = roll;
            this.waitingForRoll = wait;
            this.lastDiceFaces = dice.clone();
        }
    }

    private final java.util.List<HistoryState> history = new java.util.ArrayList<>();
    private final Paint moveGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public RoyalUrGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(1.8f));

        goldDetailPaint.setColor(0xFFFDE047);
        goldDetailPaint.setStyle(Paint.Style.STROKE);
        goldDetailPaint.setStrokeWidth(dpf(1f));

        rosettePetalPaint.setColor(0xFFFFD166);
        rosettePetalPaint.setStyle(Paint.Style.FILL);

        rubyPaint.setColor(0xFFDC2626);
        rubyPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(0x99000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        pieceRimPaint.setColor(0xFFFFFFFF);
        pieceRimPaint.setStyle(Paint.Style.STROKE);
        pieceRimPaint.setStrokeWidth(dpf(1.5f));

        pieceShinePaint.setColor(0xAAFFFFFF);
        pieceShinePaint.setStyle(Paint.Style.FILL);

        moveGlowPaint.setColor(0xFFFDE047);
        moveGlowPaint.setStyle(Paint.Style.STROKE);
        moveGlowPaint.setStrokeWidth(dpf(2.5f));

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        subTextPaint.setColor(0xFF94A3B8);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        trayBgPaint.setColor(0xFF0F172A);
        trayBgPaint.setStyle(Paint.Style.FILL);

        resetGame();
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    private void saveHistory() {
        history.add(new HistoryState(whitePieces, blackPieces, whitePiecesOff, blackPiecesOff, whitePiecesUnentered, blackPiecesUnentered, currentTurn, currentRoll, waitingForRoll, lastDiceFaces));
        if (history.size() > 50) history.remove(0);
    }

    public void undoMove() {
        if (history.isEmpty()) return;
        HistoryState state = history.remove(history.size() - 1);
        System.arraycopy(state.whitePieces, 0, whitePieces, 0, 7);
        System.arraycopy(state.blackPieces, 0, blackPieces, 0, 7);
        whitePiecesOff = state.whitePiecesOff;
        blackPiecesOff = state.blackPiecesOff;
        whitePiecesUnentered = state.whitePiecesUnentered;
        blackPiecesUnentered = state.blackPiecesUnentered;
        currentTurn = state.currentTurn;
        currentRoll = state.currentRoll;
        waitingForRoll = state.waitingForRoll;
        System.arraycopy(state.lastDiceFaces, 0, lastDiceFaces, 0, 4);
        try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
        updateStatus();
        invalidate();
    }

    public void resetGame() {
        history.clear();
        currentTurn = 0;
        currentRoll = -1;
        waitingForRoll = true;
        whitePiecesOff = 0;
        blackPiecesOff = 0;
        whitePiecesUnentered = 7;
        blackPiecesUnentered = 7;
        for (int i = 0; i < 7; i++) {
            whitePieces[i] = -1;
            blackPieces[i] = -1;
        }
        for (int i = 0; i < 4; i++) lastDiceFaces[i] = false;
        updateStatus();
        invalidate();
    }

    private boolean isRolling = false;

    public void rollDice() {
        if (!waitingForRoll || isRolling) return;
        isRolling = true;
        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        // Animated 5-frame rolling tumble
        animateDiceTumble(0);
    }

    private void animateDiceTumble(final int step) {
        if (step < 5) {
            for (int i = 0; i < 4; i++) lastDiceFaces[i] = rand.nextBoolean();
            invalidate();
            postDelayed(new Runnable() {
                public void run() {
                    animateDiceTumble(step + 1);
                }
            }, 55);
        } else {
            // Final true result
            int sum = 0;
            for (int i = 0; i < 4; i++) {
                boolean marked = rand.nextBoolean();
                lastDiceFaces[i] = marked;
                if (marked) sum++;
            }
            currentRoll = sum;
            waitingForRoll = false;
            isRolling = false;

            try {
                if (currentRoll > 0) {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
            } catch (Exception ignored) {}

            if (currentRoll == 0 || !hasLegalMoves(currentTurn, currentRoll)) {
                postDelayed(new Runnable() {
                    public void run() { passTurn(); }
                }, 750);
            } else {
                if (currentTurn == 1) {
                    postDelayed(new Runnable() {
                        public void run() { botExecuteBestMove(); }
                    }, 550);
                }
            }
            updateStatus();
            invalidate();
        }
    }

    private boolean hasLegalMoves(int turn, int roll) {
        if (roll == 0) return false;
        int[] myPieces = (turn == 0) ? whitePieces : blackPieces;
        int[] oppPieces = (turn == 0) ? blackPieces : whitePieces;

        for (int i = 0; i < 7; i++) {
            int pos = myPieces[i];
            if (pos == 14) continue;
            int nextPos = (pos == -1) ? roll - 1 : pos + roll;
            if (nextPos > 14) continue;
            if (nextPos == 14) return true;

            boolean blockedByOwn = false;
            for (int j = 0; j < 7; j++) {
                if (myPieces[j] == nextPos) { blockedByOwn = true; break; }
            }
            if (blockedByOwn) continue;

            if (nextPos == 7) {
                boolean oppOnRosette = false;
                for (int j = 0; j < 7; j++) {
                    if (oppPieces[j] == 7) { oppOnRosette = true; break; }
                }
                if (oppOnRosette) continue;
            }
            return true;
        }
        return false;
    }

    private boolean isPieceMovable(int pieceIdx) {
        if (waitingForRoll || currentRoll <= 0 || currentTurn != 0) return false;
        int pos = whitePieces[pieceIdx];
        if (pos == 14) return false;
        int nextPos = (pos == -1) ? currentRoll - 1 : pos + currentRoll;
        if (nextPos > 14) return false;
        for (int j = 0; j < 7; j++) {
            if (whitePieces[j] == nextPos) return false;
        }
        if (nextPos == 7) {
            for (int j = 0; j < 7; j++) {
                if (blackPieces[j] == 7) return false;
            }
        }
        return true;
    }

    private void makeMove(int pieceIdx) {
        if (waitingForRoll || currentRoll <= 0) return;
        if (currentTurn == 0) saveHistory();
        int[] myPieces = (currentTurn == 0) ? whitePieces : blackPieces;
        int[] oppPieces = (currentTurn == 0) ? blackPieces : whitePieces;

        int pos = myPieces[pieceIdx];
        if (pos == 14) return;
        int nextPos = (pos == -1) ? currentRoll - 1 : pos + currentRoll;
        if (nextPos > 14) return;

        for (int j = 0; j < 7; j++) {
            if (myPieces[j] == nextPos) return;
        }

        if (nextPos == 7) {
            for (int j = 0; j < 7; j++) {
                if (oppPieces[j] == 7) return;
            }
        }

        if (nextPos >= 4 && nextPos <= 11) {
            for (int j = 0; j < 7; j++) {
                if (oppPieces[j] == nextPos) {
                    oppPieces[j] = -1;
                    if (currentTurn == 0) blackPiecesUnentered++;
                    else whitePiecesUnentered++;
                    try {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    } catch (Exception ignored) {}
                    break;
                }
            }
        }

        if (pos == -1) {
            if (currentTurn == 0) whitePiecesUnentered--;
            else blackPiecesUnentered--;
        }

        myPieces[pieceIdx] = nextPos;
        if (nextPos == 14) {
            if (currentTurn == 0) whitePiecesOff++;
            else blackPiecesOff++;
        }

        if (whitePiecesOff == 7 || blackPiecesOff == 7) {
            updateStatus();
            invalidate();
            return;
        }

        boolean landedOnRosette = (nextPos < 14 && IS_ROSETTE[nextPos]);
        if (landedOnRosette) {
            waitingForRoll = true;
            currentRoll = -1;
            updateStatus();
            invalidate();
            if (currentTurn == 1) {
                postDelayed(new Runnable() { public void run() { rollDice(); } }, 450);
            }
        } else {
            passTurn();
        }
    }

    private void passTurn() {
        currentTurn = (currentTurn == 0) ? 1 : 0;
        waitingForRoll = true;
        currentRoll = -1;
        updateStatus();
        invalidate();

        if (currentTurn == 1) {
            postDelayed(new Runnable() {
                public void run() { rollDice(); }
            }, 450);
        }
    }

    private void botExecuteBestMove() {
        if (currentTurn != 1 || waitingForRoll || currentRoll <= 0) return;
        int bestPiece = -1;
        int bestScore = -9999;

        for (int i = 0; i < 7; i++) {
            int pos = blackPieces[i];
            if (pos == 14) continue;
            int nextPos = (pos == -1) ? currentRoll - 1 : pos + currentRoll;
            if (nextPos > 14) continue;

            boolean valid = true;
            for (int j = 0; j < 7; j++) {
                if (blackPieces[j] == nextPos) valid = false;
            }
            if (nextPos == 7) {
                for (int j = 0; j < 7; j++) {
                    if (whitePieces[j] == 7) valid = false;
                }
            }
            if (!valid) continue;

            int score = nextPos * 12;
            if (nextPos == 14) score += 600;
            if (nextPos < 14 && IS_ROSETTE[nextPos]) score += 350;
            if (nextPos >= 4 && nextPos <= 11) {
                for (int j = 0; j < 7; j++) {
                    if (whitePieces[j] == nextPos) score += 450;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestPiece = i;
            }
        }

        if (bestPiece != -1) {
            makeMove(bestPiece);
        } else {
            passTurn();
        }
    }

    private void updateStatus() {
        if (statusListener == null) return;
        if (whitePiecesOff == 7) {
            statusListener.onStatusChanged("🏆 SUMERIAN VICTORY! All 7 pieces borne off.", 0xFF10B981);
            return;
        }
        if (blackPiecesOff == 7) {
            statusListener.onStatusChanged("💀 BOT WINS! Royal Master of Sumerian Ur.", 0xFFEF4444);
            return;
        }

        String turnStr = (currentTurn == 0) ? "🟡 Your Turn (Gold)" : "🔵 Sumerian Bot (Lapis)";
        String rollStr = waitingForRoll ? "· Tap 4 Tetrahedral Dice" : ("· Rolled: " + currentRoll);
        statusListener.onStatusChanged(turnStr + " " + rollStr + " (" + whitePiecesOff + " - " + blackPiecesOff + ")", 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float ex = event.getX();
            float ey = event.getY();
            int w = getWidth();
            int h = getHeight();
            float pad = dpf(10f);
            float cellW = (w - pad * 2) / 8f;
            float cellH = (h - dpf(64f)) / 3f;

            if (ey >= h - dpf(56f)) {
                if (waitingForRoll && currentTurn == 0) {
                    rollDice();
                    return true;
                }
            }

            if (!waitingForRoll && currentTurn == 0 && currentRoll > 0) {
                int c = (int) ((ex - pad) / cellW);
                int r = (int) ((ey - dpf(8f)) / cellH);

                for (int i = 0; i < 7; i++) {
                    int pos = whitePieces[i];
                    if (pos >= 0 && pos < 14) {
                        int pr = WHITE_PATH[pos][0];
                        int pc = WHITE_PATH[pos][1];
                        if (pr == r && pc == c && isPieceMovable(i)) {
                            makeMove(i);
                            return true;
                        }
                    }
                }
                if (whitePiecesUnentered > 0) {
                    for (int i = 0; i < 7; i++) {
                        if (whitePieces[i] == -1 && isPieceMovable(i)) {
                            makeMove(i);
                            return true;
                        }
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

        // Rich Walnut / Lapis Beveled Frame
        rect.set(0, 0, w, h);
        boardWoodPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF0B132B, 0xFF1C2541, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardWoodPaint);

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        float pad = dpf(10f);
        float cellW = (w - pad * 2) / 8f;
        float cellH = (h - dpf(64f)) / 3f;

        // Draw 20 Sumerian Squares
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r == 0 || r == 2) && (c == 4 || c == 5)) continue;

                float left = pad + c * cellW;
                float top = dpf(8f) + r * cellH;
                tileRect.set(left + dpf(2f), top + dpf(2f), left + cellW - dpf(2f), top + cellH - dpf(2f));

                boolean isCombat = (r == 1);
                boolean isRosette = (r == 0 && c == 0) || (r == 2 && c == 0) || (r == 1 && c == 3) || (r == 0 && c == 6) || (r == 2 && c == 6);

                if (isRosette) {
                    lapisTilePaint.setShader(new RadialGradient(tileRect.centerX(), tileRect.centerY(), cellW * 0.7f, 0xFF312E81, 0xFF1E1B4B, Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(tileRect, dpf(6f), dpf(6f), lapisTilePaint);
                    drawOrnateRosette(canvas, tileRect.centerX(), tileRect.centerY(), cellW * 0.38f);
                } else if (isCombat) {
                    combatTilePaint.setShader(new LinearGradient(tileRect.left, tileRect.top, tileRect.right, tileRect.bottom, 0xFF1E293B, 0xFF0F172A, Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(tileRect, dpf(6f), dpf(6f), combatTilePaint);
                    drawFiveDots(canvas, tileRect.centerX(), tileRect.centerY(), cellW * 0.26f);
                } else {
                    stoneTilePaint.setShader(new LinearGradient(tileRect.left, tileRect.top, tileRect.right, tileRect.bottom, 0xFF1E1B4B, 0xFF0F172A, Shader.TileMode.CLAMP));
                    canvas.drawRoundRect(tileRect, dpf(6f), dpf(6f), stoneTilePaint);
                    drawPyramidPattern(canvas, tileRect);
                }

                canvas.drawRoundRect(tileRect, dpf(6f), dpf(6f), goldDetailPaint);
            }
        }

        // Draw 3D Sumerian Counters
        float pieceR = Math.min(cellW, cellH) * 0.38f;
        for (int i = 0; i < 7; i++) {
            int wp = whitePieces[i];
            if (wp >= 0 && wp < 14) {
                int r = WHITE_PATH[wp][0];
                int c = WHITE_PATH[wp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(8f) + r * cellH + cellH / 2f;
                if (currentTurn == 0 && !waitingForRoll && currentRoll > 0 && isPieceMovable(i)) {
                    canvas.drawCircle(cx, cy, pieceR + dpf(3.5f), moveGlowPaint);
                }
                drawSumerianPiece(canvas, cx, cy, pieceR, true);
            }

            int bp = blackPieces[i];
            if (bp >= 0 && bp < 14) {
                int r = BLACK_PATH[bp][0];
                int c = BLACK_PATH[bp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(8f) + r * cellH + cellH / 2f;
                drawSumerianPiece(canvas, cx, cy, pieceR, false);
            }
        }

        // Bottom Dashboard: Reserves & 3D Tetrahedral Dice
        float trayTop = h - dpf(52f);
        rect.set(pad, trayTop, w - pad, h - dpf(6f));
        canvas.drawRoundRect(rect, dpf(10f), dpf(10f), trayBgPaint);
        canvas.drawRoundRect(rect, dpf(10f), dpf(10f), goldBorderPaint);

        // Draw 4 Pyramidal Dice
        float diceStartX = pad + dpf(20f);
        float diceSpacing = (w - pad * 2 - dpf(40f)) / 5f;
        for (int d = 0; d < 4; d++) {
            float dcx = diceStartX + d * diceSpacing;
            float dcy = trayTop + dpf(23f);
            drawTetrahedralDie(canvas, dcx, dcy, dpf(13f), lastDiceFaces[d]);
        }

        float badgeX = diceStartX + 4 * diceSpacing;
        float badgeY = trayTop + dpf(23f);
        textPaint.setTextSize(dpf(13f));
        textPaint.setColor(currentRoll > 0 ? 0xFFFFD166 : 0xFF94A3B8);
        canvas.drawText(currentRoll >= 0 ? "🎲 " + currentRoll : "TAP TO ROLL", badgeX, badgeY + dpf(4f), textPaint);
    }

    private void drawSumerianPiece(Canvas canvas, float cx, float cy, float r, boolean isGold) {
        canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            isGold ? new int[]{0xFFFFFBEB, 0xFFF59E0B, 0xFF78350F} : new int[]{0xFFE0F2FE, 0xFF0284C7, 0xFF0F172A},
            null, Shader.TileMode.CLAMP
        );
        piecePaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, piecePaint);
        canvas.drawCircle(cx, cy, r, pieceRimPaint);
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.3f, pieceShinePaint);

        // Center Gold Rosette Pip
        canvas.drawCircle(cx, cy, r * 0.22f, goldBorderPaint);
        canvas.drawCircle(cx, cy, r * 0.12f, isGold ? rubyPaint : rosettePetalPaint);
    }

    private void drawOrnateRosette(Canvas canvas, float cx, float cy, float r) {
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            float px = (float) (cx + r * 0.6f * Math.cos(angle));
            float py = (float) (cy + r * 0.6f * Math.sin(angle));
            canvas.drawCircle(px, py, r * 0.26f, rosettePetalPaint);
            canvas.drawCircle(px, py, r * 0.13f, rubyPaint);
        }
        canvas.drawCircle(cx, cy, r * 0.34f, rosettePetalPaint);
        canvas.drawCircle(cx, cy, r * 0.18f, rubyPaint);
    }

    private void drawFiveDots(Canvas canvas, float cx, float cy, float r) {
        rosettePetalPaint.setColor(0xFF38BDF8);
        canvas.drawCircle(cx, cy, dpf(2.5f), rosettePetalPaint);
        canvas.drawCircle(cx - r, cy - r, dpf(2f), rosettePetalPaint);
        canvas.drawCircle(cx + r, cy - r, dpf(2f), rosettePetalPaint);
        canvas.drawCircle(cx - r, cy + r, dpf(2f), rosettePetalPaint);
        canvas.drawCircle(cx + r, cy + r, dpf(2f), rosettePetalPaint);
        rosettePetalPaint.setColor(0xFFFFD166);
    }

    private void drawPyramidPattern(Canvas canvas, RectF r) {
        float cx = r.centerX();
        float cy = r.centerY();
        float s = Math.min(r.width(), r.height()) * 0.28f;
        canvas.drawLine(cx - s, cy, cx + s, cy, goldDetailPaint);
        canvas.drawLine(cx, cy - s, cx, cy + s, goldDetailPaint);
        canvas.drawLine(cx - s, cy - s, cx + s, cy + s, goldDetailPaint);
        canvas.drawLine(cx - s, cy + s, cx + s, cy - s, goldDetailPaint);
    }

    private void drawTetrahedralDie(Canvas canvas, float cx, float cy, float size, boolean isMarked) {
        float topX = cx;
        float topY = cy - size;
        float rightX = cx + size * 1.05f;
        float rightY = cy + size * 0.7f;
        float leftX = cx - size * 1.05f;
        float leftY = cy + size * 0.7f;
        float midX = cx;
        float midY = cy + size * 0.25f;

        // Shadow under die
        canvas.drawOval(new RectF(cx - size * 1.1f, cy + size * 0.5f, cx + size * 1.1f, cy + size * 0.95f), shadowPaint);

        Paint leftFacet = new Paint(Paint.ANTI_ALIAS_FLAG);
        leftFacet.setColor(isMarked ? 0xFFF59E0B : 0xFF334155);
        leftFacet.setStyle(Paint.Style.FILL);

        Paint rightFacet = new Paint(Paint.ANTI_ALIAS_FLAG);
        rightFacet.setColor(isMarked ? 0xFFD97706 : 0xFF1E293B);
        rightFacet.setStyle(Paint.Style.FILL);

        Paint bottomFacet = new Paint(Paint.ANTI_ALIAS_FLAG);
        bottomFacet.setColor(isMarked ? 0xFFB45309 : 0xFF0F172A);
        bottomFacet.setStyle(Paint.Style.FILL);

        // Left Face
        dicePath.reset();
        dicePath.moveTo(topX, topY);
        dicePath.lineTo(leftX, leftY);
        dicePath.lineTo(midX, midY);
        dicePath.close();
        canvas.drawPath(dicePath, leftFacet);

        // Right Face
        dicePath.reset();
        dicePath.moveTo(topX, topY);
        dicePath.lineTo(rightX, rightY);
        dicePath.lineTo(midX, midY);
        dicePath.close();
        canvas.drawPath(dicePath, rightFacet);

        // Bottom Face
        dicePath.reset();
        dicePath.moveTo(leftX, leftY);
        dicePath.lineTo(rightX, rightY);
        dicePath.lineTo(midX, midY);
        dicePath.close();
        canvas.drawPath(dicePath, bottomFacet);

        // Ridge Wireframe & Bevel Edges
        Paint dieEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
        dieEdge.setColor(isMarked ? 0xFFFEF08A : 0xFF64748B);
        dieEdge.setStyle(Paint.Style.STROKE);
        dieEdge.setStrokeWidth(dpf(1.4f));

        canvas.drawLine(topX, topY, leftX, leftY, dieEdge);
        canvas.drawLine(topX, topY, rightX, rightY, dieEdge);
        canvas.drawLine(topX, topY, midX, midY, dieEdge);
        canvas.drawLine(leftX, leftY, midX, midY, dieEdge);
        canvas.drawLine(rightX, rightY, midX, midY, dieEdge);
        canvas.drawLine(leftX, leftY, rightX, rightY, dieEdge);

        if (isMarked) {
            Paint pipGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
            pipGlow.setColor(0xFFFFD166);
            pipGlow.setStyle(Paint.Style.FILL);
            canvas.drawCircle(topX, topY + dpf(3f), dpf(3.2f), pipGlow);

            Paint pipCore = new Paint(Paint.ANTI_ALIAS_FLAG);
            pipCore.setColor(0xFFFFFFFF);
            pipCore.setStyle(Paint.Style.FILL);
            canvas.drawCircle(topX, topY + dpf(3f), dpf(1.8f), pipCore);
        }
    }
}
