//! Destructible Defense Bunker Bitmasks & Crater Carving.

pub const NUM_BUNKERS: usize = 3;
pub const BUNKER_W: usize = 12;
pub const BUNKER_H: usize = 8;

#[derive(Clone, Debug)]
pub struct Bunker {
    pub blocks: [[bool; BUNKER_W]; BUNKER_H],
    pub x: f32,
    pub y: f32,
    pub width: f32,
    pub height: f32,
}

impl Bunker {
    pub fn new(x: f32, y: f32, width: f32, height: f32) -> Self {
        let mut b = Self {
            blocks: [[true; BUNKER_W]; BUNKER_H],
            x,
            y,
            width,
            height,
        };
        b.carve_initial_shape();
        b
    }

    /// Carves the classic 1978 Space Invaders arch opening and top corners
    fn carve_initial_shape(&mut self) {
        // Top-left and top-right corner notch
        self.blocks[0][0] = false;
        self.blocks[0][1] = false;
        self.blocks[0][BUNKER_W - 1] = false;
        self.blocks[0][BUNKER_W - 2] = false;

        // Bottom archway tunnel
        for r in (BUNKER_H - 3)..BUNKER_H {
            for c in 3..(BUNKER_W - 3) {
                self.blocks[r][c] = false;
            }
        }
    }

    pub fn reset(&mut self) {
        self.blocks = [[true; BUNKER_W]; BUNKER_H];
        self.carve_initial_shape();
    }

    /// Carves an explosion crater around (hit_x, hit_y) in world coordinates.
    /// Returns true if at least one block was eroded.
    pub fn carve_crater(&mut self, hit_x: f32, hit_y: f32, radius_blocks: i32) -> bool {
        if hit_x < self.x || hit_x > self.x + self.width ||
           hit_y < self.y || hit_y > self.y + self.height {
            return false;
        }

        let block_w = self.width / (BUNKER_W as f32);
        let block_h = self.height / (BUNKER_H as f32);

        let center_c = ((hit_x - self.x) / block_w) as i32;
        let center_r = ((hit_y - self.y) / block_h) as i32;

        let mut eroded = false;
        for dr in -radius_blocks..=radius_blocks {
            for dc in -radius_blocks..=radius_blocks {
                let r = center_r + dr;
                let c = center_c + dc;
                if r >= 0 && r < (BUNKER_H as i32) && c >= 0 && c < (BUNKER_W as i32) {
                    if dr * dr + dc * dc <= radius_blocks * radius_blocks + 1 {
                        if self.blocks[r as usize][c as usize] {
                            self.blocks[r as usize][c as usize] = false;
                            eroded = true;
                        }
                    }
                }
            }
        }
        eroded
    }

    pub fn test_collision(&self, px: f32, py: f32) -> bool {
        if px < self.x || px > self.x + self.width ||
           py < self.y || py > self.y + self.height {
            return false;
        }

        let block_w = self.width / (BUNKER_W as f32);
        let block_h = self.height / (BUNKER_H as f32);

        let c = ((px - self.x) / block_w) as usize;
        let r = ((py - self.y) / block_h) as usize;

        if r < BUNKER_H && c < BUNKER_W {
            self.blocks[r][c]
        } else {
            false
        }
    }
}
