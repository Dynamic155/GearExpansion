package com.gearexpansion.item;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.google.common.base.Suppliers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.client.renderer.GearArmorRenderer;
import com.gearexpansion.material.MaterialSet;

/**
 * An armor piece drawn with a 3D GeckoLib model. All four pieces of a material share
 * one model: {@code geckolib/models/item/armor/<material>_armor.geo.json}.
 */
public class GearArmorItem extends Item implements GeoItem {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final MaterialSet material;

	public GearArmorItem(Item.Properties properties, MaterialSet material) {
		super(properties);
		this.material = material;
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carried) {
		return clickAction == ClickAction.SECONDARY && !other.isEmpty() && material.behavior.onStackedOn(material, stack, other, player);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		super.inventoryTick(stack, level, owner, slot);
		material.behavior.inventoryTick(material, stack, level, owner, slot);
	}

	@Override
	public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
		// The renderer is only created on the client, the first time the armor is drawn.
		consumer.accept(new GeoRenderProvider() {
			private final Supplier<GearArmorRenderer<?>> renderer = Suppliers.memoize(() -> new GearArmorRenderer<>(material));

			@Override
			public @Nullable GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot slot) {
				return renderer.get();
			}
		});
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// No animations yet. Materials with animated armor (e.g. Brass gears) add controllers here.
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
