#!/bin/bash
# Stage the CI-built JRE + JNI headers + JET's Java code for an Xcode build (all under Vendor/, never committed).
#   stage_jvm.sh <iphoneos|iphonesimulator> <jre21-zero-SDK.tar.xz>
set -euo pipefail
SDK=$1; TAR=$2
ROOT=$(cd "$(dirname "$0")/.." && pwd); V=$ROOT/Vendor
: "${JAVA_HOME:?JDK 21 needed for javac + jni.h}"
rm -rf "$V/jre-$SDK" "$V/jni" "$V/jvm-app"; mkdir -p "$V/jre-$SDK" "$V/jni" "$V/jvm-app"
tar -C "$V/jre-$SDK" -xJf "$TAR"
cp "$JAVA_HOME/include/jni.h" "$JAVA_HOME/include/darwin/jni_md.h" "$V/jni/"

ASM="$ROOT/tools/asm/asm-9.7.jar:$ROOT/tools/asm/asm-tree-9.7.jar"
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
  CP="$ASM"
  [[ -n "$AJAR" ]] && CP="$AJAR:$CP"
  [[ -n "$SQLITE" ]] && CP="$SQLITE:$CP"
  set +e
  "$JAVA_HOME/bin/javac" -nowarn --release 21 -cp "$CP" -d "$W/shim" \
    $(find "$SHIM_SRC" -name '*.java' ! -name 'JetSkia*.java' ! -name 'SkiaInit.java') >"$W/shim-javac.log" 2>&1
  RC=$?
  set -e
  if [[ $RC -eq 0 ]]; then
    "$JAVA_HOME/bin/jar" cf "$V/jvm-app/jetshim.jar" -C "$W/shim" .
    [[ -n "$SQLITE" ]] && cp "$SQLITE" "$V/jvm-app/"
    echo "shim: OK ($(find "$W/shim" -name '*.class' | wc -l) classes)"
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
