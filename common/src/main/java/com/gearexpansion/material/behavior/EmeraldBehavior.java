package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Emerald: Illager's Bane weapons hit raiders harder, Prospector pickaxes sometimes give bonus
 * experience from ores, and the shield knocks raiders back hard.
 */
public final class EmeraldBehavior implements GearBehavior {
	static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

	@Override
	public float attackDamageBonus(MaterialSet set, ItemStack weapon, Entity victim, float damage, DamageSource source) {
		if (set.isWeapon(weapon) && victim.is(EntityTypeTags.RAIDERS)) {
			return damage * GearExpansionConfig.get().emeraldRaiderDamageBonus / 100.0F;
		}
		return 0.0F;
	}

	@Override
	public void onBlockBroken(MaterialSet set, ServerPlayer player, ItemStack tool, BlockState state, BlockPos pos) {
		if (tool.is(set.pickaxe.get()) && state.is(ORES)
				&& player.getRandom().nextInt(100) < GearExpansionConfig.get().emeraldBonusExperienceChance) {
			ExperienceOrb.award(player.level(), Vec3.atCenterOf(pos), 1 + player.getRandom().nextInt(3));
		}
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		if (attacker.is(EntityTypeTags.RAIDERS)) {
			attacker.knockback(1.5, defender.getX() - attacker.getX(), defender.getZ() - attacker.getZ(), source, damage);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.illagers_bane", GearExpansionConfig.get().emeraldRaiderDamageBonus).withStyle(ChatFormatting.DARK_GREEN));
		} else if (stack.is(set.pickaxe.get())) {
			lines.add(Component.translatable("trait.gearexpansion.prospector").withStyle(ChatFormatting.DARK_GREEN));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.raider_knockback").withStyle(ChatFormatting.DARK_GREEN));
		}
	}
}
