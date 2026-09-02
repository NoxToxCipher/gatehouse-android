package au.com.dss.gatehouse;

import android.util.Log;

/**
 * Hardened High-Performance Systems Core powered by native Zig (libgatehouse_zig.so).
 * 
 * Capabilities:
 * - SIMD Vector Camera frame luminance extraction & fast binarization (<0.5ms)
 * - Zero-allocation binary packed struct BLE Mesh / NFC packet codecs
 * - Mathematical 16-bit PCM waveform & siren DSP
 * - 3D Quaternion & Kalman sensor fusion for tilt-compensated compass
 */
public final class GatehouseNativeZig {
    private static final String TAG = "GatehouseZig";
    private static volatile boolean sLoaded = false;

    static {
        try {
            System.loadLibrary("gatehouse_zig");
            sLoaded = true;
            Log.i(TAG, "✓ Native Hardened Zig engine loaded successfully");
        } catch (Throwable t) {
            Log.e(TAG, "Native Zig engine could not be loaded: " + t.getMessage());
        }
    }

    public static boolean isAvailable() {
        return sLoaded;
    }

    /**
     * SIMD vector conversion of raw camera Y-plane buffer to greyscale.
     */
    public static int extractGreyscale(byte[] yPlaneIn, byte[] greyOut) {
        if (yPlaneIn == null || greyOut == null) return 0;
        if (sLoaded) {
            try {
                return nativeExtractGreyscale(yPlaneIn, greyOut);
            } catch (Throwable t) {
                Log.w(TAG, "Zig SIMD extract failed, falling back", t);
            }
        }
        int len = Math.min(yPlaneIn.length, greyOut.length);
        System.arraycopy(yPlaneIn, 0, greyOut, 0, len);
        return len;
    }

    /**
     * SIMD vector adaptive binarization of greyscale camera frame.
     */
    public static void binarizeFrame(byte[] greyIn, byte[] binOut, int threshold) {
        if (greyIn == null || binOut == null) return;
        if (sLoaded) {
            try {
                nativeBinarizeFrame(greyIn, binOut, threshold);
                return;
            } catch (Throwable t) {
                Log.w(TAG, "Zig binarize failed, falling back", t);
            }
        }
        int len = Math.min(greyIn.length, binOut.length);
        for (int i = 0; i < len; i++) {
            binOut[i] = (byte) ((greyIn[i] & 0xFF) > threshold ? 255 : 0);
        }
    }

    /**
     * Serializes a 20-byte BLE Mesh / NFC packet with hardware CRC-16.
     */
    public static byte[] serializeMeshPacket(int packetType, int seq, int officerId, int timestamp, int latE7) {
        if (sLoaded) {
            try {
                return nativeSerializeMeshPacket(packetType, seq, officerId, timestamp, latE7);
            } catch (Throwable t) {
                Log.w(TAG, "Zig packet serialize failed", t);
            }
        }
        return null;
    }

    /**
     * Validates magic and CRC-16 of a 20-byte packet.
     */
    public static boolean validateMeshPacket(byte[] rawPacket) {
        if (rawPacket == null || rawPacket.length < 20) return false;
        if (sLoaded) {
            try {
                return nativeValidateMeshPacket(rawPacket);
            } catch (Throwable t) {
                Log.w(TAG, "Zig packet validate failed", t);
            }
        }
        return false;
    }

    /**
     * Synthesizes 16-bit PCM audio tone waveform (shape: 0=sine, 1=square, 2=triangle, 3=sawtooth).
     */
    public static short[] synthesizeTone(float frequencyHz, int durationMs, float amplitude, int shape) {
        if (sLoaded) {
            try {
                return nativeSynthesizeTone(frequencyHz, durationMs, amplitude, shape);
            } catch (Throwable t) {
                Log.w(TAG, "Zig tone synthesis failed", t);
            }
        }
        return null;
    }

    /**
     * Computes tilt-compensated heading in degrees (0.0 to 360.0).
     */
    public static float computeHeading(float ax, float ay, float az, float mx, float my, float mz) {
        if (sLoaded) {
            try {
                return nativeComputeHeading(ax, ay, az, mx, my, mz);
            } catch (Throwable t) {
                Log.w(TAG, "Zig heading math failed", t);
            }
        }
        return 0.0f;
    }

    // ---- Native Declarations ----
    private static native int nativeExtractGreyscale(byte[] yIn, byte[] greyOut);
    private static native void nativeBinarizeFrame(byte[] greyIn, byte[] binOut, int threshold);
    private static native byte[] nativeSerializeMeshPacket(int packetType, int seq, int officerId, int timestamp, int latE7);
    private static native boolean nativeValidateMeshPacket(byte[] rawPacket);
    private static native short[] nativeSynthesizeTone(float frequencyHz, int durationMs, float amplitude, int shape);
    private static native float nativeComputeHeading(float ax, float ay, float az, float mx, float my, float mz);
}
