package com.gearexpansion.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.menu.AlloyForgeMenu;

/** The Alloy Forge screen: a furnace screen with three input slots and no recipe book. */
public final class AlloyForgeScreen extends AbstractContainerScreen<AlloyForgeMenu> {
	/** 176x166, the size of the screen. Made by tools/generate_assets.py from vanilla's furnace screen. */
	private static final Identifier TEXTURE = GearExpansion.id("textures/gui/container/alloy_forge.png");
	// Vanilla's furnace sprites; the background texture has matching empty outlines.
	private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
	private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

	public AlloyForgeScreen(AlloyForgeMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x = leftPos;
		int y = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
		if (menu.isLit()) {
			int litHeight = Mth.ceil(menu.getLitProgress() * 13.0F) + 1;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - litHeight, x + 56, y + 36 + 14 - litHeight, 14, litHeight);
		}
		int burnWidth = Mth.ceil(menu.getBurnProgress() * 24.0F);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, x + 88, y + 34, burnWidth, 16);
	}
}
