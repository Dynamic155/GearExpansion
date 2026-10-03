package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/** Stormcaller: Speed and Strength while out in the rain, and faster still in a thunderstorm. */
public final class FulguriteSetBonus extends SetBonus {
	private static final int CHECK_INTERVAL = 20;
	// A little longer than the check interval, so the effects don't flicker.
	private static final int EFFECT_TICKS = 50;

	public FulguriteSetBonus() {
		super(ModMaterials.FULGURITE);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().fulguriteSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(Component.translatable("set_bonus.gearexpansion.fulgurite.storm"));
	}

	@Override
	public void tick(ServerPlayer player) {
		ServerLevel level = player.level();
		if (player.tickCount % CHECK_INTERVAL != 0 || !level.isRainingAt(player.blockPosition().above())) {
			return;
		}
		int speed = level.isThundering() ? 1 : 0;
		refresh(player, new MobEffectInstance(MobEffects.SPEED, EFFECT_TICKS, speed, true, true));
		refresh(player, new MobEffectInstance(MobEffects.STRENGTH, EFFECT_TICKS, 0, true, true));
	}

	/** Tops up a short effect without shortening a stronger or longer one from a potion. */
	private static void refresh(ServerPlayer player, MobEffectInstance effect) {
		MobEffectInstance current = player.getEffect(effect.getEffect());
		if (current == null || (current.getAmplifier() <= effect.getAmplifier() && current.getDuration() <= CHECK_INTERVAL + 5)) {
			player.addEffect(effect);
		}
	}
}
