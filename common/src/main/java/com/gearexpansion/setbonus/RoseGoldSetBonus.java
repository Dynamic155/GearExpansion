package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/** Lucky Charm: more experience, and enchanting tables act as if more bookshelves surround them. */
public final class RoseGoldSetBonus extends SetBonus {
	public RoseGoldSetBonus() {
		super(ModMaterials.ROSE_GOLD);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().roseGoldSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.rose_gold.experience", config.roseGoldExperienceBonus),
			Component.translatable("set_bonus.gearexpansion.rose_gold.enchanting")
		);
	}

	@Override
	public int modifyExperience(int amount, Player wearer) {
		float scaled = amount * (1.0F + Mth.clamp(GearExpansionConfig.get().roseGoldExperienceBonus, 0, 100) / 100.0F);
		int whole = Mth.floor(scaled);
		// Round the leftover fraction randomly, so small orbs are also boosted on average.
		return whole + (wearer.getRandom().nextFloat() < scaled - whole ? 1 : 0);
	}

	@Override
	public int extraEnchantingBookshelves(Player wearer) {
		return Mth.clamp(GearExpansionConfig.get().roseGoldEnchantingBookshelves, 0, 15);
	}
}
