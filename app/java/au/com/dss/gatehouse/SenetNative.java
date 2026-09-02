package au.com.dss.gatehouse;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * High-performance Ancient Egyptian Senet Engine JNI Bridge powered by native Rust (libgatehouse_senet.so).
 *
 * Features:
 * - Stochastic Expectimax Tree Search AI across 3 difficulty tiers (Scribe, Priest of Thoth, Anubis)
 * - Zero-allocation 30-square track with 2-piece protection and 3-piece blockade solvers
 * - Sacred Houses mechanics (House of Rebirth 15, Beauty 26, Water Hazard 27, 3 Truths 28, Re-Atum 29, Horus 30)
 * - Procedural Sandstone, Lapis Lazuli, and Gold Leaf tile and piece shader synthesis
 */
public final class SenetNative {
    private static final String TAG = "SenetRust";
    private static volatile boolean sLoaded = false;

    static {
        try {
            System.loadLibrary("gatehouse_senet");
            sLoaded = true;
            Log.i(TAG, "✓ Native Rust Senet engine loaded successfully");
        } catch (Throwable t) {
            Log.e(TAG, "Native Senet engine could not be loaded: " + t.getMessage());
        }
    }

    public static boolean isAvailable() {
        return sLoaded;
    }

    public static class MoveResult {
        public int bestPieceIdx;
        public float winrate;
    }

    public static class BoardState {
        public int[] whitePieces = new int[5];
        public int[] blackPieces = new int[5];
        public int whiteBorneOff;
        public int blackBorneOff;
        public int currentTurn;
    }

    private long nativeHandle = 0;
    private int difficulty = 1;

    public SenetNative(int difficultyTier) {
        this.difficulty = difficultyTier;
        if (sLoaded) {
            try {
                this.nativeHandle = nativeCreateEngine(difficultyTier);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to create native Senet engine", t);
            }
        }
    }

    public void release() {
        if (sLoaded && nativeHandle != 0) {
            try {
                nativeDestroyEngine(nativeHandle);
                nativeHandle = 0;
            } catch (Throwable t) {
                Log.w(TAG, "Failed to destroy native Senet engine", t);
            }
        }
    }

    public void reset() {
        if (sLoaded && nativeHandle != 0) {
            try {
                nativeReset(nativeHandle);
            } catch (Throwable ignored) {}
        }
    }

    public boolean playMove(int pieceIdx, int roll) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativePlayMove(nativeHandle, pieceIdx, roll);
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public int getLegalMovesMask(int roll) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativeGetLegalMovesMask(nativeHandle, roll);
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public MoveResult findBestMove(int roll, int tier) {
        MoveResult res = new MoveResult();
        res.bestPieceIdx = 0;
        res.winrate = 0.5f;

        if (sLoaded && nativeHandle != 0) {
            try {
                float[] outRes = new float[2];
                res.bestPieceIdx = nativeFindBestMove(nativeHandle, roll, tier, outRes);
                res.winrate = outRes[1];
            } catch (Throwable t) {
                Log.w(TAG, "Native findBestMove failed", t);
            }
        }
        return res;
    }

    public float evaluatePosition(int turn) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativeEvaluatePosition(nativeHandle, turn);
            } catch (Throwable ignored) {}
        }
        return 0.0f;
    }

    public BoardState getBoardState() {
        BoardState state = new BoardState();
        if (sLoaded && nativeHandle != 0) {
            try {
                int[] wp = new int[5];
                int[] bp = new int[5];
                int[] meta = new int[3];
                nativeGetBoardState(nativeHandle, wp, bp, meta);
                state.whitePieces = wp;
                state.blackPieces = bp;
                state.whiteBorneOff = meta[0];
                state.blackBorneOff = meta[1];
                state.currentTurn = meta[2];
            } catch (Throwable ignored) {}
        }
        return state;
    }

    public static Bitmap renderTileBitmap(int width, int height, int tileType) {
        if (!sLoaded || width <= 0 || height <= 0) return null;
        try {
            int[] pixels = new int[width * height];
            nativeRenderTileTexture(width, height, tileType, pixels);
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } catch (Throwable t) {
            Log.w(TAG, "Native tile texture render failed", t);
            return null;
        }
    }

    public static Bitmap renderPieceBitmap(int width, int height, boolean isPharaoh) {
        if (!sLoaded || width <= 0 || height <= 0) return null;
        try {
            int[] pixels = new int[width * height];
            nativeRenderPieceTexture(width, height, isPharaoh, pixels);
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } catch (Throwable t) {
            Log.w(TAG, "Native piece texture render failed", t);
            return null;
        }
    }

    // ---- Native Declarations ----
    private static native long nativeCreateEngine(int difficulty);
    private static native void nativeDestroyEngine(long handle);
    private static native void nativeReset(long handle);
    private static native boolean nativePlayMove(long handle, int pieceIdx, int roll);
    private static native int nativeGetLegalMovesMask(long handle, int roll);
    private static native int nativeFindBestMove(long handle, int roll, int difficulty, float[] outResult);
    private static native float nativeEvaluatePosition(long handle, int turn);
    private static native void nativeGetBoardState(long handle, int[] outWhite, int[] outBlack, int[] outMeta);
    private static native void nativeRenderTileTexture(int width, int height, int tileType, int[] outPixels);
    private static native void nativeRenderPieceTexture(int width, int height, boolean isPharaoh, int[] outPixels);
}
