package com.gearexpansion.fabric.client;

import java.util.List;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import com.gearexpansion.client.GearExpansionClient;
import com.gearexpansion.compat.ClientAlloyingRecipes;
import com.gearexpansion.recipe.ModRecipes;

public final class GearExpansionFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GearExpansionClient.init();
		// The alloying recipes Fabric API synced from the server (see GearExpansionFabric).
		ClientAlloyingRecipes.setSource(() -> {
			ClientLevel level = Minecraft.getInstance().level;
			return level == null ? List.of() : level.recipeAccess().getSynchronizedRecipes().getAllOfType(ModRecipes.ALLOYING.get());
		});
	}
}
