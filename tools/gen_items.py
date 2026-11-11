#!/usr/bin/env python3
"""Generates the 16x16 item textures for the 1.1.0 items and the saddle overlay for the Triceratops.

Run from the repository root: python3 tools/gen_items.py
Shapes are built from discs / polygons sampled at pixel centres, then shaded from the top-left and outlined,
which gives the same chunky pixel-art look as the spawn eggs.
"""
import math, os, random, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
ITEM_DIR = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/textures/item")
ENTITY_DIR = os.path.join(ROOT, "src/main/resources/assets/dinosaurs/textures/entity")
N = 16

def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])

class Canvas:
    def __init__(self):
        self.px = [[None] * N for _ in range(N)]  # (r,g,b) or None

    def disc_line(self, pts):
        """pts: list of (x, y, r); fills the union of discs interpolated along the polyline."""
        mask = set()
        for (x0, y0, r0), (x1, y1, r1) in zip(pts, pts[1:]):
            steps = 40
            for i in range(steps + 1):
                t = i / steps
                x, y, r = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, r0 + (r1 - r0) * t
                for py in range(N):
                    for px in range(N):
                        if (px + 0.5 - x) ** 2 + (py + 0.5 - y) ** 2 <= r * r:
                            mask.add((px, py))
        return mask

    @staticmethod
    def polygon(poly):
        mask = set()
        for py in range(N):
            for px in range(N):
                x, y = px + 0.5, py + 0.5
                inside = False
                j = len(poly) - 1
                for i in range(len(poly)):
                    xi, yi = poly[i]; xj, yj = poly[j]
                    if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
                        inside = not inside
                    j = i
                if inside:
                    mask.add((px, py))
        return mask

    @staticmethod
    def ellipse(cx, cy, rx, ry):
        return {(px, py) for py in range(N) for px in range(N) if ((px + 0.5 - cx) / rx) ** 2 + ((py + 0.5 - cy) / ry) ** 2 <= 1.0}

    def fill(self, mask, color, rng=None, noise=6, light=True, outline=0.45):
        """Shaded fill with an outline in a darker tone of the same colour."""
        if not mask:
            return
        xs = [p[0] for p in mask]; ys = [p[1] for p in mask]
        x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
        for (x, y) in mask:
            edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge and outline:
                c = shade(color, outline)
            else:
                k = 1.0
                if light:
                    t = ((x - x0) / max(1, x1 - x0) + (y - y0) / max(1, y1 - y0)) / 2
                    k = 1.18 - 0.36 * t
                c = shade(color, k)
                if rng is not None and noise:
                    n = rng.randint(-noise, noise)
                    c = tuple(max(0, min(255, v + n)) for v in c)
            self.px[y][x] = c

    def put(self, x, y, c):
        if 0 <= x < N and 0 <= y < N:
            self.px[y][x] = c[:3]

    def save(self, path, scale_preview=None):
        img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        for y in range(N):
            for x in range(N):
                if self.px[y][x] is not None:
                    img.putpixel((x, y), self.px[y][x] + (255,))
        img.save(path)
        return img

def bezier(p0, p1, p2, r0, r1, n=10):
    out = []
    for i in range(n + 1):
        t = i / n
        x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0]
        y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]
        out.append((x, y, r0 + (r1 - r0) * t))
    return out

# ------------------------------------------------------------------ items
def t_rex_tooth():
    c = Canvas(); rng = random.Random("t_rex_tooth")
    pts = bezier((3.4, 12.8), (4.6, 4.8), (13.0, 2.4), 3.1, 0.75, 14)
    c.fill(c.disc_line(pts), (238, 228, 198), rng, outline=0.6)
    c.fill(c.disc_line(pts[:2]), (156, 116, 84), rng, outline=0.55)   # root
    for x, y in ((6, 6), (7, 5), (8, 4)):                       # highlight
        c.put(x, y, (255, 252, 240))
    return c

def raptor_claw():
    c = Canvas(); rng = random.Random("raptor_claw")
    pts = []
    for i in range(15):
        a = math.radians(-20 + i * 14)      # sweep from right/bottom over the top to the left
        r = 2.9 - 2.2 * (i / 14)
        pts.append((5.9 + 4.6 * math.cos(a), 10.0 - 5.2 * math.sin(a), r))
    c.fill(c.disc_line(pts), (104, 92, 82), rng, outline=0.5)
    c.fill(c.disc_line(pts[:2]), (176, 146, 116), rng, outline=0.6)   # toe / base
    for x, y in ((5, 4), (6, 4), (7, 4)):
        c.put(x, y, (160, 148, 136))
    return c

def megalodon_tooth():
    c = Canvas(); rng = random.Random("megalodon_tooth")
    blade = Canvas.polygon([(1.6, 4.0), (14.4, 4.0), (8.0, 15.0)])
    c.fill(blade, (226, 228, 230), rng)
    c.fill(Canvas.polygon([(2.6, 1.6), (13.4, 1.6), (14.6, 5.2), (1.4, 5.2)]), (110, 92, 78), rng)   # root
    for y in range(6, 14):                                      # serrated edges
        for x in range(N):
            if (x, y) in blade and ((x - 1, y) not in blade or (x + 1, y) not in blade) and y % 2 == 0:
                c.put(x, y, (150, 152, 156))
    for x, y in ((5, 6), (6, 7), (6, 8), (7, 9)):
        c.put(x, y, (255, 255, 255))
    return c

def amulet():
    c = Canvas(); rng = random.Random("amulet")
    cord = set()
    for i in range(25):
        a = math.radians(180 - i * 7.5)
        x, y = 8 + 5.6 * math.cos(a), 7.4 - 5.6 * math.sin(a)
        cord.add((int(x), int(y)))
    for x, y in cord:
        c.put(x, y, (120, 82, 48))
    gold = Canvas.ellipse(8, 10.8, 4.0, 4.0)
    c.fill(gold, (236, 188, 52), rng, noise=4)
    c.fill(Canvas.polygon([(6.3, 8.6), (9.7, 8.6), (8.0, 13.4)]), (246, 242, 226), None, outline=0.7)  # tooth inlay
    c.put(8, 13, (60, 140, 80)); c.put(8, 8, (60, 140, 80))  # little green gems
    c.put(6, 9, (255, 236, 150)); c.put(5, 10, (255, 236, 150))
    return c

def brachiosaurus_hide():
    c = Canvas(); rng = random.Random("hide")
    hide = Canvas.polygon([(2.2, 3.4), (7, 2.2), (13.6, 2.6), (14.2, 7.6), (13.0, 13.6), (8, 12.6), (3.4, 13.8), (1.8, 8.4)])
    c.fill(hide, (128, 132, 116), rng, noise=5)
    for _ in range(10):
        x, y = rng.randint(3, 12), rng.randint(4, 12)
        if (x, y) in hide and (x + 1, y) in hide:
            c.put(x, y, (92, 96, 82))
    for x, y in ((4, 4), (5, 4), (4, 5)):
        c.put(x, y, (176, 180, 162))
    return c

def parasaurolophus_crest():
    c = Canvas(); rng = random.Random("crest")
    pts = bezier((3.2, 13.0), (4.8, 3.6), (13.4, 2.8), 1.9, 1.5, 14)
    c.fill(c.disc_line(pts), (200, 122, 50), rng)
    c.fill(c.disc_line(pts[:3]), (196, 180, 150), rng)         # bony base
    c.put(13, 3, (110, 60, 26)); c.put(12, 3, (110, 60, 26))   # open end of the tube
    for x, y in ((5, 7), (6, 5), (7, 4)):
        c.put(x, y, (240, 170, 96))
    return c

SADDLE = [
    "................",
    "................",
    ".oo.............",
    "occo.......ooo..",
    "occo......ollo..",
    ".olloooooolllo..",
    ".ollhhhhhhllllo.",
    ".olhhhhhhhhllo..",
    "..ollllllllloo..",
    "...oollllloo....",
    "....os...os.....",
    "....os...os.....",
    "...oiio.oiio....",
    "...i..i.i..i....",
    "...iiii.iiii....",
    "................",
]

def dinosaur_saddle():
    c = Canvas(); rng = random.Random("saddle")
    pal = {"o": (62, 38, 22), "l": (140, 92, 54), "h": (128, 132, 116), "c": (204, 126, 52), "s": (92, 60, 36), "i": (206, 208, 214)}
    for y, row in enumerate(SADDLE):
        for x, ch in enumerate(row):
            if ch in pal:
                col = pal[ch]
                if ch in "lh":
                    col = shade(col, 1.12 - 0.03 * y + rng.uniform(-0.04, 0.04))
                c.put(x, y, col)
    c.put(2, 3, (240, 170, 96)); c.put(5, 6, (176, 120, 76))
    return c

def saddle_overlay():
    """Transparent copy of the Triceratops texture with a saddle painted over the body box (same UV packing)."""
    import gen_assets
    sp = gen_assets.SPECIES["triceratops"]
    tw, th = gen_assets.pack(sp["parts"])
    body = sp["parts"][0]["boxes"][0]
    w, h, d = body["s"]; u, v = body["uv"]
    img = Image.new("RGBA", (tw, th), (0, 0, 0, 0)); px = img.load()
    rng = random.Random("tri_saddle")
    leather, dark, hide, crest, iron = (140, 92, 54), (78, 50, 30), (128, 132, 116), (200, 122, 50), (200, 200, 206)
    def put(x, y, c):
        n = rng.randint(-6, 6)
        px[x, y] = tuple(max(0, min(255, k + n)) for k in c) + (255,)
    # top face: (u+d, v) size w x d ; rows run along the body length. Saddle sits on the middle rows.
    z0, z1 = d // 2 - 5, d // 2 + 4
    for row in range(z0, z1 + 1):
        for col in range(w):
            c = leather
            if row in (z0, z1) or col in (0, w - 1):
                c = dark
            elif 2 <= col <= w - 3 and z0 + 2 <= row <= z1 - 2:
                c = hide
            put(u + d + col, v + row, c)
    # pommel made of the crest colour at the front edge of the seat (front = -z = bottom rows of this face)
    for col in (w // 2 - 1, w // 2):
        put(u + d + col, v + z1, crest)
        put(u + d + col, v + z1 - 1, crest)
    # side faces: west (u, v+d, d x h) and east (u+d+w, v+d, d x h); columns run along the body length.
    for fx in (u, u + d + w):
        for col in range(z0, z1 + 1):
            depth = 3 if col not in (z0, z1) else 2
            for row in range(depth):
                put(fx + (col if fx == u else d - 1 - col), v + d + row, leather if col not in (z0, z1) else dark)
        # girth strap down the side and an iron ring
        mid = (z0 + z1) // 2
        for row in range(3, h):
            cc = mid if fx == u else d - 1 - mid
            put(fx + cc, v + d + row, dark)
        cc = mid if fx == u else d - 1 - mid
        put(fx + cc, v + d + 5, iron)
    # strap under the belly: bottom face (u+d+w, v, w x d)
    for col in range(w):
        put(u + d + w + col, v + (z0 + z1) // 2, dark)
    return img

# Creative tab icon: the Spinosaurus head from the mod icon (side view, snout to the left), simplified to 16x16.
TAB_ICON = [
    "................",
    "................",
    "..........oooo..",
    ".........oggggo.",
    ".........ogyggo.",
    "..oooooooogkgggo",
    ".odddddddggggggo",
    ".ogggggggggggggo",
    ".ogggggggggggggo",
    ".owgwgwgwgwggggo",
    ".ollllllllllggo.",
    ".obbbbbbbbbbbo..",
    "..obbbbbbbbbo...",
    "...ooooooooo....",
    "................",
    "................",
]

def tab_icon():
    c = Canvas(); rng = random.Random("tab")
    pal = {"o": (34, 40, 40), "g": (88, 100, 100), "d": (108, 120, 120), "y": (226, 190, 40), "k": (20, 20, 20),
           "w": (240, 238, 228), "l": (204, 200, 176), "b": (168, 166, 146)}
    for y, row in enumerate(TAB_ICON):
        for x, ch in enumerate(row):
            if ch in pal:
                col = pal[ch]
                if ch in "gb":
                    col = shade(col, 1.08 - 0.02 * y + rng.uniform(-0.04, 0.04))
                c.put(x, y, col)
    return c

ITEMS = {
    "tab_icon": tab_icon,
    "t_rex_tooth": t_rex_tooth, "raptor_claw": raptor_claw, "megalodon_tooth": megalodon_tooth,
    "amulet_of_dino_protection": amulet, "brachiosaurus_hide": brachiosaurus_hide,
    "parasaurolophus_crest": parasaurolophus_crest, "dinosaur_saddle": dinosaur_saddle,
}

def main():
    os.makedirs(ITEM_DIR, exist_ok=True)
    for name, fn in ITEMS.items():
        fn().save(os.path.join(ITEM_DIR, name + ".png"))
    saddle_overlay().save(os.path.join(ENTITY_DIR, "triceratops_saddle.png"))
    if "--preview" in sys.argv:
        sheet = Image.new("RGBA", (len(ITEMS) * 144, 144), (60, 60, 60, 255))
        for i, name in enumerate(ITEMS):
            im = Image.open(os.path.join(ITEM_DIR, name + ".png")).resize((128, 128), Image.NEAREST)
            sheet.alpha_composite(im, (i * 144 + 8, 8))
        sheet.save("/tmp/itemprev/items.png")
    print("ok")

if __name__ == "__main__":
    main()
