package com.gearexpansion.material.behavior;

import java.util.Comparator;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Fulgurite: Chain Lightning crits arc to nearby mobs in the rain (two in a thunderstorm), the
 * Grounded armor shrugs off lightning, and the Static shield shocks melee attackers.
 */
public final class FulguriteBehavior implements GearBehavior {
	private static final double CHAIN_RANGE = 6.0;
	private static final float LIGHTNING_PROTECTION_PER_PIECE = 0.25F;

	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		// A falling attack is a critical hit, the same test vanilla uses (minus sprinting and blindness).
		boolean critical = attacker.fallDistance > 0.0F && !attacker.onGround() && !attacker.isInWater() && !attacker.isPassenger();
		if (!set.isWeapon(weapon) || !critical || !(target.level() instanceof ServerLevel level)) {
			return;
		}
		int chains = level.isThundering() ? 2 : level.isRainingAt(attacker.blockPosition()) ? 1 : 0;
		float damage = GearExpansionConfig.get().fulguriteChainDamage;
		if (chains == 0 || damage <= 0) {
			return;
		}
		DamageSource source = level.damageSources().lightningBolt();
		level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(CHAIN_RANGE),
				entity -> entity != target && entity != attacker && entity.isAlive() && !entity.isAlliedTo(attacker))
			.stream()
			.sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(target)))
			.limit(chains)
			.forEach(next -> {
				// A harmless bolt for the look and sound; the damage is dealt here so the wielder isn't hit.
				LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
				if (bolt != null) {
					bolt.setPos(next.getX(), next.getY(), next.getZ());
					bolt.setVisualOnly(true);
					level.addFreshEntity(bolt);
				}
				next.hurtServer(level, source, damage);
			});
	}

	@Override
	public float modifyWearerDamage(MaterialSet set, LivingEntity wearer, DamageSource source, float damage, int pieces) {
		if (source.is(DamageTypeTags.IS_LIGHTNING)) {
			return damage * (1.0F - Mth.clamp(LIGHTNING_PROTECTION_PER_PIECE * pieces, 0.0F, 1.0F));
		}
		return damage;
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		int shock = GearExpansionConfig.get().fulguriteShieldShock;
		if (shock > 0 && source.getDirectEntity() == attacker && defender.level() instanceof ServerLevel level) {
			// Twice as strong in a thunderstorm.
			int amount = level.isThundering() ? shock * 2 : shock;
			attacker.hurtServer(level, level.damageSources().lightningBolt(), amount);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(),
				12, 0.3, 0.4, 0.3, 0.1);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.chain_lightning", config.fulguriteChainDamage).withStyle(ChatFormatting.YELLOW));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.static", config.fulguriteShieldShock).withStyle(ChatFormatting.YELLOW));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.grounded", Math.round(LIGHTNING_PROTECTION_PER_PIECE * 100)).withStyle(ChatFormatting.YELLOW));
		}
	}
}
