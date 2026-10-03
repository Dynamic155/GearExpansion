package com.gearexpansion.item;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;

/**
 * What every dagger does, whatever it's made of: Backstab, extra damage when hitting a mob from
 * behind. Also notes how charged each swing is, for weapons that only act on a full swing.
 */
public final class GearWeapons {
	public static final TagKey<Item> DAGGERS = TagKey.create(Registries.ITEM, GearExpansion.id("daggers"));

	/** A hit counts as from behind when the attacker is more than 120 degrees from where the target faces. */
	private static final double BEHIND_COSINE = -0.5;

	/** How charged each player's current swing is (1 is a full swing), noted as the attack starts. Server side only. */
	private static final Map<UUID, Float> SWING_STRENGTH = new ConcurrentHashMap<>();

	private GearWeapons() {
	}

	/** Notes how charged a player's swing is, before the attack resets the charge. */
	public static void recordSwing(Player player, float strength) {
		if (!player.level().isClientSide()) {
			SWING_STRENGTH.put(player.getUUID(), strength);
		}
	}

	/** Whether the player's current attack is a fully charged swing. */
	public static boolean isFullSwing(Player player) {
		return SWING_STRENGTH.getOrDefault(player.getUUID(), 0.0F) > 0.9F;
	}

	/** Backstab: extra damage when a dagger hits a living target from behind. */
	public static float backstabBonus(ItemStack weapon, Entity victim, float damage, @Nullable Entity attacker) {
		int percent = GearExpansionConfig.get().daggerBackstabBonus;
		if (percent <= 0 || !weapon.is(DAGGERS) || !(victim instanceof LivingEntity target) || attacker == null || !isBehind(attacker, target)) {
			return 0.0F;
		}
		if (target.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.4F);
		}
		return damage * percent / 100.0F;
	}

	/** Whether {@code attacker} stands behind {@code target}, judged by which way the target's body faces. */
	public static boolean isBehind(Entity attacker, LivingEntity target) {
		Vec3 facing = Vec3.directionFromRotation(0.0F, target.yBodyRot);
		Vec3 toAttacker = new Vec3(attacker.getX() - target.getX(), 0.0, attacker.getZ() - target.getZ());
		if (toAttacker.lengthSqr() < 1.0E-4) {
			return false;
		}
		return facing.dot(toAttacker.normalize()) < BEHIND_COSINE;
	}
}
