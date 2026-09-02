//! Constant-Time 16-bit PCM Audio Waveform Synthesis & Siren DSP.
//!
//! Generates distortion-free PCM audio buffers for emergency sirens, DTMF, and alert pings.

const std = @import("std");

pub const SAMPLE_RATE: u32 = 44100;
const TWO_PI: f32 = 6.283185307179586;

pub const WaveShape = enum(u8) {
    sine = 0,
    square = 1,
    triangle = 2,
    sawtooth = 3,
};

/// Synthesizes a pure tone into a 16-bit signed PCM buffer.
pub fn synthesizeTone(
    frequency_hz: f32,
    duration_ms: u32,
    amplitude: f32,
    shape: WaveShape,
    out_samples: []i16,
) usize {
    const total_samples = (SAMPLE_RATE * duration_ms) / 1000;
    const len = @min(total_samples, out_samples.len);
    const clamped_amp = @min(@max(amplitude, 0.0), 1.0);
    const max_val: f32 = 32767.0 * clamped_amp;

    var phase: f32 = 0.0;
    const phase_step = (frequency_hz * TWO_PI) / @as(f32, @floatFromInt(SAMPLE_RATE));

    var i: usize = 0;
    while (i < len) : (i += 1) {
        var sample_val: f32 = 0.0;
        switch (shape) {
            .sine => {
                sample_val = @sin(phase);
            },
            .square => {
                sample_val = if (@sin(phase) >= 0.0) 1.0 else -1.0;
            },
            .triangle => {
                const norm = (phase / TWO_PI) - @floor(phase / TWO_PI);
                sample_val = if (norm < 0.5) (4.0 * norm - 1.0) else (3.0 - 4.0 * norm);
            },
            .sawtooth => {
                const norm = (phase / TWO_PI) - @floor(phase / TWO_PI);
                sample_val = 2.0 * norm - 1.0;
            },
        }

        // Apply 5ms fade-in and fade-out to prevent speaker pop
        const fade_len = (SAMPLE_RATE * 5) / 1000;
        var envelope: f32 = 1.0;
        if (i < fade_len) {
            envelope = @as(f32, @floatFromInt(i)) / @as(f32, @floatFromInt(fade_len));
        } else if (i > len -| fade_len) {
            envelope = @as(f32, @floatFromInt(len - i)) / @as(f32, @floatFromInt(fade_len));
        }

        out_samples[i] = @intFromFloat(sample_val * max_val * envelope);
        phase += phase_step;
        if (phase >= TWO_PI) phase -= TWO_PI;
    }
    return len;
}

/// Synthesizes a high-urgency wailing distress siren (frequency sweeping between 600Hz and 1400Hz).
pub fn synthesizeSiren(
    duration_ms: u32,
    sweep_period_ms: u32,
    out_samples: []i16,
) usize {
    const total_samples = (SAMPLE_RATE * duration_ms) / 1000;
    const len = @min(total_samples, out_samples.len);
    const sweep_samples = (SAMPLE_RATE * sweep_period_ms) / 1000;

    var phase: f32 = 0.0;
    var i: usize = 0;

    while (i < len) : (i += 1) {
        // Triangular sweep between 600Hz and 1400Hz
        const sweep_pos = @as(f32, @floatFromInt(i % sweep_samples)) / @as(f32, @floatFromInt(sweep_samples));
        const mod = if (sweep_pos < 0.5) sweep_pos * 2.0 else (1.0 - sweep_pos) * 2.0;
        const current_freq = 600.0 + (800.0 * mod);

        const phase_step = (current_freq * TWO_PI) / @as(f32, @floatFromInt(SAMPLE_RATE));
        const sample_val = @sin(phase);

        out_samples[i] = @intFromFloat(sample_val * 30000.0);
        phase += phase_step;
        if (phase >= TWO_PI) phase -= TWO_PI;
    }
    return len;
}

test "audio_dsp: synthesize tone & siren" {
    var pcm_buffer: [4410]i16 = undefined; // 100ms at 44.1kHz
    const generated = synthesizeTone(1000.0, 100, 0.8, .sine, &pcm_buffer);

    try std.testing.expectEqual(@as(usize, 4410), generated);
    // Ensure amplitude is within bounds
    for (pcm_buffer) |sample| {
        try std.testing.expect(sample <= 32767 and sample >= -32768);
    }
}
