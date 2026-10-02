package com.gearexpansion.client;

import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.MinecraftServer;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.network.ModNetwork;

/**
 * The in-game settings screen, built by YACL from the annotations on {@link GearExpansionConfig}.
 * Only loaded when YACL is installed. YACL edits its own copy of the settings: the current ones are
 * saved to the file before the screen opens, and Gear Expansion reloads the file after YACL saves.
 */
final class YaclConfigScreen {
	private YaclConfigScreen() {
	}

	static Screen create(Screen parent) {
		GearExpansionConfig.save();
		ConfigClassHandler<GearExpansionConfig> handler = ConfigClassHandler.createBuilder(GearExpansionConfig.class)
			.id(GearExpansion.id("config"))
			.serializer(config -> GsonConfigSerializerBuilder.create(config).setPath(GearExpansionConfig.path()).setJson5(true).build())
			.build();
		handler.load();

		YetAnotherConfigLib generated = handler.generateGui();
		return YetAnotherConfigLib.createBuilder()
			.title(generated.title())
			.categories(generated.categories())
			.save(() -> {
				generated.saveFunction().run();
				// Pick up YACL's changes, and rewrite the file with comments.
				GearExpansionConfig.load();
				// When hosting a LAN world, players who joined keep up with the new settings.
				MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
				if (server != null) {
					server.execute(() -> server.getPlayerList().getPlayers().forEach(ModNetwork::sendSettings));
				}
			})
			.build()
			.generateScreen(parent);
	}
}
