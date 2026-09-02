//! Projectiles, Lasers, Alien Bombs & Swept AABB Collision.

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Projectile {
    pub x: f32,
    pub y: f32,
    pub vy: f32,
    pub is_player: bool,
    pub active: bool,
}

pub const MAX_PROJECTILES: usize = 32;

#[derive(Clone, Debug)]
pub struct ProjectilePool {
    pub pool: [Projectile; MAX_PROJECTILES],
}

impl ProjectilePool {
    pub fn new() -> Self {
        Self {
            pool: [Projectile {
                x: 0.0,
                y: 0.0,
                vy: 0.0,
                is_player: false,
                active: false,
            }; MAX_PROJECTILES],
        }
    }

    pub fn clear(&mut self) {
        for p in self.pool.iter_mut() {
            p.active = false;
        }
    }

    pub fn spawn(&mut self, x: f32, y: f32, vy: f32, is_player: bool) -> bool {
        for p in self.pool.iter_mut() {
            if !p.active {
                p.x = x;
                p.y = y;
                p.vy = vy;
                p.is_player = is_player;
                p.active = true;
                return true;
            }
        }
        false
    }

    pub fn update(&mut self, dt_sec: f32, screen_h: f32) {
        for p in self.pool.iter_mut() {
            if p.active {
                p.y += p.vy * dt_sec;
                if p.y < -20.0 || p.y > screen_h + 20.0 {
                    p.active = false;
                }
            }
        }
    }
}
