package com.gearexpansion.neoforge.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.gearexpansion.client.GearExpansionClient;

/** Client-only NeoForge setup. Kept in its own class so servers never load client code. */
public final class GearExpansionNeoForgeClient {
	private GearExpansionNeoForgeClient() {
	}

	public static void init(ModContainer container) {
		GearExpansionClient.init();
		// Adds a "Config" button to Gear Expansion in the Mods list.
		container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> GearExpansionClient.configScreen(parent));
	}
}
