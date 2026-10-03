package com.gearexpansion.material.behavior;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Sakura: Blossoming weapons heal you a little when they finish off a mob, and the Petal Guard
 * shield heals you a little when it blocks (at most once a second). Both burst into petals.
 */
public final class SakuraBehavior implements GearBehavior {
	private static final int SHIELD_HEAL_COOLDOWN_TICKS = 20;

	private final Map<UUID, Long> lastShieldHeal = new ConcurrentHashMap<>();

	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		int heal = GearExpansionConfig.get().sakuraKillHeal;
		if (set.isWeapon(weapon) && target.isDeadOrDying() && heal > 0) {
			attacker.heal(heal);
			petals(attacker, 12);
		}
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		int heal = GearExpansionConfig.get().sakuraBlockHeal;
		if (heal <= 0 || !(defender.level() instanceof ServerLevel level)) {
			return;
		}
		long now = level.getGameTime();
		Long last = lastShieldHeal.get(defender.getUUID());
		if (last == null || now - last >= SHIELD_HEAL_COOLDOWN_TICKS) {
			lastShieldHeal.put(defender.getUUID(), now);
			defender.heal(heal);
			petals(defender, 8);
		}
	}

	/** A small burst of cherry petals and a soft rustle around {@code entity}. */
	static void petals(LivingEntity entity, int count) {
		if (entity.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CHERRY_LEAVES, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(),
				count, 0.4, 0.4, 0.4, 0.02);
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 0.6F, 1.3F);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.blossoming", config.sakuraKillHeal / 2.0F).withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.petal_guard", config.sakuraBlockHeal / 2.0F).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}
}
