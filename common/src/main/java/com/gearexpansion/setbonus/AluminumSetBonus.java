package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/** Featherweight: less fall damage, higher jumps, and faster movement in water. */
public final class AluminumSetBonus extends SetBonus {
	public AluminumSetBonus() {
		super(ModMaterials.ALUMINUM);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().aluminumSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.aluminum.fall", config.aluminumFallDamageReduction),
			Component.translatable("set_bonus.gearexpansion.aluminum.jump", config.aluminumJumpBoost),
			Component.translatable("set_bonus.gearexpansion.aluminum.water", config.aluminumWaterSpeed)
		);
	}

	@Override
	public List<AttributeBonus> attributeBonuses() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			new AttributeBonus(GearExpansion.id("set_bonus.aluminum.fall_damage"), Attributes.FALL_DAMAGE_MULTIPLIER,
				-config.aluminumFallDamageReduction / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
			new AttributeBonus(GearExpansion.id("set_bonus.aluminum.jump"), Attributes.JUMP_STRENGTH,
				config.aluminumJumpBoost / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
			// Depth Strider I adds 0.33.
			new AttributeBonus(GearExpansion.id("set_bonus.aluminum.water"), Attributes.WATER_MOVEMENT_EFFICIENCY,
				config.aluminumWaterSpeed / 100.0, AttributeModifier.Operation.ADD_VALUE)
		);
	}
}
