package com.gearexpansion.mixin;

import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.gearexpansion.setbonus.EnchantingBonus;

/** Adds set bonus bookshelves to an enchanting table's offers. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	// getEnchantmentCost(RandomSource random, int slot, int bookcases, ItemStack itemStack): bookcases is the second int.
	@ModifyVariable(method = "getEnchantmentCost", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private static int gearexpansion$addBookshelves(int bookcases) {
		return EnchantingBonus.adjustBookshelves(bookcases);
	}
}
