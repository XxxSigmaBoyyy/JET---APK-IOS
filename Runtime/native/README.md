# JET native — AArch64 no-JIT user-mode interpreter

Embeddable portable C11 library that loads stock Android APK `.so` files (ELF ET_DYN,
AArch64) and runs them by interpretation. Designed to sit under an iOS app as
`Runtime/native/` without shipping a JIT.

## Layout

| Path | Role |
|------|------|
| `jet_native.h` | Public API |
| `jet_internal.h` | CPU / trap / ELF / block-cache internals |
| `ctx.c` | Context create/destroy, call, trap table |
| `elf_loader.c` | ELF load, RELA/RELR/APS2/TLS, init_array |
| `decode.c` / `interp.c` | AArch64 decode + interpreter + block cache |
| `host_bionic.c` | libc/libm/liblog/bionic/JNI-adjacent host shims |
| `jni_bridge.c` | Guest JNIEnv/JavaVM trap tables, RegisterNatives |
| `trampoline.c` | Host→guest trampoline pool (pthread/qsort/…) |
| `setjmp_guest.c` | In-guest setjmp/longjmp |
| `tests/` | `guest_lib.so` unit tests + Telegram acceptance |

## Build

```bash
./build.sh          # preferred (no system `make` / zlib-dev required)
# artifacts in build/: libjet_native.a, test_unit, test_tg, tests/guest_lib.so
```

Requires: `clang`, `python3`. Optional: `aarch64-linux-gnu-gcc` only if you rebuild the
guest test SO for arm64; a prebuilt `tests/guest_lib.so` is produced by `build.sh` via
the host clang `-target aarch64-linux-gnu` when available, else a checked-in flow.

## Public API (summary)

- `jet_native_create` / `jet_native_destroy`
- `jet_native_load` / `jet_native_sym` / `jet_native_call` / `jet_native_call_ex`
- `jet_native_register_host` / `_ex` / `jet_native_register_data`
- `jet_jni_onload` / `jet_jni_on_register_natives` / `jet_jni_bind`
- Host→guest: trampoline pool used by `pthread_create`, `qsort`, `bsearch`, `atexit`
- Guest `setjmp`/`longjmp` (and sig* variants) save/restore guest CPU state
- `printf`-family reads Linux AAPCS64 `va_list` (`__stack/__gr_top/__vr_top/__gr_offs/__vr_offs`)

Guest addresses are identity-mapped (guest VA == host pointer). Guest memory is RW,
never `PROT_EXEC`. Imports go through a host trap/stub page.

## Tests

```bash
./build/test_unit   # hand-made arm64 guest_lib.so — must print ALL PASS
./build/test_tg [path/to/libtmessages.49.so]
```

Default Telegram SO path (workspace harness):
`/workspace/jet/proto/harness/out/tgfoss_p33n/natives/arm64-v8a/libtmessages.49.so`

## Status (acceptance)

| Check | Result |
|-------|--------|
| Load `libtmessages.49.so` (~54MB) | OK |
| `init_array` | First ctor soft-fails (NULL deref in guest); load still returns the lib |
| `JNI_OnLoad` | Returns `0x10006` (JNI_VERSION_1_6); 64 `RegisterNatives` captured |
| Call ≥2 Java_* methods | `Intro_setScrollOffset`, `Utilities_pinBitmap` (and more) |
| Unit tests | ALL PASS (~40–140 MIPS est. on host x86_64) |
| `jet_native_sym` on dynsym `Java_*` | OK |

`RegisterNatives` dump: `build/register_natives.txt` (produced by `test_tg`).

### Known gaps

- First `.init_array` ctor faults (guest NULL); remaining ctors skipped
- Late `JNI_OnLoad` helper (tgvoip registration tail) hits `RET` with LR=0 (stack/epilogue
  edge); soft-ok path still reports version when natives were captured
- ASIMD/FP coverage incomplete (many three-same / load-store multiple forms missing)
- Unresolved imports auto-bind to `stub0` (return 0) — enough to proceed, not complete
- zlib is a local stub (`zlib_stub.h`), not full deflate
- No iOS device run here; code is portable C11 toward ios-arm64 (avoid asm/JIT)

### HWCAP

Advertises `FP|ASIMD` only (no AES/SHA/LSE) so guest code stays on interpreted paths.

## Copy into ios-repo

Copy this tree to `Runtime/native/` of the iOS app. Do **not** commit APKs or `.so`
binaries into that folder; load them at runtime from the app sandbox.
