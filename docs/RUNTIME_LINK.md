# JetBridge C ABI v2 — JET runtime on iOS

`JET/Bridge/JetBridge.h` is the contract between the SwiftUI shell and the JET runtime. This document and the header
must stay in sync.

## 1. What runs on the phone

iOS forbids JIT compilation and loading executable code at run time, so JET runs Android apps **ahead-of-time
compiled**:

```
APK (open source) --dex2jar--> classes jar --Soot Jimple--> jet.aot.JimpleToC --> Modules/<name>/<name>_app.c
                  --JET bundle (resources)--> tools/aot/gen_res.py               --> Modules/<name>/<name>_res.c
                  classes.dex CRC-32                                              --> Modules/<name>/jet_module.c
```

`tools/aot/gen_module.sh` runs the whole chain. Xcode compiles `Runtime/*.c` and `Modules/*/*.c` straight into
JET.app (`project.yml`); no prebuilt library is involved.

| file | role |
|---|---|
| `Runtime/jetrt.h`, `Runtime/rt.c` | object model (`JObj`, `JArr`, `jv`), class registry, virtual dispatch by sub-signature, exceptions (`rt_exc`), strings |
| `Runtime/kernel.c` | java.lang core (String, StringBuilder, Integer, Math, Enum, Class.forName …) and the **native Android view engine**: LayoutInflater over the generated layout tables, View/ViewGroup/LinearLayout/TableLayout/TableRow/TextView/Button, AOSP measure/layout (MeasureSpec, weights, column stretch/shrink), state-list drawables, Toast, Bundle, Activity; display-list generation and touch handling |
| `Runtime/jet_bridge_aot.c` | this ABI on top of the runtime |
| `Modules/<name>/` | one AOT app module (generated C + NOTICE with licence and source) |

At import, `jet_translate_apk` reads the APK's `classes.dex` CRC-32 from the zip central directory and matches it
against the compiled-in module. A match starts that module's code; any other APK is kept but reported as
`ERR_UNSUPPORTED` (shown as "No prebuilt module"). Running arbitrary APKs needs a DEX interpreter (future
milestone). Only open-source apps whose licence permits redistribution may be compiled in; APKs themselves are never
committed (`scripts/guard_repo.sh` enforces this in CI).

## 2. Functions

| function | thread | returns |
|---|---|---|
| `jet_bridge_abi_version()` | any | `2` |
| `jet_bridge_version()` | any | `"aot-0.2+<module>"` |
| `jet_bridge_has_runtime()` | any | `1` |
| `jet_bridge_modules()` | any | `"package\tactivity\tdexcrc\tlabel\n"` per module |
| `jet_translate_apk(apk, bundle_dir, progress, ctx)` | background | `OK` (module matched), `ERR_UNSUPPORTED`, `ERR_INPUT`, `ERR_IO` |
| `jet_runtime_set_text_measurer(fn)` | main | — host font metrics (width, ascent, descent in px) |
| `jet_runtime_set_viewport(w_px, h_px, density)` | main | — density = px per dip (`UIScreen.scale`) |
| `jet_runtime_main(bundle, activity, NULL)` | main | `OK`, `ERR_INPUT`, `ERR_UNSUPPORTED`, `ERR_RUNTIME` |
| `jet_runtime_frame(&ops, &bg)` | main | number of `JetDrawOp`s (valid until the next runtime call) |
| `jet_runtime_touch(pointer, action, x, y)` | main | `1` redraw / `0` / `ERR_STATE` / `ERR_RUNTIME` |
| `jet_runtime_next_wakeup_ms()` | main | ms until a timed redraw (Toast expiry) or `-1` |
| `jet_runtime_find_label(label, &x, &y)` | main | automation: centre of a clickable view by its text |
| `jet_runtime_ui_snapshot(buf, cap)` | main | v1 text dump of the view tree (debug/tests) |
| `jet_runtime_click(label)` | main | v1 direct click by label (tests) |
| `jet_runtime_stop()` | main | — |
| `jet_runtime_status()` | main | human-readable status of the last call |

Touch actions: `0` down (hit-test the deepest enabled clickable / long-clickable view, set pressed), `1` up (click
when still pressed and no long click consumed it), `2` move (leaving the view + 8 dip slop cancels pressed), `3`
cancel, `4` long-press timeout (host timer, 500 ms after down: `onLongClick`; returning true suppresses the click).
App listeners run synchronously inside these calls. An uncaught Java exception or a fatal runtime error stops the
app (`ERR_RUNTIME`, message in `jet_runtime_status()`), like Android's "app has stopped".

## 3. Display list

`JetDrawOp { op, flags, gravity, x, y, w, h, clip_x, clip_y, clip_w, clip_h, size, argb, text }`, in pixels, origin
top-left, drawn in order, each clipped to its clip rect. `op`: `1` filled rect, `2` text in box (font size `size` px,
`flags & 1` bold, Android `gravity` bits: horizontal 3/1/5, vertical 0x30/0x10/0x50), `3` rounded rect (radius
`size`). The window background colour comes from the app theme's `windowBackground`. `JetSurfaceView` draws it with
CoreGraphics and the system font; the text measurer uses the same font so layout and drawing agree.

## 4. Bundle

`bundle_dir/app.properties` written by `jet_translate_apk`: `jet.bridge`, `jet.abi=2`, `jet.translated=true|false`,
`apk`, `dex.crc32`, `dex.count`, and on a match `package`, `activity`, `label`, `aot.module`.

## 5. Tests

* `scripts/test_runtime.sh` — contract test (`Runtime/contract_test.c`) and render/touch test
  (`Runtime/test/frame_test.c` + `render_ops.py` → PNG) on Linux or macOS with clang.
* CI `simulator-smoke` job — builds for the iOS Simulator, imports the F-Droid APK through the real import pipeline,
  opens it and taps through `JET/Debug/SmokeTest.swift` (`-JETSmokeAPK`, `-JETSmokeTaps`), uploading screenshots.
