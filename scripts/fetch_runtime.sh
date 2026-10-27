#!/usr/bin/env bash
# Provide the optional prebuilt JET runtime (docs/RUNTIME_LINK.md) to the Xcode build.
#
#   scripts/fetch_runtime.sh            # uses Vendor/JetRuntime if present, else downloads $JET_RUNTIME_URL
#
# Env:
#   JET_RUNTIME_URL     https URL of a JetRuntime-ios-arm64.tar.gz made by scripts/build_host_lib.sh (optional)
#   JET_RUNTIME_SHA256  expected sha256 of that archive (required when JET_RUNTIME_URL is set)
#   GITHUB_ENV          when set (GitHub Actions), JET_RUNTIME=YES|NO is appended for the xcodegen step
#
# Without a runtime the app builds with JET/Bridge/JetBridgeStub.c (JET_RUNTIME=NO). The archive is verified
# (sha256, path whitelist, manifest abi + library sha256, identical JetBridge.h) before it is used.
# Vendor/JetRuntime/ is git-ignored and must never be committed.
set -euo pipefail
REPO="$(cd "$(dirname "$0")/.." && pwd)"
VENDOR="$REPO/Vendor/JetRuntime"
ABI="$(sed -n 's/^#define JET_BRIDGE_ABI_VERSION \([0-9][0-9]*\).*/\1/p' "$REPO/JET/Bridge/JetBridge.h")"

result() {
  echo "fetch_runtime: JET_RUNTIME=$1${2:+ ($2)}"
  if [ -n "${GITHUB_ENV:-}" ]; then echo "JET_RUNTIME=$1" >> "$GITHUB_ENV"; fi
  exit 0
}
fail() { echo "fetch_runtime: $*" >&2; exit 1; }

sha256() { if command -v sha256sum >/dev/null; then sha256sum "$1" | cut -d' ' -f1; else shasum -a 256 "$1" | cut -d' ' -f1; fi; }

verify() {  # $1 = JetRuntime dir
  local d="$1"
  local f
  for f in lib/libjetruntime.a manifest.json include/JetBridge.h; do [ -f "$d/$f" ] || fail "incomplete runtime in $d (no $f)"; done
  python3 - "$d/manifest.json" "$ABI" "$(sha256 "$d/lib/libjetruntime.a")" <<'PY' || fail "manifest check failed"
import json, sys
m = json.load(open(sys.argv[1])); abi = int(sys.argv[2]); sha = sys.argv[3]
assert m.get("abi") == abi, f"manifest abi {m.get('abi')} != header abi {abi}"
assert m.get("library") == "lib/libjetruntime.a", "unexpected library path"
assert m.get("sha256") == sha, "library sha256 does not match manifest"
mod = m.get("module")
print("fetch_runtime: module", (mod or {}).get("package", "(runtime-only)"))
PY
  cmp -s "$d/include/JetBridge.h" "$REPO/JET/Bridge/JetBridge.h" || fail "runtime JetBridge.h differs from JET/Bridge/JetBridge.h"
}

if [ -d "$VENDOR" ]; then verify "$VENDOR"; result YES "Vendor/JetRuntime"; fi
URL="${JET_RUNTIME_URL:-}"
[ -n "$URL" ] || result NO "no JET_RUNTIME_URL, building with the stub"
case "$URL" in https://*) ;; *) fail "JET_RUNTIME_URL must be https" ;; esac
[ -n "${JET_RUNTIME_SHA256:-}" ] || fail "JET_RUNTIME_SHA256 is required with JET_RUNTIME_URL"

T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
curl -fsSL --proto '=https' --max-filesize 209715200 -o "$T/rt.tgz" "$URL" || fail "download failed"
[ "$(sha256 "$T/rt.tgz")" = "$JET_RUNTIME_SHA256" ] || fail "archive sha256 mismatch"
while IFS= read -r p; do
  case "$p" in
    JetRuntime/|JetRuntime/lib/|JetRuntime/include/|JetRuntime/lib/libjetruntime.a|JetRuntime/include/JetBridge.h|JetRuntime/manifest.json) ;;
    *) fail "unexpected path in archive: $p" ;;
  esac
done < <(tar -tzf "$T/rt.tgz")
mkdir -p "$T/x" && tar -C "$T/x" -xzf "$T/rt.tgz" --no-same-owner --no-same-permissions
verify "$T/x/JetRuntime"
mkdir -p "$REPO/Vendor"; rm -rf "$VENDOR"; cp -R "$T/x/JetRuntime" "$VENDOR"
result YES "downloaded"
