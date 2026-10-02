package com.gearexpansion.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.setbonus.EnchantingBonus;

/**
 * Remembers which player opened an enchanting table, so their set bonus can add bookshelves
 * while the table works out its offers (see {@link EnchantmentHelperMixin}).
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
	@Unique
	private Player gearexpansion$player;

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
	private void gearexpansion$rememberPlayer(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
		this.gearexpansion$player = inventory.player;
	}

	@Inject(method = "slotsChanged", at = @At("HEAD"))
	private void gearexpansion$beginOffers(Container container, CallbackInfo ci) {
		EnchantingBonus.begin(gearexpansion$player);
	}

	@Inject(method = "slotsChanged", at = @At("RETURN"))
	private void gearexpansion$endOffers(Container container, CallbackInfo ci) {
		EnchantingBonus.end();
	}
}
