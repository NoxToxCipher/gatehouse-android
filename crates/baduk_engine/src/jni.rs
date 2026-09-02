//! JNI Interface for BadukNative.java.

use jni::JNIEnv;
use jni::objects::{JClass, JIntArray, JFloatArray, JByteArray};
use jni::sys::{jlong, jfloat, jint, jboolean};
use crate::board::{GoBoard, Point, MAX_POINTS};
use crate::mcts::MctsBot;
use crate::benson::evaluate_territory;
use crate::ladder::is_ladder_capture;

pub struct BadukEngineState {
    pub board: GoBoard,
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeCreateEngine(
    _env: JNIEnv,
    _class: JClass,
    size: jint,
) -> jlong {
    let state = Box::new(BadukEngineState {
        board: GoBoard::new(size as usize),
    });
    Box::into_raw(state) as jlong
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeDestroyEngine(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        drop(Box::from_raw(handle as *mut BadukEngineState));
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeReset(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    size: jint,
) {
    if let Some(state) = (handle as *mut BadukEngineState).as_mut() {
        state.board = GoBoard::new(size as usize);
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativePlayMove(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    x: jint,
    y: jint,
    color: jint,
) -> jboolean {
    if let Some(state) = (handle as *mut BadukEngineState).as_mut() {
        let pt = if x < 0 || y < 0 { Point::PASS } else { Point::new(x as usize, y as usize) };
        if state.board.play_move(pt, color as u8) { 1 } else { 0 }
    } else {
        0
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeIsLegal(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    x: jint,
    y: jint,
    color: jint,
) -> jboolean {
    if let Some(state) = (handle as *mut BadukEngineState).as_ref() {
        if x < 0 || y < 0 {
            1
        } else if state.board.is_legal(x as usize, y as usize, color as u8) {
            1
        } else {
            0
        }
    } else {
        0
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeFindBestMove(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    color: jint,
    difficulty_tier: jint,
    out_coords: JIntArray,
    out_heatmap: JFloatArray,
) -> jfloat {
    let state = match (handle as *mut BadukEngineState).as_mut() {
        Some(s) => s,
        None => return 0.5,
    };

    let mut bot = MctsBot::new(difficulty_tier as usize);
    let (best_pt, winrate, heatmap) = bot.search(&state.board, color as u8);

    let coords: [i32; 2] = [
        if best_pt.is_pass() { -1 } else { best_pt.x as i32 },
        if best_pt.is_pass() { -1 } else { best_pt.y as i32 },
    ];
    let _ = env.set_int_array_region(&out_coords, 0, &coords);
    let _ = env.set_float_array_region(&out_heatmap, 0, &heatmap);

    winrate
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeEvaluateTerritory(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    komi: jfloat,
    out_scores: JIntArray,
    out_territory_grid: JByteArray,
) {
    let state = match (handle as *mut BadukEngineState).as_ref() {
        Some(s) => s,
        None => return,
    };

    let (res, territory_map) = evaluate_territory(&state.board, komi);

    let scores: [i32; 5] = [
        res.black_score,
        res.white_score,
        res.black_territory as i32,
        res.white_territory as i32,
        res.dame as i32,
    ];
    let _ = env.set_int_array_region(&out_scores, 0, &scores);

    let mut byte_map = [0i8; MAX_POINTS];
    for i in 0..MAX_POINTS {
        byte_map[i] = territory_map[i] as i8;
    }
    let _ = env.set_byte_array_region(&out_territory_grid, 0, &byte_map);
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeIsLadderCapture(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    x: jint,
    y: jint,
) -> jboolean {
    if let Some(state) = (handle as *mut BadukEngineState).as_ref() {
        if x >= 0 && y >= 0 && is_ladder_capture(&state.board, x as usize, y as usize) {
            1
        } else {
            0
        }
    } else {
        0
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeRenderStoneTexture(
    mut env: JNIEnv,
    _class: JClass,
    width: jint,
    height: jint,
    stone_type: jint,
    theme: jint,
    out_pixels: JIntArray,
) {
    let w = width as usize;
    let h = height as usize;
    let total = w * h;
    if total == 0 { return; }

    let mut buf = vec![0i32; total];
    crate::shader::render_stone_texture(&mut buf, w, h, stone_type as u8, theme as u32);
    let _ = env.set_int_array_region(&out_pixels, 0, &buf);
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_BadukNative_nativeRenderWoodgrainTexture(
    mut env: JNIEnv,
    _class: JClass,
    width: jint,
    height: jint,
    theme: jint,
    out_pixels: JIntArray,
) {
    let w = width as usize;
    let h = height as usize;
    let total = w * h;
    if total == 0 { return; }

    let mut buf = vec![0i32; total];
    crate::shader::render_woodgrain_texture(&mut buf, w, h, theme as u32);
    let _ = env.set_int_array_region(&out_pixels, 0, &buf);
}
