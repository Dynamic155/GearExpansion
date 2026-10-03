package com.gearexpansion.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.setbonus.SetBonuses;

/**
 * Echo gear's silence: drops game events (the vibrations sculk sensors, shriekers, and wardens
 * hear) caused by a player mining with an echo tool, stepping in echo boots, or sneaking in the full set.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelVibrationMixin {
	@Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
		at = @At("HEAD"), cancellable = true)
	private void gearexpansion$muffle(Holder<GameEvent> event, Vec3 position, GameEvent.Context context, CallbackInfo ci) {
		if (context.sourceEntity() != null && SetBonuses.ECHO.muffles(context.sourceEntity(), event)) {
			ci.cancel();
		}
	}
}
