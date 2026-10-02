package com.gearexpansion.registry;

import java.util.Set;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.block.entity.AlloyForgeBlockEntity;

public final class ModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(GearExpansion.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

	public static final RegistrySupplier<BlockEntityType<AlloyForgeBlockEntity>> ALLOY_FORGE = BLOCK_ENTITIES.register("alloy_forge",
		() -> new BlockEntityType<>(AlloyForgeBlockEntity::new, Set.of(ModBlocks.ALLOY_FORGE.get())));

	private ModBlockEntities() {
	}
}
