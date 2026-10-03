package com.gearexpansion.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.material.MaterialSet;

/**
 * A tool or weapon of one material. Its stats come from the item properties; anything special
 * (bonus damage, effects on hit, oxidizing) is handed to the material's {@code GearBehavior}.
 */
public class GearToolItem extends Item {
	private final MaterialSet material;

	public GearToolItem(Item.Properties properties, MaterialSet material) {
		super(properties);
		this.material = material;
	}

	@Override
	public float getAttackDamageBonus(Entity victim, float damage, DamageSource source) {
		ItemStack weapon = source.getWeaponItem();
		float bonus = weapon == null ? 0.0F : material.behavior.attackDamageBonus(material, weapon, victim, damage, source)
			+ GearWeapons.backstabBonus(weapon, victim, damage, source.getEntity());
		return super.getAttackDamageBonus(victim, damage, source) + bonus;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult result = super.useOn(context);
		material.behavior.afterUseOn(material, context, result);
		return result;
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		material.behavior.onHurtEnemy(material, stack, target, attacker);
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carried) {
		return clickAction == ClickAction.SECONDARY && !other.isEmpty() && material.behavior.onStackedOn(material, stack, other, player);
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		super.inventoryTick(stack, level, owner, slot);
		material.behavior.inventoryTick(material, stack, level, owner, slot);
	}
}
