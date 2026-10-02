package com.gearexpansion.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.menu.AlloyForgeMenu;
import com.gearexpansion.recipe.AlloyingRecipe;
import com.gearexpansion.recipe.AlloyingRecipeInput;
import com.gearexpansion.recipe.ModRecipes;
import com.gearexpansion.registry.ModBlockEntities;

/**
 * The Alloy Forge's inventory and smelting logic, modeled on vanilla's furnace block entity.
 *
 * <p>Slots 0 to 2 are inputs, 3 is fuel, and 4 is the output. It burns normal furnace fuel.
 * Boost fuels ({@link #BOOST_FUELS}: blaze powder and lava buckets by default) make it alloy
 * twice as fast while they burn. Hoppers insert into the inputs from the top and into the fuel
 * slot from the sides, and take the output from the bottom.
 */
public final class AlloyForgeBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int FIRST_INPUT_SLOT = 0;
	public static final int INPUT_SLOTS = 3;
	public static final int FUEL_SLOT = 3;
	public static final int RESULT_SLOT = 4;
	public static final int SLOT_COUNT = 5;

	public static final int DATA_LIT_TIME = 0;
	public static final int DATA_LIT_DURATION = 1;
	public static final int DATA_COOKING_PROGRESS = 2;
	public static final int DATA_COOKING_TOTAL_TIME = 3;
	public static final int DATA_COUNT = 4;

	/** Fuels that make the forge alloy faster while they burn. */
	public static final TagKey<Item> BOOST_FUELS = TagKey.create(Registries.ITEM, GearExpansion.id("alloy_forge_boost_fuels"));
	public static final float BOOST_SPEED = 2.0F;
	/** Burn time for boost fuels that aren't normal furnace fuel, such as blaze powder. Same as coal. */
	public static final int BOOST_ONLY_BURN_TIME = 1600;

	private static final Component DEFAULT_NAME = Component.translatable("container.gearexpansion.alloy_forge");
	private static final int[] SLOTS_FOR_UP = {0, 1, 2};
	private static final int[] SLOTS_FOR_DOWN = {RESULT_SLOT, FUEL_SLOT};
	private static final int[] SLOTS_FOR_SIDES = {FUEL_SLOT};
	private static final int BURN_COOL_SPEED = 2;
	private static final Codec<Map<ResourceKey<Recipe<?>>, Integer>> RECIPES_USED_CODEC = Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
	private int litTimeRemaining;
	private int litTotalTime;
	private int cookingTimer;
	private int cookingTotalTime;
	private float speedMultiplier = 1.0F;
	private final Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();
	private final RecipeManager.CachedCheck<AlloyingRecipeInput, AlloyingRecipe> quickCheck = RecipeManager.createCheck(ModRecipes.ALLOYING.get());

	private final ContainerData dataAccess = new ContainerData() {
		@Override
		public int get(int dataId) {
			return switch (dataId) {
				case DATA_LIT_TIME -> litTimeRemaining;
				case DATA_LIT_DURATION -> litTotalTime;
				case DATA_COOKING_PROGRESS -> cookingTimer;
				case DATA_COOKING_TOTAL_TIME -> cookingTotalTime;
				default -> 0;
			};
		}

		@Override
		public void set(int dataId, int value) {
			switch (dataId) {
				case DATA_LIT_TIME -> litTimeRemaining = value;
				case DATA_LIT_DURATION -> litTotalTime = value;
				case DATA_COOKING_PROGRESS -> cookingTimer = value;
				case DATA_COOKING_TOTAL_TIME -> cookingTotalTime = value;
				default -> {
				}
			}
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public AlloyForgeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ALLOY_FORGE.get(), pos, state);
	}

	// Ticking --------------------------------------------------------------------------

	public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, AlloyForgeBlockEntity forge) {
		boolean changed = false;
		boolean wasLit = forge.litTimeRemaining > 0;
		if (wasLit) {
			forge.litTimeRemaining--;
		}
		boolean isLit = forge.litTimeRemaining > 0;

		ItemStack fuel = forge.items.get(FUEL_SLOT);
		AlloyingRecipeInput input = forge.input();
		boolean hasInput = !input.isEmpty();
		if (isLit || !fuel.isEmpty() && hasInput) {
			RecipeHolder<AlloyingRecipe> recipe = hasInput ? forge.quickCheck.getRecipeFor(input, level).orElse(null) : null;
			ItemStack result = recipe == null ? ItemStack.EMPTY : recipe.value().assemble(input);
			if (!result.isEmpty() && forge.canFitResult(result)) {
				if (!isLit) {
					int burnTime = forge.burnDuration(level, fuel);
					if (burnTime > 0) {
						float newSpeed = forge.speedMultiplier(level, fuel);
						forge.litTimeRemaining = burnTime;
						forge.litTotalTime = burnTime;
						if (newSpeed != forge.speedMultiplier) {
							// Keep the progress bar where it was when the speed changes mid-alloy.
							float completion = forge.cookingTotalTime > 0 ? (float) forge.cookingTimer / forge.cookingTotalTime : 0.0F;
							forge.speedMultiplier = newSpeed;
							forge.cookingTotalTime = forge.totalCookTime(recipe.value());
							forge.cookingTimer = (int) Math.ceil(completion * forge.cookingTotalTime);
						}
						consumeFuel(level, pos, forge.items, fuel);
						isLit = true;
						changed = true;
					}
				}

				if (isLit) {
					if (forge.cookingTotalTime <= 0) {
						forge.cookingTotalTime = forge.totalCookTime(recipe.value());
					}
					forge.cookingTimer++;
					if (forge.cookingTimer >= forge.cookingTotalTime) {
						forge.cookingTimer = 0;
						forge.alloy(recipe.value(), result);
						forge.cookingTotalTime = forge.totalCookTime(recipe.value());
						forge.recipesUsed.addTo(recipe.id(), 1);
						changed = true;
					}
				} else {
					forge.cookingTimer = 0;
				}
			} else {
				forge.cookingTimer = 0;
			}
		} else if (forge.cookingTimer > 0) {
			forge.cookingTimer = Mth.clamp(forge.cookingTimer - BURN_COOL_SPEED, 0, forge.cookingTotalTime);
		}

		if (wasLit != isLit) {
			changed = true;
			state = state.setValue(AbstractFurnaceBlock.LIT, isLit);
			level.setBlockAndUpdate(pos, state);
		}
		if (changed) {
			setChanged(level, pos, state);
		}
	}

	private AlloyingRecipeInput input() {
		return new AlloyingRecipeInput(items.subList(FIRST_INPUT_SLOT, FIRST_INPUT_SLOT + INPUT_SLOTS));
	}

	private boolean canFitResult(ItemStack result) {
		ItemStack output = items.get(RESULT_SLOT);
		if (output.isEmpty()) {
			return true;
		}
		if (!ItemStack.isSameItemSameComponents(output, result)) {
			return false;
		}
		return output.getCount() + result.getCount() <= Math.min(getMaxStackSize(), result.getMaxStackSize());
	}

	private void alloy(AlloyingRecipe recipe, ItemStack result) {
		recipe.consume(items.subList(FIRST_INPUT_SLOT, FIRST_INPUT_SLOT + INPUT_SLOTS));
		ItemStack output = items.get(RESULT_SLOT);
		if (output.isEmpty()) {
			items.set(RESULT_SLOT, result.copy());
		} else {
			output.grow(result.getCount());
		}
	}

	private int totalCookTime(AlloyingRecipe recipe) {
		return speedMultiplier > 0.0F ? (int) Math.ceil(recipe.cookingTime() / speedMultiplier) : recipe.cookingTime();
	}

	/** Uses one fuel item. A lava bucket leaves its empty bucket behind, like in a furnace. */
	private static void consumeFuel(ServerLevel level, BlockPos pos, NonNullList<ItemStack> items, ItemStack fuel) {
		ItemStackTemplate remainder = fuel.getItem().getCraftingRemainder();
		ItemStack newFuel = fuel;
		fuel.shrink(1);
		if (remainder != null) {
			if (fuel.isEmpty()) {
				newFuel = remainder.create();
			} else {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), remainder.create());
			}
		}
		items.set(FUEL_SLOT, newFuel);
	}

	// Fuel -------------------------------------------------------------------------------

	/** Whether {@code stack} can burn in the forge: any furnace fuel, or a boost fuel. */
	public static boolean isFuel(ItemStack stack) {
		return stack.has(DataComponents.COOKING_FUEL) || stack.is(BOOST_FUELS);
	}

	/**
	 * Furnace fuels burn as long as in a regular furnace. Their burn times are data driven in 26.x,
	 * resolved with this block as context. Boost fuels that aren't furnace fuel burn for
	 * {@link #BOOST_ONLY_BURN_TIME}.
	 */
	private int burnDuration(ServerLevel level, ItemStack fuel) {
		int burnTime = ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, getLootContext(level), 0);
		if (burnTime <= 0 && fuel.is(BOOST_FUELS)) {
			return BOOST_ONLY_BURN_TIME;
		}
		return burnTime;
	}

	private float speedMultiplier(ServerLevel level, ItemStack fuel) {
		float speed = ResolvableFloat.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::speedMultiplier, getLootContext(level), 1.0F);
		return fuel.is(BOOST_FUELS) ? speed * BOOST_SPEED : speed;
	}

	/** Whether the fuel that is burning right now is a boost fuel. */
	public boolean isBoosted() {
		return litTimeRemaining > 0 && speedMultiplier > 1.0F;
	}

	/** Ticks the current alloy has cooked for, for Jade. */
	public int cookingTimer() {
		return cookingTimer;
	}

	/** Ticks the current alloy takes in total, for Jade. Zero when nothing is cooking. */
	public int cookingTotalTime() {
		return cookingTotalTime;
	}

	// Experience -------------------------------------------------------------------------

	/** Gives the player the experience stored by finished alloys. Called when they take the output. */
	public void awardUsedRecipesAndPopExperience(ServerPlayer player) {
		List<RecipeHolder<?>> recipes = popExperience(player.level(), player.position());
		for (RecipeHolder<?> recipe : recipes) {
			player.triggerRecipeCrafted(recipe, items);
		}
		recipesUsed.clear();
	}

	private List<RecipeHolder<?>> popExperience(ServerLevel level, Vec3 position) {
		List<RecipeHolder<?>> recipes = new ArrayList<>();
		for (Reference2IntMap.Entry<ResourceKey<Recipe<?>>> entry : recipesUsed.reference2IntEntrySet()) {
			level.recipeAccess().byKey(entry.getKey()).ifPresent(holder -> {
				if (holder.value() instanceof AlloyingRecipe recipe) {
					recipes.add(holder);
					createExperience(level, position, entry.getIntValue(), recipe.experience());
				}
			});
		}
		return recipes;
	}

	private static void createExperience(ServerLevel level, Vec3 position, int amount, float value) {
		int reward = Mth.floor(amount * value);
		float fraction = Mth.frac(amount * value);
		if (fraction != 0.0F && level.getRandom().nextFloat() < fraction) {
			reward++;
		}
		ExperienceOrb.award(level, position, reward);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel serverLevel) {
			popExperience(serverLevel, Vec3.atCenterOf(pos));
		}
	}

	// Container --------------------------------------------------------------------------

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		ItemStack old = items.get(slot);
		boolean same = !stack.isEmpty() && ItemStack.isSameItemSameComponents(old, stack);
		items.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		// A different input restarts the current alloy, like changing a furnace's input.
		if (slot < INPUT_SLOTS && !same && level instanceof ServerLevel serverLevel) {
			cookingTotalTime = quickCheck.getRecipeFor(input(), serverLevel)
				.map(recipe -> totalCookTime(recipe.value()))
				.orElse(AlloyingRecipe.DEFAULT_COOKING_TIME);
			cookingTimer = 0;
			setChanged();
		}
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == RESULT_SLOT) {
			return false;
		}
		if (slot == FUEL_SLOT) {
			return isFuel(stack);
		}
		// Clients don't know the recipes, so only the server filters inputs.
		return !(level instanceof ServerLevel serverLevel) || ModRecipes.isAlloyIngredient(serverLevel.recipeAccess(), stack);
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		if (direction == Direction.DOWN) {
			return SLOTS_FOR_DOWN;
		}
		return direction == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
	}

	/**
	 * Hoppers keep each kind of item in one input slot, so a stream of one metal can't fill
	 * all three slots and block the others.
	 */
	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		if (!canPlaceItem(slot, stack)) {
			return false;
		}
		if (slot < INPUT_SLOTS && items.get(slot).isEmpty()) {
			for (int other = FIRST_INPUT_SLOT; other < INPUT_SLOTS; other++) {
				if (other != slot && ItemStack.isSameItemSameComponents(items.get(other), stack)) {
					return false;
				}
			}
		}
		return true;
	}

	/** From below, hoppers take the output and empty buckets left by lava, same as a furnace. */
	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return direction != Direction.DOWN || slot != FUEL_SLOT || stack.is(ItemTags.FURNACE_FUEL_BOTTOM_TAKEABLE);
	}

	// Menu and saving --------------------------------------------------------------------

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new AlloyForgeMenu(containerId, inventory, this, dataAccess);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		cookingTimer = input.getIntOr("cooking_time_spent", 0);
		cookingTotalTime = input.getIntOr("cooking_total_time", 0);
		litTimeRemaining = input.getIntOr("lit_time_remaining", 0);
		litTotalTime = input.getIntOr("lit_total_time", 0);
		speedMultiplier = input.getFloatOr("speed_multiplier", 1.0F);
		recipesUsed.clear();
		recipesUsed.putAll(input.read("recipes_used", RECIPES_USED_CODEC).orElse(Map.of()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("cooking_time_spent", cookingTimer);
		output.putInt("cooking_total_time", cookingTotalTime);
		output.putInt("lit_time_remaining", litTimeRemaining);
		output.putInt("lit_total_time", litTotalTime);
		output.putFloat("speed_multiplier", speedMultiplier);
		ContainerHelper.saveAllItems(output, items);
		output.store("recipes_used", RECIPES_USED_CODEC, recipesUsed);
	}
}
