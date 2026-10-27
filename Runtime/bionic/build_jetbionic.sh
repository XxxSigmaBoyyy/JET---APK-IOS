#!/bin/bash
# Build libjetbionic.dylib for macos (fast dlopen loop) or iphoneos/iphonesimulator.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
SDK=${1:?usage: build_jetbionic.sh macos|iphoneos|iphonesimulator [outdir]}
OUT=${2:-"$ROOT/build/bionic-$SDK"}
mkdir -p "$OUT"
case $SDK in
  macos) TRIPLE=arm64-apple-macos11; SYSROOT=$(xcrun --sdk macosx --show-sdk-path); MIN=-mmacosx-version-min=11.0; FRAMEWORKS="-framework OpenGL";;
  iphoneos) TRIPLE=arm64-apple-ios15.0; SYSROOT=$(xcrun --sdk iphoneos --show-sdk-path); MIN=-miphoneos-version-min=15.0; FRAMEWORKS="-framework OpenGLES -framework UIKit";;
  iphonesimulator) TRIPLE=arm64-apple-ios15.0-simulator; SYSROOT=$(xcrun --sdk iphonesimulator --show-sdk-path); MIN=-miphonesimulator-version-min=15.0; FRAMEWORKS="-framework OpenGLES -framework UIKit";;
  *) echo bad sdk; exit 2;;
esac
CC=$(xcrun --find clang)
CFLAGS="-target $TRIPLE -isysroot $SYSROOT $MIN -fPIC -O2 -Wall -I$(dirname "$0") -DJET_BIONIC=1"
SRC=$(dirname "$0")
OBJS=()
for f in "$SRC"/jb_*.c; do
  o="$OUT/$(basename "$f" .c).o"
  $CC $CFLAGS -c "$f" -o "$o"
  OBJS+=("$o")
done
if [ -f "$SRC/jb_va.S" ]; then
  $CC $CFLAGS -c "$SRC/jb_va.S" -o "$OUT/jb_va.o"
  OBJS+=("$OUT/jb_va.o")
fi
# Re-export ABI-identical Darwin symbols via a .exp + -alias or just leave them to libSystem
# (translated .so binds to libjetbionic; for identical names we re-export from libSystem).
REEXPORTS="$OUT/reexport.exp"
grep -v '^#' "$SRC/jb_exports.tbd.txt" | tr ' ' '\n' | grep -v '_NOT' | grep -v '^$' | sed 's/^/_/' | sort -u > "$REEXPORTS"
$CC $CFLAGS -dynamiclib -o "$OUT/libjetbionic.dylib" "${OBJS[@]}" \
  -install_name @rpath/libjetbionic.dylib \
  -Wl,-reexported_symbols_list,"$REEXPORTS" \
  -lz $FRAMEWORKS -lc++ \
  2>"$OUT/link.err" || { echo "link failed:"; cat "$OUT/link.err"; exit 1; }
echo "built $OUT/libjetbionic.dylib ($(wc -c < "$OUT/libjetbionic.dylib") B)"
cp "$SRC/abi_list.flat" "$OUT/abi_list.txt"
