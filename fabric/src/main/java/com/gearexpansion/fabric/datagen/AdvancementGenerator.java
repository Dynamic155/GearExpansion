package com.gearexpansion.fabric.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.registry.ModBlocks;

/**
 * A "Gear Expansion" advancement tab: one advancement per material, plus the Alloy Forge.
 * Titles and descriptions are in {@link LanguageGenerator}.
 */
final class AdvancementGenerator extends FabricAdvancementProvider {
	AdvancementGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
		AdvancementHolder root = Advancement.Builder.advancement()
			.rootDisplay(
				ModMaterials.TITANIUM.chestplate.get(),
				Component.translatable("advancements.gearexpansion.root.title"),
				Component.translatable("advancements.gearexpansion.root.description"),
				Identifier.withDefaultNamespace("gui/advancements/backgrounds/stone"),
				AdvancementType.TASK, false, false, false)
			.addCriterion("raw_or_crafted_material", anyOf(ModMaterials.ALL.stream()
				.flatMap(set -> set.ingredients().stream().map(item -> (ItemLike) item.get())).toList()))
			.build(GearExpansion.id("root"));
		output.accept(root);

		AdvancementHolder forge = task(output, root, "alloy_forge", ModBlocks.ALLOY_FORGE.get().asItem(), AdvancementType.TASK,
			List.of(ModBlocks.ALLOY_FORGE.get()));

		task(output, root, "zinc", ModMaterials.ZINC.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.ZINC.ingot.get()));
		task(output, root, "verdigris", ModMaterials.VERDIGRIS.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.VERDIGRIS.ingot.get()));
		task(output, forge, "rose_gold", ModMaterials.ROSE_GOLD.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.ROSE_GOLD.ingot.get()));
		task(output, root, "aluminum", ModMaterials.ALUMINUM.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.ALUMINUM.ingot.get()));
		task(output, forge, "brass", ModMaterials.BRASS.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.BRASS.ingot.get()));
		task(output, root, "silver", ModMaterials.SILVER.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.SILVER.ingot.get()));
		task(output, root, "emerald", ModMaterials.EMERALD.chestplate.get(), AdvancementType.TASK, gear(ModMaterials.EMERALD));
		task(output, root, "amethyst", ModMaterials.AMETHYST.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.AMETHYST.ingot.get()));
		task(output, forge, "steel", ModMaterials.STEEL.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.STEEL.ingot.get()));
		AdvancementHolder titanium = task(output, root, "titanium", ModMaterials.TITANIUM.ingot.get(), AdvancementType.TASK,
			List.of(ModMaterials.TITANIUM.ingot.get()));
		task(output, root, "cobalt", ModMaterials.COBALT.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.COBALT.ingot.get()));
		task(output, root, "tungsten", ModMaterials.TUNGSTEN.ingot.get(), AdvancementType.TASK, List.of(ModMaterials.TUNGSTEN.ingot.get()));
		AdvancementHolder template = task(output, titanium, "infernium_template", ModMaterials.INFERNIUM.upgradeTemplate.get(), AdvancementType.TASK,
			List.of(ModMaterials.INFERNIUM.upgradeTemplate.get()));
		task(output, template, "infernium", ModMaterials.INFERNIUM.chestplate.get(), AdvancementType.GOAL, gear(ModMaterials.INFERNIUM));
	}

	/** An advancement for getting any one of {@code items}. */
	private static AdvancementHolder task(Consumer<AdvancementHolder> output, AdvancementHolder parent, String name, Item icon,
			AdvancementType type, List<? extends ItemLike> items) {
		Advancement.Builder builder = Advancement.Builder.advancement()
			.parent(parent)
			.display(icon,
				Component.translatable("advancements.gearexpansion." + name + ".title"),
				Component.translatable("advancements.gearexpansion." + name + ".description"),
				type, true, true, false)
			.requirements(AdvancementRequirements.Strategy.OR);
		for (ItemLike item : items) {
			builder.addCriterion("has_" + item.asItem().builtInRegistryHolder().key().identifier().getPath(), InventoryChangeTrigger.TriggerInstance.hasItems(item));
		}
		AdvancementHolder advancement = builder.build(GearExpansion.id(name));
		output.accept(advancement);
		return advancement;
	}

	/** One criterion that's met by any of {@code items}. */
	private static Criterion<InventoryChangeTrigger.TriggerInstance> anyOf(List<ItemLike> items) {
		return InventoryChangeTrigger.TriggerInstance.hasItems(items.toArray(ItemLike[]::new));
	}

	private static List<Item> gear(MaterialSet set) {
		return set.gear().stream().map(item -> item.get()).toList();
	}
}
