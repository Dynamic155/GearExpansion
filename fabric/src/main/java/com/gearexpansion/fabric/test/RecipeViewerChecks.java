package com.gearexpansion.fabric.test;

import java.util.List;
import java.util.function.BooleanSupplier;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.compat.jei.GearJeiPlugin;
import com.gearexpansion.compat.rei.AlloyingDisplay;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.registry.ModItems;

/**
 * Checks the Alloy Forge category in whichever recipe viewers are loaded, and takes screenshots of it.
 * The JEI and REI code is in nested classes, which only load when that viewer is installed.
 *
 * <p>Choose the viewers with {@code ./gradlew :fabric:runGameTest -Precipe_viewer=jei} (or {@code rei}).
 */
final class RecipeViewerChecks {
	private static final List<Identifier> ALLOYING_RECIPES = List.of(
		GearExpansion.id("brass_ingot_from_alloying"),
		GearExpansion.id("rose_gold_ingot_from_alloying"));
	// Recipe viewers load in the background after joining a world.
	private static final int LOAD_TIMEOUT_TICKS = 400;

	interface Checker {
		void check(boolean condition, String description);
	}

	private RecipeViewerChecks() {
	}

	static void run(ClientGameTestContext ctx, Checker checker) {
		if (FabricLoader.getInstance().isModLoaded("jei")) {
			Jei.run(ctx, checker);
		}
		if (FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
			Rei.run(ctx, checker);
		}
	}

	/** JEI reads recipes on the client, so they are there as soon as it starts. */
	private static final class Jei {
		static void run(ClientGameTestContext ctx, Checker checker) {
			boolean loaded = waitUntil(ctx, () -> ctx.computeOnClient(mc -> GearJeiPlugin.runtime() != null));
			checker.check(loaded, "JEI starts with Gear Expansion's plugin");
			if (!loaded) {
				return;
			}

			List<Identifier> recipes = ctx.computeOnClient(mc -> GearJeiPlugin.runtime().getRecipeManager()
				.createRecipeLookup(GearJeiPlugin.ALLOYING)
				.get()
				.map(holder -> holder.id().identifier())
				.sorted()
				.toList());
			GearExpansion.LOGGER.info("[GameTest] JEI alloying recipes: {}", recipes);
			checker.check(recipes.equals(ALLOYING_RECIPES), "JEI's Alloy Forge category lists the brass and rose gold recipes");
			boolean catalyst = ctx.computeOnClient(mc -> GearJeiPlugin.runtime().getRecipeManager()
				.createCraftingStationLookup(GearJeiPlugin.ALLOYING)
				.getItemStack()
				.anyMatch(stack -> stack.is(ModItems.ALLOY_FORGE.get())));
			checker.check(catalyst, "the alloy forge is JEI's catalyst for alloying");
			boolean brassUsesForge = ctx.computeOnClient(mc -> {
				IJeiRuntime runtime = GearJeiPlugin.runtime();
				IFocus<ItemStack> focus = runtime.getJeiHelpers().getFocusFactory()
					.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, new ItemStack(ModMaterials.BRASS.ingot.get()));
				return runtime.getRecipeManager().createRecipeCategoryLookup()
					.limitFocus(List.of(focus))
					.get()
					.anyMatch(category -> category.getRecipeType() == GearJeiPlugin.ALLOYING);
			});
			checker.check(brassUsesForge, "JEI shows the Alloy Forge among the recipes for brass ingots");

			ctx.runOnClient(mc -> GearJeiPlugin.runtime().getRecipesGui().showTypes(List.of(GearJeiPlugin.ALLOYING)));
			ctx.waitTicks(20);
			ctx.takeScreenshot("jei_alloy_forge_category");
			// Every recipe that makes brass ingots, as when pressing R on one.
			ctx.runOnClient(mc -> {
				IJeiRuntime runtime = GearJeiPlugin.runtime();
				runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory()
					.createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, new ItemStack(ModMaterials.BRASS.ingot.get())));
			});
			ctx.waitTicks(20);
			ctx.takeScreenshot("jei_brass_ingot_recipes");
			ctx.setScreen(() -> null);
			ctx.waitTicks(5);
		}
	}

	/** REI makes its displays on the integrated server and syncs them to the client. */
	private static final class Rei {
		static void run(ClientGameTestContext ctx, Checker checker) {
			boolean synced = waitUntil(ctx, () -> ctx.computeOnClient(mc -> DisplayRegistry.getInstance().get(AlloyingDisplay.CATEGORY).size() >= ALLOYING_RECIPES.size()));
			List<Identifier> recipes = ctx.computeOnClient(mc -> DisplayRegistry.getInstance().get(AlloyingDisplay.CATEGORY).stream()
				.flatMap(display -> display.getDisplayLocation().stream())
				.sorted()
				.toList());
			GearExpansion.LOGGER.info("[GameTest] REI alloying displays: {}", recipes);
			checker.check(synced && recipes.equals(ALLOYING_RECIPES), "REI's Alloy Forge category lists the brass and rose gold recipes");
			boolean workstation = ctx.computeOnClient(mc -> CategoryRegistry.getInstance().get(AlloyingDisplay.CATEGORY).getWorkstations().stream()
				.flatMap(EntryIngredient::stream)
				.anyMatch(stack -> stack.getValue() instanceof ItemStack item && item.is(ModItems.ALLOY_FORGE.get())));
			checker.check(workstation, "the alloy forge is REI's workstation for alloying");

			boolean openedCategory = ctx.computeOnClient(mc -> ViewSearchBuilder.builder()
				.addCategory(AlloyingDisplay.CATEGORY)
				.open());
			checker.check(openedCategory, "REI opens the Alloy Forge category");
			ctx.waitTicks(20);
			ctx.takeScreenshot("rei_alloy_forge_category");
			// The recipes that make brass ingots, as when pressing R on one, on the Alloy Forge tab.
			boolean openedBrass = ctx.computeOnClient(mc -> ViewSearchBuilder.builder()
				.addRecipesFor(EntryStacks.of(ModMaterials.BRASS.ingot.get()))
				.setPreferredOpenedCategory(AlloyingDisplay.CATEGORY)
				.open());
			checker.check(openedBrass, "REI opens the recipes for brass ingots");
			ctx.waitTicks(20);
			ctx.takeScreenshot("rei_brass_ingot_recipes");
			ctx.setScreen(() -> null);
			ctx.waitTicks(5);
		}
	}

	/** Waits up to {@link #LOAD_TIMEOUT_TICKS} for {@code condition}, without failing the whole test. */
	static boolean waitUntil(ClientGameTestContext ctx, BooleanSupplier condition) {
		for (int tick = 0; tick < LOAD_TIMEOUT_TICKS; tick += 5) {
			if (condition.getAsBoolean()) {
				return true;
			}
			ctx.waitTicks(5);
		}
		return condition.getAsBoolean();
	}
}
