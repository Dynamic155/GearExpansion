package com.gearexpansion.compat.rei;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;

import com.gearexpansion.client.screen.AlloyForgeScreen;
import com.gearexpansion.compat.AlloyingRecipeLayout;
import com.gearexpansion.registry.ModItems;

/**
 * Roughly Enough Items support, the client part: the Alloy Forge category, with the forge as its
 * workstation. The displays come from the server, see {@link GearReiCommonPlugin}.
 *
 * <p>Fabric finds this through the {@code rei_client} entrypoint, NeoForge through an annotated
 * subclass in the NeoForge module.
 */
public class GearReiClientPlugin implements REIClientPlugin {
	@Override
	public void registerCategories(CategoryRegistry registry) {
		registry.add(new AlloyingReiCategory());
		registry.addWorkstations(AlloyingDisplay.CATEGORY, EntryStacks.of(ModItems.ALLOY_FORGE.get()));
	}

	/** Clicking the arrow on the Alloy Forge screen shows the alloying recipes. */
	@Override
	public void registerScreens(ScreenRegistry registry) {
		registry.registerContainerClickArea(
			new Rectangle(AlloyingRecipeLayout.SCREEN_ARROW_X, AlloyingRecipeLayout.SCREEN_ARROW_Y,
				AlloyingRecipeLayout.SCREEN_ARROW_WIDTH, AlloyingRecipeLayout.SCREEN_ARROW_HEIGHT),
			AlloyForgeScreen.class,
			AlloyingDisplay.CATEGORY);
	}
}
