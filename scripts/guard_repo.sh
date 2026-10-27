#!/usr/bin/env bash
# Repository guard (CI step "Repository guard"; run it locally before pushing):
#   * no Android packages, iOS packages or signing material are tracked
#     (*.apk *.xapk *.apks *.aab *.obb *.dex *.ipa *.p12 *.pfx *.cer *.mobileprovision *.keystore *.jks);
#   * no build outputs (dist/, Vendor/JetRuntime/, build/);
#   * no token-like strings or private keys in tracked text files. Matches are reported as file:line only — the
#     matched value is never printed.
set -euo pipefail
cd "$(dirname "$0")/.."
fail=0
if git ls-files | grep -Ei '\.(apk|xapk|apks|aab|obb|dex|ipa|p12|pfx|cer|mobileprovision|keystore|jks)$'; then
  echo "::error::Android/iOS packages and signing material must never be committed (files listed above)."; fail=1
fi
if git ls-files | grep -E '^(dist|build|Vendor/JetRuntime)/'; then
  echo "::error::dist/, build/ and Vendor/JetRuntime/ are build outputs and must never be committed."; fail=1
fi
PATTERNS=(
  'gh[pousr]_[A-Za-z0-9]{36,}'                  # GitHub classic tokens
  'github_pat_[A-Za-z0-9_]{40,}'                # GitHub fine-grained tokens
  '-----BEGIN [A-Z ]*PRIVATE KEY-----'          # PEM private keys
  'AKIA[0-9A-Z]{16}'                            # AWS access key ids
  'xox[abposr]-[A-Za-z0-9-]{10,}'               # Slack tokens
  'AIza[0-9A-Za-z_-]{35}'                       # Google API keys
  'glpat-[A-Za-z0-9_-]{20,}'                    # GitLab tokens
  'sk-[A-Za-z0-9]{32,}'                         # generic secret keys
)
for p in "${PATTERNS[@]}"; do
  hits="$(git grep -nIE -e "$p" -- . ':!scripts/guard_repo.sh' | cut -d: -f1,2 || true)"
  if [ -n "$hits" ]; then
    echo "::error::token-like string (pattern #$(( $(printf '%s\n' "${PATTERNS[@]}" | grep -nxF -- "$p" | cut -d: -f1) ))) at:"; echo "$hits"; fail=1
  fi
done
if git ls-files | grep -E '(^|/)\.gh_token$|(^|/)\.env($|\.)'; then echo "::error::token / env files must never be committed."; fail=1; fi
[ "$fail" = 0 ] && echo "guard_repo: OK ($(git ls-files | wc -l | tr -d ' ') tracked files checked)"
exit "$fail"
