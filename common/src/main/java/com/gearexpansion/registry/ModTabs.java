package com.gearexpansion.registry;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/**
 * Creative inventory tabs, split like vanilla's: Blocks, Tools, Combat, and Ingredients.
 * Each tab lists its items grouped by material, in the order players progress through them.
 */
public final class ModTabs {
	public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(GearExpansion.MOD_ID, Registries.CREATIVE_MODE_TAB);

	public static final RegistrySupplier<CreativeModeTab> BLOCKS = tab("blocks", () -> ModMaterials.TITANIUM.ore.get(),
		() -> List.of(ModBlocks.ALLOY_FORGE.get(), ModBlocks.FULGURITE.get()),
		set -> set.blocks().stream().map(Supplier::get).map(ItemLike.class::cast).toList());

	public static final RegistrySupplier<CreativeModeTab> TOOLS = tab("tools", () -> ModMaterials.TITANIUM.pickaxe.get(),
		List::of,
		set -> List.of(set.pickaxe.get(), set.axe.get(), set.shovel.get(), set.hoe.get()));

	public static final RegistrySupplier<CreativeModeTab> COMBAT = tab("combat", () -> ModMaterials.TITANIUM.sword.get(),
		List::of,
		set -> List.of(set.sword.get(), set.dagger.get(), set.scythe.get(), set.spear.get(), set.helmet.get(), set.chestplate.get(), set.leggings.get(), set.boots.get(), set.shield.get()));

	public static final RegistrySupplier<CreativeModeTab> INGREDIENTS = tab("ingredients", () -> ModMaterials.TITANIUM.ingot.get(),
		List::of,
		set -> set.ingredients().stream().map(Supplier::get).map(ItemLike.class::cast).toList());

	private ModTabs() {
	}

	/**
	 * A tab showing {@code first} (items that aren't part of a material, like the Alloy Forge), then
	 * {@code contents} of every material, in material order. Its title is {@code itemGroup.gearexpansion.<name>}.
	 */
	private static RegistrySupplier<CreativeModeTab> tab(String name, Supplier<ItemLike> icon, Supplier<List<? extends ItemLike>> first,
			Function<MaterialSet, List<? extends ItemLike>> contents) {
		return TABS.register(name, () -> CreativeTabRegistry.create(builder -> builder
			.title(Component.translatable("itemGroup.gearexpansion." + name))
			.icon(() -> new ItemStack(icon.get()))
			.displayItems((parameters, output) -> {
				first.get().forEach(output::accept);
				for (MaterialSet set : ModMaterials.ALL) {
					contents.apply(set).forEach(output::accept);
				}
			})));
	}
}
