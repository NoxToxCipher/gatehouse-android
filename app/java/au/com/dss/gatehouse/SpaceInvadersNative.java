package au.com.dss.gatehouse;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

/**
 * High-performance Space Invaders Engine JNI Bridge powered by native Rust (libspace_invaders.so).
 * 
 * Capabilities:
 * - 55-invader bitboard fleet stepping with accelerated march speeds
 * - Destructible bunker shield crater carving
 * - Swept AABB projectile physics (zero Java GC allocations)
 * - Procedural 8-bit audio DSP (4-note march, laser zap, explosion white noise, UFO siren)
 */
public final class SpaceInvadersNative {
    private static final String TAG = "SpaceInvadersRust";
    private static volatile boolean sLoaded = false;

    // Bitmask Event Flags
    public static final int EVENT_MARCH_STEP   = 1 << 0;
    public static final int EVENT_PLAYER_FIRED = 1 << 1;
    public static final int EVENT_ALIEN_KILLED = 1 << 2;
    public static final int EVENT_BUNKER_HIT   = 1 << 3;
    public static final int EVENT_PLAYER_HIT   = 1 << 4;
    public static final int EVENT_UFO_KILLED   = 1 << 5;
    public static final int EVENT_WAVE_CLEARED = 1 << 6;
    public static final int EVENT_GAME_OVER    = 1 << 7;

    static {
        try {
            System.loadLibrary("space_invaders");
            sLoaded = true;
            Log.i(TAG, "✓ Native Rust Space Invaders engine loaded successfully");
        } catch (Throwable t) {
            Log.e(TAG, "Native Space Invaders engine could not be loaded: " + t.getMessage());
        }
    }

    public static boolean isAvailable() {
        return sLoaded;
    }

    private long nativeHandle = 0;

    public SpaceInvadersNative(float screenW, float screenH) {
        if (sLoaded) {
            try {
                this.nativeHandle = nativeCreateEngine(screenW, screenH);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to create native engine", t);
            }
        }
    }

    public void release() {
        if (sLoaded && nativeHandle != 0) {
            try {
                nativeDestroyEngine(nativeHandle);
                nativeHandle = 0;
            } catch (Throwable t) {
                Log.w(TAG, "Failed to destroy native engine", t);
            }
        }
    }

    public void resetGame() {
        if (sLoaded && nativeHandle != 0) {
            try {
                nativeResetGame(nativeHandle);
            } catch (Throwable ignored) {}
        }
    }

    public int update(float dtMs, float targetX, boolean shootTrigger) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativeUpdate(nativeHandle, dtMs, targetX, shootTrigger);
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public void getState(int[] outInts, float[] outFloats) {
        if (sLoaded && nativeHandle != 0) {
            try {
                nativeGetState(nativeHandle, outInts, outFloats);
            } catch (Throwable ignored) {}
        }
    }

    public int getAlienRowMask(int row) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativeGetAlienRowMask(nativeHandle, row);
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public void getBunkerGrid(int bunkerIdx, byte[] outGrid) {
        if (sLoaded && nativeHandle != 0) {
            try {
                nativeGetBunkerGrid(nativeHandle, bunkerIdx, outGrid);
            } catch (Throwable ignored) {}
        }
    }

    public int getProjectiles(float[] outFloats) {
        if (sLoaded && nativeHandle != 0) {
            try {
                return nativeGetProjectiles(nativeHandle, outFloats);
            } catch (Throwable ignored) {}
        }
        return 0;
    }

    public static void playProceduralSfx(int sfxType, int noteIdx) {
        if (sLoaded) {
            try {
                short[] pcm = nativeSynthesizeAudio(sfxType, noteIdx);
                if (pcm != null && pcm.length > 0) {
                    AudioTrack track = new AudioTrack.Builder()
                            .setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build())
                            .setAudioFormat(new AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(44100)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build())
                            .setBufferSizeInBytes(pcm.length * 2)
                            .setTransferMode(AudioTrack.MODE_STATIC)
                            .build();

                    track.write(pcm, 0, pcm.length);
                    track.play();
                }
            } catch (Throwable ignored) {}
        }
    }

    // ---- Native Declarations ----
    private static native long nativeCreateEngine(float screenW, float screenH);
    private static native void nativeDestroyEngine(long handle);
    private static native void nativeResetGame(long handle);
    private static native int nativeUpdate(long handle, float dtMs, float targetX, boolean shoot);
    private static native void nativeGetState(long handle, int[] outInts, float[] outFloats);
    private static native int nativeGetAlienRowMask(long handle, int row);
    private static native void nativeGetBunkerGrid(long handle, int bunkerIdx, byte[] outGrid);
    private static native int nativeGetProjectiles(long handle, float[] outFloats);
    private static native short[] nativeSynthesizeAudio(int sfxType, int noteIdx);
}
