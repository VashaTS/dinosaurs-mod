#!/usr/bin/env python3
"""Dinosaur Armor textures (1.5.0): worn layers, item icons, the Dinosaur Upgrade template and the smithing-slot icons.

Bone-white plates (ribs on the chest, vertebrae down the back and the legs, knee caps, toe claws) on a dark netherite frame.
The vanilla netherite textures are used only as the shape/UV mask, so every layer lines up with the vanilla models.
Usage: tools/gen_armor.py [minecraft client jar]  (default: the 26.3 clientOnly jar in .gradle/loom-cache)
"""
import glob, io, os, sys, zipfile
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
T = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/textures")

# Bone (from dinosaur_bones.png), darkest -> lightest, and the netherite accents.
BONE = [(110, 94, 66), (158, 150, 122), (190, 180, 148), (214, 203, 167), (227, 215, 176), (240, 233, 205)]
NETH = [(22, 17, 19), (44, 36, 40), (66, 52, 58), (90, 80, 88)]


def client_jar():
    if len(sys.argv) > 1:
        return sys.argv[1]
    jars = glob.glob(os.path.join(ROOT, ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-*/*/minecraft-clientOnly-*.jar"))
    return sorted(jars)[-1]


def vanilla(z, path):
    return Image.open(io.BytesIO(z.read("assets/minecraft/textures/" + path))).convert("RGBA")


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def recolor(im, ramp):
    """Map each opaque colour to the ramp by its brightness rank in the source image."""
    cols = sorted({im.getpixel((x, y))[:3] for x in range(im.width) for y in range(im.height) if im.getpixel((x, y))[3]}, key=lum)
    out = Image.new("RGBA", im.size)
    for y in range(im.height):
        for x in range(im.width):
            p = im.getpixel((x, y))
            if p[3]:
                i = cols.index(p[:3])
                c = ramp[min(len(ramp) - 1, i * len(ramp) // len(cols))]
                out.putpixel((x, y), c + (p[3],))
    return out


# Worn layers: the darkest two source shades stay netherite (outlines, gaps), the rest become bone.
WORN_RAMP = [NETH[0], NETH[1], BONE[0], BONE[1], BONE[2], BONE[3], BONE[3], BONE[4], BONE[5]]


def put(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height and im.getpixel((x, y))[3]:
        im.putpixel((x, y), c + (255,))


def humanoid(src):
    im = recolor(src, WORN_RAMP)
    # Chest front (x 20-27, y 20-31): collar band, ribs either side of the sternum, netherite between the ribs.
    for x in range(20, 28):
        put(im, x, 20, NETH[1])
        for y in range(21, 30):
            if x in (23, 24):
                put(im, x, y, BONE[5] if y % 2 else BONE[4])  # sternum
            elif y % 2:
                put(im, x, y, BONE[4] if x in (21, 22, 25, 26) else BONE[3])  # rib
            else:
                put(im, x, y, NETH[2] if x in (21, 22, 25, 26) else NETH[1])  # gap
        put(im, x, 30, NETH[2])
        put(im, x, 31, NETH[1])
    for y in range(21, 30, 2):
        put(im, 20, y, BONE[2]); put(im, 27, y, BONE[2])
    # Back (x 32-39): vertebrae down the middle, short ribs to the sides.
    for x in range(32, 40):
        put(im, x, 20, NETH[1])
        for y in range(21, 31):
            if x in (35, 36):
                put(im, x, y, BONE[5] if y % 3 else NETH[1])
            elif y % 3 == 1:
                put(im, x, y, BONE[3])
            else:
                put(im, x, y, NETH[2] if x in (34, 37) else NETH[1])
        put(im, x, 31, NETH[1])
    # Sides of the torso (x 16-19, 28-31): rib ends.
    for x in list(range(16, 20)) + list(range(28, 32)):
        for y in range(20, 32):
            put(im, x, y, BONE[3] if y % 2 else NETH[1])
    # Shoulder pads (arm x 40-55, y 16-25): bone with a netherite rim and a dark spike line.
    for x in range(40, 56):
        for y in range(16, 26):
            if not im.getpixel((x, y))[3]:
                continue
            put(im, x, y, BONE[4] if (x + y) % 4 else BONE[3])
        put(im, x, 25, NETH[1]); put(im, x, 24, NETH[2])
    for x in (45, 46, 53, 54):
        put(im, x, 20, NETH[2]); put(im, x, 21, NETH[1])
    # Boots (legs x 0-15, y 26-31): bone shin plates, netherite sole, pale toe claws at the front.
    for x in range(0, 16):
        for y in range(26, 32):
            if im.getpixel((x, y))[3]:
                put(im, x, y, BONE[3] if y in (27, 29) else BONE[4])
        put(im, x, 26, NETH[1])
        put(im, x, 31, NETH[0])
    for x in (4, 6):
        put(im, x, 30, BONE[5]); put(im, x + 1, 30, NETH[2])
    for x in (4, 5, 6, 7):
        put(im, x, 28, NETH[2])
    return im


def leggings(src):
    im = recolor(src, WORN_RAMP)
    # Legs (x 0-15, y 16-28): bone segments like vertebrae, knee caps, netherite joints.
    for x in range(0, 16):
        for y in range(16, 29):
            if not im.getpixel((x, y))[3]:
                continue
            seg = (y - 20) % 4
            put(im, x, y, NETH[1] if seg == 3 else (BONE[4] if seg in (0, 1) else BONE[3]))
    for x in (5, 6):  # knee caps (front of the leg)
        put(im, x, 23, BONE[5]); put(im, x, 24, BONE[5])
    for x in (4, 7):
        put(im, x, 24, BONE[2])
    for y in range(16, 20):  # leg tops
        for x in range(4, 12):
            put(im, x, y, NETH[2])
    # Waist (body x 16-39, y 27-31): netherite belt with a bone buckle and pelvis plates.
    for x in range(16, 40):
        put(im, x, 27, NETH[1]); put(im, x, 28, NETH[2])
        for y in range(29, 32):
            put(im, x, y, BONE[4] if (x % 4) else NETH[1])
    for x in (23, 24):
        put(im, x, 27, BONE[5]); put(im, x, 28, BONE[5])
    return im


def item_icon(src, ribs=False, rows=()):
    """Inventory icon from the netherite one: dark outline kept, body in bone, a few netherite bands."""
    ramp = [NETH[0], NETH[1], BONE[0], BONE[1], BONE[2], BONE[3], BONE[4], BONE[5]]
    im = recolor(src, ramp)
    for y in rows:
        for x in range(16):
            p = im.getpixel((x, y))
            if p[3] and p[:3] not in (NETH[0],):
                im.putpixel((x, y), NETH[2] + (255,))
    if ribs:
        for y in (8, 10, 12):
            for x in range(16):
                p = im.getpixel((x, y))
                if p[3] and p[:3] in BONE[2:] and x not in (7, 8):
                    im.putpixel((x, y), NETH[1] + (255,))
        for y in range(5, 14):
            for x in (7, 8):
                p = im.getpixel((x, y))
                if p[3] and p[:3] != NETH[0]:
                    im.putpixel((x, y), BONE[5] + (255,))
    return im


def template(src, claw):
    """Like the netherite upgrade (a plate with an arrow): a bone plate with a dark netherite rim and a raptor claw."""
    cols = sorted({src.getpixel((x, y))[:3] for x in range(16) for y in range(16) if src.getpixel((x, y))[3]}, key=lum)
    out = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if not p[3]:
                continue
            r, g, b = p[:3]
            if b > r:  # the cyan arrow -> netherite
                c = NETH[1] if lum(p) < 140 else NETH[2]
            else:
                i = cols.index(p[:3]) * 6 // len(cols)
                c = [NETH[0], BONE[0], BONE[1], BONE[2], BONE[3], BONE[4]][i]
            out.putpixel((x, y), c + (255,))
    return out


def silhouette(icon):
    """Empty smithing-slot icon: the item's shape in the vanilla slot grey (85, 85, 85)."""
    out = Image.new("RGBA", icon.size)
    for y in range(icon.height):
        for x in range(icon.width):
            if icon.getpixel((x, y))[3] > 0:
                out.putpixel((x, y), (85, 85, 85, 255))
    return out


def save(im, rel):
    path = os.path.join(T, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)
    print("wrote", os.path.relpath(path, ROOT))


def main():
    with zipfile.ZipFile(client_jar()) as z:
        eq = "entity/equipment/"
        save(humanoid(vanilla(z, eq + "humanoid/netherite.png")), eq + "humanoid/dinosaur.png")
        save(leggings(vanilla(z, eq + "humanoid_leggings/netherite.png")), eq + "humanoid_leggings/dinosaur.png")
        save(recolor(vanilla(z, eq + "humanoid_baby/netherite.png"), WORN_RAMP), eq + "humanoid_baby/dinosaur.png")
        save(item_icon(vanilla(z, "item/netherite_chestplate.png"), ribs=True), "item/dinosaur_chestplate.png")
        save(item_icon(vanilla(z, "item/netherite_leggings.png"), rows=(2, 7, 11)), "item/dinosaur_leggings.png")
        save(item_icon(vanilla(z, "item/netherite_boots.png"), rows=(9,)), "item/dinosaur_boots.png")
        save(template(vanilla(z, "item/netherite_upgrade_smithing_template.png"), None), "item/dinosaur_upgrade_smithing_template.png")
    for name in ("megalodon_tooth", "parasaurolophus_crest", "raptor_claw"):
        icon = Image.open(os.path.join(T, "item", name + ".png")).convert("RGBA")
        save(silhouette(icon), "gui/sprites/container/slot/" + name + ".png")


if __name__ == "__main__":
    main()
