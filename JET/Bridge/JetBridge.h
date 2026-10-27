// JetBridge — C ABI between the SwiftUI shell and the JET runtime (Android-on-iOS translation layer).
//
// ABI version 1. The full contract (threading, ownership, return codes, bundle layout, link recipe) is in
// docs/RUNTIME_LINK.md; keep the two in sync. Two implementations exist:
//   * JET/Bridge/JetBridgeStub.c  — compiled when JET_RUNTIME_LINKED is not defined (shell preview);
//   * Runtime/jet_bridge_aot.c    — built by scripts/build_host_lib.sh into libjetruntime.a together with the
//                                   JET AOT runtime and at most one AOT-compiled app module.
#pragma once
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define JET_BRIDGE_ABI_VERSION 1

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

/// Version string of the bridge implementation, e.g. "stub-0.2" or "aot-0.1+<module>". Static storage.
const char *jet_bridge_version(void);

/// 1 when a real runtime is linked into this binary, 0 for the stub.
int32_t jet_bridge_has_runtime(void);

/// AOT app modules compiled into this binary, one per line: "package\tactivity\tdex-crc32-hex\tlabel\n".
/// Empty string for the stub or a runtime-only build. Static storage.
const char *jet_bridge_modules(void);

/// Translate an APK into a JET bundle directory. `bundle_dir` must exist and be writable. `progress` may be NULL.
/// Always (re)writes bundle_dir/app.properties (see docs/RUNTIME_LINK.md §4) unless it returns ERR_INPUT/ERR_IO.
/// Returns OK when an AOT module in this binary matches the APK's code (classes.dex CRC-32 from the zip central
/// directory), ERR_UNSUPPORTED when it does not, STUB from the stub. Blocking; call off the main thread.
int32_t jet_translate_apk(const char *apk_path, const char *bundle_dir, jet_progress_fn progress, void *ctx);

/// Start the bundle's launcher Activity (ctor + onCreate). `activity_class` may be NULL/"" to use the bundle's
/// launcher. `ui_view` is a UIView * (unretained, may be NULL); ABI v1 runtimes do not draw into it — the host
/// renders jet_runtime_ui_snapshot() instead. Main thread only. Returns OK, ERR_INPUT, ERR_UNSUPPORTED,
/// ERR_RUNTIME, or JET_RUNTIME_NOT_LINKED (stub). Calling it again restarts the Activity.
int32_t jet_runtime_main(const char *bundle_path, const char *activity_class, void *ui_view);

/// Touch input in view pixels. action: 0=down 1=up 2=move 3=cancel. Main thread. Ignored by ABI v1 runtimes.
void jet_runtime_touch(int32_t pointer_id, int32_t action, float x, float y);

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
