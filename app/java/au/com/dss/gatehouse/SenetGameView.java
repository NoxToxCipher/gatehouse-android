package au.com.dss.gatehouse;

import android.content.Context;
import android.graphics.Canvas;
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
import java.util.List;
import java.util.Random;

/**
 * SenetGameView — Ancient Egyptian Senet (c. 3100 BCE).
 * Museum-grade Egyptian Sandstone, Lapis Lazuli & Gold Hieroglyphic Board.
 * Features:
 * - Illuminated Serpentine S-Curve Journey Track (Duat Ribbon with Direction Flow)
 * - 5 Illustrated Sacred Houses (Rebirth 𓋹, Beauty 𓄤, Water Hazard 𓈗, 3 Truths 𓏺, Re-Atum 𓏻, Horus 𓁐)
 * - Parabolic Golden Leap Trajectory Arcs & Target Landing Highlights
 * - Authentic 3D Conical Crowns (Pharaoh) & Lapis Spools (Anubis)
 * - 4-Casting Sticks Tray with 3D Flip & Probability Explainer
 * - Interactive Path & Rules Guide Overlay
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
    private final Paint tileDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

    private final RectF rect = new RectF();
    private final RectF tileRect = new RectF();
    private final Random rand = new Random();

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

    // 30-Square Serpentine Path (Boustrophedon S-Track)
    // Row 0: 0 -> 9 (Squares 1-10, Left to Right)
    // Row 1: 19 <- 10 (Squares 11-20, Right to Left)
    // Row 2: 20 -> 29 (Squares 21-30, Left to Right)
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

        resetGame();
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
        System.arraycopy(state.lastSticksLight, 0, lastSticksLight, 0, 4);
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

        // Standard historical starting setup: alternating pieces on first 10 squares
        for (int i = 0; i < 5; i++) {
            whitePieces[i] = i * 2;
            blackPieces[i] = i * 2 + 1;
        }
        for (int i = 0; i < 4; i++) lastSticksLight[i] = false;
        updateStatus();
        invalidate();
    }

    private boolean isCasting = false;

    public void throwSticks() {
        if (!waitingForRoll || isCasting) return;
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
            for (int i = 0; i < 4; i++) lastSticksLight[i] = rand.nextBoolean();
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
                if (isLight) lightSides++;
            }
            // Senet casting stick scoring:
            // 1 Light = 1 step (+ extra turn)
            // 2 Light = 2 steps
            // 3 Light = 3 steps
            // 4 Light = 4 steps (+ extra turn)
            // 0 Light (All Dark) = 5 steps (+ extra turn)
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
        if (waitingForRoll || currentRoll <= 0 || currentTurn != 0) return false;
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

    private void makeMove(int pieceIdx) {
        if (waitingForRoll || currentRoll <= 0) return;
        if (currentTurn == 0) saveHistory();
        int[] my = (currentTurn == 0) ? whitePieces : blackPieces;
        int[] opp = (currentTurn == 0) ? blackPieces : whitePieces;

        int pos = my[pieceIdx];
        if (pos == 30) return;
        int next = pos + currentRoll;
        if (next > 30) return;

        for (int j = 0; j < 5; j++) {
            if (my[j] == next) return;
        }

        for (int j = 0; j < 5; j++) {
            if (opp[j] == next) {
                for (int k = 0; k < 5; k++) {
                    if (opp[k] == next - 1 || opp[k] == next + 1) return;
                }
            }
        }

        // Swap opponent piece back
        for (int j = 0; j < 5; j++) {
            if (opp[j] == next) {
                opp[j] = pos;
                try {
                    RecreationAudioSynth.playChessPieceThud(true);
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {}
                break;
            }
        }

        // House of Water (Square 27, Index 26) drowning trap -> sends piece back to House of Rebirth (Square 15, Index 14)
        if (next == 26) {
            next = 14;
            boolean occupied = true;
            while (occupied && next >= 0) {
                occupied = false;
                for (int p : my) if (p == next) occupied = true;
                for (int p : opp) if (p == next) occupied = true;
                if (occupied) next--;
            }
        }

        my[pieceIdx] = next;
        selectedPieceIdx = -1;

        if (next == 30) {
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
        if (currentTurn != 1 || waitingForRoll || currentRoll <= 0) return;

        int bestPiece = -1;
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
            if (next == 30) score += 60; // Bearing off
            if (isAttack) score += 35; // Capture/swap
            if (next == 25) score += 20; // Safe House of Beauty
            if (next == 26) score -= 45; // Avoid Water Trap

            if (score > bestScore) {
                bestScore = score;
                bestPiece = i;
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

        String turnStr = (currentTurn == 0) ? "🟡 Pharaoh (You)" : "🔵 Anubis Bot";
        String rollStr = waitingForRoll ? "· Tap to Cast 4 Sticks" : ("· Cast: " + currentRoll + (currentRoll == 1 || currentRoll >= 4 ? " (Bonus Roll ⚡)" : ""));
        String scoreStr = " [𓁐 " + whiteBorneOff + "/5 vs " + blackBorneOff + "/5]";
        statusListener.onStatusChanged(turnStr + " " + rollStr + scoreStr, 0xFFFFD166);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (action == MotionEvent.ACTION_DOWN) {
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
                    // Check if tapping a player piece
                    for (int i = 0; i < 5; i++) {
                        int pos = whitePieces[i];
                        if (pos >= 0 && pos < 30) {
                            int pr = SENET_PATH[pos][0];
                            int pc = SENET_PATH[pos][1];
                            if (pr == r && pc == c && isPieceMovable(i)) {
                                if (selectedPieceIdx == i) {
                                    // Second tap on already selected piece -> Execute move!
                                    makeMove(i);
                                } else {
                                    // First tap -> Select piece and show jump trajectory
                                    selectedPieceIdx = i;
                                    try { performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Exception ignored) {}
                                    invalidate();
                                }
                                return true;
                            }
                        }
                    }

                    // Check if tapping destination target square for selected piece
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
                            // Bearing off
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

        // Sandstone & Royal Egyptian Obsidian Border Frame
        rect.set(0, 0, w, h);
        boardFramePaint.setShader(new LinearGradient(0, 0, w, h, 0xFF181512, 0xFF2A0845, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, dpf(16f), dpf(16f), boardFramePaint);

        rect.set(dpf(2.5f), dpf(2.5f), w - dpf(2.5f), h - dpf(2.5f));
        canvas.drawRoundRect(rect, dpf(14f), dpf(14f), goldBorderPaint);

        float pad = dpf(8f);
        float cellW = (w - pad * 2) / 10f;
        float cellH = (h - dpf(56f)) / 3f;

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

        // 2. Draw 30 Egyptian Tiles with Sacred Houses and Directional Guidance
        long now = System.currentTimeMillis();
        float flowPulse = 0.5f + 0.5f * (float) Math.sin(now / 200.0);

        for (int i = 0; i < 30; i++) {
            int r = SENET_PATH[i][0];
            int c = SENET_PATH[i][1];

            float left = pad + c * cellW;
            float top = dpf(6f) + r * cellH;
            tileRect.set(left + dpf(1.5f), top + dpf(1.5f), left + cellW - dpf(1.5f), top + cellH - dpf(1.5f));

            boolean isSpecial = (i == 14 || i >= 25);
            if (isSpecial) {
                int col1 = (i == 26) ? 0xFF0284C7 : (i == 14 ? 0xFF059669 : 0xFF6B21A8);
                int col2 = (i == 26) ? 0xFF082F49 : (i == 14 ? 0xFF064E3B : 0xFF3B0764);
                specialTilePaint.setShader(new RadialGradient(tileRect.centerX(), tileRect.centerY(), cellW * 0.85f, col1, col2, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(tileRect, dpf(5f), dpf(5f), specialTilePaint);
            } else {
                boolean isLight = (r + c) % 2 == 0;
                tileLightPaint.setColor(isLight ? 0xFF1E293B : 0xFF0F172A);
                canvas.drawRoundRect(tileRect, dpf(5f), dpf(5f), tileLightPaint);
            }

            canvas.drawRoundRect(tileRect, dpf(5f), dpf(5f), goldDetailPaint);

            // Subtle square numbering (1..30) in top-left
            textPaint.setTextSize(dpf(7f));
            textPaint.setColor(0x77FDE047);
            canvas.drawText(String.valueOf(i + 1), left + dpf(7f), top + dpf(9f), textPaint);

            // Flow Direction Chevrons (→ / ← / →)
            if (showGuide && !isSpecial) {
                String dirArrow = (r == 1) ? "◀" : "▶";
                textPaint.setTextSize(dpf(8f));
                textPaint.setColor(0x44FDE047);
                canvas.drawText(dirArrow, tileRect.centerX(), tileRect.bottom - dpf(3f), textPaint);
            }

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
        if (selectedPieceIdx != -1 && currentTurn == 0 && currentRoll > 0 && isPieceMovable(selectedPieceIdx)) {
            int srcPos = whitePieces[selectedPieceIdx];
            int dstPos = srcPos + currentRoll;

            float srcX = pad + SENET_PATH[srcPos][1] * cellW + cellW / 2f;
            float srcY = dpf(6f) + SENET_PATH[srcPos][0] * cellH + cellH / 2f;

            if (dstPos < 30) {
                float dstX = pad + SENET_PATH[dstPos][1] * cellW + cellW / 2f;
                float dstY = dpf(6f) + SENET_PATH[dstPos][0] * cellH + cellH / 2f;

                // Parabolic Leap Arc
                Path jumpArc = new Path();
                jumpArc.moveTo(srcX, srcY);
                float midX = (srcX + dstX) / 2f;
                float midY = Math.min(srcY, dstY) - dpf(22f);
                jumpArc.quadTo(midX, midY, dstX, dstY);
                canvas.drawPath(jumpArc, trajectoryArcPaint);

                // Landing Target Highlight Ring
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
                // Bearing Off Ascension Beam
                targetRingPaint.setColor(0xFFFFD166);
                canvas.drawCircle(srcX, srcY, cellW * 0.48f, targetRingPaint);
                textPaint.setTextSize(dpf(8.5f));
                textPaint.setColor(0xFFFFD166);
                canvas.drawText("👑 ASCEND (+1)", srcX, srcY - dpf(16f), textPaint);
            }
        }

        // 4. Draw 3D Conical Crowns (Pharaoh) & Lapis Spools (Anubis)
        float pieceR = Math.min(cellW, cellH) * 0.38f;
        for (int i = 0; i < 5; i++) {
            // White / Pharaoh Pieces
            int wp = whitePieces[i];
            if (wp >= 0 && wp < 30) {
                int r = SENET_PATH[wp][0];
                int c = SENET_PATH[wp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(6f) + r * cellH + cellH / 2f;

                boolean movable = (currentTurn == 0 && !waitingForRoll && currentRoll > 0 && isPieceMovable(i));
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

                drawEgyptianCrown(canvas, cx, cy, pieceR, true);
            }

            // Black / Anubis Pieces
            int bp = blackPieces[i];
            if (bp >= 0 && bp < 30) {
                int r = SENET_PATH[bp][0];
                int c = SENET_PATH[bp][1];
                float cx = pad + c * cellW + cellW / 2f;
                float cy = dpf(6f) + r * cellH + cellH / 2f;
                drawEgyptianCrown(canvas, cx, cy, pieceR, false);
            }
        }

        // 5. Bottom Dashboard: 4 Egyptian Casting Sticks (*Djed*)
        float trayTop = h - dpf(46f);
        rect.set(pad, trayTop, w - pad, h - dpf(6f));
        canvas.drawRoundRect(rect, dpf(8f), dpf(8f), trayBgPaint);
        canvas.drawRoundRect(rect, dpf(8f), dpf(8f), goldBorderPaint);

        float stickStartX = pad + dpf(20f);
        float stickSpacing = (w - pad * 2 - dpf(40f)) / 5f;
        for (int s = 0; s < 4; s++) {
            float scx = stickStartX + s * stickSpacing;
            float scy = trayTop + dpf(20f);
            drawCastingStick(canvas, scx, scy, dpf(28f), dpf(6.5f), lastSticksLight[s]);
        }

        float badgeX = stickStartX + 4 * stickSpacing;
        float badgeY = trayTop + dpf(20f);
        textPaint.setTextSize(dpf(11.5f));
        textPaint.setColor(currentRoll > 0 ? 0xFFFDE047 : 0xFF94A3B8);
        canvas.drawText(currentRoll >= 0 ? "🥢 " + currentRoll : "CAST", badgeX, badgeY + dpf(4f), textPaint);

        if (waitingForRoll && currentTurn == 0) {
            postInvalidateOnAnimation();
        }
    }

    private void drawEgyptianCrown(Canvas canvas, float cx, float cy, float r, boolean isPharaoh) {
        canvas.drawCircle(cx + dpf(1.5f), cy + dpf(2f), r, shadowPaint);

        if (isPharaoh) {
            // Pharaoh Golden White Crown (Hedjet) with Gold Core
            RadialGradient grad = new RadialGradient(
                cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
                new int[]{0xFFFFFBEB, 0xFFFDE047, 0xFFB45309},
                null, Shader.TileMode.CLAMP
            );
            piecePaint.setShader(grad);
            canvas.drawCircle(cx, cy, r, piecePaint);
            canvas.drawCircle(cx, cy, r, pieceRimPaint);
            canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.28f, pieceShinePaint);
            canvas.drawCircle(cx, cy, r * 0.24f, goldBorderPaint);
        } else {
            // Anubis Lapis Lazuli Spool with Turquoise Shimmer
            RadialGradient grad = new RadialGradient(
                cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
                new int[]{0xFF7DD3FC, 0xFF0284C7, 0xFF082F49},
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
