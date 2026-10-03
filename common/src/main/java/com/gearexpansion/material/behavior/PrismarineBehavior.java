package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/**
 * Prismarine: Tidal weapons hit sea creatures harder (like Impaling), Gills armor breathes and mines
 * underwater (from its attribute bonuses), and the Spined shield pricks melee attackers.
 */
public final class PrismarineBehavior implements GearBehavior {
	@Override
	public float attackDamageBonus(MaterialSet set, ItemStack weapon, Entity victim, float damage, DamageSource source) {
		return set.isWeapon(weapon) && victim.is(EntityTypeTags.AQUATIC) ? GearExpansionConfig.get().prismarineAquaticDamageBonus : 0.0F;
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		int thorns = GearExpansionConfig.get().prismarineShieldThorns;
		// Only melee: the attacker hit the shield directly, not with an arrow.
		if (thorns > 0 && source.getDirectEntity() == attacker && defender.level() instanceof ServerLevel level) {
			attacker.hurtServer(level, level.damageSources().thorns(defender), thorns);
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.tidal", config.prismarineAquaticDamageBonus).withStyle(ChatFormatting.DARK_AQUA));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.spined", config.prismarineShieldThorns).withStyle(ChatFormatting.DARK_AQUA));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
			lines.add(Component.translatable("trait.gearexpansion.gills").withStyle(ChatFormatting.DARK_AQUA));
		}
	}
}
