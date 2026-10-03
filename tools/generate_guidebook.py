"""Generate the Gear Expansion Guide, an optional Modonomicon book.

The whole book is written in this file: the categories, the general entries,
and one entry per material in MATERIALS. The script turns it into Modonomicon's
JSON files under

    common/src/main/resources/data/gearexpansion/modonomicon/books/guide/

and deletes whatever was there before, so edit this file, never the JSON.

Text is Modonomicon markdown: **bold**, *italics*, "- " bullet lists, and a blank
line between paragraphs. Write a literal percent sign as a plain "%" (the script
escapes it). Keep the text plain ASCII.

The script also checks that every item and recipe it mentions exists in the
generated data, so run the data generator first after adding a material.

To add a material, add a Material(...) to MATERIALS (in the order it should appear)
and run:

    python tools/generate_guidebook.py
"""

import json
import re
import shutil
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BOOK_DIR = ROOT / "common" / "src" / "main" / "resources" / "data" / "gearexpansion" / "modonomicon" / "books" / "guide"
GENERATED = ROOT / "common" / "src" / "main" / "generated"
LANG = GENERATED / "assets" / "gearexpansion" / "lang" / "en_us.json"
RECIPES = GENERATED / "data" / "gearexpansion" / "recipe"
EXTRA_RECIPES = ROOT / "common" / "src" / "main" / "resources" / "data" / "gearexpansion" / "recipe"

NS = "gearexpansion"


# ---------------------------------------------------------------------------
# Page helpers. Each returns one page; ids are filled in from the order.
# ---------------------------------------------------------------------------

def text(title, body):
    """A page of text with a title."""
    return {"type": "modonomicon:text", "title": title, "text": body, "show_title_separator": True}


def spotlight(item, title, body):
    """Shows one item, with a title and text under it."""
    return {"type": "modonomicon:spotlight", "item": {"id": item}, "title": title, "text": body}


def crafting(recipe1, recipe2=None, body=None):
    """One or two crafting recipes (ids without the namespace, e.g. "zinc_sword")."""
    return recipe_page("crafting_recipe", recipe1, recipe2, body)


def smelting(recipe1, recipe2=None, body=None):
    return recipe_page("smelting_recipe", recipe1, recipe2, body)


def blasting(recipe1, recipe2=None, body=None):
    return recipe_page("blasting_recipe", recipe1, recipe2, body)


def smithing(recipe1, recipe2=None, body=None):
    return recipe_page("smithing_recipe", recipe1, recipe2, body)


def recipe_page(kind, recipe1, recipe2, body):
    page = {"type": "modonomicon:" + kind, "recipe_id_1": f"{NS}:{recipe1}"}
    if recipe2:
        page["recipe_id_2"] = f"{NS}:{recipe2}"
    if body:
        page["text"] = body
    return page


# Text under a smelting recipe for raw ore, which a blast furnace also takes.
BLAST_TOO = "A blast furnace works too, and is faster."


def bullets(*lines):
    """A markdown bullet list."""
    return "\n".join("- " + line for line in lines)


# ---------------------------------------------------------------------------
# Book and categories
# ---------------------------------------------------------------------------

BOOK = {
    "name": "Gear Expansion Guide",
    "tooltip": "Every Gear Expansion material, the Alloy Forge, and set abilities.",
    "description": "A guide to the materials, gear, and set bonuses of Gear Expansion.",
    "model": "modonomicon:modonomicon_red",
    # Long pages flow onto the next page instead of shrinking the text.
    "default_allow_page_split": True,
}

# id: (name, icon, sort order).
CATEGORIES = {
    "getting_started": ("Getting Started", f"{NS}:zinc_pickaxe", 0),
    "materials": ("Materials", f"{NS}:titanium_ingot", 1),
    "alloy_forge": ("Alloy Forge", f"{NS}:alloy_forge", 2),
}


# Entry frames from Modonomicon's default theme.
FRAME = "modonomicon:modonomicon/themes/default/node/entry_backgrounds/square_gold"
START_FRAME = "modonomicon:modonomicon/themes/default/node/entry_backgrounds/star_gold"


@dataclass
class Entry:
    """A general entry, placed by hand at (x, y) in its category's overview.

    (0, 0) is the middle of the overview and one step is about half an entry's width, so keep
    entries in a row at least 2 steps apart, or 3 to 4 for long names. Lines are drawn from parents.
    """
    category: str
    id: str
    name: str
    description: str
    icon: str
    x: int
    y: int
    pages: list
    parents: list = field(default_factory=list)
    start: bool = False      # the first entry to read, drawn with a star frame


ENTRIES = [
    Entry("getting_started", "welcome", "Welcome", "What Gear Expansion adds", "minecraft:book", 0, -2, [
        text("Gear Expansion",
             "Gear Expansion adds new materials, each with tools, armor, and a shield. Every material "
             "plays differently, and wearing a full set gives a set bonus.\n\n"
             "Strong effects are kept in check by meters, cooldowns, and costs, and nearly every number "
             "can be changed in the settings."),
        text("Using This Book",
             "**Materials** has a page for every material: how to get it, what its gear does, its set "
             "bonus, and its recipes.\n\n"
             "**Alloy Forge** explains the workstation that makes Brass and Rose Gold.\n\n"
             "Hover over an entry to see what it covers."),
    ], start=True),
    Entry("getting_started", "crafting_gear", "Crafting the Gear", "Tools, armor, and shields", f"{NS}:zinc_sword", -3, 0, [
        text("Crafting the Gear",
             "Every material has a **sword, pickaxe, axe, shovel, hoe, spear, helmet, chestplate, "
             "leggings, boots, and shield**.\n\n"
             "Tools and armor use the same shapes as vanilla, with the material in place of iron."),
        text("Shields and Upgrades",
             "The **shield** is a vanilla shield with four of the material around it, one on each side.\n\n"
             "Infernium is the exception: it's an upgrade at a smithing table, like netherite."),
        crafting("zinc_pickaxe", "zinc_shield"),
    ], parents=["welcome"]),
    Entry("getting_started", "set_bonuses", "Set Bonuses", "Wearing a full set", f"{NS}:titanium_chestplate", 0, 0, [
        text("Set Bonuses",
             "Wearing all four armor pieces of one material gives its **set bonus**.\n\n"
             "**Tooltips** show each item's traits, the set bonus, and how many pieces you're wearing, "
             "like \"Titanium Set (3/4)\". The bonus is greyed out until you wear all four."),
    ], parents=["welcome"]),
    Entry("getting_started", "abilities", "Set Abilities", "The Set Ability key and HUD meters", f"{NS}:brass_chestplate", -2, 2, [
        text("Set Abilities",
             "A few full sets also have an **ability**, used with the **Set Ability key**: **R** by default. "
             "Change it under Controls, in the Gear Expansion category.\n\n"
             "The key uses the ability of the full set you're wearing:\n\n"
             + bullets("**Spring Release** (Brass)", "**Ground Slam** (Tungsten)", "**Eruption** (Infernium)")),
        text("HUD Meters",
             "Meters appear above the hotbar only when they matter:\n\n"
             + bullets("Brass's spring", "Infernium's heat gauge", "Amethyst's crystal shell", "the ability cooldown")),
    ], parents=["set_bonuses"]),
    Entry("getting_started", "settings", "Settings", "Changing the numbers", "minecraft:comparator", 2, 2, [
        text("Settings",
             "Settings are saved in the **gearexpansion.json5** file in the config folder, with an "
             "explanation above each one. You can turn each set bonus on or off and change its numbers."),
        text("In Game",
             "With **YACL** installed, you can also change settings in game: from Mod Menu on Fabric, or "
             "from the Mods list on NeoForge.\n\n"
             "On a server, the server's settings count. Your game uses them until you leave, and your own "
             "settings file isn't changed."),
    ], parents=["set_bonuses"]),
    Entry("getting_started", "other_features", "Other Features", "Tabs, advancements, and more", "minecraft:knowledge_book", 3, 0, [
        text("Other Features",
             bullets(
                 "**Creative tabs:** Blocks, Tools, Combat, and Ingredients, grouped by material.",
                 "**Advancements:** one for each material, the Alloy Forge, and the Infernium upgrade, in "
                 "their own tab.",
                 "**Equip sounds:** each armor set has its own.",
             )),
        text("Compatibility",
             "Ores, raw materials, ingots, nuggets, and storage blocks use the common c: tags, so other "
             "mods' machines and recipes recognize them.\n\n"
             "With JEI or REI installed, the Alloy Forge has its own recipe category."),
    ], parents=["welcome"]),

    Entry("alloy_forge", "alloy_forge", "The Alloy Forge", "A workstation for alloys", f"{NS}:alloy_forge", 0, -1, [
        spotlight(f"{NS}:alloy_forge", "The Alloy Forge",
                  "A workstation for making alloys. Put the ingredients in any of the three input slots "
                  "and fuel in the bottom slot.\n\n"
                  "Taking the results gives experience, like a furnace."),
        crafting("alloy_forge", body="A blast furnace in the middle, a block of copper above it, and bricks around the rest."),
    ], start=True),
    Entry("alloy_forge", "alloys", "Alloys", "Brass, Rose Gold, Sakura, and Steel", f"{NS}:brass_ingot", -2, 1, [
        text("Alloys",
             "The Alloy Forge makes four alloys, shown on the next pages.\n\n"
             "Recipes use the common ingot tags, so copper, zinc, gold, and iron from other mods work too."),
        spotlight(f"{NS}:brass_ingot", "Brass",
                  "**3 copper ingots** and **1 zinc ingot** make 4 Brass Ingots.\n\n"
                  "See [Brass](entry://materials/brass) for its gear."),
        spotlight(f"{NS}:rose_gold_ingot", "Rose Gold",
                  "**3 gold ingots** and **1 copper ingot** make 2 Rose Gold Ingots.\n\n"
                  "See [Rose Gold](entry://materials/rose_gold) for its gear."),
        spotlight(f"{NS}:sakura_ingot", "Sakura",
                  "**1 iron ingot** and **4 pink petals** make 1 Sakura Ingot. Pink petals carpet the "
                  "ground in cherry groves.\n\n"
                  "See [Sakura](entry://materials/sakura) for its gear."),
        spotlight(f"{NS}:steel_ingot", "Steel",
                  "**1 iron ingot** and **2 coal or charcoal** make 1 Steel Ingot. It takes a little longer "
                  "than the other alloys.\n\n"
                  "See [Steel](entry://materials/steel) for its gear."),
    ], parents=["alloy_forge"]),
    Entry("alloy_forge", "fuel", "Fuel and Hoppers", "Fuel, boosts, and automation", "minecraft:blaze_powder", 2, 1, [
        text("Fuel",
             "The Alloy Forge burns any normal furnace fuel.\n\n"
             "**Blaze powder** and **lava buckets** make it work twice as fast.\n\n"
             "Shift-clicking fuel puts it in the fuel slot. Once that is full, extra coal goes to the inputs "
             "for steel."),
        text("Automation",
             bullets("Hoppers on top fill the inputs.",
                     "Hoppers on the side add fuel.",
                     "Hoppers underneath take the results.")
             + "\n\nWith Jade installed, looking at a forge shows its inputs, fuel, progress, and result."),
    ], parents=["alloy_forge"]),
]


# ---------------------------------------------------------------------------
# Materials
# ---------------------------------------------------------------------------

# Tiers, weakest first. Each tier is a row in the Materials overview.
TIERS = ["Copper", "Iron", "Diamond", "Netherite"]
# Entries per row before a tier wraps onto another row.
ROW_LENGTH = 7


@dataclass
class Material:
    """One material entry. Text is markdown; ids are item or recipe ids without the namespace."""
    id: str                  # material name in item ids, e.g. "rose_gold"
    name: str                # display name
    tier: str                # one of TIERS: the pickaxe's mining tier
    item: str                # the material item shown first and used as the icon, e.g. "zinc_ingot"
    summary: str             # one or two sentences
    stats: dict              # "durability", "sword" (damage), "armor" (full set)
    obtaining: str           # how to get the material
    traits: list             # bullet points about the gear
    set_bonus: str           # name of the set bonus
    set_bonus_text: str
    ability: str = None      # optional ability, "Name (R): what it does"
    recipes: list = field(default_factory=list)  # material recipe pages, shown before the gear recipes
    upgrade: bool = False    # gear is a smithing upgrade (recipes are <id>_<gear>_smithing)
    parents: list = field(default_factory=list)  # material ids to draw a line from


MATERIALS = [
    Material(
        "zinc", "Zinc", "Copper", "zinc_ingot",
        "A common early metal, a step above copper.",
        {"durability": 260, "sword": 5, "armor": 11},
        "**Zinc Ore** is common between Y 0 and Y 64, in stone and deepslate. Mine it with a stone "
        "pickaxe or better, then smelt or blast the raw zinc.\n\n"
        "Zinc is also an ingredient in [Brass](entry://materials/brass).",
        ["**Corrosion-proof:** all zinc gear (tools, armor, and shield) loses no durability while "
         "you're in water.",
         "Copper-tier tools can mine iron ore but not diamond or gold ore."],
        "Galvanized", "Poison, Hunger, and Nausea last 50% shorter.",
        recipes=[smelting("zinc_ingot_from_smelting_raw_zinc", body=BLAST_TOO)],
    ),
    Material(
        "verdigris", "Verdigris", "Copper", "verdigris_plate",
        "Copper gear that oxidizes as you use it, like copper blocks.",
        {"durability": 240, "sword": 5, "armor": "12 (up to 16 oxidized)"},
        "Craft **Verdigris Plates** from 2 copper ingots, 1 honeycomb, and 1 green dye (makes 2). "
        "The gear is made from plates.",
        ["**Oxidation:** while held or worn, the gear slowly ages through four stages: Fresh, Exposed, "
         "Weathered, and Oxidized (about 20 minutes of use per stage). Its color and stats change too.",
         "Tools mine and attack slower as they oxidize (oxidized: 70% mining speed, -0.3 attack speed).",
         "Armor gets tougher (weathered and oxidized: +1 armor per piece, and up to +1 toughness).",
         "Fully oxidized gear has a 30% chance to ignore each point of wear.",
         "**Waxing:** right-click honeycomb onto the item in your inventory to stop it changing. "
         "Right-click an axe onto it to scrape off the wax, or, if it isn't waxed, to turn it back one stage."],
        "Conductive",
        "Natural lightning within 16 blocks strikes you instead, like a lightning rod, but it doesn't hurt "
        "you. A strike gives Speed and Strength for 10 seconds (once every 2 minutes).",
        recipes=[crafting("verdigris_plate")],
    ),
    Material(
        "rose_gold", "Rose Gold", "Copper", "rose_gold_ingot",
        "An elegant alloy of gold and copper, built for enchanting.",
        {"durability": 180, "sword": 4.5, "armor": 11},
        "In the [Alloy Forge](entry://alloy_forge/alloy_forge), 3 gold ingots and 1 copper ingot make "
        "2 Rose Gold Ingots.",
        ["**Gold's strengths, fixed:** fast mining (faster than iron) and very high enchantability "
         "(22 for tools, 25 for armor), with far more durability than gold.",
         "**Piglin-friendly:** piglins treat rose gold armor like gold armor and stay neutral."],
        "Lucky Charm",
        "25% more experience from experience orbs, and enchanting tables give better offers, as if 3 more "
        "bookshelves surrounded them.",
    ),
    Material(
        "aluminum", "Aluminum", "Iron", "aluminum_ingot",
        "A light, fast metal for exploring.",
        {"durability": 180, "sword": 5.5, "armor": 12},
        "Smelt **Raw Bauxite** from **Bauxite Ore**, a reddish ore found between Y 32 and Y 96 everywhere, "
        "and in much richer deposits near the surface of **badlands** and **savannas** (Y 48 to 128). "
        "Mine it with a stone pickaxe or better.",
        ["**Fast weapons and tools:** very fast mining, and +0.3 attack speed on every tool, but low durability.",
         "**Light armor:** each piece adds 3% movement speed (12% for the full set), with a little less "
         "protection than iron.",
         "**Lightweight shield:** no slowdown while blocking (you still can't sprint)."],
        "Featherweight",
        "50% less fall damage, 5% stronger jumps, and 33% faster movement in water (like Depth Strider I).",
        recipes=[smelting("aluminum_ingot_from_smelting_raw_bauxite", body=BLAST_TOO)],
    ),
    Material(
        "brass", "Brass", "Iron", "brass_ingot",
        "A steampunk alloy of copper and zinc.",
        {"durability": 300, "sword": 6, "armor": 15},
        "In the [Alloy Forge](entry://alloy_forge/alloy_forge), 3 copper ingots and 1 zinc ingot make "
        "4 Brass Ingots.",
        ["**Momentum:** each hit in a quick combo with a brass weapon attacks faster, up to 3 stacks. "
         "The combo ends 3 seconds after the last hit."],
        "Clockwork",
        "Walking, hitting, and blocking wind up a **spring** (shown above the hotbar). About 30 seconds of "
        "walking winds it fully; hits and blocks wind it faster. A fully wound spring gives Haste.",
        ability="**Spring Release (R):** with a fully wound spring, dash forward and hit every mob in front "
                "of you for 6 damage, knocking them back. This lets the spring go.",
        parents=["zinc"],
    ),
    Material(
        "silver", "Silver", "Iron", "silver_ingot",
        "A precious metal for fighting the undead.",
        {"durability": 280, "sword": 6, "armor": 15},
        "**Silver Ore** is found between Y -16 and Y 48, in stone and deepslate. Mine it with an iron "
        "pickaxe or better, then smelt or blast the raw silver.",
        ["**Hallowed:** silver swords, spears, and axes deal 4 extra damage to undead (zombies, skeletons, "
         "phantoms, the wither, and so on). It stacks with Smite.",
         "**Warding:** each armor piece takes 6% less damage from undead, including arrows from skeletons "
         "(24% for the full set).",
         "**Shield:** blocking an undead mob knocks it back hard."],
        "Blessed",
        "Wither lasts half as long, and undead within 8 blocks glow, so you can see them through walls.",
        recipes=[smelting("silver_ingot_from_smelting_raw_silver", body=BLAST_TOO)],
    ),
    Material(
        "emerald", "Emerald", "Iron", "minecraft:emerald",
        "Gear for traders and raid defenders.",
        {"durability": 500, "sword": 6, "armor": 15},
        "Made straight from **emeralds**, so it's expensive on purpose.",
        ["**Illager's Bane:** emerald swords, spears, and axes deal 50% more damage to raiders (pillagers, "
         "vindicators, evokers, ravagers, and witches).",
         "**Prospector:** the emerald pickaxe has a 25% chance to drop bonus experience when it mines an ore.",
         "**Lucky armor:** each piece adds 1 Luck, for better fishing and loot.",
         "**Shield:** blocking a raider knocks it back hard."],
        "Merchant's Favor",
        "You count as a Hero of the Village (level I): villagers trade at a discount and sometimes toss you gifts.",
    ),
    Material(
        "amethyst", "Amethyst", "Iron", "resonant_crystal",
        "Crystal gear: fragile, but highly enchantable and full of tricks.",
        {"durability": 220, "sword": 6, "armor": 15},
        "Craft a **Resonant Crystal** from 4 amethyst shards and 1 copper ingot. The gear is made from crystals.",
        ["**Shatter:** critical hits (attacking while falling) with an amethyst sword burst crystal over "
         "nearby mobs, dealing 3 damage to each.",
         "**Resonance:** when the amethyst pickaxe mines an ore, it chimes louder and higher the more of "
         "that ore is nearby. It's a hot-or-cold hint, not x-ray.",
         "**Perfect Block:** blocking with the amethyst shield right as you raise it knocks the attacker "
         "back with a chime.",
         "**Chiming armor:** the armor chimes softly when you're hit."],
        "Shatterguard",
        "A crystal shell completely absorbs one hit, shatters, and regrows over 45 seconds (shown above the hotbar).",
        recipes=[crafting("resonant_crystal")],
    ),
    # Cobalt sits left of Titanium in the book, so Titanium is centered above Infernium, its upgrade.
    Material(
        "cobalt", "Cobalt", "Diamond", "cobalt_ingot",
        "A deep blue Nether metal for fast mining.",
        {"durability": 1100, "sword": 6.5, "armor": 15},
        "**Cobalt Ore** is found throughout the Nether, from Y 0 to Y 128, in netherrack. Mine it with an "
        "iron pickaxe or better, then smelt or blast the raw cobalt.",
        ["**Swift:** cobalt tools are the fastest in the mod, faster than gold.",
         "**Quick Guard:** the cobalt shield blocks the moment you raise it, with no delay."],
        "Overdrive",
        "Every 6 blocks you mine in a row (with at most 2 seconds between them) adds a level of Haste, up "
        "to Haste II. Stop mining and it fades.",
        recipes=[smelting("cobalt_ingot_from_smelting_raw_cobalt", body=BLAST_TOO)],
    ),
    Material(
        "titanium", "Titanium", "Diamond", "titanium_ingot",
        "The reliable workhorse: diamond-level gear that almost never breaks.",
        {"durability": 3000, "sword": 7, "armor": 20},
        "**Titanium Ore** is rare, deep underground between Y -64 and Y -16, in small, mostly buried veins. "
        "Mine it with a diamond pickaxe or better, then smelt or blast the raw titanium.\n\n"
        "Titanium gear is the base for [Infernium](entry://materials/infernium).",
        ["**Tools:** diamond tier and speed, with 3000 durability (about twice diamond), but low enchantability.",
         "**Armor:** diamond-level protection with extra toughness (2.5 per piece) and a little knockback resistance.",
         "**Shield:** 1000 durability, and axes disable it for less than half the usual time."],
        "Unbreakable Will",
        "Everything you use loses 50% less durability. Dropping below 30% health gives Resistance I for "
        "5 seconds (once a minute).",
        recipes=[smelting("titanium_ingot_from_smelting_raw_titanium", body=BLAST_TOO)],
    ),
    Material(
        "sakura", "Sakura", "Iron", "sakura_ingot",
        "A pretty, gentle set made from cherry blossoms.",
        {"durability": 300, "sword": 6, "armor": 15},
        "In the [Alloy Forge](entry://alloy_forge/alloy_forge), 1 iron ingot and 4 pink petals make "
        "1 Sakura Ingot. Pink petals carpet the ground in cherry groves.",
        ["**Gear:** about iron's strength, with very high enchantability (22).",
         "**Blossoming:** finishing off a mob with a sakura sword, spear, or axe heals one heart.",
         "**Petal Guard:** blocking with the sakura shield heals half a heart, at most once a second."],
        "Hanami",
        "Regeneration while within 3 blocks of flowers, cherry leaves, or pink petals, and you leave a "
        "trail of falling cherry petals as you walk.",
    ),
    Material(
        "steel", "Steel", "Iron", "steel_ingot",
        "Plain, dependable, and tough. No gimmicks.",
        {"durability": 750, "sword": 6.5, "armor": 16},
        "In the [Alloy Forge](entry://alloy_forge/alloy_forge), 1 iron ingot and 2 coal (or charcoal) make "
        "1 Steel Ingot.",
        ["**Gear:** iron tier, with about three times iron's durability and a little more damage.",
         "**Armor:** 1 toughness per piece.",
         "**Reinforced shield:** 1200 durability, far more than a normal shield."],
        "Hardened",
        "+2 armor toughness.",
    ),
    Material(
        "tungsten", "Tungsten", "Diamond", "tungsten_ingot",
        "The tank: heavy, slow, and immovable.",
        {"durability": 2400, "sword": 8, "armor": 20},
        "**Tungsten Ore** is very rare, at the very bottom of the world (Y -64 to Y -48), in small, mostly "
        "buried veins. Mine it with a diamond pickaxe or better, then smelt or blast the raw tungsten.",
        ["**Weapons:** slow but hard-hitting; a tungsten sword hits as hard as netherite.",
         "**Crushing:** tungsten axes and spears knock targets back further.",
         "**Tools:** 2400 durability, but they mine slowly.",
         "**Heavy armor:** 3 toughness and the most knockback resistance in the mod (0.15 per piece), but "
         "each piece makes you 4% slower.",
         "**Bulwark shield:** slow to raise, but 1500 durability and hard for axes to disable."],
        "Immovable",
        "No knockback at all while sneaking, and explosions deal 40% less damage.",
        ability="**Ground Slam (R):** stomp the ground to hurt, knock back, and slow every mob within 5 "
                "blocks. 20 second cooldown.",
        recipes=[smelting("tungsten_ingot_from_smelting_raw_tungsten", body=BLAST_TOO)],
    ),
    Material(
        "infernium", "Infernium", "Netherite", "infernium_ingot",
        "The endgame metal of the Nether. Its 3D armor glows.",
        {"durability": 2200, "sword": 8, "armor": 20},
        "1. Find **Infernium Ore** in the Nether, between Y 10 and Y 40 in netherrack. Mine it with a diamond "
        "pickaxe or better. It's hot: mining it without Fire Resistance sets you on fire for a moment.\n"
        "2. Raw Infernium can only be smelted in a **blast furnace**.\n"
        "3. Find an **Infernium Upgrade Smithing Template**: about half of Bastion treasure chests have one, "
        "and other Bastion chests (10%) and Nether Fortress chests (8%) sometimes do. Copy a template with "
        "7 titanium ingots, 1 netherrack, and the template, which makes 2.\n"
        "4. At a smithing table, combine the template, a piece of **titanium gear**, and an **Infernium Ingot**.",
        ["**Fireproof:** infernium items don't burn in fire or lava.",
         "**Searing weapons:** swords, spears, and axes set targets on fire, and deal 3 extra damage to "
         "burning targets.",
         "**Smelting pickaxe:** the infernium pickaxe smelts what it mines (iron ore drops iron ingots, and so on).",
         "**Fire Ward:** each armor piece blocks 15% of fire damage (60% for the full set).",
         "**Shield:** blocking a melee attack sets the attacker on fire."],
        "Heat Core",
        "Walk and swim in lava unharmed for up to 10 seconds, shown by the heat gauge above the hotbar. The "
        "gauge fills in lava and cools outside it. Fire doesn't burn you while the gauge has room, and melee "
        "attackers catch fire.",
        ability="**Eruption (R):** a ring of fire sets nearby mobs alight and hurts them. The more heat you've "
                "stored, the bigger and hotter it is, and it uses up the heat. 30 second cooldown.",
        recipes=[blasting("infernium_ingot_from_blasting_raw_infernium"),
                 crafting("infernium_upgrade_smithing_template")],
        upgrade=True,
        parents=["titanium"],
    ),
]


def material_pages(m):
    item = m.item if ":" in m.item else f"{NS}:{m.item}"
    stats = bullets(f"**Tier:** {m.tier}",
                    f"**Durability:** {m.stats['durability']}",
                    f"**Sword damage:** {m.stats['sword']}",
                    f"**Armor (full set):** {m.stats['armor']}")
    set_text = "**Full set bonus.** " + m.set_bonus_text
    if m.ability:
        set_text += "\n\n" + m.ability
    pages = [
        spotlight(item, m.name, m.summary + "\n\n" + stats),
        text("Getting It", m.obtaining),
        text("The Gear", bullets(*m.traits)),
        spotlight(f"{NS}:{m.id}_chestplate", m.set_bonus, set_text),
    ]
    pages += m.recipes
    if m.upgrade:
        pages.append(smithing(f"{m.id}_pickaxe_smithing", f"{m.id}_sword_smithing"))
        pages.append(smithing(f"{m.id}_chestplate_smithing", f"{m.id}_shield_smithing"))
    else:
        pages.append(crafting(f"{m.id}_pickaxe", f"{m.id}_sword"))
        pages.append(crafting(f"{m.id}_chestplate", f"{m.id}_shield"))
    return pages


def material_entries():
    """One row per tier (wrapping long tiers), with rows and entries 2 steps apart, centered."""
    rows = []
    for tier in TIERS:
        in_tier = [m for m in MATERIALS if m.tier == tier]
        rows += [in_tier[start:start + ROW_LENGTH] for start in range(0, len(in_tier), ROW_LENGTH)]
    entries = []
    for row, chunk in enumerate(rows):
        y = row * 2 - (len(rows) - 1)
        for column, m in enumerate(chunk):
            x = column * 2 - (len(chunk) - 1)
            item = m.item if ":" in m.item else f"{NS}:{m.item}"
            entries.append(Entry("materials", m.id, m.name, f"{m.tier} tier. Set bonus: {m.set_bonus}",
                                 item, x, y, material_pages(m), parents=m.parents))
    unknown = [m.id for m in MATERIALS if m.tier not in TIERS]
    if unknown:
        fail(f"unknown tier for {unknown}; use one of {TIERS}")
    return entries


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------

ERRORS = []


def fail(message):
    ERRORS.append(message)


def escape(value):
    """Book strings go through String.format, so a literal % must be written as %%."""
    return value.replace("%", "%%")


def check_text(where, value):
    if not value.isascii():
        bad = sorted({c for c in value if not c.isascii()})
        fail(f"{where}: use plain ASCII (found {bad})")


def known_items():
    lang = json.loads(LANG.read_text(encoding="utf-8"))
    items = set()
    for key in lang:
        parts = key.split(".")
        if parts[0] in ("item", "block") and parts[1] == NS and len(parts) == 3:
            items.add(f"{NS}:{parts[2]}")
    return items


def known_recipes():
    recipes = set()
    for folder in (RECIPES, EXTRA_RECIPES):
        if folder.exists():
            recipes.update(f"{NS}:{p.stem}" for p in folder.glob("*.json"))
    return recipes


def check_ids(where, page, items, recipes):
    if "item" in page:
        item = page["item"]["id"]
        if item.startswith(NS + ":") and item not in items:
            fail(f"{where}: unknown item {item}")
    for key in ("recipe_id_1", "recipe_id_2"):
        if key in page and page[key] not in recipes:
            fail(f"{where}: unknown recipe {page[key]}")


def prepare_page(where, page, items, recipes):
    check_ids(where, page, items, recipes)
    for key in ("title", "text"):
        if key in page:
            check_text(where, page[key])
            if key == "title" and "%" in page[key]:
                fail(f"{where}: titles can't contain %")
            page[key] = escape(page[key])
    return page


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def main():
    items = known_items()
    recipes = known_recipes()
    entries = ENTRIES + material_entries()

    ids = {(e.category, e.id) for e in entries}
    for e in entries:
        if e.category not in CATEGORIES:
            fail(f"{e.id}: unknown category {e.category}")
        for parent in e.parents:
            if (e.category, parent) not in ids:
                fail(f"{e.id}: unknown parent {parent}")
        for value in (e.name, e.description):
            check_text(e.id, value)
            if "%" in value:
                fail(f"{e.id}: names and descriptions can't contain %")
        if e.icon.startswith(NS + ":") and e.icon not in items:
            fail(f"{e.id}: unknown icon {e.icon}")
    for e in entries:
        for link in re.findall(r"\(entry://([^)#@]+)", json.dumps([p.get("text", "") for p in e.pages])):
            if tuple(link.split("/", 1)) not in ids:
                fail(f"{e.id}: link to unknown entry {link}")

    if ERRORS:
        for error in ERRORS:
            print("ERROR: " + error)
        sys.exit(1)

    if BOOK_DIR.exists():
        shutil.rmtree(BOOK_DIR)

    book = dict(BOOK)
    for key in ("name", "tooltip", "description"):
        book[key] = escape(book[key])
    write_json(BOOK_DIR / "book.json", book)

    for category, (name, icon, sort) in CATEGORIES.items():
        write_json(BOOK_DIR / "categories" / f"{category}.json", {
            "name": name,
            "icon": icon,
            "sort_number": sort,
        })

    for e in entries:
        pages = []
        for number, page in enumerate(e.pages):
            page = prepare_page(f"{e.category}/{e.id} page {number + 1}", dict(page), items, recipes)
            page["id"] = f"page{number + 1}"
            pages.append(page)
        write_json(BOOK_DIR / "entries" / e.category / f"{e.id}.json", {
            "type": "modonomicon:content",
            "id": f"{NS}:{e.category}/{e.id}",
            "category": f"{NS}:{e.category}",
            "name": e.name,
            "description": e.description,
            "icon": e.icon,
            "background": {"sprite": START_FRAME if e.start else FRAME, "width": 26, "height": 26},
            "x": e.x,
            "y": e.y,
            "name_style": {"render_name": "bottom", "show_name_before_unlock": True},
            "parents": [{"entry": f"{NS}:{e.category}/{p}"} for p in e.parents],
            "pages": pages,
        })

    if ERRORS:
        for error in ERRORS:
            print("ERROR: " + error)
        sys.exit(1)
    print(f"Wrote {len(entries)} entries ({len(MATERIALS)} materials) to {BOOK_DIR.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
