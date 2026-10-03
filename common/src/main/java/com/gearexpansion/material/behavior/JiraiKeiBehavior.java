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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Jirai Kei: Desperation weapons hit harder the lower your health, and the Clingy shield heals you
 * a little when it blocks while you're low.
 */
public final class JiraiKeiBehavior implements GearBehavior {
	/** At most one shield heal a second. */
	private static final int SHIELD_HEAL_INTERVAL = 20;

	private final Map<UUID, Long> lastShieldHeal = new ConcurrentHashMap<>();

	@Override
	public float attackDamageBonus(MaterialSet set, ItemStack weapon, Entity victim, float damage, DamageSource source) {
		if (!set.isWeapon(weapon) || !(source.getEntity() instanceof LivingEntity attacker)) {
			return 0.0F;
		}
		return damage * GearExpansionConfig.get().jiraiKeiDesperation / 100.0F * missingHealth(attacker);
	}

	/** How much of their health {@code entity} has lost, from 0 (full) to 1 (none left). */
	public static float missingHealth(LivingEntity entity) {
		return Math.clamp(1.0F - entity.getHealth() / entity.getMaxHealth(), 0.0F, 1.0F);
	}

	/** Whether {@code entity} is below a third of their health. */
	public static boolean isLow(LivingEntity entity) {
		return entity.getHealth() < entity.getMaxHealth() / 3.0F;
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		int heal = GearExpansionConfig.get().jiraiKeiShieldHeal;
		if (heal <= 0 || !isLow(defender) || !(defender.level() instanceof ServerLevel level)) {
			return;
		}
		long now = level.getGameTime();
		Long last = lastShieldHeal.get(defender.getUUID());
		if (last != null && now - last < SHIELD_HEAL_INTERVAL) {
			return;
		}
		lastShieldHeal.put(defender.getUUID(), now);
		defender.heal(heal);
		level.sendParticles(ParticleTypes.HEART, defender.getX(), defender.getY() + defender.getBbHeight() + 0.2, defender.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.8F);
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.desperation", config.jiraiKeiDesperation).withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (stack.is(set.shield.get()) && config.jiraiKeiShieldHeal > 0) {
			lines.add(Component.translatable("trait.gearexpansion.clingy", config.jiraiKeiShieldHeal / 2.0F).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}
}
