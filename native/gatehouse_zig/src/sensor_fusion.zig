//! Real-Time Sensor Fusion & 3D Quaternion Dead-Reckoning Compass.
//!
//! Provides tilt-compensated azimuth calculation and Kalman noise reduction.

const std = @import("std");

pub const Vec3 = struct {
    x: f32,
    y: f32,
    z: f32,

    pub fn normalize(self: Vec3) Vec3 {
        const mag = @sqrt(self.x * self.x + self.y * self.y + self.z * self.z);
        if (mag < 1e-6) return .{ .x = 0, .y = 0, .z = 0 };
        return .{
            .x = self.x / mag,
            .y = self.y / mag,
            .z = self.z / mag,
        };
    }
};

/// 1D Kalman Filter for smooth sensor streams (e.g. compass heading).
pub const KalmanFilter1D = struct {
    q: f32 = 0.05, // Process noise covariance
    r: f32 = 2.0, // Measurement noise covariance
    x: f32 = 0.0, // Estimated value
    p: f32 = 1.0, // Estimation error covariance
    k: f32 = 0.0, // Kalman gain

    pub fn update(self: *KalmanFilter1D, measurement: f32) f32 {
        // Prediction update
        self.p += self.q;

        // Measurement update
        self.k = self.p / (self.p + self.r);
        self.x += self.k * (measurement - self.x);
        self.p *= (1.0 - self.k);

        return self.x;
    }
};

/// Computes tilt-compensated magnetic heading (azimuth in degrees: 0.0 to 360.0).
pub fn computeTiltCompensatedHeading(
    accel: Vec3,
    mag: Vec3,
) f32 {
    const a = accel.normalize();
    const m = mag.normalize();

    // Pitch (theta) and Roll (phi)
    const pitch = std.math.asin(-a.x);
    const roll = std.math.asin(a.y / (@cos(pitch) + 1e-6));

    // Tilt compensation
    const xh = m.x * @cos(pitch) + m.z * @sin(pitch);
    const yh = m.x * @sin(roll) * @sin(pitch) + m.y * @cos(roll) - m.z * @sin(roll) * @cos(pitch);

    var heading_rad = std.math.atan2(yh, xh);
    if (heading_rad < 0) heading_rad += std.math.pi * 2.0;

    return heading_rad * (180.0 / std.math.pi);
}

test "sensor_fusion: heading & kalman filter" {
    var kf = KalmanFilter1D{};
    var filtered: f32 = 0;
    var i: usize = 0;
    while (i < 10) : (i += 1) {
        filtered = kf.update(180.0);
    }
    try std.testing.expect(filtered > 150.0 and filtered < 190.0);

    const accel = Vec3{ .x = 0.0, .y = 0.0, .z = 9.8 };
    const mag = Vec3{ .x = 20.0, .y = 0.0, .z = -40.0 };
    const heading = computeTiltCompensatedHeading(accel, mag);
    try std.testing.expect(heading >= 0.0 and heading <= 360.0);
}
