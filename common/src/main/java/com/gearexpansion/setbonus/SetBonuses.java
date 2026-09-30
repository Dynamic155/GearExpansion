package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Optional;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/** The set bonus for every material, plus the hooks that drive them. */
public final class SetBonuses {
	public static final SetBonus ZINC = new ZincSetBonus();
	public static final SetBonus ALUMINUM = new AluminumSetBonus();
	public static final SetBonus TITANIUM = new TitaniumSetBonus();

	public static final List<SetBonus> ALL = List.of(ZINC, ALUMINUM, TITANIUM);

	private SetBonuses() {
	}

	public static void init() {
		TickEvent.PLAYER_POST.register(player -> {
			if (player instanceof ServerPlayer serverPlayer) {
				for (SetBonus bonus : ALL) {
					boolean active = bonus.isActive(serverPlayer);
					if (active) {
						bonus.tick(serverPlayer);
					}
					syncAttributes(serverPlayer, bonus, active);
				}
			}
		});
	}

	/** Called from the durability mixin whenever an item held or worn by {@code wearer} is about to lose durability. */
	public static int modifyDurabilityLoss(int amount, LivingEntity wearer, ServerLevel level) {
		if (amount <= 0 || wearer == null) {
			return amount;
		}
		for (SetBonus bonus : ALL) {
			if (bonus.isActive(wearer)) {
				amount = bonus.modifyDurabilityLoss(amount, wearer, level);
			}
		}
		return amount;
	}

	/** Called from the effect mixin whenever a potion effect is about to be applied to {@code entity}. */
	public static MobEffectInstance modifyNewEffect(MobEffectInstance effect, LivingEntity entity) {
		for (SetBonus bonus : ALL) {
			if (bonus.isActive(entity)) {
				effect = bonus.modifyNewEffect(effect, entity);
			}
		}
		return effect;
	}

	/** Adds a bonus's attribute modifiers while it's active and removes them once it isn't. */
	private static void syncAttributes(ServerPlayer player, SetBonus bonus, boolean active) {
		for (SetBonus.AttributeBonus attribute : bonus.attributeBonuses()) {
			AttributeInstance instance = player.getAttribute(attribute.attribute());
			if (instance == null) {
				continue;
			}
			AttributeModifier current = instance.getModifier(attribute.id());
			if (active && attribute.amount() != 0) {
				if (current == null || current.amount() != attribute.amount()) {
					instance.addOrUpdateTransientModifier(attribute.modifier());
				}
			} else if (current != null) {
				instance.removeModifier(attribute.id());
			}
		}
	}

	public static Optional<SetBonus> forPiece(ItemStack stack) {
		return ALL.stream().filter(bonus -> bonus.isPiece(stack)).findFirst();
	}
}
