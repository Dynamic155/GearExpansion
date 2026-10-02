package com.gearexpansion.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.setbonus.SetBonuses;

/** Decides how much durability an item loses, after vanilla applies Unbreaking. Called from the durability mixin. */
public final class GearDurability {
	private GearDurability() {
	}

	public static int modify(ItemStack stack, int amount, @Nullable LivingEntity user, ServerLevel level) {
		if (amount <= 0 || user == null) {
			return amount;
		}
		// Galvanized gear (Zinc) is corrosion-proof: it doesn't wear down while its user is in water.
		if (user.isInWater() && GearExpansionConfig.get().zincCorrosionProof && isGalvanized(stack)) {
			return 0;
		}
		amount = SetBonuses.modifyDurabilityLoss(amount, user, level);
		int loss = amount;
		return ModMaterials.ofGear(stack).map(set -> set.behavior.modifyDurabilityLoss(set, stack, loss, user, level)).orElse(amount);
	}

	public static boolean isGalvanized(ItemStack stack) {
		return ModMaterials.ofGear(stack).map(set -> set.galvanized).orElse(false);
	}
}
