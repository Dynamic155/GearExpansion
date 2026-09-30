package com.gearexpansion.client.renderer;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.item.GearArmorItem;

/**
 * Renders a material's 3D armor. Model: {@code geckolib/models/item/armor/<material>_armor.geo.json};
 * texture: {@code textures/item/armor/<material>_armor.png}.
 *
 * <p>The {@code & GeoRenderState} bound is needed because GeckoLib's interface injection
 * isn't visible to the common module at compile time.
 */
public class GearArmorRenderer<R extends HumanoidRenderState & GeoRenderState> extends GeoArmorRenderer<GearArmorItem, R> {
	public GearArmorRenderer(String material) {
		super(new DefaultedItemGeoModel<>(GearExpansion.id("armor/" + material + "_armor")));
	}
}
