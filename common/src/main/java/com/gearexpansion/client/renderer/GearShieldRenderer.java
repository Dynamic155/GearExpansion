package com.gearexpansion.client.renderer;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.GeoItemRenderer;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.item.GearShieldItem;

/**
 * Renders a material's 3D shield. Model: {@code geckolib/models/item/<material>_shield.geo.json};
 * texture: {@code textures/item/<material>_shield.png}.
 */
public class GearShieldRenderer extends GeoItemRenderer<GearShieldItem> {
	public GearShieldRenderer(String material) {
		super(new DefaultedItemGeoModel<>(GearExpansion.id(material + "_shield")));
	}
}
