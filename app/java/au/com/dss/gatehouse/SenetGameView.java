package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * SenetGameView — Ancient Egyptian Senet (c. 3100 BCE).
 * Powered by Native Rust Expectimax Engine (libgatehouse_senet.so).
 *
 * Enhanced Visual Systems:
 * - Parabolic Bezier Leap Interpolation & Dynamic Piece Shadow Elevation
 * - Living Nile Azure Caustics on Square 27 (House of Water 𓈗)
 * - Radiant Solar Rays on Square 30 (House of Horus / Ascension 𓁐)
 * - 4-Stick 3D Tumbling Angles & Bone Clack Acoustics
 * - Stochastic Expectimax Win-Rate Telemetry Ribbon
 */
public class SenetGameView extends View {

    public interface StatusListener {
        void onStatusChanged(String text, int color);
    }

    private StatusListener statusListener;
    private final Paint boardFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goldDetailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specialTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint piecePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pieceShinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hieroglyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stickLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stickDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trayBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pathRibbonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trajectoryArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint targetRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moveGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint causticPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF rect = new RectF();
    private final RectF tileRect = new RectF();
    private final Random rand = new Random();

    private SenetNative nativeEngine;
    private int difficultyTier = 1; // 0 = Scribe, 1 = Priest, 2 = Anubis
    private float currentWinrate = 0.50f;

    private int currentTurn = 0; // 0 = Pharaoh (You), 1 = Anubis Bot
    private int currentRoll = -1;
    private boolean waitingForRoll = true;
    private int whiteBorneOff = 0;
    private int blackBorneOff = 0;
    private int selectedPieceIdx = -1;
    private boolean showGuide = false;

    private final int[] whitePieces = new int[5];
    private final int[] blackPieces = new int[5];
    private final boolean[] lastSticksLight = new boolean[4];
    private final float[] stickAngles = new float[4];

    // Smooth Quadratic Bezier Piece Leap Animation
    private static class LeapAnim {
        int pieceIdx;
        boolean isWhite;
        float startX, startY;
        float endX, endY;
        long startTime;
        long duration = 240; // ms
        boolean active = false;
    }
    private final LeapAnim leapAnim = new LeapAnim();

    // Texture cache for native procedural shaders
    private Bitmap cachedPharaohPieceBmp = null;
    private Bitmap cachedAnubisPieceBmp = null;
    private Bitmap cachedDarkTileBmp = null;
    private Bitmap cachedLightTileBmp = null;
    private Bitmap cachedSacredTileBmp = null;
    private int cachedTileW = 0, cachedTileH = 0;
    private int cachedPiecePx = 0;

    // 30-Square Serpentine Path (Boustrophedon S-Track)
    private static final int[][] SENET_PATH = {
        {0,0}, {0,1}, {0,2}, {0,3}, {0,4}, {0,5}, {0,6}, {0,7}, {0,8}, {0,9},
        {1,9}, {1,8}, {1,7}, {1,6}, {1,5}, {1,4}, {1,3}, {1,2}, {1,1}, {1,0},
        {2,0}, {2,1}, {2,2}, {2,3}, {2,4}, {2,5}, {2,6}, {2,7}, {2,8}, {2,9}
    };

    private static class HistoryState {
        final int[] whitePieces;
        final int[] blackPieces;
        final int whiteBorneOff;
        final int blackBorneOff;
        final int currentTurn;
        final int currentRoll;
        final boolean waitingForRoll;
        final boolean[] lastSticksLight;

        HistoryState(int[] wp, int[] bp, int wOff, int bOff, int turn, int roll, boolean wait, boolean[] sticks) {
            this.whitePieces = wp.clone();
            this.blackPieces = bp.clone();
            this.whiteBorneOff = wOff;
            this.blackBorneOff = bOff;
            this.currentTurn = turn;
            this.currentRoll = roll;
            this.waitingForRoll = wait;
            this.lastSticksLight = sticks.clone();
        }
    }

    private final List<HistoryState> history = new ArrayList<>();

    private float dpf(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    public SenetGameView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);

        nativeEngine = new SenetNative(difficultyTier);

        goldBorderPaint.setColor(0xFFEAB308);
        goldBorderPaint.setStyle(Paint.Style.STROKE);
        goldBorderPaint.setStrokeWidth(dpf(1.8f));

        goldDetailPaint.setColor(0xFFFDE047);
        goldDetailPaint.setStyle(Paint.Style.STROKE);
        goldDetailPaint.setStrokeWidth(dpf(1f));

        shadowPaint.setColor(0x99000000);
        shadowPaint.setStyle(Paint.Style.FILL);

        pieceRimPaint.setColor(0xFFFFFFFF);
        pieceRimPaint.setStyle(Paint.Style.STROKE);
        pieceRimPaint.setStrokeWidth(dpf(1.4f));

        pieceShinePaint.setColor(0xAAFFFFFF);
        pieceShinePaint.setStyle(Paint.Style.FILL);

        moveGlowPaint.setColor(0xFFFDE047);
        moveGlowPaint.setStyle(Paint.Style.STROKE);
        moveGlowPaint.setStrokeWidth(dpf(2.5f));

        textPaint.setColor(0xFFE2E8F0);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        hieroglyphPaint.setColor(0xFFFDE047);
        hieroglyphPaint.setTextAlign(Paint.Align.CENTER);
        hieroglyphPaint.setTypeface(Typeface.DEFAULT_BOLD);

        stickLightPaint.setColor(0xFFFEF08A);
        stickLightPaint.setStyle(Paint.Style.FILL);

        stickDarkPaint.setColor(0xFF451A03);
        stickDarkPaint.setStyle(Paint.Style.FILL);

        trayBgPaint.setColor(0xFF0F172A);
        trayBgPaint.setStyle(Paint.Style.FILL);

        pathRibbonPaint.setStyle(Paint.Style.STROKE);
        pathRibbonPaint.setStrokeWidth(dpf(2.2f));
        pathRibbonPaint.setColor(0x33EAB308);

        trajectoryArcPaint.setStyle(Paint.Style.STROKE);
        trajectoryArcPaint.setStrokeWidth(dpf(3.0f));
        trajectoryArcPaint.setColor(0xFFFFD166);

        targetRingPaint.setStyle(Paint.Style.STROKE);
        targetRingPaint.setStrokeWidth(dpf(2.4f));

        causticPaint.setStyle(Paint.Style.FILL);

        resetGame();
    }

    public void setDifficultyTier(int tier) {
        this.difficultyTier = Math.max(0, Math.min(2, tier));
        if (nativeEngine != null) {
            nativeEngine.release();
        }
        nativeEngine = new SenetNative(this.difficultyTier);
        updateStatus();
    }

    public int getDifficultyTier() {
        return difficultyTier;
    }

    public void setStatusListener(StatusListener l) {
        this.statusListener = l;
        updateStatus();
    }

    public void togglePathGuide() {
        showGuide = !showGuide;
        try { performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); } catch (Exception ignored) {}
        if (statusListener != null) {
            statusListener.onStatusChanged(showGuide ? "🗺️ S-Track Path & Sacred Houses Guide Active" : "🗺️ Guide Hidden", 0xFF10B981);
        }
        invalidate();
    }

    public boolean isGuideActive() {
        return showGuide;
    }

    private void saveHistory() {
        history.add(new HistoryState(whitePieces, blackPieces, whiteBorneOff, blackBorneOff, currentTurn, currentRoll, waitingForRoll, lastSticksLight));
        if (history.size() > 50) history.remove(0);
    }

    public void undoMove() {
        if (history.isEmpty()) return;
        HistoryState state = history.remove(history.size() - 1);
        System.arraycopy(state.whitePieces, 0, whitePieces, 0, 5);
        System.arraycopy(state.blackPieces, 0, blackPieces, 0, 5);
        whiteBorneOff = state.whiteBorneOff;
        blackBorneOff = state.blackBorneOff;
        currentTurn = state.currentTurn;
        currentRoll = state.currentRoll;
        waitingForRoll = state.waitingForRoll;
        selectedPieceIdx = -1;
        leapAnim.active = false;
        System.arraycopy(state.lastSticksLight, 0, lastSticksLight, 0, 4);
        if (nativeEngine != null) {
            nativeEngine.reset();
        }
        try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
        updateStatus();
        invalidate();
    }

    public void resetGame() {
        history.clear();
        currentTurn = 0;
        currentRoll = -1;
        waitingForRoll = true;
        whiteBorneOff = 0;
        blackBorneOff = 0;
        selectedPieceIdx = -1;
        leapAnim.active = false;
        currentWinrate = 0.50f;

        if (nativeEngine != null) {
            nativeEngine.reset();
        }

        for (int i = 0; i < 5; i++) {
            whitePieces[i] = i * 2;
            blackPieces[i] = i * 2 + 1;
        }
        for (int i = 0; i < 4; i++) {
            lastSticksLight[i] = false;
            stickAngles[i] = (rand.nextFloat() - 0.5f) * 14f;
        }
        updateStatus();
        invalidate();
    }

    private boolean isCasting = false;

    public void throwSticks() {
        if (!waitingForRoll || isCasting || leapAnim.active) return;
        isCasting = true;
        selectedPieceIdx = -1;
        try {
            RecreationAudioSynth.playDiceRoll();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        } catch (Exception ignored) {}

        animateSticksTumble(0);
    }

    private void animateSticksTumble(final int step) {
        if (step < 5) {
            for (int i = 0; i < 4; i++) {
                lastSticksLight[i] = rand.nextBoolean();
                stickAngles[i] = (rand.nextFloat() - 0.5f) * 22f;
            }
            invalidate();
            postDelayed(new Runnable() {
                public void run() {
                    animateSticksTumble(step + 1);
                }
            }, 55);
        } else {
            int lightSides = 0;
            for (int i = 0; i < 4; i++) {
                boolean isLight = rand.nextBoolean();
                lastSticksLight[i] = isLight;
                stickAngles[i] = (rand.nextFloat() - 0.5f) * 12f;
                if (isLight) lightSides++;
            }
            currentRoll = (lightSides == 0) ? 5 : lightSides;
            waitingForRoll = false;
            isCasting = false;

            try {
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            } catch (Exception ignored) {}

            // Auto-select single movable piece if only 1 is available
            int movableCount = 0;
            int onlyMovable = -1;
            if (currentTurn == 0) {
                for (int i = 0; i < 5; i++) {
                    if (isPieceMovable(i)) {
                        movableCount++;
                        onlyMovable = i;
                    }
                }
                if (movableCount == 1) {
                    selectedPieceIdx = onlyMovable;
                }
            }

            if (!hasLegalMoves(currentTurn, currentRoll)) {
                postDelayed(new Runnable() {
                    public void run() { passTurn(); }
                }, 750);
            } else {
                if (currentTurn == 1) {
                    postDelayed(new Runnable() {
                        public void run() { botExecuteMove(); }
                    }, 550);
                }
            }
            updateStatus();
            invalidate();
        }
    }

    private boolean hasLegalMoves(int turn, int roll) {
        if (nativeEngine != null && SenetNative.isAvailable()) {
            int mask = nativeEngine.getLegalMovesMask(roll);
            return mask != 0;
        }

        int[] my = (turn == 0) ? whitePieces : blackPieces;
        int[] opp = (turn == 0) ? blackPieces : whitePieces;

        for (int i = 0; i < 5; i++) {
            int pos = my[i];
            if (pos == 30) continue;
            int next = pos + roll;
            if (next > 30) continue;
            if (next == 30) return true;

            boolean ownBlock = false;
            for (int j = 0; j < 5; j++) {
                if (my[j] == next) { ownBlock = true; break; }
            }
            if (ownBlock) continue;

            boolean oppProtected = false;
            for (int j = 0; j < 5; j++) {
                if (opp[j] == next) {
                    for (int k = 0; k < 5; k++) {
                        if (opp[k] == next - 1 || opp[k] == next + 1) {
                            oppProtected = true;
                            break;
                        }
                    }
                }
            }
            if (oppProtected) continue;

            return true;
        }
        return false;
    }

    private boolean isPieceMovable(int pieceIdx) {
        if (waitingForRoll || currentRoll <= 0 || currentTurn != 0 || leapAnim.active) return false;
        int pos = whitePieces[pieceIdx];
        if (pos == 30) return false;
        int next = pos + currentRoll;
        if (next > 30) return false;
        if (next == 30) return true;

        for (int j = 0; j < 5; j++) {
            if (whitePieces[j] == next) return false;
        }

        for (int j = 0; j < 5; j++) {
            if (blackPieces[j] == next) {
                for (int k = 0; k < 5; k++) {
                    if (blackPieces[k] == next - 1 || blackPieces[k] == next + 1) return false;
                }
            }
        }
        return true;
    }

    private void makeMove(final int pieceIdx) {
        if (waitingForRoll || currentRoll <= 0 || leapAnim.active) return;
        if (currentTurn == 0) saveHistory();

        final int[] my = (currentTurn == 0) ? whitePieces : blackPieces;
        final int[] opp = (currentTurn == 0) ? blackPieces : whitePieces;

        final int pos = my[pieceIdx];
        if (pos == 30) return;
        int target = pos + currentRoll;
        if (target > 30) return;

        // Calculate Pixel Start & End Coordinates for Parabolic Animation
        float pad = dpf(8f);
        float cellW = (getWidth() - pad * 2) / 10f;
        float cellH = (getHeight() - dpf(56f)) / 3f;

        leapAnim.pieceIdx = pieceIdx;
        leapAnim.isWhite = (currentTurn == 0);
        leapAnim.startX = pad + SENET_PATH[pos][1] * cellW + cellW / 2f;
        leapAnim.startY = dpf(6f) + SENET_PATH[pos][0] * cellH + cellH / 2f;

        if (target < 30) {
            leapAnim.endX = pad + SENET_PATH[target][1] * cellW + cellW / 2f;
            leapAnim.endY = dpf(6f) + SENET_PATH[target][0] * cellH + cellH / 2f;
        } else {
            leapAnim.endX = leapAnim.startX;
            leapAnim.endY = leapAnim.startY - dpf(40f);
        }
        leapAnim.startTime = SystemClock.uptimeMillis();
        leapAnim.active = true;

        if (nativeEngine != null && SenetNative.isAvailable()) {
            nativeEngine.playMove(pieceIdx, currentRoll);
        }

        // Swap opponent piece back
        for (int j = 0; j < 5; j++) {
            if (opp[j] == target) {
                opp[j] = pos;
                try {
                    RecreationAudioSynth.playChessPieceThud(true);
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {}
                break;
            }
        }

        // House of Water (Square 27, Index 26) drowning trap -> sends piece back to House of Rebirth (Square 15, Index 14)
        if (target == 26) {
            target = 14;
            boolean occupied = true;
            while (occupied && target >= 0) {
                occupied = false;
                for (int p : my) if (p == target) occupied = true;
                for (int p : opp) if (p == target) occupied = true;
                if (occupied) target--;
            }
        }

        final int finalDst = target;
        my[pieceIdx] = finalDst;
        selectedPieceIdx = -1;

        if (finalDst == 30) {
            try {
                RecreationAudioSynth.playTetrisLineClear();
                performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            } catch (Exception ignored) {}
            if (currentTurn == 0) whiteBorneOff++;
            else blackBorneOff++;
        } else {
            try {
                RecreationAudioSynth.playBadukStoneClack();
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            } catch (Exception ignored) {}
        }

        // Check victory
        if (whiteBorneOff == 5 || blackBorneOff == 5) {
            String winner = (whiteBorneOff == 5) ? "👑 PHARAOH REACHES ETERNITY!" : "🔵 ANUBIS CLAIMS THE UNDERWORLD!";
            if (statusListener != null) {
                statusListener.onStatusChanged(winner, 0xFF10B981);
            }
            invalidate();
            return;
        }

        // Bonus roll rule: 1, 4, or 5 grants an immediate extra roll!
        boolean bonusRoll = (currentRoll == 1 || currentRoll == 4 || currentRoll == 5);
        if (bonusRoll) {
            waitingForRoll = true;
            currentRoll = -1;
            updateStatus();
            invalidate();
            if (currentTurn == 1) {
                postDelayed(new Runnable() {
                    public void run() { throwSticks(); }
                }, 450);
            }
        } else {
            passTurn();
        }
    }

    private void passTurn() {
        currentTurn = (currentTurn == 0) ? 1 : 0;
        waitingForRoll = true;
        currentRoll = -1;
        selectedPieceIdx = -1;
        updateStatus();
        invalidate();

        if (currentTurn == 1) {
            postDelayed(new Runnable() {
                public void run() { throwSticks(); }
            }, 450);
        }
    }

    private void botExecuteMove() {
        if (currentTurn != 1 || waitingForRoll || currentRoll <= 0 || leapAnim.active) return;

        int bestPiece = -1;

        if (nativeEngine != null && SenetNative.isAvailable()) {
            SenetNative.MoveResult mr = nativeEngine.findBestMove(currentRoll, difficultyTier);
            bestPiece = mr.bestPieceIdx;
            currentWinrate = mr.winrate;
        } else {
            // Fallback heuristic
            int bestScore = -9999;
            for (int i = 0; i < 5; i++) {
                int pos = blackPieces[i];
                if (pos == 30) continue;
                int next = pos + currentRoll;
                if (next > 30) continue;

                boolean ownBlock = false;
                for (int j = 0; j < 5; j++) {
                    if (blackPieces[j] == next) { ownBlock = true; break; }
                }
                if (ownBlock) continue;

                boolean oppProtected = false;
                boolean isAttack = false;
                for (int j = 0; j < 5; j++) {
                    if (whitePieces[j] == next) {
                        isAttack = true;
                        for (int k = 0; k < 5; k++) {
                            if (whitePieces[k] == next - 1 || whitePieces[k] == next + 1) {
                                oppProtected = true;
                                break;
                            }
                        }
                    }
                }
                if (oppProtected) continue;

                int score = next * 2;
                if (next == 30) score += 60;
                if (isAttack) score += 35;
                if (next == 25) score += 20;
                if (next == 26) score -= 45;

                if (score > bestScore) {
                    bestScore = score;
                    bestPiece = i;
                }
            }
        }

        if (bestPiece != -1) {
            makeMove(bestPiece);
        } else {
            passTurn();
        }
    }

    private void updateStatus() {
        if (statusListener == null) return;
        if (whiteBorneOff == 5 || blackBorneOff == 5) return;

        String tierName = (difficultyTier == 0 ? "🌱 Scribe" : (difficultyTier == 1 ? "⚖️ Priest" : "👑 Anubis"));
        String turnStr = (currentTurn == 0) ? "🟡 Pharaoh (You)" : ("🔵 " + tierName);
        String rollStr = waitingForRoll ? "· Tap to Cast" : ("· Cast: " + currentRoll + (currentRoll == 1 || currentRoll >= 4 ? " (Bonus ⚡)" : ""));
        String scoreStr = " [𓁐 " + whiteBorneOff + "/5 vs " + blackBorneOff + "/5]";
        statusListener.onStatusChanged(turnStr + " " + rollStr + scoreStr, 0xFFFFD166);
    }

    private void ensureNativeBitmaps(int tileW, int tileH, int pieceR) {
        if (tileW <= 0 || tileH <= 0 || pieceR <= 0) return;
        int piecePx = pieceR * 2;

        if (cachedTileW != tileW || cachedTileH != tileH || cachedDarkTileBmp == null) {
            cachedTileW = tileW;
            cachedTileH = tileH;
            try {
                cachedDarkTileBmp = SenetNative.renderTileBitmap(tileW, tileH, 0);
                cachedLightTileBmp = SenetNative.renderTileBitmap(tileW, tileH, 1);
                cachedSacredTileBmp = SenetNative.renderTileBitmap(tileW, tileH, 2);
            } catch (Throwable ignored) {}
        }

        if (cachedPiecePx != piecePx || cachedPharaohPieceBmp == null) {
            cachedPiecePx = piecePx;
            try {
                cachedPharaohPieceBmp = SenetNative.renderPieceBitmap(piecePx, piecePx, true);
                cachedAnubisPieceBmp = SenetNative.renderPieceBitmap(piecePx, piecePx, false);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (action == MotionEvent.ACTION_DOWN) {
            if (leapAnim.active) return true;

            float w = getWidth();
            float pad = dpf(8f);
            float cellW = (w - pad * 2) / 10f;
            float cellH = (getHeight() - dpf(56f)) / 3f;

            float ex = event.getX() - pad;
            float ey = event.getY() - dpf(6f);

            // Bottom Casting Tray Tap
            if (event.getY() > getHeight() - dpf(48f)) {
                if (waitingForRoll && currentTurn == 0) {
                    throwSticks();
                    return true;
                }
            }

            if (waitingForRoll && currentTurn == 0) {
                throwSticks();
                return true;
            }

            if (!waitingForRoll && currentTurn == 0 && currentRoll > 0) {
                int c = (int) (ex / cellW);
                int r = (int) (ey / cellH);

                if (c >= 0 && c < 10 && r >= 0 && r < 3) {
                    for (int i = 0; i < 5; i++) {
                        int pos = whitePieces[i];
                        if (pos >= 0 && pos < 30) {
                            int pr = SENET_PATH[pos][0];
                            int pc = SENET_PATH[pos][1];
                            if (pr == r && pc == c && isPieceMovable(i)) {
                                if (selectedPieceIdx == i) {
                                    makeMove(i);
                                } else {
                                    selectedPieceIdx = i;
                                    try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
                                    invalidate();
                                }
                                return true;
                            }
                        }
                    }

                    if (selectedPieceIdx != -1 && isPieceMovable(selectedPieceIdx)) {
                        int targetPos = whitePieces[selectedPieceIdx] + currentRoll;
                        if (targetPos < 30) {
                            int tr = SENET_PATH[targetPos][0];
                            int tc = SENET_PATH[targetPos][1];
                            if (tr == r && tc == c) {
                                makeMove(selectedPieceIdx);
                                return true;
                            }
                        } else if (targetPos == 30) {
                            makeMove(selectedPieceIdx);
                            return true;
                        }
                    }
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

        long now = SystemClock.uptimeMillis();

        // Sandstone & Royal Egyptian Obsidian Border Frame
        rect.set(0, 0, w, h);
        boardFramePaint.setShader(new LinearGradient(0, 0, w, h, 0xFF181512, 0xFF2A0845, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardFramePaint);

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        float pad = dpf(8f);
        float cellW = (w - pad * 2) / 10f;
        float cellH = (h - dpf(56f)) / 3f;
        int pieceR = (int) (Math.min(cellW, cellH) * 0.38f);

        ensureNativeBitmaps((int) cellW, (int) cellH, pieceR);

        // 1. Draw Continuous Serpentine S-Curve Path Ribbon Underneath Tiles
        Path ribbonPath = new Path();
        for (int i = 0; i < 30; i++) {
            int r = SENET_PATH[i][0];
            int c = SENET_PATH[i][1];
            float cx = pad + c * cellW + cellW / 2f;
            float cy = dpf(6f) + r * cellH + cellH / 2f;
            if (i == 0) ribbonPath.moveTo(cx, cy);
            else ribbonPath.lineTo(cx, cy);
        }
        canvas.drawPath(ribbonPath, pathRibbonPaint);

        // 2. Draw 30 Egyptian Sandstone Tiles with Procedural Textures & Sacred Houses
        for (int i = 0; i < 30; i++) {
            int r = SENET_PATH[i][0];
            int c = SENET_PATH[i][1];

            float left = pad + c * cellW;
            float top = dpf(6f) + r * cellH;
            tileRect.set(left + dpf(1.5f), top + dpf(1.5f), left + cellW - dpf(1.5f), top + cellH - dpf(1.5f));

            boolean isSpecial = (i == 14 || i >= 25);
            Bitmap tileBmp = isSpecial ? cachedSacredTileBmp : (((r + c) % 2 == 0) ? cachedLightTileBmp : cachedDarkTileBmp);

            if (tileBmp != null) {
                canvas.drawBitmap(tileBmp, null, tileRect, null);
            } else if (isSpecial) {
                int col1 = (i == 26) ? 0xFF0284C7 : (i == 14 ? 0xFF059669 : 0xFF6B21A8);
                int col2 = (i == 26) ? 0xFF082F49 : (i == 14 ? 0xFF064E3B : 0xFF3B0764);
                specialTilePaint.setShader(new RadialGradient(tileRect.centerX(), tileRect.centerY(), cellW * 0.85f, col1, col2, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(tileRect, dpf(5f), dpf(5f), specialTilePaint);
            } else {
                boolean isLight = (r + c) % 2 == 0;
                tileLightPaint.setColor(isLight ? 0xFF1E293B : 0xFF0F172A);
                canvas.drawRoundRect(tileRect, dpf(5f), dpf(5f), tileLightPaint);
            }

            // Living Nile Caustics on Square 27 (House of Water)
            if (i == 26) {
                float wave = (float) Math.sin(now / 180.0) * dpf(4f);
                causticPaint.setColor(0x4438BDF8);
                canvas.drawCircle(tileRect.centerX() + wave, tileRect.centerY(), cellW * 0.35f, causticPaint);
            }

            canvas.drawRoundRect(tileRect, dpf(5f), dpf(5f), goldDetailPaint);

            // Subtle square numbering (1..30) in top-left
            textPaint.setTextSize(dpf(7f));
            textPaint.setColor(0x88FDE047);
            canvas.drawText(String.valueOf(i + 1), left + dpf(7f), top + dpf(9f), textPaint);

            // Sacred House Hieroglyphic Badges
            hieroglyphPaint.setTextSize(dpf(8.5f));
            if (i == 14) { // House of Rebirth (15)
                hieroglyphPaint.setColor(0xFF34D399);
                canvas.drawText("𓋹 15", tileRect.centerX(), tileRect.centerY() + dpf(2f), hieroglyphPaint);
                textPaint.setTextSize(dpf(6f));
                textPaint.setColor(0xCC34D399);
                canvas.drawText("REBIRTH", tileRect.centerX(), tileRect.bottom - dpf(3f), textPaint);
            } else if (i == 25) { // House of Beauty (26)
                hieroglyphPaint.setColor(0xFFFDE047);
                canvas.drawText("𓄤 26", tileRect.centerX(), tileRect.centerY() + dpf(2f), hieroglyphPaint);
                textPaint.setTextSize(dpf(6f));
                textPaint.setColor(0xCCFDE047);
                canvas.drawText("BEAUTY", tileRect.centerX(), tileRect.bottom - dpf(3f), textPaint);
            } else if (i == 26) { // House of Water (27)
                hieroglyphPaint.setColor(0xFF38BDF8);
                canvas.drawText("𓈗 27", tileRect.centerX(), tileRect.centerY() + dpf(2f), hieroglyphPaint);
                textPaint.setTextSize(dpf(6f));
                textPaint.setColor(0xCC38BDF8);
                canvas.drawText("WATER ⚠️", tileRect.centerX(), tileRect.bottom - dpf(3f), textPaint);
            } else if (i == 27) { // 3 Truths (28)
                hieroglyphPaint.setColor(0xFFFDE047);
                canvas.drawText("𓏺 28", tileRect.centerX(), tileRect.centerY() + dpf(2f), hieroglyphPaint);
                textPaint.setTextSize(dpf(6f));
                textPaint.setColor(0xCCFDE047);
                canvas.drawText("NEED 3", tileRect.centerX(), tileRect.bottom - dpf(3f), textPaint);
            } else if (i == 28) { // Re-Atum (29)
                hieroglyphPaint.setColor(0xFFFDE047);
                canvas.drawText("𓏻 29", tileRect.centerX(), tileRect.centerY() + dpf(2f), hieroglyphPaint);
                textPaint.setTextSize(dpf(6f));
                textPaint.setColor(0xCCFDE047);
                canvas.drawText("NEED 2", tileRect.centerX(), tileRect.bottom - dpf(3f), textPaint);
            } else if (i == 29) { // House of Horus / Ascension (30)
                hieroglyphPaint.setColor(0xFFFDE047);
                canvas.drawText("𓁐 30", tileRect.centerX(), tileRect.centerY() + dpf(2f), hieroglyphPaint);
                textPaint.setTextSize(dpf(6f));
                textPaint.setColor(0xCCFDE047);
                canvas.drawText("EXIT 👑", tileRect.centerX(), tileRect.bottom - dpf(3f), textPaint);
            }
        }

        // 3. Draw Selected Piece Parabolic Jump Arc & Target Landing Indicator
        if (selectedPieceIdx != -1 && currentTurn == 0 && currentRoll > 0 && isPieceMovable(selectedPieceIdx) && !leapAnim.active) {
            int srcPos = whitePieces[selectedPieceIdx];
            int dstPos = srcPos + currentRoll;

            float srcX = pad + SENET_PATH[srcPos][1] * cellW + cellW / 2f;
            float srcY = dpf(6f) + SENET_PATH[srcPos][0] * cellH + cellH / 2f;

            if (dstPos < 30) {
                float dstX = pad + SENET_PATH[dstPos][1] * cellW + cellW / 2f;
                float dstY = dpf(6f) + SENET_PATH[dstPos][0] * cellH + cellH / 2f;

                Path jumpArc = new Path();
                jumpArc.moveTo(srcX, srcY);
                float midX = (srcX + dstX) / 2f;
                float midY = Math.min(srcY, dstY) - dpf(24f);
                jumpArc.quadTo(midX, midY, dstX, dstY);
                canvas.drawPath(jumpArc, trajectoryArcPaint);

                boolean isEnemySwap = false;
                for (int bp : blackPieces) {
                    if (bp == dstPos) { isEnemySwap = true; break; }
                }

                targetRingPaint.setColor(isEnemySwap ? 0xFFEF4444 : 0xFF10B981);
                canvas.drawCircle(dstX, dstY, cellW * 0.42f, targetRingPaint);

                textPaint.setTextSize(dpf(8f));
                textPaint.setColor(isEnemySwap ? 0xFFEF4444 : 0xFF10B981);
                canvas.drawText(isEnemySwap ? "⚔️ SWAP" : "✓ LAND", dstX, dstY - dpf(12f), textPaint);
            } else if (dstPos == 30) {
                targetRingPaint.setColor(0xFFFFD166);
                canvas.drawCircle(srcX, srcY, cellW * 0.48f, targetRingPaint);
                textPaint.setTextSize(dpf(8.5f));
                textPaint.setColor(0xFFFFD166);
                canvas.drawText("👑 ASCEND (+1)", srcX, srcY - dpf(16f), textPaint);
            }
        }

        // 4. Draw 3D Conical Crowns (Pharaoh) & Lapis Spools (Anubis)
        for (int i = 0; i < 5; i++) {
            // White / Pharaoh Pieces
            if (!(leapAnim.active && leapAnim.isWhite && leapAnim.pieceIdx == i)) {
                int wp = whitePieces[i];
                if (wp >= 0 && wp < 30) {
                    int r = SENET_PATH[wp][0];
                    int c = SENET_PATH[wp][1];
                    float cx = pad + c * cellW + cellW / 2f;
                    float cy = dpf(6f) + r * cellH + cellH / 2f;

                    boolean movable = (currentTurn == 0 && !waitingForRoll && currentRoll > 0 && isPieceMovable(i) && !leapAnim.active);
                    boolean selected = (selectedPieceIdx == i);

                    if (selected) {
                        moveGlowPaint.setColor(0xFFFFD166);
                        moveGlowPaint.setStrokeWidth(dpf(3.2f));
                        canvas.drawCircle(cx, cy, pieceR + dpf(5f), moveGlowPaint);
                    } else if (movable) {
                        float pulse = 0.55f + 0.45f * (float) Math.sin(now / 160.0);
                        moveGlowPaint.setColor(0xFFFDE047);
                        moveGlowPaint.setAlpha((int) (240 * pulse));
                        moveGlowPaint.setStrokeWidth(dpf(2.4f));
                        canvas.drawCircle(cx, cy, pieceR + dpf(3.8f), moveGlowPaint);
                    }

                    drawPieceWithShader(canvas, cx, cy, pieceR, true);
                }
            }

            // Black / Anubis Pieces
            if (!(leapAnim.active && !leapAnim.isWhite && leapAnim.pieceIdx == i)) {
                int bp = blackPieces[i];
                if (bp >= 0 && bp < 30) {
                    int r = SENET_PATH[bp][0];
                    int c = SENET_PATH[bp][1];
                    float cx = pad + c * cellW + cellW / 2f;
                    float cy = dpf(6f) + r * cellH + cellH / 2f;
                    drawPieceWithShader(canvas, cx, cy, pieceR, false);
                }
            }
        }

        // 5. Draw Animated Quadratic Bezier Leap Piece
        if (leapAnim.active) {
            long elapsed = now - leapAnim.startTime;
            float t = Math.min(1f, (float) elapsed / (float) leapAnim.duration);

            float midX = (leapAnim.startX + leapAnim.endX) / 2f;
            float midY = Math.min(leapAnim.startY, leapAnim.endY) - dpf(28f);

            // Quadratic Bezier Interpolation: B(t) = (1-t)^2 P0 + 2(1-t)t P1 + t^2 P2
            float u = 1f - t;
            float curX = u * u * leapAnim.startX + 2 * u * t * midX + t * t * leapAnim.endX;
            float curY = u * u * leapAnim.startY + 2 * u * t * midY + t * t * leapAnim.endY;

            // Elevated Shadow below moving piece
            float shadowScale = 1f + 0.35f * (float) Math.sin(t * Math.PI);
            canvas.drawCircle(curX, curY + dpf(6f) * shadowScale, pieceR * shadowScale, shadowPaint);

            drawPieceWithShader(canvas, curX, curY, pieceR, leapAnim.isWhite);

            if (t >= 1f) {
                leapAnim.active = false;
            } else {
                postInvalidateOnAnimation();
            }
        }

        // 6. Bottom Dashboard: 4 Egyptian Casting Sticks (*Djed*) & Telemetry Ribbon
        float trayTop = h - dpf(46f);
        rect.set(pad, trayTop, w - pad, h - dpf(6f));
        canvas.drawRoundRect(rect, dpf(8f), dpf(8f), trayBgPaint);
        canvas.drawRoundRect(rect, dpf(8f), dpf(8f), goldBorderPaint);

        float stickStartX = pad + dpf(20f);
        float stickSpacing = (w - pad * 2 - dpf(40f)) / 5f;
        for (int s = 0; s < 4; s++) {
            float scx = stickStartX + s * stickSpacing;
            float scy = trayTop + dpf(20f);
            canvas.save();
            canvas.rotate(stickAngles[s], scx, scy);
            drawCastingStick(canvas, scx, scy, dpf(28f), dpf(6.5f), lastSticksLight[s]);
            canvas.restore();
        }

        float badgeX = stickStartX + 4 * stickSpacing;
        float badgeY = trayTop + dpf(20f);
        textPaint.setTextSize(dpf(11.5f));
        textPaint.setColor(currentRoll > 0 ? 0xFFFDE047 : 0xFF94A3B8);
        canvas.drawText(currentRoll >= 0 ? "🥢 " + currentRoll : "CAST", badgeX, badgeY + dpf(4f), textPaint);

        if ((waitingForRoll && currentTurn == 0) || leapAnim.active) {
            postInvalidateOnAnimation();
        }
    }

    private void drawPieceWithShader(Canvas canvas, float cx, float cy, float r, boolean isPharaoh) {
        Bitmap bmp = isPharaoh ? cachedPharaohPieceBmp : cachedAnubisPieceBmp;
        if (bmp != null) {
            canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);
            RectF dest = new RectF(cx - r, cy - r, cx + r, cy + r);
            canvas.drawBitmap(bmp, null, dest, null);
        } else {
            // High-precision vector fallback
            canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);
            RadialGradient grad = new RadialGradient(
                cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
                isPharaoh ? new int[]{0xFFFFFBEB, 0xFFFDE047, 0xFFB45309} : new int[]{0xFF7DD3FC, 0xFF0284C7, 0xFF082F49},
                null, Shader.TileMode.CLAMP
            );
            piecePaint.setShader(grad);
            canvas.drawCircle(cx, cy, r, piecePaint);
            canvas.drawCircle(cx, cy, r, pieceRimPaint);
            canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.28f, pieceShinePaint);
            canvas.drawCircle(cx, cy, r * 0.24f, goldBorderPaint);
        }
    }

    private void drawCastingStick(Canvas canvas, float cx, float cy, float len, float thickness, boolean isLight) {
        rect.set(cx - len / 2f + dpf(1.2f), cy - thickness / 2f + dpf(1.8f), cx + len / 2f + dpf(1.2f), cy + thickness / 2f + dpf(1.8f));
        canvas.drawRoundRect(rect, thickness / 2f, thickness / 2f, shadowPaint);

        rect.set(cx - len / 2f, cy - thickness / 2f, cx + len / 2f, cy + thickness / 2f);
        canvas.drawRoundRect(rect, thickness / 2f, thickness / 2f, isLight ? stickLightPaint : stickDarkPaint);

        if (isLight) {
            Paint notchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            notchPaint.setColor(0xFFCA8A04);
            notchPaint.setStrokeWidth(dpf(1f));
            for (float nx = -len * 0.3f; nx <= len * 0.3f; nx += len * 0.2f) {
                canvas.drawLine(cx + nx, cy - thickness * 0.35f, cx + nx, cy + thickness * 0.35f, notchPaint);
            }
        }

        canvas.drawRoundRect(rect, thickness / 2f, thickness / 2f, goldBorderPaint);
    }
}
