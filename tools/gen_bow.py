#!/usr/bin/env python3
"""Dinosaur Bones and Dinosaur Bone Bow textures (16x16).

The bow is repainted from a bow silhouette: limbs in fossil-bone colours with dark outline, a netherite grip
and netherite limb tips, string unchanged. Needs the vanilla bow textures as shape templates:
python3 tools/gen_bow.py /path/to/assets/minecraft/textures/item
"""
import math, os, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/textures/item")

WOOD_TO_BONE = {
    (40, 30, 11): (88, 76, 58),
    (73, 54, 21): (150, 136, 104),
    (104, 78, 30): (200, 188, 152),
    (137, 103, 39): (238, 230, 202),
}
GRIP_TO_NETHERITE = {(107, 107, 107): (78, 68, 70), (150, 150, 150): (112, 100, 102)}
TIP = (58, 50, 52)

def bow(src, dst):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    bone = []
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if (r, g, b) in WOOD_TO_BONE:
                px[x, y] = WOOD_TO_BONE[(r, g, b)] + (255,)
                bone.append((x, y))
            elif (r, g, b) in GRIP_TO_NETHERITE:
                px[x, y] = GRIP_TO_NETHERITE[(r, g, b)] + (255,)
    # Netherite caps on the two limb ends: the bone pixels farthest up-right and down-left.
    top = max(bone, key=lambda p: (p[0] - p[1], p[0]))
    low = min(bone, key=lambda p: (p[0] - p[1], -p[1]))
    for (cx, cy) in (top, low):
        for (x, y) in bone:
            if abs(x - cx) + abs(y - cy) <= 1:
                px[x, y] = TIP + (255,)
    im.save(dst)

def dinosaur_bones(dst):
    """Two big crossed fossil bones."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    def inside_bone(x, y, x0, y0, x1, y1):
        dx, dy = x1 - x0, y1 - y0
        L = math.hypot(dx, dy)
        t = max(0.0, min(1.0, ((x - x0) * dx + (y - y0) * dy) / (L * L)))
        px, py = x0 + t * dx, y0 + t * dy
        if math.hypot(x - px, y - py) <= 1.15:
            return True
        for ex, ey in ((x0, y0), (x1, y1)):
            ux, uy = dx / L, dy / L
            nx, ny = -uy, ux
            for s in (-1, 1):
                kx, ky = ex + s * nx * 1.3, ey + s * ny * 1.3
                if math.hypot(x - kx, y - ky) <= 1.45:
                    return True
        return False
    bones = [(3.0, 3.5, 12.5, 12.5), (12.5, 3.5, 3.5, 12.5)]
    mask = {}
    for y in range(16):
        for x in range(16):
            for i, b in enumerate(bones):
                if inside_bone(x + 0.5, y + 0.5, *b):
                    mask[(x, y)] = i  # the second bone is drawn on top
    for (x, y), i in mask.items():
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        light = 1.06 - 0.022 * (x + y) / 2 + (0.05 if i == 1 else 0)
        base = (226, 214, 176)
        if edge:
            col = (110, 94, 66)
        else:
            col = tuple(max(0, min(255, int(c * light))) for c in base)
            if (x * 7 + y * 3) % 11 == 0:
                col = tuple(int(c * 0.86) for c in col)  # fossil pits
        im.putpixel((x, y), col + (255,))
    im.save(dst)

def main():
    src = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    bow(os.path.join(src, "bow.png"), os.path.join(OUT, "dinosaur_bone_bow.png"))
    for i in range(3):
        bow(os.path.join(src, f"bow_pulling_{i}.png"), os.path.join(OUT, f"dinosaur_bone_bow_pulling_{i}.png"))
    dinosaur_bones(os.path.join(OUT, "dinosaur_bones.png"))
    sheet = Image.new("RGBA", (5 * 144, 144), (90, 90, 90, 255))
    for i, n in enumerate(["dinosaur_bones", "dinosaur_bone_bow", "dinosaur_bone_bow_pulling_0", "dinosaur_bone_bow_pulling_1", "dinosaur_bone_bow_pulling_2"]):
        sheet.alpha_composite(Image.open(os.path.join(OUT, n + ".png")).resize((128, 128), Image.NEAREST), (i * 144 + 8, 8))
    sheet.save("/tmp/bow-sheet.png")
    print("ok")

if __name__ == "__main__":
    main()
