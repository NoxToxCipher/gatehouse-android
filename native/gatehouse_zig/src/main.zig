//! Gatehouse Hardened Zig Systems Core.
//!
//! Exposes SIMD Camera frame crunching, NFC/BLE packet codecs, audio DSP, and sensor fusion.

pub const guards = @import("guards.zig");
pub const simd_camera = @import("simd_camera.zig");
pub const packet_codec = @import("packet_codec.zig");
pub const audio_dsp = @import("audio_dsp.zig");
pub const sensor_fusion = @import("sensor_fusion.zig");
pub const baduk_raster = @import("baduk_raster.zig");
pub const jni_bridge = @import("jni_bridge.zig");
pub const fuzz_test = @import("fuzz_test.zig");

comptime {
    @import("std").testing.refAllDecls(jni_bridge);
}

test {
    @import("std").testing.refAllDecls(@This());
}
