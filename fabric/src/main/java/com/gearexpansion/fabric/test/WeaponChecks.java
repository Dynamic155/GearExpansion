package com.gearexpansion.fabric.test;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import com.gearexpansion.item.GearWeapons;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/** Daggers and their Backstab, checked with a titanium one. */
final class WeaponChecks {
	// The player stands at 100 -60 4 facing north; the target stands in front of them.
	private static final BlockPos TARGET = new BlockPos(100, -60, 2);

	private WeaponChecks() {
	}

	static void run(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		MaterialSet titanium = ModMaterials.TITANIUM;
		server.runCommand("tp @p 100 -60 4 180 0");
		ctx.waitTicks(2);

		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			ServerLevel level = s.overworld();
			ItemStack sword = new ItemStack(titanium.sword.get());
			ItemStack dagger = new ItemStack(titanium.dagger.get());
			checker.check(modifier(dagger, Attributes.ATTACK_SPEED) > modifier(sword, Attributes.ATTACK_SPEED)
				&& modifier(dagger, Attributes.ATTACK_DAMAGE) < modifier(sword, Attributes.ATTACK_DAMAGE),
				"daggers hit lighter but faster than swords");
			checker.check(dagger.is(GearWeapons.DAGGERS), "daggers are tagged");
			checker.check(supports(level, Enchantments.SHARPNESS, dagger) && supports(level, Enchantments.LOOTING, dagger)
				&& supports(level, Enchantments.UNBREAKING, dagger), "daggers take sword enchantments");
			checker.check(!supports(level, Enchantments.SWEEPING_EDGE, dagger), "daggers don't take Sweeping Edge");
			checker.check(ModMaterials.SILVER.isWeapon(new ItemStack(ModMaterials.SILVER.dagger.get())), "material weapon traits apply to daggers");

			// Backstab: the same hit deals more from behind.
			player.setItemSlot(EquipmentSlot.MAINHAND, dagger);
			Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			husk.setNoAi(true);
			husk.snapTo(TARGET.getX() + 0.5, TARGET.getY(), TARGET.getZ() + 0.5, 0.0F, 0.0F);
			level.addFreshEntity(husk);
			DamageSource source = level.damageSources().playerAttack(player);
			husk.setYBodyRot(0.0F);
			float front = titanium.dagger.get().getAttackDamageBonus(husk, 10.0F, source);
			husk.setYBodyRot(180.0F);
			float back = titanium.dagger.get().getAttackDamageBonus(husk, 10.0F, source);
			checker.check(front == 0.0F && back == 5.0F, "daggers deal 50% more damage from behind (front " + front + ", back " + back + ")");
			player.setItemSlot(EquipmentSlot.MAINHAND, sword);
			checker.check(titanium.sword.get().getAttackDamageBonus(husk, 10.0F, source) == 0.0F, "swords don't backstab");
			husk.discard();
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		});
		checker.check(GearGameTest.tooltip(ctx, titanium.dagger.get()).contains("Backstab"), "dagger tooltip shows Backstab");
		server.runCommand("tp @p 0 -60 0 0 0");
	}

	private static double modifier(ItemStack stack, Holder<Attribute> attribute) {
		var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
		return modifiers == null ? 0 : modifiers.modifiers().stream()
			.filter(entry -> entry.attribute().equals(attribute))
			.mapToDouble(entry -> entry.modifier().amount())
			.sum();
	}

	private static boolean supports(ServerLevel level, ResourceKey<Enchantment> key, ItemStack stack) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key).value().isSupportedItem(stack);
	}
}
