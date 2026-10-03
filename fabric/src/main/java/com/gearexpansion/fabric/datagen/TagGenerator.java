package com.gearexpansion.fabric.datagen;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.block.entity.AlloyForgeBlockEntity;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.registry.ModBlocks;
import com.gearexpansion.worldgen.ModBiomeTags;

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
			builder(BlockTags.MINEABLE_WITH_PICKAXE).add(key(ModBlocks.ALLOY_FORGE), key(ModBlocks.FULGURITE));

			for (MaterialSet set : ModMaterials.ALL) {
				if (set.storageBlock != null) {
					var block = key(set.storageBlock);
					builder(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
					builder(set.requiredToolTag).add(block);
					builder(BlockTags.BEACON_BASE_BLOCKS).add(block);

					BlockItemTagId storage = common("storage_blocks/" + set.name);
					builder(storage.block()).add(block);
					builder(ConventionalBlockItemTags.STORAGE_BLOCKS.block()).addTag(storage.block());
				}

				if (!set.hasOre) {
					continue;
				}
				List<ResourceKey<Block>> ores = new ArrayList<>(List.of(key(set.ore)));
				if (set.deepslateOre != null) {
					ores.add(key(set.deepslateOre));
				}
				var rawBlock = key(set.rawStorageBlock);
				ores.forEach(ore -> builder(BlockTags.MINEABLE_WITH_PICKAXE).add(ore));
				ores.forEach(ore -> builder(set.requiredToolTag).add(ore));
				builder(BlockTags.MINEABLE_WITH_PICKAXE).add(rawBlock);
				builder(set.requiredToolTag).add(rawBlock);

				for (String oreTag : set.oreTagNames()) {
					BlockItemTagId oreTags = common("ores/" + oreTag);
					ores.forEach(ore -> builder(oreTags.block()).add(ore));
					builder(ConventionalBlockItemTags.ORES.block()).addTag(oreTags.block());
				}
				if (set.oreKind == MaterialSet.OreKind.NETHER) {
					builder(ConventionalBlockItemTags.ORES_IN_GROUND_NETHERRACK.block()).add(key(set.ore));
				} else if (set.oreKind == MaterialSet.OreKind.OVERWORLD) {
					builder(ConventionalBlockItemTags.ORES_IN_GROUND_STONE.block()).add(key(set.ore));
					builder(ConventionalBlockItemTags.ORES_IN_GROUND_DEEPSLATE.block()).add(key(set.deepslateOre));
				}
				ores.forEach(ore -> builder(ConventionalBlockItemTags.ORE_RATES_SINGULAR.block()).add(ore));

				BlockItemTagId rawStorage = common("storage_blocks/raw_" + set.oreName);
				builder(rawStorage.block()).add(rawBlock);
				builder(ConventionalBlockItemTags.STORAGE_BLOCKS.block()).addTag(rawStorage.block());
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
			copy(ConventionalBlockItemTags.ORES_IN_GROUND_NETHERRACK);
			copy(ConventionalBlockItemTags.ORE_RATES_SINGULAR);
			copy(ConventionalBlockItemTags.STORAGE_BLOCKS);

			// Written out in full: this class's own Items would hide the vanilla one.
			builder(AlloyForgeBlockEntity.BOOST_FUELS).add(
				net.minecraft.world.item.Items.BLAZE_POWDER.builtInRegistryHolder().key(),
				net.minecraft.world.item.Items.LAVA_BUCKET.builtInRegistryHolder().key());

			for (MaterialSet set : ModMaterials.ALL) {
				if (set.storageBlock != null) {
					copy(common("storage_blocks/" + set.name));
				}
				if (set.hasOre) {
					set.oreTagNames().forEach(oreTag -> copy(common("ores/" + oreTag)));
					copy(common("storage_blocks/raw_" + set.oreName));
				}

				var ingot = itemKey(set.ingot);
				builder(set.repairMaterials).add(ingot);
				if (set.isMetal) {
					builder(ItemTags.BEACON_PAYMENT_ITEMS).add(ingot);
					TagKey<Item> ingots = commonItem("ingots/" + set.name);
					TagKey<Item> nuggets = commonItem("nuggets/" + set.name);
					builder(ingots).add(ingot);
					builder(nuggets).add(itemKey(set.nugget));
					builder(ConventionalItemTags.INGOTS).addTag(ingots);
					builder(ConventionalItemTags.NUGGETS).addTag(nuggets);
				}
				for (String oreTag : set.hasOre ? set.oreTagNames() : List.<String>of()) {
					TagKey<Item> raw = commonItem("raw_materials/" + oreTag);
					builder(raw).add(itemKey(set.rawItem));
					builder(ConventionalItemTags.RAW_MATERIALS).addTag(raw);
				}

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
				if (set.piglinSafe) {
					builder(ItemTags.PIGLIN_SAFE_ARMOR).add(helmet, chestplate, leggings, boots);
				}
				// Frostite keeps out the cold like leather: wearing any piece stops freezing.
				if (set == ModMaterials.FROSTITE) {
					builder(ItemTags.FREEZE_IMMUNE_WEARABLES).add(helmet, chestplate, leggings, boots);
				}

				builder(ConventionalItemTags.SHIELD_TOOLS).add(itemKey(set.shield));
			}
		}
	}

	/** Biomes our ores generate in, beyond vanilla's broad tags. */
	static final class Biomes extends FabricTagsProvider<Biome> {
		Biomes(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
			super(output, Registries.BIOME, registries);
		}

		@Override
		protected void addTags(HolderLookup.Provider registries) {
			builder(ModBiomeTags.HAS_FROSTITE_ORE).add(net.minecraft.world.level.biome.Biomes.FROZEN_PEAKS, net.minecraft.world.level.biome.Biomes.ICE_SPIKES);
			builder(ModBiomeTags.HAS_VERDANTITE_ORE).add(net.minecraft.world.level.biome.Biomes.LUSH_CAVES);
		}
	}
}
