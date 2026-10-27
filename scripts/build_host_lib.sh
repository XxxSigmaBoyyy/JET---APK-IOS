#!/usr/bin/env bash
# Build the JET runtime library for the JetBridge C ABI (docs/RUNTIME_LINK.md).
#
#   scripts/build_host_lib.sh [--proto DIR] [--runtime-only | --app NAME --apk APK --jar JAR --bundle DIR]
#                             [--install] [--no-test]
#
# What it produces (under dist/, git-ignored):
#   dist/host/linux-x86_64/libjetruntime.a, contract_test, contract_test_stub   (Linux host build + contract test)
#   dist/JetRuntime/lib/libjetruntime.a       arm64-apple-ios static library (Mach-O objects, darwin ar format)
#   dist/JetRuntime/include/JetBridge.h       the ABI header this library implements
#   dist/JetRuntime/manifest.json             abi, module, dex crc, sha256, toolchain
#   dist/JetRuntime-ios-arm64.tar.gz          the three above, ready to publish as a CI/release asset
# --install also copies dist/JetRuntime to Vendor/JetRuntime, which project.runtime.yml links (JET_RUNTIME=YES).
#
# On Linux the iOS objects are compiled against aot/runtime/hdr-min (ISO C prototypes of the libSystem symbols the
# runtime uses) because there is no Apple SDK; on macOS the same step uses `xcrun --sdk iphoneos clang` and the real
# SDK. Both produce identical external symbol references; the final link happens in Xcode.
#
# The JET toolchain checkout (aot/, harness/, tools/soot) is NOT part of this repository: pass --proto or set
# JET_PROTO. Only open-source APKs may be compiled into a module; APKs from an apks_closed/ directory are refused,
# and nothing built here may be committed (dist/ and Vendor/JetRuntime/ are git-ignored).
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
PROTO="${JET_PROTO:-$REPO/../proto}"
APP=""; APK=""; JAR=""; BUNDLE=""; RUNTIME_ONLY=0; INSTALL=0; TEST=1
IOS_TARGET="${JET_IOS_TARGET:-arm64-apple-ios17.0}"

die() { echo "build_host_lib: $*" >&2; exit 1; }
while [ $# -gt 0 ]; do
  case "$1" in
    --proto) PROTO="$2"; shift 2 ;;
    --app) APP="$2"; shift 2 ;;
    --apk) APK="$2"; shift 2 ;;
    --jar) JAR="$2"; shift 2 ;;
    --bundle) BUNDLE="$2"; shift 2 ;;
    --runtime-only) RUNTIME_ONLY=1; shift ;;
    --install) INSTALL=1; shift ;;
    --no-test) TEST=0; shift ;;
    -h|--help) sed -n '2,25p' "$0"; exit 0 ;;
    *) die "unknown argument $1" ;;
  esac
done

[ -f "$PROTO/aot/runtime/jetrt.h" ] || die "JET toolchain not found at $PROTO (use --proto DIR or JET_PROTO)"
PROTO="$(cd "$PROTO" && pwd)"
RTDIR="$PROTO/aot/runtime"
for t in clang python3; do command -v "$t" >/dev/null || die "$t is required"; done
AR="$(command -v llvm-ar || true)"; NM="$(command -v llvm-nm || command -v nm)"

# ---- default module: the open-source Heading Calculator demo (F-Droid, GPL) when its harness artifacts exist ----
if [ "$RUNTIME_ONLY" = 0 ] && [ -z "$APP" ]; then
  APP=heading
  APK="$PROTO/harness/apks/org.debian.eugen.headingcalculator_4.apk"
  JAR="$(find "$PROTO/harness/cache" -maxdepth 1 -name 'org.debian.eugen.headingcalculator_4.apk.*.jar' -printf '%T@ %p\n' 2>/dev/null | sort -n | tail -1 | cut -d' ' -f2-)"
  BUNDLE="$PROTO/harness/out/heading_soot/bundle"
  if [ ! -f "$APK" ] || [ -z "$JAR" ] || [ ! -f "$BUNDLE/app.properties" ]; then
    echo "build_host_lib: demo module artifacts missing (run the harness on the Heading Calculator first); building runtime-only" >&2
    RUNTIME_ONLY=1
  fi
fi
if [ "$RUNTIME_ONLY" = 0 ]; then
  for v in APP APK JAR BUNDLE; do [ -n "${!v}" ] || die "--$(echo "$v" | tr '[:upper:]' '[:lower:]') is required with --app"; done
  if [ ! -f "$APK" ] || [ ! -f "$JAR" ] || [ ! -f "$BUNDLE/app.properties" ]; then die "missing --apk / --jar / --bundle input"; fi
  case "$(cd "$(dirname "$APK")" && pwd)/" in
    */apks_closed/*) die "refusing $APK: closed APKs must never be compiled into a distributable library" ;;
  esac
  [[ "$APP" =~ ^[A-Za-z0-9_]+$ ]] || die "--app must be [A-Za-z0-9_]+"
fi

TEST_APK="${APK:-$PROTO/harness/apks/org.debian.eugen.headingcalculator_4.apk}"; [ -f "$TEST_APK" ] || TEST_APK=""
OUT="$REPO/dist"; GEN="$OUT/gen"; HOST="$OUT/host/linux-x86_64"; IOSO="$OUT/obj/ios-arm64"; PKG="$OUT/JetRuntime"
rm -rf "$GEN" "$HOST" "$IOSO" "$PKG"; mkdir -p "$GEN" "$HOST" "$IOSO" "$PKG/lib" "$PKG/include"

# ---- 1. app module: Jimple -> C (jet.aot.JimpleToC) + resources -> C, or empty stand-ins ----
if [ "$RUNTIME_ONLY" = 0 ]; then
  if ! command -v java >/dev/null || ! command -v javac >/dev/null; then die "java/javac (JDK 17+) are required for the AOT step"; fi
  SOOT="$PROTO/tools/soot/soot.jar"; ANDROID_JAR="$PROTO/tools/soot/platforms/android-33/android.jar"
  if [ ! -f "$SOOT" ] || [ ! -f "$ANDROID_JAR" ]; then die "soot.jar / android-33 android.jar missing under $PROTO/tools/soot"; fi
  mkdir -p "$GEN/aot-classes"
  echo "[1/5] AOT: $(basename "$JAR") -> ${APP}_app.c"
  javac -nowarn -cp "$SOOT" -d "$GEN/aot-classes" "$PROTO"/aot/src/jet/aot/*.java 2>&1 | grep -v '^Note' || true
  java -cp "$SOOT:$GEN/aot-classes" jet.aot.JimpleToC "$JAR" "$ANDROID_JAR" "$GEN/${APP}_app.c" 2>&1 | grep -v 'SLF4J\|^\[main\]' || true
  [ -s "$GEN/${APP}_app.c" ] || die "JimpleToC produced no output"
  python3 "$PROTO/aot/gen_res.py" "$BUNDLE" "$GEN/${APP}_res.c"
  python3 - "$APK" "$BUNDLE/app.properties" "$APP" "$GEN/jet_module.c" <<'PY'
import sys, zipfile
apk, props, name, out = sys.argv[1:]
p = dict(l.rstrip("\n").split("=", 1) for l in open(props, encoding="utf-8") if "=" in l)
crc = zipfile.ZipFile(apk).getinfo("classes.dex").CRC
def c(s): return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'
open(out, "w").write(f"""/* generated by scripts/build_host_lib.sh — AOT module metadata (ABI v1) */
#include <stdint.h>
const int jet_mod_present = 1;
const char jet_mod_name[] = {c(name)}, jet_mod_package[] = {c(p["package"])}, jet_mod_activity[] = {c(p["mainActivity"])}, jet_mod_label[] = {c(p.get("label", name))};
const uint32_t jet_mod_dexcrc = 0x{crc:08x}u;
""")
print(f"       module {name}: {p['package']} / {p['mainActivity']} dex crc {crc:08x}")
PY
  MOD_SRC=("$GEN/${APP}_app.c" "$GEN/${APP}_res.c" "$GEN/jet_module.c")
else
  echo "[1/5] runtime-only build (no app module)"
  cat > "$GEN/jet_module.c" <<'C'
/* generated by scripts/build_host_lib.sh — runtime-only build: no AOT app module */
#include "jetrt.h"
const int jet_mod_present = 0;
const char jet_mod_name[] = "", jet_mod_package[] = "", jet_mod_activity[] = "", jet_mod_label[] = "";
const uint32_t jet_mod_dexcrc = 0;
const JResLayout jet_layouts[] = {{0, NULL}};
const JResString jet_strings[] = {{0, NULL}};
void jet_app_register(void) {}
C
  MOD_SRC=("$GEN/jet_module.c")
fi
SRC=("$RTDIR/rt.c" "$RTDIR/kernel.c" "$REPO/Runtime/jet_bridge_aot.c" "${MOD_SRC[@]}")
CF=(-std=gnu11 -O2 -fwrapv -fPIC -I"$RTDIR" -I"$REPO/JET/Bridge" -Wno-parentheses-equality -Wno-unused-label -Wno-unused-variable)

# ---- 2. Linux host library + contract test ----
echo "[2/5] host: x86_64-linux libjetruntime.a"
HOBJ=()
for f in "${SRC[@]}"; do o="$HOST/$(basename "${f%.c}").o"; clang "${CF[@]}" -c "$f" -o "$o"; HOBJ+=("$o"); done
if [ -n "$AR" ]; then "$AR" rcs "$HOST/libjetruntime.a" "${HOBJ[@]}"; else ar rcs "$HOST/libjetruntime.a" "${HOBJ[@]}"; fi
clang -std=gnu11 -O1 -I"$REPO/JET/Bridge" "$REPO/Runtime/contract_test.c" "$HOST/libjetruntime.a" -lm -o "$HOST/contract_test"
clang -std=gnu11 -O1 -I"$REPO/JET/Bridge" "$REPO/Runtime/contract_test.c" "$REPO/JET/Bridge/JetBridgeStub.c" -o "$HOST/contract_test_stub"
if [ "$TEST" = 1 ]; then
  echo "[3/5] contract tests"
  W="$(mktemp -d)"; trap 'rm -rf "$W"' EXIT
  mkdir -p "$W/stub" "$W/rt"
  "$HOST/contract_test_stub" "$W/stub" ${TEST_APK:+"$TEST_APK"} | sed 's/^/       stub: /'
  if [ "$RUNTIME_ONLY" = 0 ] && [ "$APP" = heading ]; then CLICKS=(1 2 7 DEL 5); else CLICKS=(); fi
  "$HOST/contract_test" "$W/rt" ${TEST_APK:+"$TEST_APK"} "${CLICKS[@]}" | sed 's/^/       aot:  /'
else
  echo "[3/5] contract tests skipped (--no-test)"
fi

# ---- 3. arm64-apple-ios static library ----
echo "[4/5] iOS: $IOS_TARGET libjetruntime.a"
if [ "$(uname -s)" = Darwin ] && xcrun --sdk iphoneos --show-sdk-path >/dev/null 2>&1; then
  IOSCC=(xcrun --sdk iphoneos clang -target "$IOS_TARGET" -isysroot "$(xcrun --sdk iphoneos --show-sdk-path)"); IOSAR=(xcrun ar)
  HDR="Apple iPhoneOS SDK $(xcrun --sdk iphoneos --show-sdk-version)"
else
  IOSCC=(clang --target="$IOS_TARGET" -ffreestanding -nostdlibinc -isystem "$RTDIR/hdr-min")
  [ -n "$AR" ] || die "llvm-ar is required to write a darwin-format archive on Linux"
  IOSAR=("$AR" --format=darwin)
  HDR="hdr-min (Linux cross-compile, no Apple SDK)"
fi
IOBJ=()
for f in "${SRC[@]}"; do o="$IOSO/$(basename "${f%.c}").o"; "${IOSCC[@]}" "${CF[@]}" -c "$f" -o "$o"; IOBJ+=("$o"); done
rm -f "$PKG/lib/libjetruntime.a"; "${IOSAR[@]}" rcs "$PKG/lib/libjetruntime.a" "${IOBJ[@]}"
# every unresolved symbol must come from libSystem (C library / libm); anything else means a missing runtime piece
UNDEF="$("$NM" -u "$PKG/lib/libjetruntime.a" 2>/dev/null | awk 'NF==2{print $2} NF==1&&$1!~/:$/{print $1}' | sort -u)"
DEFINED="$("$NM" -g --defined-only "$PKG/lib/libjetruntime.a" 2>/dev/null | awk 'NF==3{print $3}' | sort -u)"
EXTERNAL="$(comm -23 <(echo "$UNDEF") <(echo "$DEFINED") | tr '\n' ' ')"
for s in $EXTERNAL; do
  case "$s" in
    _atan2|_calloc|_cos|_exit|_fclose|_fflush|_fopen|_fprintf|_fputs|_fread|_free|_fseek|_ftell|_fwrite|_floor|_longjmp|_malloc|_memchr|_memcmp|_memcpy|_memmove|_memset|_printf|_realloc|_rint|_setjmp|_sin|_snprintf|_sqrt|_strcat|_strchr|_strcmp|_strcpy|_strdup|_strlen|_strncmp|_strrchr|___stderrp|___stdoutp|___stack_chk_fail|___stack_chk_guard|_pow|_fmod|_exp|_log|_tan|_asin|_acos|_atan|_ceil|_fabs) ;;
    *) die "iOS library references non-libSystem symbol $s" ;;
  esac
done
for s in jet_bridge_abi_version jet_bridge_version jet_bridge_has_runtime jet_bridge_modules jet_translate_apk jet_runtime_main \
         jet_runtime_touch jet_runtime_ui_snapshot jet_runtime_click jet_runtime_stop jet_runtime_status; do
  echo "$DEFINED" | grep -qx "_$s" || die "iOS library does not export _$s"
done
echo "       external symbols (libSystem): $EXTERNAL"

# ---- 4. package ----
echo "[5/5] package dist/JetRuntime"
cp "$REPO/JET/Bridge/JetBridge.h" "$PKG/include/"
if command -v sha256sum >/dev/null; then SHA="$(sha256sum "$PKG/lib/libjetruntime.a" | cut -d' ' -f1)"; else SHA="$(shasum -a 256 "$PKG/lib/libjetruntime.a" | cut -d' ' -f1)"; fi
python3 - "$PKG/manifest.json" "$GEN/jet_module.c" "$SHA" "$IOS_TARGET" "$HDR" "$(clang --version | head -1)" <<'PY'
import sys, json, re
out, mod, sha, target, hdr, cc = sys.argv[1:]
src = open(mod).read()
def g(k):
    m = re.search(k + r'\[\] = "((?:[^"\\]|\\.)*)"', src); return m.group(1) if m else ""
crc = re.search(r"jet_mod_dexcrc = (0x[0-9a-f]+|0)", src).group(1)
json.dump({"abi": 1, "library": "lib/libjetruntime.a", "sha256": sha, "target": target, "headers": hdr, "compiler": cc,
           "module": ({"name": g("jet_mod_name"), "package": g("jet_mod_package"), "activity": g("jet_mod_activity"),
                       "label": g("jet_mod_label"), "dex_crc32": crc[2:].rjust(8, "0")} if "jet_mod_present = 1" in src else None)},
          open(out, "w"), indent=2)
PY
tar -C "$OUT" -czf "$OUT/JetRuntime-ios-arm64.tar.gz" JetRuntime
file "$PKG/lib/libjetruntime.a" 2>/dev/null | sed 's/^/       /' || true
echo "       sha256 $SHA"
if [ "$INSTALL" = 1 ]; then
  rm -rf "$REPO/Vendor/JetRuntime"; mkdir -p "$REPO/Vendor"; cp -R "$PKG" "$REPO/Vendor/JetRuntime"
  echo "       installed -> Vendor/JetRuntime (generate with: JET_RUNTIME=YES xcodegen generate)"
fi
echo "build_host_lib: done"
