# libjetbionic — generic Android arm64 (bionic) → Darwin bridge

No app-specific code. Translates the ELF→Mach-O converted APK `.so` imports to Darwin.

## Layout
- `jb_va.S` — AAPCS64 variadic trampolines → `jbv_*` marshallers
- `jb_fmt.c` — format-driven Darwin va_list builder (printf/scanf/log families)
- `jb_errno.c` / `jb_stdio.c` — errno translate, `__sF` → stdin/out/err
- `jb_posix.c` — stat/open/mmap/clock/getauxval/dl_iterate_phdr/…
- `jb_pthread.c` — size-mismatched pthread_* via lazy heap objects
- `jb_epoll.c` — epoll → kqueue, eventfd → pipe
- `abi_list.txt` — symbols that become `jb_<name>` in the converted dylib
- `jb_exports.tbd.txt` — ABI-identical symbols re-exported from libSystem

## Build
```
./build_jetbionic.sh macos|iphoneos|iphonesimulator [outdir]
```

## Use with elf2macho
```
python3 tools/macho/elf2macho.py --platform ios --abi-list Runtime/bionic/abi_list.flat \\
  libfoo.so libfoo.dylib
```
