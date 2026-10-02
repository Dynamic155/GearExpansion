package com.gearexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gearexpansion.setbonus.SetBonuses;

/** Lets set bonuses change how much experience an orb gives, e.g. Rose Gold's Lucky Charm. */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
	@ModifyExpressionValue(method = "playerTouch", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;getValue()I"))
	private int gearexpansion$modifyExperience(int amount, @Local(argsOnly = true) Player player) {
		return SetBonuses.modifyExperience(amount, player);
	}
}
