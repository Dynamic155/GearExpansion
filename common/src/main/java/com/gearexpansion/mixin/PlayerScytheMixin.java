package com.gearexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.item.GearWeapons;

/** Scythes sweep like swords, but in a wide arc in front of the player instead of around the target. */
@Mixin(Player.class)
public abstract class PlayerScytheMixin {
	@Shadow
	protected abstract float getEnchantedDamage(Entity entity, float dmg, DamageSource damageSource);

	@ModifyExpressionValue(method = "isSweepAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean gearexpansion$scythesSweep(boolean isSword) {
		return isSword || ((Player) (Object) this).getMainHandItem().is(GearWeapons.SCYTHES);
	}

	@Inject(method = "doSweepAttack", at = @At("HEAD"), cancellable = true)
	private void gearexpansion$wideSweep(Entity target, float baseDamage, DamageSource source, float attackStrengthScale, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (player.getMainHandItem().is(GearWeapons.SCYTHES)) {
			GearWeapons.scytheSweep(player, target, baseDamage, source, attackStrengthScale, this::getEnchantedDamage);
			ci.cancel();
		}
	}
}
