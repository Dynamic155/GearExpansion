package com.gearexpansion.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import com.gearexpansion.network.GearHudPayload;

/**
 * Small meters above the hotbar for set bonuses that need them: Brass's spring, Infernium's heat
 * gauge, Amethyst's crystal shell, and the set ability's cooldown. Hidden when not in use.
 */
public final class GearHudOverlay {
	private static final int WIDTH = 81;
	private static final int HEIGHT = 3;
	private static final int BACKGROUND = 0xA0000000;

	private GearHudOverlay() {
	}

	public static void init() {
		ClientGuiEvent.RENDER_HUD.register((graphics, deltaTracker) -> render(graphics));
	}

	private static void render(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.player.isSpectator()) {
			return;
		}
		GearHudPayload hud = ClientGearState.hud;
		// Centered above the experience bar, stacking upward.
		int x = graphics.guiWidth() / 2 - WIDTH / 2;
		int y = graphics.guiHeight() - 32 - 22;
		y = meter(graphics, x, y, hud.spring(), hud.springMax(), 0xFFD4A84A, "hud.gearexpansion.spring");
		y = meter(graphics, x, y, hud.heat(), hud.heatMax(), 0xFFFF6A1A, "hud.gearexpansion.heat");
		y = meter(graphics, x, y, hud.shell(), hud.shellMax(), 0xFFB57EDC, "hud.gearexpansion.shell");
		// The cooldown bar empties as the ability gets ready again.
		meter(graphics, x, y, hud.cooldown(), hud.cooldownMax(), 0xFFAAAAAA, "hud.gearexpansion.cooldown");
	}

	/** Draws one labelled meter if it's in use, and returns the y for the next one above it. */
	private static int meter(GuiGraphicsExtractor graphics, int x, int y, int value, int max, int color, String label) {
		if (max <= 0 || (label.endsWith("cooldown") && value <= 0)) {
			return y;
		}
		int filled = Math.round(WIDTH * Math.min(1.0F, value / (float) max));
		graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, BACKGROUND);
		graphics.fill(x, y, x + filled, y + HEIGHT, color);
		var font = Minecraft.getInstance().font;
		graphics.text(font, Component.translatable(label), x + WIDTH + 4, y - 3, color, true);
		return y - HEIGHT - 6;
	}
}
