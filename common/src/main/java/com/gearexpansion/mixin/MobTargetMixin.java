package com.gearexpansion.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.item.GearCombat;
import com.gearexpansion.material.ModMaterials;

/** Bees never target anyone wearing a piece of Verdantite armor, even after their hive is disturbed. */
@Mixin(Mob.class)
public abstract class MobTargetMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void gearexpansion$beeFriend(LivingEntity target, CallbackInfo ci) {
		if ((Object) this instanceof Bee && target != null && GearExpansionConfig.get().verdantiteBeeFriend
				&& GearCombat.piecesWorn(target, ModMaterials.VERDANTITE) > 0) {
			ci.cancel();
		}
	}
}
