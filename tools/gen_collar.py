#!/usr/bin/env python3
"""Dino Collar (1.7.0): a studded leather band with T-Rex tooth spikes and an iron ring.

Writes
  textures/item/dino_collar.png       16x16 inventory icon (the collar seen from the front: a leather ring with three
                                      teeth on top, iron studs, an iron chain ring hanging below)
  textures/entity/t_rex_collar.png    32x32 texture of the collar model worn by a collared T. rex (DinoCollarModel)
Palettes: the mod's bone colours, a dark leather and vanilla iron colours."""
import os
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/dinosaurs")

BONE = [(246, 240, 216), (232, 222, 186), (214, 203, 167), (190, 180, 148), (158, 150, 122), (110, 94, 66)]
OUTLINE = (30, 18, 24)
LEATHER = {"L": (139, 88, 52), "l": (112, 68, 38), "k": (82, 48, 27), "s": (176, 128, 84), "r": (150, 40, 32), "R": (186, 62, 46)}
IRON = {"i": (216, 216, 216), "m": (165, 165, 165), "d": (110, 110, 110), "x": (72, 72, 72)}

# Front view: a leather ring seen slightly from above, iron studs and a ring on the front, a red stripe, and three
# T-Rex teeth hanging from the front of the band. Digits: BONE shades; letters: LEATHER / IRON keys; "." transparent.
ICON = """
................
...sLLLLLLLLs...
..Lkk.....kkL...
.Lk.........kL..
sL...........Ls.
L.............L.
L.............L.
sl...........ls.
.Ll.........lL..
.sLiLLimiLLiLs..
..sLRrRrRrRLs...
...l12l12l12l...
....12.12.12....
....1..1..1.....
....0..0..0.....
................
"""

def icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(ICON.strip("\n").split("\n")):
        for x, k in enumerate(row):
            if k == ".":
                continue
            c = BONE[int(k)] if k.isdigit() else LEATHER.get(k) or IRON[k]
            img.putpixel((x, y), c + (255,))
    filled = {(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3]}
    for (x, y) in filled:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in filled and img.getpixel(q)[3] == 0:
                img.putpixel(q, OUTLINE + (255,))
    return img

def box_faces(u, v, w, h, d):
    """Vanilla cube UV layout: returns {face: (x0, y0, x1, y1)} (exclusive ends)."""
    return {
        "top": (u + d, v, u + d + w, v + d), "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "east": (u, v + d, u + d, v + d + h), "north": (u + d, v + d, u + d + w, v + d + h),
        "west": (u + d + w, v + d, u + 2 * d + w, v + d + h), "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }

def fill(img, rect, color):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), color + (255,))

def entity():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    # Band 7 x 3 x 7: leather sides with a red middle stripe and iron studs, darker top and bottom.
    band = box_faces(0, 0, 7, 3, 7)
    for face, rect in band.items():
        x0, y0, x1, y1 = rect
        if face in ("top", "bottom"):
            # Only the rim: the inside is the neck (a full face would look like a lid on the neck).
            for y in range(y0, y1):
                for x in range(x0, x1):
                    if x in (x0, x1 - 1) or y in (y0, y1 - 1):
                        img.putpixel((x, y), LEATHER["k"] + (255,))
            continue
        for y in range(y0, y1):
            for x in range(x0, x1):
                row = y - y0
                c = LEATHER["l"] if row != 1 else LEATHER["r"]
                if row == 1 and (x - x0) % 3 == 1:
                    c = IRON["i"]
                if row == 0:
                    c = LEATHER["L"]
                img.putpixel((x, y), c + (255,))
    # Side spikes 2 x 1 x 1: T-Rex teeth (ivory, darker root).
    for v in (10, 14):
        for face, rect in box_faces(0, v, 2, 1, 1).items():
            fill(img, rect, BONE[1] if face in ("top", "north", "south") else BONE[3])
    # Iron ring 2 x 3 x 1.
    for face, rect in box_faces(8, 10, 2, 3, 1).items():
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                edge = face in ("north", "south") and (y - y0) == 1 and (x1 - x0) > 1
                img.putpixel((x, y), (IRON["x"] if edge else IRON["m"] if (x + y) % 2 else IRON["i"]) + (255,))
    return img

if __name__ == "__main__":
    icon().save(os.path.join(A, "textures/item/dino_collar.png"))
    entity().save(os.path.join(A, "textures/entity/t_rex_collar.png"))
    icon().resize((64, 64), Image.NEAREST).save(os.path.join(ROOT, "docs/icons/dino_collar.png"))
    print("ok")
