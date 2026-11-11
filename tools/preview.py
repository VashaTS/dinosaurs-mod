#!/usr/bin/env python3
"""Software preview of the generated models (rest pose) -> /workspace/dino-previews/*.png"""
import math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_assets as G
from PIL import Image, ImageDraw
OUT = "/workspace/dino-previews"; os.makedirs(OUT, exist_ok=True)
G.main()  # ensure uv assigned / textures fresh
SCALE = {"t_rex": 2.0, "velociraptor": 0.8, "spinosaurus": 2.0, "triceratops": 1.6, "stegosaurus": 1.6, "ankylosaurus": 1.5, "brachiosaurus": 1.8, "parasaurolophus": 1.4, "megalodon": 2.0, "pteranodon": 1.4}

def rot(v, r):
    x, y, z = v; rx, ry, rz = r
    # MC applies Z then Y then X (ModelPart.translateAndRotate uses rotationZYX)
    c, s = math.cos(rz), math.sin(rz); x, y = x * c - y * s, x * s + y * c
    c, s = math.cos(ry), math.sin(ry); x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(rx), math.sin(rx); y, z = y * c - z * s, y * s + z * c
    return (x, y, z)

def world_tf(parts, name):
    chain = []
    by = {p["name"]: p for p in parts}
    p = by[name]
    while p:
        chain.append(p); p = by.get(p["parent"]) if p["parent"] else None
    def f(v):
        for q in chain:
            v = rot(v, q["rot"]); v = (v[0] + q["pivot"][0], v[1] + q["pivot"][1], v[2] + q["pivot"][2])
        return v
    return f

def render(name, sp, yaw):
    tex = Image.open(os.path.join(G.TEX_DIR, name + ".png")).convert("RGBA").load()
    quads = []
    for p in sp["parts"]:
        f = world_tf(sp["parts"], p["name"])
        for b in p["boxes"]:
            (x0, y0, z0), (w, h, d) = b["o"], b["s"]; u, v = b["uv"]
            # faces: (origin, axis_u, axis_v, n_u, n_v, tex_u0, tex_v0, normal)
            faces = [
                ((x0, y0, z0), (1, 0, 0), (0, 0, 1), w, d, u + d, v, (0, -1, 0)),            # top
                ((x0, y0 + h, z0), (1, 0, 0), (0, 0, 1), w, d, u + d + w, v, (0, 1, 0)),     # bottom
                ((x0, y0, z0), (1, 0, 0), (0, 1, 0), w, h, u + d, v + d, (0, 0, -1)),        # front
                ((x0 + w, y0, z0 + d), (-1, 0, 0), (0, 1, 0), w, h, u + 2 * d + w, v + d, (0, 0, 1)),  # back
                ((x0, y0, z0 + d), (0, 0, -1), (0, 1, 0), d, h, u, v + d, (-1, 0, 0)),       # west
                ((x0 + w, y0, z0), (0, 0, 1), (0, 1, 0), d, h, u + d + w, v + d, (1, 0, 0)),  # east
            ]
            for o, au, av, nu, nv, tu, tv, n in faces:
                nu_i, nv_i = max(1, int(math.ceil(nu))), max(1, int(math.ceil(nv)))
                su, sv = nu / nu_i, nv / nv_i
                light = {(0, -1, 0): 1.0, (0, 1, 0): 0.5, (0, 0, -1): 0.8, (0, 0, 1): 0.8, (-1, 0, 0): 0.65, (1, 0, 0): 0.65}[n]
                for i in range(nu_i):
                    for j in range(nv_i):
                        pts = []
                        for di, dj in ((0, 0), (1, 0), (1, 1), (0, 1)):
                            a, c = (i + di) * su, (j + dj) * sv
                            pts.append(f((o[0] + au[0] * a + av[0] * c, o[1] + au[1] * a + av[1] * c, o[2] + au[2] * a + av[2] * c)))
                        tx, ty = int(tu + i), int(tv + j)
                        try:
                            col = tex[tx, ty]
                        except IndexError:
                            col = (255, 0, 255, 255)
                        if col[3] == 0:
                            col = (255, 0, 255, 255)
                        quads.append((pts, tuple(int(cc * light) for cc in col[:3])))
    img = Image.new("RGB", (520, 420), (150, 200, 240)); dr = ImageDraw.Draw(img)
    cy, sy = math.cos(yaw), math.sin(yaw); pitch = 0.45; cp, spp = math.cos(pitch), math.sin(pitch)
    k = 6.0 * SCALE[name] if name not in ("brachiosaurus",) else 5.0 * SCALE[name]
    k = min(k, 9.0)
    def proj(pt):
        x, y, z = pt; x, z = x * cy - z * sy, x * sy + z * cy
        y2 = y * cp - z * spp; depth = y * spp + z * cp
        return (260 + x * k * 0.9, 330 + (y2 - 24 * cp) * k * 0.9, depth)
    pq = []
    for pts, col in quads:
        pp = [proj(p) for p in pts]
        pq.append((sum(p[2] for p in pp) / 4, [(p[0], p[1]) for p in pp], col))
    pq.sort(key=lambda t: -t[0])
    for _, poly, col in pq:
        dr.polygon(poly, fill=col)
    dr.text((8, 8), name, fill=(0, 0, 0))
    return img

for name, sp in G.SPECIES.items():
    a = render(name, sp, math.radians(-140)); b = render(name, sp, math.radians(-90))
    sheet = Image.new("RGB", (1040, 420)); sheet.paste(a, (0, 0)); sheet.paste(b, (520, 0))
    sheet.save(os.path.join(OUT, name + ".png"))
# contact sheet
ims = [Image.open(os.path.join(OUT, n + ".png")).resize((520, 210)) for n in G.SPECIES]
cs = Image.new("RGB", (1040, 210 * 5))
for i, im in enumerate(ims):
    cs.paste(im, ((i % 2) * 520, (i // 2) * 210))
cs.save(os.path.join(OUT, "all.png")); print("ok")
