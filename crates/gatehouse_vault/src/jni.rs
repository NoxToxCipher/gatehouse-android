//! JNI Exports for `au.com.dss.gatehouse.GatehouseVault`.

use std::os::raw::{c_char, c_void};
use crate::vault::{
    decrypt_vault_payload, derive_vault_key, encrypt_vault_payload, sign_shift_attestation,
    verify_shift_attestation,
};

#[repr(C)]
pub struct JNIEnv {
    functions: *const JNIInvokeInterface,
}

#[repr(C)]
struct JNIInvokeInterface {
    reserved0: *mut c_void,
    reserved1: *mut c_void,
    reserved2: *mut c_void,
    reserved3: *mut c_void,
    // Native method table functions used by JNI
    get_string_utf_chars: unsafe extern "C" fn(*mut JNIEnv, jstring, *mut jboolean) -> *const c_char,
    release_string_utf_chars: unsafe extern "C" fn(*mut JNIEnv, jstring, *const c_char),
    get_array_length: unsafe extern "C" fn(*mut JNIEnv, jarray) -> jsize,
    get_byte_array_region: unsafe extern "C" fn(*mut JNIEnv, jbyteArray, jsize, jsize, *mut i8),
    set_byte_array_region: unsafe extern "C" fn(*mut JNIEnv, jbyteArray, jsize, jsize, *const i8),
    new_byte_array: unsafe extern "C" fn(*mut JNIEnv, jsize) -> jbyteArray,
    new_string_utf: unsafe extern "C" fn(*mut JNIEnv, *const c_char) -> jstring,
}

#[allow(non_camel_case_types)]
type jclass = *mut c_void;
#[allow(non_camel_case_types)]
type jstring = *mut c_void;
#[allow(non_camel_case_types)]
type jarray = *mut c_void;
#[allow(non_camel_case_types)]
type jbyteArray = *mut c_void;
#[allow(non_camel_case_types)]
type jsize = i32;
#[allow(non_camel_case_types)]
type jboolean = u8;

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_GatehouseVault_nativeDeriveKey(
    env: *mut JNIEnv,
    _class: jclass,
    pin: jstring,
    hardware_id: jstring,
) -> jbyteArray {
    let p = get_string(env, pin);
    let h = get_string(env, hardware_id);

    let mut key = [0u8; 32];
    derive_vault_key(&p, &h, &mut key);

    new_byte_array(env, &key)
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_GatehouseVault_nativeEncryptPayload(
    env: *mut JNIEnv,
    _class: jclass,
    key_bytes: jbyteArray,
    aad_bytes: jbyteArray,
    plaintext_bytes: jbyteArray,
    nonce_bytes: jbyteArray,
) -> jbyteArray {
    let key_vec = get_byte_array(env, key_bytes);
    if key_vec.len() != 32 {
        return std::ptr::null_mut();
    }
    let mut key = [0u8; 32];
    key.copy_from_slice(&key_vec);

    let aad = get_byte_array(env, aad_bytes);
    let plaintext = get_byte_array(env, plaintext_bytes);
    let nonce_vec = get_byte_array(env, nonce_bytes);

    let mut nonce = [0u8; 12];
    if nonce_vec.len() == 12 {
        nonce.copy_from_slice(&nonce_vec);
    }

    let encrypted = encrypt_vault_payload(&key, &aad, &plaintext, &nonce);
    new_byte_array(env, &encrypted)
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_GatehouseVault_nativeDecryptPayload(
    env: *mut JNIEnv,
    _class: jclass,
    key_bytes: jbyteArray,
    aad_bytes: jbyteArray,
    payload_bytes: jbyteArray,
) -> jbyteArray {
    let key_vec = get_byte_array(env, key_bytes);
    if key_vec.len() != 32 {
        return std::ptr::null_mut();
    }
    let mut key = [0u8; 32];
    key.copy_from_slice(&key_vec);

    let aad = get_byte_array(env, aad_bytes);
    let payload = get_byte_array(env, payload_bytes);

    match decrypt_vault_payload(&key, &aad, &payload) {
        Some(decrypted) => new_byte_array(env, &decrypted),
        None => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_GatehouseVault_nativeSignAttestation(
    env: *mut JNIEnv,
    _class: jclass,
    shift_json: jstring,
    officer_licence: jstring,
    key_bytes: jbyteArray,
) -> jstring {
    let sj = get_string(env, shift_json);
    let ol = get_string(env, officer_licence);
    let key_vec = get_byte_array(env, key_bytes);
    if key_vec.len() != 32 {
        return new_string(env, "");
    }
    let mut key = [0u8; 32];
    key.copy_from_slice(&key_vec);

    let seal = sign_shift_attestation(&sj, &ol, &key);
    new_string(env, &seal)
}

#[no_mangle]
pub unsafe extern "C" fn Java_au_com_dss_gatehouse_GatehouseVault_nativeVerifyAttestation(
    env: *mut JNIEnv,
    _class: jclass,
    shift_json: jstring,
    officer_licence: jstring,
    claimed_seal: jstring,
    key_bytes: jbyteArray,
) -> jboolean {
    let sj = get_string(env, shift_json);
    let ol = get_string(env, officer_licence);
    let cs = get_string(env, claimed_seal);
    let key_vec = get_byte_array(env, key_bytes);
    if key_vec.len() != 32 {
        return 0;
    }
    let mut key = [0u8; 32];
    key.copy_from_slice(&key_vec);

    if verify_shift_attestation(&sj, &ol, &cs, &key) {
        1
    } else {
        0
    }
}

// ---- Helpers ----

unsafe fn get_string(env: *mut JNIEnv, s: jstring) -> String {
    if s.is_null() {
        return String::new();
    }
    let funcs = (*env).functions;
    let utf = ((*funcs).get_string_utf_chars)(env, s, std::ptr::null_mut());
    if utf.is_null() {
        return String::new();
    }
    let cstr = std::ffi::CStr::from_ptr(utf);
    let str_slice = cstr.to_string_lossy().into_owned();
    ((*funcs).release_string_utf_chars)(env, s, utf);
    str_slice
}

unsafe fn new_string(env: *mut JNIEnv, s: &str) -> jstring {
    let funcs = (*env).functions;
    let c_str = std::ffi::CString::new(s).unwrap_or_default();
    ((*funcs).new_string_utf)(env, c_str.as_ptr())
}

unsafe fn get_byte_array(env: *mut JNIEnv, arr: jbyteArray) -> Vec<u8> {
    if arr.is_null() {
        return Vec::new();
    }
    let funcs = (*env).functions;
    let len = ((*funcs).get_array_length)(env, arr);
    if len <= 0 {
        return Vec::new();
    }
    let mut vec = vec![0u8; len as usize];
    ((*funcs).get_byte_array_region)(env, arr, 0, len, vec.as_mut_ptr() as *mut i8);
    vec
}

unsafe fn new_byte_array(env: *mut JNIEnv, bytes: &[u8]) -> jbyteArray {
    let funcs = (*env).functions;
    let arr = ((*funcs).new_byte_array)(env, bytes.len() as jsize);
    if !arr.is_null() && !bytes.is_empty() {
        ((*funcs).set_byte_array_region)(
            env,
            arr,
            0,
            bytes.len() as jsize,
            bytes.as_ptr() as *const i8,
        );
    }
    arr
}
