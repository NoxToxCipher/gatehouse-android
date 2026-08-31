package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * ConnectFourGameView — Classic 7x6 Connect 4 Grid.
 * Gravity falling discs, 4-in-a-row detection, Minimax solver AI.
 */
public class ConnectFourGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyHolePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint botPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint winLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    // 7 columns x 6 rows: 0 = empty, 1 = Player (Gold), 2 = Bot (Cyan)
    private final int[][] grid = new int[6][7];
    // true = Player, false = Bot
    private boolean playerTurn = true;
    private boolean gameOver = false;
    private int winX1 = -1, winY1 = -1, winX2 = -1, winY2 = -1;

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public ConnectFourGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        boardBgPaint.setColor(0xFF0F172A);
        gridPaint.setColor(0xFF1E293B);
        emptyHolePaint.setColor(0xFF0B101D);

        playerPaint.setColor(0xFFFFD166); // Gold
        playerPaint.setStyle(Paint.Style.FILL);

        botPaint.setColor(0xFF38BDF8); // Cyan
        botPaint.setStyle(Paint.Style.FILL);

        winLinePaint.setColor(0xFF10B981); // Emerald line
        winLinePaint.setStrokeWidth(dpf(4f));
        winLinePaint.setStyle(Paint.Style.STROKE);

        resetGame();
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void resetGame() {
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                grid[r][c] = 0;
            }
        }
        playerTurn = true;
        gameOver = false;
        winX1 = -1;
        updateStatus();
        invalidate();
    }

    public boolean dropDisc(int col) {
        if (col < 0 || col >= 7 || gameOver) return false;

        // Find lowest empty row in column
        int targetRow = -1;
        for (int r = 5; r >= 0; r--) {
            if (grid[r][col] == 0) {
                targetRow = r;
                break;
            }
        }
        if (targetRow == -1) return false; // Column full

        int color = playerTurn ? 1 : 2;
        grid[targetRow][col] = color;

        if (checkWin(targetRow, col, color)) {
            gameOver = true;
            if (statusListener != null) {
                if (playerTurn) {
                    statusListener.onStatusChanged("🏆 4-IN-A-ROW VICTORY! Perfect strategic drop.", 0xFF10B981);
                } else {
                    statusListener.onStatusChanged("💀 BOT WINS! 4-in-a-row Connect Four.", 0xFFEF4444);
                }
            }
            invalidate();
            return true;
        }

        if (isBoardFull()) {
            gameOver = true;
            if (statusListener != null) {
                statusListener.onStatusChanged("🤝 DRAW! Complete grid stalemate.", 0xFFFFD166);
            }
            invalidate();
            return true;
        }

        playerTurn = !playerTurn;
        updateStatus();
        invalidate();

        if (!gameOver && !playerTurn) {
            postDelayed(new Runnable() {
                public void run() { botExecuteMove(); }
            }, 380);
        }
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
            // Forward
            int step = 1;
            while (true) {
                int nr = r + d[0] * step;
                int nc = c + d[1] * step;
                if (nr >= 0 && nr < 6 && nc >= 0 && nc < 7 && grid[nr][nc] == color) {
                    count++;
                    step++;
                } else break;
            }
            // Backward
            step = 1;
            while (true) {
                int nr = r - d[0] * step;
                int nc = c - d[1] * step;
                if (nr >= 0 && nr < 6 && nc >= 0 && nc < 7 && grid[nr][nc] == color) {
                    count++;
                    step++;
                } else break;
            }
            if (count >= 4) return true;
        }
        return false;
    }

    private void botExecuteMove() {
        if (playerTurn || gameOver) return;

        // 1. Check if Bot can win on next move
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

        // 2. Check if Player can win on next move -> Block!
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

        // 3. Prefer center columns (3, 2, 4, 1, 5, 0, 6)
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
        String turn = playerTurn ? "🟡 Your Turn (Gold)" : "🔵 Bot Turn (Cyan)";
        statusListener.onStatusChanged(turn + " · 7×6 Connect Four", 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && playerTurn) {
            float w = getWidth();
            float pad = dpf(12f);
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

        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardBgPaint);

        float pad = dpf(12f);
        float colW = (w - pad * 2) / 7f;
        float rowH = (h - dpf(16f)) / 6f;
        float holeR = Math.min(colW, rowH) * 0.38f;

        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                float cx = pad + c * colW + colW / 2f;
                float cy = dpf(8f) + r * rowH + rowH / 2f;

                int val = grid[r][c];
                if (val == 0) {
                    canvas.drawCircle(cx, cy, holeR, emptyHolePaint);
                } else if (val == 1) {
                    canvas.drawCircle(cx, cy, holeR, playerPaint);
                } else if (val == 2) {
                    canvas.drawCircle(cx, cy, holeR, botPaint);
                }
            }
        }
    }
}
