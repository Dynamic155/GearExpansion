package com.gearexpansion.compat;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.item.crafting.RecipeHolder;

import com.gearexpansion.recipe.AlloyingRecipe;
import com.gearexpansion.recipe.ModRecipes;

/**
 * The alloying recipes the client knows about, for recipe viewers that read recipes on the client (JEI).
 *
 * <p>On 26.x the server doesn't send its recipes to clients. Each loader asks it to send the
 * alloying recipes along with the recipes it already syncs (Fabric API's recipe sync, or NeoForge's
 * {@code OnDatapackSyncEvent}), and the loader's client setup points {@link #setSource} at them.
 * This works on dedicated servers too, since Gear Expansion is installed on both sides.
 * In singleplayer the integrated server's recipe manager is used if nothing was synced.
 */
public final class ClientAlloyingRecipes {
	private static Supplier<Collection<RecipeHolder<AlloyingRecipe>>> source = List::of;

	private ClientAlloyingRecipes() {
	}

	/** Called once by the loader's client setup. */
	public static void setSource(Supplier<Collection<RecipeHolder<AlloyingRecipe>>> recipes) {
		source = recipes;
	}

	/** Every alloying recipe the client can see, sorted by id. Empty when not in a world. */
	public static List<RecipeHolder<AlloyingRecipe>> get() {
		Collection<RecipeHolder<AlloyingRecipe>> recipes = source.get();
		if (recipes.isEmpty()) {
			IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
			if (server != null) {
				recipes = fromServer(server);
			}
		}
		return recipes.stream()
			.sorted(Comparator.comparing(holder -> holder.id().identifier()))
			.toList();
	}

	@SuppressWarnings("unchecked")
	private static List<RecipeHolder<AlloyingRecipe>> fromServer(IntegratedServer server) {
		return server.getRecipeManager().getRecipes().stream()
			.filter(holder -> holder.value().getType() == ModRecipes.ALLOYING.get())
			.map(holder -> (RecipeHolder<AlloyingRecipe>) holder)
			.toList();
	}
}
