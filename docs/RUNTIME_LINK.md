# JET runtime link path — JetBridge C ABI v1

This document is the contract between the SwiftUI shell (`JET/`) and the JET runtime library. The header
`JET/Bridge/JetBridge.h` is normative; this file explains it. Keep both in sync.

## 1. Why the runtime is a static library

iOS forbids JIT and loading code at runtime. Android app code therefore cannot be "translated" on the phone:
it is compiled ahead of time on a build machine (DEX → Jimple → C → arm64 objects) and **linked into the IPA**.
On the phone, "translate" only means: recognise which prebuilt module an imported APK corresponds to.

```
APK (open-source) ──JimpleToC + gen_res.py──▶ app.c / res.c ─┐
JET AOT runtime (rt.c, kernel.c) ────────────────────────────┼─clang─▶ libjetruntime.a ──force_load──▶ JET.app
Runtime/jet_bridge_aot.c (this ABI) ─────────────────────────┘
```

## 2. Implementations

| File | When compiled | `jet_bridge_version()` | `jet_bridge_has_runtime()` |
|---|---|---|---|
| `JET/Bridge/JetBridgeStub.c` | `JET_RUNTIME_LINKED` not defined (default, simulator) | `stub-0.2` | 0 |
| `Runtime/jet_bridge_aot.c` (in `libjetruntime.a`) | device build with `JET_RUNTIME=YES` | `aot-0.1+<module>` | 1 |

## 3. Functions and return codes

Codes: `OK 0`, `STUB 1`, `ERR_INPUT -1`, `ERR_IO -2`, `ERR_UNSUPPORTED -3`, `ERR_RUNTIME -4`, `ERR_STATE -5`,
`JET_RUNTIME_NOT_LINKED -10`. Non-negative = success.

| Function | Thread | Returns |
|---|---|---|
| `jet_bridge_abi_version()` | any | `JET_BRIDGE_ABI_VERSION` of the build (1) |
| `jet_bridge_version()` | any | static string |
| `jet_bridge_has_runtime()` | any | 0 / 1 |
| `jet_bridge_modules()` | any | `package\tactivity\tdexcrc\tlabel\n` per module, `""` if none |
| `jet_translate_apk(apk, bundle_dir, progress, ctx)` | background (blocking) | OK on module match, ERR_UNSUPPORTED otherwise, STUB from stub, ERR_INPUT / ERR_IO |
| `jet_runtime_main(bundle, activity, ui_view)` | main | OK, ERR_INPUT, ERR_UNSUPPORTED, ERR_RUNTIME, NOT_LINKED |
| `jet_runtime_touch(id, action, x, y)` | main | ignored in ABI v1 |
| `jet_runtime_ui_snapshot(buf, cap)` | main | bytes needed (excl. NUL), 0 if idle, NOT_LINKED |
| `jet_runtime_click(label)` | main | OK, ERR_INPUT (no button/listener), ERR_STATE, ERR_RUNTIME, NOT_LINKED |
| `jet_runtime_stop()` | main | idempotent |
| `jet_runtime_status()` | any | static, overwritten by next call — copy it |

Ownership: all returned strings are static storage owned by the library; `buf` is owned by the caller and always
NUL-terminated when `cap > 0`. `progress` is called synchronously on the caller's thread; `stage` lives only during
the call. Fatal runtime errors (`rt_fatal`) are caught with `setjmp/longjmp` via `rt_fatal_hook` and reported as
`ERR_RUNTIME`; the activity is discarded and `jet_runtime_main` may be called again.

## 4. Bundle layout (`bundle_dir/app.properties`)

Written by every `jet_translate_apk` that does not fail with ERR_INPUT/ERR_IO:

```
jet.bridge=aot-0.1+heading
jet.abi=1
jet.translated=true|false
apk=<file name>
dex.crc32=<8 hex, CRC-32 of classes.dex from the zip central directory>
dex.count=<number of classes*.dex>
# only when translated=true:
package=… activity=… label=… aot.module=…
```

Matching uses the CRC-32 stored in the APK's zip central directory for `classes.dex` (no decompression).

## 5. UI snapshot format

One node per line: `depth\tkind\tid-hex\tflags\ttext\n`; `kind` = `text` | `button`, `flags` = `-` | `s` (selected).
The host (`AppHostView` → `AotActivityView`) renders text rows and a button grid from it and forwards taps with
`jet_runtime_click(label)`.

## 6. Build and link recipe

```bash
# on a machine with the JET toolchain checkout (aot/, harness/, tools/soot) next to this repo
scripts/build_host_lib.sh --proto ../proto              # demo module (Heading Calculator, GPL, F-Droid)
scripts/build_host_lib.sh --proto ../proto --runtime-only
scripts/build_host_lib.sh --proto ../proto --app NAME --apk A.apk --jar A.jar --bundle DIR [--install]
```

Steps: AOT (JimpleToC) → `jet_module.c` metadata → Linux host `.a` + contract tests (stub and AOT) →
arm64-apple-ios `.a` (darwin archive; on Linux against `aot/runtime/hdr-min`, on macOS against the iPhoneOS SDK) →
check that every undefined symbol is libSystem and all 11 ABI symbols are exported → `dist/JetRuntime/`
(`lib/libjetruntime.a`, `include/JetBridge.h`, `manifest.json`) and `dist/JetRuntime-ios-arm64.tar.gz`.

Linking into the app:

* local: `scripts/build_host_lib.sh … --install` then `JET_RUNTIME=YES xcodegen generate`;
* CI: set repository variables `JET_RUNTIME_URL` (https) and `JET_RUNTIME_SHA256`; `scripts/fetch_runtime.sh`
  verifies the archive (sha256, path whitelist, manifest abi + library sha256, identical header) and exports
  `JET_RUNTIME=YES`. Without them CI builds the stub. A post-build step checks the binary contains `aot-0.1`
  (runtime) or `stub-0.2` (stub).

`project.runtime.yml` applies `JET_RUNTIME_LINKED=1` and `-force_load Vendor/JetRuntime/lib/libjetruntime.a` only
for `sdk=iphoneos*`; simulator builds keep the stub (the library has no simulator slice).

**Never commit** `Vendor/JetRuntime/`, `dist/`, or any APK. Modules may only be built from open-source APKs;
`build_host_lib.sh` refuses anything under `apks_closed/`, and closed APKs must never be redistributed.

## 7. Honest limits of ABI v1

* One AOT module per binary; only the Heading Calculator demo is proven end-to-end (contract test: clicks
  `1 2 7 DEL 5` → display `125`).
* No drawing into `ui_view`, no touch, no Canvas/GL: the host draws the snapshot. Apps using custom views,
  native libraries (`.so`), WebView, Compose or reflection-heavy frameworks are not supported by the AOT path yet.
* The iOS library was cross-compiled on Linux against `hdr-min`, not linked on a real device in this repo's CI yet;
  the first device link happens when `JET_RUNTIME_URL` is configured.
