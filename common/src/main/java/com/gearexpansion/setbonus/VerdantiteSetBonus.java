package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Overgrowth: crops, saplings, and other growing plants near the wearer grow faster, and working
 * the fields (near farmland or crops) costs no hunger.
 */
public final class VerdantiteSetBonus extends SetBonus {
	private static final int CHECK_INTERVAL = 20;
	private static final int RANGE = 4;

	/** Game time each wearer was last seen near farmland or crops. */
	private final Map<UUID, Long> lastTending = new ConcurrentHashMap<>();

	public VerdantiteSetBonus() {
		super(ModMaterials.VERDANTITE);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().verdantiteSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(
			Component.translatable("set_bonus.gearexpansion.verdantite.growth", RANGE),
			Component.translatable("set_bonus.gearexpansion.verdantite.hunger"));
	}

	@Override
	public void tick(ServerPlayer player) {
		if (player.tickCount % CHECK_INTERVAL != 0) {
			return;
		}
		ServerLevel level = player.level();
		int chance = GearExpansionConfig.get().verdantiteGrowthChance;
		boolean tending = false;
		BlockPos center = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RANGE, -1, -RANGE), center.offset(RANGE, 2, RANGE))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.FARMLAND) || state.is(BlockTags.CROPS)) {
				tending = true;
			}
			if (isGrowingPlant(state) && player.getRandom().nextInt(100) < chance) {
				// An extra random tick, the same thing that makes plants grow over time.
				state.randomTick(level, pos.immutable(), level.getRandom());
				if (!level.getBlockState(pos).equals(state)) {
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.25, 0.25, 0.25, 0.0);
				}
			}
		}
		if (tending) {
			lastTending.put(player.getUUID(), level.getGameTime());
		}
	}

	/** Crops, saplings, stems, berries, and other plants that grow on random ticks. Not grass or other spreading ground. */
	public static boolean isGrowingPlant(BlockState state) {
		return state.isRandomlyTicking() && !state.is(BlockTags.SUBSTRATE_OVERWORLD)
			&& (state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.getBlock() instanceof BonemealableBlock);
	}

	/** Whether the wearer is working the fields right now, so food exhaustion is skipped. */
	public boolean isTending(Player player) {
		Long last = lastTending.get(player.getUUID());
		return last != null && player.level().getGameTime() - last <= CHECK_INTERVAL * 2 && isActive(player);
	}
}
