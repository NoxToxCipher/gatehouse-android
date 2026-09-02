//! JNI Interface for SpaceInvadersNative.java.

use jni::JNIEnv;
use jni::objects::{JClass, JIntArray, JFloatArray, JByteArray};
use jni::sys::{jlong, jfloat, jint, jboolean, jshortArray};
use crate::engine::GameState;
use crate::audio::{synthesize_sfx, SoundEffect};
use crate::bunkers::{BUNKER_W, BUNKER_H};
use crate::projectiles::MAX_PROJECTILES;

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeCreateEngine(
    _env: JNIEnv,
    _class: JClass,
    screen_w: jfloat,
    screen_h: jfloat,
) -> jlong {
    let state = Box::new(GameState::new(screen_w, screen_h));
    Box::into_raw(state) as jlong
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeDestroyEngine(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        drop(Box::from_raw(handle as *mut GameState));
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeResetGame(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if let Some(state) = (handle as *mut GameState).as_mut() {
        state.start_new_game();
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeUpdate(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    dt_ms: jfloat,
    target_x: jfloat,
    shoot: jboolean,
) -> jint {
    if let Some(state) = (handle as *mut GameState).as_mut() {
        state.update(dt_ms, target_x, shoot != 0) as jint
    } else {
        0
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeGetState(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    out_ints: JIntArray,
    out_floats: JFloatArray,
) {
    let state = match (handle as *mut GameState).as_ref() {
        Some(s) => s,
        None => return,
    };

    // Pack ints: [score, high_score, lives, wave, game_over, game_won, ufo_active, alive_aliens, anim_frame, march_note]
    let ints: [i32; 10] = [
        state.score as i32,
        state.high_score as i32,
        state.player_lives as i32,
        state.wave as i32,
        if state.game_over { 1 } else { 0 },
        if state.game_won { 1 } else { 0 },
        if state.ufo_active { 1 } else { 0 },
        state.fleet.alive_count as i32,
        state.fleet.anim_frame as i32,
        state.march_note_idx as i32,
    ];
    let _ = env.set_int_array_region(&out_ints, 0, &ints);

    // Pack floats: [player_x, fleet_base_x, fleet_base_y, ufo_x, col_spacing, row_spacing]
    let floats: [f32; 6] = [
        state.player_x,
        state.fleet.base_x,
        state.fleet.base_y,
        state.ufo_x,
        state.col_spacing,
        state.row_spacing,
    ];
    let _ = env.set_float_array_region(&out_floats, 0, &floats);
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeGetAlienRowMask(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    row: jint,
) -> jint {
    if let Some(state) = (handle as *mut GameState).as_ref() {
        if row >= 0 && (row as usize) < crate::invaders::ROWS {
            return state.fleet.grid[row as usize] as jint;
        }
    }
    0
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeGetBunkerGrid(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    bunker_idx: jint,
    out_grid: JByteArray,
) {
    let state = match (handle as *mut GameState).as_ref() {
        Some(s) => s,
        None => return,
    };

    if bunker_idx >= 0 && (bunker_idx as usize) < state.bunkers.len() {
        let b = &state.bunkers[bunker_idx as usize];
        let mut bytes = [0i8; BUNKER_W * BUNKER_H];
        for r in 0..BUNKER_H {
            for c in 0..BUNKER_W {
                bytes[r * BUNKER_W + c] = if b.blocks[r][c] { 1 } else { 0 };
            }
        }
        let _ = env.set_byte_array_region(&out_grid, 0, &bytes);
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeGetProjectiles(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    out_floats: JFloatArray,
) -> jint {
    let state = match (handle as *mut GameState).as_ref() {
        Some(s) => s,
        None => return 0,
    };

    let mut packed = [0f32; MAX_PROJECTILES * 4];
    let mut count = 0;
    for p in state.projectiles.pool.iter() {
        if p.active {
            let offset = count * 4;
            if offset + 3 < packed.len() {
                packed[offset] = p.x;
                packed[offset + 1] = p.y;
                packed[offset + 2] = p.vy;
                packed[offset + 3] = if p.is_player { 1.0 } else { 0.0 };
                count += 1;
            }
        }
    }
    let _ = env.set_float_array_region(&out_floats, 0, &packed[..(count * 4)]);
    count as jint
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SpaceInvadersNative_nativeSynthesizeAudio(
    mut env: JNIEnv,
    _class: JClass,
    sfx_type: jint,
    note_idx: jint,
) -> jshortArray {
    let sfx = match sfx_type {
        0 => SoundEffect::MarchNote(note_idx as u8),
        1 => SoundEffect::PlayerLaser,
        2 => SoundEffect::AlienExplosion,
        3 => SoundEffect::PlayerDeath,
        4 => SoundEffect::UfoWarble,
        _ => SoundEffect::MarchNote(0),
    };

    let mut buf = [0i16; 44100 / 2]; // 500ms max buffer
    let samples_gen = synthesize_sfx(sfx, &mut buf);

    if let Ok(jarr) = env.new_short_array(samples_gen as i32) {
        let _ = env.set_short_array_region(&jarr, 0, &buf[..samples_gen]);
        jarr.into_raw()
    } else {
        std::ptr::null_mut()
    }
}
