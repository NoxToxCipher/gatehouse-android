//! Native Cryptographic Vault & Hardware Attestation Core for Gatehouse.
//!
//! Written in safe, constant-time Rust with zero external runtime dependencies.
//! Provides AES-256-GCM, ChaCha20-Poly1305, SHA-256, HMAC, and JNI bridges for DSS.

pub mod aes_gcm;
pub mod chacha20_poly1305;
pub mod jni;
pub mod sha256;
pub mod vault;

pub use aes_gcm::Aes256Gcm;
pub use chacha20_poly1305::{ChaCha20, Poly1305};
pub use sha256::{pbkdf2_hmac_sha256, HmacSha256, Sha256};
pub use vault::{
    decrypt_vault_payload, derive_vault_key, encrypt_vault_payload, sign_shift_attestation,
    verify_shift_attestation,
};
