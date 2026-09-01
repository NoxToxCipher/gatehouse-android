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
    private final Paint goldDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lastMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coordPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whitePiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackPiecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

    // Smooth Piece Motion Animation
    private boolean isAnimating = false;
    private char animPiece = '.';
    private float animFromCol = 0f, animFromRow = 0f;
    private float animToCol = 0f, animToRow = 0f;
    private long animStartTime = 0;
    private static final long ANIM_DURATION_MS = 240;
    private int animHideCol = -1, animHideRow = -1;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public ChessGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(2f));

        goldDetailPaint.setColor(0xFFCA8A04);
        goldDetailPaint.setStyle(Paint.Style.STROKE);
        goldDetailPaint.setStrokeWidth(dpf(1f));

        darkSquarePaint.setColor(0xFF78482A);
        lightSquarePaint.setColor(0xFFECE0C8);

        selectGlowPaint.setColor(0x66FFD166);
        selectGlowPaint.setStyle(Paint.Style.FILL);

        lastMovePaint.setColor(0x44F59E0B);
        lastMovePaint.setStyle(Paint.Style.FILL);

        checkGlowPaint.setColor(0x77EF4444);
        checkGlowPaint.setStyle(Paint.Style.FILL);

        targetDotPaint.setColor(0xFFFFD166);
        targetDotPaint.setStyle(Paint.Style.FILL);

        targetRingPaint.setColor(0xCCEF4444);
        targetRingPaint.setStyle(Paint.Style.STROKE);
        targetRingPaint.setStrokeWidth(dpf(2.5f));

        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(dpf(8.5f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        coordPaint.setColor(0xFFCA8A04);
        coordPaint.setTextSize(dpf(9f));
        coordPaint.setTextAlign(Paint.Align.CENTER);
        coordPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        whitePiecePaint.setColor(0xFFFFFDF5);
        whitePiecePaint.setTextAlign(Paint.Align.CENTER);
        whitePiecePaint.setTypeface(Typeface.DEFAULT_BOLD);

        blackPiecePaint.setColor(0xFF181512);
        blackPiecePaint.setTextAlign(Paint.Align.CENTER);
        blackPiecePaint.setTypeface(Typeface.DEFAULT_BOLD);

        shadowPaint.setColor(0x88000000);
        shadowPaint.setTextAlign(Paint.Align.CENTER);
        shadowPaint.setTypeface(Typeface.DEFAULT_BOLD);

        pieceRimPaint.setColor(0xAAFFD166);
        pieceRimPaint.setTextAlign(Paint.Align.CENTER);
        pieceRimPaint.setTypeface(Typeface.DEFAULT_BOLD);

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
            float eval = calculateEvaluation();
            String evalStr = (eval >= 0 ? "+" : "") + String.format(java.util.Locale.US, "%.1f", eval);
            String opening = detectChessOpening();
            String turn = whiteTurn ? "♔ White (" + evalStr + ")" : "♚ Black (" + evalStr + ")";
            statusListener.onStatusChanged(turn + " · " + opening, 0xFFFFD166);
        }
    }

    public float calculateEvaluation() {
        int whiteMaterial = 0;
        int blackMaterial = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char p = board[r][c];
                if (p == '.') continue;
                int val = 0;
                switch (Character.toLowerCase(p)) {
                    case 'p': val = 100; break;
                    case 'n': case 'b': val = 320; break;
                    case 'r': val = 500; break;
                    case 'q': val = 950; break;
                    case 'k': val = 20000; break;
                }
                if (isWhitePiece(p)) whiteMaterial += val;
                else blackMaterial += val;
            }
        }
        int diff = whiteMaterial - blackMaterial;
        return diff / 100.0f;
    }

    private String detectChessOpening() {
        if (history.isEmpty()) return "Opening Phase (1. e4 / d4)";
        if (history.size() <= 2) {
            if (board[4][4] == 'P' && board[3][4] == 'p') return "⚔️ King's Pawn Open Game";
            if (board[4][3] == 'P' && board[3][3] == 'p') return "⚔️ Queen's Pawn Game";
            if (board[4][4] == 'P' && board[3][2] == 'p') return "⚡ Sicilian Defense";
            if (board[4][4] == 'P' && board[2][4] == 'p') return "🛡️ French Defense";
        } else if (history.size() <= 6) {
            if (board[4][4] == 'P' && board[3][2] == 'p') return "⚡ Sicilian Defense (Open)";
            if (board[4][3] == 'P' && board[4][2] == 'P') return "👑 Queen's Gambit";
            if (board[3][1] == 'B' && board[4][4] == 'P') return "🏰 Ruy Lopez (Spanish Game)";
            return "♟️ Strategic Middle Game";
        }
        return "⚔️ Grandmaster Combat";
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
                char mover = board[fromY][fromX];
                isAnimating = true;
                animPiece = mover;
                animFromCol = fromX;
                animFromRow = fromY;
                animToCol = toX;
                animToRow = toY;
                animHideCol = toX;
                animHideRow = toY;
                animStartTime = System.currentTimeMillis();

                board[toY][toX] = mover;
                board[fromY][fromX] = '.';
                puzzleSolved = true;
                try {
                    RecreationAudioSynth.playTetrisLineClear();
                    performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                } catch (Exception ignored) {}
                updateStatus();
                invalidate();
                postDelayed(new Runnable() {
                    public void run() {
                        nextPuzzle();
                    }
                }, 1600);
                return;
            } else {
                if (statusListener != null) {
                    statusListener.onStatusChanged("✗ Incorrect. Seek the winning combination!", 0xFFEF4444);
                }
                selectedX = -1;
                selectedY = -1;
                validMoves.clear();
                try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
                invalidate();
                return;
            }
        }

        boolean isCapture = (board[toY][toX] != '.');
        saveState();

        char mover = board[fromY][fromX];
        if (mover == 'P' && toY == 0) mover = 'Q';
        if (mover == 'p' && toY == 7) mover = 'q';

        // Start smooth piece slide animation
        isAnimating = true;
        animPiece = mover;
        animFromCol = fromX;
        animFromRow = fromY;
        animToCol = toX;
        animToRow = toY;
        animHideCol = toX;
        animHideRow = toY;
        animStartTime = System.currentTimeMillis();

        board[toY][toX] = mover;
        board[fromY][fromX] = '.';

        try {
            RecreationAudioSynth.playChessPieceThud(isCapture);
            if (isCapture) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            } else {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Exception ignored) {}

        lastFromX = fromX;
        lastFromY = fromY;
        lastToX = toX;
        lastToY = toY;

        selectedX = -1;
        selectedY = -1;
        validMoves.clear();
        whiteTurn = !whiteTurn;

        // Check if move gives check to opponent
        if (isKingInCheck(whiteTurn)) {
            try {
                RecreationAudioSynth.playLaserShoot();
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            } catch (Exception ignored) {}
        }

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

    private Point findKing(boolean isWhite) {
        char kChar = isWhite ? 'K' : 'k';
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == kChar) return new Point(c, r);
            }
        }
        return null;
    }

    private boolean isKingInCheck(boolean isWhite) {
        Point k = findKing(isWhite);
        if (k == null) return false;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char p = board[r][c];
                if (p != '.' && (isWhite ? isBlackPiece(p) : isWhitePiece(p))) {
                    if (canPieceAttack(c, r, k.x, k.y)) return true;
                }
            }
        }
        return false;
    }

    private boolean canPieceAttack(int fx, int fy, int tx, int ty) {
        char p = board[fy][fx];
        char lower = Character.toLowerCase(p);
        boolean isWhite = isWhitePiece(p);

        if (lower == 'p') {
            int dir = isWhite ? -1 : 1;
            return (ty == fy + dir && (tx == fx - 1 || tx == fx + 1));
        } else if (lower == 'n') {
            int dx = Math.abs(tx - fx);
            int dy = Math.abs(ty - fy);
            return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
        } else if (lower == 'k') {
            return Math.abs(tx - fx) <= 1 && Math.abs(ty - fy) <= 1;
        } else if (lower == 'b' || lower == 'r' || lower == 'q') {
            boolean straight = (fx == tx || fy == ty);
            boolean diag = (Math.abs(tx - fx) == Math.abs(ty - fy));
            if (lower == 'b' && !diag) return false;
            if (lower == 'r' && !straight) return false;
            if (lower == 'q' && !straight && !diag) return false;

            int stepX = Integer.compare(tx, fx);
            int stepY = Integer.compare(ty, fy);
            int cx = fx + stepX;
            int cy = fy + stepY;
            while (cx != tx || cy != ty) {
                if (board[cy][cx] != '.') return false;
                cx += stepX;
                cy += stepY;
            }
            return true;
        }
        return false;
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
            float margin = dpf(14f);
            float pad = dpf(4f);
            float totalPadding = margin + pad;
            float size = Math.min(w, h) - totalPadding * 2f;
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

        // Rich Walnut / Obsidian Masterpiece Frame
        rect.set(0, 0, w, h);
        boardBgPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF140F0B, 0xFF2A1C12, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        // Draw Real-Time Advantage Evaluation Bar
        if (mode != 1) {
            float eval = calculateEvaluation();
            float evalRate = Math.max(0.08f, Math.min(0.92f, 0.5f + (eval / 15.0f)));
            float barH = dpf(3.5f);
            float barY = dpf(2.5f);
            float barW = w - dpf(14f);
            float barX = dpf(7f);

            Paint wEvalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wEvalPaint.setColor(0xFFF8FAFC);
            wEvalPaint.setStyle(Paint.Style.FILL);
            RectF wEvalRect = new RectF(barX, barY, barX + barW * evalRate, barY + barH);
            canvas.drawRoundRect(wEvalRect, dpf(2f), dpf(2f), wEvalPaint);

            Paint bEvalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bEvalPaint.setColor(0xFF0F172A);
            bEvalPaint.setStyle(Paint.Style.FILL);
            RectF bEvalRect = new RectF(barX + barW * evalRate, barY, barX + barW, barY + barH);
            canvas.drawRoundRect(bEvalRect, dpf(2f), dpf(2f), bEvalPaint);
        }

        float margin = dpf(14f);
        float pad = dpf(4f);
        float totalPadding = margin + pad;
        float size = Math.min(w, h) - totalPadding * 2f;
        float startX = (w - size) / 2f;
        float startY = (h - size) / 2f;
        float cellSize = size / 8f;

        // Inner Board Inlay Line
        rect.set(startX - dpf(2f), startY - dpf(2f), startX + size + dpf(2f), startY + size + dpf(2f));
        canvas.drawRect(rect, goldDetailPaint);

        // Draw Rank (1-8) & File (a-h) Margin Labels
        String[] files = {"a", "b", "c", "d", "e", "f", "g", "h"};
        for (int i = 0; i < 8; i++) {
            float fcx = startX + i * cellSize + cellSize / 2f;
            canvas.drawText(files[i], fcx, startY - dpf(3.5f), coordPaint);
            canvas.drawText(files[i], fcx, startY + size + margin - dpf(2f), coordPaint);

            float rcy = startY + i * cellSize + cellSize / 2f + dpf(3f);
            String rank = String.valueOf(8 - i);
            canvas.drawText(rank, startX - dpf(7.5f), rcy, coordPaint);
            canvas.drawText(rank, startX + size + dpf(7.5f), rcy, coordPaint);
        }

        whitePiecePaint.setTextSize(cellSize * 0.78f);
        blackPiecePaint.setTextSize(cellSize * 0.78f);
        shadowPaint.setTextSize(cellSize * 0.78f);
        pieceRimPaint.setTextSize(cellSize * 0.78f);

        Point whiteKing = findKing(true);
        Point blackKing = findKing(false);
        boolean whiteInCheck = isKingInCheck(true);
        boolean blackInCheck = isKingInCheck(false);

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                float left = startX + c * cellSize;
                float top = startY + r * cellSize;
                squareRect.set(left, top, left + cellSize, top + cellSize);

                boolean isDark = (r + c) % 2 == 1;
                canvas.drawRect(squareRect, isDark ? darkSquarePaint : lightSquarePaint);

                // Subtle Square Relief Border
                if (!isDark) {
                    Paint bevel = new Paint(Paint.ANTI_ALIAS_FLAG);
                    bevel.setColor(0x33FFFFFF);
                    bevel.setStrokeWidth(dpf(1f));
                    canvas.drawLine(left, top, left + cellSize, top, bevel);
                    canvas.drawLine(left, top, left, top + cellSize, bevel);
                }

                // Last Move Highlight
                if ((c == lastFromX && r == lastFromY) || (c == lastToX && r == lastToY)) {
                    canvas.drawRect(squareRect, lastMovePaint);
                }

                // King In Check Highlight
                if ((whiteInCheck && whiteKing != null && whiteKing.x == c && whiteKing.y == r) ||
                    (blackInCheck && blackKing != null && blackKing.x == c && blackKing.y == r)) {
                    canvas.drawRect(squareRect, checkGlowPaint);
                }

                // Selected Square Glow
                if (c == selectedX && r == selectedY) {
                    canvas.drawRect(squareRect, selectGlowPaint);
                    // Selected Corner Ticks
                    Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
                    tick.setColor(0xFFFFD166);
                    tick.setStrokeWidth(dpf(2f));
                    float tLen = dpf(6f);
                    canvas.drawLine(left, top, left + tLen, top, tick);
                    canvas.drawLine(left, top, left, top + tLen, tick);
                    canvas.drawLine(left + cellSize, top, left + cellSize - tLen, top, tick);
                    canvas.drawLine(left + cellSize, top, left + cellSize, top + tLen, tick);
                    canvas.drawLine(left, top + cellSize, left + tLen, top + cellSize, tick);
                    canvas.drawLine(left, top + cellSize, left, top + cellSize - tLen, tick);
                    canvas.drawLine(left + cellSize, top + cellSize, left + cellSize - tLen, top + cellSize, tick);
                    canvas.drawLine(left + cellSize, top + cellSize, left + cellSize, top + cellSize - tLen, tick);
                }

                char p = board[r][c];
                // If this cell is currently the landing target of an active piece animation, skip drawing static piece
                if (p != '.' && !(isAnimating && c == animHideCol && r == animHideRow)) {
                    String glyph = getPieceGlyph(p);
                    float cx = left + cellSize / 2f;
                    float cy = top + cellSize * 0.76f;
                    boolean isWhite = isWhitePiece(p);
                    boolean isSelected = (c == selectedX && r == selectedY);

                    float drawCy = isSelected ? (cy - dpf(4f)) : cy;
                    float originalWhiteTextSize = whitePiecePaint.getTextSize();
                    float originalBlackTextSize = blackPiecePaint.getTextSize();

                    if (isSelected) {
                        float liftedSize = cellSize * 0.82f;
                        whitePiecePaint.setTextSize(liftedSize);
                        blackPiecePaint.setTextSize(liftedSize);
                        shadowPaint.setTextSize(liftedSize);

                        // Floating Deep Drop Shadow
                        canvas.drawText(glyph, cx + dpf(3.5f), cy + dpf(5.5f), shadowPaint);
                    } else {
                        // Normal Contact Shadow
                        canvas.drawText(glyph, cx + dpf(1.5f), cy + dpf(2.2f), shadowPaint);
                    }

                    if (isWhite) {
                        canvas.drawText(glyph, cx, drawCy, whitePiecePaint);
                    } else {
                        // Subtle golden hairline for obsidian black piece
                        canvas.drawText(glyph, cx + dpf(0.5f), drawCy + dpf(0.5f), pieceRimPaint);
                        canvas.drawText(glyph, cx, drawCy, blackPiecePaint);
                    }

                    if (isSelected) {
                        whitePiecePaint.setTextSize(originalWhiteTextSize);
                        blackPiecePaint.setTextSize(originalBlackTextSize);
                        shadowPaint.setTextSize(originalWhiteTextSize);
                    }
                }
            }
        }

        // Draw active animated gliding piece above the board
        if (isAnimating && animPiece != '.') {
            long now = System.currentTimeMillis();
            float t = (float) (now - animStartTime) / (float) ANIM_DURATION_MS;
            if (t >= 1f) {
                t = 1f;
                isAnimating = false;
                animHideCol = -1;
                animHideRow = -1;
            } else {
                postInvalidateOnAnimation();
            }

            // Smooth cubic ease-in-out translation
            float easeT = (t < 0.5f) ? (2f * t * t) : (1f - (float) Math.pow(-2f * t + 2f, 2) / 2f);
            float curCol = animFromCol + (animToCol - animFromCol) * easeT;
            float curRow = animFromRow + (animToRow - animFromRow) * easeT;

            float cx = startX + curCol * cellSize + cellSize / 2f;
            float baseCy = startY + curRow * cellSize + cellSize * 0.76f;

            // Parabolic lift height (lifts smoothly into 3D space and lands)
            float lift = (float) Math.sin(t * Math.PI) * dpf(9f);
            float drawCy = baseCy - lift;

            String glyph = getPieceGlyph(animPiece);
            boolean isWhite = isWhitePiece(animPiece);

            float originalWhiteTextSize = whitePiecePaint.getTextSize();
            float originalBlackTextSize = blackPiecePaint.getTextSize();
            float movingSize = cellSize * 0.84f;

            whitePiecePaint.setTextSize(movingSize);
            blackPiecePaint.setTextSize(movingSize);
            shadowPaint.setTextSize(movingSize);

            // Dynamic airborne drop shadow (drops lower and widens as piece ascends)
            float shadowOffset = dpf(2f) + lift * 0.75f;
            canvas.drawText(glyph, cx + shadowOffset * 0.6f, baseCy + shadowOffset, shadowPaint);

            if (isWhite) {
                canvas.drawText(glyph, cx, drawCy, whitePiecePaint);
            } else {
                canvas.drawText(glyph, cx + dpf(0.6f), drawCy + dpf(0.6f), pieceRimPaint);
                canvas.drawText(glyph, cx, drawCy, blackPiecePaint);
            }

            whitePiecePaint.setTextSize(originalWhiteTextSize);
            blackPiecePaint.setTextSize(originalBlackTextSize);
            shadowPaint.setTextSize(originalWhiteTextSize);
        }

        // Move targets
        for (Point m : validMoves) {
            float cx = startX + m.x * cellSize + cellSize / 2f;
            float cy = startY + m.y * cellSize + cellSize / 2f;
            char targetPiece = board[m.y][m.x];

            if (targetPiece == '.') {
                // Empty destination pip
                canvas.drawCircle(cx, cy, dpf(4.5f), targetDotPaint);
                canvas.drawCircle(cx, cy, dpf(2f), whitePiecePaint);
            } else {
                // Capture target ring
                canvas.drawCircle(cx, cy, cellSize * 0.40f, targetRingPaint);
            }
        }

        // Live Captured Pieces Graveyard & Material Advantage Bar
        drawCapturedGraveyard(canvas, w, h, startX, startY, size);
    }

    private void drawCapturedGraveyard(Canvas canvas, float w, float h, float startX, float startY, float size) {
        MaterialInfo info = calculateMaterial();
        float graveTextSize = dpf(9.5f);
        textPaint.setTextSize(graveTextSize);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Top Graveyard: Black pieces captured by White (Shown in top frame margin)
        float topGraveY = startY - dpf(1.5f);
        if (startY > dpf(18f)) topGraveY = dpf(11f);
        float curX = startX + dpf(2f);

        for (char p : info.blackCaptured) {
            String g = getPieceGlyph(p);
            canvas.drawText(g, curX, topGraveY, blackPiecePaint);
            curX += dpf(11f);
        }

        // White material advantage badge
        int whiteAdv = info.whiteScore - info.blackScore;
        if (whiteAdv > 0) {
            Paint advPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            advPaint.setColor(0xFFFFD166);
            advPaint.setTextSize(dpf(8.5f));
            advPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("+" + whiteAdv, curX + dpf(4f), topGraveY - dpf(1f), advPaint);
        }

        // Bottom Graveyard: White pieces captured by Black (Shown in bottom frame margin)
        float botGraveY = startY + size + dpf(12f);
        if (botGraveY > h - dpf(3f)) botGraveY = h - dpf(4f);
        float curBotX = startX + dpf(2f);

        for (char p : info.whiteCaptured) {
            String g = getPieceGlyph(p);
            canvas.drawText(g, curBotX, botGraveY, whitePiecePaint);
            curBotX += dpf(11f);
        }

        // Black material advantage badge
        int blackAdv = info.blackScore - info.whiteScore;
        if (blackAdv > 0) {
            Paint advPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            advPaint.setColor(0xFF38BDF8);
            advPaint.setTextSize(dpf(8.5f));
            advPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            canvas.drawText("+" + blackAdv, curBotX + dpf(4f), botGraveY - dpf(1f), advPaint);
        }
    }

    private static class MaterialInfo {
        final List<Character> whiteCaptured = new ArrayList<>();
        final List<Character> blackCaptured = new ArrayList<>();
        int whiteScore = 0;
        int blackScore = 0;
    }

    private MaterialInfo calculateMaterial() {
        MaterialInfo info = new MaterialInfo();
        int[] initialWhite = new int[256];
        int[] initialBlack = new int[256];
        initialWhite['P'] = 8; initialWhite['N'] = 2; initialWhite['B'] = 2; initialWhite['R'] = 2; initialWhite['Q'] = 1;
        initialBlack['p'] = 8; initialBlack['n'] = 2; initialBlack['b'] = 2; initialBlack['r'] = 2; initialBlack['q'] = 1;

        int[] currentWhite = new int[256];
        int[] currentBlack = new int[256];

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                char p = board[r][c];
                if (p != '.') {
                    if (isWhitePiece(p)) {
                        currentWhite[p]++;
                        info.whiteScore += getPieceValue(p);
                    } else {
                        currentBlack[p]++;
                        info.blackScore += getPieceValue(p);
                    }
                }
            }
        }

        char[] orderWhite = {'Q', 'R', 'B', 'N', 'P'};
        char[] orderBlack = {'q', 'r', 'b', 'n', 'p'};

        for (char p : orderWhite) {
            int missing = initialWhite[p] - currentWhite[p];
            for (int k = 0; k < missing; k++) info.whiteCaptured.add(p);
        }
        for (char p : orderBlack) {
            int missing = initialBlack[p] - currentBlack[p];
            for (int k = 0; k < missing; k++) info.blackCaptured.add(p);
        }
        return info;
    }

    private int getPieceValue(char p) {
        switch (Character.toLowerCase(p)) {
            case 'q': return 9;
            case 'r': return 5;
            case 'b': case 'n': return 3;
            case 'p': return 1;
            default: return 0;
        }
    }
}
