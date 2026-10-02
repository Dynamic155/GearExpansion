package com.gearexpansion.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import com.gearexpansion.client.GearExpansionClient;

/** Adds a settings button to Gear Expansion's entry in Mod Menu. Only loaded when Mod Menu is installed. */
public final class GearExpansionModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		// Without YACL there's no settings screen, so Mod Menu shows no settings button.
		return GearExpansionClient.hasConfigScreen() ? GearExpansionClient::configScreen : parent -> null;
	}
}
