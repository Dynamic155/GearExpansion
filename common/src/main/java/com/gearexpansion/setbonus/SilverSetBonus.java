package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/** Blessed: Wither wears off faster, and undead nearby glow so they can't sneak up on you. */
public final class SilverSetBonus extends SetBonus {
	// Glowing lasts a little longer than the check interval, so it doesn't flicker.
	private static final int CHECK_INTERVAL = 20;
	private static final int GLOW_TICKS = 30;

	public SilverSetBonus() {
		super(ModMaterials.SILVER);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().silverSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.silver.wither", config.silverWitherReduction),
			Component.translatable("set_bonus.gearexpansion.silver.sense", config.silverUndeadSenseRange));
	}

	@Override
	public void tick(ServerPlayer player) {
		int range = GearExpansionConfig.get().silverUndeadSenseRange;
		if (range <= 0 || player.tickCount % CHECK_INTERVAL != 0) {
			return;
		}
		for (LivingEntity undead : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
				entity -> entity.is(EntityTypeTags.UNDEAD) && entity.isAlive() && entity.distanceToSqr(player) <= range * range)) {
			undead.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, true, false), player);
		}
	}

	@Override
	public MobEffectInstance modifyNewEffect(MobEffectInstance effect, LivingEntity wearer) {
		if (effect.isInfiniteDuration() || !effect.is(MobEffects.WITHER)) {
			return effect;
		}
		float keep = 1.0F - Mth.clamp(GearExpansionConfig.get().silverWitherReduction, 0, 100) / 100.0F;
		int duration = Math.max(1, Math.round(effect.getDuration() * keep));
		return new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon());
	}
}
