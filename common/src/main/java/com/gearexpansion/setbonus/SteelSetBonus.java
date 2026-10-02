package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/** Hardened: extra armor toughness. No gimmick; steel is the dependable choice. */
public final class SteelSetBonus extends SetBonus {
	public SteelSetBonus() {
		super(ModMaterials.STEEL);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().steelSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(Component.translatable("set_bonus.gearexpansion.steel.toughness", GearExpansionConfig.get().steelToughnessBonus));
	}

	@Override
	public List<AttributeBonus> attributeBonuses() {
		return List.of(new AttributeBonus(GearExpansion.id("set_bonus.steel.toughness"), Attributes.ARMOR_TOUGHNESS,
			GearExpansionConfig.get().steelToughnessBonus, AttributeModifier.Operation.ADD_VALUE));
	}
}
