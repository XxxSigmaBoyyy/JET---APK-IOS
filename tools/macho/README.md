# tools/macho — ELF analysis helpers

`elf2macho.py` remains as a **dev/analysis** tool (inspect APK `.so` layout, imports, page-share).
It is **not** the runtime delivery path.

## Runtime native path (in-app, no re-sign)
iOS forbids creating new RX pages at runtime inside a signed app. JET therefore does **not**
dlopen translated dylibs. Stock APK `lib/arm64-v8a/*.so` are loaded by a generic ELF loader and
executed by an in-process **no-JIT arm64 user-mode interpreter** (`Runtime/interp/`), with:

- bionic/JNI calls trapped to host-native implementations (`Runtime/bionic/` as call-out table)
- decoded-block translation cache (interpreted chains, not codegen)
- host fast-paths for common libc/libm/GL/JNI

No Export-to-E-SIGN / re-sign flow. Everything runs inside the single signed JET.app.
