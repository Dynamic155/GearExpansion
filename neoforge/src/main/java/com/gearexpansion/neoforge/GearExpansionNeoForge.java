package com.gearexpansion.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.neoforge.client.GearExpansionNeoForgeClient;

@Mod(GearExpansion.MOD_ID)
public final class GearExpansionNeoForge {
	public GearExpansionNeoForge(ModContainer container) {
		GearExpansion.init();

		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			GearExpansionNeoForgeClient.init(container);
		}
	}
}
