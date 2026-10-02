package com.gearexpansion.setbonus;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Carries the player whose enchanting offers are being worked out, from the enchanting
 * table's menu to the enchantment cost calculation. Offers are computed on the server
 * thread in one go, so a thread-local is enough.
 */
public final class EnchantingBonus {
	private static final ThreadLocal<Player> CURRENT = new ThreadLocal<>();

	private EnchantingBonus() {
	}

	public static void begin(@Nullable Player player) {
		CURRENT.set(player);
	}

	public static void end() {
		CURRENT.remove();
	}

	public static int adjustBookshelves(int bookcases) {
		Player player = CURRENT.get();
		if (player == null) {
			return bookcases;
		}
		// Vanilla counts at most 15 bookshelves.
		return Math.min(15, bookcases + SetBonuses.extraEnchantingBookshelves(player));
	}
}
