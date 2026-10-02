package com.gearexpansion.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.network.UseAbilityPayload;

/** The Set Ability key (R by default), which uses the worn set's ability, like Brass's Spring Release. */
public final class GearKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(GearExpansion.id("main"));
	public static final KeyMapping SET_ABILITY = new KeyMapping("key.gearexpansion.set_ability", InputConstants.KEY_R, CATEGORY);

	private GearKeys() {
	}

	public static void init() {
		KeyMappingRegistry.register(SET_ABILITY);
		ClientTickEvent.CLIENT_POST.register(minecraft -> {
			while (SET_ABILITY.consumeClick()) {
				if (minecraft.player != null && NetworkManager.canServerReceive(UseAbilityPayload.TYPE)) {
					NetworkManager.sendToServer(UseAbilityPayload.INSTANCE);
				}
			}
		});
	}
}
