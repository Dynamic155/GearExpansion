package com.gearexpansion.material.behavior;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Verdantite: the Replanting axe plants a sapling where it fells the bottom log of a tree, the Wide
 * hoe and shovel till and flatten 3x3 (sneak for a single block), and the Regrowth shield slowly
 * repairs itself while held. Bees leave wearers alone (see {@link com.gearexpansion.mixin.MobTargetMixin}).
 */
public final class VerdantiteBehavior implements GearBehavior {
	private static final int REGROWTH_INTERVAL_TICKS = 100;
	private static final Queue<PendingPlant> PENDING_PLANTS = new ConcurrentLinkedQueue<>();

	@Override
	public void afterUseOn(MaterialSet set, UseOnContext context, InteractionResult result) {
		ItemStack tool = context.getItemInHand();
		Player player = context.getPlayer();
		if (!result.consumesAction() || player == null || player.isSecondaryUseActive() || context.getLevel().isClientSide()
				|| !GearExpansionConfig.get().verdantiteWideTools || !(tool.is(set.hoe.get()) || tool.is(set.shovel.get()))) {
			return;
		}
		var transformer = tool.get(DataComponents.BLOCK_TRANSFORMER);
		if (transformer == null) {
			return;
		}
		BlockPos center = context.getClickedPos();
		Direction face = context.getClickedFace();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if ((dx == 0 && dz == 0) || tool.isEmpty()) {
					continue;
				}
				BlockPos pos = center.offset(dx, 0, dz);
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(face, 0.5), face, pos, false);
				transformer.value().transformBlock(new UseOnContext(player, context.getHand(), hit));
			}
		}
	}

	@Override
	public void onBlockBroken(MaterialSet set, ServerPlayer player, ItemStack tool, BlockState state, BlockPos pos) {
		if (!tool.is(set.axe.get()) || !GearExpansionConfig.get().verdantiteReplanting || !state.is(BlockTags.LOGS)) {
			return;
		}
		ServerLevel level = player.level();
		// Only the bottom log of a tree, standing on soil.
		if (!level.getBlockState(pos.below()).is(BlockTags.SUBSTRATE_OVERWORLD)) {
			return;
		}
		Block sapling = saplingFor(state.getBlock());
		if (sapling == null) {
			return;
		}
		// The log is still there while this event runs, so plant at the end of the tick, once it's gone.
		PENDING_PLANTS.add(new PendingPlant(level, pos.immutable(), sapling));
	}

	/** Plants the saplings queued this tick. Called at the end of every server tick. */
	public static void plantPending() {
		PendingPlant plant;
		while ((plant = PENDING_PLANTS.poll()) != null) {
			ServerLevel level = plant.level();
			BlockPos pos = plant.pos();
			BlockState state = plant.sapling().defaultBlockState();
			if (level.getBlockState(pos).isAir() && state.canSurvive(level, pos)) {
				level.setBlockAndUpdate(pos, state);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.0);
			}
		}
	}

	private record PendingPlant(ServerLevel level, BlockPos pos, Block sapling) {
	}

	/** The sapling a log grows from, by vanilla's naming: oak_log to oak_sapling, crimson_stem to crimson_fungus, and so on. */
	static @Nullable Block saplingFor(Block log) {
		Identifier id = BuiltInRegistries.BLOCK.getKey(log);
		String path = id.getPath();
		String wood = path.endsWith("_log") ? path.substring(0, path.length() - 4)
			: path.endsWith("_wood") ? path.substring(0, path.length() - 5)
			: path.endsWith("_stem") ? path.substring(0, path.length() - 5)
			: null;
		if (wood == null) {
			return null;
		}
		for (String candidate : List.of(wood + "_sapling", wood + "_propagule", wood + "_fungus")) {
			Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), candidate));
			if (block.isPresent()) {
				return block.get();
			}
		}
		return null;
	}

	@Override
	public void inventoryTick(MaterialSet set, ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		// Regrowth: a held shield slowly mends itself.
		if (stack.is(set.shield.get()) && stack.isDamaged() && (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND)
				&& owner instanceof LivingEntity && level.getGameTime() % REGROWTH_INTERVAL_TICKS == 0) {
			stack.setDamageValue(stack.getDamageValue() - 1);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (stack.is(set.axe.get())) {
			lines.add(Component.translatable("trait.gearexpansion.replanting").withStyle(ChatFormatting.GREEN));
		} else if (stack.is(set.hoe.get()) || stack.is(set.shovel.get())) {
			lines.add(Component.translatable("trait.gearexpansion.wide").withStyle(ChatFormatting.GREEN));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.regrowth").withStyle(ChatFormatting.GREEN));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.bee_friend").withStyle(ChatFormatting.GREEN));
		}
	}
}
