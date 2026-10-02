package com.gearexpansion.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;

import com.gearexpansion.client.ClientGearState;
import com.gearexpansion.setbonus.SetBonuses;

/** Packets between client and server: ability key presses and HUD meters. */
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
		} else {
			NetworkManager.registerS2CPayloadType(GearHudPayload.TYPE, GearHudPayload.STREAM_CODEC);
		}
	}
}
