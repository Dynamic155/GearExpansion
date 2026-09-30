package com.gearexpansion.client;

import java.util.List;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import com.gearexpansion.setbonus.SetBonus;
import com.gearexpansion.setbonus.SetBonuses;

/**
 * Adds set progress and the set bonus to armor tooltips, e.g. "Titanium Set (3/4)".
 * The bonus is greyed out until all four pieces are worn.
 */
public final class SetBonusTooltips {
	private SetBonusTooltips() {
	}

	public static void init() {
		ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> SetBonuses.forPiece(stack).ifPresent(bonus -> append(bonus, lines)));
	}

	private static void append(SetBonus bonus, List<Component> lines) {
		if (!bonus.enabled()) {
			return;
		}
		Player player = Minecraft.getInstance().player;
		int worn = player == null ? 0 : bonus.piecesWorn(player);
		boolean active = worn == SetBonus.PIECES;

		lines.add(Component.empty());
		lines.add(Component.translatable("tooltip.gearexpansion.set", bonus.name(), worn, SetBonus.PIECES)
			.withStyle(active ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		for (Component line : bonus.description()) {
			lines.add(Component.literal(" ").append(line).withStyle(active ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY));
		}
	}
}
