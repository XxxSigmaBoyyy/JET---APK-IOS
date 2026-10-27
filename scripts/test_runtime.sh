#!/usr/bin/env bash
# Host test of the JET runtime exactly as it is compiled into JET.app (Runtime/*.c + Modules/*/*.c), on Linux or
# macOS with clang:
#   1. ABI v2 contract test (Runtime/contract_test.c);
#   2. render/touch test (Runtime/test/frame_test.c): first frame + a tap script, display lists dumped as text and,
#      when Python Pillow is available, rendered to PNG (Runtime/test/render_ops.py).
#
#   scripts/test_runtime.sh [OUT_DIR]        (default build/runtime-test)
#
# The module's APK is needed to exercise import + launch. It is never committed: set JET_TEST_APK to a local copy,
# or the script downloads the module's open-source APK from F-Droid and checks its sha256 (Modules/<name>/NOTICE).
set -euo pipefail
REPO="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:-$REPO/build/runtime-test}"; mkdir -p "$OUT"; OUT="$(cd "$OUT" && pwd)"
CC="${CC:-clang}"
CF=(-std=gnu11 -O1 -g -fwrapv -I"$REPO/Runtime" -I"$REPO/JET/Bridge" -Wno-parentheses-equality -Wno-unused-label -Wno-unused-variable)
SRC=("$REPO"/Runtime/rt.c "$REPO"/Runtime/kernel.c "$REPO"/Runtime/jet_bridge_aot.c "$REPO"/Modules/*/*.c)
LIBM=(); [ "$(uname -s)" = Linux ] && LIBM=(-lm)
"$CC" "${CF[@]}" -o "$OUT/contract_test" "$REPO/Runtime/contract_test.c" "${SRC[@]}" "${LIBM[@]}"
"$CC" "${CF[@]}" -o "$OUT/frame_test" "$REPO/Runtime/test/frame_test.c" "${SRC[@]}" "${LIBM[@]}"

APK="${JET_TEST_APK:-}"
if [ -z "$APK" ]; then
  NOTICE="$(ls "$REPO"/Modules/*/NOTICE | head -1)"
  URL="$(sed -n 's/^apk-url: *//p' "$NOTICE")"; SHA="$(sed -n 's/^apk-sha256: *//p' "$NOTICE")"
  APK="$OUT/$(basename "$URL")"
  [ -f "$APK" ] || curl -fsSL --retry 3 -o "$APK" "$URL"
  GOT="$( (command -v sha256sum >/dev/null && sha256sum "$APK" || shasum -a 256 "$APK") | cut -d' ' -f1)"
  [ "$GOT" = "$SHA" ] || { echo "test_runtime: sha256 mismatch for $APK ($GOT != $SHA)" >&2; exit 1; }
fi
W="$(mktemp -d)"; trap 'rm -rf "$W"' EXIT; mkdir -p "$W/c" "$W/f"
echo "== contract test"; "$OUT/contract_test" "$W/c" "$APK" 1 2 7 DEL 5
METRICS=""
if python3 -c 'import PIL' 2>/dev/null; then python3 "$REPO/Runtime/test/render_ops.py" --metrics "$OUT/metrics.txt" 2>/dev/null && METRICS="$OUT/metrics.txt"; fi
echo "== frame test"
rm -f "$OUT"/frame_*.ops "$OUT"/frame_*.png
JET_TEST_METRICS="$METRICS" "$OUT/frame_test" "$W/f" "$APK" 576 1024 1.5 "$OUT/frame" 1 2 0 TAS 9 5 '~TC' 7
if [ -n "$METRICS" ]; then
  for f in "$OUT"/frame_*.ops; do python3 "$REPO/Runtime/test/render_ops.py" "$f" "${f%.ops}.png" 576 1024; done
  echo "rendered: $(ls "$OUT"/frame_*.png | wc -l | tr -d ' ') PNG(s) in $OUT"
fi
