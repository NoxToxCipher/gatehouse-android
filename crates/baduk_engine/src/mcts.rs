use crate::board::{GoBoard, Point, opponent, EMPTY, BLACK, MAX_POINTS};
use crate::patterns::evaluate_shape_score;
use crate::benson::evaluate_territory;

pub struct MctsBot {
    pub max_rollouts: usize,
    pub max_depth: usize,
    pub exploration_c: f32,
    pub rng_seed: u64,
}

impl MctsBot {
    pub fn new(tier: usize) -> Self {
        // Tier 0 = Apprentice (1-kyu), Tier 1 = Master (3-dan), Tier 2 = Grandmaster (9-dan)
        let (max_rollouts, max_depth, exploration_c) = match tier {
            0 => (150, 20, 1.4),
            1 => (800, 35, 1.2),
            _ => (2500, 60, 1.0),
        };

        Self {
            max_rollouts,
            max_depth,
            exploration_c,
            rng_seed: 0x8844_AACC_EEFF_0011,
        }
    }

    fn next_rand(&mut self) -> u32 {
        self.rng_seed = self.rng_seed.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        (self.rng_seed >> 32) as u32
    }

    /// Search for the best move for `color` on `board`.
    /// Returns: (best_point, winrate, heatmap)
    pub fn search(&mut self, board: &GoBoard, color: u8) -> (Point, f32, [f32; MAX_POINTS]) {
        let size = board.size;
        let opp = opponent(color);

        // 1. URGENT ATARI DEFENSE & CAPTURE
        if let Some(urgent) = self.find_urgent_atari_move(board, color, opp) {
            let mut hm = [0.0; MAX_POINTS];
            hm[board.idx(urgent.x as usize, urgent.y as usize)] = 1.0;
            return (urgent, 0.85, hm);
        }

        // 2. GENERATE & FILTER LEGAL CANDIDATE MOVES
        let mut candidates = Vec::with_capacity(size * size);
        for y in 0..size {
            for x in 0..size {
                if board.grid[board.idx(x, y)] != EMPTY { continue; }
                if !board.is_legal(x, y, color) { continue; }

                // Calculate local pattern heuristic
                let shape_score = evaluate_shape_score(board, x, y, color);
                candidates.push((Point::new(x, y), shape_score));
            }
        }

        if candidates.is_empty() {
            return (Point::PASS, 0.5, [0.0; MAX_POINTS]);
        }

        // Sort by shape heuristic to evaluate top moves deeply
        candidates.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

        let num_eval = candidates.len().min(if self.max_rollouts > 1000 { 16 } else { 10 });
        let rollouts_per_cand = (self.max_rollouts / num_eval).max(15);

        let mut heatmap = [0.0f32; MAX_POINTS];
        let mut best_score = -999999.0;
        let mut best_pt = candidates[0].0;
        let mut best_winrate = 0.5;

        for i in 0..num_eval {
            let (pt, shape_score) = candidates[i];
            let winrate = self.evaluate_move_rollouts(board, pt, color, rollouts_per_cand, self.max_depth);
            let idx = board.idx(pt.x as usize, pt.y as usize);
            heatmap[idx] = winrate;

            let total_score = shape_score * 0.35 + winrate * 65.0;
            if total_score > best_score {
                best_score = total_score;
                best_pt = pt;
                best_winrate = winrate;
            }
        }

        (best_pt, best_winrate, heatmap)
    }

    fn find_urgent_atari_move(&self, board: &GoBoard, color: u8, opp: u8) -> Option<Point> {
        let size = board.size;
        let mut neighbors_buf = [0usize; 4];

        // A. Defend friendly group in atari (1 liberty)
        for y in 0..size {
            for x in 0..size {
                let idx = board.idx(x, y);
                if board.grid[idx] == color {
                    let root = board.find_group_root(idx);
                    if board.liberties[root] == 1 {
                        // Find the 1 liberty
                        let mut curr = root;
                        loop {
                            let n_count = board.neighbors(curr, &mut neighbors_buf);
                            for i in 0..n_count {
                                let n_idx = neighbors_buf[i];
                                if board.grid[n_idx] == EMPTY {
                                    let (nx, ny) = board.coords(n_idx);
                                    if board.is_legal(nx, ny, color) {
                                        return Some(Point::new(nx, ny));
                                    }
                                }
                            }
                            curr = board.next_stone[curr] as usize;
                            if curr == root { break; }
                        }
                    }
                }
            }
        }

        // B. Capture opponent group in atari (1 liberty)
        for y in 0..size {
            for x in 0..size {
                let idx = board.idx(x, y);
                if board.grid[idx] == opp {
                    let root = board.find_group_root(idx);
                    if board.liberties[root] == 1 {
                        let mut curr = root;
                        loop {
                            let n_count = board.neighbors(curr, &mut neighbors_buf);
                            for i in 0..n_count {
                                let n_idx = neighbors_buf[i];
                                if board.grid[n_idx] == EMPTY {
                                    let (nx, ny) = board.coords(n_idx);
                                    if board.is_legal(nx, ny, color) {
                                        return Some(Point::new(nx, ny));
                                    }
                                }
                            }
                            curr = board.next_stone[curr] as usize;
                            if curr == root { break; }
                        }
                    }
                }
            }
        }

        None
    }

    fn evaluate_move_rollouts(
        &mut self,
        board: &GoBoard,
        first_move: Point,
        color: u8,
        num_rollouts: usize,
        max_depth: usize,
    ) -> f32 {
        let mut wins = 0;
        let opp = opponent(color);

        for _ in 0..num_rollouts {
            let mut sim = board.clone();
            if !sim.play_move(first_move, color) {
                continue;
            }

            let mut turn = opp;
            let mut passes = 0;

            for _ in 0..max_depth {
                if passes >= 2 { break; }

                let valid_pt = self.select_rollout_move(&sim, turn);
                if let Some(p) = valid_pt {
                    passes = 0;
                    sim.play_move(p, turn);
                } else {
                    passes += 1;
                }
                turn = opponent(turn);
            }

            // Area territory evaluation
            let (res, _) = evaluate_territory(&sim, 6.5);
            if color == BLACK {
                if res.black_score > res.white_score { wins += 1; }
            } else {
                if res.white_score > res.black_score { wins += 1; }
            }
        }

        (wins as f32) / (num_rollouts as f32)
    }

    fn select_rollout_move(&mut self, board: &GoBoard, color: u8) -> Option<Point> {
        let size = board.size;
        let mut legal_moves = [Point::PASS; MAX_POINTS];
        let mut count = 0;

        for y in 0..size {
            for x in 0..size {
                if board.grid[board.idx(x, y)] == EMPTY && board.is_legal(x, y, color) {
                    legal_moves[count] = Point::new(x, y);
                    count += 1;
                }
            }
        }

        if count == 0 {
            return None;
        }

        let pick = (self.next_rand() as usize) % count;
        Some(legal_moves[pick])
    }
}
