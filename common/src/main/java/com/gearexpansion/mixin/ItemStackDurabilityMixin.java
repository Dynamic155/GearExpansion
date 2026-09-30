package com.gearexpansion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gearexpansion.item.GearDurability;

/**
 * Lets material traits and set bonuses change durability loss. Runs right after vanilla applies Unbreaking.
 *
 * <p>Vanilla and Fabric call Unbreaking from the {@code ServerPlayer} overload of
 * {@code processDurabilityChange}; NeoForge moves that call into a new {@code LivingEntity}
 * overload. Each handler is optional, and exactly one matches on each loader.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackDurabilityMixin {
	private static final String UNBREAKING =
		"Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I";

	@ModifyExpressionValue(
		method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;)I",
		at = @At(value = "INVOKE", target = UNBREAKING),
		require = 0
	)
	private int gearexpansion$modifyDurabilityLoss(int amount, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) ServerPlayer player) {
		return GearDurability.modify((ItemStack) (Object) this, amount, player, level);
	}

	@ModifyExpressionValue(
		method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)I",
		at = @At(value = "INVOKE", target = UNBREAKING),
		require = 0
	)
	private int gearexpansion$modifyDurabilityLossNeoForge(int amount, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) LivingEntity entity) {
		return GearDurability.modify((ItemStack) (Object) this, amount, entity, level);
	}
}
