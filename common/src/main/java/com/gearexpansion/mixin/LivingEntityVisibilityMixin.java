package com.gearexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gearexpansion.setbonus.SetBonuses;

/**
 * The Echo set's Silence bonus: hostile mobs see the wearer as less visible, which shortens the
 * range they notice them from. Mob heads use the same mechanism.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityVisibilityMixin {
	@ModifyReturnValue(method = "getVisibilityPercent", at = @At("RETURN"))
	private double gearexpansion$hide(double visibility, ServerLevel level, Entity lookingEntity) {
		if ((Object) this instanceof Player player && lookingEntity instanceof Enemy) {
			return SetBonuses.ECHO.modifyVisibility(player, visibility);
		}
		return visibility;
	}
}
