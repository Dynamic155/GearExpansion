package com.gearexpansion.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

public final class ModTabs {
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(GearExpansion.MOD_ID, Registries.CREATIVE_MODE_TAB);

	public static final RegistrySupplier<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeTabRegistry.create(builder -> builder
		.title(Component.translatable("itemGroup.gearexpansion"))
		.icon(() -> new ItemStack(ModMaterials.TITANIUM.chestplate.get()))
		.displayItems((parameters, output) -> {
			// Grouped by material, in the order a player would find them.
			for (MaterialSet set : ModMaterials.ALL) {
				set.blocks().forEach(block -> output.accept(block.get()));
				output.accept(set.rawItem.get());
				output.accept(set.nugget.get());
				output.accept(set.ingot.get());
				set.tools().forEach(tool -> output.accept(tool.get()));
				set.armorPieces().forEach(armor -> output.accept(armor.get()));
				output.accept(set.shield.get());
			}
		})));

	private ModTabs() {
	}
}
