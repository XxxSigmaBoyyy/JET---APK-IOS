#!/usr/bin/env bash
# Compile the partner's in-process arm64 interpreter (Runtime/native/*.c) for ios-arm64 device (iphoneos) and
# arm64 simulator, with the partner's own warning flags. Writes every compiler diagnostic verbatim to
#   $OUT/native_ios_build_errors.txt   (all errors + warnings, per sdk/file)
#   $OUT/native_ios_summary.txt        (counts per sdk)
# and static archives $OUT/<sdk>/libjet_native.a. Exits 1 if any library source fails to compile.
# tests/*.c are host (Linux) test drivers: syntax-checked for information only, never fatal, never linked.
set -uo pipefail
cd "$(dirname "$0")/.."
OUT="${1:-build/native-ios}"; mkdir -p "$OUT"
LOG="$OUT/native_ios_build_errors.txt"; SUM="$OUT/native_ios_summary.txt"; : > "$LOG"; : > "$SUM"
CFLAGS=(-O2 -g -std=c11 -Wall -Wextra -Wno-unused-parameter -Wno-unused-function -fno-strict-aliasing -D_GNU_SOURCE)
SRCS=$(ls Runtime/native/*.c)
rc=0
for spec in "iphoneos:arm64-apple-ios17.0" "iphonesimulator:arm64-apple-ios17.0-simulator"; do
  sdk=${spec%%:*}; tgt=${spec#*:}; od="$OUT/$sdk"; mkdir -p "$od"
  SYSROOT=$(xcrun --sdk "$sdk" --show-sdk-path); CC=$(xcrun --sdk "$sdk" -f clang)
  echo "===== $sdk ($tgt) $($CC --version | head -1) =====" >> "$LOG"
  objs=(); nerr=0; nwarn=0; nfail=0
  for f in $SRCS; do
    o="$od/$(basename "${f%.c}").o"
    msg=$("$CC" -target "$tgt" -isysroot "$SYSROOT" "${CFLAGS[@]}" -IRuntime/native -c "$f" -o "$o" 2>&1); r=$?
    if [ -n "$msg" ] || [ $r -ne 0 ]; then echo "----- $sdk $f (exit $r)" >> "$LOG"; printf '%s\n' "$msg" >> "$LOG"; fi
    nerr=$((nerr + $(printf '%s\n' "$msg" | grep -c ': error:'))); nwarn=$((nwarn + $(printf '%s\n' "$msg" | grep -c ': warning:')))
    if [ $r -ne 0 ]; then nfail=$((nfail+1)); rc=1; else objs+=("$o"); fi
  done
  [ $nfail -eq 0 ] && xcrun --sdk "$sdk" libtool -static -o "$od/libjet_native.a" "${objs[@]}" >> "$LOG" 2>&1
  tw=0
  for f in Runtime/native/tests/*.c; do
    msg=$("$CC" -target "$tgt" -isysroot "$SYSROOT" "${CFLAGS[@]}" -IRuntime/native -fsyntax-only "$f" 2>&1)
    [ -n "$msg" ] && { echo "----- (info, host test driver) $sdk $f" >> "$LOG"; printf '%s\n' "$msg" >> "$LOG"; tw=$((tw+1)); }
  done
  echo "$sdk: files_failed=$nfail errors=$nerr warnings=$nwarn tests_with_diagnostics=$tw lib=$([ -f "$od/libjet_native.a" ] && echo built || echo none)" | tee -a "$SUM"
done
cat "$SUM" >> "$LOG"
echo "::group::native_ios_build_errors.txt"; cat "$LOG"; echo "::endgroup::"
exit $rc
