package com.gearexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.item.GearWeapons;

/**
 * Scythes sweep like swords, but in a wide arc in front of the player instead of around the target.
 * Also notes how charged each swing was, for weapons that only act on a full swing.
 *
 * <p>NeoForge changes vanilla's sweep: it asks the item for the sword-sweep ability instead of
 * checking the swords tag, and calls a version of {@code doSweepAttack} that takes the sweep area.
 * Each loader has its own pair of handlers, so they're optional ({@code require = 0}).
 */
@Mixin(Player.class)
public abstract class PlayerAttackMixin {
	@Shadow
	protected abstract float getEnchantedDamage(Entity entity, float dmg, DamageSource damageSource);

	@Inject(method = "attack", at = @At("HEAD"))
	private void gearexpansion$recordSwing(Entity target, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		GearWeapons.recordSwing(player, player.getAttackStrengthScale(0.5F));
	}

	// Fabric (vanilla): the item must be in #minecraft:swords.
	@ModifyExpressionValue(method = "isSweepAttack", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean gearexpansion$scythesSweep(boolean isSword) {
		return isSword || ((Player) (Object) this).getMainHandItem().is(GearWeapons.SCYTHES);
	}

	// NeoForge: the item must be able to perform the sword-sweep ability.
	@ModifyExpressionValue(method = "isSweepAttack", require = 0, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;canPerformAction(Lnet/neoforged/neoforge/common/ItemAbility;)Z"))
	private boolean gearexpansion$scythesSweepNeoForge(boolean canSweep) {
		return canSweep || ((Player) (Object) this).getMainHandItem().is(GearWeapons.SCYTHES);
	}

	@Inject(method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;F)V",
		require = 0, at = @At("HEAD"), cancellable = true)
	private void gearexpansion$wideSweep(Entity target, float baseDamage, DamageSource source, float attackStrengthScale, CallbackInfo ci) {
		gearexpansion$sweepWide(target, baseDamage, source, attackStrengthScale, ci);
	}

	@Inject(method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;FLnet/minecraft/world/phys/AABB;)V",
		require = 0, at = @At("HEAD"), cancellable = true)
	private void gearexpansion$wideSweepNeoForge(Entity target, float baseDamage, DamageSource source, float attackStrengthScale, AABB area,
			CallbackInfo ci) {
		gearexpansion$sweepWide(target, baseDamage, source, attackStrengthScale, ci);
	}

	private void gearexpansion$sweepWide(Entity target, float baseDamage, DamageSource source, float attackStrengthScale, CallbackInfo ci) {
		Player player = (Player) (Object) this;
		if (player.getMainHandItem().is(GearWeapons.SCYTHES)) {
			GearWeapons.scytheSweep(player, target, baseDamage, source, attackStrengthScale, this::getEnchantedDamage);
			ci.cancel();
		}
	}
}
