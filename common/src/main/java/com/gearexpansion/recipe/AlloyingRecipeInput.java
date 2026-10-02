package com.gearexpansion.recipe;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The Alloy Forge's three input slots, as seen by {@link AlloyingRecipe}. */
public record AlloyingRecipeInput(List<ItemStack> items) implements RecipeInput {
	public AlloyingRecipeInput {
		items = List.copyOf(items);
	}

	@Override
	public ItemStack getItem(int index) {
		return items.get(index);
	}

	@Override
	public int size() {
		return items.size();
	}
}
