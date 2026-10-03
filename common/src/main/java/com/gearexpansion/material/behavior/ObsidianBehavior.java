package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Obsidian: everything comes from the material's stats (slow, durable tools; heavy armor; dropped
 * items that survive explosions; a shield that blocks blasts from every side). This describes it.
 */
public final class ObsidianBehavior implements GearBehavior {
	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.blast_wall").withStyle(ChatFormatting.DARK_PURPLE));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.dense").withStyle(ChatFormatting.DARK_PURPLE));
		} else if (set.tools().stream().anyMatch(tool -> stack.is(tool.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.unyielding").withStyle(ChatFormatting.DARK_PURPLE));
		}
		lines.add(Component.translatable("trait.gearexpansion.blastproof_item").withStyle(ChatFormatting.DARK_PURPLE));
	}
}
