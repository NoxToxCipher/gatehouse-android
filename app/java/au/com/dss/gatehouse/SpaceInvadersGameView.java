package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
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
import java.util.List;
import java.util.Random;

/**
 * SpaceInvadersGameView — Museum-grade Space Invaders Engine.
 * 
 * Powered by native Rust (`libspace_invaders.so`) for 55-invader bitboard fleet stepping,
 * swept AABB projectile physics, destructible bunker shield bitmasks, and procedural 8-bit audio DSP.
 * 
 * Features:
 * - Pixel-accurate laser emergence from cannon turret tip
 * - Perfectly symmetrical equal-width ergonomic control deck with multi-touch continuous gliding
 * - Continuous rapid auto-fire holding
 * - Procedural CRT scanlines & particle physics
 */
public class SpaceInvadersGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint crtBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crtScanlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerTurretPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint laserGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint laserCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint alienLaserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint alienPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ufoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bunkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path lightningPath = new Path();
    private final Random rand = new Random();

    // Native Engine Instance
    private SpaceInvadersNative nativeEngine;
    private final int[] stateInts = new int[10];
    private final float[] stateFloats = new float[6];
    private final float[] projFloats = new float[128]; // max 32 * 4
    private final byte[] bunkerGrid = new byte[12 * 8];

    // Game State Cached from Native
    private float playerX = 0f;
    private float playerTargetX = 0f;
    private int playerLives = 3;
    private int score = 0;
    private int highScore = 1980;
    private int wave = 1;
    private boolean gameOver = false;
    private boolean gameWon = false;
    private boolean isUfoActive = false;
    private float ufoX = -100f;
    private float fleetBaseX = 20f;
    private float fleetBaseY = 40f;
    private int animFrame = 0;
    private long lastFrameTime = 0;
    private boolean shootTrigger = false;

    // Symmetrical Ergonomic Multi-Touch Control State
    private boolean isHoldingLeft = false;
    private boolean isHoldingRight = false;
    private boolean isHoldingFire = false;
    private long lastAutoFireTime = 0;

    private final RectF lBtnRect = new RectF();
    private final RectF rBtnRect = new RectF();
    private final RectF fBtnRect = new RectF();

    // Floating Score Popups (e.g. +300 for UFO)
    private static class ScorePopup {
        float x, y;
        String text;
        float alpha;
        int color;
    }
    private final List<ScorePopup> popups = new ArrayList<>();

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

        playerTurretPaint.setColor(0xFFFFD166); // Gold Gun Turret Tip
        playerTurretPaint.setStyle(Paint.Style.FILL);

        laserGlowPaint.setColor(0x8838BDF8); // Neon Cyan Laser Glow
        laserGlowPaint.setStrokeWidth(dpf(4.0f));
        laserGlowPaint.setStrokeCap(Paint.Cap.ROUND);

        laserCorePaint.setColor(0xFFFFFFFF); // Pure White Hot Beam Core
        laserCorePaint.setStrokeWidth(dpf(2.0f));
        laserCorePaint.setStrokeCap(Paint.Cap.ROUND);

        alienLaserPaint.setColor(0xFFF43F5E); // Crimson Alien Lightning
        alienLaserPaint.setStrokeWidth(dpf(2.2f));
        alienLaserPaint.setStyle(Paint.Style.STROKE);

        alienPaint.setStyle(Paint.Style.FILL);
        ufoPaint.setColor(0xFFEF4444);
        ufoPaint.setStyle(Paint.Style.FILL);

        bunkerPaint.setColor(0xFF10B981);
        bunkerPaint.setStyle(Paint.Style.FILL);

        hudPaint.setColor(0xFF94A3B8);
        hudPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        crtScanlinePaint.setColor(0x18000000);
        crtScanlinePaint.setStrokeWidth(dpf(1.2f));

        initStars();
    }

    private void initStars() {
        for (int i = 0; i < 40; i++) {
            starX[i] = rand.nextFloat();
            starY[i] = rand.nextFloat();
            starAlpha[i] = 0.2f + rand.nextFloat() * 0.8f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            if (nativeEngine != null) {
                nativeEngine.release();
            }
            nativeEngine = new SpaceInvadersNative(w, h);
            playerX = w / 2f;
            playerTargetX = w / 2f;
            lastFrameTime = System.currentTimeMillis();

            // Setup perfectly symmetrical, equal-width touch control zones
            float pad = dpf(10f);
            float gap = dpf(8f);
            float btnH = dpf(52f);
            float btnY1 = h - btnH - dpf(8f);
            float btnY2 = h - dpf(8f);

            // Left steering section: 46% of available width, split equally into 2 symmetrical buttons
            float leftSectionW = (w - pad * 2f - gap) * 0.46f;
            float steerBtnW = (leftSectionW - gap) / 2f;

            lBtnRect.set(pad, btnY1, pad + steerBtnW, btnY2);
            rBtnRect.set(pad + steerBtnW + gap, btnY1, pad + steerBtnW * 2f + gap, btnY2);

            // Right action section: Rapid Fire takes remaining width
            float fireX1 = pad + steerBtnW * 2f + gap * 2f;
            fBtnRect.set(fireX1, btnY1, w - pad, btnY2);

            updateStatus();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (nativeEngine != null) {
            nativeEngine.release();
            nativeEngine = null;
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
        if (nativeEngine != null) {
            nativeEngine.resetGame();
        }
        score = 0;
        playerLives = 3;
        wave = 1;
        gameOver = false;
        gameWon = false;
        isHoldingLeft = false;
        isHoldingRight = false;
        isHoldingFire = false;
        particles.clear();
        popups.clear();
        updateStatus();
        invalidate();
    }

    public void fireLaser() {
        if (gameOver || gameWon) return;
        shootTrigger = true;
    }

    private void triggerExplosion(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            float angle = (float) (rand.nextFloat() * Math.PI * 2);
            float speed = dpf(1.8f + rand.nextFloat() * 5.0f);
            p.vx = (float) Math.cos(angle) * speed;
            p.vy = (float) Math.sin(angle) * speed;
            p.alpha = 1.0f;
            p.size = dpf(2f + rand.nextFloat() * 3.5f);
            p.color = color;
            particles.add(p);
        }
    }

    private void addScorePopup(float x, float y, String text, int color) {
        ScorePopup sp = new ScorePopup();
        sp.x = x;
        sp.y = y;
        sp.text = text;
        sp.alpha = 1.0f;
        sp.color = color;
        popups.add(sp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }

        if (gameOver || gameWon) {
            if (action == MotionEvent.ACTION_DOWN) {
                startNewGame();
                return true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            isHoldingLeft = false;
            isHoldingRight = false;
            isHoldingFire = false;
            return true;
        }

        // Multi-touch evaluation across all active pointers
        boolean newHoldLeft = false;
        boolean newHoldRight = false;
        boolean newHoldFire = false;

        int pointerCount = event.getPointerCount();
        for (int p = 0; p < pointerCount; p++) {
            float px = event.getX(p);
            float py = event.getY(p);

            if (lBtnRect.contains(px, py)) {
                newHoldLeft = true;
            } else if (rBtnRect.contains(px, py)) {
                newHoldRight = true;
            } else if (fBtnRect.contains(px, py)) {
                newHoldFire = true;
            } else if (py < lBtnRect.top - dpf(10f)) {
                // Direct touch-to-aim glide anywhere on upper field
                playerTargetX = Math.max(dpf(20f), Math.min(getWidth() - dpf(20f), px));
            }
        }

        if (newHoldLeft && !isHoldingLeft) {
            try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
        }
        if (newHoldRight && !isHoldingRight) {
            try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
        }
        if (newHoldFire && !isHoldingFire) {
            fireLaser();
        }

        isHoldingLeft = newHoldLeft;
        isHoldingRight = newHoldRight;
        isHoldingFire = newHoldFire;

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        long now = System.currentTimeMillis();
        float dtMs = (lastFrameTime > 0) ? (now - lastFrameTime) : 16.6f;
        if (dtMs > 100f) dtMs = 16.6f; // Clamp pause jumps
        lastFrameTime = now;

        // Apply continuous button gliding (responsive, buttery speed)
        float glideSpeed = dpf(9.5f) * (dtMs / 16.6f);
        if (isHoldingLeft) {
            playerTargetX = Math.max(dpf(20f), playerTargetX - glideSpeed);
        }
        if (isHoldingRight) {
            playerTargetX = Math.min(w - dpf(20f), playerTargetX + glideSpeed);
        }

        // Apply continuous rapid auto-fire on hold (180ms cadence)
        if (isHoldingFire && (now - lastAutoFireTime >= 180)) {
            shootTrigger = true;
            lastAutoFireTime = now;
        }

        // 1. Step Native Rust Simulation
        if (nativeEngine != null && !gameOver) {
            int events = nativeEngine.update(dtMs, playerTargetX, shootTrigger);
            shootTrigger = false;

            // Handle Native Events & Procedural Audio DSP
            if ((events & SpaceInvadersNative.EVENT_MARCH_STEP) != 0) {
                nativeEngine.getState(stateInts, stateFloats);
                int marchNote = stateInts[9];
                SpaceInvadersNative.playProceduralSfx(0, marchNote);
            }
            if ((events & SpaceInvadersNative.EVENT_PLAYER_FIRED) != 0) {
                SpaceInvadersNative.playProceduralSfx(1, 0);
                try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
            }
            if ((events & SpaceInvadersNative.EVENT_ALIEN_KILLED) != 0) {
                SpaceInvadersNative.playProceduralSfx(2, 0);
                triggerExplosion(playerX, fleetBaseY + dpf(30f), 0xFF38BDF8, 16);
            }
            if ((events & SpaceInvadersNative.EVENT_PLAYER_HIT) != 0) {
                SpaceInvadersNative.playProceduralSfx(3, 0);
                triggerExplosion(playerX, h - dpf(85f), 0xFF34D399, 30);
                try { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Exception ignored) {}
            }
            if ((events & SpaceInvadersNative.EVENT_UFO_KILLED) != 0) {
                SpaceInvadersNative.playProceduralSfx(2, 0);
                triggerExplosion(ufoX, dpf(26f), 0xFFEF4444, 28);
                addScorePopup(ufoX, dpf(26f), "+300", 0xFFFFD166);
            }
            if ((events & SpaceInvadersNative.EVENT_WAVE_CLEARED) != 0) {
                updateStatus();
            }
            if ((events & SpaceInvadersNative.EVENT_GAME_OVER) != 0) {
                gameOver = true;
                updateStatus();
            }

            // Sync State
            nativeEngine.getState(stateInts, stateFloats);
            score = stateInts[0];
            highScore = stateInts[1];
            playerLives = stateInts[2];
            wave = stateInts[3];
            gameOver = stateInts[4] != 0;
            gameWon = stateInts[5] != 0;
            isUfoActive = stateInts[6] != 0;
            animFrame = stateInts[8];

            playerX = stateFloats[0];
            fleetBaseX = stateFloats[1];
            fleetBaseY = stateFloats[2];
            ufoX = stateFloats[3];
        }

        // 2. Arcade CRT Bezel & Deep Space Background
        rect.set(0, 0, w, h);
        crtBgPaint.setShader(new RadialGradient(w / 2f, h / 2f, Math.max(w, h) * 0.8f, 0xFF0B132B, 0xFF030712, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), crtBgPaint);
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), borderPaint);

        // 3. Parallax Twinkling Stars
        Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setColor(0xFFFFFFFF);
        for (int i = 0; i < 40; i++) {
            starPaint.setAlpha((int) (255 * starAlpha[i]));
            canvas.drawCircle(starX[i] * w, starY[i] * h, dpf(1.1f), starPaint);
        }

        // 4. Draw Destructible Bunkers from Native Bitmasks
        drawNativeBunkers(canvas, w, h);

        // 5. Draw 55-Invader Bitboard Fleet
        drawNativeAliens(canvas, w, h);

        // 6. Draw Mystery UFO
        if (isUfoActive) {
            drawUfo(canvas, ufoX, dpf(26f));
        }

        // 7. Draw Native Projectiles (Lasers emerge cleanly upward from turret tip)
        if (nativeEngine != null) {
            int projCount = nativeEngine.getProjectiles(projFloats);
            for (int i = 0; i < projCount; i++) {
                int off = i * 4;
                float px = projFloats[off];
                float py = projFloats[off + 1];
                boolean isPlayer = projFloats[off + 3] > 0.5f;

                if (isPlayer) {
                    // Upward neon cyan beam with bright white plasma core
                    canvas.drawLine(px, py + dpf(12f), px, py, laserGlowPaint);
                    canvas.drawLine(px, py + dpf(12f), px, py, laserCorePaint);
                } else {
                    // Jagged alien lightning bolt
                    lightningPath.reset();
                    lightningPath.moveTo(px, py - dpf(10f));
                    lightningPath.lineTo(px - dpf(2.5f), py - dpf(5f));
                    lightningPath.lineTo(px + dpf(2.5f), py);
                    lightningPath.lineTo(px, py + dpf(5f));
                    canvas.drawPath(lightningPath, alienLaserPaint);
                }
            }
        }

        // 8. Draw Explosion Particles
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

        // 9. Draw Floating Score Popups
        if (!popups.isEmpty()) {
            Paint popPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            popPaint.setTextAlign(Paint.Align.CENTER);
            popPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            popPaint.setTextSize(dpf(12f));

            for (int i = popups.size() - 1; i >= 0; i--) {
                ScorePopup sp = popups.get(i);
                sp.y -= dpf(0.8f);
                sp.alpha -= 0.025f;
                if (sp.alpha <= 0) {
                    popups.remove(i);
                } else {
                    popPaint.setColor(sp.color);
                    popPaint.setAlpha((int) (255 * sp.alpha));
                    canvas.drawText(sp.text, sp.x, sp.y, popPaint);
                }
            }
        }

        // 10. Draw Player Cannon (Turret Tip sits precisely at laser spawn altitude)
        drawPlayerCannon(canvas, playerX, h - dpf(85f));

        // 11. CRT Horizontal Scanlines
        for (int y = 0; y < h; y += (int) dpf(4f)) {
            canvas.drawLine(0, y, w, y, crtScanlinePaint);
        }

        // 12. Retro Vector HUD Header & Score
        hudPaint.setTextSize(dpf(10.5f));
        canvas.drawText("SCORE: " + score, dpf(14f), dpf(18f), hudPaint);
        hudPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("HI: " + Math.max(score, highScore), w - dpf(14f), dpf(18f), hudPaint);
        hudPaint.setTextAlign(Paint.Align.LEFT);

        // Bottom Lives Counter (Displayed neatly above the control deck)
        for (int i = 0; i < playerLives; i++) {
            drawMiniCannon(canvas, dpf(14f) + i * dpf(18f), h - dpf(72f));
        }

        // 13. Symmetrical Ergonomic Dual-Thumb Control Deck (Equal width steer buttons)
        drawControlDeck(canvas);

        postInvalidateOnAnimation();
    }

    private void drawControlDeck(Canvas canvas) {
        Paint btnBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint btnBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnBorder.setStyle(Paint.Style.STROKE);
        btnBorder.setStrokeWidth(dpf(1.5f));

        Paint btnText = new Paint(Paint.ANTI_ALIAS_FLAG);
        btnText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        btnText.setTextAlign(Paint.Align.CENTER);

        // Symmetrical Left Steer Button
        btnBg.setColor(isHoldingLeft ? 0x5510B981 : 0x2210B981);
        btnBorder.setColor(isHoldingLeft ? 0xFF10B981 : 0x6610B981);
        btnText.setColor(isHoldingLeft ? 0xFFFFFFFF : 0xFF34D399);
        btnText.setTextSize(dpf(12f));
        canvas.drawRoundRect(lBtnRect, dpf(8f), dpf(8f), btnBg);
        canvas.drawRoundRect(lBtnRect, dpf(8f), dpf(8f), btnBorder);
        canvas.drawText("◀ LEFT", lBtnRect.centerX(), lBtnRect.centerY() + dpf(4.5f), btnText);

        // Symmetrical Right Steer Button (Exact equal width to Left)
        btnBg.setColor(isHoldingRight ? 0x5510B981 : 0x2210B981);
        btnBorder.setColor(isHoldingRight ? 0xFF10B981 : 0x6610B981);
        btnText.setColor(isHoldingRight ? 0xFFFFFFFF : 0xFF34D399);
        canvas.drawRoundRect(rBtnRect, dpf(8f), dpf(8f), btnBg);
        canvas.drawRoundRect(rBtnRect, dpf(8f), dpf(8f), btnBorder);
        canvas.drawText("RIGHT ▶", rBtnRect.centerX(), rBtnRect.centerY() + dpf(4.5f), btnText);

        // Rapid Fire Action Button (Glowing Neon Coral)
        btnBg.setColor(isHoldingFire ? 0x66EF4444 : 0x28EF4444);
        btnBorder.setColor(isHoldingFire ? 0xFFF43F5E : 0x88EF4444);
        btnText.setColor(isHoldingFire ? 0xFFFFFFFF : 0xFFF43F5E);
        btnText.setTextSize(dpf(13f));
        canvas.drawRoundRect(fBtnRect, dpf(8f), dpf(8f), btnBg);
        canvas.drawRoundRect(fBtnRect, dpf(8f), dpf(8f), btnBorder);
        canvas.drawText("⚡ RAPID FIRE", fBtnRect.centerX(), fBtnRect.centerY() + dpf(4.5f), btnText);
    }

    private void drawNativeAliens(Canvas canvas, int w, int h) {
        if (nativeEngine == null) return;
        float alienColW = (w - dpf(40f)) / 11f;
        float alienRowH = dpf(18f);

        for (int r = 0; r < 5; r++) {
            int mask = nativeEngine.getAlienRowMask(r);
            int color = (r == 0) ? 0xFFF43F5E : (r <= 2 ? 0xFF38BDF8 : 0xFFFDE047);
            alienPaint.setColor(color);

            for (int c = 0; c < 11; c++) {
                if ((mask & (1 << c)) != 0) {
                    float ax = fleetBaseX + c * alienColW + alienColW / 2f;
                    float ay = fleetBaseY + r * alienRowH + alienRowH / 2f;
                    drawInvaderSprite(canvas, ax, ay, dpf(5.5f), r, animFrame);
                }
            }
        }
    }

    private void drawInvaderSprite(Canvas canvas, float cx, float cy, float s, int row, int frame) {
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

    private void drawNativeBunkers(Canvas canvas, int w, int h) {
        if (nativeEngine == null) return;
        float bunkerY = h - dpf(145f);
        float bWidth = dpf(44f);
        float bHeight = dpf(24f);
        float bGap = (w - bWidth * 3) / 4f;
        float pw = bWidth / 12f;
        float ph = bHeight / 8f;

        for (int b = 0; b < 3; b++) {
            nativeEngine.getBunkerGrid(b, bunkerGrid);
            float bLeft = bGap + b * (bWidth + bGap);
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 12; c++) {
                    if (bunkerGrid[r * 12 + c] != 0) {
                        canvas.drawRect(bLeft + c * pw, bunkerY + r * ph, bLeft + (c + 1) * pw, bunkerY + (r + 1) * ph, bunkerPaint);
                    }
                }
            }
        }
    }

    private void drawPlayerCannon(Canvas canvas, float cx, float cy) {
        float w = dpf(26f);
        float h = dpf(12f);
        // Base Chassis
        rect.set(cx - w / 2f, cy, cx + w / 2f, cy + h);
        canvas.drawRoundRect(rect, dpf(3f), dpf(3f), playerPaint);
        // Gun Turret Barrel
        rect.set(cx - dpf(3f), cy - dpf(10f), cx + dpf(3f), cy);
        canvas.drawRect(rect, playerPaint);
        // Muzzle Tip (Gold)
        rect.set(cx - dpf(2f), cy - dpf(11f), cx + dpf(2f), cy - dpf(9f));
        canvas.drawRect(rect, playerTurretPaint);
    }

    private void drawMiniCannon(Canvas canvas, float cx, float cy) {
        rect.set(cx - dpf(6f), cy, cx + dpf(6f), cy + dpf(5f));
        canvas.drawRect(rect, playerPaint);
        rect.set(cx - dpf(1.5f), cy - dpf(3f), cx + dpf(1.5f), cy);
        canvas.drawRect(rect, playerPaint);
    }
}