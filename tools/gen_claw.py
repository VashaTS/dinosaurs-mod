#!/usr/bin/env python3
"""Dinosaur Claw item texture (16x16): a curved bone claw blade (like a raptor's sickle claw) on a netherite guard and
grip, with a bone pommel. Palettes: the mod's bone colours (Dinosaur Armor) and vanilla's netherite sword colours."""
import math, os
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/textures/item/dinosaur_claw.png")

BONE = [(246, 240, 216), (232, 222, 186), (214, 203, 167), (190, 180, 148), (158, 150, 122), (110, 94, 66)]
OUTLINE = (44, 36, 40)
DARK = (22, 8, 20)
NETH = {"h": (134, 123, 134), "m": (112, 103, 112), "f": (93, 86, 93), "e": (81, 68, 78), "d": (59, 57, 59),
        "g": (50, 39, 39), "c": (35, 16, 18), "a": (74, 41, 64)}

# Netherite guard, grip and the bone pommel (x, y): key. Bone keys are digits (index into BONE).
HILT = """
................
................
................
................
................
................
................
................
.cc.............
.chmc...........
..cemhcc........
...cfmhec.......
....cdgc........
...cgdc.........
.12gc...........
1234............
"""

def blade():
    ca, ra, cb, rb = (11.5, 9.0), 8.0, (13.8, 10.6), 7.3
    px = {}
    for y in range(16):
        for x in range(16):
            p = (x + 0.5, y + 0.5)
            if not (p[1] < 10.5 and p[0] > 5 and p[0] < 15 and not (p[0] > 12.5 and p[1] > 4.5)):
                continue
            da, db = math.dist(p, ca), math.dist(p, cb)
            if da <= ra and db > rb:
                outer = ra - da          # distance to the convex (back) edge
                inner = db - rb          # distance to the concave (cutting) edge
                t = outer / (outer + inner + 1e-6)
                shade = 0 if t < 0.3 else 1 if t < 0.55 else 2 if t < 0.8 else 3
                if p[1] > 7.0:           # darker toward the root, where it sits in the guard
                    shade = min(5, shade + 1)
                px[(x, y)] = BONE[shade]
    # growth ridges across the claw (bony look)
    for q in [(6, 6), (7, 6), (8, 3), (9, 3)]:
        if q in px:
            px[q] = BONE[4]
    return px

def main():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    b = blade()
    for (x, y), c in b.items():
        img.putpixel((x, y), c + (255,))
    # outline around the blade
    for (x, y) in list(b):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in b and img.getpixel(q)[3] == 0:
                img.putpixel(q, OUTLINE + (255,))
    rows = HILT.strip("\n").split("\n")
    for y, row in enumerate(rows):
        for x, k in enumerate(row):
            if k == ".":
                continue
            img.putpixel((x, y), (BONE[int(k)] if k.isdigit() else NETH[k]) + (255,))
    img.save(OUT)
    big = img.resize((256, 256), Image.NEAREST)
    big.save("/workspace/dino-claw/texture-16x.png")
    print("wrote", OUT)

if __name__ == "__main__":
    main()
