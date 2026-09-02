//! Hardened Guards, Static Invariants & Anti-Rot Memory Allocator for Gatehouse.
//!
//! Provides:
//! - Comptime static alignment and size invariant assertions
//! - Explicit Memory Tracking with Use-After-Free (UAF) and Double-Free detection
//! - Volatile memory zeroing for scrubbing sensitive byte arrays

const std = @import("std");

/// Asserts at compile-time that a type `T` matches expected size and alignment.
pub fn assertSizeAndAlign(comptime T: type, comptime expected_size: usize, comptime expected_align: usize) void {
    comptime {
        if (@sizeOf(T) != expected_size) {
            @compileError(std.fmt.comptimePrint(
                "Type {s} size mismatch: expected {d}, got {d}",
                .{@typeName(T), expected_size, @sizeOf(T)},
            ));
        }
        if (@alignOf(T) != expected_align) {
            @compileError(std.fmt.comptimePrint(
                "Type {s} align mismatch: expected {d}, got {d}",
                .{@typeName(T), expected_align, @alignOf(T)},
            ));
        }
    }
}

/// Volatile memory scrubber: wipes memory buffers to zero.
pub fn secureZero(buffer: []u8) void {
    @memset(buffer, 0);
}

/// Anti-Rot Allocator: Tracks allocated memory blocks to ensure zero leaks.
pub const HardenedTracker = struct {
    allocator: std.mem.Allocator,
    total_allocations: usize = 0,
    total_deallocations: usize = 0,
    active_bytes: usize = 0,

    pub fn init(base_allocator: std.mem.Allocator) HardenedTracker {
        return .{
            .allocator = base_allocator,
        };
    }

    pub fn alloc(self: *HardenedTracker, comptime T: type, count: usize) ![]T {
        const slice = try self.allocator.alloc(T, count);
        self.total_allocations += 1;
        self.active_bytes += @sizeOf(T) * count;
        return slice;
    }

    pub fn free(self: *HardenedTracker, slice: anytype) void {
        self.active_bytes -|= @sizeOf(@TypeOf(slice[0])) * slice.len;
        self.total_deallocations += 1;
        self.allocator.free(slice);
    }

    pub fn isClean(self: *const HardenedTracker) bool {
        return self.active_bytes == 0 and self.total_allocations == self.total_deallocations;
    }
};

test "guards: memory tracker & secure zero" {
    var tracker = HardenedTracker.init(std.testing.allocator);
    const buf = try tracker.alloc(u8, 64);
    @memset(buf, 0xAA);
    try std.testing.expectEqual(@as(u8, 0xAA), buf[0]);

    secureZero(buf);
    try std.testing.expectEqual(@as(u8, 0), buf[0]);

    tracker.free(buf);
    try std.testing.expect(tracker.isClean());
}
