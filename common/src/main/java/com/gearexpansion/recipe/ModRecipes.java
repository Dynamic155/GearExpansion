package com.gearexpansion.recipe;

import java.lang.ref.WeakReference;
import java.util.List;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.gearexpansion.GearExpansion;

/** The Alloy Forge's recipe type and serializer, both named {@code gearexpansion:alloying}. */
public final class ModRecipes {
	public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(GearExpansion.MOD_ID, Registries.RECIPE_TYPE);
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(GearExpansion.MOD_ID, Registries.RECIPE_SERIALIZER);

	public static final RegistrySupplier<RecipeType<AlloyingRecipe>> ALLOYING = TYPES.register("alloying", () -> new RecipeType<>() {
		@Override
		public String toString() {
			return GearExpansion.MOD_ID + ":alloying";
		}
	});
	public static final RegistrySupplier<RecipeSerializer<AlloyingRecipe>> ALLOYING_SERIALIZER = SERIALIZERS.register("alloying", () -> AlloyingRecipe.SERIALIZER);

	// Alloying recipes of the last recipe manager asked about. A /reload creates a new manager, which refreshes this.
	private static WeakReference<RecipeManager> cachedManager = new WeakReference<>(null);
	private static List<AlloyingRecipe> cachedRecipes = List.of();

	private ModRecipes() {
	}

	public static void init() {
		TYPES.register();
		SERIALIZERS.register();
	}

	/** Every loaded alloying recipe. Server side only: clients don't receive recipes. */
	public static synchronized List<AlloyingRecipe> alloyingRecipes(RecipeManager manager) {
		if (manager != cachedManager.get()) {
			cachedRecipes = manager.getRecipes().stream()
				.filter(holder -> holder.value().getType() == ALLOYING.get())
				.map(holder -> (AlloyingRecipe) holder.value())
				.toList();
			cachedManager = new WeakReference<>(manager);
		}
		return cachedRecipes;
	}

	/** Whether {@code stack} is an ingredient of any alloying recipe. */
	public static boolean isAlloyIngredient(RecipeManager manager, ItemStack stack) {
		return !stack.isEmpty() && alloyingRecipes(manager).stream().anyMatch(recipe -> recipe.uses(stack));
	}
}
