package com.gearexpansion.fabric;

import net.fabricmc.api.ModInitializer;

import com.gearexpansion.GearExpansion;

public final class GearExpansionFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		GearExpansion.init();
	}
}
