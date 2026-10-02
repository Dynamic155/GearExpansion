package com.gearexpansion.client;

import com.gearexpansion.network.GearHudPayload;

/** The latest set bonus meters received from the server, for the HUD. */
public final class ClientGearState {
	public static volatile GearHudPayload hud = GearHudPayload.EMPTY;

	private ClientGearState() {
	}
}
