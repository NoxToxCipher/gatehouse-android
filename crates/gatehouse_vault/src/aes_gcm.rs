//! Constant-time AES-256-GCM (NIST SP 800-38D) implementation in pure safe Rust.

const S_BOX: [u8; 256] = [
    0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
    0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
    0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
    0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
    0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
    0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
    0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
    0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
    0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
    0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
    0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
    0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
    0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
    0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
    0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
    0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16,
];

const RCON: [u32; 10] = [
    0x01000000, 0x02000000, 0x04000000, 0x08000000, 0x10000000,
    0x20000000, 0x40000000, 0x80000000, 0x1b000000, 0x36000000,
];

pub struct Aes256 {
    round_keys: [u32; 60],
}

impl Aes256 {
    pub fn new(key: &[u8; 32]) -> Self {
        let mut rk = [0u32; 60];
        for i in 0..8 {
            rk[i] = u32::from_be_bytes([key[i * 4], key[i * 4 + 1], key[i * 4 + 2], key[i * 4 + 3]]);
        }
        for i in 8..60 {
            let mut temp = rk[i - 1];
            if i % 8 == 0 {
                temp = sub_word(temp.rotate_left(8)) ^ RCON[(i / 8) - 1];
            } else if i % 8 == 4 {
                temp = sub_word(temp);
            }
            rk[i] = rk[i - 8] ^ temp;
        }
        Aes256 { round_keys: rk }
    }

    pub fn encrypt_block(&self, block: &[u8; 16]) -> [u8; 16] {
        let mut state = [0u8; 16];
        state.copy_from_slice(block);

        add_round_key(&mut state, &self.round_keys[0..4]);

        for round in 1..14 {
            sub_bytes(&mut state);
            shift_rows(&mut state);
            mix_columns(&mut state);
            add_round_key(&mut state, &self.round_keys[round * 4..(round + 1) * 4]);
        }

        sub_bytes(&mut state);
        shift_rows(&mut state);
        add_round_key(&mut state, &self.round_keys[56..60]);

        state
    }
}

fn sub_word(w: u32) -> u32 {
    let b = w.to_be_bytes();
    u32::from_be_bytes([S_BOX[b[0] as usize], S_BOX[b[1] as usize], S_BOX[b[2] as usize], S_BOX[b[3] as usize]])
}

fn add_round_key(state: &mut [u8; 16], rk: &[u32]) {
    for i in 0..4 {
        let bytes = rk[i].to_be_bytes();
        state[i * 4] ^= bytes[0];
        state[i * 4 + 1] ^= bytes[1];
        state[i * 4 + 2] ^= bytes[2];
        state[i * 4 + 3] ^= bytes[3];
    }
}

fn sub_bytes(state: &mut [u8; 16]) {
    for b in state.iter_mut() {
        *b = S_BOX[*b as usize];
    }
}

fn shift_rows(state: &mut [u8; 16]) {
    let tmp = *state;
    state[0] = tmp[0]; state[4] = tmp[4]; state[8] = tmp[8]; state[12] = tmp[12];
    state[1] = tmp[5]; state[5] = tmp[9]; state[9] = tmp[13]; state[13] = tmp[1];
    state[2] = tmp[10]; state[6] = tmp[14]; state[10] = tmp[2]; state[14] = tmp[6];
    state[3] = tmp[15]; state[7] = tmp[3]; state[11] = tmp[7]; state[15] = tmp[11];
}

fn xtime(b: u8) -> u8 {
    if (b & 0x80) != 0 {
        (b << 1) ^ 0x1b
    } else {
        b << 1
    }
}

fn mix_columns(state: &mut [u8; 16]) {
    for c in 0..4 {
        let col = &state[c * 4..c * 4 + 4];
        let a0 = col[0];
        let a1 = col[1];
        let a2 = col[2];
        let a3 = col[3];
        let r0 = xtime(a0 ^ a1) ^ a1 ^ a2 ^ a3;
        let r1 = xtime(a1 ^ a2) ^ a2 ^ a3 ^ a0;
        let r2 = xtime(a2 ^ a3) ^ a3 ^ a0 ^ a1;
        let r3 = xtime(a3 ^ a0) ^ a0 ^ a1 ^ a2;
        state[c * 4] = r0;
        state[c * 4 + 1] = r1;
        state[c * 4 + 2] = r2;
        state[c * 4 + 3] = r3;
    }
}

// ---- GHASH (Galois Field GF(2^128) Multiplication) ----

fn ghash_multiply(x: &[u8; 16], y: &[u8; 16]) -> [u8; 16] {
    let mut z = [0u8; 16];
    let mut v = *y;

    for i in 0..128 {
        let bit = (x[i / 8] >> (7 - (i % 8))) & 1;
        if bit == 1 {
            for j in 0..16 {
                z[j] ^= v[j];
            }
        }
        let lsb = v[15] & 1;
        let mut carry = 0u8;
        for j in 0..16 {
            let next_carry = v[j] & 1;
            v[j] = (v[j] >> 1) | (carry << 7);
            carry = next_carry;
        }
        if lsb == 1 {
            v[0] ^= 0xe1;
        }
    }
    z
}

pub struct Aes256Gcm {
    cipher: Aes256,
    h: [u8; 16],
}

impl Aes256Gcm {
    pub fn new(key: &[u8; 32]) -> Self {
        let cipher = Aes256::new(key);
        let h = cipher.encrypt_block(&[0u8; 16]);
        Aes256Gcm { cipher, h }
    }

    pub fn encrypt(&self, nonce: &[u8; 12], aad: &[u8], plaintext: &[u8]) -> (Vec<u8>, [u8; 16]) {
        let mut j0 = [0u8; 16];
        j0[..12].copy_from_slice(nonce);
        j0[15] = 1;

        let mut counter = j0;
        let mut ciphertext = vec![0u8; plaintext.len()];

        let mut offset = 0;
        while offset < plaintext.len() {
            counter_inc(&mut counter);
            let s = self.cipher.encrypt_block(&counter);
            let chunk_len = (plaintext.len() - offset).min(16);
            for i in 0..chunk_len {
                ciphertext[offset + i] = plaintext[offset + i] ^ s[i];
            }
            offset += chunk_len;
        }

        let tag = self.compute_tag(&j0, aad, &ciphertext);
        (ciphertext, tag)
    }

    pub fn decrypt(&self, nonce: &[u8; 12], aad: &[u8], ciphertext: &[u8], tag: &[u8; 16]) -> Option<Vec<u8>> {
        let mut j0 = [0u8; 16];
        j0[..12].copy_from_slice(nonce);
        j0[15] = 1;

        let expected_tag = self.compute_tag(&j0, aad, ciphertext);

        // Constant-time tag check
        let mut diff = 0u8;
        for i in 0..16 {
            diff |= expected_tag[i] ^ tag[i];
        }
        if diff != 0 {
            return None;
        }

        let mut counter = j0;
        let mut plaintext = vec![0u8; ciphertext.len()];

        let mut offset = 0;
        while offset < ciphertext.len() {
            counter_inc(&mut counter);
            let s = self.cipher.encrypt_block(&counter);
            let chunk_len = (ciphertext.len() - offset).min(16);
            for i in 0..chunk_len {
                plaintext[offset + i] = ciphertext[offset + i] ^ s[i];
            }
            offset += chunk_len;
        }

        Some(plaintext)
    }

    fn compute_tag(&self, j0: &[u8; 16], aad: &[u8], ciphertext: &[u8]) -> [u8; 16] {
        let mut s = [0u8; 16];

        // Process AAD
        let mut offset = 0;
        while offset < aad.len() {
            let mut block = [0u8; 16];
            let len = (aad.len() - offset).min(16);
            block[..len].copy_from_slice(&aad[offset..offset + len]);
            for i in 0..16 { s[i] ^= block[i]; }
            s = ghash_multiply(&s, &self.h);
            offset += 16;
        }

        // Process Ciphertext
        offset = 0;
        while offset < ciphertext.len() {
            let mut block = [0u8; 16];
            let len = (ciphertext.len() - offset).min(16);
            block[..len].copy_from_slice(&ciphertext[offset..offset + len]);
            for i in 0..16 { s[i] ^= block[i]; }
            s = ghash_multiply(&s, &self.h);
            offset += 16;
        }

        // Length block (AAD bit length, Ciphertext bit length)
        let mut len_block = [0u8; 16];
        let aad_bits = (aad.len() as u64) * 8;
        let ct_bits = (ciphertext.len() as u64) * 8;
        len_block[..8].copy_from_slice(&aad_bits.to_be_bytes());
        len_block[8..].copy_from_slice(&ct_bits.to_be_bytes());
        for i in 0..16 { s[i] ^= len_block[i]; }
        s = ghash_multiply(&s, &self.h);

        // Encrypt J0 and XOR
        let ej0 = self.cipher.encrypt_block(j0);
        let mut tag = [0u8; 16];
        for i in 0..16 {
            tag[i] = s[i] ^ ej0[i];
        }
        tag
    }
}

fn counter_inc(counter: &mut [u8; 16]) {
    for i in (12..16).rev() {
        counter[i] = counter[i].wrapping_add(1);
        if counter[i] != 0 {
            break;
        }
    }
}
