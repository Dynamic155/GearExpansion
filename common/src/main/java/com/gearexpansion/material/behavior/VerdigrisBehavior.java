package com.gearexpansion.material.behavior;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.equipment.Equippable;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.registry.ModComponents;

/**
 * Verdigris: copper gear that oxidizes like copper blocks while it's used, from Fresh through
 * Exposed and Weathered to Oxidized. Fresh gear is quickest; oxidized gear is slower but tougher
 * and sometimes shrugs off wear. Clicking honeycomb onto it in the inventory waxes it (it stops
 * changing); clicking an axe onto it scrapes off the wax, or else turns it back one stage.
 */
public final class VerdigrisBehavior implements GearBehavior {
	public static final int FRESH = 0;
	public static final int OXIDIZED = 3;
	public static final String[] STAGES = {"fresh", "exposed", "weathered", "oxidized"};

	// Changes from the fresh stats, per stage.
	private static final float[] MINING_SPEED = {1.0F, 0.9F, 0.8F, 0.7F};
	private static final double[] ATTACK_SPEED = {0.0, -0.1, -0.2, -0.3};
	private static final double[] ARMOR = {0.0, 0.0, 1.0, 1.0};
	private static final double[] TOUGHNESS = {0.0, 0.0, 0.5, 1.0};
	/** Chance fully oxidized gear ignores a point of wear. */
	private static final float OXIDIZED_DURABILITY_SAVING = 0.3F;

	public static int stage(ItemStack stack) {
		return stack.getOrDefault(ModComponents.OXIDATION.get(), FRESH);
	}

	public static boolean waxed(ItemStack stack) {
		return stack.getOrDefault(ModComponents.WAXED.get(), false);
	}

	@Override
	public void inventoryTick(MaterialSet set, ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		// Only gear in use oxidizes: held, worn, or in the offhand.
		if (slot == null || waxed(stack) || stage(stack) >= OXIDIZED) {
			return;
		}
		int ticksPerStage = Math.max(1, GearExpansionConfig.get().verdigrisMinutesPerStage) * 60 * 20;
		if (level.getRandom().nextInt(ticksPerStage) == 0) {
			setStage(stack, stage(stack) + 1);
			level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.PLAYERS, 0.5F, 0.8F);
		}
	}

	/** Sets the stage and the stats that go with it. */
	public static void setStage(ItemStack stack, int stage) {
		if (stage == FRESH) {
			stack.remove(ModComponents.OXIDATION.get());
		} else {
			stack.set(ModComponents.OXIDATION.get(), stage);
		}

		// The item's own (fresh) stats, before any stage changes.
		var defaults = stack.getItem().components();
		ItemAttributeModifiers attributes = defaults.get(DataComponents.ATTRIBUTE_MODIFIERS);
		Tool tool = defaults.get(DataComponents.TOOL);
		Equippable equippable = defaults.get(DataComponents.EQUIPPABLE);

		if (attributes != null) {
			var id = GearExpansion.id("oxidation");
			if (equippable != null && equippable.slot().isArmor()) {
				EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(equippable.slot());
				if (ARMOR[stage] != 0) {
					attributes = attributes.withModifierAdded(Attributes.ARMOR, new AttributeModifier(id, ARMOR[stage], AttributeModifier.Operation.ADD_VALUE), group);
				}
				if (TOUGHNESS[stage] != 0) {
					attributes = attributes.withModifierAdded(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, TOUGHNESS[stage], AttributeModifier.Operation.ADD_VALUE), group);
				}
			} else if (ATTACK_SPEED[stage] != 0) {
				attributes = attributes.withModifierAdded(Attributes.ATTACK_SPEED, new AttributeModifier(id, ATTACK_SPEED[stage], AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
			}
			stack.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
		}
		if (tool != null) {
			float factor = MINING_SPEED[stage];
			List<Tool.Rule> rules = tool.rules().stream()
				.map(rule -> new Tool.Rule(rule.blocks(), rule.speed().map(speed -> speed * factor), rule.correctForDrops()))
				.toList();
			stack.set(DataComponents.TOOL, new Tool(rules, tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative()));
		}
	}

	@Override
	public int modifyDurabilityLoss(MaterialSet set, ItemStack stack, int amount, LivingEntity user, ServerLevel level) {
		if (stage(stack) == OXIDIZED && level.getRandom().nextFloat() < OXIDIZED_DURABILITY_SAVING) {
			return 0;
		}
		return amount;
	}

	@Override
	public boolean onStackedOn(MaterialSet set, ItemStack stack, ItemStack other, Player player) {
		if (other.is(Items.HONEYCOMB) && !waxed(stack)) {
			stack.set(ModComponents.WAXED.get(), true);
			other.consume(1, player);
			player.playSound(SoundEvents.HONEYCOMB_WAX_ON, 1.0F, 1.0F);
			return true;
		}
		if (other.is(ItemTags.AXES) && (waxed(stack) || stage(stack) > FRESH)) {
			if (waxed(stack)) {
				stack.remove(ModComponents.WAXED.get());
				player.playSound(SoundEvents.AXE_WAX_OFF.value(), 1.0F, 1.0F);
			} else {
				setStage(stack, stage(stack) - 1);
				player.playSound(SoundEvents.AXE_SCRAPE.value(), 1.0F, 1.0F);
			}
			if (player.level() instanceof ServerLevel level) {
				other.hurtAndBreak(1, level, player instanceof ServerPlayer serverPlayer ? serverPlayer : null, item -> { });
			}
			return true;
		}
		return false;
	}

	@Override
	public String textureVariant(MaterialSet set, ItemStack stack) {
		int stage = stage(stack);
		return stage == FRESH ? "" : "_" + STAGES[stage];
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		Component stage = Component.translatable("trait.gearexpansion.oxidation." + STAGES[stage(stack)]);
		lines.add(Component.translatable(waxed(stack) ? "trait.gearexpansion.oxidation_waxed" : "trait.gearexpansion.oxidation", stage)
			.withStyle(ChatFormatting.DARK_AQUA));
		lines.add(Component.translatable("trait.gearexpansion.oxidation_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
