package com.gearexpansion.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.neoforge.client.GearExpansionNeoForgeClient;
import com.gearexpansion.recipe.ModRecipes;

@Mod(GearExpansion.MOD_ID)
public final class GearExpansionNeoForge {
	public GearExpansionNeoForge(ModContainer container) {
		GearExpansion.init();

		// Send alloying recipes to clients, for recipe viewers such as JEI. See ClientAlloyingRecipes.
		NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> event.sendRecipes(ModRecipes.ALLOYING.get()));

		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			GearExpansionNeoForgeClient.init(container);
		}
	}
}
