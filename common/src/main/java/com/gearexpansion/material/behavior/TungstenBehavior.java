package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Tungsten: Crushing axes and spears knock targets back further. The armor's weight and the
 * shield's slow raise come from the material's stats; the tooltips describe them.
 */
public final class TungstenBehavior implements GearBehavior {
	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		if (weapon.is(set.axe.get()) || weapon.is(set.spear.get())) {
			double strength = GearExpansionConfig.get().tungstenKnockback / 10.0;
			if (strength > 0) {
				target.knockback(strength, attacker.getX() - target.getX(), attacker.getZ() - target.getZ(), attacker.damageSources().mobAttack(attacker), 0.0F);
			}
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (stack.is(set.axe.get()) || stack.is(set.spear.get())) {
			lines.add(Component.translatable("trait.gearexpansion.crushing").withStyle(ChatFormatting.DARK_GRAY));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.bulwark").withStyle(ChatFormatting.DARK_GRAY));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.heavy").withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
