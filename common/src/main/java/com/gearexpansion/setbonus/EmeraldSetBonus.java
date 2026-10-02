package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Merchant's Favor: the wearer counts as a Hero of the Village (level I), so villagers trade at a
 * discount and sometimes toss gifts, as vanilla does for heroes. Stronger raid rewards still win.
 */
public final class EmeraldSetBonus extends SetBonus {
	public EmeraldSetBonus() {
		super(ModMaterials.EMERALD);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().emeraldSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(Component.translatable("set_bonus.gearexpansion.emerald.hero"));
	}

	@Override
	public void tick(ServerPlayer player) {
		if (player.tickCount % 20 != 0) {
			return;
		}
		MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
		// Keep a short level I effect topped up; don't touch a stronger one from a raid.
		if (hero == null || (hero.getAmplifier() == 0 && hero.getDuration() <= 40)) {
			player.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 100, 0, true, false, true));
		}
	}
}
