package com.gearexpansion.item;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.setbonus.SetBonus;
import com.gearexpansion.setbonus.SetBonuses;

/** Combat hooks called from {@code LivingEntityCombatMixin}: damage taken and shield blocks. */
public final class GearCombat {
	private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	private GearCombat() {
	}

	/** Whether a set bonus stops this hit entirely. */
	public static boolean cancelsDamage(LivingEntity entity, ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || entity.isInvulnerableTo(level, source)) {
			return false;
		}
		for (SetBonus bonus : SetBonuses.ALL) {
			if (bonus.isActive(entity) && bonus.cancelsDamage(entity, level, source, damage)) {
				return true;
			}
		}
		return false;
	}

	/** Lets each material the entity wears change incoming damage, e.g. Infernium armor resisting fire. */
	public static float modifyIncomingDamage(LivingEntity entity, DamageSource source, float damage) {
		for (MaterialSet set : ModMaterials.ALL) {
			int pieces = piecesWorn(entity, set);
			if (pieces > 0) {
				damage = set.behavior.modifyWearerDamage(set, entity, source, damage, pieces);
			}
		}
		for (SetBonus bonus : SetBonuses.ALL) {
			if (bonus.isActive(entity)) {
				damage = bonus.modifyIncomingDamage(entity, source, damage);
			}
		}
		if (source.getEntity() instanceof LivingEntity attacker && attacker != entity) {
			for (SetBonus bonus : SetBonuses.ALL) {
				if (bonus.isActive(attacker)) {
					bonus.onAttack(attacker, entity, source, damage);
				}
			}
		}
		return damage;
	}

	/** After {@code defender} blocks an attack from {@code attacker} with a shield. */
	public static void onShieldBlock(LivingEntity defender, LivingEntity attacker, DamageSource source, float damage) {
		ItemStack shield = defender.getItemBlockingWith();
		if (shield != null) {
			ModMaterials.ofGear(shield).ifPresent(set -> set.behavior.onShieldBlock(set, defender, attacker, shield, source, damage));
		}
		for (SetBonus bonus : SetBonuses.ALL) {
			if (bonus.isActive(defender)) {
				bonus.onShieldBlock(defender, attacker, source, damage);
			}
		}
	}

	/** How many armor pieces of {@code set} the entity is wearing. */
	public static int piecesWorn(LivingEntity entity, MaterialSet set) {
		int pieces = 0;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack stack = entity.getItemBySlot(slot);
			if (!stack.isEmpty() && set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()))) {
				pieces++;
			}
		}
		return pieces;
	}
}
