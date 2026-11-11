#!/usr/bin/env python3
"""Generates dinosaur model layer Java code, entity textures, spawn egg textures and the mod icon.

Run from the repository root: python3 tools/gen_assets.py
Each species is described as a list of parts (boxes in Minecraft model pixels, y axis pointing down,
y=24 is the ground, -z is the front). UVs are packed automatically and the texture is painted per box
role, so the Java geometry and the PNG always match.
"""
import math, os, random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA_OUT = os.path.join(ROOT, "src/client/java/net/vsh33/dinosaurs/client/DinoModelLayers.java")
TEX_DIR = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/textures/entity")
EGG_DIR = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/textures/item")

def P(name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0), boxes=()):
    return dict(name=name, parent=parent, pivot=pivot, rot=rot, boxes=list(boxes))

def B(x, y, z, w, h, d, role="skin", eyes=False, teeth=False):
    return dict(o=(x, y, z), s=(w, h, d), role=role, eyes=eyes, teeth=teeth)

def mirror_x(boxes):
    out = []
    for b in boxes:
        x, y, z = b["o"]; w = b["s"][0]
        nb = dict(b); nb["o"] = (-x - w, y, z); out.append(nb)
    return out

def legs_pair(prefix, px, py, pz, boxes, rot=(0, 0, 0)):
    return [P("right_" + prefix, None, (-px, py, pz), rot, boxes), P("left_" + prefix, None, (px, py, pz), rot, mirror_x(boxes))]

SPECIES = {}

# ---------------------------------------------------------------- Tyrannosaurus rex
theropod_leg = lambda: None
SPECIES["t_rex"] = dict(
    palette=dict(skin=(104, 88, 62), dark=(62, 50, 36), belly=(176, 160, 122), accent=(120, 70, 50), horn=(60, 54, 48), eye=(230, 180, 40), teeth=(240, 236, 220)),
    pattern="back_dark",
    parts=[
        P("body", None, (0, 9, 0), boxes=[B(-5, -6, -9, 10, 11, 18)]),
        *legs_pair("leg", 4.5, 11, 3, [B(-2.5, -2, -3, 5, 9, 6), B(-2, 7, -1, 4, 4, 4), B(-2.5, 11, -4, 5, 2, 6, "dark")]),
        *legs_pair("arm", 4.5, 9.5, -6, [B(-1, 0, -1, 2, 5, 2), B(-1, 5, -2, 2, 1, 2, "horn")]),
        P("tail", None, (0, 5, 9), boxes=[B(-4, -3, 0, 8, 7, 10)]),
        P("tail2", "tail", (0, 0.5, 10), boxes=[B(-2.5, -2, 0, 5, 5, 10)]),
        P("neck", None, (0, 4, -8), boxes=[B(-3, -4, -5, 6, 7, 6)]),
        P("head", "neck", (0, -2, -4), boxes=[B(-4, -5, -11, 8, 7, 11, eyes=True, teeth=True)]),
        P("jaw", "head", (0, 2, -1), boxes=[B(-3.5, 0, -10, 7, 3, 10, "belly", teeth=True)]),
    ])

# ---------------------------------------------------------------- Velociraptor (feathered)
SPECIES["velociraptor"] = dict(
    palette=dict(skin=(150, 104, 62), feather=(132, 86, 50), dark=(70, 44, 28), belly=(214, 190, 150), accent=(40, 34, 30), horn=(40, 34, 30), eye=(250, 200, 30), teeth=(240, 236, 220)),
    pattern="stripes",
    parts=[
        P("body", None, (0, 14, 0), boxes=[B(-2.5, -3, -5, 5, 6, 10, "feather")]),
        *legs_pair("leg", 2, 16, 1, [B(-1, -1, -2, 2, 4, 3, "feather"), B(-1, 3, -1, 2, 4, 2, "skin"), B(-1, 7, -3, 2, 1, 4, "dark"), B(-0.5, 6, -3, 1, 1, 1, "horn")]),
        *legs_pair("arm", 2.5, 13.5, -3, [B(-0.5, 0, -1, 1, 4, 2, "feather"), B(-0.5, 1, 0, 1, 4, 2, "accent")]),
        P("tail", None, (0, 12, 5), boxes=[B(-1.5, -1, 0, 3, 3, 8, "feather")]),
        P("tail2", "tail", (0, 0, 8), boxes=[B(-1, 0, 0, 2, 2, 8, "feather"), B(-2.5, 0.5, 3, 5, 1, 6, "accent")]),
        P("neck", None, (0, 12, -4), boxes=[B(-1.5, -5, -2, 3, 6, 3, "feather")]),
        P("head", "neck", (0, -5, -1), boxes=[B(-2, -3, -6, 4, 3, 6, eyes=True, teeth=True)]),
        P("jaw", "head", (0, 0, 0), boxes=[B(-1.5, 0, -6, 3, 1, 5, "belly")]),
    ])

# ---------------------------------------------------------------- Spinosaurus
SPECIES["spinosaurus"] = dict(
    palette=dict(skin=(78, 92, 84), dark=(46, 56, 52), belly=(170, 168, 140), accent=(176, 70, 52), horn=(50, 50, 46), eye=(220, 200, 60), teeth=(240, 236, 220)),
    pattern="back_dark",
    parts=[
        P("body", None, (0, 10, 0), boxes=[B(-4.5, -5, -10, 9, 10, 20), B(-0.5, -14, -8, 1, 9, 16, "accent")]),
        *legs_pair("leg", 4, 13, 3, [B(-2, -1, -3, 4, 7, 5), B(-1.5, 6, -1, 3, 3, 3), B(-2, 9, -3, 4, 2, 5, "dark")]),
        *legs_pair("arm", 4, 12, -8, [B(-1, 0, -1, 2, 7, 2), B(-1, 7, -2, 2, 1, 2, "horn")]),
        P("tail", None, (0, 7, 10), boxes=[B(-3, -3, 0, 6, 6, 12), B(-0.5, -6, 0, 1, 3, 12, "accent")]),
        P("tail2", "tail", (0, 0, 12), boxes=[B(-2, -3, 0, 4, 5, 12), B(-0.5, -7, 0, 1, 4, 10, "accent")]),
        P("neck", None, (0, 6, -9), boxes=[B(-2.5, -3, -6, 5, 6, 7)]),
        P("head", "neck", (0, -1, -5), boxes=[B(-2.5, -3, -6, 5, 5, 5, eyes=True), B(-1.5, -1, -15, 3, 3, 9, teeth=True)]),
        P("jaw", "head", (0, 2, -2), boxes=[B(-1.5, 0, -13, 3, 1, 13, "belly")]),
    ])

# ---------------------------------------------------------------- Triceratops
SPECIES["triceratops"] = dict(
    palette=dict(skin=(120, 106, 74), dark=(70, 62, 44), belly=(186, 172, 136), accent=(150, 92, 52), horn=(226, 214, 180), eye=(30, 24, 20), teeth=(240, 236, 220)),
    pattern="spots",
    parts=[
        P("body", None, (0, 12, 0), boxes=[B(-6, -6, -10, 12, 11, 21)]),
        *legs_pair("front_leg", 3.5, 15, -7, [B(-2, 0, -2, 4, 9, 4)]),
        *legs_pair("hind_leg", 4, 15, 7, [B(-2.5, 0, -2.5, 5, 9, 5)]),
        P("tail", None, (0, 9, 11), boxes=[B(-2.5, -2, 0, 5, 5, 9)]),
        P("tail2", "tail", (0, 0, 9), boxes=[B(-1.5, -1, 0, 3, 3, 7)]),
        P("head", None, (0, 11, -10), boxes=[B(-4, -4, -9, 8, 8, 9, eyes=True), B(-2, 0, -12, 4, 4, 3, "dark"), B(-8, -12, -2, 16, 11, 2, "accent"), B(-0.5, -2, -11, 1, 2, 1, "horn")]),
        P("horns", "head", (0, -3, -7), rot=(-0.5, 0, 0), boxes=[B(-3.5, -1, -8, 1, 1, 8, "horn"), B(2.5, -1, -8, 1, 1, 8, "horn")]),
    ])

# ---------------------------------------------------------------- Stegosaurus
plates = []
for i, (z, h) in enumerate([(-8, 4), (-4, 6), (0, 7), (4, 6), (8, 4)]):
    plates.append(B(-1.5 if i % 2 == 0 else 0.5, -6 - h, z - 1, 1, h, 3, "accent"))
SPECIES["stegosaurus"] = dict(
    palette=dict(skin=(98, 112, 70), dark=(58, 68, 40), belly=(176, 180, 130), accent=(178, 96, 48), horn=(220, 206, 170), eye=(30, 24, 20), teeth=(240, 236, 220)),
    pattern="spots",
    parts=[
        P("body", None, (0, 11, 0), boxes=[B(-5.5, -6, -10, 11, 11, 20), *plates]),
        *legs_pair("front_leg", 3.5, 15, -7, [B(-1.5, 0, -1.5, 3, 9, 3)]),
        *legs_pair("hind_leg", 3, 14, 7, [B(-2, 0, -2, 4, 10, 4)]),
        P("head", None, (0, 13, -10), boxes=[B(-1.5, -1.5, -5, 3, 3, 5), B(-2, -2, -10, 4, 4, 5, eyes=True)]),
        P("tail", None, (0, 8, 10), boxes=[B(-2.5, -2, 0, 5, 5, 9), B(-0.5, -5, 2, 1, 3, 3, "accent")]),
        P("tail2", "tail", (0, 1, 9), boxes=[B(-1.5, -1, 0, 3, 3, 9), B(-4.5, -1, 4, 3, 1, 1, "horn"), B(1.5, -1, 4, 3, 1, 1, "horn"), B(-4.5, -1, 7, 3, 1, 1, "horn"), B(1.5, -1, 7, 3, 1, 1, "horn")]),
    ])

# ---------------------------------------------------------------- Ankylosaurus
spikes = []
for z in (-6, 0, 5):
    spikes += [B(-9, -3, z, 2, 1, 2, "horn"), B(7, -3, z, 2, 1, 2, "horn")]
SPECIES["ankylosaurus"] = dict(
    palette=dict(skin=(118, 100, 78), dark=(64, 54, 42), belly=(170, 154, 124), accent=(86, 74, 58), horn=(206, 190, 150), eye=(30, 24, 20), teeth=(240, 236, 220)),
    pattern="scutes",
    parts=[
        P("body", None, (0, 15, 0), boxes=[B(-7, -5, -9, 14, 8, 18), B(-7.5, -6.5, -8.5, 15, 2, 17, "accent"), *spikes]),
        *legs_pair("front_leg", 5, 18, -6, [B(-2, 0, -2, 4, 6, 4)]),
        *legs_pair("hind_leg", 5, 18, 6, [B(-2, 0, -2, 4, 6, 4)]),
        P("head", None, (0, 15, -9), boxes=[B(-3.5, -3, -6, 7, 6, 6, eyes=True), B(-2, 0, -7, 4, 3, 1, "dark"), B(-4.5, -3, -2, 1, 1, 2, "horn"), B(3.5, -3, -2, 1, 1, 2, "horn")]),
        P("tail", None, (0, 13, 9), boxes=[B(-2, -2, 0, 4, 4, 8)]),
        P("tail2", "tail", (0, 0, 8), boxes=[B(-1, -1, 0, 2, 2, 8), B(-3, -2, 7, 6, 4, 4, "accent")]),
    ])

# ---------------------------------------------------------------- Brachiosaurus
SPECIES["brachiosaurus"] = dict(
    palette=dict(skin=(120, 122, 112), dark=(76, 78, 70), belly=(184, 184, 168), accent=(96, 110, 90), horn=(90, 90, 80), eye=(30, 24, 20), teeth=(240, 236, 220)),
    pattern="back_dark",
    parts=[
        P("body", None, (0, 10, 0), boxes=[B(-5, -6, -9, 10, 11, 18)]),
        *legs_pair("front_leg", 3.5, 13, -6, [B(-2, 0, -2, 4, 11, 4)]),
        *legs_pair("hind_leg", 3.5, 14, 6, [B(-2, 0, -2, 4, 10, 4)]),
        P("neck", None, (0, 6, -8), rot=(0.35, 0, 0), boxes=[B(-2, -20, -3, 4, 22, 4)]),
        P("head", "neck", (0, -20, -1), rot=(-0.35, 0, 0), boxes=[B(-2, -3, -6, 4, 4, 6, eyes=True), B(-1.5, -4, -3, 3, 1, 3, "accent")]),
        # Tail rests drooping ~20 degrees down (negative pitch = down in model space); DinoModel bobs it up and down.
        P("tail", None, (0, 6, 9), rot=(-0.35, 0, 0), boxes=[B(-2.5, -2, 0, 5, 5, 10)]),
        P("tail2", "tail", (0, 0.5, 10), rot=(-0.12, 0, 0), boxes=[B(-1.5, -1.5, 0, 3, 3, 10)]),
    ])

# ---------------------------------------------------------------- Parasaurolophus
SPECIES["parasaurolophus"] = dict(
    palette=dict(skin=(86, 110, 120), dark=(50, 66, 74), belly=(190, 196, 180), accent=(196, 120, 50), horn=(70, 64, 56), eye=(30, 24, 20), teeth=(240, 236, 220)),
    pattern="stripes",
    parts=[
        P("body", None, (0, 12, 0), boxes=[B(-4, -5, -7, 8, 9, 14)]),
        *legs_pair("leg", 3, 14, 3, [B(-2, -1, -2, 4, 6, 5), B(-1.5, 5, -1, 3, 3, 3), B(-2, 8, -3, 4, 2, 5, "dark")]),
        *legs_pair("arm", 4.5, 13, -5.5, [B(-1, 0, -1, 2, 6, 2)]),
        P("tail", None, (0, 9, 7), boxes=[B(-2.5, -2, 0, 5, 5, 10)]),
        P("tail2", "tail", (0, 0, 10), boxes=[B(-1.5, -1, 0, 3, 3, 8)]),
        P("neck", None, (0, 9, -6), rot=(0.3, 0, 0), boxes=[B(-1.5, -7, -3, 3, 8, 4)]),
        P("head", "neck", (0, -7, -1), rot=(-0.3, 0, 0), boxes=[B(-2, -3, -5, 4, 4, 5, eyes=True), B(-1.5, -1, -8, 3, 2, 3, "dark")]),
        P("crest", "head", (0, -2, -2), rot=(0.6, 0, 0), boxes=[B(-1, -1, 0, 2, 2, 11, "accent")]),
    ])

# ---------------------------------------------------------------- Megalodon
SPECIES["megalodon"] = dict(
    palette=dict(skin=(86, 98, 112), dark=(56, 64, 76), belly=(220, 224, 226), accent=(70, 80, 94), horn=(60, 60, 60), eye=(16, 16, 20), teeth=(246, 244, 236)),
    pattern="back_dark",
    parts=[
        P("body", None, (0, 16, 0), boxes=[B(-5, -6, -12, 10, 12, 24), B(-0.5, -12, -4, 1, 6, 7, "dark")]),
        P("head", "body", (0, 0, -12), boxes=[B(-4, -5, -8, 8, 9, 8, eyes=True, teeth=True)]),
        P("jaw", "head", (0, 4, -1), boxes=[B(-3.5, 0, -7, 7, 2, 7, "belly", teeth=True)]),
        P("right_fin", "body", (-5, 4, -4), rot=(0, 0, 0.6), boxes=[B(-8, 0, -2, 8, 1, 5, "dark")]),
        P("left_fin", "body", (5, 4, -4), rot=(0, 0, -0.6), boxes=[B(0, 0, -2, 8, 1, 5, "dark")]),
        P("tail", None, (0, 16, 12), boxes=[B(-3, -4, 0, 6, 8, 8)]),
        P("tail2", "tail", (0, 0, 8), boxes=[B(-1.5, -3, 0, 3, 6, 6), B(-0.5, -11, 4, 1, 8, 4, "dark"), B(-0.5, 3, 4, 1, 6, 4, "dark")]),
    ])

# ---------------------------------------------------------------- Pteranodon
SPECIES["pteranodon"] = dict(
    palette=dict(skin=(196, 186, 168), dark=(90, 70, 56), belly=(226, 220, 206), accent=(170, 64, 44), wing=(150, 104, 78), horn=(212, 168, 70), eye=(20, 16, 14), teeth=(240, 236, 220)),
    pattern="none",
    parts=[
        P("body", None, (0, 18, 0), boxes=[B(-2, -2, -4, 4, 4, 8)]),
        P("head", None, (0, 16, -4), boxes=[B(-1.5, -2, -3, 3, 3, 3, eyes=True), B(-0.5, -1, -11, 1, 2, 8, "horn"), B(-0.5, -4, -1, 1, 2, 7, "accent")]),
        P("right_wing", None, (-2, 17, -2), boxes=[B(-20, 0, -2, 20, 1, 7, "wing"), B(-20, -1, -2, 20, 1, 1, "skin")]),
        P("left_wing", None, (2, 17, -2), boxes=[B(0, 0, -2, 20, 1, 7, "wing"), B(0, -1, -2, 20, 1, 1, "skin")]),
        *legs_pair("leg", 1, 20, 2, [B(-0.5, 0, -0.5, 1, 4, 1, "dark")]),
    ])

EGGS = {  # base, spots
    "t_rex": ((104, 88, 62), (176, 160, 122)),
    "velociraptor": ((150, 104, 62), (40, 34, 30)),
    "spinosaurus": ((78, 92, 84), (176, 70, 52)),
    "triceratops": ((120, 106, 74), (226, 214, 180)),
    "stegosaurus": ((98, 112, 70), (178, 96, 48)),
    "ankylosaurus": ((86, 74, 58), (206, 190, 150)),
    "brachiosaurus": ((150, 152, 140), (90, 100, 84)),
    "parasaurolophus": ((86, 110, 120), (196, 120, 50)),
    "megalodon": ((70, 82, 98), (220, 224, 226)),
    "pteranodon": ((196, 186, 168), (170, 64, 44)),
}

# ----------------------------------------------------------------------------------------- packing
def pack(parts):
    boxes = [b for p in parts for b in p["boxes"]]
    for b in boxes:
        w, h, d = b["s"]
        b["uv_size"] = (2 * (w + d), d + h)
    for size in (64, 128, 256):
        order = sorted(boxes, key=lambda b: -b["uv_size"][1])
        x = y = row_h = 0; ok = True
        for b in order:
            bw, bh = b["uv_size"]
            if x + bw > size:
                x = 0; y += row_h; row_h = 0
            if bw > size or y + bh > size:
                ok = False; break
            b["uv"] = (x, y); x += bw; row_h = max(row_h, bh)
        if ok:
            return size, size
    raise SystemExit("texture too large")

def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)

def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))

def paint(img, b, pal, pattern, rng):
    w, h, d = b["s"]; u, v = b["uv"]; role = b["role"]
    base = pal.get(role, pal["skin"]); belly = pal["belly"]
    px = img.load()
    def put(x, y, c):
        n = rng.randint(-7, 7)
        px[x, y] = (max(0, min(255, c[0] + n)), max(0, min(255, c[1] + n)), max(0, min(255, c[2] + n)), 255)
    countershade = role in ("skin", "feather", "dark") and pattern != "none"
    # faces: (u0, v0, width, height, kind)
    faces = [
        (u + d, v, w, d, "top"), (u + d + w, v, w, d, "bottom"),
        (u, v + d, d, h, "side_w"), (u + d, v + d, w, h, "front"),
        (u + d + w, v + d, d, h, "side_e"), (u + 2 * d + w, v + d, w, h, "back"),
    ]
    for fx, fy, fw, fh, kind in faces:
        for yy in range(fh):
            for xx in range(fw):
                c = base
                if kind == "bottom" and countershade:
                    c = belly
                elif kind not in ("top", "bottom") and countershade and fh >= 3:
                    t = yy / max(1, fh - 1)
                    if t > 0.6:
                        c = mix(base, belly, min(1.0, (t - 0.6) / 0.4) * 0.85)
                if role in ("skin", "feather") and kind == "top" and pattern in ("back_dark", "stripes"):
                    c = shade(c, 0.82)
                if pattern == "stripes" and role in ("skin", "feather") and kind != "bottom":
                    coord = yy if kind == "top" else xx
                    if coord % 4 == 0 and not (kind not in ("top",) and yy > fh * 0.65):
                        c = pal["dark"]
                if pattern == "spots" and role == "skin" and kind != "bottom" and rng.random() < 0.06:
                    c = pal["dark"]
                if pattern == "scutes" and role in ("accent",) and (xx % 3 == 0 or yy % 3 == 0):
                    c = shade(c, 0.75)
                if role == "wing":
                    c = shade(c, 0.9 + 0.2 * ((xx // 2) % 2))
                put(fx + xx, fy + yy, c)
    if b["eyes"]:
        ey = v + d + max(0, min(h - 2, 1))
        for ex in (u + d - 2, u + d + w + 1):
            if 0 <= ex < img.width:
                px[ex, ey] = pal["eye"] + (255,)
                if h >= 4:
                    px[ex, ey - 1 if ey - 1 >= v + d else ey] = shade(pal["dark"], 0.7) + (255,)
                    px[ex, ey] = pal["eye"] + (255,)
    if b["teeth"]:
        row = v + d + h - 1 if b["role"] != "belly" else v + d
        for xx in range(u, u + 2 * (w + d)):
            if (xx - u) % 2 == 0 and not (u + 2 * d + w <= xx < u + 2 * (d + w)):
                px[xx, row] = pal["teeth"] + (255,)

def fmt(f):
    s = ("%.4f" % f).rstrip("0").rstrip(".")
    if s in ("-0", ""): s = "0"
    return s + "F"

def java_for(name, parts, tw, th):
    m = "".join(x.capitalize() for x in name.split("_"))
    lines = [f"\tpublic static LayerDefinition create{m}() {{", "\t\tMeshDefinition mesh = new MeshDefinition();", "\t\tPartDefinition root = mesh.getRoot();"]
    var = {None: "root"}
    for p in parts:
        v = "p_" + p["name"]
        cubes = "CubeListBuilder.create()"
        for b in p["boxes"]:
            (x, y, z), (w, h, d) = b["o"], b["s"]
            cubes += f".texOffs({b['uv'][0]}, {b['uv'][1]}).addBox({fmt(x)}, {fmt(y)}, {fmt(z)}, {fmt(w)}, {fmt(h)}, {fmt(d)})"
        px_, py_, pz_ = p["pivot"]; rx, ry, rz = p["rot"]
        pose = f"PartPose.offset({fmt(px_)}, {fmt(py_)}, {fmt(pz_)})" if (rx, ry, rz) == (0, 0, 0) else \
            f"PartPose.offsetAndRotation({fmt(px_)}, {fmt(py_)}, {fmt(pz_)}, {fmt(rx)}, {fmt(ry)}, {fmt(rz)})"
        lines.append(f"\t\tPartDefinition {v} = {var[p['parent']]}.addOrReplaceChild(\"{p['name']}\", {cubes}, {pose});")
        var[p["name"]] = v
    lines.append(f"\t\treturn LayerDefinition.create(mesh, {tw}, {th});")
    lines.append("\t}")
    return "\n".join(lines)

def egg(base, spot, rng):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); px = img.load()
    for y in range(16):
        for x in range(16):
            # egg shape: ellipse, wider at bottom
            cx, cy = 7.5, 8.5
            ry = 6.5; rx = 4.6 if y < cy else 5.2
            if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                px[x, y] = base + (255,)
    outline = shade(base, 0.45)
    for y in range(16):
        for x in range(16):
            if px[x, y][3] and any(not (0 <= x + dx < 16 and 0 <= y + dy < 16) or px[x + dx, y + dy][3] == 0 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = outline + (255,)
    for _ in range(9):
        x, y = rng.randint(4, 11), rng.randint(4, 13)
        if px[x, y][3] and px[x, y][:3] != outline:
            px[x, y] = spot + (255,)
            if x + 1 < 16 and px[x + 1, y][3] and px[x + 1, y][:3] != outline:
                px[x + 1, y] = spot + (255,)
    hl = shade(base, 1.35)
    for x, y in ((5, 4), (5, 5), (6, 3)):
        if px[x, y][3]: px[x, y] = hl + (255,)
    return img

def main():
    os.makedirs(TEX_DIR, exist_ok=True); os.makedirs(EGG_DIR, exist_ok=True)
    methods = []
    for name, sp in SPECIES.items():
        rng = random.Random(name)
        tw, th = pack(sp["parts"])
        img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
        for p in sp["parts"]:
            for b in p["boxes"]:
                paint(img, b, sp["palette"], sp["pattern"], rng)
        img.save(os.path.join(TEX_DIR, name + ".png"))
        methods.append(java_for(name, sp["parts"], tw, th))
        egg(*EGGS[name], rng).save(os.path.join(EGG_DIR, name + "_spawn_egg.png"))
    java = """// GENERATED by tools/gen_assets.py - do not edit by hand.
package net.vsh33.dinosaurs.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

@SuppressWarnings("unused")
public final class DinoModelLayers {
	private DinoModelLayers() {
	}

""" + "\n\n".join(methods) + "\n}\n"
    with open(JAVA_OUT, "w") as f:
        f.write(java)
    # The mod icon (assets/dinosaurs/icon.png) is the Spinosaurus head render, same as the Modrinth logo; not generated here.
    print("ok")

if __name__ == "__main__":
    main()
