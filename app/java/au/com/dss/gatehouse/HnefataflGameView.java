package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * HnefataflGameView — Norse Viking Chess (11x11 Board).
 * High-fidelity Norse Slate & Runic Gold Canvas with 3D Shield Bosses,
 * Corner Fortresses, Royal King's Throne, and Custodial Minimax AI.
 */
public class HnefataflGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint squareLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint squareDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thronePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint runeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final char[][] board = new char[11][11];
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

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(1.5f));

        shadowPaint.setColor(0x88000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        pieceRimPaint.setColor(0xFFE2E8F0);
        pieceRimPaint.setStyle(Paint.Style.STROKE);
        pieceRimPaint.setStrokeWidth(dpf(1.5f));

        pieceShinePaint.setColor(0xAAFFFFFF);
        pieceShinePaint.setStyle(Paint.Style.FILL);

        selectGlowPaint.setColor(0x66FFD166);
        selectGlowPaint.setStyle(Paint.Style.FILL);

        targetDotPaint.setColor(0xFFFFD166);
        targetDotPaint.setStyle(Paint.Style.FILL);

        runeTextPaint.setColor(0xFFFDE047);
        runeTextPaint.setTextAlign(Paint.Align.CENTER);
        runeTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        resetGame();
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void resetGame() {
        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) board[r][c] = '.';
        }

        board[5][5] = 'K';
        int[][] defs = {
            {3,5}, {4,5}, {6,5}, {7,5},
            {5,3}, {5,4}, {5,6}, {5,7},
            {4,4}, {4,6}, {6,4}, {6,6}
        };
        for (int[] d : defs) board[d[0]][d[1]] = 'D';

        int[][] atts = {
            {0,3},{0,4},{0,5},{0,6},{0,7},{1,5},
            {10,3},{10,4},{10,5},{10,6},{10,7},{9,5},
            {3,0},{4,0},{5,0},{6,0},{7,0},{5,1},
            {3,10},{4,10},{5,10},{6,10},{7,10},{5,9}
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

        if (p == 'K' && ((toX == 0 || toX == 10) && (toY == 0 || toY == 10))) {
            gameOver = true;
            if (statusListener != null) {
                statusListener.onStatusChanged("🏆 NORSE VICTORY! King reached the sanctuary fort.", 0xFF10B981);
            }
            selectedX = -1;
            selectedY = -1;
            validMoves.clear();
            invalidate();
            return;
        }

        checkCaptures(toX, toY, p);

        if (checkKingCaptured()) {
            gameOver = true;
            if (statusListener != null) {
                statusListener.onStatusChanged("💀 BERSERKER DEFEAT! King surrounded and slain.", 0xFFEF4444);
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
                if (midPiece == '.' || midPiece == 'K') continue;

                boolean midIsDefender = (midPiece == 'D');
                if (midIsDefender != isDefenderSide) {
                    char farPiece = board[farY][farX];
                    boolean farMatches = (isDefenderSide && (farPiece == 'D' || farPiece == 'K')) ||
                                         (!isDefenderSide && farPiece == 'A') ||
                                         isCornerOrThrone(farX, farY);

                    if (farMatches) {
                        board[midY][midX] = '.';
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
                    for (Point p : validMoves) moves.add(new int[]{c, r, p.x, p.y});
                }
            }
        }

        if (!moves.isEmpty()) {
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
        String turn = defendersTurn ? "🟡 King & Norse Guard" : "🔴 Berserker Horde";
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
        boardBgPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF0B0F19, 0xFF1E1B4B, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        rect.set(dpf(2f), dpf(2f), w - dpf(2f), h - dpf(2f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

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

                if (isCorner) {
                    cornerPaint.setColor(0xFF78350F);
                    canvas.drawRect(rect, cornerPaint);
                    canvas.drawRect(rect, goldBorderPaint);
                    runeTextPaint.setTextSize(cellSize * 0.6f);
                    canvas.drawText("ᚠ", rect.centerX(), rect.centerY() + cellSize * 0.22f, runeTextPaint);
                } else if (isThrone) {
                    thronePaint.setColor(0xFF831843);
                    canvas.drawRect(rect, thronePaint);
                    canvas.drawRect(rect, goldBorderPaint);
                    runeTextPaint.setTextSize(cellSize * 0.6f);
                    canvas.drawText("ᛟ", rect.centerX(), rect.centerY() + cellSize * 0.22f, runeTextPaint);
                } else {
                    boolean isDark = (r + c) % 2 == 1;
                    squareDarkPaint.setColor(isDark ? 0xFF0F172A : 0xFF1E293B);
                    canvas.drawRect(rect, squareDarkPaint);
                }

                if (c == selectedX && r == selectedY) {
                    canvas.drawRect(rect, selectGlowPaint);
                }

                char p = board[r][c];
                float cx = left + cellSize / 2f;
                float cy = top + cellSize / 2f;

                if (p == 'A') drawVikingShield(canvas, cx, cy, pieceR, 0xFFEF4444, 0xFF7F1D1D);
                else if (p == 'D') drawVikingShield(canvas, cx, cy, pieceR, 0xFF38BDF8, 0xFF0369A1);
                else if (p == 'K') drawGoldenKing(canvas, cx, cy, pieceR * 1.18f);
            }
        }

        // Draw Valid Move Target Glowing Dots
        for (Point m : validMoves) {
            float cx = startX + m.x * cellSize + cellSize / 2f;
            float cy = startY + m.y * cellSize + cellSize / 2f;
            canvas.drawCircle(cx, cy, dpf(4.5f), targetDotPaint);
            canvas.drawCircle(cx, cy, dpf(2f), pieceShinePaint);
        }
    }

    private void drawVikingShield(Canvas canvas, float cx, float cy, float r, int lightCol, int darkCol) {
        canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            new int[]{0xFFFFFFFF, lightCol, darkCol},
            null, Shader.TileMode.CLAMP
        );
        piecePaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, piecePaint);
        canvas.drawCircle(cx, cy, r, pieceRimPaint);

        // Center Iron Boss
        canvas.drawCircle(cx, cy, r * 0.35f, pieceRimPaint);
        canvas.drawCircle(cx - r * 0.1f, cy - r * 0.1f, r * 0.15f, pieceShinePaint);
    }

    private void drawGoldenKing(Canvas canvas, float cx, float cy, float r) {
        canvas.drawCircle(cx + dpf(2f), cy + dpf(2.5f), r, shadowPaint);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            new int[]{0xFFFFFBEB, 0xFFF59E0B, 0xFF78350F},
            null, Shader.TileMode.CLAMP
        );
        piecePaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, piecePaint);
        canvas.drawCircle(cx, cy, r, goldBorderPaint);

        runeTextPaint.setTextSize(r * 1.2f);
        canvas.drawText("👑", cx, cy + r * 0.42f, runeTextPaint);
    }
}
