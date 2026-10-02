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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.network.GearHudPayload;

/**
 * Clockwork: walking, fighting, and blocking wind up a spring. A fully wound spring gives Haste,
 * and the Set Ability key lets it go: a dash forward that knocks back mobs in front.
 */
public final class BrassSetBonus extends SetBonus {
	public static final int FULL = 100;
	private static final float PER_HIT = 6.0F;
	private static final float PER_BLOCK = 15.0F;

	private final Map<UUID, Float> spring = new ConcurrentHashMap<>();
	private final Map<UUID, Vec3> lastPosition = new ConcurrentHashMap<>();

	public BrassSetBonus() {
		super(ModMaterials.BRASS);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().brassSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(
			Component.translatable("set_bonus.gearexpansion.brass.wind"),
			Component.translatable("set_bonus.gearexpansion.brass.release", Component.keybind("key.gearexpansion.set_ability"))
		);
	}

	public float spring(LivingEntity wearer) {
		return spring.getOrDefault(wearer.getUUID(), 0.0F);
	}

	private void wind(LivingEntity wearer, float amount) {
		spring.merge(wearer.getUUID(), amount, (a, b) -> Math.min(FULL, a + b));
	}

	@Override
	public void tick(ServerPlayer player) {
		Vec3 now = player.position();
		Vec3 before = lastPosition.put(player.getUUID(), now);
		if (before != null && player.onGround() && now.subtract(before).horizontalDistanceSqr() > 0.0025) {
			wind(player, (float) FULL / (Math.max(1, GearExpansionConfig.get().brassWindUpSeconds) * 20));
		}
		if (spring(player) >= FULL && player.tickCount % 20 == 0) {
			player.addEffect(new MobEffectInstance(MobEffects.HASTE, 40, 0, true, false, true));
		}
	}

	@Override
	public void onAttack(LivingEntity wearer, LivingEntity target, DamageSource source, float damage) {
		wind(wearer, PER_HIT);
	}

	@Override
	public void onShieldBlock(LivingEntity wearer, LivingEntity attacker, DamageSource source, float damage) {
		wind(wearer, PER_BLOCK);
	}

	@Override
	public boolean hasAbility() {
		return true;
	}

	@Override
	public void useAbility(ServerPlayer player) {
		if (spring(player) < FULL) {
			player.sendSystemMessage(Component.translatable("ability.gearexpansion.brass.not_wound").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		spring.put(player.getUUID(), 0.0F);
		ServerLevel level = player.level();

		// Dash forward along the ground.
		Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
		player.setDeltaMovement(look.scale(1.4).add(0, 0.25, 0));
		player.needsSync = true;

		// Hit everything in front.
		float damage = GearExpansionConfig.get().brassReleaseDamage;
		DamageSource source = level.damageSources().playerAttack(player);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0),
				entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player))) {
			Vec3 toTarget = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
			if (toTarget.dot(look) > 0.4) {
				target.hurtServer(level, source, damage);
				target.knockback(1.5, -look.x, -look.z, source, damage);
			}
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 0.7F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
		level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.8, 0.4, 0.8, 0.2);
	}

	@Override
	public void fillHud(ServerPlayer player, GearHudPayload.Builder hud) {
		hud.spring(Math.round(spring(player)), FULL);
	}
}
