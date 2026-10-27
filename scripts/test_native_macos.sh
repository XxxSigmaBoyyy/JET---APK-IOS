#!/usr/bin/env bash
# Build the partner's arm64 interpreter (Runtime/native) for the macOS arm64 HOST (the Darwin ABI closest to iOS)
# and run its unit tests natively. The aarch64-linux guest test library (tests/guest_lib.c) is cross-built as an
# ELF with Homebrew LLVM clang + lld (Apple ld cannot emit ELF). Every compiler diagnostic is kept verbatim in
#   $OUT/native_macos_build_errors.txt   and per-suite test output in $OUT/test_<suite>.txt;
#   $OUT/native_macos_summary.txt holds one line per suite: PASS / FAIL(exit) / TIMEOUT / BUILD-FAIL.
# Exits 1 if the library fails to build or any suite fails.
set -uo pipefail
cd "$(dirname "$0")/.."
OUT="$(pwd)/${1:-build/native-macos}"; mkdir -p "$OUT/bin"
LOG="$OUT/native_macos_build_errors.txt"; SUM="$OUT/native_macos_summary.txt"; : > "$LOG"; : > "$SUM"
N=Runtime/native
CC=${CC:-clang}
LLVM=$(brew --prefix llvm 2>/dev/null || true)
XCC="${LLVM:+$LLVM/bin/}clang"
CFLAGS=(-O2 -g -std=c11 -Wall -Wextra -Wno-unused-parameter -Wno-unused-function -fno-strict-aliasing -D_GNU_SOURCE -I"$N" -I"$N/third_party")
LIBS=(-lpthread -ldl -lm -lz)
echo "host: $(uname -a)" | tee -a "$LOG"; echo "cc: $($CC --version | head -1)" | tee -a "$LOG"; echo "guest cc: $($XCC --version | head -1)" | tee -a "$LOG"
rc=0; objs=(); nfail=0
for f in $N/*.c; do
  o="$OUT/bin/$(basename "${f%.c}").o"
  msg=$("$CC" -arch arm64 "${CFLAGS[@]}" -c "$f" -o "$o" 2>&1); r=$?
  if [ -n "$msg" ] || [ $r -ne 0 ]; then echo "----- macos-arm64 $f (exit $r)" >> "$LOG"; printf '%s\n' "$msg" >> "$LOG"; fi
  if [ $r -ne 0 ]; then nfail=$((nfail+1)); rc=1; else objs+=("$o"); fi
done
echo "library: files_failed=$nfail errors=$(grep -cE ': (fatal )?error:' "$LOG") warnings=$(grep -c ': warning:' "$LOG")" | tee -a "$SUM"
if [ $nfail -ne 0 ]; then cat "$SUM" >> "$LOG"; echo "::group::native_macos_build_errors.txt"; cat "$LOG"; echo "::endgroup::"; exit 1; fi
libtool -static -o "$OUT/bin/libjet_native.a" "${objs[@]}" >> "$LOG" 2>&1

# Guest test library: aarch64 Linux ELF, no libc (same recipe as build.sh/Makefile).
msg=$("$XCC" --target=aarch64-linux-gnu -fuse-ld=lld -nostdlib -shared -fPIC -O0 -o "$N/tests/guest_lib.so" "$N/tests/guest_lib.c" 2>&1); r=$?
echo "----- guest_lib.so (aarch64-linux-gnu, lld) exit $r" >> "$LOG"; printf '%s\n' "${msg:-(clean)}" >> "$LOG"
echo "guest_lib.so: $([ $r -eq 0 ] && echo built || echo "BUILD-FAIL exit=$r")" | tee -a "$SUM"

SUITES="test_unit test_jni test_mem_bitfield test_v9_jni_dl test_cxx_eh test_v10_trap_abi test_v101_guest_abi test_v11_xzr_excl test_v101_darwin_abi test_tg"
for t in $SUITES; do
  src="$N/tests/$t.c"; [ -f "$src" ] || { echo "$t: MISSING" | tee -a "$SUM"; continue; }
  msg=$("$CC" -arch arm64 "${CFLAGS[@]}" "$src" "$OUT/bin/libjet_native.a" "${LIBS[@]}" -o "$OUT/bin/$t" 2>&1); r=$?
  if [ -n "$msg" ] || [ $r -ne 0 ]; then echo "----- macos-arm64 $src (exit $r)" >> "$LOG"; printf '%s\n' "$msg" >> "$LOG"; fi
  if [ $r -ne 0 ]; then echo "$t: BUILD-FAIL exit=$r" | tee -a "$SUM"; rc=1; continue; fi
  # Run from Runtime/native (tests load tests/guest_lib.so relative to it); 120 s cap per suite.
  ( cd "$N" && perl -e 'alarm shift; exec @ARGV' 120 "$OUT/bin/$t" ) > "$OUT/$t.txt" 2>&1; r=$?
  tail_line=$(grep -E 'PASS|FAIL|passed|failed' "$OUT/$t.txt" | tail -1)
  if [ $r -eq 0 ]; then st=PASS; elif [ $r -eq 142 ]; then st=TIMEOUT; else st="FAIL(exit=$r)"; fi
  # test_tg needs the Telegram corpus (not in the repo): informational only.
  if [ "$t" = test_tg ]; then echo "$t: $st (info only; needs Telegram corpus) | $tail_line" | tee -a "$SUM"; continue; fi
  [ $st = PASS ] || rc=1
  echo "$t: $st | $tail_line" | tee -a "$SUM"
done
rm -f "$N/tests/guest_lib.so"
cat "$SUM" >> "$LOG"
echo "::group::native_macos_build_errors.txt"; cat "$LOG"; echo "::endgroup::"
for t in $SUITES; do [ -f "$OUT/$t.txt" ] && { echo "::group::$t output"; cat "$OUT/$t.txt"; echo "::endgroup::"; }; done
exit $rc
