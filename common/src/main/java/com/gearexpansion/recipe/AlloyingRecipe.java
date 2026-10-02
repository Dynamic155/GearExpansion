package com.gearexpansion.recipe;

import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * An Alloy Forge recipe: one to three ingredients, each with a count, melted into a result.
 * It is shapeless, so each ingredient can sit in any input slot, and one ingredient may be
 * spread over several slots. Every non-empty slot must feed one of the ingredients.
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "type": "gearexpansion:alloying",
 *   "ingredients": [
 *     { "ingredient": "#c:ingots/copper", "count": 3 },
 *     { "ingredient": "#c:ingots/zinc" }
 *   ],
 *   "result": { "id": "gearexpansion:brass_ingot", "count": 4 },
 *   "cookingtime": 200,
 *   "experience": 0.7
 * }
 * }</pre>
 *
 * <p>Like vanilla's brewing recipes, alloying recipes are "special": they are not learned or
 * shown in the vanilla recipe book, which has no Alloy Forge tab.
 */
public final class AlloyingRecipe implements Recipe<AlloyingRecipeInput> {
	public static final int MAX_INGREDIENTS = 3;
	public static final int DEFAULT_COOKING_TIME = 200;

	public static final MapCodec<AlloyingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			CountedIngredient.CODEC.listOf(1, MAX_INGREDIENTS).fieldOf("ingredients").forGetter(AlloyingRecipe::ingredients),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(AlloyingRecipe::result),
			ExtraCodecs.POSITIVE_INT.fieldOf("cookingtime").forGetter(AlloyingRecipe::cookingTime),
			Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(AlloyingRecipe::experience))
		.apply(i, AlloyingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, AlloyingRecipe> STREAM_CODEC = StreamCodec.composite(
		CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_INGREDIENTS)), AlloyingRecipe::ingredients,
		ItemStackTemplate.STREAM_CODEC, AlloyingRecipe::result,
		ByteBufCodecs.VAR_INT, AlloyingRecipe::cookingTime,
		ByteBufCodecs.FLOAT, AlloyingRecipe::experience,
		AlloyingRecipe::new);

	public static final RecipeSerializer<AlloyingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

	private final List<CountedIngredient> ingredients;
	private final ItemStackTemplate result;
	private final int cookingTime;
	private final float experience;

	public AlloyingRecipe(List<CountedIngredient> ingredients, ItemStackTemplate result, int cookingTime, float experience) {
		this.ingredients = List.copyOf(ingredients);
		this.result = result;
		this.cookingTime = cookingTime;
		this.experience = experience;
	}

	public List<CountedIngredient> ingredients() {
		return ingredients;
	}

	public ItemStackTemplate result() {
		return result;
	}

	public int cookingTime() {
		return cookingTime;
	}

	public float experience() {
		return experience;
	}

	/** Whether {@code stack} could be used by any of this recipe's ingredients. */
	public boolean uses(ItemStack stack) {
		return ingredients.stream().anyMatch(ingredient -> ingredient.ingredient().test(stack));
	}

	@Override
	public boolean matches(AlloyingRecipeInput input, Level level) {
		return assign(input.items()) != null;
	}

	@Override
	public ItemStack assemble(AlloyingRecipeInput input) {
		return result.create();
	}

	/**
	 * Removes exactly the required count of each ingredient from {@code slots}.
	 * Only call this after {@link #matches} returned true for the same stacks.
	 */
	public void consume(List<ItemStack> slots) {
		int[] assignment = assign(slots);
		if (assignment == null) {
			return;
		}
		for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
			int remaining = ingredients.get(ingredient).count();
			for (int slot = 0; slot < slots.size() && remaining > 0; slot++) {
				if (assignment[slot] == ingredient) {
					ItemStack stack = slots.get(slot);
					int taken = Math.min(remaining, stack.getCount());
					stack.shrink(taken);
					remaining -= taken;
				}
			}
		}
	}

	/**
	 * Finds which ingredient each slot feeds: every non-empty slot feeds one ingredient it matches,
	 * and each ingredient gets at least its count in total. Returns the ingredient index per slot
	 * (-1 for empty slots), or null if the slots don't match. There are at most 3 slots and 3
	 * ingredients, so trying every assignment is cheap.
	 */
	private int @Nullable [] assign(List<ItemStack> slots) {
		int[] assignment = new int[slots.size()];
		Arrays.fill(assignment, -1);
		return search(slots, assignment, 0) ? assignment : null;
	}

	private boolean search(List<ItemStack> slots, int[] assignment, int slot) {
		if (slot == slots.size()) {
			return countsMet(slots, assignment);
		}
		ItemStack stack = slots.get(slot);
		if (stack.isEmpty()) {
			assignment[slot] = -1;
			return search(slots, assignment, slot + 1);
		}
		for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
			if (ingredients.get(ingredient).ingredient().test(stack)) {
				assignment[slot] = ingredient;
				if (search(slots, assignment, slot + 1)) {
					return true;
				}
			}
		}
		assignment[slot] = -1;
		return false;
	}

	private boolean countsMet(List<ItemStack> slots, int[] assignment) {
		int[] totals = new int[ingredients.size()];
		for (int slot = 0; slot < slots.size(); slot++) {
			if (assignment[slot] >= 0) {
				totals[assignment[slot]] += slots.get(slot).getCount();
			}
		}
		for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
			if (totals[ingredient] < ingredients.get(ingredient).count()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<AlloyingRecipe> getSerializer() {
		return ModRecipes.ALLOYING_SERIALIZER.get();
	}

	@Override
	public RecipeType<AlloyingRecipe> getType() {
		return ModRecipes.ALLOYING.get();
	}

	/** Not placeable by the recipe book, same as vanilla's brewing recipes. */
	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	/** Required by the interface but unused: the recipe has no displays, so it never reaches a recipe book tab. */
	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}

	/** An ingredient and how many of it one craft uses. */
	public record CountedIngredient(Ingredient ingredient, int count) {
		public static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(i -> i.group(
				Ingredient.CODEC.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
				ExtraCodecs.intRange(1, 99).optionalFieldOf("count", 1).forGetter(CountedIngredient::count))
			.apply(i, CountedIngredient::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, CountedIngredient::ingredient,
			ByteBufCodecs.VAR_INT, CountedIngredient::count,
			CountedIngredient::new);
	}
}
