package com.gearexpansion.fabric.client;

import net.fabricmc.api.ClientModInitializer;

import com.gearexpansion.client.GearExpansionClient;

public final class GearExpansionFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		GearExpansionClient.init();
	}
}
