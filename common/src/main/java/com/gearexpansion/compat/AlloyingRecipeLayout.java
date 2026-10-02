package com.gearexpansion.compat;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.block.entity.AlloyForgeBlockEntity;
import com.gearexpansion.recipe.AlloyingRecipe;

/**
 * What the JEI and REI Alloy Forge categories share: the layout, which follows the Alloy Forge
 * screen (three inputs in a row, the flame under the middle one, the arrow, then the result),
 * and the item stacks and text they show.
 *
 * <p>Positions are the top-left corner of the item inside each slot, relative to the category.
 */
public final class AlloyingRecipeLayout {
	public static final int WIDTH = 110;
	public static final int HEIGHT = 46;

	/** The first input; the others follow every {@link #SLOT_SPACING} pixels to the right. */
	public static final int INPUT_X = 1;
	public static final int INPUT_Y = 1;
	public static final int SLOT_SPACING = 18;
	/** The vanilla furnace flame, 14x14, under the middle input. */
	public static final int FLAME_X = 20;
	public static final int FLAME_Y = 20;
	/** The vanilla furnace arrow, 24x17. */
	public static final int ARROW_X = 57;
	public static final int ARROW_Y = 18;
	/** The result, inside a large 26x26 slot. */
	public static final int OUTPUT_X = 89;
	public static final int OUTPUT_Y = 19;

	/** How long a vanilla furnace flame takes to burn down, in ticks. */
	public static final int FLAME_TICKS = 300;

	/** The arrow's area on the Alloy Forge screen, which opens the category when clicked. */
	public static final int SCREEN_ARROW_X = 88;
	public static final int SCREEN_ARROW_Y = 34;
	public static final int SCREEN_ARROW_WIDTH = 24;
	public static final int SCREEN_ARROW_HEIGHT = 17;

	private static final DecimalFormat NUMBER = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

	private AlloyingRecipeLayout() {
	}

	/**
	 * Every item that fits {@code ingredient}, each with the ingredient's count.
	 * {@link net.minecraft.world.item.crafting.Ingredient#items} is deprecated to steer code towards
	 * {@code test}, but listing the items is what a recipe viewer needs.
	 */
	@SuppressWarnings("deprecation")
	public static List<ItemStack> stacks(AlloyingRecipe.CountedIngredient ingredient) {
		return ingredient.ingredient().items()
			.map(item -> new ItemStack(item, ingredient.count()))
			.toList();
	}

	/** "10s": a cooking time in ticks with regular fuel. */
	public static Component cookingTime(int ticks) {
		return Component.translatable("gui.gearexpansion.alloying.cooking_time", seconds(ticks));
	}

	/** Tooltip for the cooking time: how long it takes with a boost fuel such as blaze powder. */
	public static Component boostedCookingTime(int ticks) {
		return Component.translatable("gui.gearexpansion.alloying.boosted_time", seconds(ticks / AlloyForgeBlockEntity.BOOST_SPEED));
	}

	/** "0.7 XP", or null for no experience. */
	public static @Nullable Component experience(float experience) {
		if (experience <= 0.0F) {
			return null;
		}
		return Component.translatable("gui.gearexpansion.alloying.experience", NUMBER.format(experience));
	}

	private static String seconds(float ticks) {
		return NUMBER.format(ticks / 20.0F);
	}
}
