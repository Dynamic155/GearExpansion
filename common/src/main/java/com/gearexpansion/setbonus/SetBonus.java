package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.material.MaterialSet;

/**
 * A bonus granted for wearing all four armor pieces of one material.
 * Subclasses override only the hooks they need.
 */
public abstract class SetBonus {
	public static final int PIECES = 4;

	public final MaterialSet material;

	protected SetBonus(MaterialSet material) {
		this.material = material;
	}

	/** Whether the bonus is switched on in the config. */
	public abstract boolean enabled();

	/** Tooltip lines describing the bonus, shown under each armor piece. */
	public abstract List<Component> description();

	/** Called every tick for each player wearing the full set, on the server. */
	public void tick(ServerPlayer player) {
	}

	/** Lets the bonus change how much durability an item loses. Called only while the full set is worn. */
	public int modifyDurabilityLoss(int amount, LivingEntity wearer, ServerLevel level) {
		return amount;
	}

	public Component name() {
		return Component.translatable("set_bonus.gearexpansion." + material.name);
	}

	public int piecesWorn(LivingEntity entity) {
		int worn = 0;
		for (var entry : material.armorBySlot().entrySet()) {
			if (entity.getItemBySlot(entry.getKey()).is(entry.getValue().get())) {
				worn++;
			}
		}
		return worn;
	}

	public boolean isActive(LivingEntity entity) {
		return enabled() && piecesWorn(entity) == PIECES;
	}

	/** Whether the item is one of this set's armor pieces. */
	public boolean isPiece(ItemStack stack) {
		return material.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()));
	}
}
