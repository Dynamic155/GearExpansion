package com.gearexpansion.item;

import java.util.Map;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.LootEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.material.behavior.MagicalGirlBehavior;
import com.gearexpansion.material.behavior.VerdantiteBehavior;
import com.gearexpansion.setbonus.SetBonuses;

/** Event listeners for gear that don't belong to a single item class. */
public final class GearEvents {
	private GearEvents() {
	}

	public static void init() {
		// Saplings the verdantite axe queued while breaking logs.
		TickEvent.SERVER_POST.register(server -> VerdantiteBehavior.plantPending());
		// Projectiles the magical girl shield blocked, sent back once vanilla's own bounce is done.
		TickEvent.SERVER_POST.register(server -> MagicalGirlBehavior.reflectPending());

		BlockEvent.BREAK.register((level, pos, state, player) -> {
			ItemStack tool = player.getMainHandItem();
			// A scythe harvests and replants grown crops instead of breaking them.
			if (level instanceof ServerLevel serverLevel && GearWeapons.reap(serverLevel, player, tool, pos, state)) {
				return EventResult.interruptFalse();
			}
			ModMaterials.ofGear(tool).ifPresent(set -> set.behavior.onBlockBroken(set, player, tool, state, pos));
			SetBonuses.onBlockBroken(player, state, pos);

			// Infernium ore is hot: mining it without Fire Resistance sets you alight briefly.
			var infernium = ModMaterials.INFERNIUM;
			if (infernium.ore != null && state.is(infernium.ore.get()) && !player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
				player.igniteForSeconds(3.0F);
			}
			return EventResult.pass();
		});

		// Infernium upgrade templates are found in the Nether, like netherite's.
		LootEvent.MODIFY_LOOT_TABLE.register((registries, key, context, builtin) -> {
			Float chance = TEMPLATE_CHANCES.get(key);
			if (builtin && chance != null && ModMaterials.INFERNIUM.upgradeTemplate != null) {
				context.addPool(LootPool.lootPool()
					.setRolls(ContextIntProviders.exactly(1))
					.add(LootItem.lootTableItem(ModMaterials.INFERNIUM.upgradeTemplate.get()))
					.when(LootItemRandomChanceCondition.randomChance(chance)));
			}
		});
	}

	/** Chance of an Infernium Upgrade template in each chest. Bastion treasure rooms are the best bet. */
	private static final Map<ResourceKey<LootTable>, Float> TEMPLATE_CHANCES = Map.of(
		BuiltInLootTables.BASTION_TREASURE, 0.5F,
		BuiltInLootTables.BASTION_OTHER, 0.1F,
		BuiltInLootTables.BASTION_BRIDGE, 0.1F,
		BuiltInLootTables.BASTION_HOGLIN_STABLE, 0.1F,
		BuiltInLootTables.NETHER_BRIDGE, 0.08F
	);
}
