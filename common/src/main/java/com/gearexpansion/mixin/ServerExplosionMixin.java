package com.gearexpansion.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gearexpansion.setbonus.SetBonuses;

/**
 * The Obsidian set's Blastproof bonus: an explosion near a wearer breaks no blocks. Emptying the
 * exploded positions also stops it starting fires; entities are still hurt as normal.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@ModifyReturnValue(method = "calculateExplodedPositions", at = @At("RETURN"))
	private List<BlockPos> gearexpansion$protectBlocks(List<BlockPos> positions) {
		ServerExplosion explosion = (ServerExplosion) (Object) this;
		ServerLevel level = explosion.level();
		if (!positions.isEmpty() && SetBonuses.OBSIDIAN.protectsBlocks(level, explosion.center())) {
			return List.of();
		}
		return positions;
	}
}
