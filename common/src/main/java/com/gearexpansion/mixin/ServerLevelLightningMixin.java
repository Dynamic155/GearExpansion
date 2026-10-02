package com.gearexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gearexpansion.setbonus.SetBonuses;

/** Draws natural lightning to players wearing the full Verdigris set, like a lightning rod. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelLightningMixin {
	@ModifyReturnValue(method = "findLightningTargetAround", at = @At("RETURN"))
	private BlockPos gearexpansion$drawLightning(BlockPos target) {
		return SetBonuses.VERDIGRIS.redirectLightning((ServerLevel) (Object) this, target);
	}
}
