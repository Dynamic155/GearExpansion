"""Generate textures and 3D models for Gear Expansion materials.

Textures are recolored from vanilla Minecraft textures, read straight from the
Minecraft jar in the Gradle cache (run a Gradle build once so it exists). Each
material picks vanilla textures to start from and color gradients to map onto:
pixels are recolored by brightness, so shading and detail carry over.

Usage:
    python tools/generate_assets.py               # all materials and the Alloy Forge
    python tools/generate_assets.py titanium      # one material
    python tools/generate_assets.py alloy_forge   # only the Alloy Forge
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
#   tools / armor   vanilla item prefix for the six tools / armor icons (e.g. "iron", "golden", "chainmail")
#   armor_entity    vanilla worn-armor texture name, if different from armor (gold armor items are "golden_", worn is "gold")
#   ingot / nugget / block   vanilla textures for those (default: iron's)
#   raw / ore       vanilla prefix for the raw item and raw block / the ore blocks; leave out for alloys
#   ore_name        name of our ore and raw item, if different from the material (Aluminum uses "bauxite")
#   metal           gradient for tools, armor, ingot, nugget, block, and shield rim
#   raw_colors      gradient for the raw item and raw block
#   ore_colors      gradient for the mineral in the ore blocks (defaults to metal)
#   stretch         spread the source's brightness over the whole gradient; for dark sources like netherite
#   base_item       gear is made from an existing item (Emerald): no ingot, nugget, or block textures
#   material_item   (name, vanilla texture, gradient) for a crafted material item like the Resonant Crystal
#   ore_base        for Nether ores: the plain stone texture, so only pixels that differ (the mineral) are recolored
#   template        vanilla texture to recolor for an upgrade smithing template
#   glowmask        make _glowmask textures for the 3D armor and shield from their brightest pixels
#   stages          (suffix, gradient) pairs: extra copies of all gear textures, e.g. Verdigris oxidation stages
MATERIALS = {
    "zinc": {
        "tools": "iron", "armor": "chainmail", "raw": "iron", "ore": "iron",
        # Matte blue-green grey, lower contrast than iron.
        "metal": ["#1E2A2A", "#394B4B", "#5A7170", "#7E9794", "#A6BCB8", "#CEDFDA"],
        "raw_colors": ["#25272A", "#40454A", "#5E666B", "#7F898D", "#A3ACAE", "#C7CDCD"],
    },
    "rose_gold": {
        "tools": "golden", "armor": "golden", "armor_entity": "gold",
        "ingot": "gold_ingot", "nugget": "gold_nugget", "block": "gold_block",
        # Warm pink gold.
        "metal": ["#3A1E1C", "#6E3B36", "#A15F55", "#C98577", "#E6AE9D", "#F8D8CB"],
    },
    "aluminum": {
        "tools": "diamond", "armor": "diamond", "raw": "copper", "ore": "copper", "ore_name": "bauxite",
        # Bright, clean silver-white.
        "metal": ["#3A3F46", "#646B74", "#8F97A1", "#B8C0C8", "#DCE2E8", "#FAFCFD"],
        # Bauxite: reddish-brown clay rock.
        "raw_colors": ["#2E1610", "#5A2A1C", "#83412A", "#A85C3C", "#C98059", "#E3A77F"],
        "ore_colors": ["#2E1610", "#5A2A1C", "#83412A", "#A85C3C", "#C98059", "#E3A77F"],
    },
    "brass": {
        "tools": "netherite", "armor": "netherite",
        "ingot": "netherite_ingot", "nugget": "gold_nugget", "block": "copper_block",
        # Warm yellow-brown brass. Netherite is dark, so stretch its brightness over the gradient.
        "metal": ["#3B2A10", "#6B4E1D", "#9A7430", "#C29A45", "#DEBE6A", "#F3DE9C"],
        "stretch": True,
    },
    "verdigris": {
        "tools": "copper", "armor": "copper",
        "material_item": ("verdigris_plate", "copper_ingot", ["#0E2A24", "#1D4A40", "#2F6E5F", "#4A9580", "#74BBA2", "#A8DCC6"]),
        # Fresh copper; the other stages get more green patina.
        "metal": ["#3A1A0E", "#6B3519", "#A05426", "#C9773A", "#E59C5C", "#F6C391"],
        "stages": [
            ("_exposed", ["#33231A", "#5E412D", "#8A6347", "#AB8263", "#C9A585", "#E2CBB0"]),
            ("_weathered", ["#1E2A1E", "#3A4F38", "#5B7656", "#7D9773", "#A2B996", "#C8D9BD"]),
            ("_oxidized", ["#0E2A24", "#1D4A40", "#2F6E5F", "#4A9580", "#74BBA2", "#A8DCC6"]),
        ],
    },
    "silver": {
        "tools": "iron", "armor": "iron", "raw": "iron", "ore": "iron",
        # Polished silver with a cool lavender sheen, so it reads differently from aluminum's flat white.
        "metal": ["#211D2C", "#423C58", "#6A6386", "#958FB2", "#C4BFDA", "#F1EEFA"],
        "raw_colors": ["#2A2A2D", "#47474D", "#6A6B72", "#90919A", "#B9BBC3", "#E0E2E8"],
    },
    "emerald": {
        "tools": "diamond", "armor": "diamond", "base_item": True,
        # Rich emerald green.
        "metal": ["#06281A", "#0B4A2E", "#127345", "#1E9E5E", "#4FCB86", "#A4F0C2"],
    },
    "amethyst": {
        "tools": "copper", "armor": "copper",
        "material_item": ("resonant_crystal", "amethyst_shard", ["#2A1442", "#4A2470", "#6E3BA0", "#9A62C8", "#C495E6", "#EAD3FA"]),
        # Purple crystal.
        "metal": ["#2A1442", "#4A2470", "#6E3BA0", "#9A62C8", "#C495E6", "#EAD3FA"],
    },
    "infernium": {
        "tools": "netherite", "armor": "netherite",
        "ingot": "netherite_ingot", "nugget": "iron_nugget", "block": "netherite_block",
        "raw": "gold", "ore": "nether_gold", "ore_base": "netherrack",
        "template": "netherite_upgrade_smithing_template",
        # Charred black-red to molten orange. Netherite is dark, so stretch it.
        "metal": ["#160806", "#3A120A", "#6A200E", "#A83A12", "#E56A1A", "#FFC046"],
        "raw_colors": ["#2A0E08", "#55200F", "#8A3414", "#C2501A", "#EE7D2A", "#FFB35A"],
        "ore_colors": ["#3A0E06", "#7A2208", "#C2400C", "#F06A14", "#FFA030", "#FFE07A"],
        "stretch": True,
        "glowmask": True,
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


def wooden_mask(vanilla, prefix, tool):
    """Which pixels of a vanilla tool are its wooden handle.

    Most metals are easy to tell from wood by color. Gold and copper are too close to wood, so for those
    the handle is found on the iron version instead: tools of one kind share the same handle pixels.
    """
    source = "iron" if prefix in ("golden", "copper") else prefix
    pixels = vanilla.texture(f"item/{source}_{tool}")
    return {(x, y) for y, row in enumerate(pixels) for x, px in enumerate(row) if px[3] > 0 and not tool_head(px)}


def glowmask(pixels, threshold=0.62):
    """The brightest, warmest pixels of a texture, for GeckoLib's glowing layer. Everything else is transparent."""
    return [[px if px[3] > 0 and luminance(px) >= threshold else (0, 0, 0, 0) for px in row] for row in pixels]


def differs_from(base):
    """Selects pixels that differ from the same pixel of {@code base}: the mineral in an ore texture."""
    def selector(px):
        x, y = px[4], px[5]
        return px[:4] != base[y][x]
    return selector


def indexed(pixels):
    """Adds each pixel's (x, y) so a selector can look at where it is."""
    return [[(*px, x, y) for x, px in enumerate(row)] for y, row in enumerate(pixels)]


def unindexed(pixels):
    return [[px[:4] for px in row] for row in pixels]


def generate_gear(vanilla, name, spec, metal, suffix, gear_stretch):
    """Tools, armor icons, worn armor, and the shield, with one gradient. Stages call this once each."""
    tools, armor = spec["tools"], spec["armor"]
    armor_entity = spec.get("armor_entity", armor)
    item = ASSETS / "textures" / "item"

    for tool in ("sword", "pickaxe", "axe", "shovel", "hoe", "spear", "spear_in_hand"):
        handle = wooden_mask(vanilla, tools, tool)
        pixels = indexed(vanilla.texture(f"item/{tools}_{tool}"))
        recolored = recolor(pixels, metal, lambda px: (px[4], px[5]) not in handle, gear_stretch)
        write_png(item / f"{name}_{tool}{suffix}.png", unindexed(recolored))
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        write_png(item / f"{name}_{piece}{suffix}.png", recolor(vanilla.texture(f"item/{armor}_{piece}"), metal, everything, gear_stretch))

    armor_texture_pixels = recolor(armor_texture(vanilla, armor_entity), metal, everything, gear_stretch)
    write_png(item / "armor" / f"{name}_armor{suffix}.png", armor_texture_pixels)
    shield = recolor(vanilla.texture("entity/shield/shield_base_nopattern"), metal, grey_metal)
    write_png(item / f"{name}_shield{suffix}.png", shield)
    if spec.get("glowmask"):
        write_png(item / "armor" / f"{name}_armor{suffix}_glowmask.png", glowmask(armor_texture_pixels))
        write_png(item / f"{name}_shield{suffix}_glowmask.png", glowmask(shield))


def generate(vanilla, name, spec):
    metal = spec["metal"]
    gear_stretch = (0.0, 1.0) if spec.get("stretch") else None
    item = ASSETS / "textures" / "item"
    block = ASSETS / "textures" / "block"

    def make(target, source, gradient, selector=everything, stretch=None):
        write_png(target, recolor(vanilla.texture(source), gradient, selector, stretch))

    generate_gear(vanilla, name, spec, metal, "", gear_stretch)
    for suffix, gradient in spec.get("stages", []):
        generate_gear(vanilla, name, spec, gradient, suffix, gear_stretch)

    if "material_item" in spec:
        item_name, source, gradient = spec["material_item"]
        make(item / f"{item_name}.png", f"item/{source}", gradient)
    elif not spec.get("base_item"):
        make(item / f"{name}_ingot.png", "item/" + spec.get("ingot", "iron_ingot"), metal, stretch=gear_stretch)
        make(item / f"{name}_nugget.png", "item/" + spec.get("nugget", "iron_nugget"), metal, stretch=gear_stretch)
        make(block / f"{name}_block.png", "block/" + spec.get("block", "iron_block"), metal, stretch=gear_stretch)

    if "template" in spec:
        make(item / f"{name}_upgrade_smithing_template.png", f"item/{spec['template']}", metal, stretch=(0.0, 1.0))

    if "ore" in spec:
        raw_source, ore = spec["raw"], spec["ore"]
        ore_name = spec.get("ore_name", name)
        raw = spec["raw_colors"]
        ore_colors = spec.get("ore_colors", metal)
        make(item / f"raw_{ore_name}.png", f"item/raw_{raw_source}", raw)
        make(block / f"raw_{ore_name}_block.png", f"block/raw_{raw_source}_block", raw)
        if "ore_base" in spec:
            # Nether ores: netherrack is too colorful to tell apart by color, so recolor what differs from it.
            base = vanilla.texture(f"block/{spec['ore_base']}")
            pixels = indexed(vanilla.texture(f"block/{ore}_ore"))
            write_png(block / f"{ore_name}_ore.png", unindexed(recolor(pixels, ore_colors, differs_from(base), (0.3, 1.0))))
        else:
            make(block / f"{ore_name}_ore.png", f"block/{ore}_ore", ore_colors, ore_gems, stretch=(0.35, 1.0))
            make(block / f"deepslate_{ore_name}_ore.png", f"block/deepslate_{ore}_ore", ore_colors, ore_gems, stretch=(0.35, 1.0))

    # 3D models: one armor model and one shield model per material.
    geo = ASSETS / "geckolib" / "models" / "item"
    write_json(geo / "armor" / f"{name}_armor.geo.json", armor_geo(name))
    write_json(geo / f"{name}_shield.geo.json", shield_geo(name))
    print(f"generated assets for {name}")


# ---------------------------------------------------------------------------
# Alloy Forge
# ---------------------------------------------------------------------------

# Warm copper and bronze, dark to light, for the forge's recolored blast furnace stone.
ALLOY_FORGE_COLORS = ["#1E0F08", "#5A3320", "#8A5530", "#B77A45", "#D9A066", "#F2CE96"]

# Where things sit in vanilla's furnace screen texture (x, y, width, height), and where the forge
# screen puts them. They must match the slot positions in AlloyForgeMenu and the sprite
# positions in AlloyForgeScreen (each slot's frame is one pixel up and left of the item).
FURNACE_SLOT = (55, 16, 18, 18)
FURNACE_FLAME = (56, 36, 14, 14)
FURNACE_ARROW = (79, 34, 24, 17)
FURNACE_RESULT = (111, 30, 26, 26)
FORGE_INPUTS = [(37, 16), (55, 16), (73, 16)]
FORGE_FUEL = (55, 52)
FORGE_FLAME = (56, 36)
FORGE_ARROW = (88, 34)
FORGE_RESULT = (120, 30)
SCREEN_WIDTH, SCREEN_HEIGHT = 176, 166
# The part of the screen above the player inventory that gets rebuilt, and its background color.
FURNACE_PANEL = (7, 14, 162, 58)


def copy_region(source, target, region, to):
    x, y, width, height = region
    tx, ty = to
    for dy in range(height):
        for dx in range(width):
            target[ty + dy][tx + dx] = source[y + dy][x + dx]


def alloy_forge_screen(vanilla):
    """Vanilla's furnace screen with the single input slot replaced by three in a row.

    The empty flame and arrow outlines stay (the arrow moves right to make room), and the
    progress sprites are vanilla's furnace ones, drawn over them by the screen.
    """
    furnace = vanilla.texture("gui/container/furnace")
    screen = [list(row) for row in furnace]
    x, y, width, height = FURNACE_PANEL
    background = furnace[y][x]
    for dy in range(height):
        for dx in range(width):
            screen[y + dy][x + dx] = background
    for slot in FORGE_INPUTS:
        copy_region(furnace, screen, FURNACE_SLOT, slot)
    copy_region(furnace, screen, FURNACE_SLOT, FORGE_FUEL)
    copy_region(furnace, screen, FURNACE_FLAME, FORGE_FLAME)
    copy_region(furnace, screen, FURNACE_ARROW, FORGE_ARROW)
    copy_region(furnace, screen, FURNACE_RESULT, FORGE_RESULT)
    # Vanilla's texture is 256x256 with the screen in the top-left corner; keep just the screen.
    return [row[:SCREEN_WIDTH] for row in screen[:SCREEN_HEIGHT]]


def generate_alloy_forge(vanilla):
    """Alloy Forge block textures (a copper-toned blast furnace) and its screen texture."""
    block = ASSETS / "textures" / "block"
    for face in ("front", "front_on", "side", "top"):
        # Only the grey stone and iron are recolored, so the fire in front_on keeps its colors.
        pixels = vanilla.texture(f"block/blast_furnace_{face}")
        write_png(block / f"alloy_forge_{face}.png", recolor(pixels, ALLOY_FORGE_COLORS, grey_metal))
    # The lit front is animated; reuse vanilla's animation settings.
    mcmeta = vanilla.jar.read("assets/minecraft/textures/block/blast_furnace_front_on.png.mcmeta")
    (block / "alloy_forge_front_on.png.mcmeta").write_bytes(mcmeta)

    write_png(ASSETS / "textures" / "gui" / "container" / "alloy_forge.png", alloy_forge_screen(vanilla))
    print("generated assets for the alloy forge")


EXTRAS = {"alloy_forge": generate_alloy_forge}


if __name__ == "__main__":
    source = Vanilla()
    for name in sys.argv[1:] or list(MATERIALS) + list(EXTRAS):
        if name in EXTRAS:
            EXTRAS[name](source)
        else:
            generate(source, name, MATERIALS[name])
