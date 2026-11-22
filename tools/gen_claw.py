#!/usr/bin/env python3
"""Dinosaur Claw (1.6.0): a clawed netherite gauntlet with bone knuckle plates and three sharp claws/teeth sticking out
of the knuckles.

Writes
  textures/item/dinosaur_claw.png        16x16 inventory icon (side view of the gloved fist, claws to the right)
  textures/item/dinosaur_claw_glove.png  16x16 texture sheet for the 3D held model
  models/item/dinosaur_claw_held.json    3D model worn on the fist in first and third person (items/dinosaur_claw.json
                                         picks the flat icon in GUI, ground and item frames, this model in the hands)
Palettes: the mod's bone colours (Dinosaur Armor), vanilla netherite colours and a dark leather."""
import json, math, os
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, "src/main/resources/assets/dinosaurs")

BONE = [(246, 240, 216), (232, 222, 186), (214, 203, 167), (190, 180, 148), (158, 150, 122), (110, 94, 66)]
OUTLINE = (30, 18, 24)
NETH = {"h": (134, 123, 134), "m": (112, 103, 112), "f": (93, 86, 93), "e": (81, 68, 78), "d": (59, 57, 59),
        "g": (50, 39, 39), "c": (35, 16, 18), "a": (74, 41, 64)}
LEATHER = {"L": (139, 88, 52), "l": (112, 68, 38), "k": (82, 48, 27), "s": (176, 128, 84)}

# Inventory icon: the gloved fist seen from the back, leather cuff at the bottom, netherite back plate, a row of bone
# knuckle plates, and three sharp claws growing up out of the knuckles, hooking to the right at the tips.
# Letters are NETH / LEATHER keys, digits BONE shades, "." transparent. A dark outline is added around everything.
ICON = """
......3..3..3...
.....12.12.12...
....02.02.02....
....02.02.02....
....02.02.02....
....13.13.13....
...4334334334...
..h2112112112d..
..hmmmmffffddc..
.lhmmmffffdddc..
.lhmmffffdddcc..
.kLhfffdddcccc..
..gggggggggggg..
...sLsLsLsLss...
...LLlLLlLLlL...
....kkkkkkkk....
"""

def icon():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rows = ICON.strip("\n").split("\n")
    for y, row in enumerate(rows):
        for x, k in enumerate(row):
            if k == ".":
                continue
            c = BONE[int(k)] if k.isdigit() else NETH.get(k) or LEATHER[k]
            img.putpixel((x, y), c + (255,))
    filled = {(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3]}
    for (x, y) in filled:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in filled and img.getpixel(q)[3] == 0:
                img.putpixel(q, OUTLINE + (255,))
    return img

# Texture sheet for the 3D model (u, v in pixels):
#   0-8 x 0-8   netherite plate (back of the hand)      8-16 x 0-8   dark leather (palm, fingers, cuff lining)
#   0-8 x 8-12  bone knuckle plate                      8-16 x 8-10  claw (base dark -> ivory -> sharp tip)
#   0-8 x 12-16 netherite band (cuff)                   8-16 x 10-12 claw tip (ivory -> dark point)
def glove_sheet():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    put = lambda x, y, c: img.putpixel((x, y), c + (255,))
    for y in range(8):
        for x in range(8):
            k = "h" if y == 0 or x == 0 else "c" if y == 7 or x == 7 else "m" if (x + y) % 5 == 0 else "f" if y < 4 else "d"
            put(x, y, NETH[k])
    for (x, y) in ((2, 2), (5, 2), (2, 5), (5, 5)):
        put(x, y, NETH["h"])  # rivets
    for y in range(8):
        for x in range(8, 16):
            k = "s" if y in (0,) else "k" if y == 7 or (y == 4 and x % 2 == 0) else "L" if (x + 2 * y) % 7 else "l"
            put(x, y, LEATHER[k])
    for y in range(8, 12):
        for x in range(8):
            put(x, y, BONE[1 if y == 8 else 2 if y < 11 else 4] if x % 4 else BONE[5])
    for y in range(8, 12):
        for x in range(8, 16):
            t = (x - 8) / 7.0
            if y < 10:
                shade = 4 if t < 0.15 else 2 if t < 0.4 else 1 if t < 0.7 else 0
                if y == 9:
                    shade = min(5, shade + 1)
            else:
                shade = 0 if t < 0.4 else 1 if t < 0.6 else 3 if t < 0.8 else 5
                if y == 11:
                    shade = min(5, shade + 1)
            put(x, y, BONE[shade])
    for y in range(12, 16):
        for x in range(8):
            put(x, y, NETH["h" if y == 12 else "e" if y < 15 else "c"] if x % 3 else NETH["g"])
    for y in range(12, 16):
        for x in range(8, 16):
            put(x, y, NETH["d" if (x + y) % 2 else "g"])
    return img

T = "#t"
def face(u0, v0, u1, v1):
    return {"uv": [u0, v0, u1, v1], "texture": T}
NETH_UV, LEATHER_UV, BONE_UV, CLAW_UV, TIP_UV, BAND_UV = (0, 0, 8, 8), (8, 0, 16, 8), (0, 8, 8, 12), (8, 8, 16, 10), (8, 10, 16, 12), (0, 12, 8, 16)

def box(fr, to, uv, rot=None, faces=None, name=None):
    """faces: dict of face -> uv rect overriding the default uv for that face."""
    f = {d: face(*((faces or {}).get(d, uv))) for d in ("north", "east", "south", "west", "up", "down")}
    e = {"from": [round(v, 3) for v in fr], "to": [round(v, 3) for v in to], "faces": f}
    if rot:
        e["rotation"] = rot
    if name:
        e["name"] = name
    return e

def held_model():
    # Model frame: the forearm runs along -Y into the cuff (y 0..4), the fist is y 4..10, the knuckles face +X and the
    # claws grow out of them along +X, curving down (-Y) at the tips. Z is across the fist (little finger to index).
    els = []
    els.append(box([4.5, 0, 4.5], [11.5, 4, 11.5], LEATHER_UV, faces={"north": BAND_UV, "south": BAND_UV, "east": BAND_UV, "west": BAND_UV}, name="cuff"))
    els.append(box([5, 4, 5], [10.5, 10, 11], NETH_UV, faces={"east": LEATHER_UV, "down": LEATHER_UV}, name="fist"))
    els.append(box([4.5, 5, 4.75], [9.5, 9.5, 11.25], NETH_UV, faces={"east": NETH_UV}, name="back_plate"))
    els.append(box([10.5, 7, 5], [11.5, 10, 11], BONE_UV, name="knuckles"))
    els.append(box([7, 3.5, 3.5], [10, 7, 5], LEATHER_UV, name="thumb"))
    for z0 in (5.35, 7.35, 9.35):
        w = 1.3
        zc = z0 + w / 2
        y0 = 8.6
        # base: 1.3 x 1.8, 3.5 long, straight out of the knuckle plate
        els.append(box([11.5, y0 - 0.9, z0], [15.0, y0 + 0.9, z0 + w], CLAW_UV, name="claw_base"))
        # middle: tilted 22.5 degrees down around the end of the base
        L2 = 3.5
        els.append(box([15.0, y0 - 0.65, zc - 0.5], [15.0 + L2, y0 + 0.65, zc + 0.5], CLAW_UV,
                       rot={"angle": -22.5, "axis": "z", "origin": [15.0, y0, zc]}, name="claw_mid"))
        a = math.radians(22.5)
        ex, ey = 15.0 + L2 * math.cos(a), y0 - L2 * math.sin(a)
        L3 = 3.0
        els.append(box([ex - 0.3, ey - 0.35, zc - 0.3], [ex + L3, ey + 0.35, zc + 0.3], TIP_UV,
                       rot={"angle": -45, "axis": "z", "origin": [ex, ey, zc]}, name="claw_tip"))
    return {
        "textures": {"t": "dinosaurs:item/dinosaur_claw_glove", "particle": "dinosaurs:item/dinosaur_claw"},
        "elements": els,
        "display": display(),
    }

# Display transforms. Vanilla applies translate, then rotationXYZ (Z first), then scale, with the model centred on
# (8, 8, 8). Rotation: vanilla's handheld (sword) rotation plus 45 degrees about Z, so the claws (+X) point along a
# sword blade's diagonal: forward out of the fist, the cuff (-Y) up the arm toward the elbow.
# Third person: ItemInHandLayer puts the item frame at the front-bottom edge of the arm, with item-frame axes
# (x, y, z) -> arm (-x, -z, -y) (arm space: y down the arm, -z forward, 1 unit = 1/16 block). The translation is
# solved so the knuckle end of the fist sits at the bottom of the arm, centred on it: the glove is worn on the hand.
FIST_END = (7.75, 10.0, 8.0)      # centre of the fist's knuckle/finger end (model)
THIRD_SCALE = 0.65                # fist 6 x 5.5 -> about 3.9 x 3.6, cuff 7 -> 4.6: a glove around the 4 x 4 arm
THIRD_TARGET_ARM = (0.0, 0.4, 2.0)  # where FIST_END goes, relative to the item frame origin, in arm space
# First person (no arm is drawn): the fist low on the right, seen over the back of the hand, the claws pointing up, left
# and forward (model +X -> (-0.5, 0.7, -0.6) on screen), the thumb side down-left. Solved from those directions
# (rotation applied Z first, then Y, then X) and tuned on screenshots.
FIRST = ([-67.28, 39.62, 128.23], [-0.3, 3.46, -1.31], 0.6)

def rot_xyz(r):
    import numpy as np
    x, y, z = (math.radians(v) for v in r)
    rx = np.array([[1, 0, 0], [0, math.cos(x), -math.sin(x)], [0, math.sin(x), math.cos(x)]])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rz = np.array([[math.cos(z), -math.sin(z), 0], [math.sin(z), math.cos(z), 0], [0, 0, 1]])
    return rx @ ry @ rz

def display():
    import numpy as np
    r3 = [0, -90, 55 + 45]
    ax, ay, az = THIRD_TARGET_ARM
    target = np.array([-ax, -az, -ay])          # arm -> item frame (inverse of (x, y, z) -> (-x, -z, -y))
    t3 = target - rot_xyz(r3) @ (THIRD_SCALE * (np.array(FIST_END) - 8))
    r1, t1, s1 = FIRST
    tr = lambda v: [round(float(x), 2) for x in v]
    return {"thirdperson_righthand": {"rotation": r3, "translation": tr(t3), "scale": [THIRD_SCALE] * 3},
            "firstperson_righthand": {"rotation": r1, "translation": tr(t1), "scale": [s1] * 3}}

def main():
    os.makedirs("/workspace/dino-claw", exist_ok=True)
    ic = icon()
    ic.save(os.path.join(A, "textures/item/dinosaur_claw.png"))
    ic.resize((256, 256), Image.NEAREST).save("/workspace/dino-claw/texture-16x.png")
    gs = glove_sheet()
    gs.save(os.path.join(A, "textures/item/dinosaur_claw_glove.png"))
    gs.resize((256, 256), Image.NEAREST).save("/workspace/dino-claw/glove-sheet-16x.png")
    with open(os.path.join(A, "models/item/dinosaur_claw_held.json"), "w") as f:
        json.dump(held_model(), f, indent=2)
        f.write("\n")
    icon64 = ic.resize((64, 64), Image.NEAREST)
    icon64.save(os.path.join(ROOT, "docs/icons/dinosaur_claw.png"))
    print("wrote dinosaur_claw icon, glove sheet and held model")

if __name__ == "__main__":
    main()
