package com.gearexpansion.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.item.GearWeapons;

/** Notes how charged each swing was, before the attack resets it, for weapons that only act on a full swing. */
@Mixin(Player.class)
public abstract class PlayerAttackMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void gearexpansion$recordSwing(Entity target, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		GearWeapons.recordSwing(player, player.getAttackStrengthScale(0.5F));
	}
}
