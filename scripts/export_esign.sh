#!/bin/bash
# Package translated APK natives into a JET plugin tipa for on-device E-SIGN re-signing.
# Usage: export_esign.sh <apk> <outdir> [--platform ios|iossim]
# No app-specific code: every .so under lib/arm64-v8a is converted; Info.plist is generic.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
APK=${1:?apk}; OUT=${2:?outdir}; shift 2 || true
PLATFORM=ios
while [ $# -gt 0 ]; do case $1 in --platform) PLATFORM=$2; shift 2;; *) shift;; esac; done
command -v python3 >/dev/null
E2M="$ROOT/tools/macho/elf2macho.py"
ABI="$ROOT/Runtime/bionic/abi_list.flat"
[ -f "$E2M" ] && [ -f "$ABI" ] || { echo "missing elf2macho/abi_list"; exit 1; }

WORK=$(mktemp -d); trap 'rm -rf "$WORK"' EXIT
unzip -q -d "$WORK/apk" "$APK" 'lib/arm64-v8a/*' 'META-INF/*' 'AndroidManifest.xml' 2>/dev/null || true
SO_DIR="$WORK/apk/lib/arm64-v8a"
[ -d "$SO_DIR" ] || { echo "no lib/arm64-v8a in APK"; exit 2; }

# package name from binary XML is hard; use APK basename as plugin id (generic)
PKG=$(basename "$APK" .apk | tr -cd 'A-Za-z0-9._-')
APP="$OUT/Payload/JETPlugin-$PKG.app"
rm -rf "$OUT"; mkdir -p "$APP/Frameworks"

n=0
for so in "$SO_DIR"/*.so; do
  [ -f "$so" ] || continue
  base=$(basename "$so" .so)
  echo "[esign] elf2macho $base"
  if ! python3 "$E2M" --platform "$PLATFORM" --abi-list "$ABI" \
      --bridge '@rpath/libjetbionic.dylib' \
      --install-name "@rpath/${base}.dylib" \
      "$so" "$APP/Frameworks/${base}.dylib"; then
    echo "[esign] REFUSED $base (see elf2macho); left for interpreter fallback"
    mkdir -p "$APP/elf"; cp "$so" "$APP/elf/"; continue
  fi
  n=$((n+1))
done
# Placeholder: caller copies a prebuilt libjetbionic.dylib into Frameworks/
# Info.plist minimal
cat > "$APP/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleIdentifier</key><string>dev.jet.plugin.$PKG</string>
  <key>CFBundleName</key><string>JETPlugin-$PKG</string>
  <key>CFBundleVersion</key><string>1</string>
  <key>CFBundleShortVersionString</key><string>1.0</string>
  <key>CFBundleExecutable</key><string>placeholder</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>MinimumOSVersion</key><string>15.0</string>
  <key>JETPluginAPK</key><string>$PKG</string>
  <key>JETPluginDylibCount</key><integer>$n</integer>
</dict></plist>
PLIST
# empty executable so codesign has something; E-SIGN replaces signature
echo -n 'JET' > "$APP/placeholder"
( cd "$OUT" && zip -qr "$OUT/JETPlugin-$PKG.tipa" Payload )
echo "[esign] wrote $OUT/JETPlugin-$PKG.tipa ($n dylibs) — open in E-SIGN to re-sign, then import in JET"
