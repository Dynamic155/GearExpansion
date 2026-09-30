package com.gearexpansion.fabric.datagen;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockItemTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/**
 * Block and item tags. Our items join vanilla's tool tags and the common {@code c:} tags so
 * enchanting, repairing, and other mods' recipes work with them.
 *
 * <p>Armor is added to the enchantable tags directly instead of vanilla's armor-type tags,
 * because those would also make it trimmable, and trims don't show on the 3D models yet.
 */
final class TagGenerator {
	private TagGenerator() {
	}

	static ResourceKey<Block> key(Supplier<? extends Block> block) {
		return block.get().builtInRegistryHolder().key();
	}

	static ResourceKey<Item> itemKey(Supplier<? extends Item> item) {
		return item.get().builtInRegistryHolder().key();
	}

	static BlockItemTagId common(String path) {
		Identifier id = Identifier.fromNamespaceAndPath("c", path);
		return BlockItemTagId.create(id, id);
	}

	static TagKey<Item> commonItem(String path) {
		return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
	}

	static final class Blocks extends FabricTagsProvider.BlockTagsProvider {
		Blocks(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			for (MaterialSet set : ModMaterials.ALL) {
				var ore = key(set.ore);
				var deepslateOre = key(set.deepslateOre);
				var block = key(set.storageBlock);
				var rawBlock = key(set.rawStorageBlock);

				builder(BlockTags.MINEABLE_WITH_PICKAXE).add(ore, deepslateOre, block, rawBlock);
				builder(set.requiredToolTag).add(ore, deepslateOre, block, rawBlock);
				builder(BlockTags.BEACON_BASE_BLOCKS).add(block);

				BlockItemTagId ores = common("ores/" + set.name);
				builder(ores.block()).add(ore, deepslateOre);
				builder(ConventionalBlockItemTags.ORES.block()).addTag(ores.block());
				builder(ConventionalBlockItemTags.ORES_IN_GROUND_STONE.block()).add(ore);
				builder(ConventionalBlockItemTags.ORES_IN_GROUND_DEEPSLATE.block()).add(deepslateOre);
				builder(ConventionalBlockItemTags.ORE_RATES_SINGULAR.block()).add(ore, deepslateOre);

				BlockItemTagId storage = common("storage_blocks/" + set.name);
				BlockItemTagId rawStorage = common("storage_blocks/raw_" + set.name);
				builder(storage.block()).add(block);
				builder(rawStorage.block()).add(rawBlock);
				builder(ConventionalBlockItemTags.STORAGE_BLOCKS.block()).addTag(storage.block()).addTag(rawStorage.block());
			}
		}
	}

	static final class Items extends FabricTagsProvider.ItemTagsProvider {
		Items(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries, BlockTagsProvider blockTags) {
			super(output, registries, blockTags);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			copy(ConventionalBlockItemTags.ORES);
			copy(ConventionalBlockItemTags.ORES_IN_GROUND_STONE);
			copy(ConventionalBlockItemTags.ORES_IN_GROUND_DEEPSLATE);
			copy(ConventionalBlockItemTags.ORE_RATES_SINGULAR);
			copy(ConventionalBlockItemTags.STORAGE_BLOCKS);

			for (MaterialSet set : ModMaterials.ALL) {
				copy(common("ores/" + set.name));
				copy(common("storage_blocks/" + set.name));
				copy(common("storage_blocks/raw_" + set.name));

				var ingot = itemKey(set.ingot);
				builder(set.repairMaterials).add(ingot);
				builder(ItemTags.BEACON_PAYMENT_ITEMS).add(ingot);

				TagKey<Item> ingots = commonItem("ingots/" + set.name);
				TagKey<Item> nuggets = commonItem("nuggets/" + set.name);
				TagKey<Item> raw = commonItem("raw_materials/" + set.name);
				builder(ingots).add(ingot);
				builder(nuggets).add(itemKey(set.nugget));
				builder(raw).add(itemKey(set.rawItem));
				builder(ConventionalItemTags.INGOTS).addTag(ingots);
				builder(ConventionalItemTags.NUGGETS).addTag(nuggets);
				builder(ConventionalItemTags.RAW_MATERIALS).addTag(raw);

				builder(ItemTags.SWORDS).add(itemKey(set.sword));
				builder(ItemTags.PICKAXES).add(itemKey(set.pickaxe));
				builder(ItemTags.AXES).add(itemKey(set.axe));
				builder(ItemTags.SHOVELS).add(itemKey(set.shovel));
				builder(ItemTags.HOES).add(itemKey(set.hoe));
				builder(ItemTags.SPEARS).add(itemKey(set.spear));

				var helmet = itemKey(set.helmet);
				var chestplate = itemKey(set.chestplate);
				var leggings = itemKey(set.leggings);
				var boots = itemKey(set.boots);
				builder(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(helmet);
				builder(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(chestplate);
				builder(ItemTags.LEG_ARMOR_ENCHANTABLE).add(leggings);
				builder(ItemTags.FOOT_ARMOR_ENCHANTABLE).add(boots);
				builder(ItemTags.EQUIPPABLE_ENCHANTABLE).add(helmet, chestplate, leggings, boots);
				builder(ItemTags.DURABILITY_ENCHANTABLE).add(helmet, chestplate, leggings, boots, itemKey(set.shield));
				builder(ConventionalItemTags.ARMORS).add(helmet, chestplate, leggings, boots);

				builder(ConventionalItemTags.SHIELD_TOOLS).add(itemKey(set.shield));
			}
		}
	}
}
