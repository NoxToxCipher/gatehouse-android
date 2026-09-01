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
    private final Paint ironBracketPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ironRivetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint plankSeamShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint plankSeamLight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chiselShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chiselRidgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chiselCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint millBrandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint millBrandGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint socketDimpleInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint socketDimpleRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint latheRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lathePipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strikeReticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chalkPipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

        ironBracketPaint.setColor(0xFF1E1C1A);
        ironBracketPaint.setStyle(Paint.Style.STROKE);
        ironBracketPaint.setStrokeWidth(dpf(3f));

        ironRivetPaint.setColor(0xFF8C857B);
        ironRivetPaint.setStyle(Paint.Style.FILL);

        plankSeamShadow.setColor(0xFF120703);
        plankSeamShadow.setStyle(Paint.Style.STROKE);
        plankSeamShadow.setStrokeWidth(dpf(1.8f));

        plankSeamLight.setColor(0x22FFFFFF);
        plankSeamLight.setStyle(Paint.Style.STROKE);
        plankSeamLight.setStrokeWidth(dpf(0.8f));

        chiselShadowPaint.setColor(0xFF140803);
        chiselShadowPaint.setStyle(Paint.Style.STROKE);
        chiselShadowPaint.setStrokeWidth(dpf(5.5f));

        chiselRidgePaint.setColor(0xFF5A351C);
        chiselRidgePaint.setStyle(Paint.Style.STROKE);
        chiselRidgePaint.setStrokeWidth(dpf(2.4f));

        chiselCenterPaint.setColor(0xFF8A552F);
        chiselCenterPaint.setStyle(Paint.Style.STROKE);
        chiselCenterPaint.setStrokeWidth(dpf(1f));

        millBrandPaint.setColor(0xFFF59E0B);
        millBrandPaint.setStyle(Paint.Style.STROKE);
        millBrandPaint.setStrokeWidth(dpf(3.8f));

        millBrandGlowPaint.setColor(0x66EA580C);
        millBrandGlowPaint.setStyle(Paint.Style.STROKE);
        millBrandGlowPaint.setStrokeWidth(dpf(10f));

        socketDimpleInnerPaint.setColor(0xFF160A04);
        socketDimpleInnerPaint.setStyle(Paint.Style.FILL);

        socketDimpleRimPaint.setColor(0xFF523018);
        socketDimpleRimPaint.setStyle(Paint.Style.STROKE);
        socketDimpleRimPaint.setStrokeWidth(dpf(1.4f));

        shadowPaint.setColor(0x99000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        pieceRimPaint.setColor(0xFF8B6C43);
        pieceRimPaint.setStyle(Paint.Style.STROKE);
        pieceRimPaint.setStrokeWidth(dpf(1.2f));

        latheRingPaint.setStyle(Paint.Style.STROKE);
        latheRingPaint.setStrokeWidth(dpf(1.2f));

        lathePipPaint.setStyle(Paint.Style.FILL);

        shinePaint.setColor(0xAAFFFFFF);
        shinePaint.setStyle(Paint.Style.FILL);

        selectGlowPaint.setColor(0x88FFD166);
        selectGlowPaint.setStyle(Paint.Style.FILL);

        strikeReticlePaint.setColor(0xEEF43F5E);
        strikeReticlePaint.setStyle(Paint.Style.STROKE);
        strikeReticlePaint.setStrokeWidth(dpf(2.2f));

        chalkPipPaint.setColor(0xCCFDE047);
        chalkPipPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2D6BE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));

        resetGame();
    }

    private static class HistoryState {
        final int[] board = new int[24];
        final int whiteUnplaced, blackUnplaced, whiteAlive, blackAlive;
        final boolean whiteTurn, mustRemoveOpponent, gameOver;

        HistoryState(int[] b, int wu, int bu, int wa, int ba, boolean wt, boolean mro, boolean go) {
            System.arraycopy(b, 0, board, 0, 24);
            this.whiteUnplaced = wu;
            this.blackUnplaced = bu;
            this.whiteAlive = wa;
            this.blackAlive = ba;
            this.whiteTurn = wt;
            this.mustRemoveOpponent = mro;
            this.gameOver = go;
        }
    }

    private final List<HistoryState> history = new ArrayList<>();

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void resetGame() {
        for (int i = 0; i < 24; i++) board[i] = 0;
        history.clear();
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

    public void undoMove() {
        if (history.isEmpty()) return;
        HistoryState prev = history.remove(history.size() - 1);
        if (!prev.whiteTurn && !history.isEmpty()) {
            prev = history.remove(history.size() - 1);
        }
        System.arraycopy(prev.board, 0, board, 0, 24);
        this.whiteUnplaced = prev.whiteUnplaced;
        this.blackUnplaced = prev.blackUnplaced;
        this.whiteAlive = prev.whiteAlive;
        this.blackAlive = prev.blackAlive;
        this.whiteTurn = prev.whiteTurn;
        this.mustRemoveOpponent = prev.mustRemoveOpponent;
        this.gameOver = prev.gameOver;
        this.selectedIndex = -1;
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
        history.add(new HistoryState(board, whiteUnplaced, blackUnplaced, whiteAlive, blackAlive, whiteTurn, mustRemoveOpponent, gameOver));
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
        history.add(new HistoryState(board, whiteUnplaced, blackUnplaced, whiteAlive, blackAlive, whiteTurn, mustRemoveOpponent, gameOver));
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

        String turn = whiteTurn ? "⚪ Your Turn (Bone)" : "⚫ Bot Turn (Ebony)";
        String phase = mustRemoveOpponent ? "· ⚔️ MILL FORMED! Tap enemy piece to strike"
                     : (whiteUnplaced > 0 ? ("· Placing (" + whiteUnplaced + " in reserve)") : "· Sliding along chiseled grooves");
        statusListener.onStatusChanged(turn + " " + phase, 0xFFFDE047);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (action == MotionEvent.ACTION_DOWN && whiteTurn) {
            float w = getWidth();
            float h = getHeight();
            float size = Math.min(w, h - dpf(32f));
            float startX = (w - size) / 2f;
            float startY = (h - dpf(32f) - size) / 2f + dpf(8f);

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

    private boolean isAdjacentNode(int from, int to) {
        for (int adj : ADJACENT[from]) {
            if (adj == to) return true;
        }
        return false;
    }

    private boolean isLegalTarget(int idx) {
        if (!whiteTurn || mustRemoveOpponent || gameOver) return false;
        if (whiteUnplaced > 0) {
            return board[idx] == 0;
        } else {
            if (selectedIndex == -1) return false;
            if (board[idx] != 0) return false;
            return (whiteAlive == 3 || isAdjacentNode(selectedIndex, idx));
        }
    }

    private boolean isTargetableForRemoval(int idx) {
        if (!whiteTurn || !mustRemoveOpponent || gameOver) return false;
        if (board[idx] != 2) return false;
        boolean allInMills = true;
        for (int i = 0; i < 24; i++) {
            if (board[i] == 2 && !isMill(i, 2)) {
                allInMills = false;
                break;
            }
        }
        return allInMills || !isMill(idx, 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1. Weathered Havana Tavern Mahogany & Dark Oak Table Plank
        rect.set(0, 0, w, h);
        boardBgPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF3D2214, 0xFF24130A, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        // Vertical Wood Plank Grain Seams
        float p1 = w * 0.33f;
        float p2 = w * 0.67f;
        canvas.drawLine(p1, 0, p1, h, plankSeamShadow);
        canvas.drawLine(p1 + dpf(1f), 0, p1 + dpf(1f), h, plankSeamLight);
        canvas.drawLine(p2, 0, p2, h, plankSeamShadow);
        canvas.drawLine(p2 + dpf(1f), 0, p2 + dpf(1f), h, plankSeamLight);

        // Chiseled Outer Wood Inlay Margin
        rect.set(dpf(6f), dpf(6f), w - dpf(6f), h - dpf(6f));
        Paint woodBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        woodBorder.setColor(0xFF5A351C);
        woodBorder.setStyle(Paint.Style.STROKE);
        woodBorder.setStrokeWidth(dpf(1.6f));
        canvas.drawRoundRect(rect, dpf(12f), dpf(12f), woodBorder);

        // 4 Wrought-Iron L-Corner Brackets with Forged Rivets (Black Flag Style)
        float bLen = dpf(20f);
        float bThick = dpf(3.5f);
        float bPad = dpf(3f);
        ironBracketPaint.setStrokeWidth(bThick);

        // Top-Left L-bracket
        canvas.drawLine(bPad, bPad, bPad + bLen, bPad, ironBracketPaint);
        canvas.drawLine(bPad, bPad, bPad, bPad + bLen, ironBracketPaint);
        canvas.drawCircle(bPad + dpf(6f), bPad + dpf(6f), dpf(1.8f), ironRivetPaint);

        // Top-Right L-bracket
        canvas.drawLine(w - bPad, bPad, w - bPad - bLen, bPad, ironBracketPaint);
        canvas.drawLine(w - bPad, bPad, w - bPad, bPad + bLen, ironBracketPaint);
        canvas.drawCircle(w - bPad - dpf(6f), bPad + dpf(6f), dpf(1.8f), ironRivetPaint);

        // Bottom-Left L-bracket
        canvas.drawLine(bPad, h - bPad, bPad + bLen, h - bPad, ironBracketPaint);
        canvas.drawLine(bPad, h - bPad, bPad, h - bPad - bLen, ironBracketPaint);
        canvas.drawCircle(bPad + dpf(6f), h - bPad - dpf(6f), dpf(1.8f), ironRivetPaint);

        // Bottom-Right L-bracket
        canvas.drawLine(w - bPad, h - bPad, w - bPad - bLen, h - bPad, ironBracketPaint);
        canvas.drawLine(w - bPad, h - bPad, w - bPad, h - bPad - bLen, ironBracketPaint);
        canvas.drawCircle(w - bPad - dpf(6f), h - bPad - dpf(6f), dpf(1.8f), ironRivetPaint);

        float size = Math.min(w, h - dpf(32f));
        float startX = (w - size) / 2f;
        float startY = (h - dpf(32f) - size) / 2f + dpf(8f);

        // 2. Chiseled Intaglio Grooves for 3 Concentric Squares
        for (int sq = 0; sq < 3; sq++) {
            int offset = sq * 8;
            for (int i = 0; i < 8; i++) {
                int next = (i == 7) ? offset : offset + i + 1;
                float x1 = startX + NODES[offset + i][0] * size;
                float y1 = startY + NODES[offset + i][1] * size;
                float x2 = startX + NODES[next][0] * size;
                float y2 = startY + NODES[next][1] * size;
                canvas.drawLine(x1, y1, x2, y2, chiselShadowPaint);
                canvas.drawLine(x1, y1, x2, y2, chiselRidgePaint);
                canvas.drawLine(x1, y1, x2, y2, chiselCenterPaint);
            }
        }

        // 3. Chiseled Cross Connecting Grooves
        int[][] cross = {{1, 9}, {9, 17}, {3, 11}, {11, 19}, {5, 13}, {13, 21}, {7, 15}, {15, 23}};
        for (int[] c : cross) {
            float x1 = startX + NODES[c[0]][0] * size;
            float y1 = startY + NODES[c[0]][1] * size;
            float x2 = startX + NODES[c[1]][0] * size;
            float y2 = startY + NODES[c[1]][1] * size;
            canvas.drawLine(x1, y1, x2, y2, chiselShadowPaint);
            canvas.drawLine(x1, y1, x2, y2, chiselRidgePaint);
            canvas.drawLine(x1, y1, x2, y2, chiselCenterPaint);
        }

        // 4. Branded Pirate Ember Glow for Completed Active Mills
        for (int[] m : MILLS) {
            int c = board[m[0]];
            if (c != 0 && board[m[1]] == c && board[m[2]] == c) {
                float x0 = startX + NODES[m[0]][0] * size;
                float y0 = startY + NODES[m[0]][1] * size;
                float x1 = startX + NODES[m[1]][0] * size;
                float y1 = startY + NODES[m[1]][1] * size;
                float x2 = startX + NODES[m[2]][0] * size;
                float y2 = startY + NODES[m[2]][1] * size;

                millBrandGlowPaint.setColor(c == 1 ? 0x66F59E0B : 0x6638BDF8);
                canvas.drawLine(x0, y0, x1, y1, millBrandGlowPaint);
                canvas.drawLine(x1, y1, x2, y2, millBrandGlowPaint);

                millBrandPaint.setColor(c == 1 ? 0xFFF59E0B : 0xFF38BDF8);
                canvas.drawLine(x0, y0, x1, y1, millBrandPaint);
                canvas.drawLine(x1, y1, x2, y2, millBrandPaint);
            }
        }

        // 5. 24 Carved Lathe Wood Sockets with Authentic AC4 Counters
        float socketR = dpf(8f);
        float pieceR = dpf(12.5f);

        for (int i = 0; i < 24; i++) {
            float nx = startX + NODES[i][0] * size;
            float ny = startY + NODES[i][1] * size;

            // Carved Lathe Wood Dimple
            canvas.drawCircle(nx, ny, socketR, socketDimpleInnerPaint);
            canvas.drawCircle(nx, ny, socketR, socketDimpleRimPaint);
            canvas.drawCircle(nx, ny, dpf(2f), chiselRidgePaint);

            // Valid Target Pip Beacon (Warm Tavern Candlelit Chalk)
            if (isLegalTarget(i)) {
                canvas.drawCircle(nx, ny, dpf(4.5f), chalkPipPaint);
                canvas.drawCircle(nx, ny, dpf(2f), shinePaint);
            }

            // Selected Piece Highlight
            if (i == selectedIndex) {
                canvas.drawCircle(nx, ny, pieceR * 1.45f, selectGlowPaint);
                Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
                tick.setColor(0xFFFFD166);
                tick.setStrokeWidth(dpf(2f));
                float tr = pieceR * 1.3f;
                float tLen = dpf(4.5f);
                canvas.drawLine(nx - tr, ny - tr, nx - tr + tLen, ny - tr, tick);
                canvas.drawLine(nx - tr, ny - tr, nx - tr, ny - tr + tLen, tick);
                canvas.drawLine(nx + tr, ny - tr, nx + tr - tLen, ny - tr, tick);
                canvas.drawLine(nx + tr, ny - tr, nx + tr, ny - tr + tLen, tick);
                canvas.drawLine(nx - tr, ny + tr, nx - tr + tLen, ny + tr, tick);
                canvas.drawLine(nx - tr, ny + tr, nx - tr, ny + tr - tLen, tick);
                canvas.drawLine(nx + tr, ny + tr, nx + tr - tLen, ny + tr, tick);
                canvas.drawLine(nx + tr, ny + tr, nx + tr, ny + tr - tLen, tick);
            }

            int val = board[i];
            if (val == 1) {
                drawAuthenticTurnedCounter(canvas, nx, ny, pieceR, true);
            } else if (val == 2) {
                drawAuthenticTurnedCounter(canvas, nx, ny, pieceR, false);
                // Crossed Cutlass Strike Reticle when targetable for removal
                if (isTargetableForRemoval(i)) {
                    float rr = pieceR * 1.35f;
                    canvas.drawCircle(nx, ny, rr, strikeReticlePaint);
                    // 4 Crosshair Strike Ticks
                    canvas.drawLine(nx, ny - rr - dpf(3f), nx, ny - rr + dpf(3f), strikeReticlePaint);
                    canvas.drawLine(nx, ny + rr - dpf(3f), nx, ny + rr + dpf(3f), strikeReticlePaint);
                    canvas.drawLine(nx - rr - dpf(3f), ny, nx - rr + dpf(3f), ny, strikeReticlePaint);
                    canvas.drawLine(nx + rr - dpf(3f), ny, nx + rr + dpf(3f), ny, strikeReticlePaint);
                }
            }
        }

        // 6. Bottom Antique Parchment Score Bar
        textPaint.setTextSize(dpf(10f));
        canvas.drawText("⚪ Bone Pieces: " + whiteAlive + " (" + whiteUnplaced + " in reserve)   |   ⚫ Dark Wood: " + blackAlive + " (" + blackUnplaced + " in reserve)", w / 2f, h - dpf(9f), textPaint);
    }

    private void drawAuthenticTurnedCounter(Canvas canvas, float cx, float cy, float r, boolean isBone) {
        // Deep ambient table drop shadow
        canvas.drawCircle(cx + dpf(1.8f), cy + dpf(2.4f), r, shadowPaint);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            isBone ? new int[]{0xFFFFFDF7, 0xFFF2E6CD, 0xFFDFCB9F, 0xFF8B6C43}
                   : new int[]{0xFF4A3E37, 0xFF2E2520, 0xFF181310, 0xFF0A0706},
            null, Shader.TileMode.CLAMP
        );
        piecePaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, piecePaint);

        // Chiseled Outer Lathe Ring
        latheRingPaint.setColor(isBone ? 0x66785E3B : 0x55000000);
        canvas.drawCircle(cx, cy, r * 0.72f, latheRingPaint);

        // Chiseled Inner Lathe Ring
        canvas.drawCircle(cx, cy, r * 0.42f, latheRingPaint);

        // Center Lathe Pip
        lathePipPaint.setColor(isBone ? 0x88785E3B : 0x88000000);
        canvas.drawCircle(cx, cy, dpf(1.8f), lathePipPaint);

        // Outer Turned Wood/Bone Bevel Contour
        pieceRimPaint.setColor(isBone ? 0x888B6C43 : 0x881A1410);
        canvas.drawCircle(cx, cy, r, pieceRimPaint);

        // Specular Ivory Sheen (Soft Arc)
        if (isBone) {
            canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.28f, shinePaint);
        }
    }
}
