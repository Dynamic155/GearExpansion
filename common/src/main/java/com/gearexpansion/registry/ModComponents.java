package com.gearexpansion.registry;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import com.gearexpansion.GearExpansion;

/** Item data this mod stores on stacks. */
public final class ModComponents {
	public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(GearExpansion.MOD_ID, Registries.DATA_COMPONENT_TYPE);

	/** Verdigris gear's oxidation stage: 0 fresh, 1 exposed, 2 weathered, 3 oxidized. Missing means fresh. */
	public static final RegistrySupplier<DataComponentType<Integer>> OXIDATION = COMPONENTS.register("oxidation",
		() -> DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 3)).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	/** Verdigris gear waxed with honeycomb stops oxidizing. */
	public static final RegistrySupplier<DataComponentType<Boolean>> WAXED = COMPONENTS.register("waxed",
		() -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

	private ModComponents() {
	}
}
