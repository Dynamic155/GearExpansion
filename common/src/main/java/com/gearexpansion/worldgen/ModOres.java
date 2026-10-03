package com.gearexpansion.worldgen;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.HeightMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.MaterialSet.OreGeneration;
import com.gearexpansion.material.ModMaterials;

/**
 * Ore veins for every material, built the same way as vanilla's iron and diamond ores.
 * Data generation turns these into JSON; each loader then adds each placed feature to its biomes.
 */
public final class ModOres {
	private ModOres() {
	}

	/** e.g. {@code ore_aluminum} for the main vein, {@code ore_aluminum_badlands} for an extra one. */
	public static String name(MaterialSet set, OreGeneration ore) {
		return "ore_" + set.name + (ore.id().isEmpty() ? "" : "_" + ore.id());
	}

	public static ResourceKey<Feature> featureKey(MaterialSet set, OreGeneration ore) {
		return ResourceKey.create(Registries.FEATURE, GearExpansion.id(name(set, ore)));
	}

	public static ResourceKey<PlacedFeature> placedKey(MaterialSet set, OreGeneration ore) {
		return ResourceKey.create(Registries.PLACED_FEATURE, GearExpansion.id(name(set, ore)));
	}

	/** The material's main, Overworld-wide ore vein. */
	public static ResourceKey<PlacedFeature> placedKey(MaterialSet set) {
		return placedKey(set, set.oreGeneration.getFirst());
	}

	public static void bootstrapFeatures(BootstrapContext<Feature> context) {
		// Same replacement rules vanilla uses: stone ore above Y 0, deepslate ore below Y 8, blended in between.
		RuleTest stone = RuleTest.either(new TagMatchTest(BlockTags.HEIGHT_SPECIFIC_ORE_REPLACEABLES), HeightMatchTest.min(0), new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES));
		RuleTest deepslate = RuleTest.either(new TagMatchTest(BlockTags.HEIGHT_SPECIFIC_ORE_REPLACEABLES), HeightMatchTest.max(8), new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES));

		RuleTest netherrack = new TagMatchTest(BlockTags.BASE_STONE_NETHER);
		RuleTest packedIce = new BlockMatchTest(Blocks.PACKED_ICE);
		RuleTest blueIce = new BlockMatchTest(Blocks.BLUE_ICE);

		for (MaterialSet set : ModMaterials.ALL) {
			List<BlockReplacement> replacements = switch (set.oreKind) {
				case OVERWORLD -> List.of(
					BlockReplacement.replace(stone, set.ore.get().defaultBlockState()),
					BlockReplacement.replace(deepslate, set.deepslateOre.get().defaultBlockState()));
				case NETHER -> List.of(BlockReplacement.replace(netherrack, set.ore.get().defaultBlockState()));
				case ICE -> List.of(
					BlockReplacement.replace(packedIce, set.ore.get().defaultBlockState()),
					BlockReplacement.replace(blueIce, set.ore.get().defaultBlockState()));
				case NONE -> List.of();
			};
			for (OreGeneration ore : set.oreGeneration) {
				context.register(featureKey(set, ore), new OreFeature(replacements, ore.veinSize(), ore.airExposureDiscard()));
			}
		}
	}

	public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
		var features = context.lookup(Registries.FEATURE);
		for (MaterialSet set : ModMaterials.ALL) {
			for (OreGeneration ore : set.oreGeneration) {
				Holder<Feature> feature = features.getOrThrow(featureKey(set, ore));
				context.register(placedKey(set, ore), new PlacedFeature(feature, List.of(
					CountPlacement.of(ore.veinsPerChunk()),
					InSquarePlacement.spread(),
					HeightRangePlacement.uniform(VerticalAnchor.absolute(ore.minY()), VerticalAnchor.absolute(ore.maxY())),
					BiomeFilter.biome()
				)));
			}
		}
	}
}
