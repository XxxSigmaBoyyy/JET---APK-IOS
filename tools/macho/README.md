# ELF arm64 → Mach-O arm64 dylib (Pass 30 iOS packaging prototype)

## What works here (Linux host, no Xcode)

`elf2macho.py` rewraps a final arm64 Android `.so` as an `MH_DYLIB` without touching machine code
(ELF vaddrs preserved → PC-relative ADRP/ADR/BL stay valid):

| ELF | Mach-O |
|---|---|
| PT_LOAD R-X | `__TEXT` (r-x); Mach header overwrites unused ELF ehdr area |
| PT_LOAD RW | `__DATA` (rw-); bss via vmsize > filesize |
| R_AARCH64_RELATIVE (after `unpack_relocs.py`) | addend written into slot + rebase opcodes |
| GLOB_DAT / JUMP_SLOT / ABS64 vs import | bind opcodes (ordinal 1 = `@rpath/libjetbionic.dylib`) |
| DT_INIT_ARRAY / DT_FINI_ARRAY | `__DATA,__mod_init_func` / `__mod_term_func` |
| exported dynsyms | export trie + nlist (leading `_`, Darwin C) |
| symbol versioning | dropped (Mach-O has none) |

Verified with `llvm-otool-19` / `llvm-nm-19` and by linking a client with `ld64.lld-19 -arch arm64`
against the converted dylib + a stub `libjetbionic.dylib` (also built here with
`clang-19 -target arm64-apple-ios` + `ld64.lld-19`).

## Refused (out of scope for this prototype)

- **TLS** (PT_TLS / TLSDESC / TPREL / DTPMOD) — none of the surveyed app libs use it
- **IRELATIVE / IFUNC** — runtime codegen, banned on iOS
- **TEXTREL** — W^X-hostile

## WARN (emitted, needs a real packaging step)

Most Android NDK libs are `p_align=0x1000`. On arm64 Darwin the page size is 16 KiB, so the last
RX page and first RW page of a 4K-aligned image often share one 16K page. dyld cannot W^X-split
that. A real iOS package must **re-align or move the RW segment** (insert padding so `__DATA`
starts on a 16K boundary). The converter warns and still emits; do not ship as-is.

## What needs macOS / Xcode

1. **Code signing** — on-device dyld rejects unsigned / ad-hoc-unsigned-without-entitlements images.
   Needs `codesign` (and typically an Apple developer identity + provisioning).
2. **Real bionic shim as a Darwin dylib** — `libjetbionic.dylib` here is a symbol stub. The
   production shim is our `tools/bionic-stubs` sources built with the iOS SDK against
   `libSystem.B.dylib` (or a `.tbd`), covering the j_* ABI shims, AAsset*, ANativeWindow, EGL/GLES
   over Metal/ANGLE, etc. Building that needs the macOS SDK headers / `.tbd` stubs.
3. **Chained fixups** (`LC_DYLD_CHAINED_FIXUPS`) — modern iOS prefers them over classic
   `LC_DYLD_INFO`; our output uses classic rebase/bind which still loads on recent dyld but is
   not the preferred format.
4. **On-device / Simulator `dlopen` smoke test** — cannot be done from this Linux box; needs an
   arm64 Darwin host (Mac + Xcode Simulator, or a device).
5. **App-bundle packaging** — embed converted dylibs in `Foo.app/Frameworks/`, set
   `@rpath` / `LC_RPATH`, list them in the signed bundle.

## Usage

```bash
tools/macho/.venv/bin/python tools/macho/elf2macho.py \
  path/to/arm64.so tools/macho/out/libfoo.dylib
llvm-otool-19 -hv tools/macho/out/libfoo.dylib
```

## 16 KiB page split (generic)
NDK images are 4 KiB-aligned, arm64 Darwin pages are 16 KiB. The converter keeps every vaddr and puts the
__TEXT/__DATA boundary at the 16 KiB floor of the first RW byte. If any SHF_EXECINSTR section would land in that
RW page the library is refused (route b2: arm64 interpreter). File-less tail becomes a S_ZEROFILL __bss section.
Verified on stock Telegram FOSS libtmessages.49.so: __DATA @0x3334000, 112261 rebases, 413 imports, 340 Java_* exports.

## Export to E-SIGN (b1) — design
iOS will not execute newly-written RX pages. Translated APK `.so` → `.dylib` must be signed with the
user's cert before `dlopen`. Flow (all generic, APK-derived):

1. **Import APK** in JET (or a desktop/CI helper): unzip `lib/arm64-v8a/*.so`.
2. **elf2macho** each `.so` with `--platform ios --abi-list Runtime/bionic/abi_list.flat`.
3. **Package** a thin plugin IPA / tipa: `Payload/JETPlugin-<pkg>.app/` containing
   `Frameworks/libjetbionic.dylib` + `Frameworks/<translated>.dylib` + `Info.plist` (generic).
4. **User re-signs** with E-SIGN (on-device, their cert) — same as any sideloaded tipa.
5. **JET loads** the signed plugin via `dlopen` of the translated dylibs (rpath → plugin Frameworks).

Fallback **b2** (no re-sign): arm64 user-mode interpreter executes the original ELF `.so` text
in softmmu (no JIT / no RX alloc). Same bionic bridge called through a FFI stub.

Cold-start timing: wall ms from APK open → first Activity frame callback, logged to timing.txt.
