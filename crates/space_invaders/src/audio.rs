//! Procedural 8-bit Retro Arcade Audio Synthesis.
//! Generates 16-bit signed PCM buffers (44.1kHz mono) without external audio files.

pub const SAMPLE_RATE: usize = 44100;

#[derive(Clone, Copy, Debug, PartialEq)]
pub enum SoundEffect {
    MarchNote(u8), // 0, 1, 2, 3
    PlayerLaser,
    AlienExplosion,
    PlayerDeath,
    UfoWarble,
}

pub fn synthesize_sfx(sfx: SoundEffect, out: &mut [i16]) -> usize {
    match sfx {
        SoundEffect::MarchNote(note) => {
            // Authentic 1978 Space Invaders bass tone frequencies
            let freq = match note % 4 {
                0 => 55.0,   // A1
                1 => 49.0,   // G1
                2 => 43.65,  // F1
                _ => 38.89,  // D#1
            };
            let duration_samples = (SAMPLE_RATE * 65) / 1000; // 65ms punch
            let len = duration_samples.min(out.len());
            for i in 0..len {
                let t = i as f32 / SAMPLE_RATE as f32;
                let phase = (t * freq).fract();
                // Square wave with fast decay envelope
                let square = if phase < 0.5 { 1.0 } else { -1.0 };
                let env = (1.0 - (i as f32 / len as f32)).powf(1.5);
                out[i] = (square * env * 18000.0) as i16;
            }
            len
        }
        SoundEffect::PlayerLaser => {
            // Fast downward pitch sweep (880Hz down to 220Hz over 140ms)
            let duration_samples = (SAMPLE_RATE * 140) / 1000;
            let len = duration_samples.min(out.len());
            let mut phase: f32 = 0.0;
            for i in 0..len {
                let frac = i as f32 / len as f32;
                let freq = 880.0 - (frac * 660.0);
                phase += freq / SAMPLE_RATE as f32;
                if phase >= 1.0 { phase -= 1.0; }
                let square = if phase < 0.5 { 1.0 } else { -1.0 };
                let env = 1.0 - frac;
                out[i] = (square * env * 16000.0) as i16;
            }
            len
        }
        SoundEffect::AlienExplosion => {
            // White noise burst with quick decay (180ms)
            let duration_samples = (SAMPLE_RATE * 180) / 1000;
            let len = duration_samples.min(out.len());
            let mut rng_state: u32 = 0x1978;
            for i in 0..len {
                // Linear congruential pseudorandom noise
                rng_state = rng_state.wrapping_mul(1664525).wrapping_add(1013904223);
                let noise = ((rng_state as f32 / 4294967296.0) * 2.0) - 1.0;
                let env = (1.0 - (i as f32 / len as f32)).powf(2.0);
                out[i] = (noise * env * 22000.0) as i16;
            }
            len
        }
        SoundEffect::PlayerDeath => {
            // Dramatic low rumble noise & frequency flutter (450ms)
            let duration_samples = (SAMPLE_RATE * 450) / 1000;
            let len = duration_samples.min(out.len());
            let mut rng_state: u32 = 0xDEAD;
            for i in 0..len {
                let frac = i as f32 / len as f32;
                rng_state = rng_state.wrapping_mul(1664525).wrapping_add(1013904223);
                let noise = ((rng_state as f32 / 4294967296.0) * 2.0) - 1.0;
                let flutter = ((frac * 40.0 * core::f32::consts::PI).sin()).abs();
                let env = (1.0 - frac).powf(1.2);
                out[i] = (noise * flutter * env * 24000.0) as i16;
            }
            len
        }
        SoundEffect::UfoWarble => {
            // Modulated UFO warble siren (120ms chunk)
            let duration_samples = (SAMPLE_RATE * 120) / 1000;
            let len = duration_samples.min(out.len());
            let mut phase: f32 = 0.0;
            for i in 0..len {
                let t = i as f32 / SAMPLE_RATE as f32;
                let mod_freq = 480.0 + (t * 20.0 * core::f32::consts::PI).sin() * 120.0;
                phase += mod_freq / SAMPLE_RATE as f32;
                if phase >= 1.0 { phase -= 1.0; }
                let triangle = ((phase * 2.0) - 1.0).abs() * 2.0 - 1.0;
                out[i] = (triangle * 14000.0) as i16;
            }
            len
        }
    }
}
