package com.gearexpansion.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gearexpansion.material.ModMaterials;

/** Lets a tool's material change what blocks drop, e.g. Infernium pickaxes smelting ores. */
@Mixin(Block.class)
public abstract class BlockDropsMixin {
	@ModifyReturnValue(
		method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
		at = @At("RETURN")
	)
	private static List<ItemStack> gearexpansion$modifyDrops(List<ItemStack> drops, BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, Entity breaker, ItemInstance tool) {
		if (tool instanceof ItemStack stack && !stack.isEmpty()) {
			return ModMaterials.ofGear(stack).map(set -> set.behavior.modifyDrops(set, stack, drops, level, state, pos)).orElse(drops);
		}
		return drops;
	}
}
