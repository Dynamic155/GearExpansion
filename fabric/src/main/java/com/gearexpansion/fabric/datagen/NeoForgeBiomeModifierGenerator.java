package com.gearexpansion.fabric.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.MaterialSet.OreGeneration;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.worldgen.ModOres;

/**
 * Writes NeoForge biome modifiers that add each ore vein to its biomes.
 * Fabric ignores these files and adds the ores in code instead.
 */
final class NeoForgeBiomeModifierGenerator implements DataProvider {
	private final PackOutput.PathProvider paths;

	NeoForgeBiomeModifierGenerator(FabricPackOutput output) {
		this.paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "neoforge/biome_modifier");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		List<CompletableFuture<?>> writes = new ArrayList<>();
		for (MaterialSet set : ModMaterials.ALL) {
			for (OreGeneration ore : set.oreGeneration) {
				JsonObject json = new JsonObject();
				json.addProperty("type", "neoforge:add_features");
				json.addProperty("biomes", "#" + ore.biomes().location());
				json.addProperty("features", ModOres.placedKey(set, ore).identifier().toString());
				json.addProperty("step", "underground_ores");
				writes.add(DataProvider.saveStable(output, json, paths.json(GearExpansion.id(ModOres.name(set, ore)))));
			}
		}
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	@Override
	public String getName() {
		return "Gear Expansion NeoForge Biome Modifiers";
	}
}
