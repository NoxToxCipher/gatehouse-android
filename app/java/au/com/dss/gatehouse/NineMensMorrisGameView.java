package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * NineMensMorrisGameView — Ancient Nine Men's Morris (Mill / Merels, c. 1400 BCE).
 * 3 concentric squares with 24 intersection points.
 * 3 Phases: 1. Placing (9 pieces each), 2. Moving along lines, 3. Flying (when down to 3 pieces).
 * Forming 3-in-a-row (Mill) captures opponent piece.
 */
public class NineMensMorrisGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whitePiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackPiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    // 24 points: 0 = empty, 1 = White (Human), 2 = Black (Bot)
    private final int[] board = new int[24];
    private int whiteUnplaced = 9;
    private int blackUnplaced = 9;
    private int whiteAlive = 9;
    private int blackAlive = 9;

    // true = White (Human), false = Black (Bot)
    private boolean whiteTurn = true;
    private boolean mustRemoveOpponent = false;
    private int selectedIndex = -1;
    private boolean gameOver = false;

    // Normalized coordinate points (0.0 to 1.0) for 24 intersections
    private static final float[][] NODES = {
        // Outer square (0..7)
        {0.1f, 0.1f}, {0.5f, 0.1f}, {0.9f, 0.1f},
        {0.9f, 0.5f}, {0.9f, 0.9f}, {0.5f, 0.9f},
        {0.1f, 0.9f}, {0.1f, 0.5f},
        // Middle square (8..15)
        {0.24f, 0.24f}, {0.5f, 0.24f}, {0.76f, 0.24f},
        {0.76f, 0.5f}, {0.76f, 0.76f}, {0.5f, 0.76f},
        {0.24f, 0.76f}, {0.24f, 0.5f},
        // Inner square (16..23)
        {0.38f, 0.38f}, {0.5f, 0.38f}, {0.62f, 0.38f},
        {0.62f, 0.5f}, {0.62f, 0.62f}, {0.5f, 0.62f},
        {0.38f, 0.62f}, {0.38f, 0.5f}
    };

    // Adjacency graph for moving
    private static final int[][] ADJACENT = {
        {1, 7}, {0, 2, 9}, {1, 3}, {2, 4, 11}, {3, 5}, {4, 6, 13}, {5, 7}, {0, 6, 15}, // 0..7
        {9, 15}, {1, 8, 10, 17}, {9, 11}, {3, 10, 12, 19}, {11, 13}, {5, 12, 14, 21}, {13, 15}, {7, 8, 14, 23}, // 8..15
        {17, 23}, {9, 16, 18}, {17, 19}, {11, 18, 20}, {19, 21}, {13, 20, 22}, {21, 23}, {15, 16, 22} // 16..23
    };

    // All 16 possible 3-in-a-row Mill combinations
    private static final int[][] MILLS = {
        // Outer square
        {0,1,2}, {2,3,4}, {4,5,6}, {6,7,0},
        // Middle square
        {8,9,10}, {10,11,12}, {12,13,14}, {14,15,8},
        // Inner square
        {16,17,18}, {18,19,20}, {20,21,22}, {22,23,16},
        // Cross lines
        {1,9,17}, {3,11,19}, {5,13,21}, {7,15,23}
    };

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public NineMensMorrisGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        boardBgPaint.setColor(0xFF0F172A);
        linePaint.setColor(0xFF475569);
        linePaint.setStrokeWidth(dpf(2.5f));

        nodePaint.setColor(0xFF64748B);
        nodePaint.setStyle(Paint.Style.FILL);

        whitePiecePaint.setColor(0xFFFFD166); // Gold
        whitePiecePaint.setStyle(Paint.Style.FILL);

        blackPiecePaint.setColor(0xFF38BDF8); // Cyan
        blackPiecePaint.setStyle(Paint.Style.FILL);

        selectPaint.setColor(0x88FFD166);
        selectPaint.setStyle(Paint.Style.FILL);

        targetDotPaint.setColor(0xFFFFD166);
        targetDotPaint.setStyle(Paint.Style.FILL);

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

        if (isMill(idx, color)) {
            mustRemoveOpponent = true;
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

        // In Flying phase (3 pieces left), can move anywhere! Otherwise must be adjacent
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

        if (isMill(toIdx, color)) {
            mustRemoveOpponent = true;
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

        // If all opponent pieces are in mills, can remove any; otherwise cannot remove piece in a mill
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

        mustRemoveOpponent = false;

        // Victory check (opponent down to 2 pieces or no legal moves)
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

        // Placing phase
        if (blackUnplaced > 0) {
            // Pick move that creates a mill or blocks White mill
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
            // Moving phase
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
            statusListener.onStatusChanged("🏆 VICTORY! You formed devastating mills.", 0xFF10B981);
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
            float size = Math.min(w, h - dpf(30f));
            float startX = (w - size) / 2f;
            float startY = (h - dpf(30f) - size) / 2f + dpf(10f);

            float ex = event.getX();
            float ey = event.getY();

            // Find closest node
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
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        float size = Math.min(w, h - dpf(30f));
        float startX = (w - size) / 2f;
        float startY = (h - dpf(30f) - size) / 2f + dpf(10f);

        // Draw 3 concentric squares
        for (int sq = 0; sq < 3; sq++) {
            int offset = sq * 8;
            for (int i = 0; i < 8; i++) {
                int next = (i == 7) ? offset : offset + i + 1;
                float x1 = startX + NODES[offset + i][0] * size;
                float y1 = startY + NODES[offset + i][1] * size;
                float x2 = startX + NODES[next][0] * size;
                float y2 = startY + NODES[next][1] * size;
                canvas.drawLine(x1, y1, x2, y2, linePaint);
            }
        }

        // Draw connecting cross lines
        int[][] cross = {{1, 9}, {9, 17}, {3, 11}, {11, 19}, {5, 13}, {13, 21}, {7, 15}, {15, 23}};
        for (int[] c : cross) {
            float x1 = startX + NODES[c[0]][0] * size;
            float y1 = startY + NODES[c[0]][1] * size;
            float x2 = startX + NODES[c[1]][0] * size;
            float y2 = startY + NODES[c[1]][1] * size;
            canvas.drawLine(x1, y1, x2, y2, linePaint);
        }

        // Draw 24 intersection nodes and pieces
        float nodeR = dpf(6f);
        float pieceR = dpf(11f);

        for (int i = 0; i < 24; i++) {
            float nx = startX + NODES[i][0] * size;
            float ny = startY + NODES[i][1] * size;

            canvas.drawCircle(nx, ny, nodeR, nodePaint);

            if (i == selectedIndex) {
                canvas.drawCircle(nx, ny, pieceR * 1.35f, selectPaint);
            }

            int val = board[i];
            if (val == 1) canvas.drawCircle(nx, ny, pieceR, whitePiecePaint);
            else if (val == 2) canvas.drawCircle(nx, ny, pieceR, blackPiecePaint);
        }

        textPaint.setTextSize(dpf(10f));
        canvas.drawText("🟡 Pieces: " + whiteAlive + " (Unplaced: " + whiteUnplaced + ") | 🔵 Bot: " + blackAlive + " (Unplaced: " + blackUnplaced + ")", w / 2f, h - dpf(8f), textPaint);
    }
}
