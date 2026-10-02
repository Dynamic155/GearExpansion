package com.gearexpansion.client;

import java.util.List;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.item.GearDurability;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.setbonus.SetBonus;
import com.gearexpansion.setbonus.SetBonuses;

/**
 * Adds material traits and set bonuses to tooltips, e.g. "Titanium Set (3/4)".
 * The set bonus is greyed out until all four pieces are worn.
 */
public final class GearTooltips {
	private GearTooltips() {
	}

	public static void init() {
		ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> {
			traits(stack, lines);
			SetBonuses.forPiece(stack).ifPresent(bonus -> setBonus(bonus, lines));
		});
	}

	private static void traits(ItemStack stack, List<Component> lines) {
		if (GearExpansionConfig.get().zincCorrosionProof && GearDurability.isGalvanized(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.corrosion_proof").withStyle(ChatFormatting.DARK_AQUA));
		}
		ModMaterials.ofGear(stack).ifPresent(set -> {
			if (stack.is(set.shield.get()) && set.blockingSpeed > MaterialSet.VANILLA_BLOCKING_SPEED) {
				lines.add(Component.translatable("trait.gearexpansion.lightweight_shield").withStyle(ChatFormatting.DARK_AQUA));
			}
			set.behavior.appendTooltip(set, stack, lines);
		});
	}

	private static void setBonus(SetBonus bonus, List<Component> lines) {
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
