package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Conductive: lightning nearby is drawn to the wearer like a lightning rod. They take no harm from
 * it, and a strike charges them with Speed and Strength for 10 seconds (on a cooldown).
 */
public final class VerdigrisSetBonus extends SetBonus {
	private final Map<UUID, Long> chargedUntil = new ConcurrentHashMap<>();

	public VerdigrisSetBonus() {
		super(ModMaterials.VERDIGRIS);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().verdigrisSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.verdigris.rod", config.verdigrisLightningRange),
			Component.translatable("set_bonus.gearexpansion.verdigris.charge", config.verdigrisChargeCooldown)
		);
	}

	@Override
	public boolean cancelsDamage(LivingEntity wearer, ServerLevel level, DamageSource source, float damage) {
		if (!source.is(DamageTypes.LIGHTNING_BOLT)) {
			return false;
		}
		wearer.clearFire();
		long now = level.getGameTime();
		if (now >= chargedUntil.getOrDefault(wearer.getUUID(), 0L)) {
			chargedUntil.put(wearer.getUUID(), now + GearExpansionConfig.get().verdigrisChargeCooldown * 20L);
			wearer.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
			wearer.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0));
			if (wearer instanceof ServerPlayer player) {
				player.sendSystemMessage(Component.translatable("set_bonus.gearexpansion.verdigris.charged").withStyle(ChatFormatting.AQUA), true);
			}
		}
		return true;
	}

	/**
	 * Where natural lightning should strike instead of {@code target}: the nearest player wearing the
	 * full set within range, under open sky. Called from {@code ServerLevelLightningMixin}.
	 */
	public BlockPos redirectLightning(ServerLevel level, BlockPos target) {
		int range = GearExpansionConfig.get().verdigrisLightningRange;
		if (range <= 0) {
			return target;
		}
		ServerPlayer closest = null;
		double closestDistance = (double) range * range;
		for (ServerPlayer player : level.players()) {
			double distance = player.blockPosition().distSqr(target);
			if (distance <= closestDistance && isActive(player) && level.canSeeSky(player.blockPosition())) {
				closest = player;
				closestDistance = distance;
			}
		}
		return closest == null ? target : closest.blockPosition();
	}
}
