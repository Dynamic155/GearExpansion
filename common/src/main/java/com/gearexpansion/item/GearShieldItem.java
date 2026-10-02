package com.gearexpansion.item;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import com.google.common.base.Suppliers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.client.renderer.GearShieldRenderer;
import com.gearexpansion.material.MaterialSet;

/**
 * A shield drawn with a 3D GeckoLib model: {@code geckolib/models/item/<material>_shield.geo.json}.
 * It extends {@link ShieldItem} so vanilla's first-person blocking pose applies; blocking itself
 * comes from the {@code blocks_attacks} component set in {@code MaterialSet}.
 */
public class GearShieldItem extends ShieldItem implements GeoItem {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final MaterialSet material;

	public GearShieldItem(Properties properties, MaterialSet material) {
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
		consumer.accept(new GeoRenderProvider() {
			private final Supplier<GearShieldRenderer> renderer = Suppliers.memoize(() -> new GearShieldRenderer(material));

			@Override
			public @Nullable GeoItemRenderer<?> getGeoItemRenderer() {
				return renderer.get();
			}
		});
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
