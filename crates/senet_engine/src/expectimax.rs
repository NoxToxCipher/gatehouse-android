//! Stochastic Expectimax Tree Search AI for Senet.
//! Evaluates probability-weighted casting stick outcomes across multi-tier difficulties.

use crate::board::{SenetBoard, BEAR_OFF_POS, HOUSE_BEAUTY, HOUSE_WATER, PIECE_COUNT};

pub const STICK_PROBABILITIES: [(u8, f32); 5] = [
    (1, 4.0 / 16.0), // 25% (1 White Bone)
    (2, 6.0 / 16.0), // 37.5% (2 White Bones)
    (3, 4.0 / 16.0), // 25% (3 White Bones)
    (4, 1.0 / 16.0), // 6.25% (4 White Bones)
    (5, 1.0 / 16.0), // 6.25% (0 White Bones)
];

pub struct ExpectimaxAI {
    pub difficulty_tier: u8, // 0 = Scribe (depth 1), 1 = Priest (depth 3), 2 = Anubis (depth 5)
}

impl ExpectimaxAI {
    pub fn new(tier: u8) -> Self {
        Self {
            difficulty_tier: tier.min(2),
        }
    }

    pub fn evaluate_position(&self, board: &SenetBoard, turn: u8) -> f32 {
        if board.is_game_over() {
            if board.winner() == Some(turn) {
                return 10000.0;
            } else {
                return -10000.0;
            }
        }

        let is_white = turn == 0;
        let (my_pieces, opp_pieces) = if is_white {
            (&board.white_pieces, &board.black_pieces)
        } else {
            (&board.black_pieces, &board.white_pieces)
        };

        let (my_borne, opp_borne) = if is_white {
            (board.white_borne_off as f32, board.black_borne_off as f32)
        } else {
            (board.black_borne_off as f32, board.white_borne_off as f32)
        };

        let mut score: f32 = 0.0;

        // 1. Borne off progress
        score += my_borne * 120.0;
        score -= opp_borne * 120.0;

        // 2. Race progress (sum of squares along 30-track)
        let my_progress: f32 = my_pieces.iter().map(|&p| p as f32).sum();
        let opp_progress: f32 = opp_pieces.iter().map(|&p| p as f32).sum();
        score += my_progress * 1.5;
        score -= opp_progress * 1.5;

        // 3. Defensive Protection Blocks (2 adjacent friendly pieces)
        for i in 0..PIECE_COUNT {
            let p = my_pieces[i];
            if p < BEAR_OFF_POS {
                if board.is_piece_protected(my_pieces, p) {
                    score += 18.0;
                }
                if p == HOUSE_BEAUTY {
                    score += 30.0;
                }
                if p == HOUSE_WATER {
                    score -= 80.0;
                }
            }
        }

        for i in 0..PIECE_COUNT {
            let p = opp_pieces[i];
            if p < BEAR_OFF_POS {
                if board.is_piece_protected(opp_pieces, p) {
                    score -= 18.0;
                }
                if p == HOUSE_BEAUTY {
                    score -= 30.0;
                }
                if p == HOUSE_WATER {
                    score += 80.0;
                }
            }
        }

        // 4. 3-Piece Blockades
        for step in 1..28 {
            let my_count = my_pieces.iter().filter(|&&p| p >= step && p < step + 3).count();
            if my_count >= 3 {
                score += 45.0;
            }
            let opp_count = opp_pieces.iter().filter(|&&p| p >= step && p < step + 3).count();
            if opp_count >= 3 {
                score -= 45.0;
            }
        }

        score
    }

    pub fn find_best_move(&mut self, board: &SenetBoard, roll: u8) -> (usize, f32) {
        let mask = board.get_legal_moves_mask(roll);
        if mask == 0 {
            return (0, 0.5);
        }

        let max_depth = match self.difficulty_tier {
            0 => 1,
            1 => 3,
            _ => 5,
        };

        let mut best_move = 0;
        let mut best_val = -999999.0;
        let my_turn = board.current_turn;

        for i in 0..PIECE_COUNT {
            if (mask & (1 << i)) != 0 {
                let mut next_board = *board;
                next_board.play_move(i, roll);

                let val = if next_board.current_turn == my_turn {
                    // Bonus turn -> continue maximizing for current player
                    self.chance_node(&next_board, my_turn, max_depth - 1)
                } else {
                    self.chance_node(&next_board, my_turn, max_depth - 1)
                };

                if val > best_val {
                    best_val = val;
                    best_move = i;
                }
            }
        }

        let winrate = (1.0 / (1.0 + (-best_val / 120.0).exp())).clamp(0.02, 0.98);
        (best_move, winrate)
    }

    fn chance_node(&self, board: &SenetBoard, root_player: u8, depth: usize) -> f32 {
        if depth == 0 || board.is_game_over() {
            return self.evaluate_position(board, root_player);
        }

        let mut expected_val = 0.0;
        for &(roll, prob) in &STICK_PROBABILITIES {
            let val = self.max_node(board, root_player, roll, depth - 1);
            expected_val += prob * val;
        }

        expected_val
    }

    fn max_node(&self, board: &SenetBoard, root_player: u8, roll: u8, depth: usize) -> f32 {
        let mask = board.get_legal_moves_mask(roll);
        let is_my_turn = board.current_turn == root_player;

        if mask == 0 {
            // No moves available -> pass turn to other player
            let mut pass_board = *board;
            pass_board.current_turn = 1 - pass_board.current_turn;
            return self.chance_node(&pass_board, root_player, depth);
        }

        if is_my_turn {
            let mut max_val = -999999.0;
            for i in 0..PIECE_COUNT {
                if (mask & (1 << i)) != 0 {
                    let mut next_board = *board;
                    next_board.play_move(i, roll);
                    let val = self.chance_node(&next_board, root_player, depth);
                    if val > max_val {
                        max_val = val;
                    }
                }
            }
            max_val
        } else {
            let mut min_val = 999999.0;
            for i in 0..PIECE_COUNT {
                if (mask & (1 << i)) != 0 {
                    let mut next_board = *board;
                    next_board.play_move(i, roll);
                    let val = self.chance_node(&next_board, root_player, depth);
                    if val < min_val {
                        min_val = val;
                    }
                }
            }
            min_val
        }
    }
}
