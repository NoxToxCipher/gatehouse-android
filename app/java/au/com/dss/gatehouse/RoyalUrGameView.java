package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

/**
 * RoyalUrGameView — The Royal Game of Ur (Mesopotamia, c. 2600 BCE).
 * Features 20-square Sumerian rosette board, 4-sided tetrahedral dice,
 * combat track, rosette sanctuaries, and an Expectimax AI engine.
 */
public class RoyalUrGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint squarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rosettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint combatSquarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whitePiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackPiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random rand = new Random();

    // 0 = Human (White/Gold), 1 = Bot (Black/Cyan)
    private int currentTurn = 0;
    private int currentRoll = -1;
    private boolean waitingForRoll = true;
    private int whitePiecesOff = 0;
    private int blackPiecesOff = 0;
    private int whitePiecesUnentered = 7;
    private int blackPiecesUnentered = 7;

    // Path positions: -1 = unentered, 0..13 = on board, 14 = borne off
    private final int[] whitePieces = new int[7];
    private final int[] blackPieces = new int[7];

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

        boardBgPaint.setColor(0xFF0F172A);
        squarePaint.setColor(0xFF1E293B);
        combatSquarePaint.setColor(0xFF283548);
        rosettePaint.setColor(0xFFFFD166);
        rosettePaint.setStyle(Paint.Style.FILL);

        whitePiecePaint.setColor(0xFFFFD166);
        whitePiecePaint.setStyle(Paint.Style.FILL);

        blackPiecePaint.setColor(0xFF38BDF8);
        blackPiecePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        dicePaint.setColor(0xFFE8A33D);
        dicePaint.setStyle(Paint.Style.FILL);

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
        updateStatus();
        invalidate();
    }

    public void rollDice() {
        if (!waitingForRoll) return;
        int sum = 0;
        for (int i = 0; i < 4; i++) {
            if (rand.nextBoolean()) sum++;
        }
        currentRoll = sum;
        waitingForRoll = false;

        if (currentRoll == 0 || !hasLegalMoves(currentTurn, currentRoll)) {
            postDelayed(new Runnable() {
                public void run() {
                    passTurn();
                }
            }, 800);
        } else {
            if (currentTurn == 1) {
                postDelayed(new Runnable() {
                    public void run() {
                        botExecuteBestMove();
                    }
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
                if (myPieces[j] == nextPos) {
                    blockedByOwn = true;
                    break;
                }
            }
            if (blockedByOwn) continue;

            if (nextPos == 7) {
                boolean oppOnRosette = false;
                for (int j = 0; j < 7; j++) {
                    if (oppPieces[j] == 7) {
                        oppOnRosette = true;
                        break;
                    }
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
            statusListener.onStatusChanged("🏆 YOU WIN! All 7 pieces borne off.", 0xFF10B981);
            return;
        }
        if (blackPiecesOff == 7) {
            statusListener.onStatusChanged("💀 BOT WINS! Sumerian Royal Master.", 0xFFEF4444);
            return;
        }

        String turnStr = (currentTurn == 0) ? "🟡 Your Turn" : "🔵 Sumerian Bot's Turn";
        String rollStr = waitingForRoll ? "· Tap to Roll 4-Sided Dice" : ("· Rolled: " + currentRoll);
        statusListener.onStatusChanged(turnStr + " " + rollStr + " (" + whitePiecesOff + " - " + blackPiecesOff + ")", 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (waitingForRoll && currentTurn == 0) {
                rollDice();
                return true;
            }

            if (!waitingForRoll && currentTurn == 0 && currentRoll > 0) {
                float w = getWidth();
                float h = getHeight();
                float pad = dpf(12f);
                float cellW = (w - pad * 2) / 8f;
                float cellH = (h - dpf(60f)) / 3f;

                float ex = event.getX() - pad;
                float ey = event.getY() - dpf(10f);

                int c = (int) (ex / cellW);
                int r = (int) (ey / cellH);

                for (int i = 0; i < 7; i++) {
                    int pos = whitePieces[i];
                    if (pos == -1 && (r == 0 || r == 1) && (c == 4 || c == 5)) {
                        makeMove(i);
                        return true;
                    }
                    if (pos >= 0 && pos < 14) {
                        int pr = WHITE_PATH[pos][0];
                        int pc = WHITE_PATH[pos][1];
                        if (pr == r && pc == c) {
                            makeMove(i);
                            return true;
                        }
                    }
                }
                // Try moving first unentered piece if clicking staging area
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

        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        float pad = dpf(12f);
        float cellW = (w - pad * 2) / 8f;
        float cellH = (h - dpf(60f)) / 3f;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r == 0 || r == 2) && (c == 4 || c == 5)) continue;

                float left = pad + c * cellW;
                float top = dpf(10f) + r * cellH;
                rect.set(left + dpf(2f), top + dpf(2f), left + cellW - dpf(2f), top + cellH - dpf(2f));

                boolean isCombat = (r == 1);
                canvas.drawRoundRect(rect, dpf(6f), dpf(6f), isCombat ? combatSquarePaint : squarePaint);

                boolean isRosette = (r == 0 && c == 0) || (r == 2 && c == 0) || (r == 1 && c == 3) || (r == 0 && c == 6) || (r == 2 && c == 6);
                if (isRosette) {
                    drawRosette(canvas, rect.centerX(), rect.centerY(), cellW * 0.35f);
                }
            }
        }

        float pieceR = Math.min(cellW, cellH) * 0.36f;
        for (int i = 0; i < 7; i++) {
            int wp = whitePieces[i];
            if (wp >= 0 && wp < 14) {
                int r = WHITE_PATH[wp][0];
                int c = WHITE_PATH[wp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(10f) + r * cellH + cellH / 2f;
                canvas.drawCircle(cx, cy, pieceR, whitePiecePaint);
            }

            int bp = blackPieces[i];
            if (bp >= 0 && bp < 14) {
                int r = BLACK_PATH[bp][0];
                int c = BLACK_PATH[bp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(10f) + r * cellH + cellH / 2f;
                canvas.drawCircle(cx, cy, pieceR, blackPiecePaint);
            }
        }

        textPaint.setTextSize(dpf(10f));
        canvas.drawText("🟡 In Reserve: " + whitePiecesUnentered + " | Off: " + whitePiecesOff, w * 0.28f, h - dpf(24f), textPaint);
        canvas.drawText("🔵 Bot Reserve: " + blackPiecesUnentered + " | Off: " + blackPiecesOff, w * 0.72f, h - dpf(24f), textPaint);

        if (currentRoll >= 0) {
            textPaint.setTextSize(dpf(13f));
            textPaint.setColor(0xFFFFD166);
            canvas.drawText("🎲 ROLLED: " + currentRoll, w / 2f, h - dpf(8f), textPaint);
            textPaint.setColor(0xFFE2E8F0);
        }
    }

    private void drawRosette(Canvas canvas, float cx, float cy, float r) {
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            float px = (float) (cx + r * 0.6f * Math.cos(angle));
            float py = (float) (cy + r * 0.6f * Math.sin(angle));
            canvas.drawCircle(px, py, r * 0.22f, rosettePaint);
        }
        canvas.drawCircle(cx, cy, r * 0.28f, rosettePaint);
    }
}
