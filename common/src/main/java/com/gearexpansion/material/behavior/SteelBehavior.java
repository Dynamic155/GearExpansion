package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/** Steel has no special effects: it's simply sturdy. The tooltip says so on the shield, its standout piece. */
public final class SteelBehavior implements GearBehavior {
	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.reinforced").withStyle(ChatFormatting.GRAY));
		}
	}
}
