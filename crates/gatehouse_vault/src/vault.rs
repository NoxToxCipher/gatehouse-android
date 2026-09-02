//! Doherty Security Services (DSS) Cryptographic Vault & Hardware Attestation Core.
//!
//! Provides:
//! - AES-256-GCM authenticated logbook payload encryption/decryption
//! - Hardware-bound key derivation from Officer PIN + Android ID
//! - HMAC-SHA256 digital attestation sealing for shift records
//! - Constant-time verification & zero-leak memory scrubbing

use crate::aes_gcm::Aes256Gcm;
use crate::sha256::{pbkdf2_hmac_sha256, HmacSha256, Sha256};

pub struct ZeroizeBuffer<const N: usize> {
    pub data: [u8; N],
}

impl<const N: usize> ZeroizeBuffer<N> {
    pub fn new() -> Self {
        ZeroizeBuffer { data: [0u8; N] }
    }
}

impl<const N: usize> Drop for ZeroizeBuffer<N> {
    fn drop(&mut self) {
        for byte in self.data.iter_mut() {
            unsafe {
                std::ptr::write_volatile(byte, 0);
            }
        }
    }
}

/// Derives a 256-bit master vault key from an Officer PIN and hardware salt.
pub fn derive_vault_key(pin: &str, device_hardware_id: &str, out_key: &mut [u8; 32]) {
    let mut salt = [0u8; 32];
    let mut salt_hasher = Sha256::new();
    salt_hasher.update(b"DSS-GATEHOUSE-SALT-V1:");
    salt_hasher.update(device_hardware_id.as_bytes());
    salt.copy_from_slice(&salt_hasher.finalize());

    pbkdf2_hmac_sha256(pin.as_bytes(), &salt, 10_000, out_key);
}

/// Encrypts an arbitrary logbook payload using AES-256-GCM.
/// Returns: `[12-byte Nonce] + [Ciphertext] + [16-byte Tag]`
pub fn encrypt_vault_payload(key: &[u8; 32], aad: &[u8], plaintext: &[u8], nonce: &[u8; 12]) -> Vec<u8> {
    let cipher = Aes256Gcm::new(key);
    let (ct, tag) = cipher.encrypt(nonce, aad, plaintext);

    let mut result = Vec::with_capacity(12 + ct.len() + 16);
    result.extend_from_slice(nonce);
    result.extend_from_slice(&ct);
    result.extend_from_slice(&tag);
    result
}

/// Decrypts an authenticated AES-256-GCM vault payload.
/// Expects: `[12-byte Nonce] + [Ciphertext] + [16-byte Tag]`
pub fn decrypt_vault_payload(key: &[u8; 32], aad: &[u8], payload: &[u8]) -> Option<Vec<u8>> {
    if payload.len() < 28 {
        return None;
    }

    let mut nonce = [0u8; 12];
    nonce.copy_from_slice(&payload[..12]);

    let ct_len = payload.len() - 28;
    let ct = &payload[12..12 + ct_len];

    let mut tag = [0u8; 16];
    tag.copy_from_slice(&payload[12 + ct_len..]);

    let cipher = Aes256Gcm::new(key);
    cipher.decrypt(&nonce, aad, ct, &tag)
}

/// Generates a tamper-evident digital attestation seal for a sealed shift record.
/// Output format: `DSS-SEAL-V1:<HEX_SHA256_BODY>:<HEX_HMAC_SIGNATURE>`
pub fn sign_shift_attestation(
    shift_json: &str,
    officer_licence: &str,
    master_key: &[u8; 32],
) -> String {
    let body_hash = Sha256::digest(shift_json.as_bytes());
    let mut signer = HmacSha256::new(master_key);
    signer.update(b"DSS-ATTESTATION-SIGNATURE:");
    signer.update(officer_licence.as_bytes());
    signer.update(&body_hash);
    let sig = signer.finalize();

    let hex_hash = hex_encode(&body_hash);
    let hex_sig = hex_encode(&sig);

    format!("DSS-SEAL-V1:{}:{}", hex_hash, hex_sig)
}

/// Constant-time verification of a shift attestation seal.
pub fn verify_shift_attestation(
    shift_json: &str,
    officer_licence: &str,
    claimed_seal: &str,
    master_key: &[u8; 32],
) -> bool {
    let parts: Vec<&str> = claimed_seal.split(':').collect();
    if parts.len() != 3 || parts[0] != "DSS-SEAL-V1" {
        return false;
    }

    let claimed_hash = parts[1];
    let claimed_sig = parts[2];

    let actual_body_hash = Sha256::digest(shift_json.as_bytes());
    if hex_encode(&actual_body_hash) != claimed_hash {
        return false;
    }

    let mut signer = HmacSha256::new(master_key);
    signer.update(b"DSS-ATTESTATION-SIGNATURE:");
    signer.update(officer_licence.as_bytes());
    signer.update(&actual_body_hash);
    let expected_sig = signer.finalize();

    let mut expected_sig_bytes = [0u8; 32];
    if hex_decode_32(claimed_sig, &mut expected_sig_bytes).is_err() {
        return false;
    }

    // Constant-time compare
    let mut diff = 0u8;
    for i in 0..32 {
        diff |= expected_sig[i] ^ expected_sig_bytes[i];
    }
    diff == 0
}

fn hex_encode(bytes: &[u8]) -> String {
    let hex_chars = b"0123456789abcdef";
    let mut s = String::with_capacity(bytes.len() * 2);
    for &b in bytes {
        s.push(hex_chars[(b >> 4) as usize] as char);
        s.push(hex_chars[(b & 0x0f) as usize] as char);
    }
    s
}

fn hex_decode_32(hex: &str, out: &mut [u8; 32]) -> Result<(), ()> {
    if hex.len() != 64 {
        return Err(());
    }
    let bytes = hex.as_bytes();
    for i in 0..32 {
        let hi = hex_val(bytes[i * 2])?;
        let lo = hex_val(bytes[i * 2 + 1])?;
        out[i] = (hi << 4) | lo;
    }
    Ok(())
}

fn hex_val(c: u8) -> Result<u8, ()> {
    match c {
        b'0'..=b'9' => Ok(c - b'0'),
        b'a'..=b'f' => Ok(c - b'a' + 10),
        b'A'..=b'F' => Ok(c - b'A' + 10),
        _ => Err(()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_aes_gcm_vault_roundtrip() {
        let mut key = [0u8; 32];
        derive_vault_key("4120", "ANDROID_HW_ID_DSS_01", &mut key);

        let nonce = [1u8, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
        let aad = b"DSS_GUARD_PATROL_RECORD:2026-09-02";
        let plaintext = b"Patrol Checkpoint 04 verified secure. All perimeter gates locked.";

        let encrypted = encrypt_vault_payload(&key, aad, plaintext, &nonce);
        assert_eq!(encrypted.len(), 12 + plaintext.len() + 16);

        let decrypted = decrypt_vault_payload(&key, aad, &encrypted).expect("Decryption must succeed");
        assert_eq!(decrypted, plaintext);

        // Tamper test
        let mut tampered = encrypted.clone();
        tampered[15] ^= 0xFF;
        assert!(decrypt_vault_payload(&key, aad, &tampered).is_none());

        // AAD tamper test
        assert!(decrypt_vault_payload(&key, b"WRONG_AAD", &encrypted).is_none());
    }

    #[test]
    fn test_shift_attestation_signature() {
        let mut key = [0u8; 32];
        derive_vault_key("3943", "HW_TERMINAL_POST_01", &mut key);

        let shift_json = r#"{"shiftId":"s-8821","officer":"Lochran Doherty","hours":12,"checkpoints":8}"#;
        let licence = "LIC-3943517";

        let seal = sign_shift_attestation(shift_json, licence, &key);
        assert!(seal.starts_with("DSS-SEAL-V1:"));

        assert!(verify_shift_attestation(shift_json, licence, &seal, &key));

        // Tamper with payload
        let tampered_json = r#"{"shiftId":"s-8821","officer":"Lochran Doherty","hours":14,"checkpoints":8}"#;
        assert!(!verify_shift_attestation(tampered_json, licence, &seal, &key));

        // Wrong licence
        assert!(!verify_shift_attestation(shift_json, "LIC-9999999", &seal, &key));
    }
}
