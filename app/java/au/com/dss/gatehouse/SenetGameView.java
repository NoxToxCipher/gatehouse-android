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
 * SenetGameView — Ancient Egyptian Senet (c. 3100 BCE).
 * High-fidelity Egyptian Sandstone & Gold Leaf Hieroglyphic Canvas with 3D Spools & Cones,
 * House of Water/Beauty/Horus inlays, and animated 4 casting sticks.
 */
public class SenetGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specialTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hieroglyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stickLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stickDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random rand = new Random();

    private int currentTurn = 0;
    private int currentRoll = -1;
    private boolean waitingForRoll = true;
    private int whiteBorneOff = 0;
    private int blackBorneOff = 0;

    private final int[] whitePieces = new int[5];
    private final int[] blackPieces = new int[5];
    private final boolean[] lastSticksLight = new boolean[4];

    private static final int[][] SENET_PATH = {
        {0,0}, {0,1}, {0,2}, {0,3}, {0,4}, {0,5}, {0,6}, {0,7}, {0,8}, {0,9},
        {1,9}, {1,8}, {1,7}, {1,6}, {1,5}, {1,4}, {1,3}, {1,2}, {1,1}, {1,0},
        {2,0}, {2,1}, {2,2}, {2,3}, {2,4}, {2,5}, {2,6}, {2,7}, {2,8}, {2,9}
    };

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public SenetGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(1.5f));

        shadowPaint.setColor(0x88000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        shinePaint.setColor(0xAAFFFFFF);
        shinePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        hieroglyphPaint.setColor(0xFFFDE047);
        hieroglyphPaint.setTextAlign(Paint.Align.CENTER);
        hieroglyphPaint.setTypeface(Typeface.DEFAULT_BOLD);

        stickLightPaint.setColor(0xFFFEF08A);
        stickLightPaint.setStyle(Paint.Style.FILL);

        stickDarkPaint.setColor(0xFF451A03);
        stickDarkPaint.setStyle(Paint.Style.FILL);

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

        for (int i = 0; i < 5; i++) {
            whitePieces[i] = i * 2;
            blackPieces[i] = i * 2 + 1;
        }
        for (int i = 0; i < 4; i++) lastSticksLight[i] = false;
        updateStatus();
        invalidate();
    }

    public void throwSticks() {
        if (!waitingForRoll) return;
        int lightSides = 0;
        for (int i = 0; i < 4; i++) {
            boolean isLight = rand.nextBoolean();
            lastSticksLight[i] = isLight;
            if (isLight) lightSides++;
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

            boolean ownBlock = false;
            for (int j = 0; j < 5; j++) {
                if (my[j] == next) { ownBlock = true; break; }
            }
            if (ownBlock) continue;

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

        for (int j = 0; j < 5; j++) {
            if (my[j] == next) return;
        }

        for (int j = 0; j < 5; j++) {
            if (opp[j] == next) {
                for (int k = 0; k < 5; k++) {
                    if (opp[k] == next - 1 || opp[k] == next + 1) return;
                }
            }
        }

        for (int j = 0; j < 5; j++) {
            if (opp[j] == next) {
                opp[j] = pos;
                break;
            }
        }

        // House of Water (26) trap -> back to House of Rebirth (14)
        if (next == 26) {
            next = 14;
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
            if (next == 30) score += 600;
            if (next == 25) score += 200;
            if (next == 26) score -= 300;

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
        String rollStr = waitingForRoll ? "· Tap 4 Casting Sticks" : ("· Cast: " + currentRoll);
        statusListener.onStatusChanged(turnStr + " " + rollStr + " (" + whiteBorneOff + " - " + blackBorneOff + ")", 0xFFFFD166);
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
                float pad = dpf(8f);
                float cellW = (w - pad * 2) / 10f;
                float cellH = (getHeight() - dpf(54f)) / 3f;

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

        // Sandstone/Obsidian Frame
        rect.set(0, 0, w, h);
        boardFramePaint.setShader(new LinearGradient(0, 0, w, h, 0xFF1C1917, 0xFF2E1065, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardFramePaint);

        rect.set(dpf(2f), dpf(2f), w - dpf(2f), h - dpf(2f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        float pad = dpf(8f);
        float cellW = (w - pad * 2) / 10f;
        float cellH = (h - dpf(54f)) / 3f;

        hieroglyphPaint.setTextSize(dpf(9f));

        for (int i = 0; i < 30; i++) {
            int r = SENET_PATH[i][0];
            int c = SENET_PATH[i][1];

            float left = pad + c * cellW;
            float top = dpf(6f) + r * cellH;
            rect.set(left + dpf(1.5f), top + dpf(1.5f), left + cellW - dpf(1.5f), top + cellH - dpf(1.5f));

            boolean isSpecial = (i == 14 || i >= 25);
            if (isSpecial) {
                specialTilePaint.setColor(i == 26 ? 0xFF0369A1 : 0xFF4C1D95);
                canvas.drawRoundRect(rect, dpf(5f), dpf(5f), specialTilePaint);
            } else {
                tilePaint.setColor((r + c) % 2 == 0 ? 0xFF1E293B : 0xFF0F172A);
                canvas.drawRoundRect(rect, dpf(5f), dpf(5f), tilePaint);
            }

            canvas.drawRoundRect(rect, dpf(5f), dpf(5f), goldBorderPaint);

            // Hieroglyphic Inlays
            if (i == 14) canvas.drawText("𓋹 15", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 25) canvas.drawText("𓄤 26", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 26) canvas.drawText("𓈗 27", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 27) canvas.drawText("𓏺 28", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 28) canvas.drawText("𓏻 29", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
            if (i == 29) canvas.drawText("𓁐 30", rect.centerX(), rect.centerY() + dpf(3f), hieroglyphPaint);
        }

        // Draw 3D Pieces (Spools & Cones)
        float pieceR = Math.min(cellW, cellH) * 0.38f;
        for (int i = 0; i < 5; i++) {
            int wp = whitePieces[i];
            if (wp >= 0 && wp < 30) {
                int r = SENET_PATH[wp][0];
                int c = SENET_PATH[wp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(6f) + r * cellH + cellH / 2f;
                drawEgyptianPiece(canvas, cx, cy, pieceR, true);
            }

            int bp = blackPieces[i];
            if (bp >= 0 && bp < 30) {
                int r = SENET_PATH[bp][0];
                int c = SENET_PATH[bp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(6f) + r * cellH + cellH / 2f;
                drawEgyptianPiece(canvas, cx, cy, pieceR, false);
            }
        }

        // Bottom Tray: 4 Egyptian Casting Sticks
        float trayTop = h - dpf(44f);
        rect.set(pad, trayTop, w - pad, h - dpf(6f));
        canvas.drawRoundRect(rect, dpf(8f), dpf(8f), tilePaint);
        canvas.drawRoundRect(rect, dpf(8f), dpf(8f), goldBorderPaint);

        float stickStartX = pad + dpf(20f);
        float stickSpacing = (w - pad * 2 - dpf(40f)) / 5f;
        for (int s = 0; s < 4; s++) {
            float scx = stickStartX + s * stickSpacing;
            float scy = trayTop + dpf(19f);
            drawCastingStick(canvas, scx, scy, dpf(28f), dpf(6f), lastSticksLight[s]);
        }

        float badgeX = stickStartX + 4 * stickSpacing;
        float badgeY = trayTop + dpf(19f);
        textPaint.setTextSize(dpf(12f));
        textPaint.setColor(currentRoll > 0 ? 0xFFFDE047 : 0xFF94A3B8);
        canvas.drawText(currentRoll >= 0 ? "🥢 " + currentRoll : "CAST", badgeX, badgeY + dpf(4f), textPaint);
    }

    private void drawEgyptianPiece(Canvas canvas, float cx, float cy, float r, boolean isPharaoh) {
        canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            isPharaoh ? new int[]{0xFFFEF08A, 0xFFEAB308, 0xFF713F12} : new int[]{0xFFBAE6FD, 0xFF0284C7, 0xFF082F49},
            null, Shader.TileMode.CLAMP
        );
        piecePaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, piecePaint);

        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.28f, shinePaint);
        canvas.drawCircle(cx, cy, r * 0.2f, goldBorderPaint);
    }

    private void drawCastingStick(Canvas canvas, float cx, float cy, float len, float thickness, boolean isLight) {
        rect.set(cx - len / 2f, cy - thickness / 2f, cx + len / 2f, cy + thickness / 2f);
        canvas.drawRoundRect(rect, thickness / 2f, thickness / 2f, isLight ? stickLightPaint : stickDarkPaint);
        canvas.drawRoundRect(rect, thickness / 2f, thickness / 2f, goldBorderPaint);
    }
}
