"""Generate textures and 3D models for Gear Expansion materials.

Textures are recolored from vanilla Minecraft textures, read straight from the
Minecraft jar in the Gradle cache (run a Gradle build once so it exists). Each
material picks vanilla textures to start from and color gradients to map onto:
pixels are recolored by brightness, so shading and detail carry over.

Usage:
    python tools/generate_assets.py            # all materials
    python tools/generate_assets.py titanium   # one material
"""

import glob
import json
import os
import struct
import sys
import zlib
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common" / "src" / "main" / "resources" / "assets" / "gearexpansion"

# Each material names the vanilla textures to start from and color gradients (dark to light):
#   tools / armor   vanilla prefix for the six tools / armor icons and worn armor (e.g. "iron", "chainmail")
#   raw / ore       vanilla prefix for the raw item and raw block / the ore blocks (e.g. "iron", "copper")
#   ore_name        name of our ore and raw item, if different from the material (Aluminum uses "bauxite")
#   metal           gradient for tools, armor, ingot, nugget, block, and shield rim
#   raw_colors      gradient for the raw item and raw block
#   ore_colors      gradient for the mineral in the ore blocks (defaults to metal)
MATERIALS = {
    "zinc": {
        "tools": "iron", "armor": "chainmail", "raw": "iron", "ore": "iron",
        # Matte blue-green grey, lower contrast than iron.
        "metal": ["#1E2A2A", "#394B4B", "#5A7170", "#7E9794", "#A6BCB8", "#CEDFDA"],
        "raw_colors": ["#25272A", "#40454A", "#5E666B", "#7F898D", "#A3ACAE", "#C7CDCD"],
    },
    "aluminum": {
        "tools": "diamond", "armor": "diamond", "raw": "copper", "ore": "copper", "ore_name": "bauxite",
        # Bright, clean silver-white.
        "metal": ["#3A3F46", "#646B74", "#8F97A1", "#B8C0C8", "#DCE2E8", "#FAFCFD"],
        # Bauxite: reddish-brown clay rock.
        "raw_colors": ["#2E1610", "#5A2A1C", "#83412A", "#A85C3C", "#C98059", "#E3A77F"],
        "ore_colors": ["#2E1610", "#5A2A1C", "#83412A", "#A85C3C", "#C98059", "#E3A77F"],
    },
    "titanium": {
        "tools": "iron", "armor": "iron", "raw": "iron", "ore": "diamond",
        # Cool blue-grey steel, so it reads differently from iron.
        "metal": ["#1C2630", "#34465A", "#557089", "#7E9AB2", "#A9C0D2", "#D3E1EC"],
        "raw_colors": ["#221F27", "#3E3A47", "#5F5A6B", "#857F92", "#ADA7B8", "#D2CDDA"],
    },
}

# ---------------------------------------------------------------------------
# PNG reading and writing (no external libraries needed)
# ---------------------------------------------------------------------------


def read_png(data):
    """Decode a PNG into rows of (r, g, b, a). Supports the formats Minecraft textures use."""
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG"
    pos, idat, palette, trns = 8, b"", None, None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            width, height, depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert interlace == 0, "interlaced PNGs are not supported"
        elif kind == b"PLTE":
            palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
        elif kind == b"tRNS":
            trns = body
        elif kind == b"IDAT":
            idat += body
        pos += 12 + length

    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    bits_per_pixel = channels * depth
    stride = (width * bits_per_pixel + 7) // 8
    step = max(1, bits_per_pixel // 8)
    raw = zlib.decompress(idat)

    rows, previous = [], bytearray(stride)
    for y in range(height):
        start = y * (stride + 1)
        kind, line = raw[start], bytearray(raw[start + 1:start + 1 + stride])
        for i in range(stride):
            a = line[i - step] if i >= step else 0
            b = previous[i]
            c = previous[i - step] if i >= step else 0
            if kind == 1:
                line[i] = (line[i] + a) & 0xFF
            elif kind == 2:
                line[i] = (line[i] + b) & 0xFF
            elif kind == 3:
                line[i] = (line[i] + (a + b) // 2) & 0xFF
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 0xFF
        previous = line

        if depth < 8:
            per_byte = 8 // depth
            mask = (1 << depth) - 1
            values = [(line[x // per_byte] >> (8 - depth * (x % per_byte + 1))) & mask for x in range(width)]
        else:
            values = None

        row = []
        for x in range(width):
            if color_type == 3:
                index = values[x] if values else line[x]
                alpha = trns[index] if trns and index < len(trns) else 255
                row.append((*palette[index], alpha))
            elif color_type == 6:
                row.append(tuple(line[x * 4:x * 4 + 4]))
            elif color_type == 2:
                rgb = tuple(line[x * 3:x * 3 + 3])
                # tRNS names one exact color that is fully transparent.
                transparent = trns and rgb == (trns[1], trns[3], trns[5])
                row.append((*rgb, 0 if transparent else 255))
            elif color_type == 4:
                g, alpha = line[x * 2], line[x * 2 + 1]
                row.append((g, g, g, alpha))
            else:
                sample = values[x] if values else line[x]
                g = sample * (255 // ((1 << depth) - 1)) if values else sample
                # tRNS names one exact gray value that is fully transparent.
                transparent = trns and sample == struct.unpack(">H", trns[:2])[0]
                row.append((g, g, g, 0 if transparent else 255))
        rows.append(row)
    return rows


def write_png(path, pixels):
    height, width = len(pixels), len(pixels[0])
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


# ---------------------------------------------------------------------------
# Recoloring
# ---------------------------------------------------------------------------


def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def luminance(px):
    return (0.299 * px[0] + 0.587 * px[1] + 0.114 * px[2]) / 255


def saturation(px):
    high, low = max(px[:3]), min(px[:3])
    return 0 if high == 0 else (high - low) / high


def gradient_color(stops, t):
    t = min(max(t, 0.0), 1.0) * (len(stops) - 1)
    i = min(int(t), len(stops) - 2)
    f = t - i
    a, b = stops[i], stops[i + 1]
    return tuple(round(a[c] + (b[c] - a[c]) * f) for c in range(3))


# Which pixels of a vanilla texture get recolored.
def everything(px):
    return True


def hue(px):
    r, g, b = (c / 255 for c in px[:3])
    high, low = max(r, g, b), min(r, g, b)
    if high == low:
        return 0.0
    if high == r:
        return (60 * (g - b) / (high - low)) % 360
    if high == g:
        return 60 * (b - r) / (high - low) + 120
    return 60 * (r - g) / (high - low) + 240


def grey_metal(px):
    """Grey metal only, e.g. a shield's iron rim; leaves its wooden planks alone."""
    return saturation(px) < 0.2


def tool_head(px):
    """Everything but wooden handles and bindings (browns and oranges), so any vanilla tool works as a base."""
    wooden = 10 <= hue(px) <= 50 and saturation(px) > 0.25 and luminance(px) < 0.8
    return not wooden


def ore_gems(px):
    """The mineral in an ore block; leaves the stone or deepslate alone."""
    return saturation(px) > 0.25 or luminance(px) > 0.72


def recolor(pixels, gradient, selector, stretch=None):
    """Map each selected pixel's brightness onto the gradient (black to the darkest stop, white to the lightest).

    With ``stretch=(start, end)``, the darkest and lightest selected pixels are mapped to that
    part of the gradient instead (0 is the darkest stop, 1 the lightest). This suits small
    details like the gems in an ore, which should stay bright against the stone.
    """
    stops = [hex_rgb(c) for c in gradient]
    chosen = [px for row in pixels for px in row if px[3] > 0 and selector(px)]
    if not chosen:
        return pixels
    low, high = 0.0, 1.0
    start, end = stretch or (0.0, 1.0)
    if stretch:
        low = min(map(luminance, chosen))
        high = max(map(luminance, chosen))
    span = (high - low) or 1.0
    out = []
    for row in pixels:
        new_row = []
        for px in row:
            if px[3] > 0 and selector(px):
                t = start + (luminance(px) - low) / span * (end - start)
                new_row.append((*gradient_color(stops, t), px[3]))
            else:
                new_row.append(px)
        out.append(new_row)
    return out


class Vanilla:
    """Reads textures from the Minecraft client jar in the Gradle cache."""

    def __init__(self):
        home = Path(os.environ.get("USERPROFILE") or os.environ.get("HOME"))
        jars = sorted(glob.glob(str(home / ".gradle/caches/fabric-loom/*/minecraft-client.jar")))
        if not jars:
            sys.exit("Minecraft client jar not found. Run ./gradlew build once first.")
        self.jar = ZipFile(jars[-1])

    def texture(self, path):
        return read_png(self.jar.read(f"assets/minecraft/textures/{path}.png"))


# ---------------------------------------------------------------------------
# 3D models (GeckoLib)
# ---------------------------------------------------------------------------

# Armor bones GeckoLib looks up by name. Each armor bone sits inside a "biped" bone that
# follows the player's body part. Cube UVs match vanilla's armor texture layout, with the
# leggings (vanilla's second armor layer) moved to the bottom half of a 64x64 texture.
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

# Same boxes and UVs as vanilla's shield model.
SHIELD_CUBES = [
    ([-6, -11, 1], [12, 22, 1], [0, 0]),  # plate
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


def armor_texture(vanilla, gear):
    """Combine vanilla's two armor layers into one 64x64 texture for the GeckoLib model."""
    outer = vanilla.texture(f"entity/equipment/humanoid/{gear}")
    inner = vanilla.texture(f"entity/equipment/humanoid_leggings/{gear}")
    canvas = [[(0, 0, 0, 0)] * 64 for _ in range(64)]
    for y in range(32):
        canvas[y] = list(outer[y])
    for y in range(16):
        for x in range(16):
            canvas[32 + y][x] = inner[16 + y][x]
    return canvas


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------


def generate(vanilla, name, spec):
    tools, armor, raw_source, ore = spec["tools"], spec["armor"], spec["raw"], spec["ore"]
    ore_name = spec.get("ore_name", name)
    metal, raw = spec["metal"], spec["raw_colors"]
    ore_colors = spec.get("ore_colors", metal)
    item = ASSETS / "textures" / "item"
    block = ASSETS / "textures" / "block"

    def make(target, source, gradient, selector=everything, stretch=None):
        write_png(target, recolor(vanilla.texture(source), gradient, selector, stretch))

    for tool in ("sword", "pickaxe", "axe", "shovel", "hoe", "spear", "spear_in_hand"):
        make(item / f"{name}_{tool}.png", f"item/{tools}_{tool}", metal, tool_head)
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        make(item / f"{name}_{piece}.png", f"item/{armor}_{piece}", metal)
    make(item / f"{name}_ingot.png", "item/iron_ingot", metal)
    make(item / f"{name}_nugget.png", "item/iron_nugget", metal)
    make(item / f"raw_{ore_name}.png", f"item/raw_{raw_source}", raw)

    make(block / f"{name}_block.png", "block/iron_block", metal)
    make(block / f"raw_{ore_name}_block.png", f"block/raw_{raw_source}_block", raw)
    make(block / f"{ore_name}_ore.png", f"block/{ore}_ore", ore_colors, ore_gems, stretch=(0.35, 1.0))
    make(block / f"deepslate_{ore_name}_ore.png", f"block/deepslate_{ore}_ore", ore_colors, ore_gems, stretch=(0.35, 1.0))

    # 3D models. The shield keeps vanilla's wooden planks and gets a recolored metal rim.
    geo = ASSETS / "geckolib" / "models" / "item"
    write_json(geo / "armor" / f"{name}_armor.geo.json", armor_geo(name))
    write_png(item / "armor" / f"{name}_armor.png", recolor(armor_texture(vanilla, armor), metal, everything))
    write_json(geo / f"{name}_shield.geo.json", shield_geo(name))
    make(item / f"{name}_shield.png", "entity/shield/shield_base_nopattern", metal, grey_metal)
    print(f"generated assets for {name}")


if __name__ == "__main__":
    source = Vanilla()
    for material in sys.argv[1:] or list(MATERIALS):
        generate(source, material, MATERIALS[material])
