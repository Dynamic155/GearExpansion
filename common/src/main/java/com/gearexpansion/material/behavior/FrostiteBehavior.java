package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Frostite: Frostbite weapons slow their targets and build up freezing, like powder snow; the Rime
 * shield slows melee attackers. The armor's cold immunity comes from the freeze-immune item tag,
 * and walking on powder snow from {@link com.gearexpansion.mixin.PowderSnowBlockMixin}.
 */
public final class FrostiteBehavior implements GearBehavior {
	private static final int SLOW_TICKS = 40;

	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		if (!set.isWeapon(weapon) || !target.isAlive()) {
			return;
		}
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 0), attacker);
		int freeze = GearExpansionConfig.get().frostiteFreezeTicks;
		if (freeze > 0 && target.canFreeze()) {
			// A little past fully frozen at most, so it thaws soon after the fight.
			int cap = target.getTicksRequiredToFreeze() + 40;
			target.setTicksFrozen(Math.min(cap, target.getTicksFrozen() + freeze));
		}
		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
		}
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		if (source.getDirectEntity() == attacker) {
			attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), defender);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.frostbite").withStyle(ChatFormatting.AQUA));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.rime").withStyle(ChatFormatting.AQUA));
		} else if (stack.is(set.boots.get())) {
			lines.add(Component.translatable("trait.gearexpansion.insulated").withStyle(ChatFormatting.AQUA));
			lines.add(Component.translatable("trait.gearexpansion.snow_walker").withStyle(ChatFormatting.AQUA));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.insulated").withStyle(ChatFormatting.AQUA));
		}
	}
}
