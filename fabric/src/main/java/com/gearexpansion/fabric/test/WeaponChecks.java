package com.gearexpansion.fabric.test;

import java.util.List;

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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;

import com.gearexpansion.item.GearWeapons;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/** Daggers (Backstab) and scythes (Wide Sweep and Reaping), checked with titanium ones. */
final class WeaponChecks {
	// The player stands at 100 -60 4 facing north; the targets stand in front of and around them.
	private static final BlockPos TARGET = new BlockPos(100, -60, 2);
	private static final BlockPos SIDE = new BlockPos(102, -60, 3);
	private static final BlockPos BEHIND = new BlockPos(100, -60, 6);
	private static final BlockPos CROPS = new BlockPos(106, -60, 0);

	private WeaponChecks() {
	}

	static void run(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		MaterialSet titanium = ModMaterials.TITANIUM;
		server.runCommand("tp @p 100 -60 4 180 0");
		server.runCommand("fill 96 -60 -2 110 -57 8 minecraft:air");
		server.runCommand("fill 96 -61 -2 110 -61 8 minecraft:grass_block");
		ctx.waitTicks(2);

		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			ServerLevel level = s.overworld();
			ItemStack sword = new ItemStack(titanium.sword.get());
			ItemStack dagger = new ItemStack(titanium.dagger.get());
			ItemStack scythe = new ItemStack(titanium.scythe.get());
			checker.check(modifier(dagger, Attributes.ATTACK_SPEED) > modifier(sword, Attributes.ATTACK_SPEED)
				&& modifier(dagger, Attributes.ATTACK_DAMAGE) < modifier(sword, Attributes.ATTACK_DAMAGE),
				"daggers hit lighter but faster than swords");
			checker.check(modifier(scythe, Attributes.ATTACK_SPEED) < modifier(sword, Attributes.ATTACK_SPEED)
				&& modifier(scythe, Attributes.ATTACK_DAMAGE) > modifier(sword, Attributes.ATTACK_DAMAGE),
				"scythes hit harder but slower than swords");
			checker.check(modifier(scythe, Attributes.ENTITY_INTERACTION_RANGE) > 0, "scythes reach further");
			checker.check(dagger.is(GearWeapons.DAGGERS) && scythe.is(GearWeapons.SCYTHES), "daggers and scythes are tagged");
			checker.check(supports(level, Enchantments.SHARPNESS, dagger) && supports(level, Enchantments.LOOTING, dagger)
				&& supports(level, Enchantments.UNBREAKING, dagger), "daggers take sword enchantments");
			checker.check(supports(level, Enchantments.SWEEPING_EDGE, scythe) && !supports(level, Enchantments.SWEEPING_EDGE, dagger),
				"scythes take Sweeping Edge, daggers don't");
			checker.check(ModMaterials.SILVER.isWeapon(new ItemStack(ModMaterials.SILVER.dagger.get()))
				&& ModMaterials.SILVER.isWeapon(new ItemStack(ModMaterials.SILVER.scythe.get())),
				"material weapon traits apply to daggers and scythes");

			// Backstab: the same hit deals more from behind.
			player.setItemSlot(EquipmentSlot.MAINHAND, dagger);
			Husk husk = husk(level, TARGET);
			DamageSource source = level.damageSources().playerAttack(player);
			husk.setYBodyRot(0.0F);
			float front = titanium.dagger.get().getAttackDamageBonus(husk, 10.0F, source);
			husk.setYBodyRot(180.0F);
			float back = titanium.dagger.get().getAttackDamageBonus(husk, 10.0F, source);
			checker.check(front == 0.0F && back == 5.0F, "daggers deal 50% more damage from behind (front " + front + ", back " + back + ")");
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(titanium.sword.get()));
			checker.check(titanium.sword.get().getAttackDamageBonus(husk, 10.0F, source) == 0.0F, "swords don't backstab");
			husk.discard();

			// Scythe sweep: hits a mob beside the player that a sword's sweep would miss, but not one behind.
			player.setItemSlot(EquipmentSlot.MAINHAND, scythe);
			husk(level, TARGET);
			husk(level, SIDE);
			husk(level, BEHIND);

			// Grown wheat on farmland, with one young plant that should be left alone.
			for (BlockPos pos : BlockPos.betweenClosed(CROPS.offset(-1, 0, -1), CROPS.offset(1, 0, 1))) {
				level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(pos, ((CropBlock) Blocks.WHEAT).getStateForAge(7));
			}
			level.setBlockAndUpdate(CROPS.offset(1, 0, 1), ((CropBlock) Blocks.WHEAT).getStateForAge(3));
		});
		// Let the attack cooldown fill and the husks settle.
		ctx.waitTicks(30);

		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			ServerLevel level = s.overworld();
			Husk target = huskAt(level, TARGET);
			player.attack(target);
			checker.check(target.getHealth() < target.getMaxHealth(), "the scythe hits its target");
			checker.check(huskAt(level, SIDE).getHealth() < 20.0F, "the scythe's sweep hits a mob beside the player");
			checker.check(huskAt(level, BEHIND).getHealth() == 20.0F, "the scythe's sweep misses mobs behind the player");

			// Reaping.
			ItemStack scythe = player.getMainHandItem();
			int damageBefore = scythe.getDamageValue();
			player.gameMode.destroyBlock(CROPS);
			long replanted = BlockPos.betweenClosedStream(CROPS.offset(-1, 0, -1), CROPS.offset(1, 0, 1))
				.filter(pos -> level.getBlockState(pos).is(Blocks.WHEAT) && ((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(pos)) == 0)
				.count();
			checker.check(replanted == 8, "the scythe harvests and replants the 8 grown crops around it (replanted " + replanted + ")");
			checker.check(((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(CROPS.offset(1, 0, 1))) == 3, "reaping leaves young crops alone");
			int wheat = level.getEntitiesOfClass(ItemEntity.class, new AABB(CROPS).inflate(3)).stream()
				.filter(item -> item.getItem().is(Items.WHEAT)).mapToInt(item -> item.getItem().getCount()).sum();
			checker.check(wheat == 8, "reaping drops the wheat (dropped " + wheat + ")");
			checker.check(scythe.getDamageValue() == damageBefore + 1, "reaping costs the scythe one durability");
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		});

		server.runCommand("kill @e[type=husk]");
		server.runCommand("kill @e[type=item]");
		server.runCommand("fill 96 -60 -2 110 -57 8 minecraft:air");
		server.runCommand("fill 96 -61 -2 110 -61 8 minecraft:grass_block");
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

	private static Husk husk(ServerLevel level, BlockPos pos) {
		Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.setNoAi(true);
		husk.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(husk);
		return husk;
	}

	private static Husk huskAt(ServerLevel level, BlockPos pos) {
		List<Husk> husks = level.getEntitiesOfClass(Husk.class, new AABB(pos).inflate(0.4));
		return husks.getFirst();
	}
}
