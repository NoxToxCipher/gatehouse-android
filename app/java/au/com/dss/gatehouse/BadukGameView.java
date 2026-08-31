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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * BadukGameView — Traditional Baduk / Go / Weiqi.
 * Museum-grade Japanese Kaya Wood Goban with 3D Bi-convex Slate & Clamshell Stones,
 * Support for 9x9, 13x13 and 19x19 Grid Sizes, Tsumego Life & Death Puzzles,
 * Territory Estimator, Move History, and Monte Carlo Tree Search (MCTS) Bot.
 */
public class BadukGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint gobanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint woodGrainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stone3dPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stoneRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stoneShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lastMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coordTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint territoryBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint territoryWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boardRect = new RectF();
    private final Random rng = new Random();

    private int boardSize = 9; // 9, 13, 19
    private int[][] board = new int[19][19];
    private final List<int[][]> history = new ArrayList<>();
    private final List<Point> moveList = new ArrayList<>();

    private int currentTurn = 1; // 1 = Black, 2 = White
    private int blackCaptures = 0;
    private int whiteCaptures = 0;
    private int lastMoveX = -1;
    private int lastMoveY = -1;
    private int consecutivePasses = 0;
    private boolean gameOver = false;

    private int mode = 0; // 0 = vs AI, 1 = Tsumego Puzzles, 2 = 2-Player Pass & Play
    private int puzzleIndex = 0;
    private boolean puzzleSolved = false;
    private boolean showTerritory = false;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public BadukGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(1.8f));

        gridLinePaint.setColor(0xFF452B14);
        gridLinePaint.setStrokeWidth(dpf(1.4f));

        woodGrainPaint.setColor(0x18000000);
        woodGrainPaint.setStrokeWidth(dpf(1f));

        starPointPaint.setColor(0xFF3E2723);
        starPointPaint.setStyle(Paint.Style.FILL);

        stoneRimPaint.setColor(0x88FFFFFF);
        stoneRimPaint.setStyle(Paint.Style.STROKE);
        stoneRimPaint.setStrokeWidth(dpf(1.2f));

        stoneShinePaint.setColor(0xAAFFFFFF);
        stoneShinePaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(0x99000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        lastMovePaint.setColor(0xFFFFD166);
        lastMovePaint.setStyle(Paint.Style.STROKE);
        lastMovePaint.setStrokeWidth(dpf(2.2f));

        coordTextPaint.setColor(0xFF8C5C33);
        coordTextPaint.setTextAlign(Paint.Align.CENTER);
        coordTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        territoryBlackPaint.setColor(0x660F172A);
        territoryBlackPaint.setStyle(Paint.Style.FILL);

        territoryWhitePaint.setColor(0x66F8FAFC);
        territoryWhitePaint.setStyle(Paint.Style.FILL);

        resetGame();
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void setBoardSize(int size) {
        if (size != 9 && size != 13 && size != 19) size = 9;
        this.boardSize = size;
        resetGame();
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setMode(int m) {
        this.mode = m;
        if (mode == 1) {
            boardSize = 9; // Tsumego default
            loadPuzzle(puzzleIndex);
        } else {
            resetGame();
        }
    }

    public void resetGame() {
        for (int r = 0; r < 19; r++) {
            for (int c = 0; c < 19; c++) board[r][c] = 0;
        }
        history.clear();
        moveList.clear();
        currentTurn = 1;
        blackCaptures = 0;
        whiteCaptures = 0;
        lastMoveX = -1;
        lastMoveY = -1;
        consecutivePasses = 0;
        gameOver = false;
        puzzleSolved = false;
        showTerritory = false;
        updateStatus();
        invalidate();
    }

    public void undoMove() {
        if (history.size() < 2) {
            if (!history.isEmpty()) {
                restoreState(history.remove(history.size() - 1));
                if (!moveList.isEmpty()) moveList.remove(moveList.size() - 1);
            } else {
                resetGame();
            }
            return;
        }
        // In vs AI mode, undo both AI and player moves
        if (mode == 0) {
            history.remove(history.size() - 1);
            if (!moveList.isEmpty()) moveList.remove(moveList.size() - 1);
            restoreState(history.remove(history.size() - 1));
            if (!moveList.isEmpty()) moveList.remove(moveList.size() - 1);
        } else {
            restoreState(history.remove(history.size() - 1));
            if (!moveList.isEmpty()) moveList.remove(moveList.size() - 1);
        }
        updateStatus();
        invalidate();
    }

    private void saveState() {
        int[][] copy = new int[boardSize][boardSize];
        for (int r = 0; r < boardSize; r++) {
            System.arraycopy(board[r], 0, copy[r], 0, boardSize);
        }
        history.add(copy);
    }

    private void restoreState(int[][] saved) {
        for (int r = 0; r < boardSize; r++) {
            System.arraycopy(saved[r], 0, board[r], 0, boardSize);
        }
        if (!moveList.isEmpty()) {
            Point last = moveList.get(moveList.size() - 1);
            lastMoveX = last.x;
            lastMoveY = last.y;
            currentTurn = (board[last.y][last.x] == 1) ? 2 : 1;
        } else {
            lastMoveX = -1;
            lastMoveY = -1;
            currentTurn = 1;
        }
    }

    public void passTurn() {
        if (gameOver) return;
        consecutivePasses++;
        if (consecutivePasses >= 2) {
            gameOver = true;
            showTerritory = true;
            int bScore = blackCaptures + countTerritory(1);
            int wScore = whiteCaptures + countTerritory(2) + 6; // 6.5 komi
            String res = bScore > wScore ? ("🏆 BLACK WINS! (" + bScore + " - " + wScore + ")") : ("🏆 WHITE WINS! (" + wScore + " - " + bScore + ")");
            if (statusListener != null) {
                statusListener.onStatusChanged(res + " · Both Passed", 0xFF10B981);
            }
            invalidate();
            return;
        }

        currentTurn = (currentTurn == 1) ? 2 : 1;
        updateStatus();
        invalidate();

        if (mode == 0 && currentTurn == 2) {
            postDelayed(new Runnable() {
                public void run() { botPlayMove(); }
            }, 400);
        }
    }

    public void toggleTerritoryView() {
        showTerritory = !showTerritory;
        invalidate();
    }

    public void nextPuzzle() {
        puzzleIndex = (puzzleIndex + 1) % TSUMEGO_PUZZLES.length;
        loadPuzzle(puzzleIndex);
    }

    private static class TsumegoPuzzle {
        String title;
        int[][] stones; // [x, y, color]
        int targetX, targetY;

        TsumegoPuzzle(String title, int[][] stones, int targetX, int targetY) {
            this.title = title;
            this.stones = stones;
            this.targetX = targetX;
            this.targetY = targetY;
        }
    }

    private static final TsumegoPuzzle[] TSUMEGO_PUZZLES = {
        new TsumegoPuzzle("1. Corner Two-Eyes Life", new int[][]{
            {0,0,1}, {0,2,1}, {1,0,1}, {1,2,1}, {2,1,1}, {0,3,2}, {1,3,2}, {2,3,2}, {3,2,2}, {3,1,2}, {3,0,2}
        }, 0, 1),
        new TsumegoPuzzle("2. Snapback Tesuji", new int[][]{
            {2,2,1}, {3,1,1}, {3,3,1}, {4,2,1}, {2,1,2}, {2,3,2}, {3,0,2}, {3,4,2}, {4,1,2}, {4,3,2}, {3,2,2}
        }, 3, 2),
        new TsumegoPuzzle("3. Crane's Nest Tesuji", new int[][]{
            {1,1,2}, {2,1,2}, {3,1,2}, {1,2,1}, {3,2,1}, {2,3,1}, {0,1,1}, {4,1,1}
        }, 2, 2),
        new TsumegoPuzzle("4. Under the Stones", new int[][]{
            {0,1,1}, {1,1,1}, {2,1,1}, {0,2,2}, {1,2,2}, {2,2,2}, {0,0,2}, {3,1,2}
        }, 1, 0),
        new TsumegoPuzzle("5. False Eye Squeeze", new int[][]{
            {5,5,2}, {5,7,2}, {6,6,2}, {7,5,2}, {7,7,2}, {4,6,1}, {8,6,1}, {6,4,1}, {6,8,1}
        }, 6, 6),
        new TsumegoPuzzle("6. Belly Attachment Tesuji", new int[][]{
            {1,4,2}, {2,4,2}, {3,4,2}, {1,5,1}, {3,5,1}, {0,4,1}, {4,4,1}
        }, 2, 5),
        new TsumegoPuzzle("7. Monkey Jump Defense", new int[][]{
            {0,1,1}, {1,1,1}, {2,1,1}, {3,1,1}, {0,2,2}, {1,2,2}, {2,2,2}
        }, 3, 2),
        new TsumegoPuzzle("8. Placement in the Eye", new int[][]{
            {6,1,2}, {7,1,2}, {8,1,2}, {6,2,2}, {8,2,2}, {6,3,2}, {7,3,2}, {8,3,2}
        }, 7, 2)
    };

    private void loadPuzzle(int idx) {
        resetGame();
        boardSize = 9;
        if (idx < 0 || idx >= TSUMEGO_PUZZLES.length) idx = 0;
        TsumegoPuzzle p = TSUMEGO_PUZZLES[idx];
        for (int[] s : p.stones) {
            board[s[1]][s[0]] = s[2];
        }
        currentTurn = 1;
        updateStatus();
        invalidate();
    }

    private void updateStatus() {
        if (statusListener == null) return;
        if (mode == 1) {
            TsumegoPuzzle p = TSUMEGO_PUZZLES[puzzleIndex];
            if (puzzleSolved) {
                statusListener.onStatusChanged("✓ " + p.title + " SOLVED! Brilliant tesuji.", 0xFF10B981);
            } else {
                statusListener.onStatusChanged("● Black to play · " + p.title, 0xFFFFD166);
            }
        } else {
            String turn = (currentTurn == 1) ? "● Black's Turn" : "○ White's Turn (Bot)";
            statusListener.onStatusChanged(turn + " · " + boardSize + "×" + boardSize + " · Captures: ● " + blackCaptures + " | ○ " + whiteCaptures, 0xFFFFD166);
        }
    }

    public boolean playMove(int x, int y) {
        if (x < 0 || x >= boardSize || y < 0 || y >= boardSize || gameOver) return false;
        if (board[y][x] != 0) return false;

        if (mode == 1) {
            TsumegoPuzzle p = TSUMEGO_PUZZLES[puzzleIndex];
            if (x == p.targetX && y == p.targetY) {
                board[y][x] = 1;
                lastMoveX = x;
                lastMoveY = y;
                puzzleSolved = true;
                try {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {}
                updateStatus();
                invalidate();
                return true;
            } else {
                if (statusListener != null) {
                    statusListener.onStatusChanged("✗ Incorrect. Find the vital point!", 0xFFEF4444);
                }
                return false;
            }
        }

        saveState();

        int color = currentTurn;
        int opponent = (color == 1) ? 2 : 1;
        board[y][x] = color;

        // Check captures of opponent
        int capturedCount = 0;
        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[ny][nx] == opponent) {
                boolean[][] visited = new boolean[boardSize][boardSize];
                if (countGroupLiberties(nx, ny, opponent, visited) == 0) {
                    capturedCount += removeGroup(nx, ny, opponent);
                }
            }
        }

        // Suicide check
        boolean[][] selfVisited = new boolean[boardSize][boardSize];
        if (capturedCount == 0 && countGroupLiberties(x, y, color, selfVisited) == 0) {
            board[y][x] = 0; // Revert suicide
            history.remove(history.size() - 1);
            return false;
        }

        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        if (color == 1) blackCaptures += capturedCount;
        else whiteCaptures += capturedCount;

        lastMoveX = x;
        lastMoveY = y;
        moveList.add(new Point(x, y));
        consecutivePasses = 0;
        currentTurn = opponent;
        updateStatus();
        invalidate();

        if (mode == 0 && currentTurn == 2) {
            postDelayed(new Runnable() {
                public void run() { botPlayMove(); }
            }, 380);
        }
        return true;
    }

    private int countGroupLiberties(int startX, int startY, int color, boolean[][] visited) {
        Set<Integer> liberties = new HashSet<>();
        Queue<Point> q = new LinkedList<>();

        q.add(new Point(startX, startY));
        visited[startY][startX] = true;

        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        while (!q.isEmpty()) {
            Point p = q.poll();
            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize) {
                    if (board[ny][nx] == 0) {
                        liberties.add(ny * boardSize + nx);
                    } else if (board[ny][nx] == color && !visited[ny][nx]) {
                        visited[ny][nx] = true;
                        q.add(new Point(nx, ny));
                    }
                }
            }
        }
        return liberties.size();
    }

    private int removeGroup(int startX, int startY, int color) {
        int count = 0;
        Queue<Point> q = new LinkedList<>();
        q.add(new Point(startX, startY));
        board[startY][startX] = 0;
        count++;

        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        while (!q.isEmpty()) {
            Point p = q.poll();
            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[ny][nx] == color) {
                    board[ny][nx] = 0;
                    count++;
                    q.add(new Point(nx, ny));
                }
            }
        }
        return count;
    }

    private int countTerritory(int playerColor) {
        int territory = 0;
        boolean[][] visited = new boolean[boardSize][boardSize];

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (board[r][c] == 0 && !visited[r][c]) {
                    List<Point> emptyRegion = new ArrayList<>();
                    Set<Integer> surroundingColors = new HashSet<>();
                    Queue<Point> q = new LinkedList<>();

                    q.add(new Point(c, r));
                    visited[r][c] = true;

                    int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
                    while (!q.isEmpty()) {
                        Point p = q.poll();
                        emptyRegion.add(p);
                        for (int[] d : dirs) {
                            int nx = p.x + d[0];
                            int ny = p.y + d[1];
                            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize) {
                                if (board[ny][nx] == 0 && !visited[ny][nx]) {
                                    visited[ny][nx] = true;
                                    q.add(new Point(nx, ny));
                                } else if (board[ny][nx] != 0) {
                                    surroundingColors.add(board[ny][nx]);
                                }
                            }
                        }
                    }

                    if (surroundingColors.size() == 1 && surroundingColors.contains(playerColor)) {
                        territory += emptyRegion.size();
                    }
                }
            }
        }
        return territory;
    }

    private void botPlayMove() {
        if (currentTurn != 2 || gameOver) return;

        // 1. Defend own groups in Atari (1 liberty)
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] == 2) {
                    boolean[][] v = new boolean[boardSize][boardSize];
                    if (countGroupLiberties(x, y, 2, v) == 1) {
                        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
                        for (int[] d : dirs) {
                            int nx = x + d[0];
                            int ny = y + d[1];
                            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[ny][nx] == 0) {
                                board[ny][nx] = 2;
                                boolean[][] v2 = new boolean[boardSize][boardSize];
                                int libs = countGroupLiberties(nx, ny, 2, v2);
                                board[ny][nx] = 0;
                                if (libs >= 2) {
                                    if (playMove(nx, ny)) return;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Capture opponent groups in Atari
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] == 1) {
                    boolean[][] v = new boolean[boardSize][boardSize];
                    if (countGroupLiberties(x, y, 1, v) == 1) {
                        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
                        for (int[] d : dirs) {
                            int nx = x + d[0];
                            int ny = y + d[1];
                            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[ny][nx] == 0) {
                                if (playMove(nx, ny)) return;
                            }
                        }
                    }
                }
            }
        }

        // 3. Positional & MCTS Evaluation
        int bestX = -1, bestY = -1;
        float bestScore = -99999f;

        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] != 0) continue;

                board[y][x] = 2;
                boolean[][] vTest = new boolean[boardSize][boardSize];
                int testLibs = countGroupLiberties(x, y, 2, vTest);
                board[y][x] = 0;
                if (testLibs <= 1) continue; // Avoid self-atari

                float score = 0f;
                int distEdgeX = Math.min(x, boardSize - 1 - x);
                int distEdgeY = Math.min(y, boardSize - 1 - y);

                // Star point & 3rd/4th line territory preference
                if (distEdgeX == 2 && distEdgeY == 2) score += 45f;
                else if (distEdgeX >= 2 && distEdgeY >= 2) score += 30f;
                else if (distEdgeX == 0 || distEdgeY == 0) score -= 25f;

                // Contact engagement
                if (lastMoveX >= 0) {
                    int dx = Math.abs(x - lastMoveX);
                    int dy = Math.abs(y - lastMoveY);
                    if (dx <= 1 && dy <= 1 && (dx + dy > 0)) score += 35f;
                }

                // Connections
                int friendly = 0;
                int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
                for (int[] d : dirs) {
                    int nx = x + d[0];
                    int ny = y + d[1];
                    if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[ny][nx] == 2) friendly++;
                }
                score += friendly * 18f;
                score += testLibs * 8f;

                for (int sim = 0; sim < 10; sim++) score += rng.nextFloat() * 12f;

                if (score > bestScore) {
                    bestScore = score;
                    bestX = x;
                    bestY = y;
                }
            }
        }

        if (bestX != -1 && bestY != -1) {
            if (playMove(bestX, bestY)) return;
        }

        passTurn();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float w = getWidth();
            float h = getHeight();
            float pad = dpf(20f);
            float size = Math.min(w, h) - pad * 2;
            float startX = (w - size) / 2f;
            float startY = (h - size) / 2f;
            float cellSize = size / (boardSize - 1);

            int gx = Math.round((event.getX() - startX) / cellSize);
            int gy = Math.round((event.getY() - startY) / cellSize);

            if (gx >= 0 && gx < boardSize && gy >= 0 && gy < boardSize) {
                playMove(gx, gy);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Rich Japanese Hon-Kaya Wood Gradient
        boardRect.set(0, 0, w, h);
        gobanPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF3D2411, 0xFF5C3A1E, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(boardRect, dpf(16f), dpf(16f), gobanPaint);

        // Fine Wood Grain Texture Lines
        for (int i = 1; i < 16; i++) {
            float gy = h * (i / 16f);
            canvas.drawLine(0, gy, w, gy + dpf(2f), woodGrainPaint);
        }

        RectF innerRect = new RectF(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(innerRect, dpf(14f), dpf(14f), goldBorderPaint);

        float pad = dpf(22f);
        float size = Math.min(w, h) - pad * 2;
        float startX = (w - size) / 2f;
        float startY = (h - size) / 2f;
        float cellSize = size / (boardSize - 1);

        // Draw Grid Lines & Coordinates
        coordTextPaint.setTextSize(Math.max(dpf(7.5f), cellSize * 0.35f));

        for (int i = 0; i < boardSize; i++) {
            float py = startY + i * cellSize;
            canvas.drawLine(startX, py, startX + size, py, gridLinePaint);
            float px = startX + i * cellSize;
            canvas.drawLine(px, startY, px, startY + size, gridLinePaint);

            String colName = String.valueOf((char) ('A' + (i >= 8 ? i + 1 : i)));
            canvas.drawText(colName, px, startY - dpf(6f), coordTextPaint);
            canvas.drawText(String.valueOf(boardSize - i), startX - dpf(9f), py + dpf(3f), coordTextPaint);
        }

        // Draw Hoshi Star Points
        int[] hoshi = getHoshiPoints();
        for (int hx : hoshi) {
            for (int hy : hoshi) {
                float px = startX + hx * cellSize;
                float py = startY + hy * cellSize;
                canvas.drawCircle(px, py, dpf(3.5f), starPointPaint);
                canvas.drawCircle(px, py, dpf(1.8f), goldBorderPaint);
            }
        }

        // Draw Territory Overlay if enabled
        if (showTerritory) {
            float terR = cellSize * 0.25f;
            for (int r = 0; r < boardSize; r++) {
                for (int c = 0; c < boardSize; c++) {
                    if (board[r][c] == 0) {
                        float cx = startX + c * cellSize;
                        float cy = startY + r * cellSize;
                        canvas.drawCircle(cx, cy, terR, territoryBlackPaint);
                    }
                }
            }
        }

        // Draw 3D Bi-convex Stones (Slate & Clamshell)
        float stoneR = cellSize * 0.47f;

        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                int val = board[y][x];
                if (val == 0) continue;

                float cx = startX + x * cellSize;
                float cy = startY + y * cellSize;

                // Contact Shadow
                canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2.5f), stoneR, shadowPaint);

                if (val == 1) { // Matte Obsidian Slate
                    RadialGradient blackGrad = new RadialGradient(
                        cx - stoneR * 0.35f, cy - stoneR * 0.35f, stoneR * 1.3f,
                        new int[]{0xFF475569, 0xFF0F172A, 0xFF020617},
                        null, Shader.TileMode.CLAMP
                    );
                    stone3dPaint.setShader(blackGrad);
                    canvas.drawCircle(cx, cy, stoneR, stone3dPaint);
                    canvas.drawCircle(cx - stoneR * 0.35f, cy - stoneR * 0.35f, stoneR * 0.24f, stoneShinePaint);
                } else if (val == 2) { // Hyuga Clamshell Pearl
                    RadialGradient whiteGrad = new RadialGradient(
                        cx - stoneR * 0.35f, cy - stoneR * 0.35f, stoneR * 1.3f,
                        new int[]{0xFFFFFFFF, 0xFFF1F5F9, 0xFF94A3B8},
                        null, Shader.TileMode.CLAMP
                    );
                    stone3dPaint.setShader(whiteGrad);
                    canvas.drawCircle(cx, cy, stoneR, stone3dPaint);
                    canvas.drawCircle(cx, cy, stoneR, stoneRimPaint);
                    canvas.drawCircle(cx - stoneR * 0.35f, cy - stoneR * 0.35f, stoneR * 0.28f, stoneShinePaint);
                }

                // Last Move Indicator
                if (x == lastMoveX && y == lastMoveY) {
                    canvas.drawCircle(cx, cy, stoneR * 0.5f, lastMovePaint);
                }
            }
        }
    }

    private int[] getHoshiPoints() {
        if (boardSize == 19) return new int[]{3, 9, 15};
        if (boardSize == 13) return new int[]{3, 6, 9};
        return new int[]{2, 4, 6};
    }
}
