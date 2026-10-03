package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Royal Court: your tamed pets and iron golems nearby fight with Strength and Regeneration, and
 * villagers give you a bigger discount (see {@code PastelPrincessBehavior.villagerDiscount}).
 */
public final class PastelPrincessSetBonus extends SetBonus {
	private static final int CHECK_INTERVAL = 20;
	private static final int EFFECT_TICKS = 100;
	/** Effects are topped up when they have less than this left, so Regeneration still gets to heal. */
	private static final int REFRESH_BELOW = 30;

	public PastelPrincessSetBonus() {
		super(ModMaterials.PASTEL_PRINCESS);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().pastelPrincessSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.pastel_princess.court", config.pastelPrincessCourtRange),
			Component.translatable("set_bonus.gearexpansion.pastel_princess.discount", config.pastelPrincessCourtDiscount));
	}

	@Override
	public void tick(ServerPlayer player) {
		if (player.tickCount % CHECK_INTERVAL != 0) {
			return;
		}
		ServerLevel level = player.level();
		double range = GearExpansionConfig.get().pastelPrincessCourtRange;
		for (LivingEntity subject : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
				entity -> entity.isAlive() && isLoyal(entity, player))) {
			boolean refreshed = topUp(subject, MobEffects.STRENGTH, player);
			refreshed |= topUp(subject, MobEffects.REGENERATION, player);
			if (refreshed) {
				level.sendParticles(ParticleTypes.HEART, subject.getX(), subject.getY() + subject.getBbHeight() + 0.2, subject.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
			}
		}
	}

	/** Pets the player owns and iron golems: the Royal Court. */
	public static boolean isLoyal(LivingEntity entity, ServerPlayer player) {
		return entity instanceof OwnableEntity pet && pet.getOwner() == player || entity instanceof IronGolem;
	}

	private static boolean topUp(LivingEntity subject, Holder<MobEffect> effect, ServerPlayer player) {
		MobEffectInstance current = subject.getEffect(effect);
		if (current != null && (current.getAmplifier() > 0 || current.getDuration() >= REFRESH_BELOW)) {
			return false;
		}
		subject.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, true, true), player);
		return true;
	}
}
