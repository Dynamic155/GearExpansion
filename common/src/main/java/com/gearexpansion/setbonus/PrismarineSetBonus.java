package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/** Tidebound: Conduit Power while in water or rain, as if a conduit were nearby. */
public final class PrismarineSetBonus extends SetBonus {
	private static final int CHECK_INTERVAL = 20;
	// A little longer than the check interval, so the effect doesn't flicker.
	private static final int EFFECT_TICKS = 50;

	public PrismarineSetBonus() {
		super(ModMaterials.PRISMARINE);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().prismarineSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(Component.translatable("set_bonus.gearexpansion.prismarine.tidebound"));
	}

	@Override
	public void tick(ServerPlayer player) {
		if (player.tickCount % CHECK_INTERVAL != 0 || !player.isInWaterOrRain()) {
			return;
		}
		MobEffectInstance current = player.getEffect(MobEffects.CONDUIT_POWER);
		// Don't shorten a real conduit's longer effect.
		if (current == null || current.getDuration() <= CHECK_INTERVAL + 5) {
			player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, EFFECT_TICKS, 0, true, true));
		}
	}
}
