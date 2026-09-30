package com.gearexpansion.material;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.material.MapColor;

/** All gear materials. Stats and ore placement follow ideas.md. */
public final class ModMaterials {
	public static final List<MaterialSet> ALL = new ArrayList<>();

	/**
	 * Titanium: diamond-tier speed with about twice diamond's durability, but hard to enchant.
	 * Found only deep underground in small, mostly buried veins.
	 */
	public static final MaterialSet TITANIUM = MaterialSet.builder("titanium")
		.tools(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 3000, 8.0F, 3.0F, 8)
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
}
