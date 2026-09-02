//! Advanced PUCT Monte Carlo Tree Search (MCTS) Engine for Baduk.

use crate::board::{GoBoard, Point, opponent, EMPTY, BLACK, MAX_POINTS};
use crate::patterns::{evaluate_shape_score, is_real_eye};
use crate::joseki::JosekiBook;
use crate::benson::evaluate_territory;

#[derive(Clone, Debug)]
pub struct MctsNode {
    pub point: Point,
    pub color: u8,
    pub visits: u32,
    pub total_reward: f32,
    pub prior: f32,
    pub children: Vec<MctsNode>,
}

impl MctsNode {
    pub fn new(point: Point, color: u8, prior: f32) -> Self {
        Self {
            point,
            color,
            visits: 0,
            total_reward: 0.0,
            prior,
            children: Vec::new(),
        }
    }

    #[inline(always)]
    pub fn q_value(&self) -> f32 {
        if self.visits == 0 {
            0.5
        } else {
            self.total_reward / (self.visits as f32)
        }
    }
}

pub struct MctsBot {
    pub max_rollouts: usize,
    pub max_depth: usize,
    pub cpuct: f32,
    pub rng_seed: u64,
}

impl MctsBot {
    pub fn new(tier: usize) -> Self {
        // Tier 0 = Apprentice (1-kyu), Tier 1 = Master (3-dan), Tier 2 = Grandmaster (9-dan)
        let (max_rollouts, max_depth, cpuct) = match tier {
            0 => (300, 25, 1.4),
            1 => (1500, 45, 1.25),
            _ => (5000, 80, 1.1),
        };

        Self {
            max_rollouts,
            max_depth,
            cpuct,
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
        let opp = opponent(color);

        // 1. URGENT ATARI DEFENSE & CAPTURE
        if let Some(urgent) = self.find_urgent_atari_move(board, color, opp) {
            let mut hm = [0.0; MAX_POINTS];
            hm[board.idx(urgent.x as usize, urgent.y as usize)] = 1.0;
            return (urgent, 0.88, hm);
        }

        // 2. OPENING JOSEKI BOOK QUERY (Instant Grandmaster opening)
        if board.move_count <= 8 {
            if let Some(joseki_pt) = JosekiBook::get_opening_move(board, color) {
                if board.is_legal(joseki_pt.x as usize, joseki_pt.y as usize, color) {
                    let mut hm = [0.0; MAX_POINTS];
                    hm[board.idx(joseki_pt.x as usize, joseki_pt.y as usize)] = 1.0;
                    return (joseki_pt, 0.58, hm);
                }
            }
        }

        // 3. ROOT NODE EXPANSION & PRIORS
        let mut root = MctsNode::new(Point::PASS, opponent(color), 1.0);
        self.expand_node(&mut root, board, color);

        if root.children.is_empty() {
            return (Point::PASS, 0.5, [0.0; MAX_POINTS]);
        }

        // 4. MCTS ITERATIONS (Selection, Expansion, Heavy Playout, Backprop)
        for _ in 0..self.max_rollouts {
            let mut sim_board = board.clone();
            self.mcts_step(&mut root, &mut sim_board, color);
        }

        // 5. EXTRACT BEST MOVE & WINRATE HEATMAP
        let mut heatmap = [0.0f32; MAX_POINTS];
        let mut best_visits = 0;
        let mut best_pt = root.children[0].point;
        let mut best_winrate = 0.5;

        for child in &root.children {
            let idx = board.idx(child.point.x as usize, child.point.y as usize);
            let winrate = child.q_value();
            heatmap[idx] = winrate;

            if child.visits > best_visits {
                best_visits = child.visits;
                best_pt = child.point;
                best_winrate = winrate;
            }
        }

        (best_pt, best_winrate, heatmap)
    }

    fn expand_node(&mut self, node: &mut MctsNode, board: &GoBoard, color: u8) {
        let size = board.size;
        let mut candidates = Vec::with_capacity(size * size);
        let mut sum_priors = 0.0f32;

        for y in 0..size {
            for x in 0..size {
                if board.grid[board.idx(x, y)] != EMPTY { continue; }
                if !board.is_legal(x, y, color) { continue; }
                if is_real_eye(board, x, y, color) { continue; }

                let shape_score = evaluate_shape_score(board, x, y, color);
                let prior = (shape_score / 20.0).max(1.0).min(50.0);
                sum_priors += prior;
                candidates.push((Point::new(x, y), prior));
            }
        }

        if sum_priors > 0.0 {
            for (pt, prior) in candidates {
                let norm_prior = prior / sum_priors;
                node.children.push(MctsNode::new(pt, color, norm_prior));
            }
        }
    }

    fn mcts_step(&mut self, node: &mut MctsNode, board: &mut GoBoard, root_color: u8) -> f32 {
        if node.children.is_empty() {
            if node.visits == 0 {
                // Leaf rollout
                let reward = self.heavy_playout(board, node.color, root_color);
                node.visits += 1;
                node.total_reward += reward;
                return reward;
            } else {
                // Expand leaf
                let next_color = opponent(node.color);
                self.expand_node(node, board, next_color);
                if node.children.is_empty() {
                    let reward = self.evaluate_board(board, root_color);
                    node.visits += 1;
                    node.total_reward += reward;
                    return reward;
                }
            }
        }

        // Select child with best PUCT score
        let total_sqrt = (node.visits as f32).sqrt().max(1.0);
        let mut best_score = -999999.0;
        let mut best_idx = 0;

        for (i, child) in node.children.iter().enumerate() {
            let uct = child.q_value() + self.cpuct * child.prior * (total_sqrt / (1.0 + child.visits as f32));
            if uct > best_score {
                best_score = uct;
                best_idx = i;
            }
        }

        let child = &mut node.children[best_idx];
        let pt = child.point;
        let child_color = child.color;
        board.play_move(pt, child_color);

        let reward = self.mcts_step(child, board, root_color);
        node.visits += 1;
        node.total_reward += reward;
        reward
    }

    fn heavy_playout(&mut self, board: &mut GoBoard, start_color: u8, root_color: u8) -> f32 {
        let mut turn = opponent(start_color);
        let mut passes = 0;

        for _ in 0..self.max_depth {
            if passes >= 2 { break; }

            if let Some(p) = self.select_heavy_playout_move(board, turn) {
                passes = 0;
                board.play_move(p, turn);
            } else {
                passes += 1;
            }
            turn = opponent(turn);
        }

        self.evaluate_board(board, root_color)
    }

    fn select_heavy_playout_move(&mut self, board: &GoBoard, color: u8) -> Option<Point> {
        let opp = opponent(color);

        // 1. Defend/Attack Atari in playouts
        if let Some(urgent) = self.find_urgent_atari_move(board, color, opp) {
            return Some(urgent);
        }

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

    fn evaluate_board(&self, board: &GoBoard, root_color: u8) -> f32 {
        let (res, _) = evaluate_territory(board, 6.5);
        if root_color == BLACK {
            if res.black_score > res.white_score { 1.0 } else { 0.0 }
        } else {
            if res.white_score > res.black_score { 1.0 } else { 0.0 }
        }
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
                        let mut curr = root;
                        loop {
                            let n_count = board.neighbors(curr, &mut neighbors_buf);
                            for i in 0..n_count {
                                let n_idx = neighbors_buf[i];
                                if board.grid[n_idx] == EMPTY {
                                    let (nx, ny) = board.coords(n_idx);
                                    if board.is_legal(nx, ny, color) {
                                        let mut sim = board.clone();
                                        if sim.play_move(Point::new(nx, ny), color) {
                                            let new_root = sim.find_group_root(sim.idx(nx, ny));
                                            if sim.liberties[new_root] >= 2 {
                                                return Some(Point::new(nx, ny));
                                            }
                                        }
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
}
