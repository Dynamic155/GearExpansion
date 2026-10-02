package com.gearexpansion.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.menu.AlloyForgeMenu;

public final class ModMenus {
	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(GearExpansion.MOD_ID, Registries.MENU);

	// The MenuType constructor is made public by Architectury's access widener.
	public static final RegistrySupplier<MenuType<AlloyForgeMenu>> ALLOY_FORGE = MENUS.register("alloy_forge",
		() -> new MenuType<>(AlloyForgeMenu::new, FeatureFlags.VANILLA_SET));

	private ModMenus() {
	}
}
