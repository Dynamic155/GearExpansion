package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.network.GearHudPayload;

/**
 * Immovable: no knockback while sneaking, and less damage from explosions. The Ground Slam ability
 * stomps the ground, hurting, knocking back, and slowing nearby mobs.
 */
public final class TungstenSetBonus extends SetBonus {
	private static final double SLAM_RADIUS = 5.0;
	private static final int SLAM_SLOWNESS_TICKS = 60;

	private final Map<UUID, Long> readyAt = new ConcurrentHashMap<>();

	public TungstenSetBonus() {
		super(ModMaterials.TUNGSTEN);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().tungstenSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.tungsten.immovable"),
			Component.translatable("set_bonus.gearexpansion.tungsten.explosions", config.tungstenExplosionReduction),
			Component.translatable("set_bonus.gearexpansion.tungsten.slam", Component.keybind("key.gearexpansion.set_ability"), config.tungstenSlamCooldown)
		);
	}

	@Override
	public List<AttributeBonus> attributeBonuses(ServerPlayer player) {
		// Full knockback resistance only while sneaking; 0 removes it again.
		return List.of(new AttributeBonus(GearExpansion.id("set_bonus.tungsten.immovable"), Attributes.KNOCKBACK_RESISTANCE,
			player.isShiftKeyDown() ? 1.0 : 0.0, AttributeModifier.Operation.ADD_VALUE));
	}

	@Override
	public float modifyIncomingDamage(LivingEntity wearer, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			return damage * (1.0F - Mth.clamp(GearExpansionConfig.get().tungstenExplosionReduction, 0, 100) / 100.0F);
		}
		return damage;
	}

	@Override
	public boolean hasAbility() {
		return true;
	}

	@Override
	public void useAbility(ServerPlayer player) {
		ServerLevel level = player.level();
		GearExpansionConfig config = GearExpansionConfig.get();
		long now = level.getGameTime();
		long ready = readyAt.getOrDefault(player.getUUID(), 0L);
		if (now < ready) {
			player.sendSystemMessage(Component.translatable("ability.gearexpansion.cooldown", (ready - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		readyAt.put(player.getUUID(), now + config.tungstenSlamCooldown * 20L);

		DamageSource source = level.damageSources().playerAttack(player);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SLAM_RADIUS, 2.0, SLAM_RADIUS),
				entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player) && entity.distanceTo(player) <= SLAM_RADIUS)) {
			target.hurtServer(level, source, config.tungstenSlamDamage);
			target.knockback(1.2, player.getX() - target.getX(), player.getZ() - target.getZ(), source, config.tungstenSlamDamage);
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLAM_SLOWNESS_TICKS, 1), player);
		}
		for (int i = 0; i < 40; i++) {
			double angle = Math.PI * 2 * i / 40;
			level.sendParticles(ParticleTypes.CLOUD, player.getX() + Math.cos(angle) * SLAM_RADIUS * 0.6, player.getY() + 0.1,
				player.getZ() + Math.sin(angle) * SLAM_RADIUS * 0.6, 2, 0.3, 0.05, 0.3, 0.05);
		}
		level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0, 0, 0, 0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.2F, 0.8F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6F, 0.6F);
	}

	@Override
	public void fillHud(ServerPlayer player, GearHudPayload.Builder hud) {
		long left = readyAt.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime();
		hud.cooldown((int) Math.max(0, left), GearExpansionConfig.get().tungstenSlamCooldown * 20);
	}
}
