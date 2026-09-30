package com.gearexpansion.registry;

import java.util.function.Function;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.gearexpansion.GearExpansion;

public final class ModBlocks {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(GearExpansion.MOD_ID, Registries.BLOCK);

	private ModBlocks() {
	}

	/** Registers a block. The id is set on its properties before construction, as 26.x requires. */
	public static RegistrySupplier<Block> register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, GearExpansion.id(name));
		return BLOCKS.register(key.identifier(), () -> factory.apply(properties.setId(key)));
	}
}
