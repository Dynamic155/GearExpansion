package com.gearexpansion.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;

import com.gearexpansion.client.ClientGearState;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.setbonus.SetBonuses;

/** Packets between client and server: ability key presses, HUD meters, and the server's settings. */
public final class ModNetwork {
	private ModNetwork() {
	}

	public static void init() {
		NetworkManager.registerReceiver(NetworkManager.c2s(), UseAbilityPayload.TYPE, UseAbilityPayload.STREAM_CODEC, (payload, context) ->
			context.queue(() -> {
				if (context.getPlayer() instanceof ServerPlayer player) {
					SetBonuses.useAbility(player);
				}
			}));

		if (Platform.getEnvironment() == Env.CLIENT) {
			NetworkManager.registerReceiver(NetworkManager.s2c(), GearHudPayload.TYPE, GearHudPayload.STREAM_CODEC, (payload, context) ->
				context.queue(() -> ClientGearState.hud = payload));
			NetworkManager.registerReceiver(NetworkManager.s2c(), ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC, (payload, context) ->
				context.queue(() -> ClientGearState.receiveServerSettings(payload)));
		} else {
			NetworkManager.registerS2CPayloadType(GearHudPayload.TYPE, GearHudPayload.STREAM_CODEC);
			NetworkManager.registerS2CPayloadType(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
		}

		PlayerEvent.PLAYER_JOIN.register(ModNetwork::sendSettings);
	}

	/** Sends this server's settings to a player, so their game uses the same values. */
	public static void sendSettings(ServerPlayer player) {
		if (NetworkManager.canPlayerReceive(player, ConfigSyncPayload.TYPE)) {
			NetworkManager.sendToPlayer(player, new ConfigSyncPayload(GearExpansionConfig.toSyncJson()));
		}
	}
}
