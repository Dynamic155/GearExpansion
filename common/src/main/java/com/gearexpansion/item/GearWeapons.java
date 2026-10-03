package com.gearexpansion.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * What every dagger and scythe does, whatever it's made of. Daggers Backstab: hitting a mob from
 * behind deals extra damage. Scythes sweep everything in a wide arc in front of you, and Reap:
 * breaking a grown crop harvests and replants the grown crops around it.
 */
public final class GearWeapons {
	public static final TagKey<Item> DAGGERS = TagKey.create(Registries.ITEM, GearExpansion.id("daggers"));
	public static final TagKey<Item> SCYTHES = TagKey.create(Registries.ITEM, GearExpansion.id("scythes"));

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

	/**
	 * A scythe's sweep, used instead of vanilla's: everything within reach in the half circle in front of
	 * the player takes part of the attack's damage, not just what stands next to the target.
	 *
	 * @param enchantedDamage applies the weapon's enchantments (Sharpness, Smite) to the damage for one mob
	 */
	public static void scytheSweep(Player player, Entity target, float baseDamage, DamageSource source, float attackStrengthScale, SweepDamage enchantedDamage) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 0.8F);
		float ratio = (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO);
		float share = GearExpansionConfig.get().scytheSweepDamage / 100.0F;
		// Sweeping Edge closes the gap between the sweep and a full hit.
		float sweepDamage = baseDamage * (share + (1.0F - share) * ratio);
		double reach = player.entityInteractionRange();
		Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot());

		for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach, 0.5, reach))) {
			if (nearby == player || nearby == target || player.isAlliedTo(nearby)
				|| nearby instanceof ArmorStand stand && stand.isMarker()
				|| nearby instanceof OwnableEntity pet && pet.getOwner() == player) {
				continue;
			}
			Vec3 offset = new Vec3(nearby.getX() - player.getX(), 0.0, nearby.getZ() - player.getZ());
			if (offset.horizontalDistanceSqr() > reach * reach || offset.dot(look) < 0.0) {
				continue;
			}
			float damage = enchantedDamage.apply(nearby, sweepDamage, source) * attackStrengthScale;
			if (damage > 0.0F && nearby.hurtServer(level, source, damage)) {
				nearby.knockback(0.4F, Mth.sin(player.getYRot() * Mth.DEG_TO_RAD), -Mth.cos(player.getYRot() * Mth.DEG_TO_RAD), source, damage);
				EnchantmentHelper.doPostAttackEffects(level, nearby, source);
			}
		}

		// Three sweep marks across the arc.
		for (float turn : new float[] {-50.0F, 0.0F, 50.0F}) {
			Vec3 dir = Vec3.directionFromRotation(0.0F, player.getYRot() + turn);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + dir.x * 1.6, player.getY(0.5), player.getZ() + dir.z * 1.6, 0, dir.x, 0.0, dir.z, 0.0);
		}
	}

	/** The enchanted damage a sweep deals to one mob (vanilla's {@code Player.getEnchantedDamage}). */
	@FunctionalInterface
	public interface SweepDamage {
		float apply(Entity entity, float damage, DamageSource source);
	}

	/**
	 * Reaping: when a scythe breaks a fully grown crop, it and the grown crops around it (3x3) are
	 * harvested and replanted. Returns true when it did, so the crop isn't broken as well.
	 */
	public static boolean reap(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos center, BlockState state) {
		if (!GearExpansionConfig.get().scytheReaping || !tool.is(SCYTHES) || player.isShiftKeyDown() || !isGrown(state)) {
			return false;
		}
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
			BlockState crop = level.getBlockState(pos);
			if (isGrown(crop)) {
				harvest(level, player, tool, pos.immutable(), crop);
			}
		}
		tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		player.causeFoodExhaustion(0.005F);
		return true;
	}

	private static void harvest(ServerLevel level, ServerPlayer player, ItemStack tool, BlockPos pos, BlockState state) {
		List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, player, tool));
		// One seed goes back in the ground.
		Item seed = state.getBlock().asItem();
		for (ItemStack drop : drops) {
			if (drop.is(seed)) {
				drop.shrink(1);
				break;
			}
		}
		List<ItemStack> finalDrops = ModMaterials.ofGear(tool)
			.map(set -> set.behavior.modifyDrops(set, tool, drops, level, state, pos))
			.orElse(drops);
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state));
		level.setBlockAndUpdate(pos, replanted(state));
		finalDrops.stream().filter(drop -> !drop.isEmpty()).forEach(drop -> Block.popResource(level, pos, drop));
	}

	/** Fully grown crops and nether wart. */
	public static boolean isGrown(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		return state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
	}

	private static BlockState replanted(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.getStateForAge(0);
		}
		return state.setValue(NetherWartBlock.AGE, 0);
	}
}
