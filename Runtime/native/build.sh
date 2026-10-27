#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
CC="${CC:-clang}"
CFLAGS="-O2 -g -std=c11 -Wall -Wextra -Wno-unused-parameter -Wno-unused-function -fno-strict-aliasing -D_GNU_SOURCE -I. -Ithird_party"
LDFLAGS="-lpthread -ldl -lm -Lthird_party -lz"
mkdir -p build

SRCS="ctx.c decode.c asimd.c elf_loader.c interp.c host_bionic.c jni_bridge.c trampoline.c setjmp_guest.c"
OBJS=""
for s in $SRCS; do
  o="build/${s%.c}.o"
  echo "CC $s"
  $CC $CFLAGS -c "$s" -o "$o"
  OBJS="$OBJS $o"
done
echo "AR libjet_native.a"
ar rcs build/libjet_native.a $OBJS

echo "CC guest_lib.so"
$CC --target=aarch64-linux-gnu -nostdlib -shared -fPIC -O0 \
    -o tests/guest_lib.so tests/guest_lib.c

echo "CC test_unit"
$CC $CFLAGS tests/test_unit.c -I. build/libjet_native.a $LDFLAGS -o build/test_unit

echo "CC test_tg"
$CC $CFLAGS tests/test_tg.c -I. build/libjet_native.a $LDFLAGS -o build/test_tg

echo "BUILD OK"
