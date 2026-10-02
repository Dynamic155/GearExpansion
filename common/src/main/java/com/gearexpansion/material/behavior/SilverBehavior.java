package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Silver: Hallowed weapons deal extra damage to undead, Warding armor softens their attacks,
 * and the shield knocks undead back hard.
 */
public final class SilverBehavior implements GearBehavior {
	@Override
	public float attackDamageBonus(MaterialSet set, ItemStack weapon, Entity victim, float damage, DamageSource source) {
		return set.isWeapon(weapon) && victim.is(EntityTypeTags.UNDEAD) ? GearExpansionConfig.get().silverUndeadDamageBonus : 0.0F;
	}

	@Override
	public float modifyWearerDamage(MaterialSet set, LivingEntity wearer, DamageSource source, float damage, int pieces) {
		// The attacker, or the shooter of a projectile.
		Entity attacker = source.getEntity();
		if (attacker == null || !attacker.is(EntityTypeTags.UNDEAD)) {
			return damage;
		}
		float reduction = Mth.clamp(GearExpansionConfig.get().silverUndeadProtection * pieces, 0, 100) / 100.0F;
		return damage * (1.0F - reduction);
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		if (attacker.is(EntityTypeTags.UNDEAD)) {
			attacker.knockback(1.5, defender.getX() - attacker.getX(), defender.getZ() - attacker.getZ(), source, damage);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.hallowed", GearExpansionConfig.get().silverUndeadDamageBonus).withStyle(ChatFormatting.AQUA));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.undead_knockback").withStyle(ChatFormatting.AQUA));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.warding", GearExpansionConfig.get().silverUndeadProtection).withStyle(ChatFormatting.AQUA));
		}
	}
}
