"""Generate placeholder textures and 3D models for Gear Expansion materials.

Every shape below is drawn once as 16x16 pixel art using palette letters, then
recolored per material. Nothing here is copied from Minecraft's own textures.

Usage:
    python tools/placeholder_art.py            # all materials
    python tools/placeholder_art.py titanium   # one material
"""

import json
import random
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common" / "src" / "main" / "resources" / "assets" / "gearexpansion"

# Palette letters:
#   O outline, D dark, M mid, L light, H highlight  (material colors)
#   s dark handle, t light handle                   (shared wood colors)
#   . transparent
HANDLE = {"s": (0x4A, 0x32, 0x22), "t": (0x7A, 0x54, 0x33)}


def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


MATERIALS = {
    "titanium": {
        "metal": {"O": "#2E353B", "D": "#6B7780", "M": "#9AA7B0", "L": "#C9D3DA", "H": "#EEF3F6"},
        "raw": {"O": "#26282C", "D": "#6F7C88", "M": "#9DB0C0", "L": "#C6D6E2", "H": "#EAF3FA"},
    },
}

SHAPES = {
    "ingot": [
        "................",
        "................",
        "................",
        "................",
        "......OOOOO.....",
        ".....OHLLLLO....",
        "....OLLLLLMMO...",
        "...OLLLLLMMDO...",
        "..OLLLLLMMDDO...",
        ".OMMMMMMMDDO....",
        ".ODDDDDDDDO.....",
        "..OOOOOOOO......",
        "................",
        "................",
        "................",
        "................",
    ],
    "nugget": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......OOO.......",
        ".....OLLMO......",
        "....OLLMMDO.....",
        "....OMMMDDO.....",
        ".....ODDDO......",
        "......OOO.......",
        "................",
        "................",
        "................",
        "................",
    ],
    "raw": [
        "................",
        "................",
        "................",
        ".....OOOO.......",
        "....OLLMMO.OO...",
        "...OLLMMMDOLMO..",
        "..OLLMMMDDDMMO..",
        "..OLMMMDDDMMDO..",
        ".OLMMDDDMMMDDO..",
        ".OMMDDDMMMDDO...",
        "..ODDDMMDDDO....",
        "...OOODDDOO.....",
        "......OOO.......",
        "................",
        "................",
        "................",
    ],
    "sword": [
        "..............OO",
        ".............OLO",
        "............OLMO",
        "...........OLMO.",
        "..........OLMO..",
        ".........OLMO...",
        "........OLMO....",
        ".......OLMO.....",
        "..OO..OLMO......",
        "..OMOOLMO.......",
        "...OMLMO........",
        "....OMO.........",
        "...OtOMO........",
        "..OtO.OO........",
        ".OtO............",
        ".OO.............",
    ],
    "pickaxe": [
        "................",
        "................",
        "....OOOOO.......",
        "...OLLLMMOO.....",
        "....OOOOMMMO....",
        "........OtMMO...",
        ".......OtO.OMO..",
        "......OtO...OMO.",
        ".....OtO....OMO.",
        "....OtO.....OMO.",
        "...OtO.......OO.",
        "..OtO...........",
        ".OsO............",
        "OsO.............",
        "................",
        "................",
    ],
    "axe": [
        "................",
        ".......OOOO.....",
        "......OLLLMO....",
        ".....OLLMMMMO...",
        ".....OLMMOMMDO..",
        "......OOtOOMDO..",
        ".......OtO.OO...",
        "......OtO.......",
        ".....OtO........",
        "....OtO.........",
        "...OtO..........",
        "..OtO...........",
        ".OsO............",
        "OsO.............",
        "................",
        "................",
    ],
    "shovel": [
        "................",
        "...........OOO..",
        "..........OLLMO.",
        ".........OLLMMO.",
        ".........OLMMO..",
        "........OtOOO...",
        ".......OtO......",
        "......OtO.......",
        ".....OtO........",
        "....OtO.........",
        "...OtO..........",
        "..OsO...........",
        ".OsO............",
        ".OO.............",
        "................",
        "................",
    ],
    "hoe": [
        "................",
        "........OOOO....",
        ".......OLLMMO...",
        "........OOtMO...",
        ".........OtOO...",
        "........OtO.....",
        ".......OtO......",
        "......OtO.......",
        ".....OtO........",
        "....OtO.........",
        "...OtO..........",
        "..OsO...........",
        ".OsO............",
        ".OO.............",
        "................",
        "................",
    ],
    "spear": [
        ".............OOO",
        "............OLLO",
        "...........OLMO.",
        "..........OMMO..",
        ".........OtOO...",
        "........OtO.....",
        ".......OtO......",
        "......OtO.......",
        ".....OtO........",
        "....OtO.........",
        "...OtO..........",
        "..OtO...........",
        ".OsO............",
        "OsO.............",
        "OO..............",
        "................",
    ],
    "helmet": [
        "................",
        "................",
        "................",
        "....OOOOOOOO....",
        "...OLLLLLLMMO...",
        "..OLLMMMMMMMDO..",
        "..OLMMMMMMMMDO..",
        "..OLMOOOOOOMDO..",
        "..OMDO....OMDO..",
        "..OMDO....OMDO..",
        "..OOOO....OOOO..",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "chestplate": [
        "................",
        "..OOOO....OOOO..",
        ".OLLMOO..OOMMDO.",
        ".OLMMMMOOMMMMDO.",
        ".OLMMMMMMMMMMDO.",
        ".OOLMMMMMMMMDOO.",
        "..OOLMMMMMMDOO..",
        "...OLMMMMMMDO...",
        "...OLMMMMMMDO...",
        "...OLMMMMMMDO...",
        "...OLMMMMMMDO...",
        "...OMMMMMMMDO...",
        "...ODDDDDDDDO...",
        "...OOOOOOOOOO...",
        "................",
        "................",
    ],
    "leggings": [
        "................",
        "................",
        "...OOOOOOOOOO...",
        "...OLLMMMMMDO...",
        "...OLMMMMMMDO...",
        "...OLMMOOMMDO...",
        "...OLMDOOLMDO...",
        "...OLMDOOLMDO...",
        "...OLMDOOLMDO...",
        "...OLMDOOLMDO...",
        "...OLMDOOLMDO...",
        "...OLMDOOLMDO...",
        "...OMDDOOMDDO...",
        "...OOOOOOOOOO...",
        "................",
        "................",
    ],
    "boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...OOOO..OOOO...",
        "...OLMO..OLMO...",
        "...OLMO..OLMO...",
        "...OLMO..OLMO...",
        "..OLMMO..OLMMO..",
        ".OLMMDO..OLMMDO.",
        ".OOOOOO..OOOOOO.",
        "................",
        "................",
    ],
}


def write_png(path, width, height, pixels):
    """Write RGBA pixels (list of rows of (r, g, b, a)) as a PNG file."""
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def render_shape(shape, palette):
    colors = {k: hex_rgb(v) for k, v in palette.items()} | HANDLE
    rows = []
    for line in SHAPES[shape]:
        assert len(line) == 16, f"{shape}: row is {len(line)} wide"
        rows.append([(*colors[c], 255) if c != "." else (0, 0, 0, 0) for c in line])
    return rows


def upscale(rows, factor):
    return [[px for px in row for _ in range(factor)] for row in rows for _ in range(factor)]


def shade(rgb, amount):
    return tuple(max(0, min(255, c + amount)) for c in rgb)


def noise_block(base, spread, seed, streaks=False):
    """A 16x16 stone-like texture made of random light and dark pixels."""
    rng = random.Random(seed)
    rows = []
    for y in range(16):
        row = []
        band = rng.randint(-spread // 2, spread // 2) if streaks else 0
        for _ in range(16):
            row.append((*shade(base, rng.randint(-spread, spread) + band), 255))
        rows.append(row)
    return rows


def add_ore_specks(rows, palette, seed):
    """Scatter outlined mineral clusters over a stone texture, like vanilla ores."""
    rng = random.Random(seed)
    colors = [hex_rgb(palette[k]) for k in ("M", "L", "L", "H")]
    outline = hex_rgb(palette["O"])
    clusters = []
    for _ in range(4):
        cx, cy = rng.randint(2, 12), rng.randint(2, 12)
        cells = {(cx + dx, cy + dy) for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)) if rng.random() < 0.9}
        cells.add((cx, cy))
        clusters.append(cells)
    # Outlines first, so neighboring clusters don't cover each other's fill.
    for cells in clusters:
        for x, y in cells:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and (nx, ny) not in cells:
                    rows[ny][nx] = (*outline, 255)
    for cells in clusters:
        for x, y in cells:
            rows[y][x] = (*rng.choice(colors), 255)
    return rows


def metal_block(palette):
    """A riveted metal panel."""
    o, d, m, l, h = (hex_rgb(palette[k]) for k in "ODMLH")
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = d
            elif x == 1 or y == 1:
                c = l
            elif x == 14 or y == 14:
                c = d
            elif (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
                c = h
            elif (x + y) % 7 == 0:
                c = l
            else:
                c = m
            row.append((*c, 255))
        rows.append(row)
    return rows


def raw_block(palette, seed):
    rng = random.Random(seed)
    colors = [hex_rgb(palette[k]) for k in ("D", "M", "M", "L", "H")]
    return [[(*rng.choice(colors), 255) for _ in range(16)] for _ in range(16)]


# ---------------------------------------------------------------------------
# 3D models (GeckoLib)
# ---------------------------------------------------------------------------

# Armor bones GeckoLib looks up by name. Each armor bone sits inside a "biped" bone that
# follows the player's body part. Values: parent, pivot, and one cube (origin, size, uv, inflate, mirror).
ARMOR_BONES = [
    ("bipedHead", None, [0, 24, 0], None),
    ("armorHead", "bipedHead", [0, 24, 0], ([-4, 24, -4], [8, 8, 8], [0, 0], 1.0, False)),
    ("bipedBody", None, [0, 24, 0], None),
    ("armorBody", "bipedBody", [0, 24, 0], ([-4, 12, -2], [8, 12, 4], [16, 16], 1.01, False)),
    ("bipedRightArm", None, [-5, 22, 0], None),
    ("armorRightArm", "bipedRightArm", [-5, 22, 0], ([-8, 12, -2], [4, 12, 4], [40, 16], 1.0, False)),
    ("bipedLeftArm", None, [5, 22, 0], None),
    ("armorLeftArm", "bipedLeftArm", [5, 22, 0], ([4, 12, -2], [4, 12, 4], [40, 16], 1.0, True)),
    ("bipedRightLeg", None, [-1.9, 12, 0], None),
    ("armorRightLeg", "bipedRightLeg", [-1.9, 12, 0], ([-3.9, 0, -2], [4, 12, 4], [0, 32], 0.5, False)),
    ("armorRightBoot", "bipedRightLeg", [-1.9, 12, 0], ([-3.9, 0, -2], [4, 12, 4], [0, 16], 1.0, False)),
    ("bipedLeftLeg", None, [1.9, 12, 0], None),
    ("armorLeftLeg", "bipedLeftLeg", [1.9, 12, 0], ([-0.1, 0, -2], [4, 12, 4], [0, 32], 0.5, True)),
    ("armorLeftBoot", "bipedLeftLeg", [1.9, 12, 0], ([-0.1, 0, -2], [4, 12, 4], [0, 16], 1.0, True)),
]

SHIELD_CUBES = [
    ([-6, -11, 1], [12, 22, 1], [0, 0]),  # board
    ([-1, -3, -5], [2, 6, 6], [26, 0]),   # handle
]


def geo_json(identifier, bones, size=64):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": identifier,
                "texture_width": size,
                "texture_height": size,
                "visible_bounds_width": 3,
                "visible_bounds_height": 3,
                "visible_bounds_offset": [0, 1.5, 0],
            },
            "bones": bones,
        }],
    }


def armor_geo(name):
    bones = []
    for bone_name, parent, pivot, cube in ARMOR_BONES:
        bone = {"name": bone_name, "pivot": pivot}
        if parent:
            bone["parent"] = parent
        if cube:
            origin, size, uv, inflate, mirror = cube
            entry = {"origin": origin, "size": size, "uv": uv, "inflate": inflate}
            if mirror:
                entry["mirror"] = True
            bone["cubes"] = [entry]
        bones.append(bone)
    return geo_json(f"geometry.{name}_armor", bones)


def shield_geo(name):
    cubes = [{"origin": o, "size": s, "uv": uv} for o, s, uv in SHIELD_CUBES]
    return geo_json(f"geometry.{name}_shield", [{"name": "shield", "pivot": [0, 0, 0], "cubes": cubes}])


def blank(width, height):
    return [[(0, 0, 0, 0)] * width for _ in range(height)]


def paint_panel(canvas, x0, y0, w, h, palette, emblem=False):
    """Fill one cube face with a beveled metal plate."""
    o, d, m, l, hi = (hex_rgb(palette[k]) for k in "ODMLH")
    for y in range(h):
        for x in range(w):
            if x == 0 or y == 0 or x == w - 1 or y == h - 1:
                c = d
            elif x == 1 or y == 1:
                c = l
            elif x == w - 2 or y == h - 2:
                c = d
            else:
                c = m
            canvas[y0 + y][x0 + x] = (*c, 255)
    if emblem and w >= 8 and h >= 8:
        cx, cy = x0 + w // 2, y0 + h // 3
        for i in range(-2, 3):
            canvas[cy + i][cx] = (*hi, 255)
            canvas[cy][cx + i] = (*hi, 255)
        canvas[cy][cx] = (*o, 255)


def paint_box(canvas, u, v, size, palette, emblem=False, visor=False):
    """Paint every face of a box-UV cube (Minecraft's standard cube texture layout)."""
    w, h, d = size
    faces = [
        (u + d, v, w, d),              # top
        (u + d + w, v, w, d),          # bottom
        (u, v + d, d, h),              # right
        (u + d, v + d, w, h),          # front
        (u + d + w, v + d, d, h),      # left
        (u + 2 * d + w, v + d, w, h),  # back
    ]
    for i, (x, y, fw, fh) in enumerate(faces):
        if fw > 0 and fh > 0:
            paint_panel(canvas, x, y, fw, fh, palette, emblem=emblem and i in (3, 5))
    if visor:
        # Cut an eye slit into the helmet's front face so the face shows through.
        fx, fy = u + d, v + d
        for x in range(1, w - 1):
            for y in (3, 4):
                canvas[fy + y][fx + x] = (0, 0, 0, 0)


def armor_texture(palette):
    canvas = blank(64, 64)
    paint_box(canvas, 0, 0, (8, 8, 8), palette, visor=True)  # helmet
    paint_box(canvas, 16, 16, (8, 12, 4), palette)          # chest
    paint_box(canvas, 40, 16, (4, 12, 4), palette)          # arms
    paint_box(canvas, 0, 32, (4, 12, 4), palette)           # leggings
    paint_box(canvas, 0, 16, (4, 12, 4), palette)           # boots
    return canvas


def shield_texture(palette):
    canvas = blank(64, 64)
    paint_box(canvas, 0, 0, (12, 22, 1), palette, emblem=True)
    paint_box(canvas, 26, 0, (2, 6, 6), {**palette, "M": palette["D"]})
    return canvas


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def generate(name, spec):
    metal, raw = spec["metal"], spec["raw"]
    item = ASSETS / "textures" / "item"
    block = ASSETS / "textures" / "block"

    write_png(item / f"{name}_ingot.png", 16, 16, render_shape("ingot", metal))
    write_png(item / f"{name}_nugget.png", 16, 16, render_shape("nugget", metal))
    write_png(item / f"raw_{name}.png", 16, 16, render_shape("raw", raw))
    for tool in ("sword", "pickaxe", "axe", "shovel", "hoe", "spear"):
        write_png(item / f"{name}_{tool}.png", 16, 16, render_shape(tool, metal))
    # Spears use a separate, larger sprite when held.
    write_png(item / f"{name}_spear_in_hand.png", 32, 32, upscale(render_shape("spear", metal), 2))
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        write_png(item / f"{name}_{piece}.png", 16, 16, render_shape(piece, metal))

    # 3D models. The shield's inventory icon is its 3D model, so it has no flat sprite.
    geo = ASSETS / "geckolib" / "models" / "item"
    write_json(geo / "armor" / f"{name}_armor.geo.json", armor_geo(name))
    write_png(item / "armor" / f"{name}_armor.png", 64, 64, armor_texture(metal))
    write_json(geo / f"{name}_shield.geo.json", shield_geo(name))
    write_png(item / f"{name}_shield.png", 64, 64, shield_texture(metal))

    seed = sum(map(ord, name))
    write_png(block / f"{name}_ore.png", 16, 16, add_ore_specks(noise_block((0x7D, 0x7D, 0x7D), 12, seed), raw, seed))
    write_png(block / f"deepslate_{name}_ore.png", 16, 16,
              add_ore_specks(noise_block((0x4A, 0x4A, 0x50), 10, seed + 1, streaks=True), raw, seed + 1))
    write_png(block / f"{name}_block.png", 16, 16, metal_block(metal))
    write_png(block / f"raw_{name}_block.png", 16, 16, raw_block(raw, seed + 2))
    print(f"generated textures for {name}")


if __name__ == "__main__":
    names = sys.argv[1:] or list(MATERIALS)
    for material in names:
        generate(material, MATERIALS[material])
