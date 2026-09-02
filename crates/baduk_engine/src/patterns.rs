use crate::board::{GoBoard, opponent};

/// Evaluate 3x3 shape score for a prospective move at (x, y).
pub fn evaluate_shape_score(board: &GoBoard, x: usize, y: usize, color: u8) -> f32 {
    let mut score = 0.0;
    let size = board.size;
    let opp = opponent(color);

    // 1. Distance from edge line (3rd & 4th lines are optimal territory lines)
    let dist_x = x.min(size - 1 - x);
    let dist_y = y.min(size - 1 - y);

    if dist_x == 2 && dist_y == 2 {
        score += 65.0; // 3-3 Golden territory point / San-san
    } else if dist_x == 3 && dist_y == 3 {
        score += 70.0; // 4-4 Star point / Hoshi
    } else if (dist_x == 2 && dist_y == 3) || (dist_x == 3 && dist_y == 2) {
        score += 60.0; // 3-4 Point / Komoku
    } else if dist_x >= 2 && dist_y >= 2 {
        score += 45.0; // 3rd/4th line development
    } else if dist_x == 1 || dist_y == 1 {
        score += 15.0; // 2nd line endgame
    } else if dist_x == 0 || dist_y == 0 {
        score -= 40.0; // 1st line death penalty in early/midgame
    }

    // 2. Local 3x3 Neighborhood Scan
    let mut friendly_cardinal = 0;
    let mut enemy_cardinal = 0;
    let mut friendly_diagonal = 0;
    let mut enemy_diagonal = 0;

    let cardinal_dirs = [(0isize, 1isize), (0, -1), (1, 0), (-1, 0)];
    for &(dx, dy) in &cardinal_dirs {
        let nx = x as isize + dx;
        let ny = y as isize + dy;
        if nx >= 0 && nx < size as isize && ny >= 0 && ny < size as isize {
            let stone = board.get(nx as usize, ny as usize);
            if stone == color { friendly_cardinal += 1; }
            else if stone == opp { enemy_cardinal += 1; }
        }
    }

    let diag_dirs = [(1isize, 1isize), (1, -1), (-1, 1), (-1, -1)];
    for &(dx, dy) in &diag_dirs {
        let nx = x as isize + dx;
        let ny = y as isize + dy;
        if nx >= 0 && nx < size as isize && ny >= 0 && ny < size as isize {
            let stone = board.get(nx as usize, ny as usize);
            if stone == color { friendly_diagonal += 1; }
            else if stone == opp { enemy_diagonal += 1; }
        }
    }

    // A. Hane at the head of enemy stones (powerful pressure)
    if enemy_cardinal >= 1 && friendly_cardinal >= 1 {
        score += 55.0;
    }

    // B. Tiger's Mouth (eye-making shape)
    if friendly_cardinal >= 2 && friendly_diagonal >= 1 {
        score += 75.0;
    }

    // C. Kosumi (Diagonal connection with cut protection)
    if friendly_diagonal >= 1 && enemy_cardinal == 0 {
        score += 42.0;
    }

    // D. Bamboo Joint / Solid Connection
    if friendly_cardinal >= 2 {
        score += 35.0;
    }

    // E. Cut / Wedge between enemy stones
    if enemy_cardinal >= 2 {
        score += 48.0;
    }

    // F. Prevent enemy from forming an eye (Eye placement tesuji)
    if enemy_cardinal >= 2 && enemy_diagonal >= 1 {
        score += 60.0;
    }

    score
}
