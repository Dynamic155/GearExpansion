# Gear Expansion

A Minecraft mod that adds new tools, armor, and shields crafted from new ores, alloys, and fantasy materials. Each material has its own identity, and wearing a full armor set grants a set bonus.

The mod aims for a Vanilla+ feel: new content that fits naturally alongside vanilla progression.

## Status

Early development. Nothing is playable yet.

## Planned for 1.0

Materials:

- Titanium
- Zinc
- Brass
- Aluminum
- Rose Gold
- Verdigris (oxidizing copper)
- Emerald
- Amethyst
- Infernium

Features:

- A full tool set (sword, pickaxe, axe, shovel, hoe, spear), armor set, and shield for each material
- Full set bonuses, plus keybind abilities for some sets
- New ores generated in the world
- An Alloy Forge block for making alloys such as Brass and Rose Gold
- 3D armor models
- In-game configuration

Later updates will add more materials, grouped into themed releases.

## Requirements

- Minecraft 26.3
- Fabric Loader or NeoForge
- Architectury API
- GeckoLib
- Fabric API (Fabric only)

## Building from source

Requires a Java 25 JDK.

```
git clone <repository-url>
cd GearExpansion
./gradlew build
```

The built mod files are placed in `fabric/build/libs` and `neoforge/build/libs`.

To start a development copy of the game with the mod loaded:

```
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

## Project layout

The mod uses Architectury to support both loaders from one codebase.

- `common` holds the shared code and assets. Most of the mod lives here.
- `fabric` holds the Fabric entrypoints and `fabric.mod.json`.
- `neoforge` holds the NeoForge entrypoint and `neoforge.mods.toml`.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
