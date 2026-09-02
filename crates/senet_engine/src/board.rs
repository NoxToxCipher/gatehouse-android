//! Senet Board Representation and Rules Engine (Kendall & Bell Standard).
//! Zero-allocation 30-square track with protection blocks and sacred house mechanics.

pub const TRACK_SIZE: usize = 30;
pub const BEAR_OFF_POS: u8 = 30;
pub const PIECE_COUNT: usize = 5;

pub const HOUSE_REBIRTH: u8 = 14; // Square 15
pub const HOUSE_BEAUTY: u8 = 25;  // Square 26
pub const HOUSE_WATER: u8 = 26;   // Square 27
pub const HOUSE_TRUTHS: u8 = 27;  // Square 28
pub const HOUSE_RE_ATUM: u8 = 28; // Square 29
pub const HOUSE_HORUS: u8 = 29;   // Square 30

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct SenetBoard {
    pub white_pieces: [u8; PIECE_COUNT],
    pub black_pieces: [u8; PIECE_COUNT],
    pub white_borne_off: u8,
    pub black_borne_off: u8,
    pub current_turn: u8, // 0 = White (Pharaoh), 1 = Black (Anubis)
}

impl SenetBoard {
    pub fn new() -> Self {
        Self {
            white_pieces: [0, 2, 4, 6, 8],
            black_pieces: [1, 3, 5, 7, 9],
            white_borne_off: 0,
            black_borne_off: 0,
            current_turn: 0,
        }
    }

    pub fn get_my_pieces(&self) -> &[u8; PIECE_COUNT] {
        if self.current_turn == 0 {
            &self.white_pieces
        } else {
            &self.black_pieces
        }
    }

    pub fn get_opp_pieces(&self) -> &[u8; PIECE_COUNT] {
        if self.current_turn == 0 {
            &self.black_pieces
        } else {
            &self.white_pieces
        }
    }

    pub fn is_piece_protected(&self, opp_pieces: &[u8; PIECE_COUNT], pos: u8) -> bool {
        if pos >= BEAR_OFF_POS { return false; }
        // House of Beauty (25) is always protected
        if pos == HOUSE_BEAUTY { return true; }

        for &p in opp_pieces {
            if p < BEAR_OFF_POS && (p == pos.wrapping_sub(1) || p == pos + 1) {
                return true;
            }
        }
        false
    }

    pub fn is_blocked_by_wall(&self, opp_pieces: &[u8; PIECE_COUNT], src: u8, dst: u8) -> bool {
        // 3 consecutive opponent pieces form an impassable blockade
        for step in (src + 1)..dst {
            if step >= BEAR_OFF_POS { break; }
            let p0 = opp_pieces.iter().any(|&p| p == step);
            let p_prev = step > 0 && opp_pieces.iter().any(|&p| p == step - 1);
            let p_next = step + 1 < BEAR_OFF_POS as u8 && opp_pieces.iter().any(|&p| p == step + 1);

            if p0 && p_prev && p_next {
                return true;
            }
        }
        false
    }

    pub fn is_legal_move(&self, piece_idx: usize, roll: u8) -> bool {
        if piece_idx >= PIECE_COUNT || roll == 0 { return false; }
        let my = self.get_my_pieces();
        let opp = self.get_opp_pieces();

        let pos = my[piece_idx];
        if pos >= BEAR_OFF_POS { return false; }

        let dst = pos + roll;
        if dst > BEAR_OFF_POS { return false; }

        // Exact roll needed from last houses to bear off
        if pos == HOUSE_TRUTHS && roll != 3 && dst == BEAR_OFF_POS { return false; }
        if pos == HOUSE_RE_ATUM && roll != 2 && dst == BEAR_OFF_POS { return false; }
        if pos == HOUSE_HORUS && roll != 1 && dst == BEAR_OFF_POS { return false; }

        if dst == BEAR_OFF_POS { return true; }

        // Blocked by own piece
        if my.iter().any(|&p| p == dst) { return false; }

        // Blocked by opponent wall
        if self.is_blocked_by_wall(opp, pos, dst) { return false; }

        // Blocked by protected opponent piece
        if opp.iter().any(|&p| p == dst) {
            if self.is_piece_protected(opp, dst) {
                return false;
            }
        }

        true
    }

    pub fn get_legal_moves_mask(&self, roll: u8) -> u8 {
        let mut mask = 0u8;
        for i in 0..PIECE_COUNT {
            if self.is_legal_move(i, roll) {
                mask |= 1 << i;
            }
        }
        mask
    }

    pub fn play_move(&mut self, piece_idx: usize, roll: u8) -> bool {
        if !self.is_legal_move(piece_idx, roll) {
            return false;
        }

        let is_white = self.current_turn == 0;
        let (my_pieces, opp_pieces) = if is_white {
            (&mut self.white_pieces, &mut self.black_pieces)
        } else {
            (&mut self.black_pieces, &mut self.white_pieces)
        };

        let src = my_pieces[piece_idx];
        let mut dst = src + roll;

        // Check opponent swap
        if dst < BEAR_OFF_POS {
            for opp_p in opp_pieces.iter_mut() {
                if *opp_p == dst {
                    *opp_p = src;
                    break;
                }
            }
        }

        // House of Water (26 / Square 27) Drowning Trap -> Send back to House of Rebirth (14)
        if dst == HOUSE_WATER {
            dst = HOUSE_REBIRTH;
            while (my_pieces.iter().any(|&p| p == dst) || opp_pieces.iter().any(|&p| p == dst)) && dst > 0 {
                dst -= 1;
            }
        }

        my_pieces[piece_idx] = dst;

        if dst == BEAR_OFF_POS {
            if is_white {
                self.white_borne_off += 1;
            } else {
                self.black_borne_off += 1;
            }
        }

        // Check if bonus turn is awarded (rolls of 1, 4, or 5 award extra roll in Senet)
        let bonus = roll == 1 || roll == 4 || roll == 5;
        if !bonus {
            self.current_turn = 1 - self.current_turn;
        }

        true
    }

    pub fn is_game_over(&self) -> bool {
        self.white_borne_off == PIECE_COUNT as u8 || self.black_borne_off == PIECE_COUNT as u8
    }

    pub fn winner(&self) -> Option<u8> {
        if self.white_borne_off == PIECE_COUNT as u8 {
            Some(0)
        } else if self.black_borne_off == PIECE_COUNT as u8 {
            Some(1)
        } else {
            None
        }
    }
}
