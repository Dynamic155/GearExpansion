package com.gearexpansion.client;

import net.minecraft.client.gui.screens.Screen;

import com.gearexpansion.config.GearExpansionConfig;

/** Client-only setup, called by both loaders' client entrypoints. */
public final class GearExpansionClient {
	private GearExpansionClient() {
	}

	public static void init() {
		GearTooltips.init();
	}

	/** The settings screen, opened from Mod Menu on Fabric or the Mods list on NeoForge. */
	public static Screen configScreen(Screen parent) {
		return GearExpansionConfig.HANDLER.generateGui().generateScreen(parent);
	}
}
