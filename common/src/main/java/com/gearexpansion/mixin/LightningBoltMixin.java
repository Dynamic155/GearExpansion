package com.gearexpansion.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.block.FulguriteFormation;

/**
 * Lightning fuses sand into Fulgurite. Hooked where vanilla scrapes oxidation off copper, which
 * runs once per real strike (never for visual-only bolts) with the block that was struck.
 */
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {
	@Inject(method = "clearCopperOnLightningStrike", at = @At("HEAD"))
	private static void gearexpansion$formFulgurite(Level level, BlockPos struck, CallbackInfo ci) {
		FulguriteFormation.onStrike(level, struck);
	}
}
