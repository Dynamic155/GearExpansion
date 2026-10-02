package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Amethyst: critical hits shatter crystal over nearby mobs, the Resonance pickaxe chimes louder
 * when more of an ore is nearby, a perfectly timed shield block knocks attackers back, and the
 * armor chimes softly when hit.
 */
public final class AmethystBehavior implements GearBehavior {
	private static final int RESONANCE_RADIUS = 5;
	private static final int PERFECT_BLOCK_TICKS = 10;

	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		// A falling attack is a critical hit, the same test vanilla uses (minus sprinting and blindness).
		boolean critical = attacker.fallDistance > 0.0F && !attacker.onGround() && !attacker.isInWater() && !attacker.isPassenger();
		if (!weapon.is(set.sword.get()) || !critical || !(target.level() instanceof ServerLevel level)) {
			return;
		}
		float damage = GearExpansionConfig.get().amethystShatterDamage;
		DamageSource source = attacker instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(attacker);
		for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(2.5),
				entity -> entity != target && entity != attacker && entity.isAlive() && !entity.isAlliedTo(attacker))) {
			nearby.hurtServer(level, source, damage);
		}
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
			target.getX(), target.getY(0.5), target.getZ(), 30, 0.6, 0.6, 0.6, 0.15);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	@Override
	public void onBlockBroken(MaterialSet set, ServerPlayer player, ItemStack tool, BlockState state, BlockPos pos) {
		if (!tool.is(set.pickaxe.get()) || !state.is(EmeraldBehavior.ORES)) {
			return;
		}
		int nearby = 0;
		for (BlockPos other : BlockPos.betweenClosed(pos.offset(-RESONANCE_RADIUS, -RESONANCE_RADIUS, -RESONANCE_RADIUS),
				pos.offset(RESONANCE_RADIUS, RESONANCE_RADIUS, RESONANCE_RADIUS))) {
			if (!other.equals(pos) && player.level().getBlockState(other).is(state.getBlock())) {
				nearby++;
			}
		}
		// Louder and higher the more of the same ore is around: a hot-or-cold hint, not x-ray.
		float volume = Mth.clamp(0.2F + nearby * 0.15F, 0.2F, 2.0F);
		float pitch = Mth.clamp(0.7F + nearby * 0.08F, 0.7F, 2.0F);
		player.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, volume, pitch);
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		if (defender.getTicksUsingItem() <= PERFECT_BLOCK_TICKS) {
			attacker.knockback(1.2, defender.getX() - attacker.getX(), defender.getZ() - attacker.getZ(), source, damage);
			defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.0F);
		}
	}

	@Override
	public float modifyWearerDamage(MaterialSet set, LivingEntity wearer, DamageSource source, float damage, int pieces) {
		wearer.level().playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.3F * pieces, 1.0F);
		return damage;
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (stack.is(set.sword.get())) {
			lines.add(Component.translatable("trait.gearexpansion.shatter").withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (stack.is(set.pickaxe.get())) {
			lines.add(Component.translatable("trait.gearexpansion.resonance").withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.perfect_block").withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}
}
