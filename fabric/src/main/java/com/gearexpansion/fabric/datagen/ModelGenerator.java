package com.gearexpansion.fabric.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/**
 * Block and item models. Shields use GeckoLib models instead, written by {@link GeckoLibItemModelGenerator}.
 */
final class ModelGenerator extends FabricModelProvider {
	ModelGenerator(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
		for (MaterialSet set : ModMaterials.ALL) {
			set.blocks().forEach(block -> generators.createTrivialCube(block.get()));
		}
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		for (MaterialSet set : ModMaterials.ALL) {
			generators.generateFlatItem(set.rawItem.get(), ModelTemplates.FLAT_ITEM);
			generators.generateFlatItem(set.ingot.get(), ModelTemplates.FLAT_ITEM);
			generators.generateFlatItem(set.nugget.get(), ModelTemplates.FLAT_ITEM);

			generators.generateFlatItem(set.sword.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			generators.generateFlatItem(set.pickaxe.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			generators.generateFlatItem(set.axe.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			generators.generateFlatItem(set.shovel.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			generators.generateFlatItem(set.hoe.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			// Uses item/<name>_spear in the inventory and the larger item/<name>_spear_in_hand when held.
			generators.generateSpear(set.spear.get());

			// Armor icons are flat sprites; the 3D model is only used when worn.
			set.armorPieces().forEach(piece -> generators.generateFlatItem(piece.get(), ModelTemplates.FLAT_ITEM));
		}
	}
}
