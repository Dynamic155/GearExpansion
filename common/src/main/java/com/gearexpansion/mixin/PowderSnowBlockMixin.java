package com.gearexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gearexpansion.material.ModMaterials;

/** Frostite boots let their wearer walk on powder snow, like leather boots. */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
	@ModifyReturnValue(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"))
	private static boolean gearexpansion$frostiteBoots(boolean canWalk, Entity entity) {
		return canWalk || entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.FEET).is(ModMaterials.FROSTITE.boots.get());
	}
}
