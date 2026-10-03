package com.gearexpansion.material;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Special behavior shared by all of one material's gear, e.g. Emerald weapons hitting illagers
 * harder or Infernium pickaxes smelting what they mine. Every hook is optional.
 *
 * <p>Hooks receive the item stack, so a behavior can tell tools, armor, and shields apart
 * with the material's fields (e.g. {@code stack.is(set.pickaxe.get())}).
 */
public interface GearBehavior {
	GearBehavior NONE = new GearBehavior() {
	};

	/** Extra damage a weapon deals to {@code victim}, on top of its attack damage. */
	default float attackDamageBonus(MaterialSet set, ItemStack weapon, Entity victim, float damage, DamageSource source) {
		return 0.0F;
	}

	/** After a weapon hits {@code target}. Server side. */
	default void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
	}

	/** Every tick for gear in an inventory or equipment slot. Server side. */
	default void inventoryTick(MaterialSet set, ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
	}

	/** After a tool of this material was used on a block (tilling, path-making, stripping, and so on). */
	default void afterUseOn(MaterialSet set, UseOnContext context, InteractionResult result) {
	}

	/** After a player breaks a block with one of this material's tools. */
	default void onBlockBroken(MaterialSet set, ServerPlayer player, ItemStack tool, BlockState state, BlockPos pos) {
	}

	/** Changes what a block drops when mined with one of this material's tools. */
	default List<ItemStack> modifyDrops(MaterialSet set, ItemStack tool, List<ItemStack> drops, ServerLevel level, BlockState state, BlockPos pos) {
		return drops;
	}

	/** When a shield of this material blocks an attack from {@code attacker}. */
	default void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
	}

	/**
	 * Changes damage taken by someone wearing {@code pieces} of this material's armor (1 to 4).
	 * Runs before armor and enchantments reduce it.
	 */
	default float modifyWearerDamage(MaterialSet set, LivingEntity wearer, DamageSource source, float damage, int pieces) {
		return damage;
	}

	/**
	 * When the player clicks {@code other} onto one of this material's items in an inventory.
	 * Return true if handled, e.g. waxing with honeycomb. Both loaders call this on both sides.
	 */
	default boolean onStackedOn(MaterialSet set, ItemStack stack, ItemStack other, Player player) {
		return false;
	}

	/** Changes durability loss for one of this material's items, after set bonuses. */
	default int modifyDurabilityLoss(MaterialSet set, ItemStack stack, int amount, LivingEntity user, ServerLevel level) {
		return amount;
	}

	/**
	 * A suffix for the 3D model's texture, so one item can change looks, e.g. Verdigris returns
	 * "_weathered" to use {@code <material>_armor_weathered.png}. Empty for the normal texture.
	 */
	default String textureVariant(MaterialSet set, ItemStack stack) {
		return "";
	}

	/** Extra tooltip lines for this material's gear. */
	default void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
	}
}
