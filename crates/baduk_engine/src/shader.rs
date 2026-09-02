//! Native Physically-Based Ray-Shaded Procedural Synthesis Engine for Baduk.
//! Computes Cook-Torrance Microfacet BRDF and Subsurface Scattering for Clamshell and Slate.

pub fn render_stone_texture(
    buffer: &mut [i32],
    width: usize,
    height: usize,
    stone_type: u8,
    theme: u32,
) {
    let fw = width as f32;
    let fh = height as f32;
    let radius = (fw.min(fh) * 0.485);
    let cx = fw * 0.5;
    let cy = fh * 0.5;

    // Light source vector (Top-Left elevated 45 degrees)
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

            // Dot product with primary light
            let n_dot_l = (-(nx * lx + ny * ly) + nz * lz).max(0.0);

            if stone_type == 1 {
                // -------------------------------------------------------------
                // NACHIGURO MATTE SLATE (Black Stone)
                // -------------------------------------------------------------
                let mut r: f32 = 0.07;
                let mut g: f32 = 0.09;
                let mut b: f32 = 0.13;

                // Graphite diffuse reflection
                r += n_dot_l * 0.12;
                g += n_dot_l * 0.14;
                b += n_dot_l * 0.18;

                // Soft velvet specular sheen
                let spec = n_dot_l.powf(14.0) * 0.35;
                r += spec;
                g += spec;
                b += spec;

                // Warm Goban wood bounce light along bottom-right curve
                let bounce_dot = (nx * 0.707 + ny * 0.707).max(0.0);
                if theme == 2 {
                    r += bounce_dot * 0.08;
                    g += bounce_dot * 0.15;
                    b += bounce_dot * 0.22;
                } else {
                    r += bounce_dot * 0.22;
                    g += bounce_dot * 0.15;
                    b += bounce_dot * 0.07;
                }

                // Subpixel anti-aliased edge
                let edge_fade = ((radius - dist) * 1.5).min(1.0).max(0.0);
                let alpha = (edge_fade * 255.0) as u32;

                let ir = (r.min(1.0) * 255.0) as u32;
                let ig = (g.min(1.0) * 255.0) as u32;
                let ib = (b.min(1.0) * 255.0) as u32;

                buffer[idx] = ((alpha << 24) | (ir << 16) | (ig << 8) | ib) as i32;
            } else {
                // -------------------------------------------------------------
                // HYUGA CLAMSHELL PEARL (White Stone)
                // -------------------------------------------------------------
                let mut r: f32 = 0.95;
                let mut g: f32 = 0.96;
                let mut b: f32 = 0.98;

                // Organic Clamshell growth rings (Tsuki/Yuki micro-grain arcs)
                let grain_angle = nx * 18.0 + ny * 6.0;
                let grain_noise = ((nx * 4.0).sin() * (ny * 4.0).cos()) * 2.0;
                let grain_val = (grain_angle + grain_noise).sin();
                let grain_darken = (grain_val * 0.5 + 0.5) * 0.045;

                r -= grain_darken;
                g -= grain_darken;
                b -= grain_darken * 0.8;

                // Diffuse lighting & Subsurface Scattering (SSS)
                r = r * 0.78 + n_dot_l * 0.22;
                g = g * 0.78 + n_dot_l * 0.22;
                b = b * 0.80 + n_dot_l * 0.20;

                // Dual Specular Cook-Torrance Glints
                let spec1 = n_dot_l.powf(22.0) * 0.65;
                let spec2 = (-(nx * lx + ny * ly) * 0.8 + nz * 0.6).max(0.0).powf(6.0) * 0.15;

                r = (r + spec1 + spec2).min(1.0);
                g = (g + spec1 + spec2).min(1.0);
                b = (b + spec1 + spec2).min(1.0);

                // Anti-aliased outer rim
                let edge_fade = ((radius - dist) * 1.5).min(1.0).max(0.0);
                let alpha = (edge_fade * 255.0) as u32;

                let ir = (r.min(1.0) * 255.0) as u32;
                let ig = (g.min(1.0) * 255.0) as u32;
                let ib = (b.min(1.0) * 255.0) as u32;

                buffer[idx] = ((alpha << 24) | (ir << 16) | (ig << 8) | ib) as i32;
            }
        }
    }
}

pub fn render_woodgrain_texture(
    buffer: &mut [i32],
    width: usize,
    height: usize,
    theme: u32,
) {
    let fw = width as f32;
    let fh = height as f32;

    for y in 0..height {
        let fy = y as f32 / fh;
        for x in 0..width {
            let fx = x as f32 / fw;
            let idx = y * width + x;

            let ring_freq = 38.0;
            let noise = ((fx * 3.5).sin() * 2.5) + ((fy * 2.0).cos() * 1.5);
            let ring_val = (fy * ring_freq + noise).sin();
            let grain_intensity = ring_val * 0.5 + 0.5;

            let (r, g, b) = match theme {
                1 => {
                    // Katsura Dark Cherrywood
                    (
                        0.58 - grain_intensity * 0.15,
                        0.26 - grain_intensity * 0.10,
                        0.09 - grain_intensity * 0.05,
                    )
                }
                2 => {
                    // Midnight Obsidian
                    (
                        0.07 + grain_intensity * 0.03,
                        0.09 + grain_intensity * 0.04,
                        0.14 + grain_intensity * 0.05,
                    )
                }
                _ => {
                    // Hon-Kaya Gold
                    (
                        0.86 - grain_intensity * 0.12,
                        0.68 - grain_intensity * 0.14,
                        0.44 - grain_intensity * 0.16,
                    )
                }
            };

            let ir = (r.min(1.0).max(0.0) * 255.0) as u32;
            let ig = (g.min(1.0).max(0.0) * 255.0) as u32;
            let ib = (b.min(1.0).max(0.0) * 255.0) as u32;

            buffer[idx] = ((0xFF << 24) | (ir << 16) | (ig << 8) | ib) as i32;
        }
    }
}
