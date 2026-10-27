#!/bin/bash
# Stage the CI-built JRE + JNI headers + JET's Java code for an Xcode build (all under Vendor/, never committed).
#   stage_jvm.sh <iphoneos|iphonesimulator> <jre21-zero-SDK.tar.xz>
set -euo pipefail
SDK=$1; TAR=$2
ROOT=$(cd "$(dirname "$0")/.." && pwd); V=$ROOT/Vendor
: "${JAVA_HOME:?JDK 21 needed for javac + jni.h}"
rm -rf "$V/jre-$SDK" "$V/jni" "$V/jvm-app"; mkdir -p "$V/jre-$SDK" "$V/jni" "$V/jvm-app"
tar -C "$V/jre-$SDK" -xJf "$TAR"
# Fonts for Java2D text (DejaVu 2.37, free license, fetched + sha-checked, never committed). Without a
# lib/fontconfig.properties the iOS JRE's FontConfiguration throws "Fontconfig head is null" on first text.
DJV_SHA=7576310b219e04159d35ff61dd4a4ec4cdba4f35c00e002a136f00e96a908b0a
DJVZ="$V/dejavu-fonts-ttf-2.37.zip"
[[ -f "$DJVZ" ]] || curl -fsSL -o "$DJVZ" "https://github.com/dejavu-fonts/dejavu-fonts/releases/download/version_2_37/dejavu-fonts-ttf-2.37.zip"
echo "$DJV_SHA  $DJVZ" | shasum -a 256 -c - >/dev/null || { echo "dejavu-fonts-ttf-2.37.zip sha256 mismatch"; exit 1; }
JLIB="$V/jre-$SDK/jre/lib"; mkdir -p "$JLIB/fonts"
for f in DejaVuSans DejaVuSans-Bold DejaVuSans-Oblique DejaVuSans-BoldOblique DejaVuSerif DejaVuSerif-Bold DejaVuSerif-Italic DejaVuSerif-BoldItalic \
         DejaVuSansMono DejaVuSansMono-Bold DejaVuSansMono-Oblique DejaVuSansMono-BoldOblique; do
  unzip -qjo "$DJVZ" "dejavu-fonts-ttf-2.37/ttf/$f.ttf" -d "$JLIB/fonts"
done
unzip -qjo "$DJVZ" "dejavu-fonts-ttf-2.37/LICENSE" -d "$JLIB/fonts" && mv "$JLIB/fonts/LICENSE" "$JLIB/fonts/LICENSE-DejaVu"
if [[ ! -f "$JLIB/fontconfig.properties" && ! -f "$JLIB/fontconfig.bfc" ]]; then
  {
    echo "version=1"
    for st in plain:"":"" bold:" Bold":"-Bold" italic:" Oblique":"-Oblique" bolditalic:" Bold Oblique":"-BoldOblique"; do
      IFS=: read -r s sfx fsfx <<<"$st"
      ssfx=${sfx/Oblique/Italic}
      echo "serif.$s.latin-1=DejaVu Serif$ssfx"
      for l in sansserif dialog; do echo "$l.$s.latin-1=DejaVu Sans$sfx"; done
      for l in monospaced dialoginput; do echo "$l.$s.latin-1=DejaVu Sans Mono$sfx"; done
    done
    echo "sequence.allfonts=latin-1"
    for n in "DejaVu Sans" "DejaVu Sans Mono"; do
      b=${n// /}
      for v in "":"" " Bold":"-Bold" " Oblique":"-Oblique" " Bold Oblique":"-BoldOblique"; do
        IFS=: read -r nsfx fsfx <<<"$v"; k="$n$nsfx"; echo "filename.${k// /_}=\$JRE_LIB_FONTS/$b$fsfx.ttf"
      done
    done
    for v in "":"" " Bold":"-Bold" " Italic":"-Italic" " Bold Italic":"-BoldItalic"; do
      IFS=: read -r nsfx fsfx <<<"$v"; k="DejaVu Serif$nsfx"; echo "filename.${k// /_}=\$JRE_LIB_FONTS/DejaVuSerif$fsfx.ttf"
    done
  } > "$JLIB/fontconfig.properties.in"
fi
cp "$JAVA_HOME/include/jni.h" "$JAVA_HOME/include/darwin/jni_md.h" "$V/jni/"

ASM="$ROOT/tools/asm/asm-9.7.jar:$ROOT/tools/asm/asm-tree-9.7.jar"
# dex2jar v2.4 (Apache-2.0) runtime pieces for the per-class DEX->JVM loader (jet.dex.DexClassLoader); never committed.
D2J_SHA=ee7c45eb3c1d2474a6145d8d447e651a736a22d9664b6d3d3be5a5a817dda23a
D2J_JARS="dex-reader-v2.4.jar dex-reader-api-v2.4.jar dex-ir-v2.4.jar dex-translator-v2.4.jar asm-commons-9.5.jar asm-analysis-9.5.jar"
D2JZ="$ROOT/Vendor/dex-tools-v2.4.zip"; mkdir -p "$ROOT/Vendor/d2j"
if [[ ! -f "$D2JZ" ]]; then
  if [[ -f /workspace/jet/proto/tools/dex2jar.zip ]]; then cp /workspace/jet/proto/tools/dex2jar.zip "$D2JZ"
  else echo "fetching dex-tools-v2.4..."; curl -fsSL -o "$D2JZ" "https://github.com/pxb1988/dex2jar/releases/download/v2.4/dex-tools-v2.4.zip"; fi
fi
echo "$D2J_SHA  $D2JZ" | shasum -a 256 -c - >/dev/null || { echo "dex-tools-v2.4.zip sha256 mismatch"; exit 1; }
for j in $D2J_JARS; do unzip -qjo "$D2JZ" "dex-tools-v2.4/lib/$j" -d "$ROOT/Vendor/d2j"; done
D2J=""; for j in $D2J_JARS; do D2J="$D2J:$ROOT/Vendor/d2j/$j"; done; D2J=${D2J#:}
W=$(mktemp -d)
# 1) nativeapi (+ ASM for JetAppClassLoader) and hello
mkdir -p "$W/api"
"$JAVA_HOME/bin/javac" --release 21 -cp "$ASM" -d "$W/api" \
  $(find "$ROOT/Java/hello" "$ROOT/Java/nativeapi" -name '*.java' 2>/dev/null)
"$JAVA_HOME/bin/jar" cf "$V/jvm-app/jethello.jar" -C "$W/api" .
# Pack ASM into jvm-app so JetAppClassLoader works at runtime
cp "$ROOT/tools/asm/asm-9.7.jar" "$ROOT/tools/asm/asm-tree-9.7.jar" "$V/jvm-app/"

# 2) Android shim (optional if sources present). Compile against android.jar stubs if available.
SHIM_SRC="$ROOT/Java/shim"
if [[ -d "$SHIM_SRC" ]]; then
  AJAR=""
  for c in "$ROOT/tools/android/android-33.jar" "$ROOT/Vendor/android-33.jar"; do
    [[ -f "$c" ]] && AJAR=$c && break
  done
  # Fetch platform stub jar once into Vendor if missing (not committed).
  if [[ -z "$AJAR" ]]; then
    mkdir -p "$ROOT/Vendor"
    if [[ -f /workspace/jet/proto/tools/soot/platforms/android-33/android.jar ]]; then
      cp /workspace/jet/proto/tools/soot/platforms/android-33/android.jar "$ROOT/Vendor/android-33.jar"
      AJAR="$ROOT/Vendor/android-33.jar"
    else
      # CI: fetch Android 33 platform android.jar (compile-only stubs; not shipped in the IPA).
      Z="$ROOT/Vendor/platform-33.zip"
      echo "fetching Android platform-33 android.jar..."
      curl -fsSL -o "$Z" "https://dl.google.com/android/repository/platform-33_r02.zip"
      unzip -qo "$Z" "android-13/android.jar" -d "$ROOT/Vendor/platform-33"
      # zip layout varies; find android.jar
      AJAR=$(find "$ROOT/Vendor/platform-33" -name android.jar | head -1)
      if [[ -n "$AJAR" ]]; then cp "$AJAR" "$ROOT/Vendor/android-33.jar"; AJAR="$ROOT/Vendor/android-33.jar"; fi
      rm -f "$Z"
    fi
  fi
  # sqlite-jdbc (Maven Central) if still missing
  if [[ -z "${SQLITE:-}" ]]; then :; fi
  mkdir -p "$W/shim"
  # Optional SQLite JDBC (android.database.sqlite shim)
  SQLITE=""
  for c in "$ROOT/tools/android/sqlite-jdbc.jar" "$ROOT/Vendor/sqlite-jdbc.jar"; do
    [[ -f "$c" ]] && SQLITE=$c && break
  done
  if [[ -z "$SQLITE" && -f /workspace/jet/proto/tools/sqlite-jdbc.jar ]]; then
    cp /workspace/jet/proto/tools/sqlite-jdbc.jar "$ROOT/Vendor/sqlite-jdbc.jar"
    SQLITE="$ROOT/Vendor/sqlite-jdbc.jar"
  fi
  if [[ -z "$SQLITE" ]]; then
    echo "fetching sqlite-jdbc..."
    curl -fsSL -o "$ROOT/Vendor/sqlite-jdbc.jar"       "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar"
    SQLITE="$ROOT/Vendor/sqlite-jdbc.jar"
  fi
  CP="$ASM:$D2J"
  [[ -n "$AJAR" ]] && CP="$AJAR:$CP"
  [[ -n "$SQLITE" ]] && CP="$SQLITE:$CP"
  set +e
  "$JAVA_HOME/bin/javac" -nowarn --release 21 -cp "$CP" -d "$W/shim" \
    $(find "$SHIM_SRC" -name '*.java' ! -name 'JetSkia*.java' ! -name 'SkiaInit.java') >"$W/shim-javac.log" 2>&1
  RC=$?
  set -e
  if [[ $RC -eq 0 ]]; then
    # non-Java resources of the shim (e.g. jet/apk/fwattrs.properties) go into the jar next to the classes
    (cd "$SHIM_SRC" && find . -type f ! -name '*.java' -exec sh -c 'mkdir -p "$0/$(dirname "$1")" && cp "$1" "$0/$1"' "$W/shim" {} \;)
    "$JAVA_HOME/bin/jar" cf "$V/jvm-app/jetshim.jar" -C "$W/shim" .
    for j in $D2J_JARS; do cp "$ROOT/Vendor/d2j/$j" "$V/jvm-app/"; done
    [[ -n "$SQLITE" ]] && cp "$SQLITE" "$V/jvm-app/"
    echo "shim: OK ($(find "$W/shim" -name '*.class' | wc -l) classes)"
    # Runtime data for stock-APK import (jvm-app/data, not on the classpath):
    #  - android-model.jar: android.jar with code stripped, source for SDK stubs of framework classes the shim lacks
    #  - fw/: framework-res (values/colors/drawables) converted from android.jar, linked into every imported bundle
    if [[ -n "$AJAR" ]]; then
      mkdir -p "$V/jvm-app/data"
      "$JAVA_HOME/bin/java" -cp "$W/shim:$CP" jet.dex.SdkStubs "$AJAR" "$V/jvm-app/data/android-model.jar"
      "$JAVA_HOME/bin/java" -Djava.awt.headless=true -Djet.fw.sdk=28 -cp "$W/shim:$CP" jet.apk.ApkBundler --framework "$AJAR" "$V/jvm-app/data/fw"
      echo "data: model $(du -sh "$V/jvm-app/data/android-model.jar" | cut -f1), fw $(du -sh "$V/jvm-app/data/fw" | cut -f1)"
    fi
  else
    echo "shim: javac failed (non-fatal for hello smoke); see Vendor leftover log"
    mkdir -p "$V/jvm-app"
    cp "$W/shim-javac.log" "$V/jvm-app/shim-javac.log" || true
    tail -20 "$W/shim-javac.log" || true
  fi
fi

cat "$V/jre-$SDK/BUILDINFO.txt" | head -5
du -sh "$V/jre-$SDK/jre"
ls -la "$V/jvm-app"
