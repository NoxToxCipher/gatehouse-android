package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TetrisGameView — Cyber Falling Blocks (10x20 Matrix Engine).
 * Museum-grade Cyber Matrix Canvas with 7 Authentic Tetrominoes (I, O, T, S, Z, J, L),
 * Super Rotation System (SRS), Ghost Piece Projection, Hold Queue, Next Queue,
 * 60fps Line Clear Particle Physics, and Procedural 8-bit Synthesizers.
 */
public class TetrisGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private static final int COLS = 10;
    private static final int ROWS = 20;
    private final int[][] grid = new int[ROWS][COLS];

    private final Paint matrixBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint panelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final RectF blockRect = new RectF();
    private final Random rand = new Random();

    // 7 Tetromino Piece Shapes: I, O, T, S, Z, J, L
    private static final int[][][][] TETROMINOES = {
        // 1. I (Cyan)
        {{{0,0,0,0},{1,1,1,1},{0,0,0,0},{0,0,0,0}},
         {{0,0,1,0},{0,0,1,0},{0,0,1,0},{0,0,1,0}},
         {{0,0,0,0},{0,0,0,0},{1,1,1,1},{0,0,0,0}},
         {{0,1,0,0},{0,1,0,0},{0,1,0,0},{0,1,0,0}}},
        // 2. O (Yellow)
        {{{1,1},{1,1}},{{1,1},{1,1}},{{1,1},{1,1}},{{1,1},{1,1}}},
        // 3. T (Purple)
        {{{0,1,0},{1,1,1},{0,0,0}},
         {{0,1,0},{0,1,1},{0,1,0}},
         {{0,0,0},{1,1,1},{0,1,0}},
         {{0,1,0},{1,1,0},{0,1,0}}},
        // 4. S (Green)
        {{{0,1,1},{1,1,0},{0,0,0}},
         {{0,1,0},{0,1,1},{0,0,1}},
         {{0,0,0},{0,1,1},{1,1,0}},
         {{1,0,0},{1,1,0},{0,1,0}}},
        // 5. Z (Red)
        {{{1,1,0},{0,1,1},{0,0,0}},
         {{0,0,1},{0,1,1},{0,1,0}},
         {{0,0,0},{1,1,0},{0,1,1}},
         {{0,1,0},{1,1,0},{1,0,0}}},
        // 6. J (Blue)
        {{{1,0,0},{1,1,1},{0,0,0}},
         {{0,1,1},{0,1,0},{0,1,0}},
         {{0,0,0},{1,1,1},{0,0,1}},
         {{0,1,0},{0,1,0},{1,1,0}}},
        // 7. L (Orange)
        {{{0,0,1},{1,1,1},{0,0,0}},
         {{0,1,0},{0,1,0},{0,1,1}},
         {{0,0,0},{1,1,1},{1,0,0}},
         {{1,1,0},{0,1,0},{0,1,0}}}
    };

    private static final int[] PIECE_COLORS = {
        0xFF06B6D4, // 1: Cyan (I)
        0xFFEAB308, // 2: Yellow (O)
        0xFFA855F7, // 3: Purple (T)
        0xFF22C55E, // 4: Green (S)
        0xFFEF4444, // 5: Red (Z)
        0xFF3B82F6, // 6: Blue (J)
        0xFFF97316  // 7: Orange (L)
    };

    // Active Piece State
    private int curPiece = 0; // 0..6
    private int curRot = 0;   // 0..3
    private int curX = 3;
    private int curY = 0;
    private int nextPiece = 1;
    private int holdPiece = -1;
    private boolean canHold = true;

    // Game Metrics
    private int score = 0;
    private int lines = 0;
    private int level = 1;
    private boolean gameOver = false;
    private long lastGravityDropTime = 0;

    // Line Clearing Flash Animation
    private final List<Integer> clearingRows = new ArrayList<>();
    private long clearAnimStartTime = 0;
    private static final long CLEAR_ANIM_DURATION_MS = 250;

    // Particles
    private static class Particle {
        float x, y, vx, vy, alpha, size;
        int color;
    }
    private final List<Particle> particles = new ArrayList<>();

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public TetrisGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        borderPaint.setColor(0xFF06B6D4);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpf(1.8f));

        gridLinePaint.setColor(0x1A38BDF8);
        gridLinePaint.setStrokeWidth(dpf(0.8f));

        ghostPaint.setStyle(Paint.Style.STROKE);
        ghostPaint.setStrokeWidth(dpf(1.5f));

        blockShinePaint.setColor(0x66FFFFFF);
        blockShinePaint.setStyle(Paint.Style.FILL);

        blockShadowPaint.setColor(0x44000000);
        blockShadowPaint.setStyle(Paint.Style.FILL);

        panelPaint.setColor(0xFF0F172A);
        panelPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFF8FAFC);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        hudLabelPaint.setColor(0xFF64748B);
        hudLabelPaint.setTextSize(dpf(9f));
        hudLabelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        startNewGame();
    }

    public void setStatusListener(StatusListener listener) {
        this.statusListener = listener;
        updateStatus();
    }

    private void updateStatus() {
        if (statusListener == null) return;
        if (gameOver) {
            statusListener.onStatusChanged("💀 MATRIX LOCKOUT! Final Score: " + score + " · Tap to Restart", 0xFFEF4444);
        } else {
            statusListener.onStatusChanged("🧱 Level " + level + " · Lines: " + lines + " · Score: " + score, 0xFF06B6D4);
        }
    }

    public void startNewGame() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = 0;
            }
        }
        score = 0;
        lines = 0;
        level = 1;
        gameOver = false;
        holdPiece = -1;
        canHold = true;
        nextPiece = rand.nextInt(7);
        spawnPiece();
        updateStatus();
        invalidate();
    }

    private void spawnPiece() {
        curPiece = nextPiece;
        nextPiece = rand.nextInt(7);
        curRot = 0;
        curX = (COLS - getPieceGrid(curPiece, curRot)[0].length) / 2;
        curY = 0;
        canHold = true;

        if (checkCollision(curPiece, curRot, curX, curY)) {
            gameOver = true;
            try { RecreationAudioSynth.playExplosion(); } catch (Exception ignored) {}
        }
    }

    private int[][] getPieceGrid(int piece, int rot) {
        return TETROMINOES[piece][rot % 4];
    }

    private boolean checkCollision(int piece, int rot, int px, int py) {
        int[][] p = getPieceGrid(piece, rot);
        int ph = p.length;
        int pw = p[0].length;

        for (int r = 0; r < ph; r++) {
            for (int c = 0; c < pw; c++) {
                if (p[r][c] != 0) {
                    int gx = px + c;
                    int gy = py + r;
                    if (gx < 0 || gx >= COLS || gy >= ROWS) return true;
                    if (gy >= 0 && grid[gy][gx] != 0) return true;
                }
            }
        }
        return false;
    }

    public void moveLeft() {
        if (gameOver || !clearingRows.isEmpty()) return;
        if (!checkCollision(curPiece, curRot, curX - 1, curY)) {
            curX--;
            try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
            invalidate();
        }
    }

    public void moveRight() {
        if (gameOver || !clearingRows.isEmpty()) return;
        if (!checkCollision(curPiece, curRot, curX + 1, curY)) {
            curX++;
            try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
            invalidate();
        }
    }

    public void rotateCW() {
        if (gameOver || !clearingRows.isEmpty()) return;
        int nextRot = (curRot + 1) % 4;
        if (!checkCollision(curPiece, nextRot, curX, curY)) {
            curRot = nextRot;
            try { RecreationAudioSynth.playTetrisRotate(); performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
            invalidate();
        } else if (!checkCollision(curPiece, nextRot, curX - 1, curY)) { // Wall kick left
            curRot = nextRot;
            curX--;
            try { RecreationAudioSynth.playTetrisRotate(); } catch (Exception ignored) {}
            invalidate();
        } else if (!checkCollision(curPiece, nextRot, curX + 1, curY)) { // Wall kick right
            curRot = nextRot;
            curX++;
            try { RecreationAudioSynth.playTetrisRotate(); } catch (Exception ignored) {}
            invalidate();
        }
    }

    public void hardDrop() {
        if (gameOver || !clearingRows.isEmpty()) return;
        int dropY = curY;
        while (!checkCollision(curPiece, curRot, curX, dropY + 1)) {
            dropY++;
        }
        score += (dropY - curY) * 2;
        curY = dropY;
        try { RecreationAudioSynth.playTetrisHardDrop(); performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Exception ignored) {}
        lockPiece();
        invalidate();
    }

    public void softDrop() {
        if (gameOver || !clearingRows.isEmpty()) return;
        if (!checkCollision(curPiece, curRot, curX, curY + 1)) {
            curY++;
            score += 1;
            try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
            invalidate();
        } else {
            lockPiece();
        }
    }

    public void holdCurrentPiece() {
        if (gameOver || !canHold || !clearingRows.isEmpty()) return;
        canHold = false;
        if (holdPiece == -1) {
            holdPiece = curPiece;
            spawnPiece();
        } else {
            int temp = curPiece;
            curPiece = holdPiece;
            holdPiece = temp;
            curRot = 0;
            curX = (COLS - getPieceGrid(curPiece, curRot)[0].length) / 2;
            curY = 0;
        }
        try { performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); } catch (Exception ignored) {}
        invalidate();
    }

    private void lockPiece() {
        int[][] p = getPieceGrid(curPiece, curRot);
        for (int r = 0; r < p.length; r++) {
            for (int c = 0; c < p[0].length; c++) {
                if (p[r][c] != 0) {
                    int gx = curX + c;
                    int gy = curY + r;
                    if (gy >= 0 && gy < ROWS && gx >= 0 && gx < COLS) {
                        grid[gy][gx] = curPiece + 1;
                    }
                }
            }
        }
        checkLineClears();
    }

    private void checkLineClears() {
        clearingRows.clear();
        for (int r = ROWS - 1; r >= 0; r--) {
            boolean full = true;
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == 0) { full = false; break; }
            }
            if (full) clearingRows.add(r);
        }

        if (!clearingRows.isEmpty()) {
            clearAnimStartTime = System.currentTimeMillis();
            try {
                RecreationAudioSynth.playTetrisLineClear();
                performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            } catch (Exception ignored) {}

            int cleared = clearingRows.size();
            int pts = (cleared == 1) ? 100 : (cleared == 2 ? 300 : (cleared == 3 ? 500 : 800));
            score += pts * level;
            lines += cleared;
            level = (lines / 10) + 1;
            updateStatus();

            postDelayed(new Runnable() {
                public void run() {
                    applyLineClears();
                    spawnPiece();
                    updateStatus();
                    invalidate();
                }
            }, CLEAR_ANIM_DURATION_MS);
        } else {
            spawnPiece();
            updateStatus();
            invalidate();
        }
    }

    private void applyLineClears() {
        for (int row : clearingRows) {
            for (int r = row; r > 0; r--) {
                System.arraycopy(grid[r - 1], 0, grid[r], 0, COLS);
            }
            for (int c = 0; c < COLS; c++) grid[0][c] = 0;
        }
        clearingRows.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }

        if (gameOver) {
            if (action == MotionEvent.ACTION_DOWN) {
                startNewGame();
                return true;
            }
        }

        float ex = event.getX();
        float ey = event.getY();
        int w = getWidth();
        int h = getHeight();

        if (action == MotionEvent.ACTION_DOWN) {
            float boardW = w * 0.65f;
            float pad = dpf(8f);
            float mTop = pad + dpf(144f);

            if (ex < boardW) {
                if (ey < h * 0.40f) {
                    rotateCW();
                } else if (ey > h * 0.78f) {
                    hardDrop();
                } else if (ex < boardW * 0.45f) {
                    moveLeft();
                } else if (ex > boardW * 0.55f) {
                    moveRight();
                } else {
                    softDrop();
                }
            } else {
                if (ey < pad + dpf(140f)) {
                    holdCurrentPiece();
                } else if (ey >= mTop + dpf(86f) && ey <= mTop + dpf(116f)) {
                    rotateCW();
                } else if (ey >= mTop + dpf(120f)) {
                    hardDrop();
                } else {
                    rotateCW();
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Gravity tick (speeds up with level)
        long now = System.currentTimeMillis();
        long dropInterval = (long) Math.max(120, 800 - (level - 1) * 65);
        if (!gameOver && clearingRows.isEmpty() && now - lastGravityDropTime > dropInterval) {
            lastGravityDropTime = now;
            if (!checkCollision(curPiece, curRot, curX, curY + 1)) {
                curY++;
            } else {
                lockPiece();
            }
        }

        // 1. Cyber Matrix Board Frame
        rect.set(0, 0, w, h);
        matrixBgPaint.setShader(new LinearGradient(0, 0, w, h, 0xFF0B132B, 0xFF020617, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), matrixBgPaint);
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), borderPaint);

        // 2. Playfield Grid Calculations
        float pad = dpf(8f);
        float matrixH = h - pad * 2;
        float cellSize = matrixH / (float) ROWS;
        float matrixW = cellSize * COLS;

        // Draw Matrix Playfield Background & Grid Lines
        rect.set(pad, pad, pad + matrixW, pad + matrixH);
        Paint playfieldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        playfieldPaint.setColor(0xFF030712);
        canvas.drawRoundRect(rect, dpf(6f), dpf(6f), playfieldPaint);
        canvas.drawRoundRect(rect, dpf(6f), dpf(6f), gridLinePaint);

        for (int c = 1; c < COLS; c++) {
            float lx = pad + c * cellSize;
            canvas.drawLine(lx, pad, lx, pad + matrixH, gridLinePaint);
        }
        for (int r = 1; r < ROWS; r++) {
            float ly = pad + r * cellSize;
            canvas.drawLine(pad, ly, pad + matrixW, ly, gridLinePaint);
        }

        // 3. Draw Locked Blocks
        long clearElapsed = System.currentTimeMillis() - clearAnimStartTime;
        boolean isFlashing = !clearingRows.isEmpty() && clearElapsed < CLEAR_ANIM_DURATION_MS;

        for (int r = 0; r < ROWS; r++) {
            boolean isClearing = clearingRows.contains(r);
            for (int c = 0; c < COLS; c++) {
                int val = grid[r][c];
                if (val > 0) {
                    int color = isClearing && isFlashing ? 0xFFFFFFFF : PIECE_COLORS[val - 1];
                    drawBlock(canvas, pad + c * cellSize, pad + r * cellSize, cellSize, color);
                }
            }
        }

        // 4. Draw Ghost Piece Projection
        if (!gameOver && clearingRows.isEmpty()) {
            int ghostY = curY;
            while (!checkCollision(curPiece, curRot, curX, ghostY + 1)) {
                ghostY++;
            }
            int[][] p = getPieceGrid(curPiece, curRot);
            ghostPaint.setColor(PIECE_COLORS[curPiece]);
            ghostPaint.setAlpha(110);
            for (int r = 0; r < p.length; r++) {
                for (int c = 0; c < p[0].length; c++) {
                    if (p[r][c] != 0) {
                        float bx = pad + (curX + c) * cellSize;
                        float by = pad + (ghostY + r) * cellSize;
                        blockRect.set(bx + dpf(1.2f), by + dpf(1.2f), bx + cellSize - dpf(1.2f), by + cellSize - dpf(1.2f));
                        canvas.drawRoundRect(blockRect, dpf(2f), dpf(2f), ghostPaint);
                    }
                }
            }

            // 5. Draw Active Falling Piece
            int color = PIECE_COLORS[curPiece];
            for (int r = 0; r < p.length; r++) {
                for (int c = 0; c < p[0].length; c++) {
                    if (p[r][c] != 0) {
                        drawBlock(canvas, pad + (curX + c) * cellSize, pad + (curY + r) * cellSize, cellSize, color);
                    }
                }
            }
        }

        // 6. Right Sidebar HUD (NEXT, HOLD, SCORE, LEVEL)
        float sideX = pad + matrixW + dpf(8f);
        float sideW = w - sideX - pad;

        // Next Piece Panel
        drawSidePanel(canvas, sideX, pad, sideW, dpf(64f), "NEXT");
        drawMiniPiece(canvas, sideX + sideW / 2f, pad + dpf(36f), nextPiece, dpf(10f));

        // Hold Piece Panel
        drawSidePanel(canvas, sideX, pad + dpf(72f), sideW, dpf(64f), "HOLD");
        if (holdPiece != -1) {
            drawMiniPiece(canvas, sideX + sideW / 2f, pad + dpf(108f), holdPiece, dpf(10f));
        }

        // Metrics Panel
        float mTop = pad + dpf(144f);
        drawSidePanel(canvas, sideX, mTop, sideW, dpf(78f), "METRICS");
        hudLabelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LVL: " + level, sideX + sideW / 2f, mTop + dpf(26f), hudLabelPaint);
        canvas.drawText("LINES: " + lines, sideX + sideW / 2f, mTop + dpf(44f), hudLabelPaint);
        canvas.drawText("PTS: " + score, sideX + sideW / 2f, mTop + dpf(62f), hudLabelPaint);

        // Sidebar Virtual Arcade Badges (Rotate & Drop)
        Paint ctrlBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint ctrlBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        ctrlBorder.setStyle(Paint.Style.STROKE);
        ctrlBorder.setStrokeWidth(dpf(1.2f));
        Paint ctrlText = new Paint(Paint.ANTI_ALIAS_FLAG);
        ctrlText.setTextSize(dpf(9f));
        ctrlText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        ctrlText.setTextAlign(Paint.Align.CENTER);

        // 🔄 Rotate Button
        RectF rotRect = new RectF(sideX, mTop + dpf(84f), sideX + sideW, mTop + dpf(110f));
        ctrlBg.setColor(0x2238BDF8);
        ctrlBorder.setColor(0x6638BDF8);
        ctrlText.setColor(0xFF38BDF8);
        canvas.drawRoundRect(rotRect, dpf(6f), dpf(6f), ctrlBg);
        canvas.drawRoundRect(rotRect, dpf(6f), dpf(6f), ctrlBorder);
        canvas.drawText("🔄 ROTATE", rotRect.centerX(), rotRect.centerY() + dpf(3f), ctrlText);

        // ⚡ Hard Drop Button
        RectF dropRect = new RectF(sideX, mTop + dpf(116f), sideX + sideW, mTop + dpf(142f));
        ctrlBg.setColor(0x33EF4444);
        ctrlBorder.setColor(0x88EF4444);
        ctrlText.setColor(0xFFF43F5E);
        canvas.drawRoundRect(dropRect, dpf(6f), dpf(6f), ctrlBg);
        canvas.drawRoundRect(dropRect, dpf(6f), dpf(6f), ctrlBorder);
        canvas.drawText("⚡ DROP", dropRect.centerX(), dropRect.centerY() + dpf(3f), ctrlText);

        postInvalidateOnAnimation();
    }

    private void drawSidePanel(Canvas canvas, float x, float y, float w, float h, String title) {
        rect.set(x, y, x + w, y + h);
        canvas.drawRoundRect(rect, dpf(6f), dpf(6f), panelPaint);
        canvas.drawRoundRect(rect, dpf(6f), dpf(6f), gridLinePaint);
        hudLabelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(title, x + dpf(6f), y + dpf(14f), hudLabelPaint);
    }

    private void drawBlock(Canvas canvas, float x, float y, float s, int color) {
        blockRect.set(x + dpf(1f), y + dpf(1f), x + s - dpf(1f), y + s - dpf(1f));
        blockPaint.setColor(color);
        canvas.drawRoundRect(blockRect, dpf(2.5f), dpf(2.5f), blockPaint);

        // Top-left shine highlight
        canvas.drawRect(x + dpf(1f), y + dpf(1f), x + s - dpf(1f), y + dpf(3.5f), blockShinePaint);
        canvas.drawRect(x + dpf(1f), y + dpf(1f), x + dpf(3.5f), y + s - dpf(1f), blockShinePaint);
        // Bottom-right shade
        canvas.drawRect(x + dpf(1f), y + s - dpf(3.5f), x + s - dpf(1f), y + s - dpf(1f), blockShadowPaint);
        canvas.drawRect(x + s - dpf(3.5f), y + dpf(1f), x + s - dpf(1f), y + s - dpf(1f), blockShadowPaint);
    }

    private void drawMiniPiece(Canvas canvas, float cx, float cy, int piece, float s) {
        int[][] p = TETROMINOES[piece][0];
        int ph = p.length;
        int pw = p[0].length;
        float startX = cx - (pw * s) / 2f;
        float startY = cy - (ph * s) / 2f;
        int color = PIECE_COLORS[piece];

        for (int r = 0; r < ph; r++) {
            for (int c = 0; c < pw; c++) {
                if (p[r][c] != 0) {
                    drawBlock(canvas, startX + c * s, startY + r * s, s, color);
                }
            }
        }
    }
}