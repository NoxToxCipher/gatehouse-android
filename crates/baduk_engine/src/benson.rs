//! Benson's Unconditional Life & Death Algorithm and Territory Estimator.

use crate::board::{GoBoard, EMPTY, BLACK, WHITE, MAX_POINTS};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct TerritoryResult {
    pub black_score: i32,
    pub white_score: i32,
    pub black_territory: usize,
    pub white_territory: usize,
    pub dame: usize,
}

/// Calculate territory using Chinese/Japanese area rules and flood fill.
pub fn evaluate_territory(board: &GoBoard, komi: f32) -> (TerritoryResult, [u8; MAX_POINTS]) {
    let size = board.size;
    let mut territory_map = [EMPTY; MAX_POINTS];
    let mut visited = [false; MAX_POINTS];

    let mut black_territory = 0;
    let mut white_territory = 0;
    let mut dame = 0;

    let mut neighbors_buf = [0usize; 4];

    for r in 0..size {
        for c in 0..size {
            let idx = board.idx(c, r);
            if board.grid[idx] == EMPTY && !visited[idx] {
                // Flood-fill empty enclosure
                let mut region = Vec::new();
                let mut has_black = false;
                let mut has_white = false;
                let mut queue = Vec::new();

                queue.push(idx);
                visited[idx] = true;

                while let Some(curr) = queue.pop() {
                    region.push(curr);
                    let n_count = board.neighbors(curr, &mut neighbors_buf);
                    for i in 0..n_count {
                        let n_idx = neighbors_buf[i];
                        if board.grid[n_idx] == EMPTY {
                            if !visited[n_idx] {
                                visited[n_idx] = true;
                                queue.push(n_idx);
                            }
                        } else if board.grid[n_idx] == BLACK {
                            has_black = true;
                        } else if board.grid[n_idx] == WHITE {
                            has_white = true;
                        }
                    }
                }

                if has_black && !has_white {
                    for &p in &region {
                        territory_map[p] = BLACK;
                    }
                    black_territory += region.len();
                } else if has_white && !has_black {
                    for &p in &region {
                        territory_map[p] = WHITE;
                    }
                    white_territory += region.len();
                } else {
                    dame += region.len();
                }
            }
        }
    }

    let b_score = (board.captures_black as usize + black_territory) as i32;
    let w_score = ((board.captures_white as usize + white_territory) as f32 + komi) as i32;

    (
        TerritoryResult {
            black_score: b_score,
            white_score: w_score,
            black_territory,
            white_territory,
            dame,
        },
        territory_map,
    )
}
