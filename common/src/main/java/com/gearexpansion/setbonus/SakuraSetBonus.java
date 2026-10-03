package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Hanami: gentle Regeneration while standing among flowers (including cherry leaves and pink
 * petals), and a trail of falling cherry petals while you walk.
 */
public final class SakuraSetBonus extends SetBonus {
	private static final int CHECK_INTERVAL = 20;
	// A little longer than the check interval, so Regeneration doesn't flicker on and off.
	private static final int REGENERATION_TICKS = 50;
	private static final int TRAIL_INTERVAL = 3;

	public SakuraSetBonus() {
		super(ModMaterials.SAKURA);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().sakuraSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(
			Component.translatable("set_bonus.gearexpansion.sakura.hanami", GearExpansionConfig.get().sakuraFlowerRange),
			Component.translatable("set_bonus.gearexpansion.sakura.trail"));
	}

	@Override
	public void tick(ServerPlayer player) {
		ServerLevel level = player.level();
		GearExpansionConfig config = GearExpansionConfig.get();

		if (config.sakuraPetalTrail && player.tickCount % TRAIL_INTERVAL == 0 && player.onGround()
				&& player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4) {
			level.sendParticles(ParticleTypes.CHERRY_LEAVES, player.getX(), player.getY() + 1.2, player.getZ(), 1, 0.3, 0.3, 0.3, 0.0);
		}

		if (player.tickCount % CHECK_INTERVAL == 0 && nearFlowers(level, player.blockPosition(), config.sakuraFlowerRange)) {
			MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
			if (regeneration == null || (regeneration.getAmplifier() == 0 && regeneration.getDuration() <= CHECK_INTERVAL + 5)) {
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, 0, true, true));
			}
		}
	}

	/** Whether a flower, cherry leaves, or pink petals are within {@code range} blocks. */
	public static boolean nearFlowers(ServerLevel level, BlockPos center, int range) {
		if (range <= 0) {
			return false;
		}
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -1, -range), center.offset(range, 2, range))) {
			var state = level.getBlockState(pos);
			if (state.is(BlockTags.FLOWERS)) {
				return true;
			}
		}
		return false;
	}
}
