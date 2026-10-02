package com.gearexpansion.menu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.gearexpansion.block.entity.AlloyForgeBlockEntity;
import com.gearexpansion.recipe.ModRecipes;
import com.gearexpansion.registry.ModMenus;

/**
 * The Alloy Forge screen's slots: three inputs in a row, fuel below them, and the output,
 * followed by the player's inventory. Laid out like a furnace.
 */
public final class AlloyForgeMenu extends AbstractContainerMenu {
	public static final int RESULT_SLOT = AlloyForgeBlockEntity.RESULT_SLOT;
	private static final int FUEL_SLOT = AlloyForgeBlockEntity.FUEL_SLOT;
	private static final int INV_SLOT_START = AlloyForgeBlockEntity.SLOT_COUNT;
	private static final int INV_SLOT_END = INV_SLOT_START + 27;
	private static final int USE_ROW_SLOT_END = INV_SLOT_END + 9;

	private final Container container;
	private final ContainerData data;
	private final Level level;

	/** Client side: the server sends the contents. */
	public AlloyForgeMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(AlloyForgeBlockEntity.SLOT_COUNT), new SimpleContainerData(AlloyForgeBlockEntity.DATA_COUNT));
	}

	public AlloyForgeMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.ALLOY_FORGE.get(), containerId);
		checkContainerSize(container, AlloyForgeBlockEntity.SLOT_COUNT);
		checkContainerDataCount(data, AlloyForgeBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;
		this.level = inventory.player.level();

		for (int i = 0; i < AlloyForgeBlockEntity.INPUT_SLOTS; i++) {
			addSlot(new Slot(container, AlloyForgeBlockEntity.FIRST_INPUT_SLOT + i, 38 + i * 18, 17));
		}
		addSlot(new FuelSlot(container, FUEL_SLOT, 56, 53));
		addSlot(new AlloyForgeResultSlot(inventory.player, container, RESULT_SLOT, 125, 35));
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		ItemStack clicked = ItemStack.EMPTY;
		Slot slot = slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return clicked;
		}
		ItemStack stack = slot.getItem();
		clicked = stack.copy();
		if (slotIndex == RESULT_SLOT) {
			if (!moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, clicked);
		} else if (slotIndex < INV_SLOT_START) {
			if (!moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			boolean ingredient = isAlloyIngredient(stack);
			boolean fuel = AlloyForgeBlockEntity.isFuel(stack);
			if (ingredient || fuel) {
				// Fuel goes to the fuel slot first. Anything left of an item that is also an ingredient
				// (coal, for steel) then goes to the inputs.
				int before = stack.getCount();
				if (fuel) {
					moveItemStackTo(stack, FUEL_SLOT, FUEL_SLOT + 1, false);
				}
				if (ingredient && !stack.isEmpty()) {
					moveItemStackTo(stack, 0, AlloyForgeBlockEntity.INPUT_SLOTS, false);
				}
				if (stack.getCount() == before) {
					return ItemStack.EMPTY;
				}
			} else if (slotIndex < INV_SLOT_END) {
				if (!moveItemStackTo(stack, INV_SLOT_END, USE_ROW_SLOT_END, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == clicked.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return clicked;
	}

	/**
	 * Only the server knows the recipes. On the client this is always false, so a shift-click
	 * on an ingredient is predicted wrong for a moment and then corrected by the server.
	 */
	private boolean isAlloyIngredient(ItemStack stack) {
		return level instanceof ServerLevel serverLevel && ModRecipes.isAlloyIngredient(serverLevel.recipeAccess(), stack);
	}

	/** How far the current alloy is, from 0 to 1. */
	public float getBurnProgress() {
		int current = data.get(AlloyForgeBlockEntity.DATA_COOKING_PROGRESS);
		int total = data.get(AlloyForgeBlockEntity.DATA_COOKING_TOTAL_TIME);
		return total != 0 && current != 0 ? Mth.clamp((float) current / total, 0.0F, 1.0F) : 0.0F;
	}

	/** How much of the current fuel is left, from 0 to 1. */
	public float getLitProgress() {
		int duration = data.get(AlloyForgeBlockEntity.DATA_LIT_DURATION);
		if (duration == 0) {
			duration = 200;
		}
		return Mth.clamp((float) data.get(AlloyForgeBlockEntity.DATA_LIT_TIME) / duration, 0.0F, 1.0F);
	}

	public boolean isLit() {
		return data.get(AlloyForgeBlockEntity.DATA_LIT_TIME) > 0;
	}

	private static final class FuelSlot extends Slot {
		FuelSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return AlloyForgeBlockEntity.isFuel(stack);
		}
	}

	/** The output slot. Taking alloys gives the experience they stored, like a furnace's output. */
	private static final class AlloyForgeResultSlot extends Slot {
		private final Player player;
		private int removeCount;

		AlloyForgeResultSlot(Player player, Container container, int slot, int x, int y) {
			super(container, slot, x, y);
			this.player = player;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public ItemStack remove(int amount) {
			if (hasItem()) {
				removeCount += Math.min(amount, getItem().getCount());
			}
			return super.remove(amount);
		}

		@Override
		public void onTake(Player player, ItemStack carried) {
			checkTakeAchievements(carried);
			super.onTake(player, carried);
		}

		@Override
		protected void onQuickCraft(ItemStack picked, int count) {
			removeCount += count;
			checkTakeAchievements(picked);
		}

		@Override
		protected void checkTakeAchievements(ItemStack carried) {
			carried.onCraftedBy(player, removeCount);
			if (player instanceof ServerPlayer serverPlayer && container instanceof AlloyForgeBlockEntity forge) {
				forge.awardUsedRecipesAndPopExperience(serverPlayer);
			}
			removeCount = 0;
		}
	}
}
