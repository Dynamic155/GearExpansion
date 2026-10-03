package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.network.GearHudPayload;

/**
 * Landmine: the first hit that drops the wearer below a third of their health sets off a burst of
 * pink hearts that hurts and throws back every mob nearby. It breaks no blocks, and it has a cooldown.
 */
public final class JiraiKeiSetBonus extends SetBonus {
	private static final double RADIUS = 4.0;
	private static final int PINK = 0xFF6EB4;

	private final Map<UUID, Long> readyAt = new ConcurrentHashMap<>();

	public JiraiKeiSetBonus() {
		super(ModMaterials.JIRAI_KEI);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().jiraiKeiSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.jirai_kei.landmine", config.jiraiKeiLandmineDamage),
			Component.translatable("set_bonus.gearexpansion.jirai_kei.cooldown", config.jiraiKeiLandmineCooldown));
	}

	@Override
	public float modifyIncomingDamage(LivingEntity wearer, DamageSource source, float damage) {
		if (damage > 0.0F && wearer.getHealth() >= wearer.getMaxHealth() / 3.0F && wearer.getHealth() - damage < wearer.getMaxHealth() / 3.0F
				&& wearer.level() instanceof ServerLevel level) {
			long now = level.getGameTime();
			if (now >= readyAt.getOrDefault(wearer.getUUID(), 0L)) {
				readyAt.put(wearer.getUUID(), now + GearExpansionConfig.get().jiraiKeiLandmineCooldown * 20L);
				detonate(level, wearer);
			}
		}
		return damage;
	}

	/** The heart burst: hurts and throws back every mob within reach, but never the wearer, allies, or their pets. */
	public static void detonate(ServerLevel level, LivingEntity wearer) {
		float damage = GearExpansionConfig.get().jiraiKeiLandmineDamage;
		DamageSource source = wearer instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(wearer);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, wearer.getBoundingBox().inflate(RADIUS),
				entity -> entity != wearer && entity.isAlive() && !entity.isAlliedTo(wearer) && entity.distanceTo(wearer) <= RADIUS
					&& !(entity instanceof OwnableEntity pet && pet.getOwner() == wearer))) {
			if (damage > 0) {
				target.hurtServer(level, source, damage);
			}
			target.knockback(1.5, wearer.getX() - target.getX(), wearer.getZ() - target.getZ(), source, damage);
		}
		double y = wearer.getY(0.5);
		level.sendParticles(ParticleTypes.EXPLOSION, wearer.getX(), y, wearer.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.sendParticles(ParticleTypes.HEART, wearer.getX(), y, wearer.getZ(), 20, RADIUS / 2, 0.6, RADIUS / 2, 0.0);
		level.sendParticles(new DustParticleOptions(PINK, 1.5F), wearer.getX(), y, wearer.getZ(), 60, RADIUS / 2, 0.8, RADIUS / 2, 0.0);
		level.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.5F);
		level.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
	}

	@Override
	public void fillHud(ServerPlayer player, GearHudPayload.Builder hud) {
		long left = readyAt.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime();
		hud.cooldown((int) Math.max(0, left), GearExpansionConfig.get().jiraiKeiLandmineCooldown * 20);
	}
}
