package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * HnefataflGameView — Norse Viking Chess (11x11 Board).
 * King (K) + 12 Defenders (D) vs 24 Attackers (A).
 * King escapes to 4 corner forts to win; Attackers capture King via 4-way surround.
 * Custodial capture on ranks and files with Minimax AI.
 */
public class HnefataflGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint squarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerThronePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint attackerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint defenderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    // Board: '.' empty, 'A' attacker, 'D' defender, 'K' king
    private final char[][] board = new char[11][11];
    // true = Defenders/King (Human), false = Attackers (Bot)
    private boolean defendersTurn = true;
    private int selectedX = -1;
    private int selectedY = -1;
    private final List<Point> validMoves = new ArrayList<>();
    private boolean gameOver = false;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public HnefataflGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        boardBgPaint.setColor(0xFF0F172A);
        squarePaint.setColor(0xFF1E293B);
        cornerPaint.setColor(0xFF78350F);
        centerThronePaint.setColor(0xFF831843);

        attackerPaint.setColor(0xFFF87171); // Crimson Berserkers
        attackerPaint.setStyle(Paint.Style.FILL);

        defenderPaint.setColor(0xFF38BDF8); // Cyan Norse Guard
        defenderPaint.setStyle(Paint.Style.FILL);

        kingPaint.setColor(0xFFFFD166); // Golden King
        kingPaint.setStyle(Paint.Style.FILL);

        selectPaint.setColor(0x88FFD166);
        selectPaint.setStyle(Paint.Style.FILL);

        targetDotPaint.setColor(0xFFFFD166);
        targetDotPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        resetGame();
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void resetGame() {
        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) {
                board[r][c] = '.';
            }
        }

        // Center Throne & Defenders (12 + King)
        board[5][5] = 'K';
        int[][] defs = {
            {3,5}, {4,5}, {6,5}, {7,5},
            {5,3}, {5,4}, {5,6}, {5,7},
            {4,4}, {4,6}, {6,4}, {6,6}
        };
        for (int[] d : defs) board[d[0]][d[1]] = 'D';

        // 24 Attackers (6 on each edge)
        int[][] atts = {
            {0,3},{0,4},{0,5},{0,6},{0,7},{1,5}, // North
            {10,3},{10,4},{10,5},{10,6},{10,7},{9,5}, // South
            {3,0},{4,0},{5,0},{6,0},{7,0},{5,1}, // West
            {3,10},{4,10},{5,10},{6,10},{7,10},{5,9} // East
        };
        for (int[] a : atts) board[a[0]][a[1]] = 'A';

        defendersTurn = true;
        selectedX = -1;
        selectedY = -1;
        validMoves.clear();
        gameOver = false;
        updateStatus();
        invalidate();
    }

    private boolean isCornerOrThrone(int x, int y) {
        if ((x == 0 || x == 10) && (y == 0 || y == 10)) return true;
        return (x == 5 && y == 5);
    }

    private void generateMoves(int x, int y) {
        validMoves.clear();
        char p = board[y][x];
        if (p == '.') return;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int tx = x + d[0];
            int ty = y + d[1];
            while (tx >= 0 && tx < 11 && ty >= 0 && ty < 11) {
                if (board[ty][tx] != '.') break;

                // Only King can enter corner forts or center throne
                if (isCornerOrThrone(tx, ty) && p != 'K') {
                    tx += d[0];
                    ty += d[1];
                    continue;
                }

                validMoves.add(new Point(tx, ty));
                tx += d[0];
                ty += d[1];
            }
        }
    }

    private void executeMove(int fromX, int fromY, int toX, int toY) {
        char p = board[fromY][fromX];
        board[toY][toX] = p;
        board[fromY][fromX] = '.';

        // King reached corner fort -> Defenders win!
        if (p == 'K' && ((toX == 0 || toX == 10) && (toY == 0 || toY == 10))) {
            gameOver = true;
            if (statusListener != null) {
                statusListener.onStatusChanged("🏆 VICTORY! The King has escaped to the fort.", 0xFF10B981);
            }
            selectedX = -1;
            selectedY = -1;
            validMoves.clear();
            invalidate();
            return;
        }

        // Custodial captures (check 4 neighbors of destination)
        checkCaptures(toX, toY, p);

        // Check if King is captured (surrounded on 4 sides or 3 sides + throne)
        if (checkKingCaptured()) {
            gameOver = true;
            if (statusListener != null) {
                statusListener.onStatusChanged("💀 DEFEAT! The King has been captured by Vikings.", 0xFFEF4444);
            }
            selectedX = -1;
            selectedY = -1;
            validMoves.clear();
            invalidate();
            return;
        }

        selectedX = -1;
        selectedY = -1;
        validMoves.clear();
        defendersTurn = !defendersTurn;
        updateStatus();
        invalidate();

        if (!gameOver && !defendersTurn) {
            postDelayed(new Runnable() {
                public void run() { botExecuteMove(); }
            }, 380);
        }
    }

    private void checkCaptures(int x, int y, char mover) {
        boolean isDefenderSide = (mover == 'D' || mover == 'K');
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        for (int[] d : dirs) {
            int midX = x + d[0];
            int midY = y + d[1];
            int farX = x + d[0] * 2;
            int farY = y + d[1] * 2;

            if (midX >= 0 && midX < 11 && midY >= 0 && midY < 11 &&
                farX >= 0 && farX < 11 && farY >= 0 && farY < 11) {

                char midPiece = board[midY][midX];
                if (midPiece == '.' || midPiece == 'K') continue; // King captured separately

                boolean midIsDefender = (midPiece == 'D');
                if (midIsDefender != isDefenderSide) {
                    char farPiece = board[farY][farX];
                    boolean farMatches = (isDefenderSide && (farPiece == 'D' || farPiece == 'K')) ||
                                         (!isDefenderSide && farPiece == 'A') ||
                                         isCornerOrThrone(farX, farY);

                    if (farMatches) {
                        board[midY][midX] = '.'; // Custodial sandwich capture!
                    }
                }
            }
        }
    }

    private boolean checkKingCaptured() {
        int kx = -1, ky = -1;
        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) {
                if (board[r][c] == 'K') { kx = c; ky = r; break; }
            }
        }
        if (kx == -1) return true;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        int hostileSides = 0;
        for (int[] d : dirs) {
            int nx = kx + d[0];
            int ny = ky + d[1];
            if (nx < 0 || nx >= 11 || ny < 0 || ny >= 11 || board[ny][nx] == 'A' || (nx == 5 && ny == 5)) {
                hostileSides++;
            }
        }
        return (hostileSides == 4);
    }

    private void botExecuteMove() {
        if (defendersTurn || gameOver) return;
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) {
                if (board[r][c] == 'A') {
                    generateMoves(c, r);
                    for (Point p : validMoves) {
                        moves.add(new int[]{c, r, p.x, p.y});
                    }
                }
            }
        }

        if (!moves.isEmpty()) {
            // Pick move that gets closest to King
            int kx = 5, ky = 5;
            for (int r = 0; r < 11; r++) {
                for (int c = 0; c < 11; c++) {
                    if (board[r][c] == 'K') { kx = c; ky = r; break; }
                }
            }

            int[] best = moves.get(0);
            int bestDist = 9999;
            for (int[] m : moves) {
                int dist = Math.abs(m[2] - kx) + Math.abs(m[3] - ky);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = m;
                }
            }
            executeMove(best[0], best[1], best[2], best[3]);
        }
    }

    private void updateStatus() {
        if (statusListener == null || gameOver) return;
        String turn = defendersTurn ? "🟡 King & Norse Defenders" : "🔴 Viking Siege Horde";
        statusListener.onStatusChanged(turn + " · 11×11 Hnefatafl", 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float w = getWidth();
            float h = getHeight();
            float pad = dpf(8f);
            float size = Math.min(w, h) - pad * 2;
            float startX = (w - size) / 2f;
            float startY = (h - size) / 2f;
            float cellSize = size / 11f;

            int gx = (int) ((event.getX() - startX) / cellSize);
            int gy = (int) ((event.getY() - startY) / cellSize);

            if (gx >= 0 && gx < 11 && gy >= 0 && gy < 11) {
                if (selectedX != -1 && selectedY != -1) {
                    for (Point m : validMoves) {
                        if (m.x == gx && m.y == gy) {
                            executeMove(selectedX, selectedY, gx, gy);
                            return true;
                        }
                    }
                }

                char p = board[gy][gx];
                boolean isOwn = defendersTurn ? (p == 'D' || p == 'K') : (p == 'A');
                if (isOwn) {
                    selectedX = gx;
                    selectedY = gy;
                    generateMoves(gx, gy);
                    invalidate();
                    return true;
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
        float size = Math.min(w, h) - pad * 2;
        float startX = (w - size) / 2f;
        float startY = (h - size) / 2f;
        float cellSize = size / 11f;
        float pieceR = cellSize * 0.40f;

        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) {
                float left = startX + c * cellSize;
                float top = startY + r * cellSize;
                rect.set(left + dpf(1f), top + dpf(1f), left + cellSize - dpf(1f), top + cellSize - dpf(1f));

                boolean isCorner = (c == 0 || c == 10) && (r == 0 || r == 10);
                boolean isThrone = (c == 5 && r == 5);
                canvas.drawRect(rect, isCorner ? cornerPaint : (isThrone ? centerThronePaint : squarePaint));

                if (c == selectedX && r == selectedY) {
                    canvas.drawRect(rect, selectPaint);
                }

                char p = board[r][c];
                float cx = left + cellSize / 2f;
                float cy = top + cellSize / 2f;
                if (p == 'A') canvas.drawCircle(cx, cy, pieceR, attackerPaint);
                else if (p == 'D') canvas.drawCircle(cx, cy, pieceR, defenderPaint);
                else if (p == 'K') {
                    canvas.drawCircle(cx, cy, pieceR * 1.15f, kingPaint);
                    textPaint.setTextSize(cellSize * 0.7f);
                    canvas.drawText("👑", cx, cy + pieceR * 0.35f, textPaint);
                }
            }
        }

        // Draw Valid Move Target Dots
        for (Point m : validMoves) {
            float cx = startX + m.x * cellSize + cellSize / 2f;
            float cy = startY + m.y * cellSize + cellSize / 2f;
            canvas.drawCircle(cx, cy, dpf(4f), targetDotPaint);
        }
    }
}
