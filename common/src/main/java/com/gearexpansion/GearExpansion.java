package com.gearexpansion;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.item.GearEvents;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.network.ModNetwork;
import com.gearexpansion.recipe.ModRecipes;
import com.gearexpansion.registry.ModBlockEntities;
import com.gearexpansion.registry.ModBlocks;
import com.gearexpansion.registry.ModComponents;
import com.gearexpansion.registry.ModItems;
import com.gearexpansion.registry.ModMenus;
import com.gearexpansion.registry.ModTabs;
import com.gearexpansion.setbonus.SetBonuses;

public final class GearExpansion {
	public static final String MOD_ID = "gearexpansion";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Shared setup, called by both the Fabric and NeoForge entrypoints.
	public static void init() {
		GearExpansionConfig.load();

		ModComponents.COMPONENTS.register();
		ModMaterials.init();
		ModBlocks.BLOCKS.register();
		ModItems.ITEMS.register();
		ModBlockEntities.BLOCK_ENTITIES.register();
		ModMenus.MENUS.register();
		ModRecipes.init();
		ModTabs.TABS.register();

		SetBonuses.init();
		GearEvents.init();
		ModNetwork.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
