package com.gearexpansion.material.behavior;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.item.GearWeapons;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Magical Girl: at full health, a fully charged swing also fires a Sparkle Beam that hurts every mob
 * in a short line ahead. The Barrier shield bounces blocked projectiles back at whoever shot them.
 */
public final class MagicalGirlBehavior implements GearBehavior {
	private static final double BEAM_LENGTH = 8.0;
	private static final double BEAM_WIDTH = 0.9;

	/** Projectiles the shield blocked this tick. They're sent back at the end of the tick, after vanilla's own bounce. */
	private static final Queue<Reflection> REFLECTIONS = new ConcurrentLinkedQueue<>();

	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		if (!set.isWeapon(weapon) || !(attacker instanceof Player player) || !(player.level() instanceof ServerLevel level)
				|| player.getHealth() < player.getMaxHealth() || !GearWeapons.isFullSwing(player)) {
			return;
		}
		sparkleBeam(level, player, target);
	}

	/** A short beam of light from the player's eyes, hurting every mob it passes through except the one already hit. */
	private static void sparkleBeam(ServerLevel level, Player player, @Nullable Entity alreadyHit) {
		float damage = GearExpansionConfig.get().magicalGirlBeamDamage;
		Vec3 start = player.getEyePosition();
		Vec3 direction = player.getLookAngle();
		Vec3 end = start.add(direction.scale(BEAM_LENGTH));
		DamageSource source = level.damageSources().playerAttack(player);
		if (damage > 0) {
			for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(BEAM_WIDTH),
					entity -> entity != player && entity != alreadyHit && entity.isAlive() && !entity.isAlliedTo(player))) {
				if (distanceToLine(entity.getBoundingBox().getCenter(), start, direction) <= BEAM_WIDTH + entity.getBbWidth() / 2) {
					entity.hurtServer(level, source, damage);
				}
			}
		}
		for (double d = 1.0; d <= BEAM_LENGTH; d += 0.5) {
			Vec3 point = start.add(direction.scale(d));
			level.sendParticles(ParticleTypes.END_ROD, point.x, point.y - 0.2, point.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
	}

	/** Distance from {@code point} to the ray from {@code start} along the unit vector {@code direction}. */
	private static double distanceToLine(Vec3 point, Vec3 start, Vec3 direction) {
		Vec3 offset = point.subtract(start);
		double along = Math.max(0.0, offset.dot(direction));
		return offset.subtract(direction.scale(along)).length();
	}

	@Override
	public void onProjectileBlocked(MaterialSet set, LivingEntity defender, ItemStack shield, Projectile projectile) {
		if (GearExpansionConfig.get().magicalGirlBarrier) {
			REFLECTIONS.add(new Reflection(projectile, defender, projectile.getOwner(), projectile.getDeltaMovement().length()));
		}
	}

	/** Sends the projectiles blocked this tick back at whoever shot them. Called at the end of every server tick. */
	public static void reflectPending() {
		Reflection next;
		while ((next = REFLECTIONS.poll()) != null) {
			Reflection reflection = next;
			Projectile projectile = reflection.projectile();
			if (projectile.isRemoved() || !(projectile.level() instanceof ServerLevel level)) {
				continue;
			}
			Vec3 direction = reflection.shooter() != null && reflection.shooter().isAlive()
				? reflection.shooter().getEyePosition().subtract(projectile.position()).normalize()
				: projectile.position().subtract(reflection.defender().position()).multiply(1.0, 0.0, 1.0).normalize();
			projectile.deflect((p, entity, random, power) -> p.setDeltaMovement(direction.scale(Math.max(reflection.speed(), 1.0))),
				reflection.defender(), EntityReference.of(reflection.defender()), false, 1.0);
			projectile.needsSync = true;
			level.sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
			level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 1.5F);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (set.isWeapon(stack) && config.magicalGirlBeamDamage > 0) {
			lines.add(Component.translatable("trait.gearexpansion.sparkle_beam", config.magicalGirlBeamDamage).withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (stack.is(set.shield.get()) && config.magicalGirlBarrier) {
			lines.add(Component.translatable("trait.gearexpansion.barrier").withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	private record Reflection(Projectile projectile, LivingEntity defender, @Nullable Entity shooter, double speed) {
	}
}
