//! Fast Baduk (Go) Board Engine with zero-allocation group tracking and Zobrist hashing.

pub const MAX_SIZE: usize = 19;
pub const MAX_POINTS: usize = MAX_SIZE * MAX_SIZE;

pub const EMPTY: u8 = 0;
pub const BLACK: u8 = 1;
pub const WHITE: u8 = 2;

#[inline(always)]
pub fn opponent(color: u8) -> u8 {
    3 - color
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Point {
    pub x: u8,
    pub y: u8,
}

impl Point {
    pub const PASS: Point = Point { x: 255, y: 255 };

    #[inline(always)]
    pub fn is_pass(&self) -> bool {
        self.x == 255 && self.y == 255
    }

    #[inline(always)]
    pub fn new(x: usize, y: usize) -> Self {
        Point { x: x as u8, y: y as u8 }
    }
}

#[derive(Clone, Debug)]
pub struct GoBoard {
    pub size: usize,
    pub grid: [u8; MAX_POINTS],
    pub parent: [u16; MAX_POINTS],
    pub next_stone: [u16; MAX_POINTS],
    pub stones_in_group: [u16; MAX_POINTS],
    pub liberties: [u16; MAX_POINTS],
    pub ko_point: Option<usize>,
    pub zobrist_hash: u64,
    pub captures_black: u32,
    pub captures_white: u32,
    pub move_count: u32,
}

impl GoBoard {
    pub fn new(size: usize) -> Self {
        let mut b = Self {
            size: size.clamp(5, 19),
            grid: [EMPTY; MAX_POINTS],
            parent: [0; MAX_POINTS],
            next_stone: [0; MAX_POINTS],
            stones_in_group: [0; MAX_POINTS],
            liberties: [0; MAX_POINTS],
            ko_point: None,
            zobrist_hash: 0,
            captures_black: 0,
            captures_white: 0,
            move_count: 0,
        };
        b.reset();
        b
    }

    pub fn reset(&mut self) {
        self.grid = [EMPTY; MAX_POINTS];
        for i in 0..MAX_POINTS {
            self.parent[i] = i as u16;
            self.next_stone[i] = i as u16;
            self.stones_in_group[i] = 0;
            self.liberties[i] = 0;
        }
        self.ko_point = None;
        self.zobrist_hash = 0;
        self.captures_black = 0;
        self.captures_white = 0;
        self.move_count = 0;
    }

    #[inline(always)]
    pub fn idx(&self, x: usize, y: usize) -> usize {
        y * self.size + x
    }

    #[inline(always)]
    pub fn coords(&self, idx: usize) -> (usize, usize) {
        (idx % self.size, idx / self.size)
    }

    #[inline(always)]
    pub fn is_valid_coord(&self, x: usize, y: usize) -> bool {
        x < self.size && y < self.size
    }

    #[inline(always)]
    pub fn get(&self, x: usize, y: usize) -> u8 {
        if self.is_valid_coord(x, y) {
            self.grid[self.idx(x, y)]
        } else {
            3 // Border
        }
    }

    pub fn neighbors(&self, idx: usize, out: &mut [usize; 4]) -> usize {
        let (x, y) = self.coords(idx);
        let mut count = 0;
        if x > 0 { out[count] = self.idx(x - 1, y); count += 1; }
        if x + 1 < self.size { out[count] = self.idx(x + 1, y); count += 1; }
        if y > 0 { out[count] = self.idx(x, y - 1); count += 1; }
        if y + 1 < self.size { out[count] = self.idx(x, y + 1); count += 1; }
        count
    }

    pub fn find_group_root(&self, idx: usize) -> usize {
        let mut curr = idx;
        while self.parent[curr] as usize != curr {
            curr = self.parent[curr] as usize;
        }
        curr
    }

    pub fn count_liberties(&self, root: usize) -> u16 {
        let mut counted = [false; MAX_POINTS];
        let mut libs = 0;
        let mut curr = root;
        let mut neighbors_buf = [0usize; 4];

        loop {
            let n_count = self.neighbors(curr, &mut neighbors_buf);
            for i in 0..n_count {
                let n_idx = neighbors_buf[i];
                if self.grid[n_idx] == EMPTY && !counted[n_idx] {
                    counted[n_idx] = true;
                    libs += 1;
                }
            }
            curr = self.next_stone[curr] as usize;
            if curr == root {
                break;
            }
        }
        libs
    }

    pub fn is_legal(&self, x: usize, y: usize, color: u8) -> bool {
        if !self.is_valid_coord(x, y) { return false; }
        let idx = self.idx(x, y);
        if self.grid[idx] != EMPTY { return false; }
        if Some(idx) == self.ko_point { return false; }

        let opp = opponent(color);
        let mut neighbors_buf = [0usize; 4];
        let n_count = self.neighbors(idx, &mut neighbors_buf);

        // 1. Direct empty liberty available
        for i in 0..n_count {
            if self.grid[neighbors_buf[i]] == EMPTY {
                return true;
            }
        }

        // 2. Can capture adjacent opponent group with 1 liberty
        for i in 0..n_count {
            let n_idx = neighbors_buf[i];
            if self.grid[n_idx] == opp {
                let opp_root = self.find_group_root(n_idx);
                if self.liberties[opp_root] <= 1 {
                    return true;
                }
            }
        }

        // 3. Connect to friendly group with > 1 liberty
        for i in 0..n_count {
            let n_idx = neighbors_buf[i];
            if self.grid[n_idx] == color {
                let f_root = self.find_group_root(n_idx);
                if self.liberties[f_root] > 1 {
                    return true;
                }
            }
        }

        // 4. Test suicide rule via clone simulation
        let mut sim = self.clone();
        sim.play_move(Point::new(x, y), color)
    }

    pub fn play_move(&mut self, p: Point, color: u8) -> bool {
        if p.is_pass() {
            self.ko_point = None;
            self.move_count += 1;
            return true;
        }

        let (x, y) = (p.x as usize, p.y as usize);
        if !self.is_valid_coord(x, y) { return false; }
        let idx = self.idx(x, y);
        if self.grid[idx] != EMPTY { return false; }
        if Some(idx) == self.ko_point { return false; }

        let opp = opponent(color);
        self.grid[idx] = color;
        self.parent[idx] = idx as u16;
        self.next_stone[idx] = idx as u16;
        self.stones_in_group[idx] = 1;

        let mut neighbors_buf = [0usize; 4];
        let n_count = self.neighbors(idx, &mut neighbors_buf);

        // 1. Check & capture adjacent opponent groups with 0 liberties
        let mut captured_stones = 0;
        let mut single_capture_pos = None;

        for i in 0..n_count {
            let n_idx = neighbors_buf[i];
            if self.grid[n_idx] == opp {
                let opp_root = self.find_group_root(n_idx);
                let opp_libs = self.count_liberties(opp_root);
                if opp_libs == 0 {
                    let count = self.remove_group(opp_root);
                    captured_stones += count;
                    if count == 1 {
                        single_capture_pos = Some(n_idx);
                    }
                } else {
                    self.liberties[opp_root] = opp_libs;
                }
            }
        }

        if color == BLACK {
            self.captures_black += captured_stones as u32;
        } else {
            self.captures_white += captured_stones as u32;
        }

        // 2. Merge with friendly adjacent groups
        let mut my_root = idx;
        for i in 0..n_count {
            let n_idx = neighbors_buf[i];
            if self.grid[n_idx] == color {
                let f_root = self.find_group_root(n_idx);
                if f_root != my_root {
                    my_root = self.merge_groups(my_root, f_root);
                }
            }
        }

        // 3. Recalculate liberties of own group
        let my_libs = self.count_liberties(my_root);
        self.liberties[my_root] = my_libs;

        // 4. Suicide check: illegal if 0 liberties and captured nothing
        if my_libs == 0 && captured_stones == 0 {
            // Undo
            self.grid[idx] = EMPTY;
            return false;
        }

        // 5. Update Ko rule
        if captured_stones == 1 && my_libs == 1 && self.stones_in_group[my_root] == 1 {
            self.ko_point = single_capture_pos;
        } else {
            self.ko_point = None;
        }

        self.move_count += 1;
        true
    }

    fn remove_group(&mut self, root: usize) -> usize {
        let mut count = 0;
        let mut curr = root;
        let mut stones_to_remove = [0usize; MAX_POINTS];

        loop {
            stones_to_remove[count] = curr;
            count += 1;
            curr = self.next_stone[curr] as usize;
            if curr == root {
                break;
            }
        }

        for i in 0..count {
            let s_idx = stones_to_remove[i];
            self.grid[s_idx] = EMPTY;
            self.parent[s_idx] = s_idx as u16;
            self.next_stone[s_idx] = s_idx as u16;
            self.stones_in_group[s_idx] = 0;
            self.liberties[s_idx] = 0;
        }

        // Update liberties of surrounding groups
        let mut neighbors_buf = [0usize; 4];
        for i in 0..count {
            let s_idx = stones_to_remove[i];
            let n_count = self.neighbors(s_idx, &mut neighbors_buf);
            for j in 0..n_count {
                let n_idx = neighbors_buf[j];
                if self.grid[n_idx] != EMPTY {
                    let r = self.find_group_root(n_idx);
                    self.liberties[r] = self.count_liberties(r);
                }
            }
        }

        count
    }

    fn merge_groups(&mut self, r1: usize, r2: usize) -> usize {
        if r1 == r2 { return r1; }

        self.parent[r2] = r1 as u16;
        self.stones_in_group[r1] += self.stones_in_group[r2];

        // Swap next_stone pointers to splice cyclical linked lists
        let temp = self.next_stone[r1];
        self.next_stone[r1] = self.next_stone[r2];
        self.next_stone[r2] = temp;

        r1
    }
}
