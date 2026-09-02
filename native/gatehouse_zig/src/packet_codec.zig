//! Zero-Allocation Binary Packet Codec for NFC Tags and BLE Mesh Gossip.
//!
//! Uses hardware-mapped packed structs with strict CRC-16 validation.

const std = @import("std");

/// Magic header for Doherty Security Services over-the-air BLE Mesh packets.
pub const DSS_PACKET_MAGIC: u16 = 0x4453; // "DS"

pub const PacketType = enum(u8) {
    checkpoint_ping = 0x01,
    officer_heartbeat = 0x02,
    duress_beacon = 0x03,
    passdown_note = 0x04,
    fuel_radar_gossip = 0x05,
    _,
};

/// 20-byte packed over-the-air BLE Mesh telemetry frame.
pub const DssMeshPacket = extern struct {
    magic: u16, // 2 bytes: 0x4453
    packet_type: u8, // 1 byte
    sequence: u8, // 1 byte
    officer_id: u32, // 4 bytes (e.g. 41207)
    timestamp_epoch: u32, // 4 bytes (seconds since Unix epoch)
    latitude_e7: i32, // 4 bytes (GPS latitude * 1e7)
    reserved: u16 = 0, // 2 bytes
    crc16: u16, // 2 bytes (CRC-16-CCITT)
};

comptime {
    if (@sizeOf(DssMeshPacket) != 20) {
        @compileError("DssMeshPacket must be exactly 20 bytes for BLE advertisement payloads");
    }
}

/// Computes CRC-16-CCITT (Polynomial 0x1021, Initial 0xFFFF).
pub fn computeCrc16(data: []const u8) u16 {
    var crc: u16 = 0xFFFF;
    for (data) |byte| {
        crc ^= (@as(u16, byte) << 8);
        var i: u8 = 0;
        while (i < 8) : (i += 1) {
            if ((crc & 0x8000) != 0) {
                crc = (crc << 1) ^ 0x1021;
            } else {
                crc = crc << 1;
            }
        }
    }
    return crc;
}

/// Serializes a mesh telemetry packet into a 20-byte raw buffer with valid CRC.
pub fn serializePacket(
    packet_type: PacketType,
    seq: u8,
    officer_id: u32,
    timestamp: u32,
    lat_e7: i32,
    out_buffer: *[20]u8,
) void {
    var pkt = DssMeshPacket{
        .magic = DSS_PACKET_MAGIC,
        .packet_type = @intFromEnum(packet_type),
        .sequence = seq,
        .officer_id = officer_id,
        .timestamp_epoch = timestamp,
        .latitude_e7 = lat_e7,
        .crc16 = 0,
    };

    const raw_ptr: [*]const u8 = @ptrCast(&pkt);
    const calculated_crc = computeCrc16(raw_ptr[0..18]);
    pkt.crc16 = calculated_crc;

    const final_ptr: [*]const u8 = @ptrCast(&pkt);
    @memcpy(out_buffer, final_ptr[0..20]);
}

/// Deserializes and validates a 20-byte packet. Returns true if magic and CRC match.
pub fn deserializeAndValidate(
    raw_buffer: []const u8,
    out_packet: *DssMeshPacket,
) bool {
    if (raw_buffer.len < 20) return false;

    const pkt_ptr: *const DssMeshPacket = @ptrCast(@alignCast(raw_buffer.ptr));
    if (pkt_ptr.magic != DSS_PACKET_MAGIC) return false;

    const expected_crc = computeCrc16(raw_buffer[0..18]);
    if (pkt_ptr.crc16 != expected_crc) return false;

    out_packet.* = pkt_ptr.*;
    return true;
}

test "packet_codec: serialize & deserialize round-trip" {
    var buffer: [20]u8 = undefined;
    serializePacket(.checkpoint_ping, 42, 41207, 1788250000, -276650000, &buffer);

    var parsed: DssMeshPacket = undefined;
    const valid = deserializeAndValidate(&buffer, &parsed);

    try std.testing.expect(valid);
    try std.testing.expectEqual(@as(u8, @intFromEnum(PacketType.checkpoint_ping)), parsed.packet_type);
    try std.testing.expectEqual(@as(u32, 41207), parsed.officer_id);
    try std.testing.expectEqual(@as(i32, -276650000), parsed.latitude_e7);

    // Tamper test
    buffer[5] ^= 0xFF;
    try std.testing.expect(!deserializeAndValidate(&buffer, &parsed));
}
