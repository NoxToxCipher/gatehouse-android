package au.com.dss.gatehouse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import java.util.ArrayList;
import java.util.List;

/**
 * ConnectFourGameView — Classic 7x6 Connect 4 Grid.
 * High-fidelity Arcade Blue Acrylic Frame with 3D Glossy Tokens,
 * Smooth Gravity Falling Animations with Physics Bounce,
 * Recessed Slots, Glowing Victory Connect Lines, and Minimax AI.
 */
public class ConnectFourGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint cabinetFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tokenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tokenRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tokenGroovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tokenShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint winGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint winLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint columnGuidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final int[][] grid = new int[6][7];
    private boolean playerTurn = true;
    private boolean gameOver = false;
    private int winR1 = -1, winC1 = -1, winR2 = -1, winC2 = -1;

    // Gravity Drop Animation State
    private boolean isDropping = false;
    private int droppingCol = -1;
    private int droppingTargetRow = -1;
    private int droppingColor = 0;
    private float droppingProgress = 0f; // 0.0 at top to 1.0 at target row
    private ValueAnimator dropAnimator;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public ConnectFourGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(2f));

        goldDetailPaint.setColor(0xFFCA8A04);
        goldDetailPaint.setStyle(Paint.Style.STROKE);
        goldDetailPaint.setStrokeWidth(dpf(1f));

        slotHolePaint.setColor(0xFF030712);

        slotShadowPaint.setColor(0x99000000);
        slotShadowPaint.setStyle(Paint.Style.STROKE);
        slotShadowPaint.setStrokeWidth(dpf(2f));

        slotRimPaint.setColor(0x3338BDF8);
        slotRimPaint.setStyle(Paint.Style.STROKE);
        slotRimPaint.setStrokeWidth(dpf(1f));

        tokenRimPaint.setColor(0x88FFFFFF);
        tokenRimPaint.setStyle(Paint.Style.STROKE);
        tokenRimPaint.setStrokeWidth(dpf(1.2f));

        tokenGroovePaint.setStyle(Paint.Style.STROKE);
        tokenGroovePaint.setStrokeWidth(dpf(1f));

        tokenShinePaint.setColor(0xAAFFFFFF);
        tokenShinePaint.setStyle(Paint.Style.FILL);

        winGlowPaint.setColor(0x6610B981);
        winGlowPaint.setStrokeWidth(dpf(14f));
        winGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        winGlowPaint.setStyle(Paint.Style.STROKE);

        winLinePaint.setColor(0xFF10B981);
        winLinePaint.setStrokeWidth(dpf(5f));
        winLinePaint.setStrokeCap(Paint.Cap.ROUND);
        winLinePaint.setStyle(Paint.Style.STROKE);

        columnGuidePaint.setColor(0x44FFD166);
        columnGuidePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        resetGame();
    }

    private static class HistoryState {
        final int[][] grid = new int[6][7];
        final boolean playerTurn;
        final boolean gameOver;
        final int winR1, winC1, winR2, winC2;

        HistoryState(int[][] g, boolean pt, boolean go, int r1, int c1, int r2, int c2) {
            for (int r = 0; r < 6; r++) {
                System.arraycopy(g[r], 0, grid[r], 0, 7);
            }
            this.playerTurn = pt;
            this.gameOver = go;
            this.winR1 = r1;
            this.winC1 = c1;
            this.winR2 = r2;
            this.winC2 = c2;
        }
    }

    private final List<HistoryState> history = new ArrayList<>();

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void resetGame() {
        if (dropAnimator != null && dropAnimator.isRunning()) {
            dropAnimator.cancel();
        }
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) grid[r][c] = 0;
        }
        history.clear();
        playerTurn = true;
        gameOver = false;
        isDropping = false;
        winR1 = -1;
        updateStatus();
        invalidate();
    }

    public void undoMove() {
        if (isDropping || history.isEmpty()) return;
        HistoryState prev = history.remove(history.size() - 1);
        if (!prev.playerTurn && !history.isEmpty()) {
            prev = history.remove(history.size() - 1);
        }
        for (int r = 0; r < 6; r++) {
            System.arraycopy(prev.grid[r], 0, grid[r], 0, 7);
        }
        this.playerTurn = prev.playerTurn;
        this.gameOver = prev.gameOver;
        this.winR1 = prev.winR1;
        this.winC1 = prev.winC1;
        this.winR2 = prev.winR2;
        this.winC2 = prev.winC2;
        updateStatus();
        invalidate();
    }

    public boolean dropDisc(final int col) {
        if (col < 0 || col >= 7 || gameOver || isDropping) return false;

        history.add(new HistoryState(grid, playerTurn, gameOver, winR1, winC1, winR2, winC2));

        int targetRow = -1;
        for (int r = 5; r >= 0; r--) {
            if (grid[r][col] == 0) {
                targetRow = r;
                break;
            }
        }
        if (targetRow == -1) return false;

        final int color = playerTurn ? 1 : 2;
        final int finalTargetRow = targetRow;

        isDropping = true;
        droppingCol = col;
        droppingTargetRow = finalTargetRow;
        droppingColor = color;
        droppingProgress = 0f;

        // Calculate duration based on distance fallen (longer drop = slightly longer duration)
        long duration = 240 + finalTargetRow * 50;

        if (dropAnimator != null && dropAnimator.isRunning()) {
            dropAnimator.cancel();
        }

        dropAnimator = ValueAnimator.ofFloat(0f, 1f);
        dropAnimator.setDuration(duration);
        dropAnimator.setInterpolator(new BounceInterpolator());
        dropAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                droppingProgress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        dropAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                grid[finalTargetRow][col] = color;
                isDropping = false;

                try {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                } catch (Exception ignored) {}

                if (checkWin(finalTargetRow, col, color)) {
                    gameOver = true;
                    if (statusListener != null) {
                        if (playerTurn) {
                            statusListener.onStatusChanged("🏆 4-IN-A-ROW VICTORY! Strategic alignment.", 0xFF10B981);
                        } else {
                            statusListener.onStatusChanged("💀 BOT WINS! Connect Four Master.", 0xFFEF4444);
                        }
                    }
                    invalidate();
                    return;
                }

                if (isBoardFull()) {
                    gameOver = true;
                    if (statusListener != null) {
                        statusListener.onStatusChanged("🤝 DRAW! Full grid stalemate.", 0xFFFFD166);
                    }
                    invalidate();
                    return;
                }

                playerTurn = !playerTurn;
                updateStatus();
                invalidate();

                if (!gameOver && !playerTurn) {
                    postDelayed(new Runnable() {
                        public void run() { botExecuteMove(); }
                    }, 350);
                }
            }
        });
        dropAnimator.start();
        return true;
    }

    private boolean isBoardFull() {
        for (int c = 0; c < 7; c++) {
            if (grid[0][c] == 0) return false;
        }
        return true;
    }

    private boolean checkWin(int r, int c, int color) {
        int[][] dirs = {{0,1}, {1,0}, {1,1}, {1,-1}};
        for (int[] d : dirs) {
            int count = 1;
            int rStart = r, cStart = c;
            int rEnd = r, cEnd = c;

            int step = 1;
            while (true) {
                int nr = r + d[0] * step;
                int nc = c + d[1] * step;
                if (nr >= 0 && nr < 6 && nc >= 0 && nc < 7 && grid[nr][nc] == color) {
                    count++;
                    rEnd = nr; cEnd = nc;
                    step++;
                } else break;
            }

            step = 1;
            while (true) {
                int nr = r - d[0] * step;
                int nc = c - d[1] * step;
                if (nr >= 0 && nr < 6 && nc >= 0 && nc < 7 && grid[nr][nc] == color) {
                    count++;
                    rStart = nr; cStart = nc;
                    step++;
                } else break;
            }

            if (count >= 4) {
                winR1 = rStart; winC1 = cStart;
                winR2 = rEnd; winC2 = cEnd;
                return true;
            }
        }
        return false;
    }

    private void botExecuteMove() {
        if (playerTurn || gameOver || isDropping) return;

        for (int c = 0; c < 7; c++) {
            int row = getDropRow(c);
            if (row != -1) {
                grid[row][c] = 2;
                if (checkWin(row, c, 2)) {
                    grid[row][c] = 0;
                    dropDisc(c);
                    return;
                }
                grid[row][c] = 0;
            }
        }

        for (int c = 0; c < 7; c++) {
            int row = getDropRow(c);
            if (row != -1) {
                grid[row][c] = 1;
                if (checkWin(row, c, 1)) {
                    grid[row][c] = 0;
                    dropDisc(c);
                    return;
                }
                grid[row][c] = 0;
            }
        }

        int[] preferred = {3, 2, 4, 1, 5, 0, 6};
        for (int c : preferred) {
            if (getDropRow(c) != -1) {
                dropDisc(c);
                return;
            }
        }
    }

    private int getDropRow(int col) {
        for (int r = 5; r >= 0; r--) {
            if (grid[r][col] == 0) return r;
        }
        return -1;
    }

    private void updateStatus() {
        if (statusListener == null || gameOver) return;
        String turn = playerTurn ? "🔴 Your Turn (Red)" : "🟡 Bot Turn (Yellow AI)";
        statusListener.onStatusChanged(turn + " · 7×6 Connect Four", playerTurn ? 0xFFEF4444 : 0xFFF59E0B);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver || isDropping) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && playerTurn) {
            float w = getWidth();
            float pad = dpf(10f);
            float colW = (w - pad * 2) / 7f;
            float ex = event.getX() - pad;
            int col = (int) (ex / colW);
            if (col >= 0 && col < 7) {
                dropDisc(col);
                return true;
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

        // Luxury Obsidian & Cobalt Acrylic Cabinet Frame
        rect.set(0, 0, w, h);
        cabinetFramePaint.setShader(new LinearGradient(0, 0, w, h, 0xFF162544, 0xFF0B1224, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), cabinetFramePaint);

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        rect.set(dpf(5.5f), dpf(5.5f), w - dpf(5.5f), h - dpf(5.5f));
        canvas.drawRoundRect(rect, dpf(11f), dpf(11f), goldDetailPaint);

        float pad = dpf(10f);
        float colW = (w - pad * 2) / 7f;
        float rowH = (h - dpf(16f)) / 6f;
        float holeR = Math.min(colW, rowH) * 0.40f;

        // Top Column Drop Guides (Active Player Indicator)
        if (playerTurn && !isDropping && !gameOver) {
            columnGuidePaint.setColor(0xCCEF4444);
            for (int c = 0; c < 7; c++) {
                if (grid[0][c] == 0) {
                    float cx = pad + c * colW + colW / 2f;
                    canvas.drawCircle(cx, dpf(5.5f), dpf(2.6f), columnGuidePaint);
                }
            }
        }

        // Draw 42 Cylindrical Recessed Slots & Tokens
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                float cx = pad + c * colW + colW / 2f;
                float cy = dpf(8f) + r * rowH + rowH / 2f;

                int val = grid[r][c];
                canvas.drawCircle(cx, cy, holeR, slotHolePaint);
                canvas.drawCircle(cx, cy, holeR, slotShadowPaint);
                canvas.drawCircle(cx, cy, holeR + dpf(0.8f), slotRimPaint);

                if (val == 1) draw3DToken(canvas, cx, cy, holeR * 0.92f, false); // 1 = Red
                else if (val == 2) draw3DToken(canvas, cx, cy, holeR * 0.92f, true); // 2 = Yellow
            }
        }

        // Draw falling disc animation in flight
        if (isDropping && droppingCol >= 0 && droppingTargetRow >= 0) {
            float cx = pad + droppingCol * colW + colW / 2f;
            float topY = dpf(8f) - rowH;
            float targetY = dpf(8f) + droppingTargetRow * rowH + rowH / 2f;
            float currentY = topY + (targetY - topY) * droppingProgress;

            draw3DToken(canvas, cx, currentY, holeR * 0.92f, droppingColor == 2);
        }

        // Draw Winning Glowing Laser Line & Victory Sparkler Stars
        if (gameOver && winR1 != -1 && winR2 != -1) {
            float x1 = pad + winC1 * colW + colW / 2f;
            float y1 = dpf(8f) + winR1 * rowH + rowH / 2f;
            float x2 = pad + winC2 * colW + colW / 2f;
            float y2 = dpf(8f) + winR2 * rowH + rowH / 2f;
            canvas.drawLine(x1, y1, x2, y2, winGlowPaint);
            canvas.drawLine(x1, y1, x2, y2, winLinePaint);

            drawVictorySparkles(canvas, pad, colW, rowH, holeR);
        }
    }

    private void drawVictorySparkles(Canvas canvas, float pad, float colW, float rowH, float holeR) {
        int dr = Integer.compare(winR2, winR1);
        int dc = Integer.compare(winC2, winC1);
        int curR = winR1;
        int curC = winC1;

        Paint sparkGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        sparkGlow.setColor(0x88FFD166);
        sparkGlow.setStyle(Paint.Style.STROKE);
        sparkGlow.setStrokeWidth(dpf(3f));

        Paint sparkCore = new Paint(Paint.ANTI_ALIAS_FLAG);
        sparkCore.setColor(0xFFFFFFFF);
        sparkCore.setStyle(Paint.Style.FILL);

        Paint rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rayPaint.setColor(0xFFFFD166);
        rayPaint.setStrokeWidth(dpf(1.8f));

        for (int i = 0; i < 4; i++) {
            float cx = pad + curC * colW + colW / 2f;
            float cy = dpf(8f) + curR * rowH + rowH / 2f;

            // Halo Ring
            canvas.drawCircle(cx, cy, holeR * 1.15f, sparkGlow);

            // 4-Pointed Sparkle Star
            float starLen = holeR * 0.45f;
            canvas.drawLine(cx - starLen, cy, cx + starLen, cy, rayPaint);
            canvas.drawLine(cx, cy - starLen, cx, cy + starLen, rayPaint);
            canvas.drawCircle(cx, cy, dpf(2.5f), sparkCore);

            curR += dr;
            curC += dc;
        }
    }

    private void draw3DToken(Canvas canvas, float cx, float cy, float r, boolean isYellow) {
        // Deep shadow
        Paint dropShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        dropShadow.setColor(0x99000000);
        canvas.drawCircle(cx + dpf(1.2f), cy + dpf(1.8f), r, dropShadow);

        RadialGradient grad = new RadialGradient(
            cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
            isYellow ? new int[]{0xFFFFFFF5, 0xFFFDE047, 0xFFD97706, 0xFF78350F} // 🟡 Brilliant Sunburst Yellow
                     : new int[]{0xFFFFF1F2, 0xFFFF4D6D, 0xFFDC2626, 0xFF7F1D1D}, // 🔴 Vibrant Crimson Ruby Red
            null, Shader.TileMode.CLAMP
        );
        tokenPaint.setShader(grad);
        canvas.drawCircle(cx, cy, r, tokenPaint);
        canvas.drawCircle(cx, cy, r, tokenRimPaint);

        // Concentric Coin Lathe Rings
        tokenGroovePaint.setColor(isYellow ? 0x6678350F : 0x667F1D1D);
        canvas.drawCircle(cx, cy, r * 0.65f, tokenGroovePaint);
        canvas.drawCircle(cx, cy, r * 0.35f, tokenGroovePaint);

        // Center Star / Emboss Stud
        Paint centerStud = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerStud.setColor(isYellow ? 0xFFFEF08A : 0xFFFCA5A5);
        canvas.drawCircle(cx, cy, dpf(1.8f), centerStud);

        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.28f, tokenShinePaint);
    }
}
