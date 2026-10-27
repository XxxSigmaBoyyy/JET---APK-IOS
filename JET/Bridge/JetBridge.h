// JetBridge — C ABI between the SwiftUI shell and the JET runtime (Android-on-iOS translation layer).
//
// ABI version 2. The full contract (threading, ownership, return codes, bundle layout, rendering) is in
// docs/RUNTIME_LINK.md; keep the two in sync. Implementation: Runtime/jet_bridge_aot.c, compiled from source into
// the app target together with the JET AOT runtime (Runtime/*.c) and the AOT-compiled app modules (Modules/*/).
// iOS forbids JIT and loading code at run time, so app code is translated ahead of time (DEX -> C) and linked in;
// importing an APK on the phone matches its classes.dex against the compiled-in modules.
#pragma once
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define JET_BRIDGE_ABI_VERSION 2

/// Return codes. Non-negative = success, negative = failure.
#define JET_BRIDGE_OK               0   ///< step completed by the real runtime
#define JET_BRIDGE_STUB             1   ///< step accepted by the stub; nothing was translated
#define JET_BRIDGE_ERR_INPUT       -1   ///< bad arguments / not an APK (no zip central directory, no classes.dex)
#define JET_BRIDGE_ERR_IO          -2   ///< file system error (bundle_dir missing or not writable)
#define JET_BRIDGE_ERR_UNSUPPORTED -3   ///< valid APK, but this binary contains no AOT module for its code
#define JET_BRIDGE_ERR_RUNTIME     -4   ///< the translated app threw / hit a fatal runtime error (see status)
#define JET_BRIDGE_ERR_STATE       -5   ///< call not valid now (e.g. click before jet_runtime_main succeeded)
#define JET_RUNTIME_NOT_LINKED    -10   ///< jet_runtime_main and friends: no runtime in this binary

/// Progress callback: fraction 0...1 and a short stage name ("scan", "match", "bundle", ...).
/// Invoked synchronously on the calling thread; `stage` is valid only during the call.
typedef void (*jet_progress_fn)(float fraction, const char *stage, void *ctx);

/// ABI version implemented by the linked bridge (JET_BRIDGE_ABI_VERSION of its build).
int32_t jet_bridge_abi_version(void);

/// Version string of the bridge implementation, e.g. "aot-0.2+<module>". Static storage.
const char *jet_bridge_version(void);

/// 1 when a real runtime is linked into this binary (always 1 since ABI v2; the stub was removed).
int32_t jet_bridge_has_runtime(void);

/// AOT app modules compiled into this binary, one per line: "package\tactivity\tdex-crc32-hex\tlabel\n".
/// Empty for a runtime-only build. Static storage.
const char *jet_bridge_modules(void);

/// Translate an APK into a JET bundle directory. `bundle_dir` must exist and be writable. `progress` may be NULL.
/// Always (re)writes bundle_dir/app.properties (see docs/RUNTIME_LINK.md §4) unless it returns ERR_INPUT/ERR_IO.
/// Returns OK when an AOT module in this binary matches the APK's code (classes.dex CRC-32 from the zip central
/// directory), ERR_UNSUPPORTED when it does not. Blocking; call off the main thread.
int32_t jet_translate_apk(const char *apk_path, const char *bundle_dir, jet_progress_fn progress, void *ctx);

/// Start the bundle's launcher Activity (ctor + onCreate). `activity_class` may be NULL/"" to use the bundle's
/// launcher. `ui_view` is reserved (pass NULL); the host draws jet_runtime_frame(). Main thread only. Returns OK,
/// ERR_INPUT, ERR_UNSUPPORTED or ERR_RUNTIME. Calling it again restarts the Activity.
int32_t jet_runtime_main(const char *bundle_path, const char *activity_class, void *ui_view);

/// Touch input in viewport pixels (see jet_runtime_set_viewport). action: 0=down 1=up 2=move 3=cancel,
/// 4=long-press timeout (the host fires it ViewConfiguration.getLongPressTimeout() = 500 ms after a down that was not
/// yet released or cancelled). Runs the app's listeners synchronously. Main thread.
/// Returns 1 when the frame changed (host should redraw), 0 when not, ERR_STATE/ERR_RUNTIME on failure.
int32_t jet_runtime_touch(int32_t pointer_id, int32_t action, float x, float y);

// ---- ABI v2: rendering. The runtime lays the Activity out (AOSP measure/layout semantics) and returns a display
// list; the host draws it (CoreGraphics on iOS) in order, each op clipped to its clip rect. Coordinates are pixels
// with the origin at the top left of the viewport.
#define JET_OP_RECT   1   ///< filled rect x,y,w,h in argb
#define JET_OP_TEXT   2   ///< text in box x,y,w,h: font size `size` px, `flags`, Android `gravity` bits
#define JET_OP_RRECT  3   ///< filled rounded rect, corner radius `size`
#define JET_TEXT_BOLD 1
typedef struct JetDrawOp {
    int32_t op, flags, gravity, reserved;
    float x, y, w, h;
    float clip_x, clip_y, clip_w, clip_h;
    float size;
    uint32_t argb;
    const char *text;        ///< UTF-8; valid until the next runtime call
} JetDrawOp;

/// Text metrics callback (host font): advance width, ascent and descent (positive) in px for one line of UTF-8 text.
typedef void (*jet_text_measure_fn)(const char *utf8, float size_px, int32_t bold, float *width, float *ascent, float *descent);
void jet_runtime_set_text_measurer(jet_text_measure_fn fn);

/// Viewport size in pixels and density (pixels per dip, e.g. UIScreen.scale). Call before jet_runtime_main and on
/// every size change. Main thread.
void jet_runtime_set_viewport(int32_t width_px, int32_t height_px, float density);

/// Lay out and draw the running Activity. Stores a pointer to `count` ops in *ops (valid until the next runtime
/// call) and the window background colour in *window_bg. Returns the op count (0 when nothing runs). Main thread.
int32_t jet_runtime_frame(const JetDrawOp **ops, uint32_t *window_bg);

/// Milliseconds until the runtime wants another frame (e.g. a Toast expiring), or -1 for none.
int32_t jet_runtime_next_wakeup_ms(void);

/// Automation helper: centre (pixels) of the first visible clickable view whose text equals `label`.
/// Returns OK or ERR_INPUT.
int32_t jet_runtime_find_label(const char *label, float *x, float *y);

/// Copy the running Activity's view tree into `buf` (UTF-8, NUL-terminated, truncated to cap-1 bytes), one node per
/// line: "depth\tkind\tid-hex\tflags\ttext\n" where kind is "text" or "button", flags is "-" or "s" (selected).
/// Returns the full length needed (excluding NUL), 0 when nothing runs, or JET_RUNTIME_NOT_LINKED. Main thread.
int32_t jet_runtime_ui_snapshot(char *buf, int32_t cap);

/// Fire the click listener of the first button whose label equals `label` (UTF-8). Main thread.
/// Returns OK, ERR_INPUT (no such button / no listener), ERR_STATE, ERR_RUNTIME, or JET_RUNTIME_NOT_LINKED.
int32_t jet_runtime_click(const char *label);

/// Stop the running Activity (called when the host view goes away). Idempotent. Main thread.
void jet_runtime_stop(void);

/// Human-readable status of the last bridge call. Static storage, overwritten by the next call; copy it.
const char *jet_runtime_status(void);

#ifdef __cplusplus
}
#endif
