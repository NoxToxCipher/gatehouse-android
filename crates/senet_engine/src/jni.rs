//! JNI Interface for SenetNative.java.

use jni::JNIEnv;
use jni::objects::{JClass, JIntArray, JFloatArray};
use jni::sys::{jlong, jfloat, jint, jboolean};
use crate::board::SenetBoard;
use crate::expectimax::ExpectimaxAI;
use crate::shader::{render_sandstone_tile, render_piece_texture};

pub struct SenetEngineState {
    pub board: SenetBoard,
    pub ai: ExpectimaxAI,
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeCreateEngine(
    _env: JNIEnv,
    _class: JClass,
    difficulty: jint,
) -> jlong {
    let state = Box::new(SenetEngineState {
        board: SenetBoard::new(),
        ai: ExpectimaxAI::new(difficulty as u8),
    });
    Box::into_raw(state) as jlong
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeDestroyEngine(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        drop(Box::from_raw(handle as *mut SenetEngineState));
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeReset(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if let Some(state) = (handle as *mut SenetEngineState).as_mut() {
        state.board = SenetBoard::new();
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativePlayMove(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    piece_idx: jint,
    roll: jint,
) -> jboolean {
    if let Some(state) = (handle as *mut SenetEngineState).as_mut() {
        if state.board.play_move(piece_idx as usize, roll as u8) {
            1
        } else {
            0
        }
    } else {
        0
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeGetLegalMovesMask(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    roll: jint,
) -> jint {
    if let Some(state) = (handle as *mut SenetEngineState).as_ref() {
        state.board.get_legal_moves_mask(roll as u8) as jint
    } else {
        0
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeFindBestMove(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    roll: jint,
    difficulty: jint,
    out_result: JFloatArray, // [best_move_idx, winrate]
) -> jint {
    if let Some(state) = (handle as *mut SenetEngineState).as_mut() {
        state.ai.difficulty_tier = difficulty.clamp(0, 2) as u8;
        let (best_move, winrate) = state.ai.find_best_move(&state.board, roll as u8);
        let res: [f32; 2] = [best_move as f32, winrate];
        let _ = env.set_float_array_region(&out_result, 0, &res);
        best_move as jint
    } else {
        0
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeEvaluatePosition(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    turn: jint,
) -> jfloat {
    if let Some(state) = (handle as *mut SenetEngineState).as_ref() {
        state.ai.evaluate_position(&state.board, turn as u8)
    } else {
        0.0
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeGetBoardState(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    out_white_pieces: JIntArray,
    out_black_pieces: JIntArray,
    out_meta: JIntArray, // [white_borne, black_borne, current_turn]
) {
    if let Some(state) = (handle as *mut SenetEngineState).as_ref() {
        let wp: [i32; 5] = [
            state.board.white_pieces[0] as i32,
            state.board.white_pieces[1] as i32,
            state.board.white_pieces[2] as i32,
            state.board.white_pieces[3] as i32,
            state.board.white_pieces[4] as i32,
        ];
        let bp: [i32; 5] = [
            state.board.black_pieces[0] as i32,
            state.board.black_pieces[1] as i32,
            state.board.black_pieces[2] as i32,
            state.board.black_pieces[3] as i32,
            state.board.black_pieces[4] as i32,
        ];
        let meta: [i32; 3] = [
            state.board.white_borne_off as i32,
            state.board.black_borne_off as i32,
            state.board.current_turn as i32,
        ];

        let _ = env.set_int_array_region(&out_white_pieces, 0, &wp);
        let _ = env.set_int_array_region(&out_black_pieces, 0, &bp);
        let _ = env.set_int_array_region(&out_meta, 0, &meta);
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeRenderTileTexture(
    env: JNIEnv,
    _class: JClass,
    width: jint,
    height: jint,
    tile_type: jint,
    out_pixels: JIntArray,
) {
    let w = width as usize;
    let h = height as usize;
    let total = w * h;
    if total == 0 { return; }

    let mut buf = vec![0i32; total];
    render_sandstone_tile(&mut buf, w, h, tile_type as u8);
    let _ = env.set_int_array_region(&out_pixels, 0, &buf);
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_SenetNative_nativeRenderPieceTexture(
    env: JNIEnv,
    _class: JClass,
    width: jint,
    height: jint,
    is_pharaoh: jboolean,
    out_pixels: JIntArray,
) {
    let w = width as usize;
    let h = height as usize;
    let total = w * h;
    if total == 0 { return; }

    let mut buf = vec![0i32; total];
    render_piece_texture(&mut buf, w, h, is_pharaoh != 0);
    let _ = env.set_int_array_region(&out_pixels, 0, &buf);
}
