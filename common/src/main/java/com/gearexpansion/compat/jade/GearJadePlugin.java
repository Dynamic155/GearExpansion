package com.gearexpansion.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;

import com.gearexpansion.block.AlloyForgeBlock;
import com.gearexpansion.block.entity.AlloyForgeBlockEntity;

/**
 * Jade support: looking at an Alloy Forge shows its inputs, fuel, progress, and result.
 *
 * <p>Jade only loads this when it is installed. Fabric finds it through the {@code jade}
 * entrypoint, NeoForge through an annotated subclass in the NeoForge module. Jade already shows
 * which tool our ores need, from the vanilla mining tags.
 */
public class GearJadePlugin implements IWailaPlugin {
	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(AlloyForgeJadeProvider.INSTANCE, AlloyForgeBlockEntity.class);
		registration.registerItemStorage(AlloyForgeJadeProvider.HideItemStorage.INSTANCE, AlloyForgeBlockEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(AlloyForgeJadeClient.INSTANCE, AlloyForgeBlock.class);
	}
}
