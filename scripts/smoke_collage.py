#!/usr/bin/env python3
"""Collage of the run-time-imported stock APKs: build/smoke-apk/<name>/{00_first_frame,01_tap}.png + timing captions."""
import os, sys
from PIL import Image, ImageDraw
root = sys.argv[1] if len(sys.argv) > 1 else "build/smoke-apk"
out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(root, "collage.png")
tiles = []
for name in sorted(os.listdir(root)):
    d = os.path.join(root, name)
    if not os.path.isdir(d): continue
    res = open(os.path.join(d, "result.txt")).read().strip() if os.path.exists(os.path.join(d, "result.txt")) else "no result"
    tim = {}
    if os.path.exists(os.path.join(d, "timing.txt")):
        for l in open(os.path.join(d, "timing.txt")):
            if "=" in l: k, v = l.strip().split("=", 1); tim[k] = v
    for shot in ("00_first_frame.png", "01_tap.png"):
        p = os.path.join(d, shot)
        if os.path.exists(p):
            cap = f"{name} {shot[:-4]}  {res.split()[0]}  cold={tim.get('cold_start_process_to_first_frame_on_screen_ms','?')}ms"
            tiles.append((Image.open(p).convert("RGB"), cap))
if not tiles: print("no screenshots"); sys.exit(0)
W = 300; H = max(int(t.height * W / t.width) for t, _ in tiles)
im = Image.new("RGB", (W * len(tiles), H + 40), "white"); dr = ImageDraw.Draw(im)
for i, (t, cap) in enumerate(tiles):
    im.paste(t.resize((W, int(t.height * W / t.width))), (i * W, 40)); dr.text((i * W + 4, 4), cap[:48], fill="black"); dr.text((i * W + 4, 20), cap[48:96], fill="black")
im.save(out); print("collage", out, im.size)
