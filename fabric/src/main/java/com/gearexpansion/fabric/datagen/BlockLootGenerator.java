package com.gearexpansion.fabric.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.core.HolderLookup;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.registry.ModBlocks;

final class BlockLootGenerator extends FabricBlockLootSubProvider {
	BlockLootGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generate() {
		dropSelf(ModBlocks.ALLOY_FORGE.get());

		for (MaterialSet set : ModMaterials.ALL) {
			if (set.storageBlock != null) {
				dropSelf(set.storageBlock.get());
			}
			if (set.hasOre) {
				// Ores drop raw material, affected by Fortune; Silk Touch drops the ore itself. Same as vanilla iron.
				add(set.ore.get(), block -> createOreDrop(block, set.rawItem.get()));
				if (set.deepslateOre != null) {
					add(set.deepslateOre.get(), block -> createOreDrop(block, set.rawItem.get()));
				}
				dropSelf(set.rawStorageBlock.get());
			}
		}
	}
}
