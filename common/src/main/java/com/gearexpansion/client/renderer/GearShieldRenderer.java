package com.gearexpansion.client.renderer;

import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.item.GearShieldItem;
import com.gearexpansion.material.MaterialSet;

/**
 * Renders a material's 3D shield. Model: {@code geckolib/models/item/<material>_shield.geo.json};
 * texture: {@code textures/item/<material>_shield.png}, plus {@code _glowmask.png} for glowing materials.
 */
public class GearShieldRenderer extends GeoItemRenderer<GearShieldItem> {
	private final MaterialSet material;

	public GearShieldRenderer(MaterialSet material) {
		super(new GearGeoModel<GearShieldItem>(GearExpansion.id(material.name + "_shield")));
		this.material = material;
		if (material.glowing) {
			withRenderLayer(AutoGlowingGeoLayer::new);
		}
	}

	@Override
	public void addRenderData(GearShieldItem item, RenderData data, GeoRenderState state, float partialTick) {
		state.addGeckolibData(GearGeoModel.TEXTURE_VARIANT, material.behavior.textureVariant(material, data.itemStack()));
	}
}
