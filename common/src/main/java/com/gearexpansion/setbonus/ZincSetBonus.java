package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/** Galvanized: Poison, Hunger, and Nausea wear off faster. */
public final class ZincSetBonus extends SetBonus {
	private static final List<Holder<MobEffect>> RESISTED = List.of(MobEffects.POISON, MobEffects.HUNGER, MobEffects.NAUSEA);

	public ZincSetBonus() {
		super(ModMaterials.ZINC);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().zincSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(Component.translatable("set_bonus.gearexpansion.zinc.effects", GearExpansionConfig.get().zincEffectReduction));
	}

	@Override
	public MobEffectInstance modifyNewEffect(MobEffectInstance effect, LivingEntity wearer) {
		if (effect.isInfiniteDuration() || RESISTED.stream().noneMatch(effect::is)) {
			return effect;
		}
		float keep = 1.0F - Mth.clamp(GearExpansionConfig.get().zincEffectReduction, 0, 100) / 100.0F;
		int duration = Math.max(1, Math.round(effect.getDuration() * keep));
		return new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon());
	}
}
