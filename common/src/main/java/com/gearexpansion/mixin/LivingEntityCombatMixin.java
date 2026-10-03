package com.gearexpansion.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gearexpansion.item.GearCombat;

/**
 * Lets gear change combat: set bonuses can stop a hit (Amethyst's shell), armor can reduce damage
 * (Infernium against fire), and shields can react to blocking. Players reach these through
 * {@code super} calls, so this covers players and mobs alike.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCombatMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void gearexpansion$cancelDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
		if (GearCombat.cancelsDamage((LivingEntity) (Object) this, level, source, damage)) {
			cir.setReturnValue(false);
		}
	}

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float gearexpansion$modifyDamage(float damage, ServerLevel level, DamageSource source) {
		return GearCombat.modifyIncomingDamage((LivingEntity) (Object) this, source, damage);
	}

	@Inject(method = "applyItemBlocking", at = @At("RETURN"))
	private void gearexpansion$onProjectileBlocked(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		if (cir.getReturnValue() > 0.0F && source.getDirectEntity() instanceof Projectile projectile) {
			GearCombat.onProjectileBlocked((LivingEntity) (Object) this, projectile);
		}
	}

	@Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), cancellable = true)
	private void gearexpansion$steadyShield(double power, double xd, double zd, DamageSource source, float damage, boolean comesFromEffect, CallbackInfo ci) {
		if (GearCombat.preventsKnockback((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}

	@Inject(method = "blockUsingItem", at = @At("HEAD"))
	private void gearexpansion$onShieldBlock(ServerLevel level, LivingEntity attacker, DamageSource source, float damage, boolean fullyBlocked, CallbackInfo ci) {
		GearCombat.onShieldBlock((LivingEntity) (Object) this, attacker, source, damage);
	}
}
