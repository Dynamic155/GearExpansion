package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Blastproof: explosions near the wearer don't break blocks (so creepers can't blow holes in your
 * base while you're home), and explosions hurt the wearer less.
 */
public final class ObsidianSetBonus extends SetBonus {
	public ObsidianSetBonus() {
		super(ModMaterials.OBSIDIAN);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().obsidianSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.obsidian.blocks", config.obsidianBlastproofRange),
			Component.translatable("set_bonus.gearexpansion.obsidian.damage", config.obsidianExplosionReduction));
	}

	@Override
	public float modifyIncomingDamage(LivingEntity wearer, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			return damage * (1.0F - Mth.clamp(GearExpansionConfig.get().obsidianExplosionReduction, 0, 100) / 100.0F);
		}
		return damage;
	}

	/** Whether a player wearing the full set is close enough to an explosion at {@code center} to stop it breaking blocks. */
	public boolean protectsBlocks(ServerLevel level, Vec3 center) {
		int range = GearExpansionConfig.get().obsidianBlastproofRange;
		if (range <= 0) {
			return false;
		}
		double rangeSquared = (double) range * range;
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(center) <= rangeSquared && isActive(player)) {
				return true;
			}
		}
		return false;
	}
}
