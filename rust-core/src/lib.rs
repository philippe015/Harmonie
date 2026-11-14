//! Noyau Rust compatible Android arm64-v8a.
//! Les traitements PCM sont prêts à être utilisés par un décodeur natif ;
//! l'interface v10 utilise actuellement Web Audio pour l'écho et l'espace.
use std::ffi::{c_char, CStr};
use std::ptr;

static VERSION: &[u8] = b"0.3.0\0";

#[no_mangle]
pub extern "C" fn harmonie_version() -> *const c_char {
    VERSION.as_ptr().cast()
}

/// Identifiant stable d'une station radio, sans allocation.
#[no_mangle]
pub unsafe extern "C" fn harmonie_station_id(name: *const c_char, url: *const c_char) -> u64 {
    if name.is_null() || url.is_null() {return 0;}
    let mut hash: u64 = 0xcbf29ce484222325;
    for slice in [CStr::from_ptr(name).to_bytes(), CStr::from_ptr(url).to_bytes()] {
        for byte in slice {hash = (hash ^ *byte as u64).wrapping_mul(0x100000001b3);}
        hash = (hash ^ 0xff).wrapping_mul(0x100000001b3);
    }
    hash
}

/// Parse les deux formats courants de balise LRC [mm:ss] et [mm:ss.xx].
#[no_mangle]
pub unsafe extern "C" fn harmonie_lrc_ms(value: *const c_char) -> i64 {
    if value.is_null() {return -1;}
    let text = match CStr::from_ptr(value).to_str() {Ok(s)=>s.trim(), Err(_)=>return -1};
    let t = text.strip_prefix('[').unwrap_or(text);
    let t = t.strip_suffix(']').unwrap_or(t);
    let (minute, second) = match t.split_once(':') {Some(x)=>x, None=>return -1};
    let min: i64 = match minute.parse(){Ok(n)=>n,Err(_)=>return -1};
    let sec: f64 = match second.parse(){Ok(n)=>n,Err(_)=>return -1};
    if !(0..=999).contains(&min) || !sec.is_finite() || !(0.0..60.0).contains(&sec) {return -1;}
    min*60_000+(sec*1000.0).round() as i64
}

/// Modifie un tampon PCM intercalé stéréo en place. Valeur width dans [0, 2].
/// 1.0 = naturel ; 0.0 = mono ; 2.0 = largeur accentuée.
#[no_mangle]
pub unsafe extern "C" fn harmonie_stereo_width(samples: *mut f32, frames: usize, width: f32) -> i32 {
    if samples.is_null() || frames > 2_000_000 || !width.is_finite() {return -1;}
    let samples = std::slice::from_raw_parts_mut(samples, frames*2);
    let w = width.clamp(0.0, 2.0);
    for frame in samples.chunks_exact_mut(2) {
        let mid = (frame[0]+frame[1])*0.5;
        let side = (frame[0]-frame[1])*0.5*w;
        frame[0] = (mid+side).clamp(-1.0, 1.0);
        frame[1] = (mid-side).clamp(-1.0, 1.0);
    }
    0
}

pub struct Echo {
    left: Vec<f32>, right: Vec<f32>, cursor: usize, feedback: f32,
}

/// Crée un écho PCM dont la mémoire reste dans Rust (à libérer avec echo_free).
#[no_mangle]
pub extern "C" fn harmonie_echo_new(rate: u32, delay_ms: u32, feedback: f32) -> *mut Echo {
    if !(8_000..=192_000).contains(&rate) || !(10..=2000).contains(&delay_ms) || !feedback.is_finite() {
        return ptr::null_mut();
    }
    let n = ((rate as u64 * delay_ms as u64)/1000) as usize;
    Box::into_raw(Box::new(Echo {left:vec![0.0;n.max(1)],right:vec![0.0;n.max(1)],cursor:0,feedback:feedback.clamp(0.0,0.85)}))
}

#[no_mangle]
pub unsafe extern "C" fn harmonie_echo_process(echo: *mut Echo, samples: *mut f32, frames: usize, wet: f32) -> i32 {
    if echo.is_null() || samples.is_null() || frames > 2_000_000 || !wet.is_finite() {return -1;}
    let e=&mut *echo;
    let data=std::slice::from_raw_parts_mut(samples,frames*2);
    let wet=wet.clamp(0.0,1.0);
    for frame in data.chunks_exact_mut(2) {
        let l=frame[0];let r=frame[1];
        let dl=e.left[e.cursor];let dr=e.right[e.cursor];
        e.left[e.cursor]=(l+dl*e.feedback).clamp(-1.0,1.0);
        e.right[e.cursor]=(r+dr*e.feedback).clamp(-1.0,1.0);
        frame[0]=(l*(1.0-wet)+dl*wet).clamp(-1.0,1.0);
        frame[1]=(r*(1.0-wet)+dr*wet).clamp(-1.0,1.0);
        e.cursor=(e.cursor+1)%e.left.len();
    }
    0
}

#[no_mangle]
pub unsafe extern "C" fn harmonie_echo_free(echo: *mut Echo) {
    if !echo.is_null() {drop(Box::from_raw(echo));}
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::ffi::CString;
    #[test] fn lrc(){unsafe{let x=CString::new("[01:05.50]").unwrap();assert_eq!(harmonie_lrc_ms(x.as_ptr()),65500);}}
    #[test] fn stereo_mono(){let mut f=[1.0_f32,0.0];unsafe{assert_eq!(harmonie_stereo_width(f.as_mut_ptr(),1,0.0),0)};assert!((f[0]-0.5).abs()<0.001);}
    #[test] fn echo_starts(){let e=harmonie_echo_new(48000,100,0.3);assert!(!e.is_null());unsafe{harmonie_echo_free(e)}}
    #[test] fn stable_id(){unsafe{let a=CString::new("Jazz").unwrap();let b=CString::new("https://example.com").unwrap();assert_eq!(harmonie_station_id(a.as_ptr(),b.as_ptr()),harmonie_station_id(a.as_ptr(),b.as_ptr()))}}
}
