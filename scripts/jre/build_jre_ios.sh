#!/bin/bash
# Build a no-JIT OpenJDK 21 (Zero interpreter + libffi) runtime for iOS arm64.
#   usage: build_jre_ios.sh <iphoneos|iphonesimulator> <outdir>
# Sources are fetched at build time, pinned by commit / sha256; nothing built here is committed.
#  - OpenJDK jdk21u (GPLv2+CPE)            pinned commit below
#  - iOS port patch set from PojavLauncherTeam/android-openjdk-build-multiarch (pinned commit + sha256)
#  - libffi (MIT)                          pinned release tarball + sha256
# Produces <outdir>/jre (java.home layout: lib/zero/libjvm.dylib, lib/*.dylib, lib/modules) and
# <outdir>/BUILDINFO.txt.  JIT is impossible on stock iOS; Zero is HotSpot's portable C++ interpreter.
set -euo pipefail
SDK=${1:?sdk}; OUT=${2:?outdir}
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
W=${JET_JRE_WORK:-$ROOT/build/jre-work-$SDK}
JDK_REPO=https://github.com/openjdk/jdk21u
JDK_COMMIT=060c4f7589e7f13febd402f4dac3320f4c032b08     # branch jdk21.0.1
POJAV_REPO=https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch
POJAV_COMMIT=f3936fba6fcf14f45202c5dd8d2d8179dde051b2
PATCH_SHA=23b3ab7ee3eda98529c15fbc6d7485bdd1a272f0bc634f92e39cf55aa0fbaa6d
FFI_VER=3.5.2
FFI_SHA=f3a3082a23b37c293a4fcd1053147b371f2ff91fa7ea1b2a52e335676bac82dc
CUPS_VER=2.2.4
MINOS=15.0
JRE_MODULES=${JRE_MODULES:-java.base,java.logging,java.xml,java.prefs,java.naming,java.sql,java.desktop,jdk.unsupported,jdk.zipfs,jdk.charsets,jdk.crypto.ec,jdk.localedata}

case "$SDK" in
  iphoneos)        TRIPLE=arm64-apple-ios$MINOS ;;
  iphonesimulator) TRIPLE=arm64-apple-ios$MINOS-simulator ;;
  *) echo "bad sdk $SDK"; exit 2 ;;
esac
SYSROOT=$(xcrun --sdk "$SDK" --show-sdk-path)
MACSYSROOT=$(xcrun --sdk macosx --show-sdk-path)
CLANG=$(xcrun --sdk "$SDK" -f clang); CLANGXX=$(xcrun --sdk "$SDK" -f clang++)
BOOT_JDK=${BOOT_JDK:-${JAVA_HOME:?need JDK 21 as JAVA_HOME}}
mkdir -p "$W/bin" "$OUT"; cd "$W"
log(){ echo "=== [$(date +%H:%M:%S)] $*"; }

# --- compiler wrappers (target) ---
cat > bin/ios-cc <<W
#!/bin/bash
exec "$CLANG" -target $TRIPLE -isysroot "$SYSROOT" "\$@"
W
cat > bin/ios-c++ <<W
#!/bin/bash
exec "$CLANGXX" -target $TRIPLE -isysroot "$SYSROOT" "\$@"
W
chmod +x bin/*

# --- libffi (static) ---
if [ ! -f ffi/lib/libffi.a ]; then
  log "libffi $FFI_VER for $TRIPLE"
  curl -fsSL -o ffi.tgz https://github.com/libffi/libffi/releases/download/v$FFI_VER/libffi-$FFI_VER.tar.gz
  echo "$FFI_SHA  ffi.tgz" | shasum -a 256 -c -
  rm -rf ffi-src && mkdir ffi-src && tar xzf ffi.tgz -C ffi-src --strip-components=1
  ( cd ffi-src && ./configure --host=aarch64-apple-darwin --prefix="$W/ffi" --disable-shared --enable-static \
      --disable-docs --disable-multi-os-directory CC="$W/bin/ios-cc" CXX="$W/bin/ios-c++" >../ffi-configure.log 2>&1 \
      || { tail -50 ../ffi-configure.log; exit 1; }
    make -j"$(sysctl -n hw.ncpu)" >../ffi-make.log 2>&1 || { tail -80 ../ffi-make.log; exit 1; }
    make install >/dev/null )
  rm -f ffi/lib/*.dylib
fi
lipo -info ffi/lib/libffi.a

# --- iOS patch set + missing headers (pinned) ---
if [ ! -d pojav/.git ]; then
  log "fetch iOS port patches @ $POJAV_COMMIT"
  git init -q pojav && git -C pojav fetch -q --depth 1 "$POJAV_REPO" "$POJAV_COMMIT" && git -C pojav checkout -q FETCH_HEAD
fi
[ "$(git -C pojav rev-parse HEAD)" = "$POJAV_COMMIT" ]
echo "$PATCH_SHA  pojav/patches/jre_21/ios/jdk21u_ios.diff" | shasum -a 256 -c -
INC=$W/inc; rm -rf "$INC"; cp -R pojav/ios-missing-include "$INC"
# cups headers (java.desktop printing stubs compile against them)
if [ ! -d "cups-$CUPS_VER" ]; then
  curl -fsSL -o cups.tgz https://github.com/apple/cups/releases/download/v$CUPS_VER/cups-$CUPS_VER-source.tar.gz
  tar xzf cups.tgz
fi
ln -sfn "$W/cups-$CUPS_VER/cups" "$INC/cups"
ln -sfn "$MACSYSROOT/System/Library/Frameworks/CoreAudio.framework/Headers" "$INC/CoreAudio"
ln -sfn "$MACSYSROOT/System/Library/Frameworks/IOKit.framework/Headers" "$INC/IOKit"
BREW=$(brew --prefix)
command -v autoconf >/dev/null || HOMEBREW_NO_AUTO_UPDATE=1 brew install autoconf >/dev/null
[ -d "$BREW/include/fontconfig" ] || HOMEBREW_NO_AUTO_UPDATE=1 brew install fontconfig >/dev/null
ln -sfn "$BREW/include/fontconfig" "$INC/fontconfig"
# X11 headers: only needed as include path for headless awt; take them from brew libx11/xorgproto
for f in libx11 xorgproto libxext libxrender libxtst libxi libxt libxrandr; do
  [ -d "$BREW/opt/$f" ] || NEED_X=1
done
[ -z "${NEED_X:-}" ] || HOMEBREW_NO_AUTO_UPDATE=1 brew install libx11 xorgproto libxext libxrender libxtst libxi libxt libxrandr >/dev/null
XINC=$W/xinc; rm -rf "$XINC"; mkdir -p "$XINC/X11"
for f in libx11 xorgproto libxext libxrender libxtst libxi libxt libxrandr; do
  cp -R "$BREW/opt/$f/include/X11/." "$XINC/X11/" 2>/dev/null || true
done

# --- OpenJDK source ---
if [ ! -d jdk/.git ]; then
  log "fetch jdk21u @ $JDK_COMMIT"
  git init -q jdk && git -C jdk fetch -q --depth 1 "$JDK_REPO" "$JDK_COMMIT" && git -C jdk checkout -q FETCH_HEAD
fi
cd jdk
git reset -q --hard "$JDK_COMMIT"; git clean -qfdx -e build
git apply --whitespace=nowarn ../pojav/patches/jre_21/ios/jdk21u_ios.diff
# same hack as the iOS port: don't build the AppKit-based macOS desktop code
mv src/java.desktop/macosx src/java.desktop/macosx_NOTIOS
mkdir -p src/java.desktop/macosx/native
mv src/java.desktop/macosx_NOTIOS/native/libjsound src/java.desktop/macosx/native/
# JET local patches (if any)
for p in "$ROOT"/scripts/jre/patches/*.diff; do [ -f "$p" ] && { log "apply $(basename "$p")"; git apply --whitespace=nowarn "$p"; }; done

FLAGS="-DHEADLESS=1 -I$INC -Wno-implicit-function-declaration -Wno-int-conversion -Wno-incompatible-function-pointer-types -DTARGET_OS_OSX -DLE_STANDALONE -O2"
export BUILD_SYSROOT_CFLAGS="-isysroot $MACSYSROOT"
export HOTSPOT_DISABLE_DTRACE_PROBES=1
log "configure ($SDK, zero)"
bash ./configure \
  --openjdk-target=aarch64-apple-ios \
  --with-toolchain-type=clang --with-sysroot="$SYSROOT" \
  --with-boot-jdk="$BOOT_JDK" --with-build-jdk="$BOOT_JDK" \
  --with-jvm-variants=zero --with-jvm-features=-dtrace \
  --with-libffi-include="$W/ffi/include" --with-libffi-lib="$W/ffi/lib" \
  --enable-headless-only=yes --with-freetype=bundled \
  --with-cups-include="$W/cups-$CUPS_VER" --with-fontconfig-include="$INC" \
  --x-includes="$XINC" --x-libraries=/usr/lib \
  --with-extra-cflags="$FLAGS" --with-extra-cxxflags="$FLAGS" --with-extra-ldflags="" \
  --disable-precompiled-headers --disable-warnings-as-errors \
  --with-native-debug-symbols=none --with-debug-level=release \
  --disable-cds-archive --disable-javac-server \
  CC="$W/bin/ios-cc" CXX="$W/bin/ios-c++" \
  BUILD_CC=/usr/bin/clang BUILD_CXX=/usr/bin/clang++ \
  > "$W/jdk-configure.log" 2>&1 || { tail -60 "$W/jdk-configure.log"; grep -n "error" build/*/config.log | tail -30 || true; exit 1; }
tail -40 "$W/jdk-configure.log"
BD=$(ls -d build/*/ | head -1)
log "make images ($BD)"
make -C "$BD" JOBS="$(sysctl -n hw.ncpu)" images LOG=warn > "$W/jdk-make.log" 2>&1 || {
  grep -n -E "error:|Error [0-9]|failed" "$W/jdk-make.log" | head -60; tail -80 "$W/jdk-make.log"; exit 1; }
log "jlink ($JRE_MODULES)"
rm -rf "$OUT/jre"
"$BOOT_JDK/bin/jlink" --module-path "$BD/images/jmods" --add-modules "$JRE_MODULES" \
  --output "$OUT/jre" --strip-debug --no-header-files --no-man-pages --compress=zip-6 \
  || { echo "jlink cross failed; copying full image"; cp -R "$BD/images/jdk" "$OUT/jre"; }
rm -rf "$OUT/jre/bin" "$OUT/jre/legal" "$OUT/jre/lib/src.zip"
find "$OUT/jre" -name '*.dSYM' -prune -exec rm -rf {} +
{ echo "JET JRE: OpenJDK 21 Zero (no JIT) for $TRIPLE"
  echo "jdk21u $JDK_COMMIT + PojavLauncher iOS patch $POJAV_COMMIT ($PATCH_SHA)"
  echo "libffi $FFI_VER ($FFI_SHA)"; echo "modules: $JRE_MODULES"
  echo "built $(date -u +%FT%TZ) on $(sw_vers -productVersion) / $(xcodebuild -version | head -1)"
  du -sh "$OUT/jre"; find "$OUT/jre" -name '*.dylib' | sort; } > "$OUT/BUILDINFO.txt"
for d in $(find "$OUT/jre" -name '*.dylib'); do vtool -show-build "$d" | grep -m1 platform || true; done | sort | uniq -c
cat "$OUT/BUILDINFO.txt"
