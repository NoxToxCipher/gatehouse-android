//! Procedural Shading & Texture Synthesis for Ancient Egyptian Senet.
//! Synthesizes Carved Sandstone, Gold Inlaid Cartouches, and Lapis Lazuli Spools.

pub fn render_sandstone_tile(
    buffer: &mut [i32],
    width: usize,
    height: usize,
    tile_type: u8, // 0 = Standard Dark, 1 = Standard Light, 2 = Sacred House (Gold/Lapis)
) {
    let fw = width as f32;
    let fh = height as f32;

    for y in 0..height {
        let fy = y as f32 / fh;
        for x in 0..width {
            let fx = x as f32 / fw;
            let idx = y * width + x;

            // Multi-frequency sandstone grain formula
            let grain = ((fx * 24.0).sin() * (fy * 24.0).cos()) * 0.05
                + ((fx * 8.0).cos() + (fy * 8.0).sin()) * 0.08;

            let (r, g, b) = match tile_type {
                2 => {
                    // Sacred House: Royal Lapis Lazuli & Gold Sheen
                    (
                        (0.20 + grain * 0.4).clamp(0.0, 1.0),
                        (0.12 + grain * 0.3).clamp(0.0, 1.0),
                        (0.48 + grain * 0.5).clamp(0.0, 1.0),
                    )
                }
                1 => {
                    // Light Sandstone
                    (
                        (0.18 + grain * 0.3).clamp(0.0, 1.0),
                        (0.16 + grain * 0.3).clamp(0.0, 1.0),
                        (0.22 + grain * 0.3).clamp(0.0, 1.0),
                    )
                }
                _ => {
                    // Dark Obsidian Sandstone
                    (
                        (0.09 + grain * 0.2).clamp(0.0, 1.0),
                        (0.08 + grain * 0.2).clamp(0.0, 1.0),
                        (0.12 + grain * 0.2).clamp(0.0, 1.0),
                    )
                }
            };

            let ir = (r * 255.0) as u32;
            let ig = (g * 255.0) as u32;
            let ib = (b * 255.0) as u32;

            buffer[idx] = ((0xFF << 24) | (ir << 16) | (ig << 8) | ib) as i32;
        }
    }
}

pub fn render_piece_texture(
    buffer: &mut [i32],
    width: usize,
    height: usize,
    is_pharaoh: bool,
) {
    let fw = width as f32;
    let fh = height as f32;
    let radius = fw.min(fh) * 0.48;
    let cx = fw * 0.5;
    let cy = fh * 0.5;

    // Light source (Top-Left 45 deg)
    let lx: f32 = -0.5773;
    let ly: f32 = -0.5773;
    let lz: f32 = 0.5773;

    for y in 0..height {
        let fy = y as f32 - cy;
        for x in 0..width {
            let fx = x as f32 - cx;
            let dist = (fx * fx + fy * fy).sqrt();
            let idx = y * width + x;

            if dist > radius {
                buffer[idx] = 0x00000000;
                continue;
            }

            let nx = fx / radius;
            let ny = fy / radius;
            let n_lens = 1.0 - (nx * nx + ny * ny);
            let nz = if n_lens > 0.0 { n_lens.sqrt() } else { 0.0 };

            let n_dot_l = (-(nx * lx + ny * ly) + nz * lz).max(0.0);

            let (r, g, b) = if is_pharaoh {
                // Pharaoh Ivory Cone with Golden Sun Specular
                let mut pr = 0.98;
                let mut pg = 0.92;
                let mut pb = 0.65;

                pr = pr * 0.75 + n_dot_l * 0.25;
                pg = pg * 0.75 + n_dot_l * 0.25;
                pb = pb * 0.70 + n_dot_l * 0.20;

                let spec = n_dot_l.powf(18.0) * 0.55;
                (pr + spec, pg + spec, pb + spec * 0.8)
            } else {
                // Anubis Lapis Lazuli Spool with Turquoise Rim
                let mut pr = 0.12;
                let mut pg = 0.45;
                let mut pb = 0.88;

                pr = pr * 0.70 + n_dot_l * 0.20;
                pg = pg * 0.70 + n_dot_l * 0.25;
                pb = pb * 0.75 + n_dot_l * 0.25;

                let spec = n_dot_l.powf(16.0) * 0.45;
                (pr + spec * 0.6, pg + spec * 0.8, pb + spec)
            };

            let edge_fade = ((radius - dist) * 1.5).clamp(0.0, 1.0);
            let alpha = (edge_fade * 255.0) as u32;

            let ir = (r.clamp(0.0, 1.0) * 255.0) as u32;
            let ig = (g.clamp(0.0, 1.0) * 255.0) as u32;
            let ib = (b.clamp(0.0, 1.0) * 255.0) as u32;

            buffer[idx] = ((alpha << 24) | (ir << 16) | (ig << 8) | ib) as i32;
        }
    }
}
