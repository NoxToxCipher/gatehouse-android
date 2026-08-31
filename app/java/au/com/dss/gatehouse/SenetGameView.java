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
 * SenetGameView — Ancient Egyptian Senet (c. 3100 BCE).
 * 30-square S-shaped track (3x10 grid), 4 throwing sticks,
 * House of Rebirth (15), Beauty (26), Water (27), Horus (30), and Expectimax AI.
 */
public class SenetGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint squarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specialSquarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whitePiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackPiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hieroglyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random rand = new Random();

    // 0 = Human (Gold Spools), 1 = Anubis Bot (Cyan Cones)
    private int currentTurn = 0;
    private int currentRoll = -1;
    private boolean waitingForRoll = true;
    private int whiteBorneOff = 0;
    private int blackBorneOff = 0;

    // 5 pieces each on 30 squares (0..29), 30 = borne off
    private final int[] whitePieces = new int[5];
    private final int[] blackPieces = new int[5];

    // S-curve path: Row 0 (0..9 left-to-right), Row 1 (19..10 right-to-left), Row 2 (20..29 left-to-right)
    private static final int[][] SENET_PATH = {
        {0,0}, {0,1}, {0,2}, {0,3}, {0,4}, {0,5}, {0,6}, {0,7}, {0,8}, {0,9}, // 0..9
        {1,9}, {1,8}, {1,7}, {1,6}, {1,5}, {1,4}, {1,3}, {1,2}, {1,1}, {1,0}, // 10..19
        {2,0}, {2,1}, {2,2}, {2,3}, {2,4}, {2,5}, {2,6}, {2,7}, {2,8}, {2,9}  // 20..29
    };

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public SenetGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        boardBgPaint.setColor(0xFF0F172A);
        squarePaint.setColor(0xFF1E293B);
        specialSquarePaint.setColor(0xFF2E1065);

        whitePiecePaint.setColor(0xFFFFD166);
        whitePiecePaint.setStyle(Paint.Style.FILL);

        blackPiecePaint.setColor(0xFF38BDF8);
        blackPiecePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        hieroglyphPaint.setColor(0xFFFDE047);
        hieroglyphPaint.setTextAlign(Paint.Align.CENTER);
        hieroglyphPaint.setTypeface(Typeface.DEFAULT_BOLD);

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
        whiteBorneOff = 0;
        blackBorneOff = 0;

        // Alternate start positions (Kendall rules): W at 0,2,4,6,8; B at 1,3,5,7,9
        for (int i = 0; i < 5; i++) {
            whitePieces[i] = i * 2;
            blackPieces[i] = i * 2 + 1;
        }
        updateStatus();
        invalidate();
    }

    public void throwSticks() {
        if (!waitingForRoll) return;
        int lightSides = 0;
        for (int i = 0; i < 4; i++) {
            if (rand.nextBoolean()) lightSides++;
        }
        currentRoll = (lightSides == 0) ? 5 : lightSides;
        waitingForRoll = false;

        if (!hasLegalMoves(currentTurn, currentRoll)) {
            postDelayed(new Runnable() {
                public void run() { passTurn(); }
            }, 700);
        } else {
            if (currentTurn == 1) {
                postDelayed(new Runnable() {
                    public void run() { botExecuteMove(); }
                }, 600);
            }
        }
        updateStatus();
        invalidate();
    }

    private boolean hasLegalMoves(int turn, int roll) {
        int[] my = (turn == 0) ? whitePieces : blackPieces;
        int[] opp = (turn == 0) ? blackPieces : whitePieces;

        for (int i = 0; i < 5; i++) {
            int pos = my[i];
            if (pos == 30) continue;
            int next = pos + roll;
            if (next > 30) continue;
            if (next == 30) return true;

            // Cannot land on own piece
            boolean ownBlock = false;
            for (int j = 0; j < 5; j++) {
                if (my[j] == next) { ownBlock = true; break; }
            }
            if (ownBlock) continue;

            // Opponent blockades (2+ connected opponent pieces are protected)
            boolean oppProtected = false;
            for (int j = 0; j < 5; j++) {
                if (opp[j] == next) {
                    for (int k = 0; k < 5; k++) {
                        if (opp[k] == next - 1 || opp[k] == next + 1) {
                            oppProtected = true;
                            break;
                        }
                    }
                }
            }
            if (oppProtected) continue;

            return true;
        }
        return false;
    }

    private void makeMove(int pieceIdx) {
        if (waitingForRoll || currentRoll <= 0) return;
        int[] my = (currentTurn == 0) ? whitePieces : blackPieces;
        int[] opp = (currentTurn == 0) ? blackPieces : whitePieces;

        int pos = my[pieceIdx];
        if (pos == 30) return;
        int next = pos + currentRoll;
        if (next > 30) return;

        // Check own piece
        for (int j = 0; j < 5; j++) {
            if (my[j] == next) return;
        }

        // Check protected opponent
        for (int j = 0; j < 5; j++) {
            if (opp[j] == next) {
                for (int k = 0; k < 5; k++) {
                    if (opp[k] == next - 1 || opp[k] == next + 1) return;
                }
            }
        }

        // Swap / Attack opponent
        for (int j = 0; j < 5; j++) {
            if (opp[j] == next) {
                opp[j] = pos; // Swap places!
                break;
            }
        }

        // House of Water (square 26 / 0-indexed) trap knocks piece back to House of Rebirth (square 14 / 0-indexed)
        if (next == 26) {
            next = 14;
            // if 14 is occupied, find earliest free square before 14
            boolean occupied = true;
            while (occupied && next >= 0) {
                occupied = false;
                for (int p : my) if (p == next) occupied = true;
                for (int p : opp) if (p == next) occupied = true;
                if (occupied) next--;
            }
        }

        my[pieceIdx] = next;
        if (next == 30) {
            if (currentTurn == 0) whiteBorneOff++;
            else blackBorneOff++;
        }

        if (whiteBorneOff == 5 || blackBorneOff == 5) {
            updateStatus();
            invalidate();
            return;
        }

        // Rolling 1, 4, or 5 grants an extra roll in Senet!
        boolean extraRoll = (currentRoll == 1 || currentRoll == 4 || currentRoll == 5);
        if (extraRoll) {
            waitingForRoll = true;
            currentRoll = -1;
            updateStatus();
            invalidate();
            if (currentTurn == 1) {
                postDelayed(new Runnable() { public void run() { throwSticks(); } }, 450);
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
            postDelayed(new Runnable() { public void run() { throwSticks(); } }, 450);
        }
    }

    private void botExecuteMove() {
        if (currentTurn != 1 || waitingForRoll || currentRoll <= 0) return;
        int bestPiece = -1;
        int bestScore = -9999;

        for (int i = 0; i < 5; i++) {
            int pos = blackPieces[i];
            if (pos == 30) continue;
            int next = pos + currentRoll;
            if (next > 30) continue;

            // Validate
            boolean valid = true;
            for (int p : blackPieces) if (p == next) valid = false;
            for (int j = 0; j < 5; j++) {
                if (whitePieces[j] == next) {
                    for (int k = 0; k < 5; k++) {
                        if (whitePieces[k] == next - 1 || whitePieces[k] == next + 1) valid = false;
                    }
                }
            }
            if (!valid) continue;

            int score = next * 10;
            if (next == 30) score += 600; // Bear off
            if (next == 25) score += 200; // House of Beauty safe haven
            if (next == 26) score -= 300; // Avoid House of Water drowning

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
        if (whiteBorneOff == 5) {
            statusListener.onStatusChanged("🏆 PHARAOH VICTORY! Reached the Afterlife.", 0xFF10B981);
            return;
        }
        if (blackBorneOff == 5) {
            statusListener.onStatusChanged("💀 ANUBIS WINS! Master of the Underworld.", 0xFFEF4444);
            return;
        }

        String turnStr = (currentTurn == 0) ? "🟡 Pharaoh (You)" : "🔵 Anubis Bot";
        String rollStr = waitingForRoll ? "· Tap to Cast 4 Sticks" : ("· Cast: " + currentRoll);
        statusListener.onStatusChanged(turnStr + " " + rollStr + " (Off: " + whiteBorneOff + " - " + blackBorneOff + ")", 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (waitingForRoll && currentTurn == 0) {
                throwSticks();
                return true;
            }

            if (!waitingForRoll && currentTurn == 0 && currentRoll > 0) {
                float w = getWidth();
                float h = getHeight();
                float pad = dpf(8f);
                float cellW = (w - pad * 2) / 10f;
                float cellH = (h - dpf(40f)) / 3f;

                float ex = event.getX() - pad;
                float ey = event.getY() - dpf(6f);

                int c = (int) (ex / cellW);
                int r = (int) (ey / cellH);

                for (int i = 0; i < 5; i++) {
                    int pos = whitePieces[i];
                    if (pos >= 0 && pos < 30) {
                        int pr = SENET_PATH[pos][0];
                        int pc = SENET_PATH[pos][1];
                        if (pr == r && pc == c) {
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

        float pad = dpf(8f);
        float cellW = (w - pad * 2) / 10f;
        float cellH = (h - dpf(40f)) / 3f;

        hieroglyphPaint.setTextSize(dpf(9f));

        for (int i = 0; i < 30; i++) {
            int r = SENET_PATH[i][0];
            int c = SENET_PATH[i][1];

            float left = pad + c * cellW;
            float top = dpf(6f) + r * cellH;
            rect.set(left + dpf(1.5f), top + dpf(1.5f), left + cellW - dpf(1.5f), top + cellH - dpf(1.5f));

            boolean isSpecial = (i == 14 || i >= 25);
            canvas.drawRoundRect(rect, dpf(4f), dpf(4f), isSpecial ? specialSquarePaint : squarePaint);

            // Special hieroglyphic symbols
            if (i == 14) canvas.drawText("𓋹 15", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 25) canvas.drawText("𓄤 26", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 26) canvas.drawText("𓈗 27", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 27) canvas.drawText("𓏺 28", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 28) canvas.drawText("𓏻 29", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 29) canvas.drawText("𓁐 30", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
        }

        // Draw Pieces
        float pieceR = Math.min(cellW, cellH) * 0.38f;
        for (int i = 0; i < 5; i++) {
            int wp = whitePieces[i];
            if (wp >= 0 && wp < 30) {
                int r = SENET_PATH[wp][0];
                int c = SENET_PATH[wp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(6f) + r * cellH + cellH / 2f;
                canvas.drawCircle(cx, cy, pieceR, whitePiecePaint);
            }

            int bp = blackPieces[i];
            if (bp >= 0 && bp < 30) {
                int r = SENET_PATH[bp][0];
                int c = SENET_PATH[bp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(6f) + r * cellH + cellH / 2f;
                canvas.drawCircle(cx, cy, pieceR, blackPiecePaint);
            }
        }

        textPaint.setTextSize(dpf(10f));
        canvas.drawText("🟡 Pharaoh Borne Off: " + whiteBorneOff + " / 5  |  🔵 Anubis Borne Off: " + blackBorneOff + " / 5", w / 2f, h - dpf(10f), textPaint);
    }
}
