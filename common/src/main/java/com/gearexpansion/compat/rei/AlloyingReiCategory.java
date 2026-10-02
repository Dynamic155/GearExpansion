package com.gearexpansion.compat.rei;

import java.util.ArrayList;
import java.util.List;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

import com.gearexpansion.compat.AlloyingRecipeLayout;
import com.gearexpansion.recipe.AlloyingRecipe;
import com.gearexpansion.registry.ModItems;

/** REI's Alloy Forge category: up to three counted inputs, the result, cooking time, and experience. */
final class AlloyingReiCategory implements DisplayCategory<AlloyingDisplay> {
	// REI's colors for furnace text, in light and dark mode.
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int DARK_TEXT_COLOR = 0xFFBBBBBB;
	// Space between REI's recipe border and the layout.
	private static final int PADDING = 5;

	@Override
	public CategoryIdentifier<? extends AlloyingDisplay> getCategoryIdentifier() {
		return AlloyingDisplay.CATEGORY;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("container.gearexpansion.alloy_forge");
	}

	@Override
	public Renderer getIcon() {
		return EntryStacks.of(ModItems.ALLOY_FORGE.get());
	}

	@Override
	public int getDisplayHeight() {
		return AlloyingRecipeLayout.HEIGHT + 2 * PADDING;
	}

	@Override
	public List<Widget> setupDisplay(AlloyingDisplay display, Rectangle bounds) {
		// The layout's top-left corner, centered in the bounds.
		int x = bounds.getCenterX() - AlloyingRecipeLayout.WIDTH / 2;
		int y = bounds.y + PADDING;
		List<Widget> widgets = new ArrayList<>();
		widgets.add(Widgets.createRecipeBase(bounds));

		List<EntryIngredient> inputs = display.getInputEntries();
		for (int i = 0; i < AlloyingRecipe.MAX_INGREDIENTS; i++) {
			Slot slot = Widgets.createSlot(new Point(x + AlloyingRecipeLayout.INPUT_X + i * AlloyingRecipeLayout.SLOT_SPACING, y + AlloyingRecipeLayout.INPUT_Y))
				.markInput();
			if (i < inputs.size()) {
				slot.entries(inputs.get(i));
			}
			widgets.add(slot);
		}

		widgets.add(Widgets.createBurningFire(new Point(x + AlloyingRecipeLayout.FLAME_X, y + AlloyingRecipeLayout.FLAME_Y))
			.animationDurationTicks(AlloyingRecipeLayout.FLAME_TICKS));
		widgets.add(Widgets.createArrow(new Point(x + AlloyingRecipeLayout.ARROW_X, y + AlloyingRecipeLayout.ARROW_Y))
			.animationDurationTicks(display.cookingTime()));

		Point output = new Point(x + AlloyingRecipeLayout.OUTPUT_X, y + AlloyingRecipeLayout.OUTPUT_Y);
		widgets.add(Widgets.createResultSlotBackground(output));
		widgets.add(Widgets.createSlot(output)
			.entries(display.getOutputEntries().getFirst())
			.disableBackground()
			.markOutput());

		Component experience = AlloyingRecipeLayout.experience(display.experience());
		if (experience != null) {
			widgets.add(Widgets.createLabel(new Point(x + AlloyingRecipeLayout.WIDTH, y), experience)
				.rightAligned()
				.noShadow()
				.color(TEXT_COLOR, DARK_TEXT_COLOR));
		}
		widgets.add(Widgets.createLabel(new Point(x + AlloyingRecipeLayout.WIDTH / 2, y + AlloyingRecipeLayout.HEIGHT - 9), AlloyingRecipeLayout.cookingTime(display.cookingTime()))
			.noShadow()
			.color(TEXT_COLOR, DARK_TEXT_COLOR)
			.tooltip(AlloyingRecipeLayout.boostedCookingTime(display.cookingTime())));
		return widgets;
	}
}
