package com.gearexpansion.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.world.level.levelgen.GenerationStep;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.MaterialSet.OreGeneration;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.recipe.ModRecipes;
import com.gearexpansion.worldgen.ModOres;

public final class GearExpansionFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		GearExpansion.init();

		// NeoForge adds ores through generated biome modifier files; Fabric does it here.
		for (MaterialSet set : ModMaterials.ALL) {
			for (OreGeneration ore : set.oreGeneration) {
				BiomeModifications.addFeature(BiomeSelectors.tag(ore.biomes()), GenerationStep.Decoration.UNDERGROUND_ORES, ModOres.placedKey(set, ore));
			}
		}

		// Send alloying recipes to clients, for recipe viewers such as JEI. See ClientAlloyingRecipes.
		RecipeSynchronization.synchronizeRecipeSerializer(ModRecipes.ALLOYING_SERIALIZER.get());
	}
}
