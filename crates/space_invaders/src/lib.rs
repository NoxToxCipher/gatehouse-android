//! Museum-grade Space Invaders Engine in Rust (Native Android + WASM).

pub mod invaders;
pub mod bunkers;
pub mod projectiles;
pub mod audio;
pub mod engine;
pub mod jni;

pub use engine::GameState;
