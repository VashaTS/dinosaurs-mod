#!/usr/bin/env python3
"""T-Rex Skull and T-Rex Skull Helmet: 3D item models (block-model cuboids) and their 16x16 textures.

The helmet's Equippable component has no equipment asset, so vanilla's CustomHeadLayer draws this item
model on the head (like a carved pumpkin) for players, armor stands, zombies and so on. Model space:
16 units = the size of a worn pumpkin; the wearer's head sits inside the cranium, the snout points north (-z),
which is the wearer's front. Run from the repository root: python3 tools/gen_skull.py
"""
import json, os, random
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/textures/item")
MOD = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/models/item")

def noise_tex(name, base, var, seed, extra=None):
    rng = random.Random(seed)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 1.0 + rng.uniform(-var, var)
            im.putpixel((x, y), tuple(max(0, min(255, int(c * k))) for c in base) + (255,))
    if extra:
        extra(im, rng)
    im.save(os.path.join(TEX, name + ".png"))

def bone_marks(im, rng):
    # A few darker pits and hairline cracks, like old fossil bone.
    for _ in range(9):
        x, y = rng.randrange(16), rng.randrange(16)
        r, g, b, a = im.getpixel((x, y))
        im.putpixel((x, y), (int(r * 0.82), int(g * 0.8), int(b * 0.76), 255))
    x, y = rng.randrange(3, 13), 0
    while y < 16:
        r, g, b, a = im.getpixel((x, y))
        im.putpixel((x, y), (int(r * 0.86), int(g * 0.84), int(b * 0.8), 255))
        y += 1
        x = max(0, min(15, x + rng.choice((-1, 0, 0, 1))))

def tooth_grad(im, rng):
    for y in range(16):
        for x in range(16):
            r, g, b, a = im.getpixel((x, y))
            k = 0.86 + 0.14 * (y / 15)  # yellowish root at the top, white tip at the bottom
            im.putpixel((x, y), (int(r * (0.97 + 0.03 * y / 15)), int(g * k + 6), int(b * k), 255))

def netherite_lines(im, rng):
    for y in (3, 9, 14):
        for x in range(16):
            im.putpixel((x, y), (96, 86, 88, 255))

def stitches(im, rng):
    for x in range(1, 16, 3):
        im.putpixel((x, 3), (206, 170, 120, 255))
        im.putpixel((x, 12), (206, 170, 120, 255))

def textures():
    os.makedirs(TEX, exist_ok=True)
    noise_tex("t_rex_skull_bone", (224, 210, 172), 0.05, "bone", bone_marks)
    noise_tex("t_rex_skull_jaw", (198, 182, 144), 0.05, "jaw", bone_marks)
    noise_tex("t_rex_skull_dark", (40, 28, 22), 0.15, "dark")
    noise_tex("t_rex_skull_tooth", (250, 246, 232), 0.02, "tooth", tooth_grad)
    noise_tex("t_rex_skull_netherite", (70, 61, 63), 0.08, "neth", netherite_lines)
    noise_tex("t_rex_skull_leather", (132, 80, 48), 0.07, "leather", stitches)

def box(frm, to, tex):
    (x1, y1, z1), (x2, y2, z2) = frm, to
    dims = {"north": (x2 - x1, y2 - y1), "south": (x2 - x1, y2 - y1), "east": (z2 - z1, y2 - y1),
            "west": (z2 - z1, y2 - y1), "up": (x2 - x1, z2 - z1), "down": (x2 - x1, z2 - z1)}
    faces = {}
    for i, (face, (w, h)) in enumerate(dims.items()):
        w, h = min(16, w), min(16, h)
        ou = (int(abs(x1 * 3 + z1 * 5 + i * 7)) % 16)
        ov = (int(abs(y1 * 5 + z2 * 3 + i * 11)) % 16)
        u0, v0 = min(ou, 16 - w), min(ov, 16 - h)
        faces[face] = {"uv": [round(u0, 3), round(v0, 3), round(u0 + w, 3), round(v0 + h, 3)], "texture": "#" + tex}
    return {"from": list(frm), "to": list(to), "faces": faces}

def skull_elements():
    e = []
    B, J, D, T = "bone", "jaw", "dark", "tooth"
    # Cranium around the wearer's head (hat layer included): bone above, the jaw-coloured mandible hinge below.
    e.append(box((-0.2, 6, 0.4), (16.2, 16.5, 17), B))
    e.append(box((-0.4, 1, 0.3), (16.4, 6, 16.5), J))
    e.append(box((3, 16.5, 3), (13, 17.5, 15), B))           # sagittal crest
    e.append(box((2, 5, 17), (14, 15, 18.5), B))             # occiput
    e.append(box((-1.2, 6, 3), (-0.2, 9, 13), J))            # cheekbones (jugal)
    e.append(box((16.2, 6, 3), (17.2, 9, 13), J))
    # Brow horns above the eyes (lacrimal bosses).
    e.append(box((0.5, 15, -0.5), (5, 18, 4), B))
    e.append(box((11, 15, -0.5), (15.5, 18, 4), B))
    # Eye sockets (keyhole shaped) and the big opening behind them: dark patches a hair outside the faces.
    e.append(box((1, 11.5, 0.3), (4.2, 14.5, 0.4), D))
    e.append(box((11.8, 11.5, 0.3), (15, 14.5, 0.4), D))
    for x0, x1 in ((-0.3, -0.2), (16.2, 16.3)):
        e.append(box((x0, 11.5, 1.2), (x1, 14.5, 4.2), D))
        e.append(box((x0, 10, 2), (x1, 11.5, 3.4), D))
        e.append(box((x0, 9.5, 7), (x1, 13.5, 11), D))
    # Upper snout, stepped nose ridge rising into the forehead, and the tip.
    e.append(box((3, 6.5, -11), (13, 14, 1), B))
    e.append(box((5, 14, -9.5), (11, 15, 1), B))
    e.append(box((5.5, 15, -3.5), (10.5, 16, 1), B))
    e.append(box((4, 7, -13), (12, 13, -11), B))
    # Antorbital openings on the snout sides and nostrils on the tip.
    e.append(box((2.9, 9.5, -7.5), (3, 12.5, -2), D))
    e.append(box((13, 9.5, -7.5), (13.1, 12.5, -2), D))
    e.append(box((5, 11, -13.1), (6.5, 12.2, -13), D))
    e.append(box((9.5, 11, -13.1), (11, 12.2, -13), D))
    # Open mouth: dark inside between the jaws.
    e.append(box((4, 4.5, -11.5), (12, 6.5, 1.5), D))
    # Lower jaw: front under the snout, side bars running back to the hinge.
    e.append(box((3.5, 2, -10.5), (12.5, 4.5, 3), J))
    e.append(box((4.5, 2.2, -12), (11.5, 4.3, -10.5), J))
    e.append(box((3.1, 1.6, -9), (3.6, 4.7, 1), J))
    e.append(box((12.4, 1.6, -9), (12.9, 4.7, 1), J))
    # Teeth: long upper teeth of different lengths along the snout edge, shorter lower teeth pointing up.
    for z, length in ((-10.5, 2.0), (-8, 2.5), (-5.5, 2.3), (-3, 1.9), (-0.5, 1.5)):
        e.append(box((3.2, 6.5 - length, z), (4.2, 6.6, z + 1), T))
        e.append(box((11.8, 6.5 - length, z), (12.8, 6.6, z + 1), T))
    for x in (5, 7.5, 10):
        e.append(box((x, 5, -12.6), (x + 1, 7.1, -11.6), T))
    for z in (-9.2, -6.7, -4.2, -1.7):
        e.append(box((4, 4.5, z), (4.9, 5.7, z + 0.9), T))
        e.append(box((11.1, 4.5, z), (12, 5.7, z + 0.9), T))
    return e

def helmet_elements():
    e = skull_elements()
    N, L = "netherite", "leather"
    # Netherite crest plate and brow caps, and a leather strap around the back of the skull.
    e.append(box((2.5, 17.5, 2.5), (13.5, 18.5, 15.5), N))
    e.append(box((0.3, 18, -0.7), (5.2, 18.8, 4.2), N))
    e.append(box((10.8, 18, -0.7), (15.7, 18.8, 4.2), N))
    e.append(box((-0.4, 3, 9), (16.4, 5, 17.3), L))
    e.append(box((1.7, 3, 18.5), (14.3, 5, 18.8), L))
    return e

TEXTURES = {k: f"dinosaurs:item/t_rex_skull_{k}" for k in ("bone", "jaw", "dark", "tooth", "netherite", "leather")}

DISPLAY = {
    "gui": {"rotation": [15, 250, 0], "translation": [-2.6, 0.5, 0], "scale": [0.44, 0.44, 0.44]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "on_shelf": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [1, 2, -1], "scale": [0.22, 0.22, 0.22]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [1, 2, -1], "scale": [0.22, 0.22, 0.22]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}

def model(elements, textures):
    tex = {k: TEXTURES[k] for k in textures}
    tex["particle"] = TEXTURES["bone"]
    return {"gui_light": "front", "textures": tex, "display": DISPLAY, "elements": elements}

def main():
    textures()
    os.makedirs(MOD, exist_ok=True)
    for name, elems, texs in (("t_rex_skull", skull_elements(), ("bone", "jaw", "dark", "tooth")),
                              ("t_rex_skull_helmet", helmet_elements(), ("bone", "jaw", "dark", "tooth", "netherite", "leather"))):
        with open(os.path.join(MOD, name + ".json"), "w") as f:
            json.dump(model(elems, texs), f, indent=1)
            f.write("\n")
    print("ok")

if __name__ == "__main__":
    main()
