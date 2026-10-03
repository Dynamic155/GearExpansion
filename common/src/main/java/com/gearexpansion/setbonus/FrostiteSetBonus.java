package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Permafrost: melee attackers are slowed, and sprinting along water freezes it underfoot, like a
 * weaker Frost Walker (radius 1). The ice melts again soon after.
 */
public final class FrostiteSetBonus extends SetBonus {
	private static final int FREEZE_RADIUS = 1;

	public FrostiteSetBonus() {
		super(ModMaterials.FROSTITE);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().frostiteSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(
			Component.translatable("set_bonus.gearexpansion.frostite.slow"),
			Component.translatable("set_bonus.gearexpansion.frostite.walk"));
	}

	@Override
	public float modifyIncomingDamage(LivingEntity wearer, DamageSource source, float damage) {
		if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && attacker != wearer) {
			attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), wearer);
		}
		return damage;
	}

	@Override
	public void tick(ServerPlayer player) {
		if (!GearExpansionConfig.get().frostiteFreezeWater || !player.isSprinting() || !player.onGround()) {
			return;
		}
		ServerLevel level = player.level();
		BlockState frostedIce = Blocks.FROSTED_ICE.defaultBlockState();
		BlockPos feet = player.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-FREEZE_RADIUS, -1, -FREEZE_RADIUS), feet.offset(FREEZE_RADIUS, -1, FREEZE_RADIUS))) {
			// Still water with open air above, the same rule Frost Walker uses.
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.WATER) && state.getFluidState().isSourceOfType(Fluids.WATER) && level.getBlockState(pos.above()).isAir()
					&& frostedIce.canSurvive(level, pos) && level.isUnobstructed(frostedIce, pos, CollisionContext.empty())) {
				level.setBlockAndUpdate(pos.immutable(), frostedIce);
				level.scheduleTick(pos.immutable(), Blocks.FROSTED_ICE, Mth.nextInt(player.getRandom(), 60, 120));
			}
		}
	}
}
