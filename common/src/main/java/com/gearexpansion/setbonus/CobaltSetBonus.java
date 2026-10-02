package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Overdrive: breaking blocks quickly one after another builds up Haste, up to Haste II. The chain
 * breaks if too long passes between blocks.
 */
public final class CobaltSetBonus extends SetBonus {
	public static final int MAX_LEVEL = 2;

	private final Map<UUID, Chain> chains = new ConcurrentHashMap<>();

	public CobaltSetBonus() {
		super(ModMaterials.COBALT);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().cobaltSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(Component.translatable("set_bonus.gearexpansion.cobalt.overdrive", config.cobaltOverdriveBlocks, config.cobaltOverdriveWindow));
	}

	@Override
	public void onBlockBroken(ServerPlayer player, BlockState state, BlockPos pos) {
		GearExpansionConfig config = GearExpansionConfig.get();
		int window = Math.max(1, config.cobaltOverdriveWindow) * 20;
		long now = player.level().getGameTime();
		Chain previous = chains.get(player.getUUID());
		int blocks = previous != null && now - previous.lastBreak() <= window ? previous.blocks() + 1 : 1;
		chains.put(player.getUUID(), new Chain(blocks, now));

		int level = Math.min(MAX_LEVEL, blocks / Math.max(1, config.cobaltOverdriveBlocks));
		if (level > 0) {
			// Lasts as long as the window, so it fades once the chain breaks.
			player.addEffect(new MobEffectInstance(MobEffects.HASTE, window, level - 1, true, true));
		}
	}

	/** How many blocks the player has broken in a row, for the game test. */
	public int chainLength(ServerPlayer player) {
		Chain chain = chains.get(player.getUUID());
		return chain == null ? 0 : chain.blocks();
	}

	private record Chain(int blocks, long lastBreak) {
	}
}
