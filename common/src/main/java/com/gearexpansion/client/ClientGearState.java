package com.gearexpansion.client;

import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.client.Minecraft;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.network.ConfigSyncPayload;
import com.gearexpansion.network.GearHudPayload;

/** What the client knows from the server: the set bonus meters for the HUD, and the server's settings. */
public final class ClientGearState {
	public static volatile GearHudPayload hud = GearHudPayload.EMPTY;

	private ClientGearState() {
	}

	static void init() {
		// Forget the last server's meters and settings when leaving it.
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
			hud = GearHudPayload.EMPTY;
			GearExpansionConfig.clearServerSettings();
		});
	}

	public static void receiveServerSettings(ConfigSyncPayload payload) {
		// In singleplayer, and when hosting a LAN world, this game is the server and already uses its own settings.
		if (!Minecraft.getInstance().hasSingleplayerServer()) {
			GearExpansionConfig.useServerSettings(payload.json());
		}
	}
}
