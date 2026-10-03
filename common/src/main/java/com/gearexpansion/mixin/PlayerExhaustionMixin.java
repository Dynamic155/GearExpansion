package com.gearexpansion.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.setbonus.SetBonuses;

/** The Verdantite set's Overgrowth bonus: working the fields costs no hunger. */
@Mixin(Player.class)
public abstract class PlayerExhaustionMixin {
	@Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
	private void gearexpansion$tendingFields(float exhaustion, CallbackInfo ci) {
		if (SetBonuses.VERDANTITE.isTending((Player) (Object) this)) {
			ci.cancel();
		}
	}
}
