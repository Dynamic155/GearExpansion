package com.gearexpansion.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MapColor;

/** All gear materials. Stats and ore placement follow ideas.md. */
public final class ModMaterials {
	public static final List<MaterialSet> ALL = new ArrayList<>();

	/**
	 * Zinc: an early metal a step above copper. Common, and galvanized: zinc gear doesn't
	 * lose durability in water.
	 */
	public static final MaterialSet ZINC = MaterialSet.builder("zinc")
		.tools(ToolTier.COPPER, 260, 5.0F, 1.0F, 13)
		.armor(13, 2, 5, 3, 1, 10, SoundEvents.ARMOR_EQUIP_COPPER, 0.0F, 0.0F)
		.shield(400, 1.0F)
		.requiresTool(BlockTags.NEEDS_STONE_TOOL)
		.ore(9, 10, 0, 64, 0.0F)
		.galvanized()
		.colors(MapColor.CLAY, MapColor.STONE)
		.build();

	/**
	 * Aluminum: light and fast. Mines like iron with faster attacks but low durability;
	 * armor is lighter than iron and makes you quicker. Smelted from Bauxite, which is
	 * richest near the surface of badlands and savannas.
	 */
	public static final MaterialSet ALUMINUM = MaterialSet.builder("aluminum")
		.oreName("bauxite")
		.tools(ToolTier.IRON, 180, 9.0F, 1.5F, 16)
		.attackSpeedBonus(0.3F)
		.armor(12, 2, 5, 4, 1, 14, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F)
		.armorBonus("speed", Attributes.MOVEMENT_SPEED, 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
		.shield(300, 1.0F)
		.blockingSpeed(1.0F)
		.requiresTool(BlockTags.NEEDS_STONE_TOOL)
		.ore(8, 5, 32, 96, 0.3F)
		.ore("badlands", BiomeTags.IS_BADLANDS, 12, 10, 48, 128, 0.0F)
		.ore("savanna", BiomeTags.IS_SAVANNA, 10, 6, 48, 128, 0.0F)
		.colors(MapColor.QUARTZ, MapColor.TERRACOTTA_ORANGE)
		.build();

	/**
	 * Titanium: diamond-tier speed with about twice diamond's durability, but hard to enchant.
	 * Found only deep underground in small, mostly buried veins.
	 */
	public static final MaterialSet TITANIUM = MaterialSet.builder("titanium")
		.tools(ToolTier.DIAMOND, 3000, 8.0F, 3.0F, 8)
		.armor(45, 3, 8, 6, 3, 8, SoundEvents.ARMOR_EQUIP_IRON, 2.5F, 0.05F)
		.shield(1000, 0.4F)
		.requiresTool(BlockTags.NEEDS_DIAMOND_TOOL)
		.ore(5, 4, -64, -16, 0.5F)
		.colors(MapColor.COLOR_LIGHT_GRAY, MapColor.TERRACOTTA_LIGHT_GRAY)
		.build();

	private ModMaterials() {
	}

	/** Loads this class so every material registers its blocks and items. */
	public static void init() {
	}

	/** The material an item of gear (tool, armor piece, or shield) is made of. */
	public static Optional<MaterialSet> ofGear(ItemStack stack) {
		return ALL.stream().filter(set -> set.isGear(stack)).findFirst();
	}
}
