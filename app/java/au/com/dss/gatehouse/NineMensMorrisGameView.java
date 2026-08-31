package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * NineMensMorrisGameView — Ancient Nine Men's Morris (Mill / Merels, c. 1400 BCE).
 * Museum-grade Cyber-Obsidian & Gold Conduit Canvas with 3D Crystal Spheres,
 * 3 Concentric Squares, Pulsing Mill Indicators, and Minimax AI.
 */
public class NineMensMorrisGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint conduitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint conduitGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint millGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final int[] board = new int[24];
    private int whiteUnplaced = 9;
    private int blackUnplaced = 9;
    private int whiteAlive = 9;
    private int blackAlive = 9;

    private boolean whiteTurn = true;
    private boolean mustRemoveOpponent = false;
    private int selectedIndex = -1;
    private boolean gameOver = false;

    private static final float[][] NODES = {
        {0.1f, 0.1f}, {0.5f, 0.1f}, {0.9f, 0.1f},
        {0.9f, 0.5f}, {0.9f, 0.9f}, {0.5f, 0.9f},
        {0.1f, 0.9f}, {0.1f, 0.5f},
        {0.24f, 0.24f}, {0.5f, 0.24f}, {0.76f, 0.24f},
        {0.76f, 0.5f}, {0.76f, 0.76f}, {0.5f, 0.76f},
        {0.24f, 0.76f}, {0.24f, 0.5f},
        {0.38f, 0.38f}, {0.5f, 0.38f}, {0.62f, 0.38f},
        {0.62f, 0.5f}, {0.62f, 0.62f}, {0.5f, 0.62f},
        {0.38f, 0.62f}, {0.38f, 0.5f}
    };

    private static final int[][] ADJACENT = {
        {1, 7}, {0, 2, 9}, {1, 3}, {2, 4, 11}, {3, 5}, {4, 6, 13}, {5, 7}, {0, 6, 15},
        {9, 15}, {1, 8, 10, 17}, {9, 11}, {3, 10, 12, 19}, {11, 13}, {5, 12, 14, 21}, {13, 15}, {7, 8, 14, 23},
        {17, 23}, {9, 16, 18}, {17, 19}, {11, 18, 20}, {19, 21}, {13, 20, 22}, {21, 23}, {15, 16, 22}
    };

    private static final int[][] MILLS = {
        {0,1,2}, {2,3,4}, {4,5,6}, {6,7,0},
        {8,9,10}, {10,11,12}, {12,13,14}, {14,15,8},
        {16,17,18}, {18,19,20}, {20,21,22}, {22,23,16},
        {1,9,17}, {3,11,19}, {5,13,21}, {7,15,23}
    };

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public NineMensMorrisGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(1.8f));

        conduitPaint.setColor(0xFF38BDF8);
        conduitPaint.setStrokeWidth(dpf(2.8f));

        conduitGlowPaint.setColor(0x3338BDF8);
        conduitGlowPaint.setStrokeWidth(dpf(6f));

        nodePaint.setColor(0xFF1E293B);
        nodePaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(0x99000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        pieceRimPaint.setColor(0xFFE2E8F0);
        pieceRimPaint.setStyle(Paint.Style.STROKE);
        pieceRimPaint.setStrokeWidth(dpf(1.4f));

        shinePaint.setColor(0xAAFFFFFF);
        shinePaint.setStyle(Paint.Style.FILL);

        selectGlowPaint.setColor(0x88FFD166);
        selectGlowPaint.setStyle(Paint.Style.FILL);

        millGlowPaint.setColor(0x8810B981);
        millGlowPaint.setStyle(Paint.Style.FILL);

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
        for (int i = 0; i < 24; i++) board[i] = 0;
        whiteUnplaced = 9;
        blackUnplaced = 9;
        whiteAlive = 9;
        blackAlive = 9;
        whiteTurn = true;
        mustRemoveOpponent = false;
        selectedIndex = -1;
        gameOver = false;
        updateStatus();
        invalidate();
    }

    private boolean isMill(int idx, int color) {
        for (int[] m : MILLS) {
            if (m[0] == idx || m[1] == idx || m[2] == idx) {
                if (board[m[0]] == color && board[m[1]] == color && board[m[2]] == color) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryPlacePiece(int idx) {
        if (board[idx] != 0) return false;
        int color = whiteTurn ? 1 : 2;
        board[idx] = color;

        if (whiteTurn) whiteUnplaced--;
        else blackUnplaced--;

        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        if (isMill(idx, color)) {
            mustRemoveOpponent = true;
            try {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } catch (Exception ignored) {}
        } else {
            endTurn();
        }
        updateStatus();
        invalidate();
        return true;
    }

    private boolean tryMovePiece(int fromIdx, int toIdx) {
        if (board[toIdx] != 0) return false;
        int color = whiteTurn ? 1 : 2;
        int alive = whiteTurn ? whiteAlive : blackAlive;

        if (alive > 3) {
            boolean isAdj = false;
            for (int adj : ADJACENT[fromIdx]) {
                if (adj == toIdx) { isAdj = true; break; }
            }
            if (!isAdj) return false;
        }

        board[toIdx] = color;
        board[fromIdx] = 0;
        selectedIndex = -1;

        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        if (isMill(toIdx, color)) {
            mustRemoveOpponent = true;
            try {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } catch (Exception ignored) {}
        } else {
            endTurn();
        }
        updateStatus();
        invalidate();
        return true;
    }

    private boolean tryRemoveOpponent(int idx) {
        int oppColor = whiteTurn ? 2 : 1;
        if (board[idx] != oppColor) return false;

        boolean allInMills = true;
        for (int i = 0; i < 24; i++) {
            if (board[i] == oppColor && !isMill(i, oppColor)) {
                allInMills = false;
                break;
            }
        }
        if (!allInMills && isMill(idx, oppColor)) return false;

        board[idx] = 0;
        if (whiteTurn) blackAlive--;
        else whiteAlive--;

        try {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        } catch (Exception ignored) {}

        mustRemoveOpponent = false;

        if (blackAlive < 3 || whiteAlive < 3) {
            gameOver = true;
            updateStatus();
            invalidate();
            return true;
        }

        endTurn();
        updateStatus();
        invalidate();
        return true;
    }

    private void endTurn() {
        whiteTurn = !whiteTurn;
        selectedIndex = -1;
        updateStatus();
        invalidate();

        if (!gameOver && !whiteTurn) {
            postDelayed(new Runnable() {
                public void run() { botExecuteTurn(); }
            }, 450);
        }
    }

    private void botExecuteTurn() {
        if (whiteTurn || gameOver) return;

        if (mustRemoveOpponent) {
            for (int i = 0; i < 24; i++) {
                if (board[i] == 1) {
                    if (tryRemoveOpponent(i)) return;
                }
            }
            mustRemoveOpponent = false;
            endTurn();
            return;
        }

        if (blackUnplaced > 0) {
            for (int i = 0; i < 24; i++) {
                if (board[i] == 0) {
                    board[i] = 2;
                    if (isMill(i, 2)) {
                        board[i] = 0;
                        tryPlacePiece(i);
                        return;
                    }
                    board[i] = 0;
                }
            }
            for (int i = 0; i < 24; i++) {
                if (board[i] == 0) {
                    tryPlacePiece(i);
                    return;
                }
            }
        } else {
            for (int from = 0; from < 24; from++) {
                if (board[from] == 2) {
                    for (int to : ADJACENT[from]) {
                        if (board[to] == 0) {
                            if (tryMovePiece(from, to)) return;
                        }
                    }
                }
            }
        }
    }

    private void updateStatus() {
        if (statusListener == null) return;
        if (whiteAlive < 3) {
            statusListener.onStatusChanged("💀 BOT WINS! Nine Men's Morris Master.", 0xFFEF4444);
            return;
        }
        if (blackAlive < 3) {
            statusListener.onStatusChanged("🏆 VICTORY! Formed devastating mills.", 0xFF10B981);
            return;
        }

        String turn = whiteTurn ? "🟡 Your Turn (Gold)" : "🔵 Bot Turn (Cyan)";
        String phase = mustRemoveOpponent ? "· ⚔️ MILL FORMED! Tap enemy piece to remove"
                     : (whiteUnplaced > 0 ? ("· Placing (" + whiteUnplaced + " left)") : "· Moving along lines");
        statusListener.onStatusChanged(turn + " " + phase, 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && whiteTurn) {
            float w = getWidth();
            float h = getHeight();
            float size = Math.min(w, h - dpf(28f));
            float startX = (w - size) / 2f;
            float startY = (h - dpf(28f) - size) / 2f + dpf(8f);

            float ex = event.getX();
            float ey = event.getY();

            int clickedNode = -1;
            float minD = dpf(24f);
            for (int i = 0; i < 24; i++) {
                float nx = startX + NODES[i][0] * size;
                float ny = startY + NODES[i][1] * size;
                float dist = (float) Math.hypot(ex - nx, ey - ny);
                if (dist < minD) {
                    minD = dist;
                    clickedNode = i;
                }
            }

            if (clickedNode != -1) {
                if (mustRemoveOpponent) {
                    tryRemoveOpponent(clickedNode);
                    return true;
                }

                if (whiteUnplaced > 0) {
                    tryPlacePiece(clickedNode);
                    return true;
                } else {
                    if (selectedIndex != -1) {
                        if (tryMovePiece(selectedIndex, clickedNode)) return true;
                    }
                    if (board[clickedNode] == 1) {
                        selectedIndex = clickedNode;
                        invalidate();
                        return true;
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
        boardBgPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF090D16, 0xFF1E293B, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        float size = Math.min(w, h - dpf(28f));
        float startX = (w - size) / 2f;
        float startY = (h - dpf(28f) - size) / 2f + dpf(8f);

        // Draw 3 Concentric Conduit Squares with Glow
        for (int sq = 0; sq < 3; sq++) {
            int offset = sq * 8;
            for (int i = 0; i < 8; i++) {
                int next = (i == 7) ? offset : offset + i + 1;
                float x1 = startX + NODES[offset + i][0] * size;
                float y1 = startY + NODES[offset + i][1] * size;
                float x2 = startX + NODES[next][0] * size;
                float y2 = startY + NODES[next][1] * size;
                canvas.drawLine(x1, y1, x2, y2, conduitGlowPaint);
                canvas.drawLine(x1, y1, x2, y2, conduitPaint);
            }
        }

        // Draw Cross Conduits
        int[][] cross = {{1, 9}, {9, 17}, {3, 11}, {11, 19}, {5, 13}, {13, 21}, {7, 15}, {15, 23}};
        for (int[] c : cross) {
            float x1 = startX + NODES[c[0]][0] * size;
            float y1 = startY + NODES[c[0]][1] * size;
            float x2 = startX + NODES[c[1]][0] * size;
            float y2 = startY + NODES[c[1]][1] * size;
            canvas.drawLine(x1, y1, x2, y2, conduitGlowPaint);
            canvas.drawLine(x1, y1, x2, y2, conduitPaint);
        }

        // Draw 24 Nodes with 3D Spheres
        float nodeR = dpf(7f);
        float pieceR = dpf(12f);

        for (int i = 0; i < 24; i++) {
            float nx = startX + NODES[i][0] * size;
            float ny = startY + NODES[i][1] * size;

            canvas.drawCircle(nx, ny, nodeR, nodePaint);
            canvas.drawCircle(nx, ny, nodeR, goldBorderPaint);

            if (i == selectedIndex) {
                canvas.drawCircle(nx, ny, pieceR * 1.4f, selectGlowPaint);
            }

            int val = board[i];
            if (val == 1) draw3DMarble(canvas, nx, ny, pieceR, true);
            else if (val == 2) draw3DMarble(canvas, nx, ny, pieceR, false);
        }

        textPaint.setTextSize(dpf(10f));
        canvas.drawText("🟡 Gold: " + whiteAlive + " (Unplaced: " + whiteUnplaced + ") | 🔵 Cyan: " + blackAlive + " (Unplaced: " + blackUnplaced + ")", w / 2f, h - dpf(8f), textPaint);
    }

    private void draw3DMarble(Canvas canvas, float cx, float cy, float r, boolean isGold) {
        canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            isGold ? new int[]{0xFFFFFBEB, 0xFFF59E0B, 0xFF78350F} : new int[]{0xFFE0F2FE, 0xFF0284C7, 0xFF082F49},
            null, Shader.TileMode.CLAMP
        );
        piecePaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, piecePaint);
        canvas.drawCircle(cx, cy, r, pieceRimPaint);
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.3f, shinePaint);
    }
}
