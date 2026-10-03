package com.gearexpansion.fabric.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.recipe.AlloyingRecipe;
import com.gearexpansion.recipe.AlloyingRecipe.CountedIngredient;
import com.gearexpansion.registry.ModBlocks;

final class RecipeGenerator extends FabricRecipeProvider {
	RecipeGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
		return new RecipeProvider(recipes, advancements) {
			@Override
			public void buildRecipes() {
				for (MaterialSet set : ModMaterials.ALL) {
					materials(set);
					if (set.upgradedFrom != null) {
						upgrades(set);
					} else {
						tools(set);
						armor(set);
						shield(set);
					}
				}
				craftedMaterials();
				alloyForge();
				alloys();
			}

			private void alloyForge() {
				shaped(RecipeCategory.DECORATIONS, ModBlocks.ALLOY_FORGE.get())
					.define('B', Items.BRICKS)
					.define('C', TagGenerator.commonItem("storage_blocks/copper"))
					.define('F', Items.BLAST_FURNACE)
					.pattern("BCB")
					.pattern("BFB")
					.pattern("BBB")
					.unlockedBy(getHasName(Items.BLAST_FURNACE), has(Items.BLAST_FURNACE))
					.save(output);
			}

			// Ingredients use the common tags, so other mods' copper, zinc, and gold work too.
			private void alloys() {
				alloy(ModMaterials.BRASS.ingot.get(), 4, 200, 0.7F, metal("copper", 3), metal("zinc", 1));
				alloy(ModMaterials.ROSE_GOLD.ingot.get(), 2, 200, 0.7F, metal("gold", 3), metal("copper", 1));
				alloy(ModMaterials.SAKURA.ingot.get(), 1, 200, 0.7F, metal("iron", 1), new CountedIngredient(Ingredient.of(Items.PINK_PETALS), 4));
				// Steel takes longer: iron tempered with coal or charcoal.
				alloy(ModMaterials.STEEL.ingot.get(), 1, 300, 1.0F, metal("iron", 1), new CountedIngredient(tag(ItemTags.COALS), 2));
			}

			private CountedIngredient metal(String name, int count) {
				return new CountedIngredient(tag(TagGenerator.commonItem("ingots/" + name)), count);
			}

			/** Saves {@code <result>_from_alloying}. Alloy recipes aren't in the recipe book, so they have no unlock advancement. */
			private void alloy(ItemLike result, int count, int cookingTime, float experience, CountedIngredient... ingredients) {
				AlloyingRecipe recipe = new AlloyingRecipe(List.of(ingredients), new ItemStackTemplate(result.asItem(), count), cookingTime, experience);
				output.accept(ResourceKey.create(Registries.RECIPE, GearExpansion.id(getItemName(result) + "_from_alloying")), recipe, null);
			}

			/** Recipes for materials that are crafted from other items rather than smelted. */
			private void craftedMaterials() {
				MaterialSet amethyst = ModMaterials.AMETHYST;
				shapeless(RecipeCategory.MISC, amethyst.ingot.get())
					.requires(Items.AMETHYST_SHARD, 4)
					.requires(tag(ConventionalItemTags.COPPER_INGOTS))
					.unlockedBy(getHasName(Items.AMETHYST_SHARD), has(Items.AMETHYST_SHARD))
					.save(output);
				MaterialSet verdigris = ModMaterials.VERDIGRIS;
				shapeless(RecipeCategory.MISC, verdigris.ingot.get(), 2)
					.requires(tag(ConventionalItemTags.COPPER_INGOTS), 2)
					.requires(Items.HONEYCOMB)
					.requires(Items.DYE.green())
					.unlockedBy(getHasName(Items.COPPER_INGOT), has(Items.COPPER_INGOT))
					.save(output);
			}

			/** Upgrade materials (like netherite): base gear + template + ingot at a smithing table, and a way to copy the template. */
			private void upgrades(MaterialSet set) {
				MaterialSet base = set.upgradedFrom.get();
				Item template = set.upgradeTemplate.get();
				List<Item> from = base.gear().stream().map(item -> item.get()).toList();
				List<Item> to = set.gear().stream().map(item -> item.get()).toList();
				for (int i = 0; i < from.size(); i++) {
					RecipeCategory category = i < 6 ? RecipeCategory.TOOLS : RecipeCategory.COMBAT;
					SmithingTransformRecipeBuilder.smithing(Ingredient.of(template), Ingredient.of(from.get(i)), Ingredient.of(set.ingot.get()), category, to.get(i))
						.unlocks(getHasName(set.ingot.get()), has(set.ingot.get()))
						.save(output, getItemName(to.get(i)) + "_smithing");
				}
				// Copy the template with base material ingots around it, like vanilla uses diamonds.
				shaped(RecipeCategory.MISC, template, 2)
					.define('#', base.ingot.get())
					.define('C', Items.NETHERRACK)
					.define('S', template)
					.pattern("#S#")
					.pattern("#C#")
					.pattern("###")
					.unlockedBy(getHasName(template), has(template))
					.save(output);
			}

			private void materials(MaterialSet set) {
				Item ingot = set.ingot.get();
				String ingotName = set.ingotName;
				if (set.hasOre) {
					List<ItemLike> smeltables = new ArrayList<>(List.of(set.ore.get(), set.rawItem.get()));
					if (set.deepslateOre != null) {
						smeltables.add(set.deepslateOre.get());
					}
					// Some ores (Infernium) only melt down in a blast furnace.
					if (!set.blastFurnaceOnly) {
						oreSmelting(smeltables, RecipeCategory.MISC, CookingBookCategory.MISC, ingot, 1.0F, 200, ingotName);
					}
					oreBlasting(smeltables, RecipeCategory.MISC, CookingBookCategory.MISC, ingot, 1.0F, 100, ingotName);
					nineBlockStorageRecipes(RecipeCategory.MISC, set.rawItem.get(), RecipeCategory.BUILDING_BLOCKS, set.rawStorageBlock.get());
				}
				if (set.isMetal) {
					nineBlockStorageRecipesRecipesWithCustomUnpacking(RecipeCategory.MISC, ingot, RecipeCategory.BUILDING_BLOCKS, set.storageBlock.get(),
						ingotName + "_from_" + set.name + "_block", ingotName);
					nineBlockStorageRecipesWithCustomPacking(RecipeCategory.MISC, set.nugget.get(), RecipeCategory.MISC, ingot,
						ingotName + "_from_nuggets", ingotName);
				}
			}

			private void tools(MaterialSet set) {
				tool(set, RecipeCategory.COMBAT, set.sword.get(), "X", "X", "#");
				tool(set, RecipeCategory.TOOLS, set.pickaxe.get(), "XXX", " # ", " # ");
				tool(set, RecipeCategory.TOOLS, set.axe.get(), "XX", "X#", " #");
				tool(set, RecipeCategory.TOOLS, set.shovel.get(), "X", "#", "#");
				tool(set, RecipeCategory.TOOLS, set.hoe.get(), "XX", " #", " #");
				tool(set, RecipeCategory.COMBAT, set.spear.get(), "  X", " # ", "#  ");
			}

			private void tool(MaterialSet set, RecipeCategory category, Item result, String... pattern) {
				var recipe = shaped(category, result).define('#', Items.STICK).define('X', set.repairMaterials);
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(set.ingot.get()), has(set.ingot.get())).save(output);
			}

			private void armor(MaterialSet set) {
				armorPiece(set, set.helmet.get(), "XXX", "X X");
				armorPiece(set, set.chestplate.get(), "X X", "XXX", "XXX");
				armorPiece(set, set.leggings.get(), "XXX", "X X", "X X");
				armorPiece(set, set.boots.get(), "X X", "X X");
			}

			private void armorPiece(MaterialSet set, Item result, String... pattern) {
				var recipe = shaped(RecipeCategory.COMBAT, result).define('X', set.ingot.get());
				for (String row : pattern) {
					recipe.pattern(row);
				}
				recipe.unlockedBy(getHasName(set.ingot.get()), has(set.ingot.get())).save(output);
			}

			// A reinforced vanilla shield: four ingots around a regular shield.
			private void shield(MaterialSet set) {
				shaped(RecipeCategory.COMBAT, set.shield.get())
					.define('X', set.ingot.get())
					.define('S', Items.SHIELD)
					.pattern(" X ")
					.pattern("XSX")
					.pattern(" X ")
					.unlockedBy(getHasName(set.ingot.get()), has(set.ingot.get()))
					.save(output);
			}
		};
	}

	@Override
	public String getName() {
		return "Gear Expansion Recipes";
	}
}
