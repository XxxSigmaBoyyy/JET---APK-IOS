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
W=$(mktemp -d); "$JAVA_HOME/bin/javac" --release 21 -d "$W" $(find "$ROOT/Java/hello" -name '*.java')
"$JAVA_HOME/bin/jar" cf "$V/jvm-app/jethello.jar" -C "$W" .
cat "$V/jre-$SDK/BUILDINFO.txt" | head -5; du -sh "$V/jre-$SDK/jre"; ls -la "$V/jvm-app"
