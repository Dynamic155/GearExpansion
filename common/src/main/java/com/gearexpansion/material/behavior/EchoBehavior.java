package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Echo: Muffled tools and Soft Step boots make no vibrations (see
 * {@link com.gearexpansion.setbonus.EchoSetBonus#muffles}), and the Echo Guard shield wraps melee
 * attackers in Darkness, like the warden.
 */
public final class EchoBehavior implements GearBehavior {
	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		int seconds = GearExpansionConfig.get().echoShieldDarknessSeconds;
		if (seconds > 0 && source.getDirectEntity() == attacker) {
			attacker.addEffect(new MobEffectInstance(MobEffects.DARKNESS, seconds * 20), defender);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.echo_guard", GearExpansionConfig.get().echoShieldDarknessSeconds).withStyle(ChatFormatting.DARK_AQUA));
		} else if (stack.is(set.boots.get())) {
			lines.add(Component.translatable("trait.gearexpansion.soft_step").withStyle(ChatFormatting.DARK_AQUA));
		} else if (set.tools().stream().anyMatch(tool -> stack.is(tool.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.muffled").withStyle(ChatFormatting.DARK_AQUA));
		}
	}
}
