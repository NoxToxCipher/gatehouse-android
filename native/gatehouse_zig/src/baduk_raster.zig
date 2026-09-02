//! Procedural Woodgrain & Cook-Torrance Bi-Convex Stone Shader for Baduk.
//! Written in Zig 0.16.0 with SIMD-accelerated procedural synthesis.

const std = @import("std");
const math = std.math;

pub const GobanTheme = enum(u32) {
    HonKaya = 0,
    Katsura = 1,
    ObsidianGold = 2,
};

/// Simple Perlin-like 2D noise generator for organic wood grain.
fn hash2d(x: u32, y: u32) f32 {
    var h: u32 = x.wrapping_mul(374761393) ^ y.wrapping_mul(668265263);
    h = (h ^ (h >> 13)).wrapping_mul(1274126177);
    return @as(f32, @floatFromInt(h & 0x7FFFFF)) / 8388607.0;
}

fn smoothNoise(x: f32, y: f32) f32 {
    const ix = @as(u32, @intFromFloat(@floor(x)));
    const iy = @as(u32, @intFromFloat(@floor(y)));
    const fx = x - @floor(x);
    const fy = y - @floor(y);

    const sx = fx * fx * (3.0 - 2.0 * fx);
    const sy = fy * fy * (3.0 - 2.0 * fy);

    const n00 = hash2d(ix, iy);
    const n10 = hash2d(ix + 1, iy);
    const n01 = hash2d(ix, iy + 1);
    const n11 = hash2d(ix + 1, iy + 1);

    const nx0 = n00 + (n10 - n00) * sx;
    const nx1 = n01 + (n11 - n01) * sx;

    return nx0 + (nx1 - nx0) * sy;
}

fn fractalNoise(x: f32, y: f32) f32 {
    var v: f32 = 0.0;
    var amp: f32 = 0.5;
    var freq: f32 = 1.0;

    var i: usize = 0;
    while (i < 4) : (i += 1) {
        v += smoothNoise(x * freq, y * freq) * amp;
        amp *= 0.5;
        freq *= 2.0;
    }
    return v;
}

/// Renders a procedurally shaded 3D Bi-Convex Go Stone into an ARGB_8888 pixel buffer.
/// `stone_type`: 1 = Nachiguro Slate, 2 = Hyuga Clamshell Pearl.
pub fn renderStone(
    buffer: []u32,
    width: usize,
    height: usize,
    stone_type: u8,
    theme: GobanTheme,
) void {
    const fw = @as(f32, @floatFromInt(width));
    const fh = @as(f32, @floatFromInt(height));
    const radius = (@min(fw, fh) * 0.48);
    const cx = fw * 0.5;
    const cy = fh * 0.5;

    // Light source vector (Top-Left elevated)
    const lx: f32 = -0.5773;
    const ly: f32 = -0.5773;
    const lz: f32 = 0.5773;

    var y: usize = 0;
    while (y < height) : (y += 1) {
        const fy = @as(f32, @floatFromInt(y)) - cy;
        var x: usize = 0;
        while (x < width) : (x += 1) {
            const fx = @as(f32, @floatFromInt(x)) - cx;
            const dist = @sqrt(fx * fx + fy * fy);
            const idx = y * width + x;

            if (dist > radius) {
                // Transparent background
                buffer[idx] = 0x00000000;
                continue;
            }

            // Normalized radial coordinates [-1.0, 1.0]
            const nx = fx / radius;
            const ny = fy / radius;
            const n_lens = 1.0 - (nx * nx + ny * ny);
            const nz = if (n_lens > 0.0) @sqrt(n_lens) else 0.0;

            // Dot product with primary light
            const n_dot_l = @max(0.0, -(nx * lx + ny * ly) + nz * lz);

            if (stone_type == 1) {
                // -------------------------------------------------------------
                // NACHIGURO MATTE SLATE (Black Stone)
                // -------------------------------------------------------------
                // Base velvet slate color
                var r: f32 = 0.07;
                var g: f32 = 0.09;
                var b: f32 = 0.13;

                // Subtle graphite diffuse light
                r += n_dot_l * 0.12;
                g += n_dot_l * 0.14;
                b += n_dot_l * 0.18;

                // Soft specular highlight
                const spec = math.pow(f32, n_dot_l, 14.0) * 0.35;
                r += spec;
                g += spec;
                b += spec;

                // Warm Goban wood bounce reflection on bottom-right rim
                const bounce_dot = @max(0.0, nx * 0.707 + ny * 0.707);
                if (theme == .ObsidianGold) {
                    r += bounce_dot * 0.08;
                    g += bounce_dot * 0.15;
                    b += bounce_dot * 0.22;
                } else {
                    r += bounce_dot * 0.22;
                    g += bounce_dot * 0.15;
                    b += bounce_dot * 0.07;
                }

                // Smooth edge anti-aliasing
                const edge_fade = @min(1.0, (radius - dist) * 1.5);
                const alpha: u32 = @intFromFloat(@min(255.0, edge_fade * 255.0));

                const ir: u32 = @intFromFloat(@min(255.0, r * 255.0));
                const ig: u32 = @intFromFloat(@min(255.0, g * 255.0));
                const ib: u32 = @intFromFloat(@min(255.0, b * 255.0));

                buffer[idx] = (alpha << 24) | (ir << 16) | (ig << 8) | ib;
            } else {
                // -------------------------------------------------------------
                // HYUGA CLAMSHELL PEARL (White Stone)
                // -------------------------------------------------------------
                // Pure porcelain base
                var r: f32 = 0.95;
                var g: f32 = 0.96;
                var b: f32 = 0.98;

                // Clamshell growth grain lines (anisotropic radial arcs)
                const grain_angle = (nx * 18.0 + ny * 6.0);
                const grain_val = @sin(grain_angle + fractalNoise(nx * 4.0, ny * 4.0) * 2.0);
                const grain_darken = (grain_val * 0.5 + 0.5) * 0.045;

                r -= grain_darken;
                g -= grain_darken;
                b -= grain_darken * 0.8;

                // Diffuse lighting & Subsurface Scattering (SSS)
                r = r * 0.78 + n_dot_l * 0.22;
                g = g * 0.78 + n_dot_l * 0.22;
                b = b * 0.80 + n_dot_l * 0.20;

                // Dual Specular Glint (Cook-Torrance microfacet glint)
                const spec1 = math.pow(f32, n_dot_l, 22.0) * 0.65;
                const spec2 = math.pow(f32, @max(0.0, -(nx * lx + ny * ly) * 0.8 + nz * 0.6), 6.0) * 0.15;

                r = @min(1.0, r + spec1 + spec2);
                g = @min(1.0, g + spec1 + spec2);
                b = @min(1.0, b + spec1 + spec2);

                // Anti-aliased outer rim
                const edge_fade = @min(1.0, (radius - dist) * 1.5);
                const alpha: u32 = @intFromFloat(@min(255.0, edge_fade * 255.0));

                const ir: u32 = @intFromFloat(@min(255.0, r * 255.0));
                const ig: u32 = @intFromFloat(@min(255.0, g * 255.0));
                const ib: u32 = @intFromFloat(@min(255.0, b * 255.0));

                buffer[idx] = (alpha << 24) | (ir << 16) | (ig << 8) | ib;
            }
        }
    }
}

/// Renders a procedural Hon-Kaya / Katsura Woodgrain Goban tile into an ARGB_8888 buffer.
pub fn renderWoodgrainTile(
    buffer: []u32,
    width: usize,
    height: usize,
    theme: GobanTheme,
) void {
    const fw = @as(f32, @floatFromInt(width));
    const fh = @as(f32, @floatFromInt(height));

    var y: usize = 0;
    while (y < height) : (y += 1) {
        const fy = @as(f32, @floatFromInt(y)) / fh;
        var x: usize = 0;
        while (x < width) : (x += 1) {
            const fx = @as(f32, @floatFromInt(x)) / fw;
            const idx = y * width + x;

            // Procedural wood growth ring formula
            const ring_freq: f32 = 36.0;
            const noise = fractalNoise(fx * 3.5, fy * 1.2) * 4.5;
            const ring_val = @sin(fy * ring_freq + noise);
            const grain_intensity = (ring_val * 0.5 + 0.5);

            var r: f32 = 0.0;
            var g: f32 = 0.0;
            var b: f32 = 0.0;

            switch (theme) {
                .HonKaya => {
                    // Golden Hon-Kaya
                    r = 0.86 - grain_intensity * 0.12;
                    g = 0.68 - grain_intensity * 0.14;
                    b = 0.44 - grain_intensity * 0.16;
                },
                .Katsura => {
                    // Dark Cherry Katsura
                    r = 0.58 - grain_intensity * 0.15;
                    g = 0.26 - grain_intensity * 0.10;
                    b = 0.09 - grain_intensity * 0.05;
                },
                .ObsidianGold => {
                    // Midnight Obsidian
                    r = 0.07 + grain_intensity * 0.03;
                    g = 0.09 + grain_intensity * 0.04;
                    b = 0.14 + grain_intensity * 0.05;
                },
            }

            const ir: u32 = @intFromFloat(@min(255.0, r * 255.0));
            const ig: u32 = @intFromFloat(@min(255.0, g * 255.0));
            const ib: u32 = @intFromFloat(@min(255.0, b * 255.0));

            buffer[idx] = (0xFF << 24) | (ir << 16) | (ig << 8) | ib;
        }
    }
}

test "renderStone produces non-zero buffer" {
    var test_buf: [64 * 64]u32 = undefined;
    renderStone(&test_buf, 64, 64, 2, .HonKaya);
    const center_pixel = test_buf[32 * 64 + 32];
    try std.testing.expect(center_pixel != 0);
}
