package com.gearexpansion.config;

import dev.architectury.platform.Platform;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.IntSlider;
import dev.isxander.yacl3.config.v2.api.autogen.TickBox;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;

import com.gearexpansion.GearExpansion;

/**
 * Gear Expansion settings, saved to {@code config/gearexpansion.json5}.
 * The in-game screen is generated from the annotations below.
 */
public final class GearExpansionConfig {
	public static final ConfigClassHandler<GearExpansionConfig> HANDLER = ConfigClassHandler.createBuilder(GearExpansionConfig.class)
		.id(GearExpansion.id("config"))
		.serializer(config -> GsonConfigSerializerBuilder.create(config)
			.setPath(Platform.getConfigFolder().resolve("gearexpansion.json5"))
			.setJson5(true)
			.build())
		.build();

	@AutoGen(category = "titanium", group = "set_bonus")
	@TickBox
	@SerialEntry(comment = "Whether the full Titanium set grants its Unbreakable Will bonus.")
	public boolean titaniumSetBonus = true;

	@AutoGen(category = "titanium", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry(comment = "Percent less durability gear loses while wearing the full set.")
	public int titaniumDurabilitySaving = 50;

	@AutoGen(category = "titanium", group = "set_bonus")
	@IntSlider(min = 10, max = 90, step = 5, format = "%d%%")
	@SerialEntry(comment = "Percent of max health below which Resistance kicks in.")
	public int titaniumResistanceThreshold = 30;

	@AutoGen(category = "titanium", group = "set_bonus")
	@IntSlider(min = 1, max = 20, step = 1, format = "%ds")
	@SerialEntry(comment = "How many seconds of Resistance I are granted.")
	public int titaniumResistanceSeconds = 5;

	@AutoGen(category = "titanium", group = "set_bonus")
	@IntSlider(min = 10, max = 600, step = 10, format = "%ds")
	@SerialEntry(comment = "Seconds before the Resistance bonus can trigger again.")
	public int titaniumResistanceCooldown = 60;

	public static GearExpansionConfig get() {
		return HANDLER.instance();
	}

	public static void load() {
		HANDLER.load();
	}
}
