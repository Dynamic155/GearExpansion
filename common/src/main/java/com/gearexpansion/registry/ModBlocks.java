package com.gearexpansion.registry;

import java.util.function.Function;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.block.AlloyForgeBlock;

public final class ModBlocks {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(GearExpansion.MOD_ID, Registries.BLOCK);

	/** Same strength, sound, and light as a blast furnace. */
	public static final RegistrySupplier<Block> ALLOY_FORGE = register("alloy_forge", AlloyForgeBlock::new, BlockBehaviour.Properties.of()
		.mapColor(MapColor.COLOR_ORANGE)
		.instrument(NoteBlockInstrument.BASEDRUM)
		.requiresCorrectToolForDrops()
		.strength(3.5F)
		.lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 13 : 0));

	private ModBlocks() {
	}

	/** Registers a block. The id is set on its properties before construction, as 26.x requires. */
	public static RegistrySupplier<Block> register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, GearExpansion.id(name));
		return BLOCKS.register(key.identifier(), () -> factory.apply(properties.setId(key)));
	}
}
