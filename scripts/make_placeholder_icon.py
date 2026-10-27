#!/usr/bin/env python3
"""Draws the placeholder JET app icon (1024x1024, opaque RGB): violet/blue aurora, glass disc, fighter-jet
silhouette (same outline as JET/Theme/JetGlyph.swift). Replace it by dropping your own Assets/AppIcon-1024.png
into the repo; scripts/sync_icon.sh copies that into the asset catalog."""
import math, sys
from PIL import Image, ImageDraw, ImageFilter

OUTLINE = [(50, 3), (53.5, 14), (55.2, 27), (56, 38), (64, 43), (91, 63), (91, 70), (58, 64.5),
           (58.5, 77), (73, 88.5), (73, 94), (56.5, 90), (54.5, 97), (45.5, 97), (43.5, 90),
           (27, 94), (27, 88.5), (41.5, 77), (42, 64.5), (9, 70), (9, 63), (36, 43), (44, 38),
           (44.8, 27), (46.5, 14)]
S = 2048  # supersampled canvas
deep, night = (9, 10, 28), (22, 15, 52)
violet, blue, cyan = (143, 97, 255), (61, 133, 255), (97, 219, 255)

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

img = Image.new("RGB", (S, S))
px = ImageDraw.Draw(img)
for y in range(S):
    px.line([(0, y), (S, y)], fill=lerp(deep, night, y / S))

def glow(color, cx, cy, r, alpha, blur):
    layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(layer).ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (alpha,))
    return layer.filter(ImageFilter.GaussianBlur(blur))

base = img.convert("RGBA")
for g in (glow(violet, S * 0.30, S * 0.28, S * 0.42, 210, S * 0.12),
          glow(blue, S * 0.74, S * 0.70, S * 0.40, 200, S * 0.12),
          glow(cyan, S * 0.62, S * 0.22, S * 0.18, 110, S * 0.10)):
    base = Image.alpha_composite(base, g)

# glass disc: translucent fill + bright rim
disc = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(disc)
c, r = S / 2, S * 0.36
d.ellipse([c - r, c - r, c + r, c + r], fill=(255, 255, 255, 34), outline=(255, 255, 255, 120), width=int(S * 0.006))
hl = Image.new("RGBA", (S, S), (0, 0, 0, 0))
ImageDraw.Draw(hl).ellipse([c - r * 0.85, c - r * 0.95, c + r * 0.55, c - r * 0.15], fill=(255, 255, 255, 46))
base = Image.alpha_composite(base, disc)
base = Image.alpha_composite(base, hl.filter(ImageFilter.GaussianBlur(S * 0.02)))

# jet silhouette, rotated 35 degrees (climbing to the upper right), with a cyan glow
def jet_points(scale, angle_deg):
    a = math.radians(angle_deg); out = []
    for x, y in OUTLINE:
        dx, dy = (x - 50) * scale, (y - 50) * scale
        out.append((c + dx * math.cos(a) - dy * math.sin(a), c + dx * math.sin(a) + dy * math.cos(a)))
    return out

pts = jet_points(S * 0.0062, 35)
glow_l = Image.new("RGBA", (S, S), (0, 0, 0, 0))
ImageDraw.Draw(glow_l).polygon(pts, fill=cyan + (170,))
base = Image.alpha_composite(base, glow_l.filter(ImageFilter.GaussianBlur(S * 0.025)))
jet = Image.new("RGBA", (S, S), (0, 0, 0, 0))
ImageDraw.Draw(jet).polygon(pts, fill=(245, 248, 255, 255))
grad = Image.new("RGBA", (S, S), (0, 0, 0, 0))
gd = ImageDraw.Draw(grad)
for y in range(S):
    gd.line([(0, y), (S, y)], fill=lerp((255, 255, 255), (170, 225, 255), y / S) + (255,))
jet = Image.composite(grad, jet, jet.split()[3])
base = Image.alpha_composite(base, jet)

out = base.convert("RGB").resize((1024, 1024), Image.LANCZOS)
out.save(sys.argv[1] if len(sys.argv) > 1 else "JET/Resources/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png", optimize=True)
