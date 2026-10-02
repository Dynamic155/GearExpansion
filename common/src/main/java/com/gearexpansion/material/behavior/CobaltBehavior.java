package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Cobalt: the fastest tools in the mod, and a shield that blocks the moment it's raised. Both
 * come from the material's stats; this only describes them in tooltips.
 */
public final class CobaltBehavior implements GearBehavior {
	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (stack.is(set.pickaxe.get()) || stack.is(set.shovel.get()) || stack.is(set.axe.get()) || stack.is(set.hoe.get())) {
			lines.add(Component.translatable("trait.gearexpansion.swift").withStyle(ChatFormatting.BLUE));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.quick_guard").withStyle(ChatFormatting.BLUE));
		}
	}
}
