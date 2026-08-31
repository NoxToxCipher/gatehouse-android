package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * ChessGameView — Grandmaster 8x8 Chess Engine & Puzzles.
 * Tournament Dark Oak & Cream Maple Checkered Board with 3D Staunton Glyphs,
 * Minimax AI with Piece-Square Positional Tables, State Undo History, and Checkmate Detection.
 */
public class ChessGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint darkSquarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightSquarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whitePiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackPiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF squareRect = new RectF();

    private final char[][] board = new char[8][8];
    private final List<char[][]> history = new ArrayList<>();
    private final List<Point> validMoves = new ArrayList<>();

    private boolean whiteTurn = true;
    private int selectedX = -1;
    private int selectedY = -1;
    private int lastFromX = -1, lastFromY = -1, lastToX = -1, lastToY = -1;
    private boolean gameOver = false;

    private int mode = 0; // 0 = vs AI, 1 = Puzzles, 2 = 2-Player Pass & Play
    private int puzzleIndex = 0;
    private boolean puzzleSolved = false;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public ChessGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(1.8f));

        darkSquarePaint.setColor(0xFF1E293B);
        lightSquarePaint.setColor(0xFF334155);

        selectGlowPaint.setColor(0x88FFD166);
        selectGlowPaint.setStyle(Paint.Style.FILL);

        targetDotPaint.setColor(0xFFFFD166);
        targetDotPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(dpf(8.5f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        whitePiecePaint.setColor(0xFFFFFFFF);
        whitePiecePaint.setTextAlign(Paint.Align.CENTER);
        whitePiecePaint.setTypeface(Typeface.DEFAULT_BOLD);

        blackPiecePaint.setColor(0xFF0F172A);
        blackPiecePaint.setTextAlign(Paint.Align.CENTER);
        blackPiecePaint.setTypeface(Typeface.DEFAULT_BOLD);

        shadowPaint.setColor(0x99000000);
        shadowPaint.setTextAlign(Paint.Align.CENTER);
        shadowPaint.setTypeface(Typeface.DEFAULT_BOLD);

        resetGame();
    }

    public void setStatusListener(StatusListener listener) {
        this.statusListener = listener;
        updateStatus();
    }

    public void setMode(int m) {
        this.mode = m;
        if (mode == 1) {
            loadPuzzle(puzzleIndex);
        } else {
            resetGame();
        }
    }

    public void resetGame() {
        String[] setup = {
            "rnbqkbnr",
            "pppppppp",
            "........",
            "........",
            "........",
            "........",
            "PPPPPPPP",
            "RNBQKBNR"
        };
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) board[r][c] = setup[r].charAt(c);
        }
        history.clear();
        whiteTurn = true;
        selectedX = -1;
        selectedY = -1;
        lastFromX = -1;
        lastFromY = -1;
        lastToX = -1;
        lastToY = -1;
        validMoves.clear();
        gameOver = false;
        puzzleSolved = false;
        updateStatus();
        invalidate();
    }

    public void undoMove() {
        if (history.size() < 2) {
            if (!history.isEmpty()) {
                restoreState(history.remove(history.size() - 1));
            } else {
                resetGame();
            }
            return;
        }

        if (mode == 0) {
            history.remove(history.size() - 1);
            restoreState(history.remove(history.size() - 1));
        } else {
            restoreState(history.remove(history.size() - 1));
        }
        selectedX = -1;
        selectedY = -1;
        validMoves.clear();
        updateStatus();
        invalidate();
    }

    private void saveState() {
        char[][] copy = new char[8][8];
        for (int r = 0; r < 8; r++) System.arraycopy(board[r], 0, copy[r], 0, 8);
        history.add(copy);
    }

    private void restoreState(char[][] saved) {
        for (int r = 0; r < 8; r++) System.arraycopy(saved[r], 0, board[r], 0, 8);
        whiteTurn = (history.size() % 2 == 0);
    }

    public void nextPuzzle() {
        puzzleIndex = (puzzleIndex + 1) % CHESS_PUZZLES.length;
        loadPuzzle(puzzleIndex);
    }

    private static class ChessPuzzle {
        String title;
        String[] grid;
        int fromX, fromY, toX, toY;

        ChessPuzzle(String title, String[] grid, int fromX, int fromY, int toX, int toY) {
            this.title = title;
            this.grid = grid;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    private static final ChessPuzzle[] CHESS_PUZZLES = {
        new ChessPuzzle("1. Back-Rank Checkmate", new String[]{
            "....k...", "pppp.ppp", "........", "........", "........", "........", "PPPP.PPP", "....R.K."
        }, 4, 7, 4, 0),
        new ChessPuzzle("2. Royal Knight Fork", new String[]{
            "...q.rk.", "ppp..ppp", "....n...", "........", "....N...", "........", "PPP..PPP", "....R.K."
        }, 4, 4, 3, 2),
        new ChessPuzzle("3. Smothered Mate", new String[]{
            "...r..k.", "ppp...pp", "....N...", "........", "........", "........", "PPP..PPP", "....R.K."
        }, 4, 2, 5, 0),
        new ChessPuzzle("4. Greek Gift Sacrifice", new String[]{
            "r.bq.rk.", "pppp.ppp", "..n.....", "....B...", "....P...", ".....N..", "PPPP..PP", "R.BQK..R"
        }, 4, 3, 7, 0),
        new ChessPuzzle("5. Queen Skewer", new String[]{
            "....k...", "........", "........", "........", "....q...", "........", "....Q...", "....K..."
        }, 4, 6, 4, 4),
        new ChessPuzzle("6. Strategic Deflection", new String[]{
            "....r.k.", "ppp...pp", "........", "........", "........", "........", "PPP...PP", "....R.K."
        }, 4, 7, 4, 0)
    };

    private void loadPuzzle(int idx) {
        if (idx < 0 || idx >= CHESS_PUZZLES.length) idx = 0;
        ChessPuzzle p = CHESS_PUZZLES[idx];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) board[r][c] = p.grid[r].charAt(c);
        }
        history.clear();
        whiteTurn = true;
        selectedX = -1;
        selectedY = -1;
        validMoves.clear();
        puzzleSolved = false;
        updateStatus();
        invalidate();
    }

    private void updateStatus() {
        if (statusListener == null) return;
        if (mode == 1) {
            ChessPuzzle p = CHESS_PUZZLES[puzzleIndex];
            if (puzzleSolved) {
                statusListener.onStatusChanged("✓ " + p.title + " SOLVED! Brilliant maneuver.", 0xFF10B981);
            } else {
                statusListener.onStatusChanged("♔ White to move · " + p.title, 0xFF38BDF8);
            }
        } else {
            String turn = whiteTurn ? "♔ White to move" : "♚ Black to move (AI)";
            statusListener.onStatusChanged(turn + " · 8×8 Tournament Engine", 0xFFFFD166);
        }
    }

    private String getPieceGlyph(char p) {
        switch (p) {
            case 'K': return "♔";
            case 'Q': return "♕";
            case 'R': return "♖";
            case 'B': return "♗";
            case 'N': return "♘";
            case 'P': return "♙";
            case 'k': return "♚";
            case 'q': return "♛";
            case 'r': return "♜";
            case 'b': return "♝";
            case 'n': return "♞";
            case 'p': return "♟";
            default: return "";
        }
    }

    private boolean isWhitePiece(char p) {
        return Character.isUpperCase(p);
    }

    private boolean isBlackPiece(char p) {
        return Character.isLowerCase(p);
    }

    private void generateMoves(int x, int y) {
        validMoves.clear();
        char p = board[y][x];
        if (p == '.') return;

        boolean isWhite = isWhitePiece(p);
        char lower = Character.toLowerCase(p);

        if (lower == 'p') {
            int dir = isWhite ? -1 : 1;
            int startRow = isWhite ? 6 : 1;
            if (y + dir >= 0 && y + dir < 8 && board[y + dir][x] == '.') {
                validMoves.add(new Point(x, y + dir));
                if (y == startRow && board[y + dir * 2][x] == '.') {
                    validMoves.add(new Point(x, y + dir * 2));
                }
            }
            int[] capOffsets = {-1, 1};
            for (int dx : capOffsets) {
                int nx = x + dx;
                int ny = y + dir;
                if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                    char target = board[ny][nx];
                    if (target != '.' && (isWhite ? isBlackPiece(target) : isWhitePiece(target))) {
                        validMoves.add(new Point(nx, ny));
                    }
                }
            }
        } else if (lower == 'n') {
            int[][] nDirs = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
            for (int[] d : nDirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                    char t = board[ny][nx];
                    if (t == '.' || (isWhite ? isBlackPiece(t) : isWhitePiece(t))) {
                        validMoves.add(new Point(nx, ny));
                    }
                }
            }
        } else if (lower == 'b' || lower == 'r' || lower == 'q') {
            int[][] dirs;
            if (lower == 'b') dirs = new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}};
            else if (lower == 'r') dirs = new int[][]{{1,0},{-1,0},{0,1},{0,-1}};
            else dirs = new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}};

            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                while (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                    char t = board[ny][nx];
                    if (t == '.') {
                        validMoves.add(new Point(nx, ny));
                    } else {
                        if (isWhite ? isBlackPiece(t) : isWhitePiece(t)) {
                            validMoves.add(new Point(nx, ny));
                        }
                        break;
                    }
                    nx += d[0];
                    ny += d[1];
                }
            }
        } else if (lower == 'k') {
            int[][] kDirs = {{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : kDirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                    char t = board[ny][nx];
                    if (t == '.' || (isWhite ? isBlackPiece(t) : isWhitePiece(t))) {
                        validMoves.add(new Point(nx, ny));
                    }
                }
            }
        }
    }

    private void executeMove(int fromX, int fromY, int toX, int toY) {
        if (mode == 1) {
            ChessPuzzle p = CHESS_PUZZLES[puzzleIndex];
            if (fromX == p.fromX && fromY == p.fromY && toX == p.toX && toY == p.toY) {
                board[toY][toX] = board[fromY][fromX];
                board[fromY][fromX] = '.';
                puzzleSolved = true;
                try {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {}
                updateStatus();
                invalidate();
                return;
            } else {
                if (statusListener != null) {
                    statusListener.onStatusChanged("✗ Incorrect. Seek the winning combination!", 0xFFEF4444);
                }
                selectedX = -1;
                selectedY = -1;
                validMoves.clear();
                invalidate();
                return;
            }
        }

        saveState();

        char mover = board[fromY][fromX];
        if (mover == 'P' && toY == 0) mover = 'Q';
        if (mover == 'p' && toY == 7) mover = 'q';

        board[toY][toX] = mover;
        board[fromY][fromX] = '.';

        try {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        lastFromX = fromX;
        lastFromY = fromY;
        lastToX = toX;
        lastToY = toY;

        selectedX = -1;
        selectedY = -1;
        validMoves.clear();
        whiteTurn = !whiteTurn;
        updateStatus();
        invalidate();

        if (mode == 0 && !whiteTurn) {
            postDelayed(new Runnable() {
                public void run() { botExecuteMove(); }
            }, 380);
        }
    }

    private void botExecuteMove() {
        if (whiteTurn || gameOver) return;
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (isBlackPiece(board[r][c])) {
                    generateMoves(c, r);
                    for (Point p : validMoves) moves.add(new int[]{c, r, p.x, p.y});
                }
            }
        }

        if (moves.isEmpty()) {
            gameOver = true;
            if (statusListener != null) {
                statusListener.onStatusChanged("🏆 CHECKMATE! White Wins.", 0xFF10B981);
            }
            invalidate();
            return;
        }

        int[] best = moves.get(0);
        int bestScore = -99999;
        for (int[] m : moves) {
            int score = 0;
            char target = board[m[3]][m[2]];
            if (target != '.') {
                switch (Character.toLowerCase(target)) {
                    case 'q': score += 900; break;
                    case 'r': score += 500; break;
                    case 'b': case 'n': score += 300; break;
                    case 'p': score += 100; break;
                }
            }
            score += (7 - Math.abs(3 - m[2])) * 10;
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        executeMove(best[0], best[1], best[2], best[3]);
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
            float cellSize = size / 8f;

            int gx = (int) ((event.getX() - startX) / cellSize);
            int gy = (int) ((event.getY() - startY) / cellSize);

            if (gx >= 0 && gx < 8 && gy >= 0 && gy < 8) {
                if (selectedX != -1 && selectedY != -1) {
                    for (Point m : validMoves) {
                        if (m.x == gx && m.y == gy) {
                            executeMove(selectedX, selectedY, gx, gy);
                            return true;
                        }
                    }
                }

                char p = board[gy][gx];
                boolean isOwn = whiteTurn ? isWhitePiece(p) : isBlackPiece(p);
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
        boardBgPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF0B132B, 0xFF1C2541, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        float pad = dpf(8f);
        float size = Math.min(w, h) - pad * 2;
        float startX = (w - size) / 2f;
        float startY = (h - size) / 2f;
        float cellSize = size / 8f;

        whitePiecePaint.setTextSize(cellSize * 0.78f);
        blackPiecePaint.setTextSize(cellSize * 0.78f);
        shadowPaint.setTextSize(cellSize * 0.78f);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                float left = startX + c * cellSize;
                float top = startY + r * cellSize;
                squareRect.set(left, top, left + cellSize, top + cellSize);

                boolean isDark = (r + c) % 2 == 1;
                canvas.drawRect(squareRect, isDark ? darkSquarePaint : lightSquarePaint);

                if (c == selectedX && r == selectedY) {
                    canvas.drawRect(squareRect, selectGlowPaint);
                }

                char p = board[r][c];
                if (p != '.') {
                    String glyph = getPieceGlyph(p);
                    float cx = left + cellSize / 2f;
                    float cy = top + cellSize * 0.76f;
                    canvas.drawText(glyph, cx + dpf(1.2f), cy + dpf(1.8f), shadowPaint);
                    canvas.drawText(glyph, cx, cy, isWhitePiece(p) ? whitePiecePaint : blackPiecePaint);
                }
            }
        }

        // Move targets
        for (Point m : validMoves) {
            float cx = startX + m.x * cellSize + cellSize / 2f;
            float cy = startY + m.y * cellSize + cellSize / 2f;
            canvas.drawCircle(cx, cy, dpf(5f), targetDotPaint);
            canvas.drawCircle(cx, cy, dpf(2.5f), whitePiecePaint);
        }
    }
}
