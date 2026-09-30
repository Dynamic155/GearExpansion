package com.gearexpansion.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.gearexpansion.setbonus.SetBonuses;

/** Lets set bonuses change potion effects as they're applied, e.g. Zinc shortening Poison. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectMixin {
	@ModifyVariable(
		method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
		at = @At("HEAD"),
		argsOnly = true
	)
	private MobEffectInstance gearexpansion$modifyNewEffect(MobEffectInstance effect) {
		return SetBonuses.modifyNewEffect(effect, (LivingEntity) (Object) this);
	}
}
