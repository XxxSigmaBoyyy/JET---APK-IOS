#!/usr/bin/env python3
"""Render a frame_test display-list dump (*.ops) to PNG with PIL — the same draw semantics the iOS host implements
with CoreGraphics in JET/App/JetSurfaceView.swift (rects, rounded rects, text boxes with Android gravity)."""
import sys
from PIL import Image, ImageDraw, ImageFont
FONTS = {0: "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 1: "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"}
def argb(v): v = int(v, 16); return ((v >> 16) & 255, (v >> 8) & 255, v & 255, (v >> 24) & 255)
def render(path, out, w, h):
    lines = open(path, encoding="utf-8").read().split("\n")
    img = Image.new("RGBA", (w, h), argb(lines[0].split()[1]))
    for ln in lines[1:]:
        if not ln: continue
        head, _, text = ln.partition("\t"); f = head.split()
        op, x, y, bw, bh = int(f[0]), *map(float, f[1:5]); col = argb(f[5]); size = float(f[6]); flags, grav = int(f[7]), int(f[8])
        cx, cy, cw, ch = map(float, f[9:13])
        layer = Image.new("RGBA", (w, h), (0, 0, 0, 0)); d = ImageDraw.Draw(layer)
        if op == 1: d.rectangle([x, y, x + bw - 1, y + bh - 1], fill=col)
        elif op == 3: d.rounded_rectangle([x, y, x + bw - 1, y + bh - 1], radius=size, fill=col)
        elif op == 2:
            font = ImageFont.truetype(FONTS[flags & 1], max(1, round(size)))
            asc, desc = font.getmetrics(); tw = font.getlength(text)
            hg, vg = grav & 7, grav & 0x70
            tx = x + (bw - tw if hg == 5 else (bw - tw) / 2 if hg == 1 else 0)
            ty = y + (bh - asc - desc if vg == 0x50 else (bh - asc - desc) / 2 if vg == 0x10 else 0)
            d.text((tx, ty), text, font=font, fill=col)
        mask = Image.new("L", (w, h), 0); ImageDraw.Draw(mask).rectangle([cx, cy, cx + cw - 1, cy + ch - 1], fill=255)
        img.paste(Image.alpha_composite(img, layer), (0, 0), mask)
    img.convert("RGB").save(out)
def metrics(out):
    f = [ImageFont.truetype(FONTS[b], 1000) for b in (0, 1)]; a, d = f[0].getmetrics()
    rows = [f"{a / 1000:.4f} {d / 1000:.4f}"] + [" ".join(f"{f[b].getlength(chr(i)) / 1000:.4f}" for i in range(256)) for b in (0, 1)]
    open(out, "w").write("\n".join(rows) + "\n")
if __name__ == "__main__":
    if sys.argv[1] == "--metrics": metrics(sys.argv[2]); sys.exit(0)
    w, h = int(sys.argv[3]), int(sys.argv[4]); render(sys.argv[1], sys.argv[2], w, h)
