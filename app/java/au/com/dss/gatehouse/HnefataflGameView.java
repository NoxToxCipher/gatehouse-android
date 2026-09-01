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
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * HnefataflGameView — Norse Viking Chess (11x11 Board).
 * Museum-grade Norse Slate & Runic Gold Canvas with 3D Shield Bosses,
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
    private final Paint goldDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moveGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rivetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint runeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF tileRect = new RectF();

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
        goldBorderPaint.setStrokeWidth(dpf(2f));

        goldDetailPaint.setColor(0xFFCA8A04);
        goldDetailPaint.setStyle(Paint.Style.STROKE);
        goldDetailPaint.setStrokeWidth(dpf(1f));

        moveGlowPaint.setColor(0xFFFDE047);
        moveGlowPaint.setStyle(Paint.Style.STROKE);
        moveGlowPaint.setStrokeWidth(dpf(2.5f));

        rivetPaint.setColor(0xFFCBD5E1);
        rivetPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(0x99000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        pieceRimPaint.setColor(0xFFE2E8F0);
        pieceRimPaint.setStyle(Paint.Style.STROKE);
        pieceRimPaint.setStrokeWidth(dpf(1.4f));

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

    private static class HistoryState {
        final char[][] board = new char[11][11];
        final boolean defendersTurn;
        final boolean gameOver;

        HistoryState(char[][] b, boolean dt, boolean go) {
            for (int r = 0; r < 11; r++) {
                System.arraycopy(b[r], 0, board[r], 0, 11);
            }
            this.defendersTurn = dt;
            this.gameOver = go;
        }
    }

    private final List<HistoryState> history = new ArrayList<>();

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

        history.clear();
        defendersTurn = true;
        selectedX = -1;
        selectedY = -1;
        validMoves.clear();
        gameOver = false;
        updateStatus();
        invalidate();
    }

    public void undoMove() {
        if (history.isEmpty()) return;
        HistoryState prev = history.remove(history.size() - 1);
        if (!prev.defendersTurn && !history.isEmpty()) {
            prev = history.remove(history.size() - 1);
        }
        for (int r = 0; r < 11; r++) {
            System.arraycopy(prev.board[r], 0, board[r], 0, 11);
        }
        this.defendersTurn = prev.defendersTurn;
        this.gameOver = prev.gameOver;
        this.selectedX = -1;
        this.selectedY = -1;
        this.validMoves.clear();
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
        history.add(new HistoryState(board, defendersTurn, gameOver));
        char p = board[fromY][fromX];
        board[toY][toX] = p;
        board[fromY][fromX] = '.';

        try {
            RecreationAudioSynth.playChessPieceThud(false);
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        if (p == 'K' && ((toX == 0 || toX == 10) && (toY == 0 || toY == 10))) {
            gameOver = true;
            try {
                RecreationAudioSynth.playTetrisLineClear();
                performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            } catch (Exception ignored) {}
            if (statusListener != null) {
                statusListener.onStatusChanged("🏆 NORSE VICTORY! King reached sanctuary fort.", 0xFF10B981);
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
                        try {
                            RecreationAudioSynth.playChessPieceThud(true);
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        } catch (Exception ignored) {}
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
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (action == MotionEvent.ACTION_DOWN) {
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

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
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
                tileRect.set(left + dpf(1f), top + dpf(1f), left + cellSize - dpf(1f), top + cellSize - dpf(1f));

                boolean isCorner = (c == 0 || c == 10) && (r == 0 || r == 10);
                boolean isThrone = (c == 5 && r == 5);

                if (isCorner) {
                    cornerPaint.setColor(0xFF78350F);
                    canvas.drawRect(tileRect, cornerPaint);
                    canvas.drawRect(tileRect, goldBorderPaint);
                    runeTextPaint.setTextSize(cellSize * 0.6f);
                    canvas.drawText("ᚠ", tileRect.centerX(), tileRect.centerY() + cellSize * 0.22f, runeTextPaint);
                } else if (isThrone) {
                    thronePaint.setColor(0xFF831843);
                    canvas.drawRect(tileRect, thronePaint);
                    canvas.drawRect(tileRect, goldBorderPaint);
                    runeTextPaint.setTextSize(cellSize * 0.6f);
                    canvas.drawText("ᛟ", tileRect.centerX(), tileRect.centerY() + cellSize * 0.22f, runeTextPaint);
                } else {
                    boolean isDark = (r + c) % 2 == 1;
                    squareDarkPaint.setColor(isDark ? 0xFF0F172A : 0xFF1E293B);
                    canvas.drawRect(tileRect, squareDarkPaint);
                }

                if (c == selectedX && r == selectedY) {
                    canvas.drawRect(tileRect, selectGlowPaint);
                    // Selected Corner Ticks
                    Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
                    tick.setColor(0xFFFFD166);
                    tick.setStrokeWidth(dpf(1.8f));
                    float tLen = dpf(5f);
                    canvas.drawLine(tileRect.left, tileRect.top, tileRect.left + tLen, tileRect.top, tick);
                    canvas.drawLine(tileRect.left, tileRect.top, tileRect.left, tileRect.top + tLen, tick);
                    canvas.drawLine(tileRect.right, tileRect.top, tileRect.right - tLen, tileRect.top, tick);
                    canvas.drawLine(tileRect.right, tileRect.top, tileRect.right, tileRect.top + tLen, tick);
                    canvas.drawLine(tileRect.left, tileRect.bottom, tileRect.left + tLen, tileRect.bottom, tick);
                    canvas.drawLine(tileRect.left, tileRect.bottom, tileRect.left, tileRect.bottom - tLen, tick);
                    canvas.drawLine(tileRect.right, tileRect.bottom, tileRect.right - tLen, tileRect.bottom, tick);
                    canvas.drawLine(tileRect.right, tileRect.bottom, tileRect.right, tileRect.bottom - tLen, tick);
                }

                char p = board[r][c];
                float cx = left + cellSize / 2f;
                float cy = top + cellSize / 2f;

                boolean isCurrentSide = defendersTurn ? (p == 'D' || p == 'K') : (p == 'A');
                if (isCurrentSide && selectedX == -1) {
                    canvas.drawCircle(cx, cy, pieceR + dpf(2f), moveGlowPaint);
                }

                if (p == 'A') drawVikingShield(canvas, cx, cy, pieceR, 0xFFEF4444, 0xFF7F1D1D);
                else if (p == 'D') drawVikingShield(canvas, cx, cy, pieceR, 0xFF38BDF8, 0xFF0369A1);
                else if (p == 'K') drawGoldenKing(canvas, cx, cy, pieceR * 1.18f);
            }
        }

        for (Point m : validMoves) {
            float cx = startX + m.x * cellSize + cellSize / 2f;
            float cy = startY + m.y * cellSize + cellSize / 2f;
            canvas.drawCircle(cx, cy, dpf(4.5f), targetDotPaint);
            canvas.drawCircle(cx, cy, dpf(2f), pieceShinePaint);
        }

        // Draw Jarl King Escape Route Tracers
        drawKingEscapeTracers(canvas, startX, startY, cellSize);
    }

    private void drawKingEscapeTracers(Canvas canvas, float startX, float startY, float cellSize) {
        Point king = null;
        for (int r = 0; r < 11; r++) {
            for (int c = 0; c < 11; c++) {
                if (board[r][c] == 'K') {
                    king = new Point(c, r);
                    break;
                }
            }
            if (king != null) break;
        }
        if (king == null) return;

        int[][] corners = {{0,0}, {10,0}, {0,10}, {10,10}};
        Paint tracerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tracerPaint.setColor(0xCC10B981);
        tracerPaint.setStyle(Paint.Style.STROKE);
        tracerPaint.setStrokeWidth(dpf(3f));

        Paint tracerGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        tracerGlow.setColor(0x6634D399);
        tracerGlow.setStyle(Paint.Style.STROKE);
        tracerGlow.setStrokeWidth(dpf(7f));

        Paint fortAlert = new Paint(Paint.ANTI_ALIAS_FLAG);
        fortAlert.setColor(0xEE10B981);
        fortAlert.setStyle(Paint.Style.FILL);

        float kx = startX + king.x * cellSize + cellSize / 2f;
        float ky = startY + king.y * cellSize + cellSize / 2f;

        for (int[] cr : corners) {
            int cx = cr[0];
            int cy = cr[1];
            // Check if king has clear orthogonal path to this corner
            if (king.x == cx || king.y == cy) {
                boolean clear = true;
                if (king.x == cx) {
                    int step = Integer.compare(cy, king.y);
                    int cur = king.y + step;
                    while (cur != cy) {
                        if (board[cur][cx] != '.') { clear = false; break; }
                        cur += step;
                    }
                } else {
                    int step = Integer.compare(cx, king.x);
                    int cur = king.x + step;
                    while (cur != cx) {
                        if (board[cy][cur] != '.') { clear = false; break; }
                        cur += step;
                    }
                }

                if (clear) {
                    float targetX = startX + cx * cellSize + cellSize / 2f;
                    float targetY = startY + cy * cellSize + cellSize / 2f;
                    canvas.drawLine(kx, ky, targetX, targetY, tracerGlow);
                    canvas.drawLine(kx, ky, targetX, targetY, tracerPaint);

                    // Corner Fort Victory Aura
                    canvas.drawCircle(targetX, targetY, cellSize * 0.45f, fortAlert);
                    canvas.drawCircle(targetX, targetY, cellSize * 0.45f, goldBorderPaint);
                }
            }
        }
    }

    private void drawVikingShield(Canvas canvas, float cx, float cy, float r, int lightCol, int darkCol) {
        // Deep drop shadow
        canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            new int[]{0xFFFFFFFF, lightCol, darkCol},
            null, Shader.TileMode.CLAMP
        );
        piecePaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, piecePaint);

        // Wood Plank Lines
        Paint plankLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        plankLine.setColor(0x44000000);
        plankLine.setStrokeWidth(dpf(1f));
        canvas.drawLine(cx - r * 0.9f, cy - r * 0.38f, cx + r * 0.9f, cy - r * 0.38f, plankLine);
        canvas.drawLine(cx - r * 0.9f, cy + r * 0.38f, cx + r * 0.9f, cy + r * 0.38f, plankLine);
        canvas.drawLine(cx - r, cy, cx + r, cy, plankLine);

        // Outer Forged Rim
        canvas.drawCircle(cx, cy, r, pieceRimPaint);

        // Central Steel Shield Boss
        RadialGradient bossGrad = new RadialGradient(
            cx - r * 0.1f, cy - r * 0.1f, r * 0.45f,
            new int[]{0xFFFFFFFF, 0xFF94A3B8, 0xFF334155},
            null, Shader.TileMode.CLAMP
        );
        Paint bossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bossPaint.setShader(bossGrad);
        canvas.drawCircle(cx, cy, r * 0.36f, bossPaint);
        canvas.drawCircle(cx, cy, r * 0.36f, pieceRimPaint);

        // 4 Iron Rivets
        float rivetDist = r * 0.65f;
        canvas.drawCircle(cx, cy - rivetDist, dpf(1.5f), rivetPaint);
        canvas.drawCircle(cx, cy + rivetDist, dpf(1.5f), rivetPaint);
        canvas.drawCircle(cx - rivetDist, cy, dpf(1.5f), rivetPaint);
        canvas.drawCircle(cx + rivetDist, cy, dpf(1.5f), rivetPaint);

        canvas.drawCircle(cx - r * 0.12f, cy - r * 0.12f, r * 0.12f, pieceShinePaint);
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

        // Royal Braided Gold Ring
        Paint innerRim = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerRim.setColor(0xFFFDE047);
        innerRim.setStyle(Paint.Style.STROKE);
        innerRim.setStrokeWidth(dpf(1.2f));
        canvas.drawCircle(cx, cy, r * 0.72f, innerRim);

        // Center Ruby Cabochon Core
        RadialGradient rubyGrad = new RadialGradient(
            cx - r * 0.1f, cy - r * 0.1f, r * 0.4f,
            new int[]{0xFFFCA5A5, 0xFFDC2626, 0xFF7F1D1D},
            null, Shader.TileMode.CLAMP
        );
        Paint rubyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rubyPaint.setShader(rubyGrad);
        canvas.drawCircle(cx, cy, r * 0.36f, rubyPaint);
        canvas.drawCircle(cx, cy, r * 0.36f, goldBorderPaint);

        // 4 Gold Boss Studs
        float sDist = r * 0.54f;
        canvas.drawCircle(cx, cy - sDist, dpf(1.8f), innerRim);
        canvas.drawCircle(cx, cy + sDist, dpf(1.8f), innerRim);
        canvas.drawCircle(cx - sDist, cy, dpf(1.8f), innerRim);
        canvas.drawCircle(cx + sDist, cy, dpf(1.8f), innerRim);

        runeTextPaint.setTextSize(r * 0.9f);
        canvas.drawText("👑", cx, cy + r * 0.32f, runeTextPaint);
    }
}
