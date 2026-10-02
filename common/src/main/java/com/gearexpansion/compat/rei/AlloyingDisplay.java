package com.gearexpansion.compat.rei;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.compat.AlloyingRecipeLayout;
import com.gearexpansion.recipe.AlloyingRecipe;

/**
 * One alloying recipe as REI shows it. REI builds these on the server from the recipe manager and
 * sends them to clients with {@link #SERIALIZER}, so this class must not use client-only code.
 */
public class AlloyingDisplay extends BasicDisplay {
	public static final CategoryIdentifier<AlloyingDisplay> CATEGORY = CategoryIdentifier.of(GearExpansion.MOD_ID, "alloying");

	public static final DisplaySerializer<AlloyingDisplay> SERIALIZER = DisplaySerializer.of(
		RecordCodecBuilder.mapCodec(i -> i.group(
				EntryIngredient.codec().listOf().fieldOf("inputs").forGetter(AlloyingDisplay::getInputEntries),
				EntryIngredient.codec().listOf().fieldOf("outputs").forGetter(AlloyingDisplay::getOutputEntries),
				Identifier.CODEC.optionalFieldOf("location").forGetter(AlloyingDisplay::getDisplayLocation),
				Codec.INT.fieldOf("cookingtime").forGetter(AlloyingDisplay::cookingTime),
				Codec.FLOAT.fieldOf("experience").forGetter(AlloyingDisplay::experience))
			.apply(i, AlloyingDisplay::new)),
		StreamCodec.composite(
			EntryIngredient.streamCodec().apply(ByteBufCodecs.list()), AlloyingDisplay::getInputEntries,
			EntryIngredient.streamCodec().apply(ByteBufCodecs.list()), AlloyingDisplay::getOutputEntries,
			ByteBufCodecs.optional(Identifier.STREAM_CODEC), AlloyingDisplay::getDisplayLocation,
			ByteBufCodecs.VAR_INT, AlloyingDisplay::cookingTime,
			ByteBufCodecs.FLOAT, AlloyingDisplay::experience,
			AlloyingDisplay::new));

	private final int cookingTime;
	private final float experience;

	public AlloyingDisplay(RecipeHolder<AlloyingRecipe> holder) {
		this(holder.value().ingredients().stream()
				.map(ingredient -> EntryIngredients.ofItemStacks(AlloyingRecipeLayout.stacks(ingredient)))
				.toList(),
			List.of(EntryIngredient.of(EntryStacks.of(holder.value().result().create()))),
			Optional.of(holder.id().identifier()),
			holder.value().cookingTime(),
			holder.value().experience());
	}

	public AlloyingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs, Optional<Identifier> location, int cookingTime, float experience) {
		super(inputs, outputs, location);
		this.cookingTime = cookingTime;
		this.experience = experience;
	}

	/** In ticks, with regular fuel. */
	public int cookingTime() {
		return cookingTime;
	}

	public float experience() {
		return experience;
	}

	@Override
	public CategoryIdentifier<?> getCategoryIdentifier() {
		return CATEGORY;
	}

	@Override
	public DisplaySerializer<? extends AlloyingDisplay> getSerializer() {
		return SERIALIZER;
	}
}
