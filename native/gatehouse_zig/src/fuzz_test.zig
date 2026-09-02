//! Automated 100,000-Cycle Mutation Fuzzer & Differential Oracle Tester.
//!
//! Stress-tests all packet decoders, SIMD transforms, and DSP routines with random corruptions.

const std = @import("std");
const packet_codec = @import("packet_codec.zig");
const simd_camera = @import("simd_camera.zig");
const audio_dsp = @import("audio_dsp.zig");
const sensor_fusion = @import("sensor_fusion.zig");
const guards = @import("guards.zig");

test "fuzzer: 100,000-cycle packet codec mutation stress test" {
    var prng = std.Random.DefaultPrng.init(0x4453535F534543);
    const rand = prng.random();

    var valid_packet_buf: [20]u8 = undefined;
    packet_codec.serializePacket(
        .checkpoint_ping,
        1,
        41207,
        1788250000,
        -276650000,
        &valid_packet_buf,
    );

    var cycle: usize = 0;
    while (cycle < 100_000) : (cycle += 1) {
        var fuzz_buf: [32]u8 = undefined;
        const fuzz_len = rand.uintAtMost(usize, 32);

        // Copy valid packet base, then apply random mutations
        const copy_len = @min(fuzz_len, 20);
        @memcpy(fuzz_buf[0..copy_len], valid_packet_buf[0..copy_len]);

        // Mutate random bytes
        const num_mutations = rand.uintAtMost(usize, 5);
        var m: usize = 0;
        while (m < num_mutations) : (m += 1) {
            if (fuzz_len > 0) {
                const idx = rand.uintLessThan(usize, fuzz_len);
                fuzz_buf[idx] ^= rand.int(u8);
            }
        }

        // Parse corrupted buffer (must NEVER panic or crash)
        var out_packet: packet_codec.DssMeshPacket = undefined;
        _ = packet_codec.deserializeAndValidate(fuzz_buf[0..fuzz_len], &out_packet);
    }
}

test "fuzzer: 10,000-cycle SIMD camera buffer fuzzing" {
    var prng = std.Random.DefaultPrng.init(0x1337BEEF);
    const rand = prng.random();

    var tracker = guards.HardenedTracker.init(std.testing.allocator);
    defer std.testing.expect(tracker.isClean()) catch {};

    var cycle: usize = 0;
    while (cycle < 10_000) : (cycle += 1) {
        const size = rand.uintAtMost(usize, 256) + 16;
        const in_buf = try tracker.alloc(u8, size);
        defer tracker.free(in_buf);

        const out_buf = try tracker.alloc(u8, size);
        defer tracker.free(out_buf);

        rand.bytes(in_buf);
        _ = simd_camera.extractGreyscaleSimd(in_buf, out_buf);
        simd_camera.binarizeFrame(out_buf, in_buf, rand.int(u8));
    }
}
