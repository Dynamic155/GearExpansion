package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Fairy Kei: Whimsy weapons make what they hit float for a moment, each armor piece softens falls
 * (Cloud Step), and the shield stops knockback while blocking.
 */
public final class FairyKeiBehavior implements GearBehavior {
	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		int ticks = GearExpansionConfig.get().fairyKeiFloatTicks;
		if (!set.isWeapon(weapon) || ticks <= 0 || !target.isAlive() || target.is(PastelPrincessBehavior.BOSSES)
				|| !(target.level() instanceof ServerLevel level)) {
			return;
		}
		target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ticks, 0), attacker);
		level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 6, 0.3, 0.1, 0.3, 0.01);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.8F);
	}

	@Override
	public float modifyWearerDamage(MaterialSet set, LivingEntity wearer, DamageSource source, float damage, int pieces) {
		if (source.is(DamageTypeTags.IS_FALL)) {
			return damage * Math.max(0.0F, 1.0F - pieces * GearExpansionConfig.get().fairyKeiFallReduction / 100.0F);
		}
		return damage;
	}

	@Override
	public boolean preventsKnockbackWhileBlocking(MaterialSet set, LivingEntity defender, ItemStack shield) {
		return GearExpansionConfig.get().fairyKeiSteadyShield;
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (set.isWeapon(stack) && config.fairyKeiFloatTicks > 0) {
			lines.add(Component.translatable("trait.gearexpansion.whimsy").withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (stack.is(set.shield.get()) && config.fairyKeiSteadyShield) {
			lines.add(Component.translatable("trait.gearexpansion.steady").withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get())) && config.fairyKeiFallReduction > 0) {
			lines.add(Component.translatable("trait.gearexpansion.cloud_step", config.fairyKeiFallReduction).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}
}
