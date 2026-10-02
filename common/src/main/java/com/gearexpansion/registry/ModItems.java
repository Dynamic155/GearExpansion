package com.gearexpansion.registry;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import com.gearexpansion.GearExpansion;

public final class ModItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(GearExpansion.MOD_ID, Registries.ITEM);

	public static final RegistrySupplier<BlockItem> ALLOY_FORGE = register("alloy_forge", p -> new BlockItem(ModBlocks.ALLOY_FORGE.get(), p), Item.Properties::useBlockDescriptionPrefix);

	private ModItems() {
	}

	/** Registers an item in the mod's creative tab. The id is set on its properties before construction, as 26.x requires. */
	public static <T extends Item> RegistrySupplier<T> register(String name, Function<Item.Properties, T> factory, UnaryOperator<Item.Properties> properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, GearExpansion.id(name));
		return ITEMS.register(key.identifier(), () -> factory.apply(properties.apply(new Item.Properties()).setId(key)));
	}
}
