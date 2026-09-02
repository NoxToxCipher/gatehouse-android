//! RFC 8439 ChaCha20-Poly1305 Authenticated Encryption in pure safe Rust.

pub struct ChaCha20 {
    state: [u32; 16],
}

impl ChaCha20 {
    pub fn new(key: &[u8; 32], nonce: &[u8; 12], counter: u32) -> Self {
        let mut state = [0u32; 16];
        state[0] = 0x61707865; // "expa"
        state[1] = 0x3320646e; // "nd 3"
        state[2] = 0x79622d32; // "2-by"
        state[3] = 0x6b206574; // "te k"

        for i in 0..8 {
            state[4 + i] = u32::from_le_bytes([
                key[i * 4], key[i * 4 + 1], key[i * 4 + 2], key[i * 4 + 3],
            ]);
        }
        state[12] = counter;
        for i in 0..3 {
            state[13 + i] = u32::from_le_bytes([
                nonce[i * 4], nonce[i * 4 + 1], nonce[i * 4 + 2], nonce[i * 4 + 3],
            ]);
        }

        ChaCha20 { state }
    }

    pub fn apply_keystream(&mut self, data: &mut [u8]) {
        let mut offset = 0;
        while offset < data.len() {
            let block = self.generate_block();
            let chunk = (data.len() - offset).min(64);
            for i in 0..chunk {
                data[offset + i] ^= block[i];
            }
            offset += chunk;
            self.state[12] = self.state[12].wrapping_add(1);
        }
    }

    pub fn generate_block(&self) -> [u8; 64] {
        let mut x = self.state;
        for _ in 0..10 {
            // Column round
            quarter_round(&mut x, 0, 4, 8, 12);
            quarter_round(&mut x, 1, 5, 9, 13);
            quarter_round(&mut x, 2, 6, 10, 14);
            quarter_round(&mut x, 3, 7, 11, 15);
            // Diagonal round
            quarter_round(&mut x, 0, 5, 10, 15);
            quarter_round(&mut x, 1, 6, 11, 12);
            quarter_round(&mut x, 2, 7, 8, 13);
            quarter_round(&mut x, 3, 4, 9, 14);
        }

        let mut out = [0u8; 64];
        for i in 0..16 {
            let sum = x[i].wrapping_add(self.state[i]);
            out[i * 4..(i + 1) * 4].copy_from_slice(&sum.to_le_bytes());
        }
        out
    }
}

fn quarter_round(x: &mut [u32; 16], a: usize, b: usize, c: usize, d: usize) {
    x[a] = x[a].wrapping_add(x[b]); x[d] = (x[d] ^ x[a]).rotate_left(16);
    x[c] = x[c].wrapping_add(x[d]); x[b] = (x[b] ^ x[c]).rotate_left(12);
    x[a] = x[a].wrapping_add(x[b]); x[d] = (x[d] ^ x[a]).rotate_left(8);
    x[c] = x[c].wrapping_add(x[d]); x[b] = (x[b] ^ x[c]).rotate_left(7);
}

// ---- Poly1305 One-Time Authenticator (RFC 8439) ----

pub struct Poly1305 {
    r: [u32; 5],
    h: [u32; 5],
    pad: [u32; 4],
    buffer: [u8; 16],
    buf_len: usize,
}

impl Poly1305 {
    pub fn new(key: &[u8; 32]) -> Self {
        let mut r = [0u32; 5];
        let t0 = u32::from_le_bytes([key[0], key[1], key[2], key[3]]) & 0x0fffffff;
        let t1 = u32::from_le_bytes([key[4], key[5], key[6], key[7]]) & 0x0ffffffc;
        let t2 = u32::from_le_bytes([key[8], key[9], key[10], key[11]]) & 0x0ffffffc;
        let t3 = u32::from_le_bytes([key[12], key[13], key[14], key[15]]) & 0x0ffffffc;

        r[0] = t0 & 0x3ffffff;
        r[1] = ((t0 >> 26) | (t1 << 6)) & 0x3ffffff;
        r[2] = ((t1 >> 20) | (t2 << 12)) & 0x3ffffff;
        r[3] = ((t2 >> 14) | (t3 << 18)) & 0x3ffffff;
        r[4] = t3 >> 8;

        let pad = [
            u32::from_le_bytes([key[16], key[17], key[18], key[19]]),
            u32::from_le_bytes([key[20], key[21], key[22], key[23]]),
            u32::from_le_bytes([key[24], key[25], key[26], key[27]]),
            u32::from_le_bytes([key[28], key[29], key[30], key[31]]),
        ];

        Poly1305 {
            r,
            h: [0u32; 5],
            pad,
            buffer: [0u8; 16],
            buf_len: 0,
        }
    }

    pub fn update(&mut self, data: &[u8]) {
        let mut offset = 0;
        if self.buf_len > 0 {
            let space = 16 - self.buf_len;
            if data.len() >= space {
                self.buffer[self.buf_len..16].copy_from_slice(&data[..space]);
                self.process_block(16, false);
                offset = space;
                self.buf_len = 0;
            } else {
                self.buffer[self.buf_len..self.buf_len + data.len()].copy_from_slice(data);
                self.buf_len += data.len();
                return;
            }
        }

        while offset + 16 <= data.len() {
            self.buffer.copy_from_slice(&data[offset..offset + 16]);
            self.process_block(16, false);
            offset += 16;
        }

        if offset < data.len() {
            let rem = data.len() - offset;
            self.buffer[..rem].copy_from_slice(&data[offset..]);
            self.buf_len = rem;
        }
    }

    pub fn finalize(mut self) -> [u8; 16] {
        if self.buf_len > 0 {
            let len = self.buf_len;
            self.process_block(len, true);
        }

        let mut h0 = self.h[0] | (self.h[1] << 26);
        let mut h1 = (self.h[1] >> 6) | (self.h[2] << 20);
        let mut h2 = (self.h[2] >> 12) | (self.h[3] << 14);
        let mut h3 = (self.h[3] >> 18) | (self.h[4] << 8);

        let mut c = (h0 as u64) + (self.pad[0] as u64);
        h0 = c as u32;
        c = (h1 as u64) + (self.pad[1] as u64) + (c >> 32);
        h1 = c as u32;
        c = (h2 as u64) + (self.pad[2] as u64) + (c >> 32);
        h2 = c as u32;
        c = (h3 as u64) + (self.pad[3] as u64) + (c >> 32);
        h3 = c as u32;

        let mut tag = [0u8; 16];
        tag[0..4].copy_from_slice(&h0.to_le_bytes());
        tag[4..8].copy_from_slice(&h1.to_le_bytes());
        tag[8..12].copy_from_slice(&h2.to_le_bytes());
        tag[12..16].copy_from_slice(&h3.to_le_bytes());
        tag
    }

    fn process_block(&mut self, len: usize, _is_final_partial: bool) {
        let mut w = [0u8; 17];
        w[..len].copy_from_slice(&self.buffer[..len]);
        w[len] = 1; // Pad bit

        let t0 = u32::from_le_bytes([w[0], w[1], w[2], w[3]]);
        let t1 = u32::from_le_bytes([w[4], w[5], w[6], w[7]]);
        let t2 = u32::from_le_bytes([w[8], w[9], w[10], w[11]]);
        let t3 = u32::from_le_bytes([w[12], w[13], w[14], w[15]]);
        let t4 = w[16] as u32;

        let m0 = t0 & 0x3ffffff;
        let m1 = ((t0 >> 26) | (t1 << 6)) & 0x3ffffff;
        let m2 = ((t1 >> 20) | (t2 << 12)) & 0x3ffffff;
        let m3 = ((t2 >> 14) | (t3 << 18)) & 0x3ffffff;
        let m4 = (t3 >> 8) | (t4 << 24);

        self.h[0] = self.h[0].wrapping_add(m0);
        self.h[1] = self.h[1].wrapping_add(m1);
        self.h[2] = self.h[2].wrapping_add(m2);
        self.h[3] = self.h[3].wrapping_add(m3);
        self.h[4] = self.h[4].wrapping_add(m4);

        // Multiply h * r mod 2^130 - 5
        let r0 = self.r[0] as u64;
        let r1 = self.r[1] as u64;
        let r2 = self.r[2] as u64;
        let r3 = self.r[3] as u64;
        let r4 = self.r[4] as u64;

        let s1 = r1 * 5;
        let s2 = r2 * 5;
        let s3 = r3 * 5;
        let s4 = r4 * 5;

        let h0 = self.h[0] as u64;
        let h1 = self.h[1] as u64;
        let h2 = self.h[2] as u64;
        let h3 = self.h[3] as u64;
        let h4 = self.h[4] as u64;

        let d0 = h0*r0 + h1*s4 + h2*s3 + h3*s2 + h4*s1;
        let d1 = h0*r1 + h1*r0 + h2*s4 + h3*s3 + h4*s2;
        let d2 = h0*r2 + h1*r1 + h2*r0 + h3*s4 + h4*s3;
        let d3 = h0*r3 + h1*r2 + h2*r1 + h3*r0 + h4*s4;
        let d4 = h0*r4 + h1*r3 + h2*r2 + h3*r1 + h4*r0;

        let mut c = d0 >> 26; self.h[0] = (d0 & 0x3ffffff) as u32;
        let mut d = d1 + c; c = d >> 26; self.h[1] = (d & 0x3ffffff) as u32;
        d = d2 + c; c = d >> 26; self.h[2] = (d & 0x3ffffff) as u32;
        d = d3 + c; c = d >> 26; self.h[3] = (d & 0x3ffffff) as u32;
        d = d4 + c; c = d >> 26; self.h[4] = (d & 0x3ffffff) as u32;
        self.h[0] = self.h[0].wrapping_add(((c * 5) & 0x3ffffff) as u32);
    }
}
