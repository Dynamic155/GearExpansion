package com.gearexpansion.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

import com.gearexpansion.worldgen.ModOres;

/**
 * Generates the mod's JSON data (models, recipes, loot, tags, translations, and world generation)
 * into {@code common/src/main/generated}, which both loaders ship. Run with {@code ./gradlew :fabric:runDatagen}.
 */
public final class GearExpansionDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(ModelGenerator::new);
		pack.addProvider(GeckoLibItemModelGenerator::new);
		pack.addProvider(VerdigrisModelGenerator::new);
		pack.addProvider(RecipeGenerator::new);
		pack.addProvider(BlockLootGenerator::new);
		pack.addProvider(LanguageGenerator::new);
		pack.addProvider(AdvancementGenerator::new);
		pack.addProvider(WorldgenGenerator::new);
		pack.addProvider(NeoForgeBiomeModifierGenerator::new);
		pack.addProvider(TagGenerator.Biomes::new);
		TagGenerator.Blocks blockTags = pack.addProvider(TagGenerator.Blocks::new);
		pack.addProvider((output, registries) -> new TagGenerator.Items(output, registries, blockTags));
	}

	@Override
	public void buildRegistry(RegistrySetBuilder builder) {
		builder.add(Registries.FEATURE, ModOres::bootstrapFeatures);
		builder.add(Registries.PLACED_FEATURE, ModOres::bootstrapPlacedFeatures);
	}
}
