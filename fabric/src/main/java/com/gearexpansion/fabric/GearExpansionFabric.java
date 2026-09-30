package com.gearexpansion.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.level.levelgen.GenerationStep;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.worldgen.ModOres;

public final class GearExpansionFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		GearExpansion.init();

		// NeoForge adds ores through generated biome modifier files; Fabric does it here.
		for (MaterialSet set : ModMaterials.ALL) {
			BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, ModOres.placedKey(set));
		}
	}
}
