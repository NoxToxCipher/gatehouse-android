//! Deterministic Ladder (Shicho) Reader.

use crate::board::{GoBoard, Point, opponent, EMPTY};

pub const MAX_LADDER_DEPTH: usize = 60;

/// Returns true if the group at (target_x, target_y) will be captured in a ladder.
pub fn is_ladder_capture(board: &GoBoard, target_x: usize, target_y: usize) -> bool {
    let prey_color = board.get(target_x, target_y);
    if prey_color == EMPTY || prey_color > 2 {
        return false;
    }

    let hunter_color = opponent(prey_color);
    let mut sim = board.clone();

    read_ladder_recursive(&mut sim, target_x, target_y, prey_color, hunter_color, 0)
}

fn read_ladder_recursive(
    board: &mut GoBoard,
    target_x: usize,
    target_y: usize,
    prey: u8,
    hunter: u8,
    depth: usize,
) -> bool {
    if depth > MAX_LADDER_DEPTH {
        return false; // Ladder escaped
    }

    let prey_root = board.find_group_root(board.idx(target_x, target_y));
    let prey_libs = board.count_liberties(prey_root);

    if prey_libs == 0 {
        return true; // Captured!
    }
    if prey_libs >= 3 {
        return false; // Escaped with 3+ liberties
    }

    if depth % 2 == 1 {
        // Hunter's turn: try to keep prey in atari (1 liberty)
        let mut neighbors_buf = [0usize; 4];
        let mut best_captured = false;

        // Find liberties of prey group
        let mut curr = prey_root;
        loop {
            let n_count = board.neighbors(curr, &mut neighbors_buf);
            for i in 0..n_count {
                let n_idx = neighbors_buf[i];
                if board.grid[n_idx] == EMPTY {
                    let (nx, ny) = board.coords(n_idx);
                    let mut clone = board.clone();
                    if clone.play_move(Point::new(nx, ny), hunter) {
                        if read_ladder_recursive(&mut clone, target_x, target_y, prey, hunter, depth + 1) {
                            best_captured = true;
                            break;
                        }
                    }
                }
            }
            if best_captured { break; }
            curr = board.next_stone[curr] as usize;
            if curr == prey_root { break; }
        }

        best_captured
    } else {
        // Prey's turn: try to run to liberty
        let mut neighbors_buf = [0usize; 4];
        let mut can_escape = false;

        let mut curr = prey_root;
        loop {
            let n_count = board.neighbors(curr, &mut neighbors_buf);
            for i in 0..n_count {
                let n_idx = neighbors_buf[i];
                if board.grid[n_idx] == EMPTY {
                    let (nx, ny) = board.coords(n_idx);
                    let mut clone = board.clone();
                    if clone.play_move(Point::new(nx, ny), prey) {
                        let new_root = clone.find_group_root(clone.idx(nx, ny));
                        if clone.count_liberties(new_root) >= 3 {
                            can_escape = true;
                            break;
                        }
                        if !read_ladder_recursive(&mut clone, nx, ny, prey, hunter, depth + 1) {
                            can_escape = true;
                            break;
                        }
                    }
                }
            }
            if can_escape { break; }
            curr = board.next_stone[curr] as usize;
            if curr == prey_root { break; }
        }

        !can_escape
    }
}
