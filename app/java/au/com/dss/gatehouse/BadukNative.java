package au.com.dss.gatehouse;

import android.graphics.Point;
import android.util.Log;

/**
 * High-performance Baduk (Go) Engine JNI Bridge powered by native Rust (libgatehouse_baduk.so).
 * 
 * Features:
 * - Bitboard Zero-Allocation Group/Chain tracker (50,000+ rollouts/sec)
 * - MCTS UCT search engine with multi-tier difficulty (Apprentice, Master, Grandmaster)
 * - 3x3 local shape pattern matching & Joseki evaluations
 * - Fast Shicho (ladder) reader
 * - Benson's algorithm territory & dead stone evaluation
 */
public final class BadukNative {
    private static final String TAG = "BadukRust";
    private static volatile boolean sLoaded = false;

    static {
        try {
            System.loadLibrary("gatehouse_baduk");
            sLoaded = true;
            Log.i(TAG, "✓ Native Rust Baduk engine loaded successfully");
        } catch (Throwable t) {
            Log.e(TAG, "Native Baduk engine could not be loaded: " + t.getMessage());
        }
    }

    public static boolean isAvailable() {
        return sLoaded;
    }

    public static class MoveResult {
        public Point point;
        public float winrate;
        public float[] heatmap;
    }

    public static class TerritoryResult {
        public int blackScore;
        public int whiteScore;
        public int blackTerritory;
        public int whiteTerritory;
        public int dame;
        public byte[] territoryGrid; // 1 = Black, 2 = White, 0 = Neutral
    }

    private long nativeHandle = 0;
    private int boardSize = 9;

    public BadukNative(int size) {
        this.boardSize = size;
        if (sLoaded) {
            try {
                this.nativeHandle = nativeCreateEngine(size);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to create native Baduk engine", t);
            }
        }
    }

    public void release() {
        if (sLoaded && nativeHandle != 0) {
            try {
                nativeDestroyEngine(nativeHandle);
                nativeHandle = 0;
            } catch (Throwable t) {
                Log.w(TAG, "Failed to destroy native Baduk engine", t);
            }
        }
    }

    public void reset(int size) {
        this.boardSize = size;
        if (sLoaded && nativeHandle != 0) {
            try {
                nativeReset(nativeHandle, size);
            } catch (Throwable ignored) {}
        }
    }

    public boolean playMove(int x, int y, int color) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativePlayMove(nativeHandle, x, y, color);
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public boolean isLegal(int x, int y, int color) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativeIsLegal(nativeHandle, x, y, color);
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public MoveResult findBestMove(int color, int difficultyTier) {
        MoveResult res = new MoveResult();
        res.point = null;
        res.winrate = 0.5f;
        res.heatmap = new float[19 * 19];

        if (sLoaded && nativeHandle != 0) {
            try {
                int[] coords = new int[2];
                res.winrate = nativeFindBestMove(nativeHandle, color, difficultyTier, coords, res.heatmap);
                if (coords[0] >= 0 && coords[1] >= 0) {
                    res.point = new Point(coords[0], coords[1]);
                }
            } catch (Throwable t) {
                Log.w(TAG, "Native findBestMove failed", t);
            }
        }
        return res;
    }

    public TerritoryResult evaluateTerritory(float komi) {
        TerritoryResult tr = new TerritoryResult();
        tr.territoryGrid = new byte[19 * 19];
        int[] scores = new int[5];

        if (sLoaded && nativeHandle != 0) {
            try {
                nativeEvaluateTerritory(nativeHandle, komi, scores, tr.territoryGrid);
                tr.blackScore = scores[0];
                tr.whiteScore = scores[1];
                tr.blackTerritory = scores[2];
                tr.whiteTerritory = scores[3];
                tr.dame = scores[4];
            } catch (Throwable t) {
                Log.w(TAG, "Native evaluateTerritory failed", t);
            }
        }
        return tr;
    }

    public boolean isLadderCapture(int x, int y) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativeIsLadderCapture(nativeHandle, x, y);
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static android.graphics.Bitmap renderStoneBitmap(int width, int height, int stoneType, int theme) {
        if (!sLoaded || width <= 0 || height <= 0) return null;
        try {
            int[] pixels = new int[width * height];
            nativeRenderStoneTexture(width, height, stoneType, theme, pixels);
            return android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888);
        } catch (Throwable t) {
            Log.w(TAG, "Native stone texture render failed", t);
            return null;
        }
    }

    public static android.graphics.Bitmap renderWoodgrainBitmap(int width, int height, int theme) {
        if (!sLoaded || width <= 0 || height <= 0) return null;
        try {
            int[] pixels = new int[width * height];
            nativeRenderWoodgrainTexture(width, height, theme, pixels);
            return android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888);
        } catch (Throwable t) {
            Log.w(TAG, "Native woodgrain texture render failed", t);
            return null;
        }
    }

    // ---- Native Declarations ----
    private static native long nativeCreateEngine(int size);
    private static native void nativeDestroyEngine(long handle);
    private static native void nativeReset(long handle, int size);
    private static native boolean nativePlayMove(long handle, int x, int y, int color);
    private static native boolean nativeIsLegal(long handle, int x, int y, int color);
    private static native float nativeFindBestMove(long handle, int color, int difficultyTier, int[] outCoords, float[] outHeatmap);
    private static native void nativeEvaluateTerritory(long handle, float komi, int[] outScores, byte[] outTerritoryGrid);
    private static native boolean nativeIsLadderCapture(long handle, int x, int y);
    private static native void nativeRenderStoneTexture(int width, int height, int stoneType, int theme, int[] outPixels);
    private static native void nativeRenderWoodgrainTexture(int width, int height, int theme, int[] outPixels);
}
