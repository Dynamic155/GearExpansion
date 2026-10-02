package com.gearexpansion.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

import com.gearexpansion.compat.AlloyingRecipeLayout;
import com.gearexpansion.recipe.AlloyingRecipe;
import com.gearexpansion.registry.ModItems;

/** JEI's Alloy Forge category: up to three counted inputs, the result, cooking time, and experience. */
final class AlloyingJeiCategory extends AbstractRecipeCategory<RecipeHolder<AlloyingRecipe>> {
	// The gray JEI uses for furnace times and experience.
	private static final int TEXT_COLOR = 0xFF808080;

	AlloyingJeiCategory(IGuiHelper guiHelper) {
		super(GearJeiPlugin.ALLOYING,
			Component.translatable("container.gearexpansion.alloy_forge"),
			guiHelper.createDrawableItemLike(ModItems.ALLOY_FORGE.get()),
			AlloyingRecipeLayout.WIDTH,
			AlloyingRecipeLayout.HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
		AlloyingRecipe recipe = holder.value();
		List<AlloyingRecipe.CountedIngredient> ingredients = recipe.ingredients();
		// All three slots get a background, like the forge, even when the recipe uses fewer.
		for (int i = 0; i < AlloyingRecipe.MAX_INGREDIENTS; i++) {
			IRecipeSlotBuilder slot = builder.addInputSlot(AlloyingRecipeLayout.INPUT_X + i * AlloyingRecipeLayout.SLOT_SPACING, AlloyingRecipeLayout.INPUT_Y)
				.setStandardSlotBackground();
			if (i < ingredients.size()) {
				slot.addItemStacks(AlloyingRecipeLayout.stacks(ingredients.get(i)));
			}
		}
		builder.addOutputSlot(AlloyingRecipeLayout.OUTPUT_X, AlloyingRecipeLayout.OUTPUT_Y)
			.setOutputSlotBackground()
			.add(recipe.result());
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<AlloyingRecipe> holder, IFocusGroup focuses) {
		AlloyingRecipe recipe = holder.value();
		builder.addAnimatedRecipeFlameWidget(AlloyingRecipeLayout.FLAME_TICKS)
			.setPosition(AlloyingRecipeLayout.FLAME_X, AlloyingRecipeLayout.FLAME_Y);
		builder.addAnimatedRecipeArrowWidget(recipe.cookingTime())
			.setPosition(AlloyingRecipeLayout.ARROW_X, AlloyingRecipeLayout.ARROW_Y);

		Component experience = AlloyingRecipeLayout.experience(recipe.experience());
		if (experience != null) {
			builder.addText(experience, getWidth(), 10)
				.setPosition(0, 0, getWidth(), getHeight(), HorizontalAlignment.RIGHT, VerticalAlignment.TOP)
				.setTextAlignment(HorizontalAlignment.RIGHT)
				.setColor(TEXT_COLOR);
		}
		builder.addText(AlloyingRecipeLayout.cookingTime(recipe.cookingTime()), getWidth(), 10)
			.setPosition(0, 0, getWidth(), getHeight(), HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM)
			.setTextAlignment(HorizontalAlignment.CENTER)
			.setTextAlignment(VerticalAlignment.BOTTOM)
			.setColor(TEXT_COLOR)
			.setTooltip(AlloyingRecipeLayout.boostedCookingTime(recipe.cookingTime()));
	}
}
