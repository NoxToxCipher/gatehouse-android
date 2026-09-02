//! High-Performance Native Baduk (Go) Engine.

pub mod board;
pub mod patterns;
pub mod ladder;
pub mod benson;
pub mod joseki;
pub mod mcts;
pub mod jni;

pub use board::{GoBoard, Point, BLACK, WHITE, EMPTY};
pub use mcts::MctsBot;
pub use joseki::JosekiBook;
pub use benson::evaluate_territory;
pub use ladder::is_ladder_capture;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_board_capture_single_stone() {
        let mut board = GoBoard::new(9);
        // Place White stone at (1, 1)
        assert!(board.play_move(Point::new(1, 1), WHITE));

        // Surround with Black stones at (1, 0), (0, 1), (2, 1), (1, 2)
        assert!(board.play_move(Point::new(1, 0), BLACK));
        assert!(board.play_move(Point::new(0, 1), BLACK));
        assert!(board.play_move(Point::new(2, 1), BLACK));
        assert_eq!(board.get(1, 1), WHITE);

        // Final capturing move
        assert!(board.play_move(Point::new(1, 2), BLACK));

        // White stone should be captured and removed
        assert_eq!(board.get(1, 1), EMPTY);
        assert_eq!(board.captures_black, 1);
    }

    #[test]
    fn test_board_ko_rule() {
        let mut board = GoBoard::new(9);
        // Setup classic Ko shape:
        // Black at (1,0), (0,1), (1,2)
        // White at (2,0), (3,1), (2,2)
        assert!(board.play_move(Point::new(1, 0), BLACK));
        assert!(board.play_move(Point::new(2, 0), WHITE));
        assert!(board.play_move(Point::new(0, 1), BLACK));
        assert!(board.play_move(Point::new(3, 1), WHITE));
        assert!(board.play_move(Point::new(1, 2), BLACK));
        assert!(board.play_move(Point::new(2, 2), WHITE));

        // Black plays at (2, 1) to create shape
        assert!(board.play_move(Point::new(2, 1), BLACK));

        // White plays at (1, 1) and captures (2, 1)
        assert!(board.play_move(Point::new(1, 1), WHITE));
        assert_eq!(board.get(2, 1), EMPTY);
        assert_eq!(board.captures_white, 1);

        // Black cannot immediately recapture at (2, 1) due to Ko rule
        assert!(!board.is_legal(2, 1, BLACK));
        assert!(!board.play_move(Point::new(2, 1), BLACK));

        // Black plays a Ko threat elsewhere (e.g. at 5, 5)
        assert!(board.play_move(Point::new(5, 5), BLACK));
        // White responds elsewhere (e.g. at 6, 6)
        assert!(board.play_move(Point::new(6, 6), WHITE));

        // Now Black can legally recapture at (2, 1)
        assert!(board.is_legal(2, 1, BLACK));
        assert!(board.play_move(Point::new(2, 1), BLACK));
        assert_eq!(board.get(1, 1), EMPTY);
        assert_eq!(board.captures_black, 1);
    }

    #[test]
    fn test_suicide_prevention() {
        let mut board = GoBoard::new(9);
        // Surround corner (0, 0) with Black at (1, 0) and (0, 1)
        assert!(board.play_move(Point::new(1, 0), BLACK));
        assert!(board.play_move(Point::new(0, 1), BLACK));

        // White playing at (0, 0) is suicide (0 liberties and 0 captures)
        assert!(!board.is_legal(0, 0, WHITE));
        assert!(!board.play_move(Point::new(0, 0), WHITE));
        assert_eq!(board.get(0, 0), EMPTY);
    }

    #[test]
    fn test_ladder_reading() {
        let mut board = GoBoard::new(9);
        // Set up a standard 1-stone ladder scenario with diagonal shoulder stone
        board.play_move(Point::new(2, 2), WHITE);
        board.play_move(Point::new(3, 2), BLACK);
        board.play_move(Point::new(2, 3), BLACK);
        board.play_move(Point::new(3, 1), BLACK); // Diagonal blocker

        // White has 2 liberties at (1, 2) and (2, 1)
        // If Black plays at (1, 2), White is put into atari
        board.play_move(Point::new(1, 2), BLACK);

        let is_caught = is_ladder_capture(&board, 2, 2);
        assert!(is_caught, "White stone at (2, 2) should be caught in ladder");
    }

    #[test]
    fn test_benson_territory_evaluation() {
        let mut board = GoBoard::new(9);
        // Black encloses top-left corner
        board.play_move(Point::new(0, 2), BLACK);
        board.play_move(Point::new(1, 2), BLACK);
        board.play_move(Point::new(2, 2), BLACK);
        board.play_move(Point::new(2, 1), BLACK);
        board.play_move(Point::new(2, 0), BLACK);

        let (result, map) = evaluate_territory(&board, 6.5);
        assert!(result.black_territory > 0, "Black should own the enclosed corner");
        assert_eq!(map[board.idx(0, 0)], BLACK);
        assert_eq!(map[board.idx(1, 1)], BLACK);
    }

    #[test]
    fn test_mcts_bot_search() {
        let mut board = GoBoard::new(9);
        // Black plays Tengen
        board.play_move(Point::new(4, 4), BLACK);

        // Bot plays as White
        let mut bot = MctsBot::new(1); // Master tier
        let (best_move, winrate, heatmap) = bot.search(&board, WHITE);

        assert!(!best_move.is_pass(), "Bot must find a valid move");
        assert!(best_move.x < 9 && best_move.y < 9, "Coordinates must be in bounds");
        assert!(winrate >= 0.0 && winrate <= 1.0, "Winrate must be probabilistic");
        assert!(heatmap[board.idx(best_move.x as usize, best_move.y as usize)] >= 0.0);
    }

    #[test]
    fn test_urgent_atari_defense() {
        let mut board = GoBoard::new(9);
        // White stone at (0, 0) with Black at (1, 0). (0, 0) has 1 liberty at (0, 1)
        board.play_move(Point::new(0, 0), WHITE);
        board.play_move(Point::new(1, 0), BLACK);

        let mut bot = MctsBot::new(1);
        let (best_move, _, _) = bot.search(&board, WHITE);

        // Bot must defend by playing at (0, 1)
        assert_eq!(best_move, Point::new(0, 1));
    }

    #[test]
    fn test_tournament_puct_bot_dominance() {
        let mut board = GoBoard::new(9);
        let mut smart_bot = MctsBot::new(1); // 3-Dan PUCT MctsBot

        // Run a 30-move mini tournament where Black is the smart PUCT bot and White plays pseudo-random legal moves
        let mut rng = 0x12345678u64;
        let mut passes = 0;

        for _ in 0..30 {
            if passes >= 2 { break; }

            // Black's turn: Smart PUCT Bot
            let (b_move, _, _) = smart_bot.search(&board, BLACK);
            if b_move.is_pass() {
                passes += 1;
            } else {
                passes = 0;
                board.play_move(b_move, BLACK);
            }

            // White's turn: Random legal move
            let mut w_moves = Vec::new();
            for y in 0..9 {
                for x in 0..9 {
                    if board.is_legal(x, y, WHITE) {
                        w_moves.push(Point::new(x, y));
                    }
                }
            }

            if w_moves.is_empty() {
                passes += 1;
            } else {
                rng = rng.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
                let pick = (rng >> 32) as usize % w_moves.len();
                board.play_move(w_moves[pick], WHITE);
                passes = 0;
            }
        }

        // Evaluate territory — Smart bot must hold commanding lead
        let (score, _) = evaluate_territory(&board, 6.5);
        assert!(score.black_score > score.white_score, "Smart PUCT Bot must defeat random playout bot (Score: B {} vs W {})", score.black_score, score.white_score);
    }

    #[test]
    fn test_nakade_vital_point_killing() {
        let mut board = GoBoard::new(9);
        // Set up Black enclosing the top-left area
        board.play_move(Point::new(0, 1), BLACK);
        board.play_move(Point::new(0, 2), BLACK);
        board.play_move(Point::new(0, 3), BLACK);
        board.play_move(Point::new(2, 1), BLACK);
        board.play_move(Point::new(2, 2), BLACK);
        board.play_move(Point::new(2, 3), BLACK);
        board.play_move(Point::new(1, 0), BLACK);
        board.play_move(Point::new(1, 4), BLACK);

        // White tries to make two eyes by playing at (1, 1) and (1, 3)
        board.play_move(Point::new(1, 1), WHITE);
        board.play_move(Point::new(1, 3), WHITE);

        // Advance move count
        board.move_count = 12;

        let mut bot = MctsBot::new(2); // 9-Dan Grandmaster
        let (best_move, _, _) = bot.search(&board, BLACK);

        // Black must strike the vital splitting/Nakade point at (1, 2)
        assert_eq!(best_move, Point::new(1, 2), "Bot should strike the vital Nakade point at (1, 2)");
    }
}
