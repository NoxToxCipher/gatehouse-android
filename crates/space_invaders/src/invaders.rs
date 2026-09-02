//! 55-Invader Bitboard & Fleet Stepper.

pub const ROWS: usize = 5;
pub const COLS: usize = 11;
pub const TOTAL_INVADERS: usize = ROWS * COLS;

#[derive(Clone, Copy, Debug, PartialEq)]
pub enum InvaderType {
    Squid,   // Top row (30 pts)
    Crab,    // Middle 2 rows (20 pts)
    Octopus, // Bottom 2 rows (10 pts)
}

impl InvaderType {
    pub fn score_value(&self) -> u32 {
        match self {
            InvaderType::Squid => 30,
            InvaderType::Crab => 20,
            InvaderType::Octopus => 10,
        }
    }
}

#[derive(Clone, Debug)]
pub struct InvaderFleet {
    pub grid: [u16; ROWS], // Bitmask per row (11 bits used)
    pub base_x: f32,
    pub base_y: f32,
    pub dir_x: f32,        // +1.0 or -1.0
    pub step_speed: f32,   // pixels per step
    pub step_interval_ms: f32,
    pub time_since_last_step_ms: f32,
    pub anim_frame: u8,
    pub alive_count: usize,
    pub width: f32,
    pub height: f32,
}

impl InvaderFleet {
    pub fn new(screen_w: f32) -> Self {
        let mut grid = [0u16; ROWS];
        let full_row = (1 << COLS) - 1; // 0b11111111111 (11 ones)
        for r in 0..ROWS {
            grid[r] = full_row;
        }

        Self {
            grid,
            base_x: 20.0,
            base_y: 40.0,
            dir_x: 1.0,
            step_speed: 12.0,
            step_interval_ms: 750.0,
            time_since_last_step_ms: 0.0,
            anim_frame: 0,
            alive_count: TOTAL_INVADERS,
            width: screen_w,
            height: 600.0,
        }
    }

    pub fn reset_wave(&mut self, wave: u32, screen_w: f32) {
        let full_row = (1 << COLS) - 1;
        for r in 0..ROWS {
            self.grid[r] = full_row;
        }
        self.base_x = 20.0;
        self.base_y = 40.0 + (wave.saturating_sub(1) as f32 * 10.0).min(60.0);
        self.dir_x = 1.0;
        self.alive_count = TOTAL_INVADERS;
        self.anim_frame = 0;
        self.time_since_last_step_ms = 0.0;
        self.width = screen_w;
        // Speed up as waves progress
        let base_interval = (750.0 - (wave as f32 * 40.0)).max(250.0);
        self.step_interval_ms = base_interval;
    }

    pub fn is_alive(&self, row: usize, col: usize) -> bool {
        if row >= ROWS || col >= COLS {
            return false;
        }
        (self.grid[row] & (1 << col)) != 0
    }

    pub fn kill_invader(&mut self, row: usize, col: usize) -> Option<u32> {
        if self.is_alive(row, col) {
            self.grid[row] &= !(1 << col);
            self.alive_count = self.alive_count.saturating_sub(1);

            // Recompute step interval based on remaining invaders (classic accelerating march)
            let pct_alive = (self.alive_count as f32) / (TOTAL_INVADERS as f32);
            self.step_interval_ms = 50.0 + (pct_alive * 650.0);

            let inv_type = match row {
                0 => InvaderType::Squid,
                1 | 2 => InvaderType::Crab,
                _ => InvaderType::Octopus,
            };
            Some(inv_type.score_value())
        } else {
            None
        }
    }

    /// Advances the march clock. Returns true if a step occurred.
    pub fn update(&mut self, dt_ms: f32, col_spacing: f32, row_spacing: f32) -> bool {
        self.time_since_last_step_ms += dt_ms;
        if self.time_since_last_step_ms < self.step_interval_ms {
            return false;
        }

        self.time_since_last_step_ms = 0.0;
        self.anim_frame = 1 - self.anim_frame;

        // Calculate bounding box of alive columns
        let mut min_col = COLS;
        let mut max_col = 0;
        for r in 0..ROWS {
            for c in 0..COLS {
                if (self.grid[r] & (1 << c)) != 0 {
                    if c < min_col { min_col = c; }
                    if c > max_col { max_col = c; }
                }
            }
        }

        if min_col > max_col {
            return true; // Fleet is empty (wave cleared)
        }

        let left_edge = self.base_x + (min_col as f32 * col_spacing);
        let right_edge = self.base_x + (max_col as f32 * col_spacing) + (col_spacing * 0.8);

        let margin = 20.0;
        let mut drop_down = false;

        if self.dir_x > 0.0 && right_edge >= (self.width - margin) {
            self.dir_x = -1.0;
            drop_down = true;
        } else if self.dir_x < 0.0 && left_edge <= margin {
            self.dir_x = 1.0;
            drop_down = true;
        }

        if drop_down {
            self.base_y += row_spacing * 0.6;
        } else {
            self.base_x += self.dir_x * self.step_speed;
        }

        true
    }

    pub fn lowest_invader_y(&self, row_spacing: f32) -> f32 {
        for r in (0..ROWS).rev() {
            if self.grid[r] != 0 {
                return self.base_y + (r as f32 * row_spacing);
            }
        }
        self.base_y
    }
}
