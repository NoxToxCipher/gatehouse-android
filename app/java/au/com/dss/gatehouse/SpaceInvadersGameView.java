package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * SpaceInvadersGameView — Authentic 1978 Arcade Space Invaders Engine.
 * Museum-grade Arcade Vector Canvas with 5 Alien Rows (Squids, Crabs, Octopuses),
 * Destructible Defense Bunkers, Mystery Red Mothership UFO, Particle Physics,
 * CRT Scanlines, and Procedural 8-bit Audio Synthesis.
 */
public class SpaceInvadersGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint crtBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crtScanlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint alienLaserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint alienPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ufoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bunkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random rand = new Random();

    // Game Entities & State
    private static final int ALIEN_ROWS = 5;
    private static final int ALIEN_COLS = 9;
    private final boolean[][] aliens = new boolean[ALIEN_ROWS][ALIEN_COLS];
    private float alienBaseX = 0f;
    private float alienBaseY = 0f;
    private float alienDirX = 1f;
    private float alienSpeed = 1.0f;
    private long lastAlienStepTime = 0;
    private int alienAnimFrame = 0;

    // Player Cannon
    private float playerX = 0f;
    private float playerTargetX = 0f;
    private int playerLives = 3;
    private int score = 0;
    private int highScore = 1980;
    private int wave = 1;
    private boolean gameOver = false;
    private boolean gameWon = false;
    private boolean isPaused = false;

    // Lasers & Projectiles
    private static class Projectile {
        float x, y, vy;
        boolean isPlayer;
        Projectile(float x, float y, float vy, boolean isPlayer) {
            this.x = x; this.y = y; this.vy = vy; this.isPlayer = isPlayer;
        }
    }
    private final List<Projectile> projectiles = new ArrayList<>();
    private long lastPlayerFireTime = 0;

    // Mystery UFO Mothership
    private float ufoX = -100f;
    private float ufoSpeed = 0f;
    private boolean isUfoActive = false;
    private long nextUfoSpawnTime = 0;

    // 3 Destructible Bunkers (Grid of pixel blocks per bunker)
    private static final int BUNKER_W = 12;
    private static final int BUNKER_H = 8;
    private final boolean[][][] bunkers = new boolean[3][BUNKER_H][BUNKER_W];

    // Explosion Particles
    private static class Particle {
        float x, y, vx, vy, alpha, size;
        int color;
    }
    private final List<Particle> particles = new ArrayList<>();

    // Background Twinkling Stars
    private final float[] starX = new float[40];
    private final float[] starY = new float[40];
    private final float[] starAlpha = new float[40];

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public SpaceInvadersGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        borderPaint.setColor(0xFF10B981);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpf(1.8f));

        playerPaint.setColor(0xFF34D399); // Arcade Emerald Cannon
        playerPaint.setStyle(Paint.Style.FILL);

        laserPaint.setColor(0xFF38BDF8); // Neon Cyan Laser
        laserPaint.setStrokeWidth(dpf(2.4f));
        laserPaint.setStrokeCap(Paint.Cap.ROUND);

        alienLaserPaint.setColor(0xFFF43F5E); // Crimson Alien Lightning
        alienLaserPaint.setStrokeWidth(dpf(2.0f));

        alienPaint.setStyle(Paint.Style.FILL);
        ufoPaint.setColor(0xFFEF4444);
        ufoPaint.setStyle(Paint.Style.FILL);

        bunkerPaint.setColor(0xFF10B981);
        bunkerPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(0xFFF8FAFC);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        hudPaint.setColor(0xFF94A3B8);
        hudPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        crtScanlinePaint.setColor(0x18000000);
        crtScanlinePaint.setStrokeWidth(dpf(1.2f));

        initStars();
        startNewGame();
    }

    private void initStars() {
        for (int i = 0; i < 40; i++) {
            starX[i] = rand.nextFloat();
            starY[i] = rand.nextFloat();
            starAlpha[i] = 0.2f + rand.nextFloat() * 0.8f;
        }
    }

    public void setStatusListener(StatusListener listener) {
        this.statusListener = listener;
        updateStatus();
    }

    private void updateStatus() {
        if (statusListener == null) return;
        if (gameOver) {
            statusListener.onStatusChanged("💀 INVASION COMPLETE! Final Score: " + score + " · Tap to Restart", 0xFFEF4444);
        } else if (gameWon) {
            statusListener.onStatusChanged("🏆 WAVE " + wave + " CLEARED! +1000 Bonus · Score: " + score, 0xFF10B981);
        } else {
            statusListener.onStatusChanged("👾 Wave " + wave + " · Score: " + score + " · Lives: " + playerLives, 0xFF38BDF8);
        }
    }

    public void startNewGame() {
        score = 0;
        playerLives = 3;
        wave = 1;
        gameOver = false;
        gameWon = false;
        initWave();
        updateStatus();
        invalidate();
    }

    private void initWave() {
        projectiles.clear();
        particles.clear();
        alienBaseX = dpf(20f);
        alienBaseY = dpf(42f);
        alienDirX = 1f;
        alienSpeed = dpf(1.2f) + (wave - 1) * dpf(0.3f);
        isUfoActive = false;
        nextUfoSpawnTime = System.currentTimeMillis() + 8000 + rand.nextInt(12000);

        for (int r = 0; r < ALIEN_ROWS; r++) {
            for (int c = 0; c < ALIEN_COLS; c++) {
                aliens[r][c] = true;
            }
        }
        initBunkers();
    }

    private void initBunkers() {
        for (int b = 0; b < 3; b++) {
            for (int r = 0; r < BUNKER_H; r++) {
                for (int c = 0; c < BUNKER_W; c++) {
                    // Arch cutout in center-bottom
                    boolean isArch = (r >= 5 && c >= 4 && c <= 7);
                    bunkers[b][r][c] = !isArch;
                }
            }
        }
    }

    public void fireLaser() {
        if (gameOver || gameWon) return;
        long now = System.currentTimeMillis();
        if (now - lastPlayerFireTime < 260) return; // Fire rate limiter
        lastPlayerFireTime = now;

        float py = getHeight() - dpf(46f);
        projectiles.add(new Projectile(playerX, py, -dpf(9f), true));
        try {
            RecreationAudioSynth.playLaserShoot();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}
    }

    private void triggerExplosion(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            float angle = (float) (rand.nextFloat() * Math.PI * 2);
            float speed = dpf(1.5f + rand.nextFloat() * 4.5f);
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed;
            p.alpha = 1.0f;
            p.size = dpf(2f + rand.nextFloat() * 3f);
            p.color = color;
            particles.add(p);
        }
        try {
            RecreationAudioSynth.playExplosion();
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }

        if (gameOver || gameWon) {
            if (action == MotionEvent.ACTION_DOWN) {
                startNewGame();
                return true;
            }
        }

        float ex = event.getX();
        float ey = event.getY();
        int w = getWidth();
        int h = getHeight();

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            // Check touch controls at bottom
            if (ey > h - dpf(30f)) {
                if (ex >= w - dpf(130f) && ex <= w - dpf(92f)) {
                    playerTargetX = Math.max(dpf(24f), playerTargetX - dpf(18f));
                    try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
                    return true;
                } else if (ex >= w - dpf(88f) && ex <= w - dpf(46f)) {
                    fireLaser();
                    return true;
                } else if (ex >= w - dpf(42f) && ex <= w - dpf(6f)) {
                    playerTargetX = Math.min(w - dpf(24f), playerTargetX + dpf(18f));
                    try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
                    return true;
                }
            }

            // Touch / drag anywhere on upper screen to steer cannon
            playerTargetX = Math.max(dpf(24f), Math.min(w - dpf(24f), ex));
            if (action == MotionEvent.ACTION_DOWN && ey < h - dpf(45f)) {
                fireLaser();
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

        if (playerX == 0f) {
            playerX = w / 2f;
            playerTargetX = w / 2f;
        }

        // Smooth cannon lerp
        playerX += (playerTargetX - playerX) * 0.35f;

        // 1. Arcade CRT Bezel & Deep Space
        rect.set(0, 0, w, h);
        crtBgPaint.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.8f, 0xFF0B132B, 0xFF030712, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), crtBgPaint);
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), borderPaint);

        // 2. Parallax Twinkling Stars
        Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(0xFFFFFFFF);
        for (int i = 0; i < 40; i++) {
            starPaint.setAlpha((int) (255 * starAlpha[i]));
            canvas.drawCircle(starX[i] * w, starY[i] * h, dpf(1.1f), starPaint);
        }

        // 3. Update & Draw Game Logic (60fps cycle)
        if (!gameOver && !gameWon) {
            updateGamePhysics(w, h);
        }

        // Draw Bunkers
        drawBunkers(canvas, w, h);

        // Draw Aliens
        drawAliens(canvas, w, h);

        // Draw Mystery UFO
        if (isUfoActive) {
            drawUfo(canvas, ufoX, dpf(26f));
        }

        // Draw Projectiles
        for (Projectile p : projectiles) {
            if (p.isPlayer) {
                canvas.drawLine(p.x, p.y, p.x, p.y + dpf(8f), laserPaint);
            } else {
                canvas.drawLine(p.x, p.y, p.x, p.y - dpf(6f), alienLaserPaint);
            }
        }

        // Draw Explosion Particles
        if (!particles.isEmpty()) {
            Paint partPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            partPaint.setStyle(Paint.Style.FILL);
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.x += p.vx;
                p.y += p.vy;
                p.alpha -= 0.045f;
                if (p.alpha <= 0) {
                    particles.remove(i);
                } else {
                    partPaint.setColor(p.color);
                    partPaint.setAlpha((int) (255 * p.alpha));
                    canvas.drawCircle(p.x, p.y, p.size * p.alpha, partPaint);
                }
            }
        }

        // Draw Player Cannon
        drawPlayerCannon(canvas, playerX, h - dpf(38f));

        // 4. CRT Horizontal Scanlines
        for (int y = 0; y < h; y += (int) dpf(4f)) {
            canvas.drawLine(0, y, w, y, crtScanlinePaint);
        }

        // 5. Retro Vector HUD Header & Score
        hudPaint.setTextSize(dpf(10f));
        canvas.drawText("SCORE: " + score, dpf(14f), dpf(18f), hudPaint);
        hudPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("HI: " + Math.max(score, highScore), w - dpf(14f), dpf(18f), hudPaint);
        hudPaint.setTextAlign(Paint.Align.LEFT);

        // Bottom Lives Counter
        for (int i = 0; i < playerLives; i++) {
            drawMiniCannon(canvas, dpf(14f) + i * dpf(18f), h - dpf(12f));
        }

        // Retro Touch Control Badges
        Paint ctrlBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        ctrlBg.setColor(0x2210B981);
        Paint ctrlBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        ctrlBorder.setColor(0x6610B981);
        ctrlBorder.setStyle(Paint.Style.STROKE);
        ctrlBorder.setStrokeWidth(dpf(1.2f));
        Paint ctrlText = new Paint(Paint.ANTI_ALIAS_FLAG);
        ctrlText.setColor(0xFF34D399);
        ctrlText.setTextSize(dpf(9f));
        ctrlText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        ctrlText.setTextAlign(Paint.Align.CENTER);

        // Left button
        RectF lRect = new RectF(w - dpf(130f), h - dpf(20f), w - dpf(92f), h - dpf(4f));
        canvas.drawRoundRect(lRect, dpf(4f), dpf(4f), ctrlBg);
        canvas.drawRoundRect(lRect, dpf(4f), dpf(4f), ctrlBorder);
        canvas.drawText("◀", lRect.centerX(), lRect.centerY() + dpf(3f), ctrlText);

        // Fire button
        RectF fRect = new RectF(w - dpf(88f), h - dpf(20f), w - dpf(46f), h - dpf(4f));
        ctrlBg.setColor(0x33EF4444);
        ctrlBorder.setColor(0x88EF4444);
        ctrlText.setColor(0xFFF43F5E);
        canvas.drawRoundRect(fRect, dpf(4f), dpf(4f), ctrlBg);
        canvas.drawRoundRect(fRect, dpf(4f), dpf(4f), ctrlBorder);
        canvas.drawText("⚡ FIRE", fRect.centerX(), fRect.centerY() + dpf(3f), ctrlText);

        // Right button
        RectF rRect = new RectF(w - dpf(42f), h - dpf(20f), w - dpf(6f), h - dpf(4f));
        ctrlBg.setColor(0x2210B981);
        ctrlBorder.setColor(0x6610B981);
        ctrlText.setColor(0xFF34D399);
        canvas.drawRoundRect(rRect, dpf(4f), dpf(4f), ctrlBg);
        canvas.drawRoundRect(rRect, dpf(4f), dpf(4f), ctrlBorder);
        canvas.drawText("▶", rRect.centerX(), rRect.centerY() + dpf(3f), ctrlText);

        postInvalidateOnAnimation();
    }

    private void updateGamePhysics(int w, int h) {
        long now = System.currentTimeMillis();

        // 1. Move Aliens
        int remainingAliens = 0;
        float minAlienX = 9999f, maxAlienX = -9999f;
        float alienColW = (w - dpf(36f)) / (float) ALIEN_COLS;
        float alienRowH = dpf(18f);

        for (int r = 0; r < ALIEN_ROWS; r++) {
            for (int c = 0; c < ALIEN_COLS; c++) {
                if (aliens[r][c]) {
                    remainingAliens++;
                    float ax = alienBaseX + c * alienColW;
                    if (ax < minAlienX) minAlienX = ax;
                    if (ax > maxAlienX) maxAlienX = ax;
                }
            }
        }

        if (remainingAliens == 0) {
            gameWon = true;
            score += 1000;
            wave++;
            updateStatus();
            postDelayed(new Runnable() { public void run() { initWave(); gameWon = false; updateStatus(); } }, 1800);
            return;
        }

        // Alien march cadence speeds up as invaders are destroyed
        long stepInterval = (long) Math.max(80, 500 * (remainingAliens / 45.0));
        if (now - lastAlienStepTime > stepInterval) {
            lastAlienStepTime = now;
            alienAnimFrame = 1 - alienAnimFrame;
            alienBaseX += alienDirX * (dpf(4f) + (wave * 0.4f));

            if (maxAlienX + alienColW > w - dpf(14f)) {
                alienDirX = -1f;
                alienBaseY += dpf(8f);
            } else if (minAlienX < dpf(14f)) {
                alienDirX = 1f;
                alienBaseY += dpf(8f);
            }
        }

        // Alien reaches bunkers/cannon -> Game Over
        if (alienBaseY + ALIEN_ROWS * alienRowH >= h - dpf(70f)) {
            gameOver = true;
            updateStatus();
            return;
        }

        // Random Alien Bomb drops
        if (rand.nextInt(100) < (4 + wave * 2)) {
            int c = rand.nextInt(ALIEN_COLS);
            for (int r = ALIEN_ROWS - 1; r >= 0; r--) {
                if (aliens[r][c]) {
                    float bx = alienBaseX + c * alienColW + alienColW / 2f;
                    float by = alienBaseY + r * alienRowH + alienRowH;
                    projectiles.add(new Projectile(bx, by, dpf(4.2f), false));
                    break;
                }
            }
        }

        // UFO Spawn & Movement
        if (!isUfoActive && now > nextUfoSpawnTime) {
            isUfoActive = true;
            ufoX = -dpf(30f);
            ufoSpeed = dpf(2.4f);
        }
        if (isUfoActive) {
            ufoX += ufoSpeed;
            if (ufoX > w + dpf(40f)) {
                isUfoActive = false;
                nextUfoSpawnTime = now + 12000 + rand.nextInt(15000);
            }
        }

        // 2. Update Projectiles & Collisions
        float playerY = h - dpf(38f);
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile p = it.next();
            p.y += p.vy;

            // Screen boundary cleanup
            if (p.y < dpf(10f) || p.y > h - dpf(10f)) {
                it.remove();
                continue;
            }

            // Player Laser -> Alien Collision
            if (p.isPlayer) {
                boolean hit = false;

                // Check UFO hit
                if (isUfoActive && Math.abs(p.x - ufoX) < dpf(18f) && Math.abs(p.y - dpf(26f)) < dpf(12f)) {
                    isUfoActive = false;
                    hit = true;
                    int ufoPts = (rand.nextInt(4) + 1) * 100;
                    score += ufoPts;
                    triggerExplosion(ufoX, dpf(26f), 0xFFEF4444, 25);
                    it.remove();
                    continue;
                }

                // Check Aliens hit
                for (int r = 0; r < ALIEN_ROWS && !hit; r++) {
                    for (int c = 0; c < ALIEN_COLS && !hit; c++) {
                        if (aliens[r][c]) {
                            float ax = alienBaseX + c * alienColW + alienColW / 2f;
                            float ay = alienBaseY + r * alienRowH + alienRowH / 2f;
                            if (Math.abs(p.x - ax) < alienColW * 0.48f && Math.abs(p.y - ay) < alienRowH * 0.55f) {
                                aliens[r][c] = false;
                                hit = true;
                                int pts = (r == 0) ? 30 : (r <= 2 ? 20 : 10);
                                score += pts;
                                int color = (r == 0) ? 0xFFF43F5E : (r <= 2 ? 0xFF38BDF8 : 0xFFFDE047);
                                triggerExplosion(ax, ay, color, 14);
                                it.remove();
                            }
                        }
                    }
                }
                if (hit) continue;
            }

            // Alien Bomb -> Player Cannon Collision
            if (!p.isPlayer) {
                if (Math.abs(p.x - playerX) < dpf(16f) && Math.abs(p.y - playerY) < dpf(12f)) {
                    triggerExplosion(playerX, playerY, 0xFF34D399, 30);
                    playerLives--;
                    it.remove();
                    if (playerLives <= 0) {
                        gameOver = true;
                    }
                    updateStatus();
                    continue;
                }
            }

            // Projectile -> Bunker Damage
            if (checkBunkerHit(p.x, p.y, w, h)) {
                it.remove();
            }
        }
    }

    private boolean checkBunkerHit(float px, float py, int w, int h) {
        float bunkerY = h - dpf(90f);
        float bWidth = dpf(42f);
        float bGap = (w - bWidth * 3) / 4f;

        for (int b = 0; b < 3; b++) {
            float bLeft = bGap + b * (bWidth + bGap);
            if (px >= bLeft && px <= bLeft + bWidth && py >= bunkerY && py <= bunkerY + dpf(28f)) {
                int col = (int) ((px - bLeft) / (bWidth / BUNKER_W));
                int row = (int) ((py - bunkerY) / (dpf(28f) / BUNKER_H));
                if (col >= 0 && col < BUNKER_W && row >= 0 && row < BUNKER_H && bunkers[b][row][col]) {
                    // Destroy cluster of 4 pixels
                    bunkers[b][row][col] = false;
                    if (row > 0) bunkers[b][row - 1][col] = false;
                    if (row < BUNKER_H - 1) bunkers[b][row + 1][col] = false;
                    if (col > 0) bunkers[b][row][col - 1] = false;
                    if (col < BUNKER_W - 1) bunkers[b][row][col + 1] = false;
                    triggerExplosion(px, py, 0xFF10B981, 6);
                    return true;
                }
            }
        }
        return false;
    }

    private void drawAliens(Canvas canvas, int w, int h) {
        float alienColW = (w - dpf(36f)) / (float) ALIEN_COLS;
        float alienRowH = dpf(18f);

        for (int r = 0; r < ALIEN_ROWS; r++) {
            int color = (r == 0) ? 0xFFF43F5E : (r <= 2 ? 0xFF38BDF8 : 0xFFFDE047);
            alienPaint.setColor(color);

            for (int c = 0; c < ALIEN_COLS; c++) {
                if (aliens[r][c]) {
                    float ax = alienBaseX + c * alienColW + alienColW / 2f;
                    float ay = alienBaseY + r * alienRowH + alienRowH / 2f;
                    drawInvaderSprite(canvas, ax, ay, dpf(6.5f), r, alienAnimFrame);
                }
            }
        }
    }

    private void drawInvaderSprite(Canvas canvas, float cx, float cy, float s, int row, int frame) {
        // Squid (Top row), Crab (Mid rows), Octopus (Bottom rows)
        if (row == 0) { // Squid
            rect.set(cx - s * 0.6f, cy - s * 0.7f, cx + s * 0.6f, cy + s * 0.7f);
            canvas.drawRoundRect(rect, dpf(2f), dpf(2f), alienPaint);
            if (frame == 0) {
                canvas.drawRect(cx - s * 0.9f, cy, cx - s * 0.6f, cy + s * 0.9f, alienPaint);
                canvas.drawRect(cx + s * 0.6f, cy, cx + s * 0.9f, cy + s * 0.9f, alienPaint);
            } else {
                canvas.drawRect(cx - s * 0.7f, cy + s * 0.2f, cx - s * 0.4f, cy + s * 1.1f, alienPaint);
                canvas.drawRect(cx + s * 0.4f, cy + s * 0.2f, cx + s * 0.7f, cy + s * 1.1f, alienPaint);
            }
        } else if (row <= 2) { // Crab
            rect.set(cx - s * 0.8f, cy - s * 0.6f, cx + s * 0.8f, cy + s * 0.5f);
            canvas.drawRoundRect(rect, dpf(2f), dpf(2f), alienPaint);
            if (frame == 0) {
                canvas.drawRect(cx - s * 1.1f, cy - s * 0.4f, cx - s * 0.8f, cy + s * 0.7f, alienPaint);
                canvas.drawRect(cx + s * 0.8f, cy - s * 0.4f, cx + s * 1.1f, cy + s * 0.7f, alienPaint);
            } else {
                canvas.drawRect(cx - s * 1.1f, cy, cx - s * 0.8f, cy - s * 0.9f, alienPaint);
                canvas.drawRect(cx + s * 0.8f, cy, cx + s * 1.1f, cy - s * 0.9f, alienPaint);
            }
        } else { // Octopus
            rect.set(cx - s * 0.9f, cy - s * 0.6f, cx + s * 0.9f, cy + s * 0.4f);
            canvas.drawRoundRect(rect, dpf(3f), dpf(3f), alienPaint);
            if (frame == 0) {
                canvas.drawRect(cx - s * 0.8f, cy + s * 0.4f, cx - s * 0.4f, cy + s * 0.9f, alienPaint);
                canvas.drawRect(cx + s * 0.4f, cy + s * 0.4f, cx + s * 0.8f, cy + s * 0.9f, alienPaint);
            } else {
                canvas.drawRect(cx - s * 0.5f, cy + s * 0.4f, cx - s * 0.1f, cy + s * 0.9f, alienPaint);
                canvas.drawRect(cx + s * 0.1f, cy + s * 0.4f, cx + s * 0.5f, cy + s * 0.9f, alienPaint);
            }
        }
    }

    private void drawUfo(Canvas canvas, float cx, float cy) {
        float w = dpf(28f);
        float h = dpf(12f);
        rect.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
        canvas.drawRoundRect(rect, dpf(6f), dpf(6f), ufoPaint);
        rect.set(cx - w * 0.28f, cy - h * 0.9f, cx + w * 0.28f, cy);
        canvas.drawRoundRect(rect, dpf(4f), dpf(4f), ufoPaint);
    }

    private void drawBunkers(Canvas canvas, int w, int h) {
        float bunkerY = h - dpf(90f);
        float bWidth = dpf(42f);
        float bHeight = dpf(28f);
        float bGap = (w - bWidth * 3) / 4f;
        float pw = bWidth / BUNKER_W;
        float ph = bHeight / BUNKER_H;

        for (int b = 0; b < 3; b++) {
            float bLeft = bGap + b * (bWidth + bGap);
            for (int r = 0; r < BUNKER_H; r++) {
                for (int c = 0; c < BUNKER_W; c++) {
                    if (bunkers[b][r][c]) {
                        canvas.drawRect(bLeft + c * pw, bunkerY + r * ph, bLeft + (c + 1) * pw, bunkerY + (r + 1) * ph, bunkerPaint);
                    }
                }
            }
        }
    }

    private void drawPlayerCannon(Canvas canvas, float cx, float cy) {
        float w = dpf(26f);
        float h = dpf(12f);
        // Base
        rect.set(cx - w / 2f, cy, cx + w / 2f, cy + h);
        canvas.drawRoundRect(rect, dpf(3f), dpf(3f), playerPaint);
        // Gun Turret
        rect.set(cx - dpf(3f), cy - dpf(8f), cx + dpf(3f), cy);
        canvas.drawRect(rect, playerPaint);
    }

    private void drawMiniCannon(Canvas canvas, float cx, float cy) {
        rect.set(cx - dpf(6f), cy, cx + dpf(6f), cy + dpf(5f));
        canvas.drawRect(rect, playerPaint);
        rect.set(cx - dpf(1.5f), cy - dpf(3f), cx + dpf(1.5f), cy);
        canvas.drawRect(rect, playerPaint);
    }
}