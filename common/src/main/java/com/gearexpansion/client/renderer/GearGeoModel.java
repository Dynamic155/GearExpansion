package com.gearexpansion.client.renderer;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

/**
 * A GeckoLib item model whose texture can change per item stack: the renderer stores a suffix
 * (from the material's {@code GearBehavior#textureVariant}) and this picks {@code <texture><suffix>.png}.
 */
public class GearGeoModel<T extends GeoAnimatable> extends DefaultedItemGeoModel<T> {
	public static final DataTicket<String> TEXTURE_VARIANT = DataTicket.create("gearexpansion_texture_variant", String.class);

	public GearGeoModel(Identifier assetSubpath) {
		super(assetSubpath);
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		Identifier texture = super.getTextureResource(renderState);
		String variant = renderState.getOrDefaultGeckolibData(TEXTURE_VARIANT, "");
		if (variant.isEmpty()) {
			return texture;
		}
		String path = texture.getPath();
		return texture.withPath(path.substring(0, path.length() - ".png".length()) + variant + ".png");
	}
}
