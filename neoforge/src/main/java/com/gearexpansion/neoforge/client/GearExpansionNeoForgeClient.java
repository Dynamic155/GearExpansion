package com.gearexpansion.neoforge.client;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import com.gearexpansion.client.GearExpansionClient;
import com.gearexpansion.compat.ClientAlloyingRecipes;
import com.gearexpansion.recipe.AlloyingRecipe;
import com.gearexpansion.recipe.ModRecipes;

/** Client-only NeoForge setup. Kept in its own class so servers never load client code. */
public final class GearExpansionNeoForgeClient {
	// The alloying recipes the server sent with its last recipe sync (see GearExpansionNeoForge).
	private static Collection<RecipeHolder<AlloyingRecipe>> syncedRecipes = List.of();

	private GearExpansionNeoForgeClient() {
	}

	public static void init(ModContainer container) {
		GearExpansionClient.init();
		// Adds a "Config" button to Gear Expansion in the Mods list, when YACL is there to build it.
		if (GearExpansionClient.hasConfigScreen()) {
			container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> GearExpansionClient.configScreen(parent));
		}

		// JEI listens at the lowest priority, so the recipes are stored before it reloads.
		NeoForge.EVENT_BUS.addListener(RecipesReceivedEvent.class, event -> syncedRecipes = List.copyOf(event.getRecipeMap().byType(ModRecipes.ALLOYING.get())));
		NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> syncedRecipes = List.of());
		ClientAlloyingRecipes.setSource(() -> syncedRecipes);
	}
}
