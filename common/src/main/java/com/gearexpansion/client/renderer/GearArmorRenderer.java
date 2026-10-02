package com.gearexpansion.client.renderer;

import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.item.GearArmorItem;
import com.gearexpansion.material.MaterialSet;

/**
 * Renders a material's 3D armor. Model: {@code geckolib/models/item/armor/<material>_armor.geo.json};
 * texture: {@code textures/item/armor/<material>_armor.png}, plus {@code _glowmask.png} for glowing materials.
 *
 * <p>The {@code & GeoRenderState} bound is needed because GeckoLib's interface injection
 * isn't visible to the common module at compile time.
 */
public class GearArmorRenderer<R extends HumanoidRenderState & GeoRenderState> extends GeoArmorRenderer<GearArmorItem, R> {
	private final MaterialSet material;

	public GearArmorRenderer(MaterialSet material) {
		super(new GearGeoModel<>(GearExpansion.id("armor/" + material.name + "_armor")));
		this.material = material;
		if (material.glowing) {
			withRenderLayer(AutoGlowingGeoLayer::new);
		}
	}

	@Override
	public void addRenderData(GearArmorItem item, RenderData data, R state, float partialTick) {
		state.addGeckolibData(GearGeoModel.TEXTURE_VARIANT, material.behavior.textureVariant(material, data.itemStack()));
	}
}
