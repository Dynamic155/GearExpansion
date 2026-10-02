package com.gearexpansion.compat.rei;

import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.recipe.AlloyingRecipe;
import com.gearexpansion.recipe.ModRecipes;

/**
 * Roughly Enough Items support, the part that runs on both sides: REI turns the server's alloying
 * recipes into {@link AlloyingDisplay}s and syncs them to clients that have REI. In multiplayer this
 * needs REI on the server as well, the same as for any other mod's recipes on 26.x.
 *
 * <p>REI only loads this when it is installed. Fabric finds it through the {@code rei_common}
 * entrypoint, NeoForge through an annotated subclass in the NeoForge module.
 */
public class GearReiCommonPlugin implements REICommonPlugin {
	@Override
	public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
		registry.register(GearExpansion.id("alloying"), AlloyingDisplay.SERIALIZER);
	}

	@Override
	public void registerDisplays(ServerDisplayRegistry registry) {
		registry.beginRecipeFiller(AlloyingRecipe.class)
			.filterType(ModRecipes.ALLOYING.get())
			.fill(AlloyingDisplay::new);
	}
}
