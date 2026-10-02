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
import net.minecraft.world.entity.LivingEntity;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.network.GearHudPayload;

/**
 * Heat Core: the wearer can walk and swim in lava unharmed until the heat gauge fills, and it
 * cools down outside lava. Melee attackers catch fire. The Set Ability key triggers Eruption, a
 * ring of fire that grows stronger the more heat is stored.
 */
public final class InferniumSetBonus extends SetBonus {
	private final Map<UUID, Integer> heat = new ConcurrentHashMap<>();
	private final Map<UUID, Long> readyAt = new ConcurrentHashMap<>();

	public InferniumSetBonus() {
		super(ModMaterials.INFERNIUM);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().inferniumSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.infernium.lava", config.inferniumLavaSeconds),
			Component.translatable("set_bonus.gearexpansion.infernium.retaliate"),
			Component.translatable("set_bonus.gearexpansion.infernium.eruption", Component.keybind("key.gearexpansion.set_ability"), config.inferniumEruptionCooldown)
		);
	}

	private int maxHeat() {
		return GearExpansionConfig.get().inferniumLavaSeconds * 20;
	}

	private int heat(LivingEntity wearer) {
		return heat.getOrDefault(wearer.getUUID(), 0);
	}

	@Override
	public void tick(ServerPlayer player) {
		int current = heat(player);
		if (player.isInLava()) {
			heat.put(player.getUUID(), Math.min(maxHeat(), current + 1));
		} else {
			heat.put(player.getUUID(), Math.max(0, current - 2));
			// Out of the lava with heat to spare: the core keeps the flames off.
			if (current < maxHeat() && player.isOnFire()) {
				player.clearFire();
			}
		}
	}

	@Override
	public boolean cancelsDamage(LivingEntity wearer, ServerLevel level, DamageSource source, float damage) {
		return source.is(DamageTypeTags.IS_FIRE) && heat(wearer) < maxHeat();
	}

	@Override
	public float modifyIncomingDamage(LivingEntity wearer, DamageSource source, float damage) {
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && attacker != wearer) {
			attacker.igniteForSeconds(4.0F);
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
		long now = level.getGameTime();
		long ready = readyAt.getOrDefault(player.getUUID(), 0L);
		if (now < ready) {
			player.sendSystemMessage(Component.translatable("ability.gearexpansion.cooldown", (ready - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		readyAt.put(player.getUUID(), now + GearExpansionConfig.get().inferniumEruptionCooldown * 20L);

		// Stored heat makes the eruption bigger and hotter, and is used up.
		float power = maxHeat() == 0 ? 0.0F : Mth.clamp(heat(player) / (float) maxHeat(), 0.0F, 1.0F);
		heat.put(player.getUUID(), 0);
		double radius = 4.0 + 3.0 * power;
		float damage = 4.0F + 6.0F * power;
		DamageSource source = level.damageSources().playerAttack(player);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
				entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player) && entity.distanceTo(player) <= radius)) {
			target.igniteForSeconds(5.0F + 5.0F * power);
			target.hurtServer(level, source, damage);
		}
		for (int i = 0; i < 48; i++) {
			double angle = Math.PI * 2 * i / 48;
			level.sendParticles(ParticleTypes.FLAME, player.getX() + Math.cos(angle) * radius, player.getY() + 0.2,
				player.getZ() + Math.sin(angle) * radius, 3, 0.1, 0.2, 0.1, 0.02);
		}
		level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.5, 0.3, 0.5, 0.1);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.5F, 0.7F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
	}

	@Override
	public void fillHud(ServerPlayer player, GearHudPayload.Builder hud) {
		hud.heat(heat(player), maxHeat());
		long left = readyAt.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime();
		hud.cooldown((int) Math.max(0, left), GearExpansionConfig.get().inferniumEruptionCooldown * 20);
	}
}
