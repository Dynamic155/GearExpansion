package com.gearexpansion.material.behavior;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Infernium: weapons set targets alight and hit burning targets harder, the pickaxe smelts what
 * it mines, the shield burns melee attackers, and each armor piece resists fire.
 */
public final class InferniumBehavior implements GearBehavior {
	/** Fire damage each armor piece blocks (four pieces block 60%). */
	private static final float FIRE_RESISTANCE_PER_PIECE = 0.15F;

	@Override
	public float attackDamageBonus(MaterialSet set, ItemStack weapon, Entity victim, float damage, DamageSource source) {
		return set.isWeapon(weapon) && victim.isOnFire() ? GearExpansionConfig.get().infernumBurningDamageBonus : 0.0F;
	}

	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		if (set.isWeapon(weapon)) {
			target.igniteForSeconds(4.0F);
		}
	}

	@Override
	public List<ItemStack> modifyDrops(MaterialSet set, ItemStack tool, List<ItemStack> drops, ServerLevel level, BlockState state, BlockPos pos) {
		if (!tool.is(set.pickaxe.get())) {
			return drops;
		}
		boolean smelted = false;
		List<ItemStack> result = new ArrayList<>(drops.size());
		for (ItemStack drop : drops) {
			var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(drop), level);
			if (recipe.isPresent()) {
				ItemStack smeltedStack = recipe.get().value().assemble(new SingleRecipeInput(drop));
				smeltedStack.setCount(smeltedStack.getCount() * drop.getCount());
				result.add(smeltedStack);
				smelted = true;
			} else {
				result.add(drop);
			}
		}
		if (smelted) {
			level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.01);
		}
		return result;
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		if (source.getDirectEntity() == attacker) {
			attacker.igniteForSeconds(4.0F);
		}
	}

	@Override
	public float modifyWearerDamage(MaterialSet set, LivingEntity wearer, DamageSource source, float damage, int pieces) {
		return source.is(DamageTypeTags.IS_FIRE) ? damage * (1.0F - FIRE_RESISTANCE_PER_PIECE * pieces) : damage;
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.searing", GearExpansionConfig.get().infernumBurningDamageBonus).withStyle(ChatFormatting.GOLD));
		} else if (stack.is(set.pickaxe.get())) {
			lines.add(Component.translatable("trait.gearexpansion.auto_smelt").withStyle(ChatFormatting.GOLD));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.burning_shield").withStyle(ChatFormatting.GOLD));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.fire_ward", Math.round(FIRE_RESISTANCE_PER_PIECE * 100)).withStyle(ChatFormatting.GOLD));
		}
	}
}
