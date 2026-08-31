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
import java.util.Random;

/**
 * RoyalUrGameView — The Royal Game of Ur (Mesopotamia, c. 2600 BCE).
 * High-fidelity Lapis Lazuli & Gold Sumerian Mosaic Canvas with 3D counters,
 * intricate 8-petal rosettes, and animated tetrahedral dice.
 */
public class RoyalUrGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileWoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileLapisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldInlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldRosettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rubyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whitePiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackPiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceInlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint diceFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path dicePath = new Path();
    private final Random rand = new Random();

    private int currentTurn = 0;
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

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public RoyalUrGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        boardBorderPaint.setColor(0xFF0D131F);
        tileWoodPaint.setColor(0xFF1E293B);
        tileLapisPaint.setColor(0xFF1E1B4B);

        goldInlayPaint.setColor(0xFFEAB308);
        goldInlayPaint.setStyle(Paint.Style.STROKE);
        goldInlayPaint.setStrokeWidth(dpf(1.5f));

        goldRosettePaint.setColor(0xFFFFD166);
        goldRosettePaint.setStyle(Paint.Style.FILL);

        rubyPaint.setColor(0xFFDC2626);
        rubyPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(0x88000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        whitePiecePaint.setStyle(Paint.Style.FILL);
        blackPiecePaint.setStyle(Paint.Style.FILL);

        pieceShinePaint.setColor(0xAAFFFFFF);
        pieceShinePaint.setStyle(Paint.Style.FILL);

        pieceInlayPaint.setColor(0xFF0F172A);
        pieceInlayPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        diceFramePaint.setColor(0xFF1E293B);
        diceFramePaint.setStyle(Paint.Style.FILL);

        resetGame();
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void resetGame() {
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

    public void rollDice() {
        if (!waitingForRoll) return;
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            boolean marked = rand.nextBoolean();
            lastDiceFaces[i] = marked;
            if (marked) sum++;
        }
        currentRoll = sum;
        waitingForRoll = false;

        if (currentRoll == 0 || !hasLegalMoves(currentTurn, currentRoll)) {
            postDelayed(new Runnable() {
                public void run() { passTurn(); }
            }, 800);
        } else {
            if (currentTurn == 1) {
                postDelayed(new Runnable() {
                    public void run() { botExecuteBestMove(); }
                }, 600);
            }
        }
        updateStatus();
        invalidate();
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

    private void makeMove(int pieceIdx) {
        if (waitingForRoll || currentRoll <= 0) return;
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

            int score = nextPos * 10;
            if (nextPos == 14) score += 500;
            if (nextPos < 14 && IS_ROSETTE[nextPos]) score += 300;
            if (nextPos >= 4 && nextPos <= 11) {
                for (int j = 0; j < 7; j++) {
                    if (whitePieces[j] == nextPos) score += 400;
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
            statusListener.onStatusChanged("🏆 SUMERIAN VICTORY! Borne off all 7 pieces.", 0xFF10B981);
            return;
        }
        if (blackPiecesOff == 7) {
            statusListener.onStatusChanged("💀 BOT WINS! Master of Sumerian Ur.", 0xFFEF4444);
            return;
        }

        String turnStr = (currentTurn == 0) ? "🟡 Your Turn (Gold)" : "🔵 Sumerian Bot (Lapis)";
        String rollStr = waitingForRoll ? "· Tap 4 Tetrahedral Dice" : ("· Rolled: " + currentRoll);
        statusListener.onStatusChanged(turnStr + " " + rollStr + " (" + whitePiecesOff + " - " + blackPiecesOff + ")", 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float w = getWidth();
            float h = getHeight();
            float pad = dpf(10f);
            float cellW = (w - pad * 2) / 8f;
            float cellH = (h - dpf(68f)) / 3f;

            float ex = event.getX();
            float ey = event.getY();

            // Check if tapped the dice tray at the bottom
            if (ey > h - dpf(48f)) {
                if (waitingForRoll && currentTurn == 0) {
                    rollDice();
                    return true;
                }
            }

            if (waitingForRoll && currentTurn == 0) {
                rollDice();
                return true;
            }

            if (!waitingForRoll && currentTurn == 0 && currentRoll > 0) {
                int c = (int) ((ex - pad) / cellW);
                int r = (int) ((ey - dpf(10f)) / cellH);

                for (int i = 0; i < 7; i++) {
                    int pos = whitePieces[i];
                    if (pos >= 0 && pos < 14) {
                        int pr = WHITE_PATH[pos][0];
                        int pc = WHITE_PATH[pos][1];
                        if (pr == r && pc == c) {
                            makeMove(i);
                            return true;
                        }
                    }
                }
                if (whitePiecesUnentered > 0) {
                    for (int i = 0; i < 7; i++) {
                        if (whitePieces[i] == -1) {
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

        // Outer board frame with subtle gold edge
        rect.set(0, 0, w, h);
        boardBorderPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF090D16, 0xFF172554, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBorderPaint);

        rect.set(dpf(2f), dpf(2f), w - dpf(2f), h - dpf(2f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldInlayPaint);

        float pad = dpf(10f);
        float cellW = (w - pad * 2) / 8f;
        float cellH = (h - dpf(68f)) / 3f;

        // Draw 20 squares of Ur
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r == 0 || r == 2) && (c == 4 || c == 5)) continue;

                float left = pad + c * cellW;
                float top = dpf(10f) + r * cellH;
                rect.set(left + dpf(2f), top + dpf(2f), left + cellW - dpf(2f), top + cellH - dpf(2f));

                boolean isCombat = (r == 1);
                boolean isRosette = (r == 0 && c == 0) || (r == 2 && c == 0) || (r == 1 && c == 3) || (r == 0 && c == 6) || (r == 2 && c == 6);

                // Tile Background
                if (isRosette) {
                    tileLapisPaint.setColor(0xFF1E1B4B);
                    canvas.drawRoundRect(rect, dpf(6f), dpf(6f), tileLapisPaint);
                    drawOrnateRosette(canvas, rect.centerX(), rect.centerY(), cellW * 0.38f);
                } else if (isCombat) {
                    tileWoodPaint.setColor(0xFF1E293B);
                    canvas.drawRoundRect(rect, dpf(6f), dpf(6f), tileWoodPaint);
                    drawFiveDots(canvas, rect.centerX(), rect.centerY(), cellW * 0.28f);
                } else {
                    tileWoodPaint.setColor(0xFF0F172A);
                    canvas.drawRoundRect(rect, dpf(6f), dpf(6f), tileWoodPaint);
                    drawPyramidPattern(canvas, rect);
                }

                // Inlay gold outline
                canvas.drawRoundRect(rect, dpf(6f), dpf(6f), goldInlayPaint);
            }
        }

        // Draw 3D Counters with Shading
        float pieceR = Math.min(cellW, cellH) * 0.38f;
        for (int i = 0; i < 7; i++) {
            // White/Gold pieces
            int wp = whitePieces[i];
            if (wp >= 0 && wp < 14) {
                int r = WHITE_PATH[wp][0];
                int c = WHITE_PATH[wp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(10f) + r * cellH + cellH / 2f;
                drawSumerianPiece(canvas, cx, cy, pieceR, true);
            }

            // Black/Cyan pieces
            int bp = blackPieces[i];
            if (bp >= 0 && bp < 14) {
                int r = BLACK_PATH[bp][0];
                int c = BLACK_PATH[bp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(10f) + r * cellH + cellH / 2f;
                drawSumerianPiece(canvas, cx, cy, pieceR, false);
            }
        }

        // Tray Section: 4 Tetrahedral Pyramidal Dice + Reserve Track
        float diceTrayTop = h - dpf(54f);
        rect.set(pad, diceTrayTop, w - pad, h - dpf(8f));
        canvas.drawRoundRect(rect, dpf(10f), dpf(10f), diceFramePaint);
        canvas.drawRoundRect(rect, dpf(10f), dpf(10f), goldInlayPaint);

        // Draw 4 Tetrahedral Dice
        float diceStartX = pad + dpf(20f);
        float diceSpacing = (w - pad * 2 - dpf(40f)) / 5f;
        for (int d = 0; d < 4; d++) {
            float dcx = diceStartX + d * diceSpacing;
            float dcy = diceTrayTop + dpf(23f);
            drawTetrahedralDie(canvas, dcx, dcy, dpf(12f), lastDiceFaces[d]);
        }

        // Roll Score Badge
        float badgeX = diceStartX + 4 * diceSpacing;
        float badgeY = diceTrayTop + dpf(23f);
        textPaint.setTextSize(dpf(12f));
        textPaint.setColor(currentRoll > 0 ? 0xFFFFD166 : 0xFF94A3B8);
        canvas.drawText(currentRoll >= 0 ? "🎲 " + currentRoll : "ROLL", badgeX, badgeY + dpf(4f), textPaint);
    }

    private void drawSumerianPiece(Canvas canvas, float cx, float cy, float r, boolean isGold) {
        // Drop shadow
        canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);

        // 3D Spherical Radial Gradient
        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            isGold ? new int[]{0xFFFFFBEB, 0xFFF59E0B, 0xFF78350F} : new int[]{0xFFE0F2FE, 0xFF0284C7, 0xFF0F172A},
            null, Shader.TileMode.CLAMP
        );
        (isGold ? whitePiecePaint : blackPiecePaint).setShader(grad);
        canvas.drawCircle(cx, cy, r, isGold ? whitePiecePaint : blackPiecePaint);

        // Specular highlight crescent
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.3f, pieceShinePaint);

        // Inlaid dot markers
        canvas.drawCircle(cx, cy, r * 0.22f, pieceInlayPaint);
        canvas.drawCircle(cx, cy, r * 0.12f, goldRosettePaint);
    }

    private void drawOrnateRosette(Canvas canvas, float cx, float cy, float r) {
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            float px = (float) (cx + r * 0.6f * Math.cos(angle));
            float py = (float) (cy + r * 0.6f * Math.sin(angle));
            canvas.drawCircle(px, py, r * 0.26f, goldRosettePaint);
            canvas.drawCircle(px, py, r * 0.13f, rubyPaint);
        }
        canvas.drawCircle(cx, cy, r * 0.34f, goldRosettePaint);
        canvas.drawCircle(cx, cy, r * 0.18f, rubyPaint);
    }

    private void drawFiveDots(Canvas canvas, float cx, float cy, float r) {
        goldRosettePaint.setColor(0xFF38BDF8);
        canvas.drawCircle(cx, cy, dpf(2.5f), goldRosettePaint);
        canvas.drawCircle(cx - r, cy - r, dpf(2f), goldRosettePaint);
        canvas.drawCircle(cx + r, cy - r, dpf(2f), goldRosettePaint);
        canvas.drawCircle(cx - r, cy + r, dpf(2f), goldRosettePaint);
        canvas.drawCircle(cx + r, cy + r, dpf(2f), goldRosettePaint);
        goldRosettePaint.setColor(0xFFFFD166);
    }

    private void drawPyramidPattern(Canvas canvas, RectF r) {
        float cx = r.centerX();
        float cy = r.centerY();
        float s = Math.min(r.width(), r.height()) * 0.25f;
        canvas.drawLine(cx - s, cy, cx + s, cy, goldInlayPaint);
        canvas.drawLine(cx, cy - s, cx, cy + s, goldInlayPaint);
    }

    private void drawTetrahedralDie(Canvas canvas, float cx, float cy, float size, boolean isMarked) {
        dicePath.reset();
        dicePath.moveTo(cx, cy - size);
        dicePath.lineTo(cx + size, cy + size * 0.7f);
        dicePath.lineTo(cx - size, cy + size * 0.7f);
        dicePath.close();

        Paint diePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diePaint.setColor(isMarked ? 0xFFF59E0B : 0xFF334155);
        diePaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(dicePath, diePaint);

        Paint dieEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
        dieEdge.setColor(0xFFE2E8F0);
        dieEdge.setStyle(Paint.Style.STROKE);
        dieEdge.setStrokeWidth(dpf(1.5f));
        canvas.drawPath(dicePath, dieEdge);

        if (isMarked) {
            // Marked white tip
            Paint tip = new Paint(Paint.ANTI_ALIAS_FLAG);
            tip.setColor(0xFFFFFFFF);
            tip.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy - size * 0.6f, dpf(3f), tip);
        }
    }
}
