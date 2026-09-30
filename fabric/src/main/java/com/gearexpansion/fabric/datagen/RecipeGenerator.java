package com.gearexpansion.fabric.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

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
					tools(set);
					armor(set);
					shield(set);
				}
			}

			private void materials(MaterialSet set) {
				Item ingot = set.ingot.get();
				String ingotName = set.name + "_ingot";
				List<ItemLike> smeltables = List.of(set.ore.get(), set.deepslateOre.get(), set.rawItem.get());
				oreSmelting(smeltables, RecipeCategory.MISC, CookingBookCategory.MISC, ingot, 1.0F, 200, ingotName);
				oreBlasting(smeltables, RecipeCategory.MISC, CookingBookCategory.MISC, ingot, 1.0F, 100, ingotName);

				nineBlockStorageRecipes(RecipeCategory.MISC, set.rawItem.get(), RecipeCategory.BUILDING_BLOCKS, set.rawStorageBlock.get());
				nineBlockStorageRecipesRecipesWithCustomUnpacking(RecipeCategory.MISC, ingot, RecipeCategory.BUILDING_BLOCKS, set.storageBlock.get(),
					ingotName + "_from_" + set.name + "_block", ingotName);
				nineBlockStorageRecipesWithCustomPacking(RecipeCategory.MISC, set.nugget.get(), RecipeCategory.MISC, ingot,
					ingotName + "_from_nuggets", ingotName);
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
