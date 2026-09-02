//! Comprehensive Joseki & Opening Theory Book for 9x9, 13x13, and 19x19 Baduk.

use crate::board::{GoBoard, Point, EMPTY};

pub struct JosekiBook;

impl JosekiBook {
    /// Queries the Joseki opening book for an optimal move in early game.
    pub fn get_opening_move(board: &GoBoard, color: u8) -> Option<Point> {
        let size = board.size;
        let move_count = board.move_count;

        if size == 9 {
            return Self::get_9x9_opening(board, color, move_count);
        } else if size == 13 {
            return Self::get_13x13_opening(board, color, move_count);
        } else if size == 19 {
            return Self::get_19x19_opening(board, color, move_count);
        }

        None
    }

    fn get_9x9_opening(board: &GoBoard, _color: u8, move_count: u32) -> Option<Point> {
        if move_count == 0 {
            // Move 1: Tengen (Center 4, 4) or 3-3 Corner (2, 2)
            return Some(Point::new(4, 4));
        }

        if move_count == 1 {
            // Move 2: If opponent took Tengen (4,4), take 3-3 (2,2) or 3-4 (2,3)
            if board.get(4, 4) != EMPTY {
                return Some(Point::new(2, 2));
            } else {
                return Some(Point::new(4, 4));
            }
        }

        if move_count == 2 {
            // Move 3: Symmetrical corner claim or shoulder hit
            if board.get(6, 6) == EMPTY && board.get(2, 2) != EMPTY {
                return Some(Point::new(6, 6));
            }
            if board.get(2, 6) == EMPTY {
                return Some(Point::new(2, 6));
            }
        }

        if move_count <= 4 {
            let candidate_corners = [
                (2, 2), (6, 2), (2, 6), (6, 6),
                (3, 2), (2, 3), (5, 2), (6, 3),
            ];
            for &(x, y) in &candidate_corners {
                if board.get(x, y) == EMPTY {
                    return Some(Point::new(x, y));
                }
            }
        }

        None
    }

    fn get_13x13_opening(board: &GoBoard, _color: u8, move_count: u32) -> Option<Point> {
        let star = 3;
        let star_high = 9;
        let corners = [
            (star, star), (star_high, star), (star, star_high), (star_high, star_high),
            (6, 6), // Tengen
            (star, 4), (4, star), (star_high, 4), (4, star_high),
        ];

        if move_count <= 4 {
            for &(x, y) in &corners {
                if board.get(x, y) == EMPTY {
                    return Some(Point::new(x, y));
                }
            }
        }

        None
    }

    fn get_19x19_opening(board: &GoBoard, _color: u8, move_count: u32) -> Option<Point> {
        // Standard Star Points (4-4 / Hoshi) and 3-4 Points (Komoku)
        let star_points = [
            (3, 3), (15, 3), (3, 15), (15, 15), // 4 Corner Star Points
            (3, 2), (2, 3), (15, 2), (16, 3),   // 3-4 Komoku variations
            (3, 16), (2, 15), (15, 16), (16, 15),
            (9, 3), (3, 9), (15, 9), (9, 15),   // Side Star Points
            (9, 9),                             // Tengen
        ];

        if move_count < 4 {
            for &(x, y) in &star_points[0..4] {
                if board.get(x, y) == EMPTY {
                    return Some(Point::new(x, y));
                }
            }
        } else if move_count < 8 {
            // Check for Corner Enclosures (Keima Shimari) or 3-3 Invasions
            for &(x, y) in &star_points[4..12] {
                if board.get(x, y) == EMPTY {
                    return Some(Point::new(x, y));
                }
            }
        }

        None
    }
}
