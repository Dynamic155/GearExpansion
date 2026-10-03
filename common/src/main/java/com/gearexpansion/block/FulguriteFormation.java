package com.gearexpansion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.registry.ModBlocks;

/**
 * Lightning fuses sand into Fulgurite, like real fulgurites. A strike turns the sand it hits into
 * Fulgurite, and some of the sand around and below it. It works with natural lightning, lightning
 * rods standing on sand, and Channeling tridents.
 */
public final class FulguriteFormation {
	private FulguriteFormation() {
	}

	/** Called when lightning strikes {@code struck} (the block under the bolt, or the lightning rod it hit). */
	public static void onStrike(Level level, BlockPos struck) {
		int chance = GearExpansionConfig.get().fulguriteFormChance;
		if (!(level instanceof ServerLevel server) || chance <= 0) {
			return;
		}
		RandomSource random = server.getRandom();
		boolean formed = false;
		// The struck block and everything within one block around and two below it.
		for (BlockPos pos : BlockPos.betweenClosed(struck.offset(-1, -2, -1), struck.offset(1, 0, 1))) {
			if (!server.getBlockState(pos).is(BlockTags.SAND)) {
				continue;
			}
			boolean center = pos.getX() == struck.getX() && pos.getZ() == struck.getZ();
			if (center || random.nextInt(100) < chance) {
				server.setBlockAndUpdate(pos.immutable(), ModBlocks.FULGURITE.get().defaultBlockState());
				formed = true;
			}
		}
		if (formed) {
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, struck.getX() + 0.5, struck.getY() + 0.5, struck.getZ() + 0.5, 20, 0.6, 0.4, 0.6, 0.2);
		}
	}
}
