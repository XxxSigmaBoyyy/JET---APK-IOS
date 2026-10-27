#!/bin/bash
# Packs an unsigned .app into an .ipa (Payload/JET.app) for E-SIGN, AltStore or Sideloadly to re-sign.
# usage: scripts/package_ipa.sh <path/to/JET.app> <out.ipa>
set -euo pipefail
app="$1"; out="$2"
[ -d "$app" ] || { echo "no .app at $app" >&2; exit 1; }
work="$(mktemp -d)"; trap 'rm -rf "$work"' EXIT
mkdir -p "$work/Payload"
cp -R "$app" "$work/Payload/"
# strip any leftover signature so the re-signer starts clean
rm -rf "$work/Payload/$(basename "$app")/_CodeSignature" "$work/Payload/$(basename "$app")/embedded.mobileprovision"
mkdir -p "$(dirname "$out")"; out_abs="$(cd "$(dirname "$out")" && pwd)/$(basename "$out")"
(cd "$work" && zip -qry "$out_abs" Payload)
echo "ipa: $out_abs ($(du -h "$out_abs" | cut -f1))"
