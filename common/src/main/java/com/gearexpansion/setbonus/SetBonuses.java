package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Optional;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** The set bonus for every material, plus the hooks that drive them. */
public final class SetBonuses {
	public static final SetBonus TITANIUM = new TitaniumSetBonus();

	public static final List<SetBonus> ALL = List.of(TITANIUM);

	private SetBonuses() {
	}

	public static void init() {
		TickEvent.PLAYER_POST.register(player -> {
			if (player instanceof ServerPlayer serverPlayer) {
				for (SetBonus bonus : ALL) {
					if (bonus.isActive(serverPlayer)) {
						bonus.tick(serverPlayer);
					}
				}
			}
		});
	}

	/** Called from the durability mixin whenever an item held or worn by {@code wearer} is about to lose durability. */
	public static int modifyDurabilityLoss(int amount, LivingEntity wearer, ServerLevel level) {
		if (amount <= 0 || wearer == null) {
			return amount;
		}
		for (SetBonus bonus : ALL) {
			if (bonus.isActive(wearer)) {
				amount = bonus.modifyDurabilityLoss(amount, wearer, level);
			}
		}
		return amount;
	}

	public static Optional<SetBonus> forPiece(ItemStack stack) {
		return ALL.stream().filter(bonus -> bonus.isPiece(stack)).findFirst();
	}
}
