//! JNI C-ABI Bridge for Android Java (`au.com.dss.gatehouse.GatehouseNativeZig`).

const std = @import("std");
const simd_camera = @import("simd_camera.zig");
const packet_codec = @import("packet_codec.zig");
const audio_dsp = @import("audio_dsp.zig");
const sensor_fusion = @import("sensor_fusion.zig");

// JNI Types
pub const JNIEnv = opaque {};
pub const jobject = ?*opaque {};
pub const jclass = ?*opaque {};
pub const jbyteArray = ?*opaque {};
pub const jshortArray = ?*opaque {};
pub const jsize = i32;
pub const jboolean = u8;
pub const jint = i32;
pub const jfloat = f32;

// JNI Native Interface function table pointers
pub const JNINativeInterface = extern struct {
    reserved0: ?*anyopaque,
    reserved1: ?*anyopaque,
    reserved2: ?*anyopaque,
    reserved3: ?*anyopaque,
    // Methods
    GetStringUTFChars: ?*anyopaque,
    ReleaseStringUTFChars: ?*anyopaque,
    GetArrayLength: *const fn (?*JNIEnv, ?*anyopaque) callconv(.c) jsize,
    GetByteArrayRegion: *const fn (?*JNIEnv, jbyteArray, jsize, jsize, [*]i8) callconv(.c) void,
    SetByteArrayRegion: *const fn (?*JNIEnv, jbyteArray, jsize, jsize, [*]const i8) callconv(.c) void,
    GetShortArrayRegion: *const fn (?*JNIEnv, jshortArray, jsize, jsize, [*]i16) callconv(.c) void,
    SetShortArrayRegion: *const fn (?*JNIEnv, jshortArray, jsize, jsize, [*]const i16) callconv(.c) void,
    NewByteArray: *const fn (?*JNIEnv, jsize) callconv(.c) jbyteArray,
    NewShortArray: *const fn (?*JNIEnv, jsize) callconv(.c) jshortArray,
};

fn getInterface(env: ?*JNIEnv) *const JNINativeInterface {
    const p: *const *const JNINativeInterface = @ptrCast(@alignCast(env.?));
    return p.*;
}

export fn Java_au_com_dss_gatehouse_GatehouseNativeZig_nativeExtractGreyscale(
    env: ?*JNIEnv,
    _: jclass,
    y_in: jbyteArray,
    grey_out: jbyteArray,
) jint {
    const iface = getInterface(env);
    const in_len = iface.GetArrayLength(env, y_in);
    const out_len = iface.GetArrayLength(env, grey_out);
    const len: usize = @intCast(@min(in_len, out_len));
    if (len == 0) return 0;

    const alloc = std.heap.page_allocator;

    const in_buf = alloc.alloc(u8, len) catch return 0;
    defer alloc.free(in_buf);
    const out_buf = alloc.alloc(u8, len) catch return 0;
    defer alloc.free(out_buf);

    iface.GetByteArrayRegion(env, y_in, 0, @intCast(len), @ptrCast(in_buf.ptr));
    const processed = simd_camera.extractGreyscaleSimd(in_buf, out_buf);
    iface.SetByteArrayRegion(env, grey_out, 0, @intCast(processed), @ptrCast(out_buf.ptr));

    return @intCast(processed);
}

export fn Java_au_com_dss_gatehouse_GatehouseNativeZig_nativeBinarizeFrame(
    env: ?*JNIEnv,
    _: jclass,
    grey_in: jbyteArray,
    bin_out: jbyteArray,
    threshold: jint,
) void {
    const iface = getInterface(env);
    const in_len = iface.GetArrayLength(env, grey_in);
    const out_len = iface.GetArrayLength(env, bin_out);
    const len: usize = @intCast(@min(in_len, out_len));
    if (len == 0) return;

    const alloc = std.heap.page_allocator;

    const in_buf = alloc.alloc(u8, len) catch return;
    defer alloc.free(in_buf);
    const out_buf = alloc.alloc(u8, len) catch return;
    defer alloc.free(out_buf);

    iface.GetByteArrayRegion(env, grey_in, 0, @intCast(len), @ptrCast(in_buf.ptr));
    simd_camera.binarizeFrame(in_buf, out_buf, @intCast(threshold));
    iface.SetByteArrayRegion(env, bin_out, 0, @intCast(len), @ptrCast(out_buf.ptr));
}

export fn Java_au_com_dss_gatehouse_GatehouseNativeZig_nativeSerializeMeshPacket(
    env: ?*JNIEnv,
    _: jclass,
    packet_type: jint,
    seq: jint,
    officer_id: jint,
    timestamp: jint,
    lat_e7: jint,
) jbyteArray {
    const iface = getInterface(env);
    var pkt_buf: [20]u8 = undefined;

    packet_codec.serializePacket(
        @enumFromInt(@as(u8, @intCast(packet_type))),
        @intCast(seq),
        @intCast(officer_id),
        @intCast(timestamp),
        @intCast(lat_e7),
        &pkt_buf,
    );

    const jarr = iface.NewByteArray(env, 20);
    if (jarr != null) {
        iface.SetByteArrayRegion(env, jarr, 0, 20, @ptrCast(&pkt_buf));
    }
    return jarr;
}

export fn Java_au_com_dss_gatehouse_GatehouseNativeZig_nativeValidateMeshPacket(
    env: ?*JNIEnv,
    _: jclass,
    raw_packet: jbyteArray,
) jboolean {
    const iface = getInterface(env);
    const len = iface.GetArrayLength(env, raw_packet);
    if (len < 20) return 0;

    var buf: [20]u8 = undefined;
    iface.GetByteArrayRegion(env, raw_packet, 0, 20, @ptrCast(&buf));

    var parsed: packet_codec.DssMeshPacket = undefined;
    if (packet_codec.deserializeAndValidate(&buf, &parsed)) {
        return 1;
    }
    return 0;
}

export fn Java_au_com_dss_gatehouse_GatehouseNativeZig_nativeSynthesizeTone(
    env: ?*JNIEnv,
    _: jclass,
    frequency_hz: jfloat,
    duration_ms: jint,
    amplitude: jfloat,
    shape: jint,
) jshortArray {
    const iface = getInterface(env);
    const total_samples: usize = @intCast((audio_dsp.SAMPLE_RATE * @as(u32, @intCast(duration_ms))) / 1000);

    const alloc = std.heap.page_allocator;

    const pcm = alloc.alloc(i16, total_samples) catch return null;
    defer alloc.free(pcm);

    const generated = audio_dsp.synthesizeTone(
        frequency_hz,
        @intCast(duration_ms),
        amplitude,
        @enumFromInt(@as(u8, @intCast(shape))),
        pcm,
    );

    const jarr = iface.NewShortArray(env, @intCast(generated));
    if (jarr != null) {
        iface.SetShortArrayRegion(env, jarr, 0, @intCast(generated), pcm.ptr);
    }
    return jarr;
}

export fn Java_au_com_dss_gatehouse_GatehouseNativeZig_nativeComputeHeading(
    _: ?*JNIEnv,
    _: jclass,
    ax: jfloat,
    ay: jfloat,
    az: jfloat,
    mx: jfloat,
    my: jfloat,
    mz: jfloat,
) jfloat {
    const accel = sensor_fusion.Vec3{ .x = ax, .y = ay, .z = az };
    const mag = sensor_fusion.Vec3{ .x = mx, .y = my, .z = mz };
    return sensor_fusion.computeTiltCompensatedHeading(accel, mag);
}
