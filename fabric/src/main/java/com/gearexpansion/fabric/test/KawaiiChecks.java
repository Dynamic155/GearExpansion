package com.gearexpansion.fabric.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.item.GearWeapons;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.material.behavior.MagicalGirlBehavior;
import com.gearexpansion.material.behavior.PastelPrincessBehavior;
import com.gearexpansion.setbonus.FairyKeiSetBonus;
import com.gearexpansion.setbonus.SetBonuses;

/** The Kawaii sets: Pastel Princess, Jirai Kei, Magical Girl, and Fairy Kei. */
final class KawaiiChecks {
	// The player stands at 130 -60 4 facing north.
	private static final BlockPos PLAYER = new BlockPos(130, -60, 4);

	private KawaiiChecks() {
	}

	static void run(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		server.runCommand("tp @p 130 -60 4 180 0");
		server.runCommand("fill 122 -60 -6 138 -56 12 minecraft:air");
		server.runCommand("fill 122 -61 -6 138 -61 12 minecraft:grass_block");
		ctx.waitTicks(2);

		server.runOnServer(s -> {
			for (MaterialSet set : List.of(ModMaterials.PASTEL_PRINCESS, ModMaterials.JIRAI_KEI, ModMaterials.MAGICAL_GIRL, ModMaterials.FAIRY_KEI)) {
				String alloy = set.ingotName + "_from_alloying";
				checker.check(s.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, GearExpansion.id(alloy))).isPresent(),
					"recipe exists: " + alloy);
			}
			checker.check(new ItemStack(ModMaterials.PASTEL_PRINCESS.helmet.get()).is(ItemTags.PIGLIN_SAFE_ARMOR), "piglins treat pastel princess armor as gold");
		});

		pastelPrincess(ctx, server, checker);
		jiraiKei(ctx, server, checker);
		magicalGirl(ctx, server, checker);
		fairyKei(ctx, server, checker);

		server.runCommand("kill @e[type=!player]");
		server.runCommand("fill 122 -60 -6 138 -56 12 minecraft:air");
		server.runCommand("fill 122 -61 -6 138 -61 12 minecraft:grass_block");
		server.runCommand("tp @p 0 -60 0 0 0");
	}

	private static void pastelPrincess(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		MaterialSet princess = ModMaterials.PASTEL_PRINCESS;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			// Charm, at a sure chance so the test is quick.
			GearExpansionConfig.get().pastelPrincessCharmChance = 100;
			Zombie charmed = spawn(level, EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND), 128, 0);
			Zombie plain = spawn(level, EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND), 132, 0);
			princess.behavior.onHurtEnemy(princess, new ItemStack(princess.sword.get()), charmed, player);
			charmed.setTarget(player);
			plain.setTarget(player);
			checker.check(charmed.getTarget() == null && plain.getTarget() == player, "a charmed mob leaves the princess alone");
			GearExpansionConfig.get().pastelPrincessCharmChance = 25;
			charmed.discard();
			plain.discard();

			// Royal Favor and Royal Court discounts.
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(princess.helmet.get()));
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(princess.boots.get()));
			double two = PastelPrincessBehavior.villagerDiscount(player);
			equip(player, princess);
			double full = PastelPrincessBehavior.villagerDiscount(player);
			checker.check(Math.abs(two - 0.10) < 1e-6 && Math.abs(full - 0.35) < 1e-6,
				"villagers take 5% off per princess piece, and 15% more for the full set (" + two + ", " + full + ")");

			// Royal Court: a tamed wolf and an iron golem nearby are strengthened.
			Wolf wolf = spawn(level, EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND), 127, 6);
			wolf.tame(player);
			spawn(level, EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.COMMAND), 133, 6);
		});
		ctx.waitTicks(25);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			Wolf wolf = level.getEntitiesOfClass(Wolf.class, new AABB(PLAYER).inflate(8)).getFirst();
			IronGolem golem = level.getEntitiesOfClass(IronGolem.class, new AABB(PLAYER).inflate(8)).getFirst();
			checker.check(wolf.hasEffect(MobEffects.STRENGTH) && wolf.hasEffect(MobEffects.REGENERATION), "Royal Court strengthens the princess's pets");
			checker.check(golem.hasEffect(MobEffects.STRENGTH) && golem.hasEffect(MobEffects.REGENERATION), "Royal Court strengthens iron golems");
			wolf.discard();
			golem.discard();
			unequip(player);
		});
		checker.check(GearGameTest.tooltip(ctx, princess.sword.get()).contains("Charm"), "pastel princess weapon tooltip shows Charm");
		checker.check(GearGameTest.tooltip(ctx, princess.chestplate.get()).contains("Royal Court"), "pastel princess armor tooltip shows its set bonus");
	}

	private static void jiraiKei(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		MaterialSet jirai = ModMaterials.JIRAI_KEI;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			// Desperation grows as health drops.
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(jirai.sword.get()));
			Husk dummy = spawn(level, husk(level), 130, 0);
			DamageSource source = level.damageSources().playerAttack(player);
			player.setHealth(player.getMaxHealth());
			float full = jirai.sword.get().getAttackDamageBonus(dummy, 10.0F, source);
			player.setHealth(player.getMaxHealth() / 2);
			float half = jirai.sword.get().getAttackDamageBonus(dummy, 10.0F, source);
			checker.check(full == 0.0F && Math.abs(half - 2.5F) < 1e-4, "Desperation: +25% damage at half health, none at full (" + full + ", " + half + ")");
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			dummy.discard();

			// Landmine: a hit that drops the wearer below a third of their health throws mobs back.
			equip(player, jirai);
			spawn(level, husk(level), 132, 4);
			spawn(level, husk(level), 128, 3);
			player.setHealth(10.0F);
			player.hurtServer(level, level.damageSources().generic(), 5.0F);
		});
		ctx.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			List<Husk> husks = level.getEntitiesOfClass(Husk.class, new AABB(PLAYER).inflate(8));
			checker.check(husks.size() == 2 && husks.stream().allMatch(husk -> husk.getHealth() < husk.getMaxHealth()),
				"the Landmine hurts every mob nearby when the jirai kei wearer drops below a third of their health");
			husks.forEach(Husk::discard);
			unequip(player);
			player.setHealth(player.getMaxHealth());
		});
		checker.check(GearGameTest.tooltip(ctx, jirai.sword.get()).contains("Desperation"), "jirai kei weapon tooltip shows Desperation");
		checker.check(GearGameTest.tooltip(ctx, jirai.chestplate.get()).contains("Landmine"), "jirai kei armor tooltip shows its set bonus");
		ctx.waitTicks(20);
	}

	private static void magicalGirl(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		MaterialSet magical = ModMaterials.MAGICAL_GIRL;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			player.setHealth(player.getMaxHealth());
			player.snapTo(130.5, -60, 4.5, 180.0F, 0.0F);

			// Sparkle Beam: a full swing at full health hits a mob further along the line.
			Husk target = spawn(level, husk(level), 130, 2);
			Husk beyond = spawn(level, husk(level), 130, -2);
			GearWeapons.recordSwing(player, 1.0F);
			magical.behavior.onHurtEnemy(magical, new ItemStack(magical.sword.get()), target, player);
			checker.check(beyond.getHealth() < beyond.getMaxHealth(), "the Sparkle Beam hits mobs in a line ahead");
			target.discard();
			beyond.discard();

			// Barrier: a blocked arrow flies back at whoever shot it, now owned by the defender.
			Husk archer = spawn(level, husk(level), 130, -4);
			Arrow arrow = new Arrow(level, archer, new ItemStack(Items.ARROW), null);
			arrow.setPos(130.5, -59, 3.0);
			arrow.setDeltaMovement(0.0, 0.0, 2.0);
			level.addFreshEntity(arrow);
			magical.behavior.onProjectileBlocked(magical, player, new ItemStack(magical.shield.get()), arrow);
			MagicalGirlBehavior.reflectPending();
			checker.check(arrow.getOwner() == player && arrow.getDeltaMovement().z < -1.0, "the magical girl shield bounces arrows back at the shooter");
			arrow.discard();
			archer.discard();

			// Transform.
			equip(player, magical);
			SetBonuses.useAbility(player);
			checker.check(player.hasEffect(MobEffects.STRENGTH) && player.hasEffect(MobEffects.SPEED)
				&& player.hasEffect(MobEffects.RESISTANCE) && player.hasEffect(MobEffects.SLOW_FALLING),
				"Transform gives Strength, Speed, Resistance, and Slow Falling");
			checker.check(SetBonuses.MAGICAL_GIRL.isTransformed(player), "the magical girl is transformed");
			player.removeAllEffects();
			unequip(player);
		});
		checker.check(GearGameTest.tooltip(ctx, magical.sword.get()).contains("Sparkle Beam"), "magical girl weapon tooltip shows Sparkle Beam");
		checker.check(GearGameTest.tooltip(ctx, magical.shield.get()).contains("Barrier"), "magical girl shield tooltip shows Barrier");
	}

	private static void fairyKei(ClientGameTestContext ctx, TestServerContext server, RecipeViewerChecks.Checker checker) {
		MaterialSet fairy = ModMaterials.FAIRY_KEI;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			Husk target = spawn(level, husk(level), 130, 2);
			fairy.behavior.onHurtEnemy(fairy, new ItemStack(fairy.sword.get()), target, player);
			checker.check(target.hasEffect(MobEffects.LEVITATION), "fairy kei weapons make mobs float");
			target.discard();

			float halved = fairy.behavior.modifyWearerDamage(fairy, player, level.damageSources().fall(), 10.0F, 2);
			float none = fairy.behavior.modifyWearerDamage(fairy, player, level.damageSources().fall(), 10.0F, 4);
			checker.check(halved == 5.0F && none == 0.0F, "each fairy kei piece takes a quarter off fall damage");
			checker.check(fairy.behavior.preventsKnockbackWhileBlocking(fairy, player, new ItemStack(fairy.shield.get())),
				"the fairy kei shield stops knockback while blocking");

			equip(player, fairy);
			((FairyKeiSetBonus) SetBonuses.FAIRY_KEI).doubleJump(player);
			checker.check(player.getDeltaMovement().y > 0.5, "Daydream's double jump launches the player up");
			player.setDeltaMovement(Vec3.ZERO);
			unequip(player);
		});
		checker.check(GearGameTest.tooltip(ctx, fairy.sword.get()).contains("Whimsy"), "fairy kei weapon tooltip shows Whimsy");
		checker.check(GearGameTest.tooltip(ctx, fairy.chestplate.get()).contains("Daydream"), "fairy kei armor tooltip shows its set bonus");
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static Husk husk(ServerLevel level) {
		Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
		husk.setNoAi(true);
		return husk;
	}

	private static <T extends Mob> T spawn(ServerLevel level, T mob, int x, int z) {
		mob.snapTo(x + 0.5, -60, z + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(mob);
		return mob;
	}

	private static void equip(ServerPlayer player, MaterialSet set) {
		set.armorBySlot().forEach((slot, item) -> player.setItemSlot(slot, new ItemStack(item.get())));
	}

	private static void unequip(ServerPlayer player) {
		for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			player.setItemSlot(slot, ItemStack.EMPTY);
		}
	}
}
