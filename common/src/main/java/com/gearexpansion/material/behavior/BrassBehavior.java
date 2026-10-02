package com.gearexpansion.material.behavior;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;

/** Brass: weapons attack faster with each hit in a combo (up to three stacks), like a mechanism gathering pace. */
public final class BrassBehavior implements GearBehavior {
	private static final Identifier COMBO = GearExpansion.id("brass_combo");
	private static final int MAX_STACKS = 3;
	private static final double SPEED_PER_STACK = 0.15;
	/** Ticks without a hit before the combo ends. */
	private static final int COMBO_WINDOW = 60;

	/** Per attacker: game time of the last hit and the current stack count. Not saved. */
	private final Map<UUID, long[]> combos = new ConcurrentHashMap<>();

	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		if (!set.isWeapon(weapon) || !GearExpansionConfig.get().brassCombo) {
			return;
		}
		long now = attacker.level().getGameTime();
		long[] combo = combos.computeIfAbsent(attacker.getUUID(), id -> new long[2]);
		combo[1] = now - combo[0] <= COMBO_WINDOW ? Math.min(MAX_STACKS, combo[1] + 1) : 1;
		combo[0] = now;
		AttributeInstance speed = attacker.getAttribute(Attributes.ATTACK_SPEED);
		if (speed != null) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(COMBO, combo[1] * SPEED_PER_STACK, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	@Override
	public void inventoryTick(MaterialSet set, ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
		if (!(owner instanceof LivingEntity living) || !set.isWeapon(stack)) {
			return;
		}
		long[] combo = combos.get(living.getUUID());
		boolean expired = combo == null || level.getGameTime() - combo[0] > COMBO_WINDOW || slot != EquipmentSlot.MAINHAND;
		AttributeInstance speed = living.getAttribute(Attributes.ATTACK_SPEED);
		if (expired && speed != null && speed.getModifier(COMBO) != null) {
			speed.removeModifier(COMBO);
			combos.remove(living.getUUID());
		}
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		if (set.isWeapon(stack) && GearExpansionConfig.get().brassCombo) {
			lines.add(Component.translatable("trait.gearexpansion.momentum").withStyle(ChatFormatting.GOLD));
		}
	}
}
