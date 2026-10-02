package com.gearexpansion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.block.entity.AlloyForgeBlockEntity;
import com.gearexpansion.registry.ModBlockEntities;

/**
 * The Alloy Forge: a furnace with three input slots that melts metals together into alloys.
 * Facing, lighting, comparator output, and dropping contents all work like a vanilla furnace,
 * and it crackles and smokes like a blast furnace while lit.
 */
public final class AlloyForgeBlock extends AbstractFurnaceBlock {
	public AlloyForgeBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AlloyForgeBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel serverLevel
			? createTickerHelper(type, ModBlockEntities.ALLOY_FORGE.get(), (innerLevel, pos, innerState, forge) -> AlloyForgeBlockEntity.serverTick(serverLevel, pos, innerState, forge))
			: null;
	}

	@Override
	protected void openContainer(Level level, BlockPos pos, Player player) {
		if (level.getBlockEntity(pos) instanceof AlloyForgeBlockEntity forge) {
			player.openMenu(forge);
		}
	}

	/** Same sounds and smoke as {@link net.minecraft.world.level.block.BlastFurnaceBlock}, plus a few flames. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		double x = pos.getX() + 0.5;
		double y = pos.getY();
		double z = pos.getZ() + 0.5;
		if (random.nextDouble() < 0.1) {
			level.playLocalSound(x, y, z, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
		}

		Direction direction = state.getValue(FACING);
		Direction.Axis axis = direction.getAxis();
		double side = random.nextDouble() * 0.6 - 0.3;
		double dx = axis == Direction.Axis.X ? direction.getStepX() * 0.52 : side;
		double dy = random.nextDouble() * 9.0 / 16.0;
		double dz = axis == Direction.Axis.Z ? direction.getStepZ() * 0.52 : side;
		level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
		level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
	}
}
