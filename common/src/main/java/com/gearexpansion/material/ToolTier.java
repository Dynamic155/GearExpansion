package com.gearexpansion.material;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * A vanilla tool tier: which blocks it can't mine, plus the per-tool damage and speed
 * baselines vanilla uses at that tier. The material's damage bonus is added on top.
 */
public enum ToolTier {
	COPPER(BlockTags.INCORRECT_FOR_COPPER_TOOL, 7.0F, -3.2F, -1.0F, -2.0F,
		new float[] {0.85F, 0.82F, 0.65F, 4.0F, 12.0F, 8.25F, 5.1F, 12.5F, 4.6F}),
	IRON(BlockTags.INCORRECT_FOR_IRON_TOOL, 6.0F, -3.1F, -2.0F, -1.0F,
		new float[] {0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F}),
	DIAMOND(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 5.0F, -3.0F, -3.0F, 0.0F,
		new float[] {1.05F, 1.075F, 0.5F, 3.0F, 10.0F, 6.5F, 5.1F, 10.0F, 4.6F}),
	NETHERITE(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 5.0F, -3.0F, -4.0F, 0.0F,
		new float[] {1.15F, 1.2F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F});

	public final TagKey<Block> incorrectBlocksForDrops;
	final float axeDamage;
	final float axeSpeed;
	final float hoeDamage;
	final float hoeSpeed;
	/** The nine timing values passed to {@code Item.Properties#spear}, copied from the vanilla spear of this tier. */
	final float[] spear;

	ToolTier(TagKey<Block> incorrectBlocksForDrops, float axeDamage, float axeSpeed, float hoeDamage, float hoeSpeed, float[] spear) {
		this.incorrectBlocksForDrops = incorrectBlocksForDrops;
		this.axeDamage = axeDamage;
		this.axeSpeed = axeSpeed;
		this.hoeDamage = hoeDamage;
		this.hoeSpeed = hoeSpeed;
		this.spear = spear;
	}
}
