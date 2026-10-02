# Changelog

## 1.1.0 - Metals (in progress)

### Materials

- **Silver**: an iron-tier ore metal against the undead. Weapons deal extra damage to undead, armor takes less damage from them, and the shield knocks them back. Set bonus Blessed: Wither lasts half as long, and nearby undead glow.

## 1.0.1 - Compatibility

Finishes what 1.0 set out to do.

- **JEI and REI**: an Alloy Forge category lists every alloying recipe, with the cooking time, the faster time with a boost fuel, and the experience. REI needs to be installed on the server too for the recipes to show in multiplayer; JEI only needs to be on the client.
- **Jade**: looking at an Alloy Forge shows its inputs, fuel, progress, and result, and whether a boost fuel is burning.
- **Settings sync**: when you join a server, your game uses the server's settings while you're connected, so tooltips match what actually happens. Your own settings file is never changed, and your settings come back when you leave.
- **Equip sounds**: every armor set has its own equip sound.

### Known issues

- REI 26.3.823 on NeoForge can crash shortly after joining a world, inside REI's own code. JEI works on both loaders.
- Jade 26.3.4 crashes on startup by itself, with or without Gear Expansion, on both loaders. Jade 26.3.3 (Fabric) and 26.3.1 (NeoForge) work.

## 1.0.0 - Foundations

The first release, for Minecraft 26.3 on Fabric and NeoForge.

### Materials

Nine materials, each with a sword, pickaxe, axe, shovel, hoe, spear, 3D armor set, 3D shield, and a full set bonus:

- **Zinc**: a common early metal. Corrosion-proof gear (no durability loss in water). Set bonus Galvanized: Poison, Hunger, and Nausea last half as long.
- **Verdigris**: copper gear that oxidizes with use, from Fresh to Oxidized, trading speed for toughness. Wax it with honeycomb, scrape it with an axe. Set bonus Conductive: lightning is drawn to you, harmlessly, and charges you with Speed and Strength.
- **Rose Gold**: a gold and copper alloy with high enchantability that piglins treat as gold. Set bonus Lucky Charm: 25% more experience and better enchanting offers.
- **Aluminum**: smelted from Bauxite, which is richest in badlands and savannas. Fast attacks, armor that makes you quicker, and a shield that doesn't slow you. Set bonus Featherweight: less fall damage, stronger jumps, faster in water.
- **Brass**: a copper and zinc alloy. Weapons attack faster in combos. Set bonus Clockwork: moving and fighting wind a spring that gives Haste when full, and Spring Release dashes forward and knocks back mobs.
- **Emerald**: weapons deal 50% more damage to raiders, the pickaxe finds bonus experience, and armor adds Luck. Set bonus Merchant's Favor: Hero of the Village.
- **Amethyst**: crystal gear made from Resonant Crystals. Critical hits shatter over nearby mobs, the pickaxe chimes louder near more ore, and a perfectly timed block knocks attackers back. Set bonus Shatterguard: a crystal shell absorbs one hit, then regrows.
- **Titanium**: a rare deep ore with about twice diamond's durability. Set bonus Unbreakable Will: gear wears out half as fast, and low health grants Resistance.
- **Infernium**: a Nether ore, upgraded onto titanium gear with a template found in Bastion and Fortress chests. Fireproof gear that sets targets alight, a pickaxe that smelts what it mines, and fire-resistant armor. Set bonus Heat Core: swim in lava until the heat gauge fills, and Eruption sets nearby mobs alight.

### Features

- **Alloy Forge**: a workstation for Brass and Rose Gold. Burns normal fuel, runs twice as fast on blaze powder or lava, gives experience, and works with hoppers.
- **Set abilities** on the Set Ability key (R by default), with meters above the hotbar.
- **Advancements** for every material.
- **Creative tabs**: Blocks, Tools, Combat, and Ingredients.
- **Tooltips** for item traits and set progress.
- **Settings** for nearly every value, in `config/gearexpansion.json5`, with an in-game screen when YACL is installed.
- **Compatibility**: common `c:` tags for ores, ingots, nuggets, and storage blocks.
