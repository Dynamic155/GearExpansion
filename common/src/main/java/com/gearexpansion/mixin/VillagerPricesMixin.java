package com.gearexpansion.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gearexpansion.material.behavior.PastelPrincessBehavior;

/** Pastel Princess armor earns villager discounts, on top of reputation and Hero of the Village. */
@Mixin(Villager.class)
public abstract class VillagerPricesMixin {
	@Inject(method = "updateSpecialPrices", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/npc/villager/Villager;getTradingPlayer()Lnet/minecraft/world/entity/player/Player;"))
	private void gearexpansion$royalFavor(Player player, CallbackInfo ci) {
		double discount = PastelPrincessBehavior.villagerDiscount(player);
		if (discount <= 0.0) {
			return;
		}
		for (MerchantOffer offer : ((Villager) (Object) this).getOffers()) {
			int reduction = (int) Math.floor(discount * offer.getBaseCostA().getCount());
			offer.addToSpecialPriceDiff(-Math.max(reduction, 1));
		}
	}
}
