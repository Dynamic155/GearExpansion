package com.gearexpansion.client;

import dev.architectury.platform.Platform;
import dev.architectury.registry.client.gui.MenuScreenRegistry;
import net.minecraft.client.gui.screens.Screen;

import com.gearexpansion.client.screen.AlloyForgeScreen;
import com.gearexpansion.registry.ModMenus;

/** Client-only setup, called by both loaders' client entrypoints. */
public final class GearExpansionClient {
	private GearExpansionClient() {
	}

	public static void init() {
		GearTooltips.init();
		GearKeys.init();
		GearHudOverlay.init();
		// Waits until the menu type is registered: on NeoForge that happens after this runs.
		ModMenus.ALLOY_FORGE.listen(type -> MenuScreenRegistry.registerScreenFactory(type, AlloyForgeScreen::new));
	}

	/** Whether the in-game settings screen is available: it needs YACL, which is optional. */
	public static boolean hasConfigScreen() {
		return Platform.isModLoaded("yet_another_config_lib_v3");
	}

	/** The settings screen, opened from Mod Menu on Fabric or the Mods list on NeoForge. Needs YACL. */
	public static Screen configScreen(Screen parent) {
		return YaclConfigScreen.create(parent);
	}
}
