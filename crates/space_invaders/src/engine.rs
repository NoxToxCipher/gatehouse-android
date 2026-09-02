//! Space Invaders State Machine & Step Engine.

use crate::invaders::{InvaderFleet, COLS, ROWS};
use crate::bunkers::{Bunker, NUM_BUNKERS};
use crate::projectiles::ProjectilePool;

pub const EVENT_MARCH_STEP: u32    = 1 << 0;
pub const EVENT_PLAYER_FIRED: u32  = 1 << 1;
pub const EVENT_ALIEN_KILLED: u32  = 1 << 2;
pub const EVENT_BUNKER_HIT: u32    = 1 << 3;
pub const EVENT_PLAYER_HIT: u32    = 1 << 4;
pub const EVENT_UFO_KILLED: u32    = 1 << 5;
pub const EVENT_WAVE_CLEARED: u32  = 1 << 6;
pub const EVENT_GAME_OVER: u32     = 1 << 7;

#[derive(Clone, Debug)]
pub struct GameState {
    pub fleet: InvaderFleet,
    pub bunkers: [Bunker; NUM_BUNKERS],
    pub projectiles: ProjectilePool,
    pub player_x: f32,
    pub player_target_x: f32,
    pub player_speed: f32,
    pub player_lives: u32,
    pub score: u32,
    pub high_score: u32,
    pub wave: u32,
    pub game_over: bool,
    pub game_won: bool,
    pub ufo_x: f32,
    pub ufo_active: bool,
    pub ufo_speed: f32,
    pub ufo_timer_ms: f32,
    pub screen_w: f32,
    pub screen_h: f32,
    pub col_spacing: f32,
    pub row_spacing: f32,
    pub player_fire_cooldown_ms: f32,
    pub march_note_idx: u8,
    pub rng_seed: u32,
}

impl GameState {
    pub fn new(screen_w: f32, screen_h: f32) -> Self {
        let col_spacing = (screen_w - 40.0) / (COLS as f32);
        let row_spacing = 30.0;

        let bunker_w = col_spacing * 1.8;
        let bunker_h = 24.0;
        let bunker_y = screen_h - 130.0;
        let bunker_spacing = (screen_w - (bunker_w * 3.0)) / 4.0;

        let bunkers = [
            Bunker::new(bunker_spacing, bunker_y, bunker_w, bunker_h),
            Bunker::new(bunker_spacing * 2.0 + bunker_w, bunker_y, bunker_w, bunker_h),
            Bunker::new(bunker_spacing * 3.0 + bunker_w * 2.0, bunker_y, bunker_w, bunker_h),
        ];

        Self {
            fleet: InvaderFleet::new(screen_w),
            bunkers,
            projectiles: ProjectilePool::new(),
            player_x: screen_w / 2.0,
            player_target_x: screen_w / 2.0,
            player_speed: 400.0,
            player_lives: 3,
            score: 0,
            high_score: 1980,
            wave: 1,
            game_over: false,
            game_won: false,
            ufo_x: -100.0,
            ufo_active: false,
            ufo_speed: 120.0,
            ufo_timer_ms: 8000.0,
            screen_w,
            screen_h,
            col_spacing,
            row_spacing,
            player_fire_cooldown_ms: 0.0,
            march_note_idx: 0,
            rng_seed: 0x50AC_E197,
        }
    }

    fn next_rand(&mut self) -> f32 {
        self.rng_seed = self.rng_seed.wrapping_mul(1664525).wrapping_add(1013904223);
        (self.rng_seed as f32) / 4294967296.0
    }

    pub fn start_new_game(&mut self) {
        self.score = 0;
        self.player_lives = 3;
        self.wave = 1;
        self.game_over = false;
        self.game_won = false;
        self.start_wave();
    }

    pub fn start_wave(&mut self) {
        self.fleet.reset_wave(self.wave, self.screen_w);
        self.projectiles.clear();
        for b in self.bunkers.iter_mut() {
            b.reset();
        }
        self.ufo_active = false;
        self.ufo_timer_ms = 8000.0 + self.next_rand() * 12000.0;
        self.player_fire_cooldown_ms = 0.0;
        self.march_note_idx = 0;
    }

    pub fn update(&mut self, dt_ms: f32, target_x: f32, fire_trigger: bool) -> u32 {
        if self.game_over {
            return EVENT_GAME_OVER;
        }

        let dt_sec = dt_ms / 1000.0;
        let mut events: u32 = 0;

        // 1. Smooth Player Movement towards Target X
        let dx = target_x - self.player_x;
        let max_move = self.player_speed * dt_sec;
        if dx.abs() <= max_move {
            self.player_x = target_x;
        } else {
            self.player_x += dx.signum() * max_move;
        }
        let cannon_half_w = 20.0;
        self.player_x = self.player_x.clamp(cannon_half_w + 10.0, self.screen_w - cannon_half_w - 10.0);

        // 2. Player Laser Firing
        self.player_fire_cooldown_ms -= dt_ms;
        if fire_trigger && self.player_fire_cooldown_ms <= 0.0 {
            let player_y = self.screen_h - 75.0;
            if self.projectiles.spawn(self.player_x, player_y, -550.0, true) {
                self.player_fire_cooldown_ms = 350.0;
                events |= EVENT_PLAYER_FIRED;
            }
        }

        // 3. Invader Fleet Marching Step
        if self.fleet.update(dt_ms, self.col_spacing, self.row_spacing) {
            events |= EVENT_MARCH_STEP;
            self.march_note_idx = (self.march_note_idx + 1) % 4;

            // Check if invaders reached bunker / player altitude
            if self.fleet.lowest_invader_y(self.row_spacing) >= (self.screen_h - 100.0) {
                self.player_lives = 0;
                self.game_over = true;
                events |= EVENT_GAME_OVER;
                return events;
            }
        }

        // 4. Invader Random Bomb Drops (from bottom-most alive aliens)
        let bomb_chance = (0.015 + (self.wave as f32 * 0.005)) * (dt_ms / 16.6);
        if self.next_rand() < bomb_chance {
            let col = (self.next_rand() * (COLS as f32)) as usize;
            for r in (0..ROWS).rev() {
                if self.fleet.is_alive(r, col) {
                    let ax = self.fleet.base_x + (col as f32 * self.col_spacing) + (self.col_spacing * 0.4);
                    let ay = self.fleet.base_y + (r as f32 * self.row_spacing) + 20.0;
                    self.projectiles.spawn(ax, ay, 240.0 + (self.wave as f32 * 20.0), false);
                    break;
                }
            }
        }

        // 5. Mystery UFO Mothership
        if self.ufo_active {
            self.ufo_x += self.ufo_speed * dt_sec;
            if self.ufo_x > self.screen_w + 60.0 {
                self.ufo_active = false;
            }
        } else {
            self.ufo_timer_ms -= dt_ms;
            if self.ufo_timer_ms <= 0.0 {
                self.ufo_active = true;
                self.ufo_x = -50.0;
                self.ufo_timer_ms = 12000.0 + self.next_rand() * 15000.0;
            }
        }

        // 6. Projectile Physics Update
        self.projectiles.update(dt_sec, self.screen_h);

        // 7. Collision Detection
        let player_y = self.screen_h - 75.0;
        let mut wave_cleared = false;

        for p in self.projectiles.pool.iter_mut() {
            if !p.active { continue; }

            if p.is_player {
                // A. Check collision with Mystery UFO
                if self.ufo_active && p.y <= 40.0 && (p.x - self.ufo_x).abs() < 30.0 {
                    p.active = false;
                    self.ufo_active = false;
                    let ufo_pts = [50, 100, 150, 300][(p.x as usize) % 4];
                    self.score += ufo_pts;
                    if self.score > self.high_score { self.high_score = self.score; }
                    events |= EVENT_UFO_KILLED;
                    continue;
                }

                // B. Check collision with Invaders
                let rel_x = p.x - self.fleet.base_x;
                let rel_y = p.y - self.fleet.base_y;
                if rel_x >= 0.0 && rel_y >= 0.0 {
                    let c = (rel_x / self.col_spacing) as usize;
                    let r = (rel_y / self.row_spacing) as usize;

                    if r < ROWS && c < COLS && self.fleet.is_alive(r, c) {
                        if let Some(pts) = self.fleet.kill_invader(r, c) {
                            p.active = false;
                            self.score += pts;
                            if self.score > self.high_score { self.high_score = self.score; }
                            events |= EVENT_ALIEN_KILLED;

                            if self.fleet.alive_count == 0 {
                                self.wave += 1;
                                self.score += 1000;
                                events |= EVENT_WAVE_CLEARED;
                                wave_cleared = true;
                            }
                            continue;
                        }
                    }
                }

                // C. Check collision with Bunkers
                for b in self.bunkers.iter_mut() {
                    if b.test_collision(p.x, p.y) {
                        b.carve_crater(p.x, p.y, 2);
                        p.active = false;
                        events |= EVENT_BUNKER_HIT;
                        break;
                    }
                }
            } else {
                // Alien Bomb vs Player
                if (p.y - player_y).abs() < 14.0 && (p.x - self.player_x).abs() < cannon_half_w {
                    p.active = false;
                    self.player_lives = self.player_lives.saturating_sub(1);
                    events |= EVENT_PLAYER_HIT;

                    if self.player_lives == 0 {
                        self.game_over = true;
                        events |= EVENT_GAME_OVER;
                    }
                    continue;
                }

                // Alien Bomb vs Bunkers
                for b in self.bunkers.iter_mut() {
                    if b.test_collision(p.x, p.y) {
                        b.carve_crater(p.x, p.y, 2);
                        p.active = false;
                        events |= EVENT_BUNKER_HIT;
                        break;
                    }
                }
            }
        }

        if wave_cleared {
            self.start_wave();
        }

        events
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_game_loop_1000_frames() {
        let mut state = GameState::new(800.0, 600.0);
        assert_eq!(state.fleet.alive_count, 55);

        for _ in 0..1000 {
            let _events = state.update(16.6, 400.0, true);
        }

        assert!(state.score >= 0);
    }
}
