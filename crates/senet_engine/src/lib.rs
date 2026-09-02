//! Ancient Egyptian Senet Native Engine.
//! Kendall & Bell Rules, Stochastic Expectimax AI, and Procedural Shaders.

pub mod board;
pub mod expectimax;
pub mod shader;
pub mod jni;

pub use board::{SenetBoard, BEAR_OFF_POS, HOUSE_BEAUTY, HOUSE_WATER, HOUSE_REBIRTH};
pub use expectimax::ExpectimaxAI;
pub use shader::{render_sandstone_tile, render_piece_texture};

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_board_initial_setup() {
        let board = SenetBoard::new();
        assert_eq!(board.white_pieces, [0, 2, 4, 6, 8]);
        assert_eq!(board.black_pieces, [1, 3, 5, 7, 9]);
        assert_eq!(board.white_borne_off, 0);
        assert_eq!(board.black_borne_off, 0);
        assert_eq!(board.current_turn, 0);
    }

    #[test]
    fn test_protected_block() {
        let mut board = SenetBoard::new();
        // Place Black pieces adjacent at 3 and 4 (forming a protected block)
        board.black_pieces[1] = 3;
        board.black_pieces[2] = 4;

        // White tries to move piece 0 from 0 with roll 3 -> lands on 3
        // Since Black pieces at 3 and 4 are directly adjacent, 3 is protected!
        assert!(!board.is_legal_move(0, 3), "Protected piece at 3 should block swap");
    }

    #[test]
    fn test_water_hazard_drowning() {
        let mut board = SenetBoard::new();
        board.white_pieces[0] = 24; // 1 square before House of Beauty
        // Roll 2 -> lands on 26 (House of Water / Square 27)
        assert!(board.is_legal_move(0, 2));
        board.play_move(0, 2);
        // Should drown and respawn at House of Rebirth (14 / Square 15)
        assert_eq!(board.white_pieces[0], HOUSE_REBIRTH, "Drowned piece must respawn at House of Rebirth (14)");
    }

    #[test]
    fn test_expectimax_ai_move() {
        let board = SenetBoard::new();
        let mut ai = ExpectimaxAI::new(2); // Anubis Grandmaster
        let (best_move, winrate) = ai.find_best_move(&board, 2);
        assert!(best_move < 5);
        assert!(winrate >= 0.0 && winrate <= 1.0);
    }

    #[test]
    fn test_shader_synthesis() {
        let mut tile_buf = vec![0i32; 32 * 32];
        render_sandstone_tile(&mut tile_buf, 32, 32, 2);
        assert!(tile_buf[16 * 32 + 16] != 0);

        let mut piece_buf = vec![0i32; 32 * 32];
        render_piece_texture(&mut piece_buf, 32, 32, true);
        assert!(piece_buf[16 * 32 + 16] != 0);
    }
}
