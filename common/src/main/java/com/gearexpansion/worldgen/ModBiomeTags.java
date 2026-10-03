package com.gearexpansion.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import com.gearexpansion.GearExpansion;

/** Biomes our ores generate in, beyond vanilla's broad tags. Filled by the data generator. */
public final class ModBiomeTags {
	/** Frozen Peaks and Ice Spikes, where Frostite grows in packed and blue ice. */
	public static final TagKey<Biome> HAS_FROSTITE_ORE = create("has_frostite_ore");
	/** Lush Caves, where Verdantite grows. */
	public static final TagKey<Biome> HAS_VERDANTITE_ORE = create("has_verdantite_ore");

	private ModBiomeTags() {
	}

	private static TagKey<Biome> create(String name) {
		return TagKey.create(Registries.BIOME, GearExpansion.id(name));
	}
}
