package com.gearexpansion.fabric.datagen;

import java.util.Optional;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.resources.Identifier;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.registry.ModBlocks;

/**
 * Block and item models. Shields use GeckoLib models instead, written by {@link GeckoLibItemModelGenerator}.
 */
final class ModelGenerator extends FabricModelProvider {
	static final String SCYTHE_PARENT = "gearexpansion:item/scythe_handheld";
	private static final ModelTemplate SCYTHE = new ModelTemplate(Optional.of(Identifier.parse(SCYTHE_PARENT)), Optional.empty(), TextureSlot.LAYER0);

	ModelGenerator(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators generators) {
		// Same models as the blast furnace: front, side, and top textures, with a lit front.
		generators.createFurnace(ModBlocks.ALLOY_FORGE.get(), TexturedModel.ORIENTABLE_ONLY_TOP);
		generators.createTrivialCube(ModBlocks.FULGURITE.get());

		for (MaterialSet set : ModMaterials.ALL) {
			set.blocks().forEach(block -> generators.createTrivialCube(block.get()));
		}
	}

	@Override
	public void generateItemModels(ItemModelGenerators generators) {
		for (MaterialSet set : ModMaterials.ALL) {
			set.ingredients().forEach(item -> generators.generateFlatItem(item.get(), ModelTemplates.FLAT_ITEM));
			if (set == ModMaterials.VERDIGRIS) {
				// Verdigris gear has a model per oxidation stage; see VerdigrisModelGenerator.
				continue;
			}

			generators.generateFlatItem(set.sword.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			generators.generateFlatItem(set.pickaxe.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			generators.generateFlatItem(set.axe.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			generators.generateFlatItem(set.shovel.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			generators.generateFlatItem(set.hoe.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			// Uses item/<name>_spear in the inventory and the larger item/<name>_spear_in_hand when held.
			generators.generateSpear(set.spear.get());
			generators.generateFlatItem(set.dagger.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
			// Held a little larger than other tools (see assets/gearexpansion/models/item/scythe_handheld.json).
			generators.generateFlatItem(set.scythe.get(), SCYTHE);

			// Armor icons are flat sprites; the 3D model is only used when worn.
			set.armorPieces().forEach(piece -> generators.generateFlatItem(piece.get(), ModelTemplates.FLAT_ITEM));
		}
	}
}
