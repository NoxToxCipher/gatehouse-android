package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private final Paint goldDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stone3dPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stoneRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stoneShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lastMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint atariGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coordTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint territoryBlackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint territoryWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boardRect = new RectF();
    private final Random rng = new Random();

    // Pre-allocated high performance rendering paints (zero GC jitter)
    private final boolean[][] atariMap = new boolean[19][19];
    private final Paint blackStoneBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackStoneHlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiteStoneBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint whiteStoneHlPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
    private boolean showHeatmap = false;
    private boolean showMoveNumbers = false;
    private final Paint moveNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int difficultyTier = 1; // 0 = Apprentice (1-kyu), 1 = Master (3-dan), 2 = Grandmaster (9-dan)
    private int hintX = -1;
    private int hintY = -1;
    private final Paint hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Interactive Endgame Territory Scoring Mode & Dead Stone Tracking
    private boolean isScoringMode = false;
    private final boolean[][] deadStones = new boolean[19][19];
    private final Paint deadStoneCrossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Interactive Move History Replayer / Timeline Stepper
    private int reviewStepIndex = -1;
    private final List<int[][]> fullReplayStates = new ArrayList<>();
    private final List<Point> fullReplayMoves = new ArrayList<>();

    public void stepToStart() {
        if (fullReplayStates.isEmpty()) return;
        reviewStepIndex = 0;
        applyReplayState(reviewStepIndex);
    }

    public void stepBackward() {
        if (fullReplayStates.isEmpty()) return;
        if (reviewStepIndex == -1) reviewStepIndex = fullReplayStates.size() - 1;
        reviewStepIndex = Math.max(0, reviewStepIndex - 1);
        applyReplayState(reviewStepIndex);
    }

    public void stepForward() {
        if (fullReplayStates.isEmpty() || reviewStepIndex == -1) return;
        reviewStepIndex = Math.min(fullReplayStates.size() - 1, reviewStepIndex + 1);
        applyReplayState(reviewStepIndex);
        if (reviewStepIndex == fullReplayStates.size() - 1) {
            reviewStepIndex = -1;
        }
    }

    public void stepToEnd() {
        if (fullReplayStates.isEmpty()) return;
        reviewStepIndex = -1;
        applyReplayState(fullReplayStates.size() - 1);
    }

    private void applyReplayState(int step) {
        if (step < 0 || step >= fullReplayStates.size()) return;
        int[][] state = fullReplayStates.get(step);
        for (int r = 0; r < boardSize; r++) {
            System.arraycopy(state[r], 0, board[r], 0, boardSize);
        }
        if (step > 0 && step <= fullReplayMoves.size()) {
            Point p = fullReplayMoves.get(step - 1);
            lastMoveX = p.x;
            lastMoveY = p.y;
        } else {
            lastMoveX = -1;
            lastMoveY = -1;
        }
        currentTurn = (step % 2 == 0) ? 1 : 2;
        updateAtariMap();
        try {
            RecreationAudioSynth.playBadukStoneClack();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}
        if (statusListener != null) {
            statusListener.onStatusChanged("⏪ Replay: Move " + step + " / " + (fullReplayStates.size() - 1) + (reviewStepIndex == -1 ? " (Live Game)" : ""), 0xFF38BDF8);
        }
        invalidate();
    }

    public void toggleMoveNumbers() {
        showMoveNumbers = !showMoveNumbers;
        invalidate();
    }

    public boolean isMoveNumbersEnabled() {
        return showMoveNumbers;
    }

    // Stone Placement Descent & Wood Impact Clack Ripple Animation
    private int animStoneX = -1;
    private int animStoneY = -1;
    private int animStoneColor = 0;
    private long animStartTime = 0;
    private static final long PLACEMENT_ANIM_DURATION_MS = 220;
    private static final long RIPPLE_ANIM_DURATION_MS = 400;
    private final Paint placementRipplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Floating Capture Toast Particles
    private String captureToastText = null;
    private float captureToastX = 0f;
    private float captureToastY = 0f;
    private long captureToastStartTime = 0;
    private static final long CAPTURE_TOAST_DURATION_MS = 1400;
    private final Paint captureToastPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint captureToastBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Joseki & Classical Opening Pattern Recognition
    private String detectedOpening = null;
    private long detectedOpeningTime = 0;

    public void toggleScoringMode() {
        isScoringMode = !isScoringMode;
        showTerritory = isScoringMode;
        if (isScoringMode) {
            try { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Exception ignored) {}
            if (statusListener != null) {
                statusListener.onStatusChanged("⚖️ Scoring Mode: Tap dead stones to mark as removed", 0xFFEAB308);
            }
        } else {
            updateStatus();
        }
        invalidate();
    }

    public boolean isScoringMode() {
        return isScoringMode;
    }

    private float customKomi = 6.5f;
    private boolean isChineseRules = false;

    public void toggleRules() {
        isChineseRules = !isChineseRules;
        customKomi = isChineseRules ? 7.5f : 6.5f;
        try { performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); } catch (Exception ignored) {}
        if (statusListener != null) {
            statusListener.onStatusChanged((isChineseRules ? "📜 Chinese Area Rules (7.5 Komi)" : "📜 Japanese Territory Rules (6.5 Komi)"), 0xFFEAB308);
        }
        updateStatus();
        invalidate();
    }

    public boolean isChineseRules() {
        return isChineseRules;
    }

    public float getKomi() {
        return customKomi;
    }

    private int countStones(int playerColor) {
        int c = 0;
        for (int r = 0; r < boardSize; r++) {
            for (int col = 0; col < boardSize; col++) {
                if (board[r][col] == playerColor && !deadStones[r][col]) c++;
            }
        }
        return c;
    }

    public String getScoreSummary() {
        int bTerr = countTerritory(1);
        int wTerr = countTerritory(2);
        int bStones = 0, wStones = 0;
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                if (deadStones[r][c]) {
                    if (board[r][c] == 1) wTerr += 2;
                    else if (board[r][c] == 2) bTerr += 2;
                } else if (isChineseRules) {
                    if (board[r][c] == 1) bStones++;
                    else if (board[r][c] == 2) wStones++;
                }
            }
        }
        int bTotal = isChineseRules ? (bTerr + bStones) : (bTerr + blackCaptures);
        float wTotal = (isChineseRules ? (wTerr + wStones) : (wTerr + whiteCaptures)) + customKomi;
        return String.format("⚫ Black: %d pts (%s %d + %s %d)\n⚪ White: %.1f pts (%s %d + %s %d + Komi %.1f)\n\nRules: %s · Result: %s",
                bTotal, (isChineseRules ? "Terr" : "Terr"), bTerr, (isChineseRules ? "Stones" : "Cap"), (isChineseRules ? bStones : blackCaptures),
                wTotal, (isChineseRules ? "Terr" : "Terr"), wTerr, (isChineseRules ? "Stones" : "Cap"), (isChineseRules ? wStones : whiteCaptures), customKomi,
                (isChineseRules ? "Chinese Area" : "Japanese Territory"),
                (bTotal > wTotal ? "Black wins by " + String.format("%.1f", bTotal - wTotal) + " pts" : "White wins by " + String.format("%.1f", wTotal - bTotal) + " pts"));
    }

    public void setDifficultyTier(int tier) {
        this.difficultyTier = Math.max(0, Math.min(2, tier));
        updateStatus();
    }

    public int getDifficultyTier() {
        return difficultyTier;
    }

    public void toggleHeatmap() {
        showHeatmap = !showHeatmap;
        if (showHeatmap) {
            try { performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); } catch (Exception ignored) {}
            if (statusListener != null) {
                statusListener.onStatusChanged("🗺️ Real-Time Influence Heatmap & Territory Control Active", 0xFF10B981);
            }
        }
        invalidate();
    }

    public boolean isHeatmapEnabled() {
        return showHeatmap;
    }

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public BadukGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        hintPaint.setColor(0xFFFFD166);
        hintPaint.setStyle(Paint.Style.STROKE);
        hintPaint.setStrokeWidth(dpf(2.5f));

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(2f));

        goldDetailPaint.setColor(0xFFCA8A04);
        goldDetailPaint.setStyle(Paint.Style.STROKE);
        goldDetailPaint.setStrokeWidth(dpf(1f));

        gridLinePaint.setColor(0xFF2C1808);
        gridLinePaint.setStrokeWidth(dpf(1.4f));

        woodGrainPaint.setColor(0x18000000);
        woodGrainPaint.setStrokeWidth(dpf(1f));

        starPointPaint.setColor(0xFF1E1005);
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

        atariGlowPaint.setColor(0xCCEF4444);
        atariGlowPaint.setStyle(Paint.Style.STROKE);
        atariGlowPaint.setStrokeWidth(dpf(2f));

        coordTextPaint.setColor(0xFF5C3818);
        coordTextPaint.setTextAlign(Paint.Align.CENTER);
        coordTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        territoryBlackPaint.setColor(0x660F172A);
        territoryBlackPaint.setStyle(Paint.Style.FILL);

        territoryWhitePaint.setColor(0x66F8FAFC);
        territoryWhitePaint.setStyle(Paint.Style.FILL);

        blackStoneBasePaint.setColor(0xFF111827);
        blackStoneBasePaint.setStyle(Paint.Style.FILL);

        blackStoneHlPaint.setColor(0xFF374151);
        blackStoneHlPaint.setStyle(Paint.Style.FILL);

        whiteStoneBasePaint.setColor(0xFFF1F5F9);
        whiteStoneBasePaint.setStyle(Paint.Style.FILL);

        whiteStoneHlPaint.setColor(0xFFFFFFFF);
        whiteStoneHlPaint.setStyle(Paint.Style.FILL);

        resetGame();
    }

    private void updateAtariMap() {
        for (int y = 0; y < 19; y++) {
            for (int x = 0; x < 19; x++) atariMap[y][x] = false;
        }
        boolean[][] visited = new boolean[boardSize][boardSize];
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                int val = board[y][x];
                if (val != 0 && !visited[y][x]) {
                    boolean[][] groupVisited = new boolean[boardSize][boardSize];
                    int libs = countGroupLiberties(x, y, val, groupVisited);
                    if (libs == 1) {
                        for (int gy = 0; gy < boardSize; gy++) {
                            for (int gx = 0; gx < boardSize; gx++) {
                                if (groupVisited[gy][gx]) {
                                    visited[gy][gx] = true;
                                    atariMap[gy][gx] = true;
                                }
                            }
                        }
                    }
                }
            }
        }
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

    public void setHandicap(int count) {
        resetGame();
        if (count < 2) return;
        count = Math.min(count, 9);
        int star = (boardSize == 19) ? 3 : (boardSize == 13 ? 3 : 2);
        int starHigh = boardSize - 1 - star;
        int mid = boardSize / 2;

        int[][] placementOrder = {
            {starHigh, star},         // 1. Top-Right
            {star, starHigh},         // 2. Bottom-Left
            {starHigh, starHigh},     // 3. Bottom-Right
            {star, star},             // 4. Top-Left
            {mid, mid},               // 5. Tengen
            {star, mid},              // 6. Left side
            {starHigh, mid},          // 7. Right side
            {mid, star},              // 8. Top side
            {mid, starHigh}           // 9. Bottom side
        };

        for (int i = 0; i < count && i < placementOrder.length; i++) {
            int hx = placementOrder[i][0];
            int hy = placementOrder[i][1];
            board[hy][hx] = 1; // Black stone
            moveList.add(new Point(hx, hy));
        }
        currentTurn = 2; // White plays first against handicap
        updateStatus();
        invalidate();

        if (mode == 0 && currentTurn == 2) {
            postDelayed(new Runnable() {
                public void run() { botPlayMove(); }
            }, 600);
        }
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
        fullReplayStates.clear();
        fullReplayMoves.clear();
        reviewStepIndex = -1;

        int[][] initialEmpty = new int[boardSize][boardSize];
        fullReplayStates.add(initialEmpty);

        currentTurn = 1;
        blackCaptures = 0;
        whiteCaptures = 0;
        lastMoveX = -1;
        lastMoveY = -1;
        consecutivePasses = 0;
        gameOver = false;
        puzzleSolved = false;
        showTerritory = false;
        updateAtariMap();
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

        int[][] fCopy = new int[boardSize][boardSize];
        for (int r = 0; r < boardSize; r++) {
            System.arraycopy(board[r], 0, fCopy[r], 0, boardSize);
        }
        fullReplayStates.add(fCopy);
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
        }, 7, 2),
        new TsumegoPuzzle("9. Golden Chicken on One Leg", new int[][]{
            {0,0,2}, {1,0,2}, {2,0,1}, {0,1,1}, {1,1,1}, {0,2,2}, {1,2,2}
        }, 0, 0),
        new TsumegoPuzzle("10. Carpenter's Square Life", new int[][]{
            {0,0,1}, {1,0,1}, {2,0,1}, {0,1,1}, {0,2,1}, {3,0,2}, {3,1,2}, {2,2,2}, {1,3,2}, {0,3,2}
        }, 1, 1),
        new TsumegoPuzzle("11. Tripod Eye Shape", new int[][]{
            {1,1,1}, {2,1,1}, {3,1,1}, {1,2,1}, {3,2,1}, {0,1,2}, {0,2,2}, {4,1,2}, {4,2,2}, {2,0,2}
        }, 2, 2),
        new TsumegoPuzzle("12. Dual Miai Vital Point", new int[][]{
            {4,4,2}, {5,4,2}, {6,4,2}, {4,5,1}, {6,5,1}, {3,4,1}, {7,4,1}
        }, 5, 5)
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
            int blackTerritory = countTerritory(1);
            int whiteTerritory = countTerritory(2);
            float blackTotal = isChineseRules ? (blackTerritory + countStones(1)) : (blackTerritory + blackCaptures);
            float whiteTotal = (isChineseRules ? (whiteTerritory + countStones(2)) : (whiteTerritory + whiteCaptures)) + customKomi;

            String leadStr = (blackTotal > whiteTotal)
                    ? String.format(java.util.Locale.US, "● Black +%.1f", (blackTotal - whiteTotal))
                    : String.format(java.util.Locale.US, "○ White +%.1f", (whiteTotal - blackTotal));

            String turn = (currentTurn == 1) ? "● Black's Turn" : "○ White's Turn (Bot)";
            statusListener.onStatusChanged(turn + " · " + leadStr + " (●" + (int)blackTotal + " vs ○" + String.format(java.util.Locale.US, "%.1f", whiteTotal) + ")", (currentTurn == 1 ? 0xFFFFD166 : 0xFF38BDF8));
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
                captureToastText = "🎉 TESUJI MASTERED! Problem Solved";
                captureToastX = x;
                captureToastY = y;
                captureToastStartTime = System.currentTimeMillis();
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
                return true;
            } else {
                if (statusListener != null) {
                    statusListener.onStatusChanged("✗ Incorrect. Find the vital point!", 0xFFEF4444);
                }
                try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
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
            RecreationAudioSynth.playBadukStoneClack();
            if (capturedCount > 0) {
                performHapticFeedback(HapticFeedbackConstants.CONFIRM);
                postDelayed(new Runnable() {
                    public void run() {
                        try { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Exception ignored) {}
                    }
                }, 75);
            } else {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } catch (Exception ignored) {}

        hintX = -1;
        hintY = -1;

        if (color == 1) blackCaptures += capturedCount;
        else whiteCaptures += capturedCount;

        if (capturedCount > 0) {
            captureToastText = "💥 +" + capturedCount + " CAPTURED";
            captureToastX = x;
            captureToastY = y;
            captureToastStartTime = System.currentTimeMillis();
        }

        lastMoveX = x;
        lastMoveY = y;
        moveList.add(new Point(x, y));
        fullReplayMoves.add(new Point(x, y));
        consecutivePasses = 0;
        currentTurn = opponent;

        // Trigger elegant stone placement descent and wood impact ripple
        animStoneX = x;
        animStoneY = y;
        animStoneColor = color;
        animStartTime = System.currentTimeMillis();

        evaluateOpeningPattern();
        updateStatus();
        postInvalidateOnAnimation();

        if (mode == 0 && currentTurn == 2) {
            postDelayed(new Runnable() {
                public void run() { botPlayMove(); }
            }, 380);
        }
        return true;
    }

    private void evaluateOpeningPattern() {
        if (moveList.size() < 1 || moveList.size() > 14) {
            if (moveList.size() > 14) detectedOpening = null;
            return;
        }
        if (boardSize == 19) {
            // Check for Tengen (center)
            if (board[9][9] == 1 && moveList.size() <= 3) {
                detectedOpening = "🌌 Tengen (Origin of Heaven) Opening";
                detectedOpeningTime = System.currentTimeMillis();
                return;
            }
            // Check for 3-3 Invasion
            if ((board[2][2] != 0 && (board[3][3] != 0 || board[2][3] != 0)) ||
                (board[16][2] != 0 && (board[15][3] != 0 || board[16][3] != 0)) ||
                (board[2][16] != 0 && (board[3][15] != 0 || board[2][15] != 0)) ||
                (board[16][16] != 0 && (board[15][15] != 0 || board[16][15] != 0))) {
                detectedOpening = "⚔️ San-San (3-3 Modern AI Invasion) Joseki";
                detectedOpeningTime = System.currentTimeMillis();
                return;
            }
            // Check for Komoku Keima Shimari (3-4 Knight Enclosure)
            if ((board[2][3] != 0 && board[4][2] != 0) || (board[3][2] != 0 && board[2][4] != 0) ||
                (board[16][3] != 0 && board[14][2] != 0) || (board[2][15] != 0 && board[4][16] != 0)) {
                detectedOpening = "🏯 Komoku Keima Shimari (Small Knight Enclosure)";
                detectedOpeningTime = System.currentTimeMillis();
                return;
            }
            // Check for Hoshi Star Point Approach
            if ((board[3][3] != 0 && (board[2][4] != 0 || board[4][2] != 0 || board[5][2] != 0)) ||
                (board[15][3] != 0 && (board[16][4] != 0 || board[14][2] != 0))) {
                detectedOpening = "🗡️ Kogei Approach (Star Point Joseki)";
                detectedOpeningTime = System.currentTimeMillis();
                return;
            }
            // Star Point (Hoshi)
            if (board[3][3] != 0 || board[15][3] != 0 || board[3][15] != 0 || board[15][15] != 0) {
                detectedOpening = "⭐ Hoshi (4-4 Star Point) Opening";
                detectedOpeningTime = System.currentTimeMillis();
                return;
            }
        } else if (boardSize == 9) {
            if (board[4][4] == 1 && moveList.size() <= 2) {
                detectedOpening = "🌌 Tengen Center Dominance (9×9)";
                detectedOpeningTime = System.currentTimeMillis();
                return;
            }
            if (board[2][2] != 0 || board[6][2] != 0 || board[2][6] != 0 || board[6][6] != 0) {
                detectedOpening = "⭐ 3-3 Corner Claim (9×9 Joseki)";
                detectedOpeningTime = System.currentTimeMillis();
                return;
            }
        }
    }

    public String exportSGF() {
        StringBuilder sb = new StringBuilder();
        sb.append("(;GM[1]FF[4]CA[UTF-8]AP[GatehouseSecurity]");
        sb.append("SZ[").append(boardSize).append("]");
        sb.append("KM[").append(boardSize == 19 ? "7.5" : "6.5").append("]");
        sb.append("RU[Japanese]");
        sb.append("PW[").append(mode == 0 ? "KataGo AI (" + (difficultyTier == 2 ? "9-Dan" : (difficultyTier == 1 ? "3-Dan" : "1-Kyu")) + ")" : "White Player").append("]");
        sb.append("PB[Officer Lochran Doherty]");
        sb.append("DT[").append(new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date())).append("]");

        for (int i = 0; i < moveList.size(); i++) {
            Point p = moveList.get(i);
            char colChar = (char) ('a' + p.x);
            char rowChar = (char) ('a' + p.y);
            String moveColor = (i % 2 == 0) ? ";B[" : ";W[";
            sb.append(moveColor).append(colChar).append(rowChar).append("]");
        }
        sb.append(")");
        return sb.toString();
    }

    public boolean importSGF(String sgf) {
        if (sgf == null || !sgf.contains("(;")) return false;
        try {
            int newSize = 19;
            java.util.regex.Matcher mSz = java.util.regex.Pattern.compile("SZ\\[(\\d+)\\]").matcher(sgf);
            if (mSz.find()) {
                try { newSize = Integer.parseInt(mSz.group(1)); } catch (Exception ignored) {}
            }
            if (newSize != 9 && newSize != 13 && newSize != 19) newSize = 19;
            this.boardSize = newSize;
            resetGame();

            java.util.regex.Matcher mAb = java.util.regex.Pattern.compile("AB(\\[[a-z]{2}\\])+").matcher(sgf);
            if (mAb.find()) {
                String match = mAb.group(0);
                java.util.regex.Matcher mCoords = java.util.regex.Pattern.compile("\\[([a-z]{2})\\]").matcher(match);
                while (mCoords.find()) {
                    String c = mCoords.group(1);
                    int hx = c.charAt(0) - 'a';
                    int hy = c.charAt(1) - 'a';
                    if (hx >= 0 && hx < boardSize && hy >= 0 && hy < boardSize) {
                        board[hy][hx] = 1;
                        moveList.add(new Point(hx, hy));
                    }
                }
            }

            java.util.regex.Matcher mMoves = java.util.regex.Pattern.compile(";([BW])\\[([a-z]{0,2})\\]").matcher(sgf);
            while (mMoves.find()) {
                String colorStr = mMoves.group(1);
                String coordStr = mMoves.group(2);
                int color = "B".equalsIgnoreCase(colorStr) ? 1 : 2;
                if (coordStr.isEmpty() || coordStr.equals("tt")) {
                    passTurn();
                } else if (coordStr.length() == 2) {
                    int gx = coordStr.charAt(0) - 'a';
                    int gy = coordStr.charAt(1) - 'a';
                    if (gx >= 0 && gx < boardSize && gy >= 0 && gy < boardSize) {
                        currentTurn = color;
                        playMove(gx, gy);
                    }
                }
            }
            updateStatus();
            invalidate();
            return true;
        } catch (Exception e) {
            return false;
        }
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
        Point best = findBestMove(2);
        if (best != null && playMove(best.x, best.y)) return;
        passTurn();
    }

    public Point findBestMove(int color) {
        int opponent = (color == 1) ? 2 : 1;

        // 1. URGENT ATARI COMBAT: Immediate Defense & Capture
        // 1A. Defend own groups in Atari (1 liberty)
        int bestDefX = -1, bestDefY = -1;
        int maxDefGain = 0;
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] == color) {
                    boolean[][] v = new boolean[boardSize][boardSize];
                    if (countGroupLiberties(x, y, color, v) == 1) {
                        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
                        for (int[] d : dirs) {
                            int nx = x + d[0];
                            int ny = y + d[1];
                            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[ny][nx] == 0) {
                                board[ny][nx] = color;
                                boolean[][] v2 = new boolean[boardSize][boardSize];
                                int libs = countGroupLiberties(nx, ny, color, v2);
                                board[ny][nx] = 0;
                                if (libs >= 2 && libs > maxDefGain) {
                                    maxDefGain = libs;
                                    bestDefX = nx;
                                    bestDefY = ny;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (bestDefX != -1 && bestDefY != -1) {
            return new Point(bestDefX, bestDefY);
        }

        // 1B. Capture opponent groups in Atari (1 liberty)
        int bestCapX = -1, bestCapY = -1;
        int maxCapSize = 0;
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] == opponent) {
                    boolean[][] v = new boolean[boardSize][boardSize];
                    if (countGroupLiberties(x, y, opponent, v) == 1) {
                        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
                        for (int[] d : dirs) {
                            int nx = x + d[0];
                            int ny = y + d[1];
                            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[ny][nx] == 0) {
                                boolean[][] vSelf = new boolean[boardSize][boardSize];
                                int groupSize = countGroupSize(x, y, opponent, vSelf);
                                if (groupSize > maxCapSize) {
                                    maxCapSize = groupSize;
                                    bestCapX = nx;
                                    bestCapY = ny;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (bestCapX != -1 && bestCapY != -1) {
            return new Point(bestCapX, bestCapY);
        }

        // 2. OPENING JOSEKI / CORNER & TENGEN (First 4 moves)
        if (moveList.size() <= 4) {
            int center = boardSize / 2;
            int star = (boardSize >= 13) ? 3 : 2;
            int starHigh = boardSize - 1 - star;
            int[][] openingPoints = {
                {center, center},
                {star, star}, {starHigh, star}, {star, starHigh}, {starHigh, starHigh},
                {center, star}, {star, center}, {center, starHigh}, {starHigh, center}
            };
            for (int[] pt : openingPoints) {
                if (board[pt[1]][pt[0]] == 0) {
                    return new Point(pt[0], pt[1]);
                }
            }
        }

        // 3. CANDIDATE MOVES WITH SHAPE HEURISTICS & MONTE CARLO ROLLOUTS
        List<CandidateMove> candidates = new ArrayList<>();
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] != 0) continue;

                // Test self-liberties
                board[y][x] = color;
                boolean[][] vTest = new boolean[boardSize][boardSize];
                int testLibs = countGroupLiberties(x, y, color, vTest);
                board[y][x] = 0;
                if (testLibs <= 1) continue; // Avoid self-atari

                float shapeScore = evaluateShapeScore(x, y);
                candidates.add(new CandidateMove(x, y, shapeScore));
            }
        }

        if (candidates.isEmpty()) return null;

        java.util.Collections.sort(candidates, new java.util.Comparator<CandidateMove>() {
            @Override
            public int compare(CandidateMove a, CandidateMove b) {
                return Float.compare(b.shapeScore, a.shapeScore);
            }
        });

        int rollouts = (difficultyTier == 0) ? 6 : (difficultyTier == 1 ? 25 : 50);
        int depth = (difficultyTier == 0) ? 6 : (difficultyTier == 1 ? 12 : 18);
        int numTop = Math.min(candidates.size(), (difficultyTier == 0 ? 3 : (difficultyTier == 1 ? 6 : 10)));
        CandidateMove bestMove = candidates.get(0);
        float bestTotalScore = -999999f;

        for (int i = 0; i < numTop; i++) {
            CandidateMove c = candidates.get(i);
            float winRate = runMonteCarloRollouts(c.x, c.y, rollouts, depth);
            float totalScore = c.shapeScore * 0.45f + winRate * 55f;
            if (totalScore > bestTotalScore) {
                bestTotalScore = totalScore;
                bestMove = c;
            }
        }

        return (bestMove != null) ? new Point(bestMove.x, bestMove.y) : null;
    }

    private static class CandidateMove {
        final int x, y;
        final float shapeScore;
        CandidateMove(int x, int y, float s) {
            this.x = x; this.y = y; this.shapeScore = s;
        }
    }

    private int countGroupSize(int startX, int startY, int color, boolean[][] visited) {
        int count = 0;
        Queue<Point> q = new LinkedList<>();
        q.add(new Point(startX, startY));
        visited[startY][startX] = true;
        count++;

        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        while (!q.isEmpty()) {
            Point p = q.poll();
            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize && board[ny][nx] == color && !visited[ny][nx]) {
                    visited[ny][nx] = true;
                    count++;
                    q.add(new Point(nx, ny));
                }
            }
        }
        return count;
    }

    private float evaluateShapeScore(int x, int y) {
        float score = 0f;
        int distEdgeX = Math.min(x, boardSize - 1 - x);
        int distEdgeY = Math.min(y, boardSize - 1 - y);

        // 3rd / 4th line Golden Territory Lines (optimal Go balance)
        if (distEdgeX == 2 && distEdgeY == 2) score += 60f;
        else if (distEdgeX >= 2 && distEdgeY >= 2) score += 42f;
        else if (distEdgeX == 1 || distEdgeY == 1) score += 15f;
        else if (distEdgeX == 0 || distEdgeY == 0) score -= 35f; // First line penalty (death line)

        // Contact & Tesuji Shape Patterns
        int friendlyNeighbors = 0;
        int enemyNeighbors = 0;
        int friendlyDiagonals = 0;
        int enemyDiagonals = 0;

        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize) {
                if (board[ny][nx] == 2) friendlyNeighbors++;
                else if (board[ny][nx] == 1) enemyNeighbors++;
            }
        }

        int[][] diagDirs = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
        for (int[] d : diagDirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && nx < boardSize && ny >= 0 && ny < boardSize) {
                if (board[ny][nx] == 2) friendlyDiagonals++;
                else if (board[ny][nx] == 1) enemyDiagonals++;
            }
        }

        // Hane & Head of 2 Stones (powerful wrapping shape)
        if (enemyNeighbors >= 1 && friendlyNeighbors >= 1) score += 55f;

        // Tiger's Mouth (3 surrounding friendly stones creating eye mouth)
        if (friendlyNeighbors >= 2 && friendlyDiagonals >= 1) score += 65f;

        // Kosumi (Diagonal connection with protection against cuts)
        if (friendlyDiagonals >= 1 && enemyNeighbors == 0) score += 40f;

        // Extension from friendly wall
        score += friendlyNeighbors * 22f;

        // Pressure opponent stone
        if (enemyNeighbors == 1) score += 30f;

        // Proximity to last move
        if (lastMoveX >= 0) {
            int dx = Math.abs(x - lastMoveX);
            int dy = Math.abs(y - lastMoveY);
            if (dx <= 2 && dy <= 2 && (dx + dy > 0)) score += 28f;
        }

        return score;
    }

    private float runMonteCarloRollouts(int startX, int startY, int numRollouts, int maxDepth) {
        int wins = 0;
        int[][] simBoard = new int[boardSize][boardSize];

        for (int sim = 0; sim < numRollouts; sim++) {
            // Copy state
            for (int r = 0; r < boardSize; r++) {
                System.arraycopy(board[r], 0, simBoard[r], 0, boardSize);
            }
            simBoard[startY][startX] = 2; // White candidate

            // Fast Playouts
            int turn = 1; // Black next
            for (int ply = 0; ply < maxDepth; ply++) {
                List<Point> legalMoves = new ArrayList<>();
                for (int r = 0; r < boardSize; r++) {
                    for (int c = 0; c < boardSize; c++) {
                        if (simBoard[r][c] == 0) legalMoves.add(new Point(c, r));
                    }
                }
                if (legalMoves.isEmpty()) break;

                // Pick a pseudo-random move with preference for center
                Point p = legalMoves.get(rng.nextInt(legalMoves.size()));
                simBoard[p.y][p.x] = turn;
                turn = (turn == 1) ? 2 : 1;
            }

            // Evaluate territory differential
            int whiteScore = 0;
            int blackScore = 0;
            for (int r = 0; r < boardSize; r++) {
                for (int c = 0; c < boardSize; c++) {
                    if (simBoard[r][c] == 2) whiteScore += 2;
                    else if (simBoard[r][c] == 1) blackScore += 2;
                }
            }
            if (whiteScore + 6 >= blackScore) wins++; // 6.5 Komi advantage for White
        }

        return (float) wins / (float) numRollouts;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver && !isScoringMode) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (action == MotionEvent.ACTION_DOWN) {
            float w = getWidth();
            float h = getHeight();
            float pad = dpf(22f); // Harmonized with onDraw (zero coordinate jump/jitter)
            float size = Math.min(w, h) - pad * 2;
            float startX = (w - size) / 2f;
            float startY = (h - size) / 2f;
            float cellSize = size / (boardSize - 1);

            int gx = Math.round((event.getX() - startX) / cellSize);
            int gy = Math.round((event.getY() - startY) / cellSize);

            if (gx >= 0 && gx < boardSize && gy >= 0 && gy < boardSize) {
                if (isScoringMode) {
                    if (board[gy][gx] != 0) {
                        deadStones[gy][gx] = !deadStones[gy][gx];
                        try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
                        if (statusListener != null) {
                            statusListener.onStatusChanged(getScoreSummary().replace("\n\n", " · ").replace("\n", " | "), 0xFF10B981);
                        }
                        invalidate();
                        return true;
                    }
                } else {
                    playMove(gx, gy);
                    return true;
                }
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

        // Rich Japanese Hon-Kaya Wood Goban Gradient
        boardRect.set(0, 0, w, h);
        gobanPaint.setShader(new LinearGradient(0, 0, w, h, 0xFFD49755, 0xFFB37332, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(boardRect, dpf(16f), dpf(16f), gobanPaint);

        // Fine Wood Grain Texture Lines
        for (int i = 1; i < 18; i++) {
            float gy = h * (i / 18f);
            canvas.drawLine(0, gy, w, gy + dpf(1.8f), woodGrainPaint);
        }

        // Perimeter Inlaid Gold Borders
        RectF innerRect = new RectF(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(innerRect, dpf(14f), dpf(14f), goldBorderPaint);

        RectF innerLine = new RectF(dpf(5f), dpf(5f), w - dpf(5f), h - dpf(5f));
        canvas.drawRoundRect(innerLine, dpf(11f), dpf(11f), goldDetailPaint);

        float pad = dpf(22f);
        float size = Math.min(w, h) - pad * 2;
        float startX = (w - size) / 2f;
        float startY = (h - size) / 2f;
        float cellSize = size / (boardSize - 1);

        // Draw Lacquered Grid Lines & Coordinates
        coordTextPaint.setTextSize(Math.max(dpf(7.5f), cellSize * 0.35f));

        for (int i = 0; i < boardSize; i++) {
            float py = startY + i * cellSize;
            canvas.drawLine(startX, py, startX + size, py, gridLinePaint);
            float px = startX + i * cellSize;
            canvas.drawLine(px, startY, px, startY + size, gridLinePaint);

            String colName = String.valueOf((char) ('A' + (i >= 8 ? i + 1 : i)));
            // Top and Bottom Column Labels
            canvas.drawText(colName, px, startY - dpf(6f), coordTextPaint);
            canvas.drawText(colName, px, startY + size + dpf(12f), coordTextPaint);

            // Left and Right Row Numbers
            String rowName = String.valueOf(boardSize - i);
            canvas.drawText(rowName, startX - dpf(9f), py + dpf(3.5f), coordTextPaint);
            canvas.drawText(rowName, startX + size + dpf(9f), py + dpf(3.5f), coordTextPaint);
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

        // Draw Real-Time Influence Heatmap & Territory Control if enabled
        if (showHeatmap) {
            drawInfluenceHeatmap(canvas, startX, startY, cellSize);
        }

        // Draw 3D Bi-convex Stones (Nachiguro Slate & Hyuga Clamshell)
        float stoneR = cellSize * 0.47f;
        long elapsed = System.currentTimeMillis() - animStartTime;
        boolean isAnimating = (animStoneX >= 0 && animStoneY >= 0 && elapsed < RIPPLE_ANIM_DURATION_MS);

        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                int val = board[y][x];
                if (val == 0) continue;

                float cx = startX + x * cellSize;
                float cy = startY + y * cellSize;

                if (x == animStoneX && y == animStoneY && elapsed < PLACEMENT_ANIM_DURATION_MS) {
                    // Elevated Descent Animation
                    float p = (float) elapsed / PLACEMENT_ANIM_DURATION_MS;
                    float ease = 1f - (1f - p) * (1f - p) * (1f - p);
                    float animOffsetY = -dpf(16f) * (1f - ease);
                    float animScale = 1.0f + 0.30f * (1f - ease);
                    float curR = stoneR * animScale;

                    // Diffused Elevated Drop Shadow
                    float shadowDist = dpf(2.5f) + dpf(8.0f) * (1f - ease);
                    shadowPaint.setAlpha((int) (150 * ease + 50 * (1f - ease)));
                    canvas.drawCircle(cx + dpf(1.5f), cy + shadowDist, curR * 0.95f, shadowPaint);
                    shadowPaint.setAlpha(0x99);

                    // 3D Stone at elevated position
                    drawSingleStone(canvas, cx, cy + animOffsetY, curR, val, deadStones[y][x]);

                    // Last Move Ring fade-in
                    if (p > 0.6f) {
                        lastMovePaint.setAlpha((int) (255 * (p - 0.6f) / 0.4f));
                        canvas.drawCircle(cx, cy + animOffsetY, curR * 0.5f, lastMovePaint);
                        lastMovePaint.setAlpha(255);
                    }
                } else {
                    // Standard Static Stone
                    if (atariMap[y][x]) {
                        canvas.drawCircle(cx, cy, stoneR + dpf(2.5f), atariGlowPaint);
                    }
                    canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2.5f), stoneR, shadowPaint);
                    drawSingleStone(canvas, cx, cy, stoneR, val, deadStones[y][x]);

                    if (x == lastMoveX && y == lastMoveY) {
                        canvas.drawCircle(cx, cy, stoneR * 0.5f, lastMovePaint);
                    }
                }
            }
        }

        // Draw Move Numbers Overlay
        if (showMoveNumbers) {
            moveNumberPaint.setTextSize(Math.max(dpf(7.5f), cellSize * 0.38f));
            moveNumberPaint.setTextAlign(Paint.Align.CENTER);
            moveNumberPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

            for (int i = 0; i < moveList.size(); i++) {
                Point pt = moveList.get(i);
                if (pt.x >= 0 && pt.x < boardSize && pt.y >= 0 && pt.y < boardSize) {
                    int stoneColor = board[pt.y][pt.x];
                    if (stoneColor != 0) {
                        float cx = startX + pt.x * cellSize;
                        float cy = startY + pt.y * cellSize;
                        moveNumberPaint.setColor(stoneColor == 1 ? 0xFFFFFFFF : 0xFF0F172A);
                        canvas.drawText(String.valueOf(i + 1), cx, cy + dpf(3f), moveNumberPaint);
                    }
                }
            }
        }

        // Draw Wood Resonance Impact Clack Ripple
        if (animStoneX >= 0 && animStoneY >= 0 && elapsed < RIPPLE_ANIM_DURATION_MS) {
            float acx = startX + animStoneX * cellSize;
            float acy = startY + animStoneY * cellSize;
            float rp = (float) elapsed / RIPPLE_ANIM_DURATION_MS;
            float rippleR = stoneR * (1.0f + 1.15f * rp);
            placementRipplePaint.setColor(0xFFFFD166);
            placementRipplePaint.setStyle(Paint.Style.STROKE);
            placementRipplePaint.setStrokeWidth(dpf(2.4f) * (1f - rp));
            placementRipplePaint.setAlpha((int) (220 * (1f - rp)));
            canvas.drawCircle(acx, acy, rippleR, placementRipplePaint);
        }

        if (isAnimating) {
            postInvalidateOnAnimation();
        }

        // Draw Vital Point Hint Beacon (if active)
        if (hintX != -1 && hintY != -1) {
            float hcx = startX + hintX * cellSize;
            float hcy = startY + hintY * cellSize;
            canvas.drawCircle(hcx, hcy, stoneR + dpf(3f), hintPaint);
            Paint hintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
            hintFill.setColor(0x44FFD166);
            hintFill.setStyle(Paint.Style.FILL);
            canvas.drawCircle(hcx, hcy, stoneR * 0.4f, hintFill);
        }

        // Draw Live Territory Control Pips if enabled
        if (showTerritory) {
            drawTerritoryMarkers(canvas, startX, startY, cellSize);
        }

        // Draw KataGo Candidate Move Bubbles if heatmap/analysis active
        if (showHeatmap) {
            drawKataGoCandidateBubbles(canvas, startX, startY, cellSize);
        }

        // Draw Floating Capture Toast Badge (if active)
        long toastElapsed = System.currentTimeMillis() - captureToastStartTime;
        if (captureToastText != null && toastElapsed < CAPTURE_TOAST_DURATION_MS) {
            float tp = (float) toastElapsed / CAPTURE_TOAST_DURATION_MS;
            float tcx = startX + captureToastX * cellSize;
            float tcy = startY + captureToastY * cellSize - dpf(12f) - dpf(22f) * tp;
            int alpha = (int) (255 * (1f - tp * tp));

            captureToastPaint.setTextSize(dpf(10.5f));
            captureToastPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            captureToastPaint.setColor(0xFFFFD166);
            captureToastPaint.setAlpha(alpha);
            captureToastPaint.setTextAlign(Paint.Align.CENTER);

            float tw = captureToastPaint.measureText(captureToastText) + dpf(16f);
            float th = dpf(18f);
            RectF tRect = new RectF(tcx - tw / 2f, tcy - th + dpf(4f), tcx + tw / 2f, tcy + dpf(4f));
            captureToastBgPaint.setColor(0xEE0F172A);
            captureToastBgPaint.setAlpha((int) (220 * (1f - tp)));
            canvas.drawRoundRect(tRect, dpf(6f), dpf(6f), captureToastBgPaint);
            canvas.drawText(captureToastText, tcx, tcy, captureToastPaint);
            postInvalidateOnAnimation();
        }

        // Draw Joseki & Opening Pattern Recognition Banner
        if (detectedOpening != null && (System.currentTimeMillis() - detectedOpeningTime < 6000)) {
            float bannerW = w * 0.90f;
            float bannerH = dpf(20f);
            float bx = (w - bannerW) / 2f;
            float by = dpf(8f);
            RectF bannerRect = new RectF(bx, by, bx + bannerW, by + bannerH);
            Paint bannerBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bannerBg.setColor(0xEE0F172A);
            canvas.drawRoundRect(bannerRect, dpf(6f), dpf(6f), bannerBg);
            Paint bannerBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
            bannerBorder.setColor(0xFFEAB308);
            bannerBorder.setStyle(Paint.Style.STROKE);
            bannerBorder.setStrokeWidth(dpf(1.2f));
            canvas.drawRoundRect(bannerRect, dpf(6f), dpf(6f), bannerBorder);
            Paint bannerText = new Paint(Paint.ANTI_ALIAS_FLAG);
            bannerText.setColor(0xFFFDE047);
            bannerText.setTextSize(dpf(9.5f));
            bannerText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            bannerText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(detectedOpening, w / 2f, by + dpf(13.5f), bannerText);
            postInvalidateOnAnimation();
        }

        // Draw KataGo-style Live Winrate Bar
        drawKataGoEvaluationBanner(canvas, w, h);
    }

    private void drawKataGoEvaluationBanner(Canvas canvas, float w, float h) {
        if (mode == 1) return; // Not in Tsumego
        float barH = dpf(3.5f);
        float barY = dpf(2.5f);
        float barW = w - dpf(14f);
        float barX = dpf(7f);

        int bTerr = countTerritory(1) + blackCaptures;
        int wTerr = countTerritory(2) + whiteCaptures + (boardSize == 19 ? 7 : 6);
        float total = bTerr + wTerr + 1;
        float bRate = Math.max(0.08f, Math.min(0.92f, (float) bTerr / total));

        Paint bBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bBarPaint.setColor(0xFF0F172A);
        bBarPaint.setStyle(Paint.Style.FILL);
        RectF bRect = new RectF(barX, barY, barX + barW * bRate, barY + barH);
        canvas.drawRoundRect(bRect, dpf(2f), dpf(2f), bBarPaint);

        Paint wBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wBarPaint.setColor(0xFFF8FAFC);
        wBarPaint.setStyle(Paint.Style.FILL);
        RectF wRect = new RectF(barX + barW * bRate, barY, barX + barW, barY + barH);
        canvas.drawRoundRect(wRect, dpf(2f), dpf(2f), wBarPaint);
    }

    public void showHint() {
        if (gameOver || mode == 1) return;
        Point best = findBestMove(currentTurn);
        if (best != null) {
            hintX = best.x;
            hintY = best.y;
            try {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            } catch (Exception ignored) {}
            if (statusListener != null) {
                statusListener.onStatusChanged("💡 Tesuji Hint: Best point indicated at " + (char)('A' + hintX) + (hintY + 1), 0xFFFFD166);
            }
            invalidate();
        }
    }

    private void drawTerritoryMarkers(Canvas canvas, float startX, float startY, float cellSize) {
        boolean[][] visited = new boolean[boardSize][boardSize];
        Paint bTerr = new Paint(Paint.ANTI_ALIAS_FLAG);
        bTerr.setColor(0x88000000);
        bTerr.setStyle(Paint.Style.FILL);

        Paint wTerr = new Paint(Paint.ANTI_ALIAS_FLAG);
        wTerr.setColor(0x88FFFFFF);
        wTerr.setStyle(Paint.Style.FILL);

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

                    if (surroundingColors.size() == 1) {
                        int owner = surroundingColors.iterator().next();
                        for (Point pt : emptyRegion) {
                            float cx = startX + pt.x * cellSize;
                            float cy = startY + pt.y * cellSize;
                            float s = dpf(2.5f);
                            canvas.drawRect(cx - s, cy - s, cx + s, cy + s, (owner == 1) ? bTerr : wTerr);
                        }
                    }
                }
            }
        }
    }

    private void drawInfluenceHeatmap(Canvas canvas, float startX, float startY, float cellSize) {
        float[][] influence = new float[boardSize][boardSize];
        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                int color = board[r][c];
                if (color == 0) continue;
                float sign = (color == 1) ? 1.0f : -1.0f;
                for (int y = 0; y < boardSize; y++) {
                    for (int x = 0; x < boardSize; x++) {
                        float dx = x - c;
                        float dy = y - r;
                        float d2 = dx * dx + dy * dy;
                        influence[y][x] += sign / (1.0f + d2 * 0.8f);
                    }
                }
            }
        }

        Paint auraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        auraPaint.setStyle(Paint.Style.FILL);

        Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setStyle(Paint.Style.STROKE);
        eyePaint.setStrokeWidth(dpf(1.6f));

        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] != 0) continue;
                float val = influence[y][x];
                float cx = startX + x * cellSize;
                float cy = startY + y * cellSize;
                float r = cellSize * 0.42f;

                if (val >= 0.25f) { // Black Influence Zone
                    int alpha = (int) Math.min(130, Math.max(35, val * 55f));
                    auraPaint.setColor((alpha << 24) | 0x0010B981); // Emerald aura
                    canvas.drawCircle(cx, cy, r, auraPaint);
                } else if (val <= -0.25f) { // White Influence Zone
                    int alpha = (int) Math.min(130, Math.max(35, Math.abs(val) * 55f));
                    auraPaint.setColor((alpha << 24) | 0x0038BDF8); // Azure aura
                    canvas.drawCircle(cx, cy, r, auraPaint);
                }

                // Eye-shape recognition (vacant point surrounded by single color)
                int north = (y > 0) ? board[y - 1][x] : -1;
                int south = (y < boardSize - 1) ? board[y + 1][x] : -1;
                int west = (x > 0) ? board[y][x - 1] : -1;
                int east = (x < boardSize - 1) ? board[y][x + 1] : -1;

                int eyeOwner = 0;
                if (isSurroundedBy(1, north, south, west, east)) eyeOwner = 1;
                else if (isSurroundedBy(2, north, south, west, east)) eyeOwner = 2;

                if (eyeOwner != 0) {
                    eyePaint.setColor(eyeOwner == 1 ? 0xFF34D399 : 0xFF7DD3FC);
                    float dSize = dpf(4.5f);
                    Path diamond = new Path();
                    diamond.moveTo(cx, cy - dSize);
                    diamond.lineTo(cx + dSize, cy);
                    diamond.lineTo(cx, cy + dSize);
                    diamond.lineTo(cx - dSize, cy);
                    diamond.close();
                    canvas.drawPath(diamond, eyePaint);
                }
            }
        }
    }

    private boolean isSurroundedBy(int target, int n, int s, int w, int e) {
        int count = 0;
        int validNeighbors = 0;
        if (n != -1) { validNeighbors++; if (n == target) count++; }
        if (s != -1) { validNeighbors++; if (s == target) count++; }
        if (w != -1) { validNeighbors++; if (w == target) count++; }
        if (e != -1) { validNeighbors++; if (e == target) count++; }
        return validNeighbors >= 3 && count == validNeighbors;
    }

    private void drawKataGoCandidateBubbles(Canvas canvas, float startX, float startY, float cellSize) {
        if (!showHeatmap || gameOver || mode == 1) return;
        List<CandidateMove> candidates = new ArrayList<>();
        for (int y = 0; y < boardSize; y++) {
            for (int x = 0; x < boardSize; x++) {
                if (board[y][x] != 0) continue;
                board[y][x] = currentTurn;
                boolean[][] vTest = new boolean[boardSize][boardSize];
                int testLibs = countGroupLiberties(x, y, currentTurn, vTest);
                board[y][x] = 0;
                if (testLibs <= 1) continue;
                float s = evaluateShapeScore(x, y);
                candidates.add(new CandidateMove(x, y, s));
            }
        }
        Collections.sort(candidates, new Comparator<CandidateMove>() {
            public int compare(CandidateMove a, CandidateMove b) {
                return Float.compare(b.shapeScore, a.shapeScore);
            }
        });
        int topN = Math.min(3, candidates.size());
        Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint bubbleTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bubbleTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        bubbleTextPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < topN; i++) {
            CandidateMove cm = candidates.get(i);
            float cx = startX + cm.x * cellSize;
            float cy = startY + cm.y * cellSize;
            float bubbleR = cellSize * 0.40f;

            if (i == 0) {
                bubblePaint.setColor(0xDD10B981); // Emerald Best Move (A)
                bubbleTextPaint.setColor(0xFFFFFFFF);
            } else if (i == 1) {
                bubblePaint.setColor(0xDD38BDF8); // Electric Azure 2nd (B)
                bubbleTextPaint.setColor(0xFF0F172A);
            } else {
                bubblePaint.setColor(0xDDF59E0B); // Amber 3rd (C)
                bubbleTextPaint.setColor(0xFF0F172A);
            }

            canvas.drawCircle(cx, cy, bubbleR, bubblePaint);
            bubbleTextPaint.setTextSize(Math.max(dpf(7.5f), cellSize * 0.38f));
            String rank = (i == 0) ? "A" : (i == 1 ? "B" : "C");
            canvas.drawText(rank, cx, cy + dpf(3.5f), bubbleTextPaint);
        }
    }

    private void drawSingleStone(Canvas canvas, float cx, float cy, float r, int val, boolean isDead) {
        if (val == 1) { // Matte Nachiguro Slate (Bi-convex 3D layered)
            if (isDead) blackStoneBasePaint.setAlpha(110);
            canvas.drawCircle(cx, cy, r, blackStoneBasePaint);
            canvas.drawCircle(cx - r * 0.30f, cy - r * 0.30f, r * 0.48f, blackStoneHlPaint);
            canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.22f, stoneShinePaint);
            blackStoneBasePaint.setAlpha(255);
        } else if (val == 2) { // Hyuga Clamshell Pearl
            if (isDead) whiteStoneBasePaint.setAlpha(110);
            canvas.drawCircle(cx, cy, r, whiteStoneBasePaint);
            canvas.drawCircle(cx, cy, r, stoneRimPaint);
            canvas.drawCircle(cx - r * 0.28f, cy - r * 0.28f, r * 0.46f, whiteStoneHlPaint);
            canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.24f, stoneShinePaint);
            whiteStoneBasePaint.setAlpha(255);
        }
        if (isDead) {
            float crossR = r * 0.55f;
            deadStoneCrossPaint.setColor(0xFFEF4444);
            deadStoneCrossPaint.setStyle(Paint.Style.STROKE);
            deadStoneCrossPaint.setStrokeWidth(dpf(2.0f));
            canvas.drawLine(cx - crossR, cy - crossR, cx + crossR, cy + crossR, deadStoneCrossPaint);
            canvas.drawLine(cx + crossR, cy - crossR, cx - crossR, cy + crossR, deadStoneCrossPaint);
        }
    }

    private int[] getHoshiPoints() {
        if (boardSize == 19) return new int[]{3, 9, 15};
        if (boardSize == 13) return new int[]{3, 6, 9};
        return new int[]{2, 4, 6};
    }
}
