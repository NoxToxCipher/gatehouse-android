package au.com.dss.gatehouse;

import android.content.Context;
import android.provider.Settings;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * High-Performance Native Cryptographic Vault & Digital Attestation Engine.
 * 
 * Powered by compiled native Rust (libgatehouse_vault.so):
 * - Constant-time AES-256-GCM authenticated payload encryption
 * - ChaCha20-Poly1305 / HMAC-SHA256 digital shift attestation
 * - PBKDF2-HMAC-SHA256 hardware-bound key derivation
 * - Zero-leak memory security
 */
public final class GatehouseVault {
    private static volatile boolean sLoaded = false;
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        try {
            System.loadLibrary("gatehouse_vault");
            sLoaded = true;
        } catch (Throwable t) {
            android.util.Log.e("GatehouseVault", "Native gatehouse_vault library could not be loaded: " + t.getMessage());
        }
    }

    public static boolean isAvailable() {
        return sLoaded;
    }

    /**
     * Derives a 256-bit hardware-bound master key from an Officer PIN and Android Device ID.
     */
    public static byte[] deriveMasterKey(String pin, String hardwareId) {
        if (!sLoaded || pin == null || hardwareId == null) {
            return fallbackDeriveKey(pin, hardwareId);
        }
        try {
            return nativeDeriveKey(pin, hardwareId);
        } catch (Throwable t) {
            return fallbackDeriveKey(pin, hardwareId);
        }
    }

    /**
     * Encrypts arbitrary plaintext bytes with AES-256-GCM.
     * Returns: [12-byte Nonce] + [Ciphertext] + [16-byte Tag]
     */
    public static byte[] encrypt(byte[] key, byte[] aad, byte[] plaintext) {
        if (key == null || plaintext == null) return null;
        byte[] nonce = new byte[12];
        RANDOM.nextBytes(nonce);

        if (sLoaded) {
            try {
                return nativeEncryptPayload(key, aad != null ? aad : new byte[0], plaintext, nonce);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    /**
     * Decrypts an authenticated AES-256-GCM payload.
     */
    public static byte[] decrypt(byte[] key, byte[] aad, byte[] payload) {
        if (key == null || payload == null || payload.length < 28) return null;
        if (sLoaded) {
            try {
                return nativeDecryptPayload(key, aad != null ? aad : new byte[0], payload);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    /**
     * Generates a tamper-evident digital seal for a shift record.
     */
    public static String signShift(String shiftJson, String officerLicence, byte[] masterKey) {
        if (shiftJson == null || officerLicence == null || masterKey == null) return "";
        if (sLoaded) {
            try {
                return nativeSignAttestation(shiftJson, officerLicence, masterKey);
            } catch (Throwable ignored) {}
        }
        return "";
    }

    /**
     * Verifies a digital seal on a shift record.
     */
    public static boolean verifyShift(String shiftJson, String officerLicence, String claimedSeal, byte[] masterKey) {
        if (shiftJson == null || officerLicence == null || claimedSeal == null || masterKey == null) return false;
        if (sLoaded) {
            try {
                return nativeVerifyAttestation(shiftJson, officerLicence, claimedSeal, masterKey);
            } catch (Throwable ignored) {}
        }
        return false;
    }

    public static String getDeviceHardwareId(Context context) {
        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return androidId != null ? androidId : "DSS-DEFAULT-DEVICE-HW-01";
        } catch (Exception e) {
            return "DSS-DEFAULT-DEVICE-HW-01";
        }
    }

    private static byte[] fallbackDeriveKey(String pin, String hardwareId) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update("DSS-GATEHOUSE-SALT-V1:".getBytes(StandardCharsets.UTF_8));
            md.update(String.valueOf(hardwareId).getBytes(StandardCharsets.UTF_8));
            byte[] salt = md.digest();

            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    (pin != null ? pin : "0000").toCharArray(), salt, 10000, 256);
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            return new byte[32];
        }
    }

    // ---- Native Declarations ----
    private static native byte[] nativeDeriveKey(String pin, String hardwareId);
    private static native byte[] nativeEncryptPayload(byte[] key, byte[] aad, byte[] plaintext, byte[] nonce);
    private static native byte[] nativeDecryptPayload(byte[] key, byte[] aad, byte[] payload);
    private static native String nativeSignAttestation(String shiftJson, String officerLicence, byte[] key);
    private static native boolean nativeVerifyAttestation(String shiftJson, String officerLicence, String claimedSeal, byte[] key);
}
