#!/bin/bash
# Uses Assets/AppIcon-1024.png (your artwork, 1024x1024, no alpha) when present; otherwise keeps the placeholder.
# The same file is also the in-app brand mark (BrandIcon imageset, used by JetLogo).
set -euo pipefail
cd "$(dirname "$0")/.."
xc=JET/Resources/Assets.xcassets
if [ -f Assets/AppIcon-1024.png ]; then
  cp Assets/AppIcon-1024.png "$xc/AppIcon.appiconset/AppIcon-1024.png"
  cp Assets/AppIcon-1024.png "$xc/BrandIcon.imageset/BrandIcon.png"
  echo "icon: Assets/AppIcon-1024.png"
else
  cp "$xc/AppIcon.appiconset/AppIcon-1024.png" "$xc/BrandIcon.imageset/BrandIcon.png"
  echo "icon: placeholder"
fi
