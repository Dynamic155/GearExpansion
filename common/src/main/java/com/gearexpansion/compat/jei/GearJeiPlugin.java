package com.gearexpansion.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.client.screen.AlloyForgeScreen;
import com.gearexpansion.compat.AlloyingRecipeLayout;
import com.gearexpansion.compat.ClientAlloyingRecipes;
import com.gearexpansion.recipe.AlloyingRecipe;
import com.gearexpansion.registry.ModItems;

/**
 * Just Enough Items support: an Alloy Forge category with every alloying recipe.
 *
 * <p>JEI only loads this when it is installed. Fabric finds it through the {@code jei_mod_plugin}
 * entrypoint, NeoForge through an annotated subclass in the NeoForge module.
 *
 * <p>JEI reads recipes on the client, so they come from {@link ClientAlloyingRecipes}.
 */
public class GearJeiPlugin implements IModPlugin {
	public static final IRecipeHolderType<AlloyingRecipe> ALLOYING = IRecipeHolderType.create(GearExpansion.id("alloying"));

	private static @Nullable IJeiRuntime runtime;

	/** JEI's runtime while a world is open, otherwise null. Used by the game test. */
	public static @Nullable IJeiRuntime runtime() {
		return runtime;
	}

	@Override
	public Identifier getPluginUid() {
		return GearExpansion.id("jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new AlloyingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(ALLOYING, ClientAlloyingRecipes.get());
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addCraftingStation(ALLOYING, ModItems.ALLOY_FORGE.get());
	}

	/** Clicking the arrow on the Alloy Forge screen shows the alloying recipes. */
	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addRecipeClickArea(AlloyForgeScreen.class,
			AlloyingRecipeLayout.SCREEN_ARROW_X, AlloyingRecipeLayout.SCREEN_ARROW_Y,
			AlloyingRecipeLayout.SCREEN_ARROW_WIDTH, AlloyingRecipeLayout.SCREEN_ARROW_HEIGHT,
			ALLOYING);
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
	}

	@Override
	public void onRuntimeUnavailable() {
		runtime = null;
	}
}
