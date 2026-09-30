package com.gearexpansion.fabric.test;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.client.GearExpansionClient;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.worldgen.ModOres;

/**
 * End-to-end checks for Titanium in a real game: registration, recipes, mining tiers,
 * ore generation, the set bonus, tooltips, and screenshots of the 3D armor and shield.
 *
 * <p>Run with {@code ./gradlew :fabric:runGameTest}. Screenshots are saved to
 * {@code fabric/build/gametest/screenshots}. Failures are collected and reported together.
 */
public final class TitaniumGameTest implements FabricClientGameTest {
	private final List<String> failures = new ArrayList<>();
	private final MaterialSet titanium = ModMaterials.TITANIUM;

	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamemode survival @p");
			ctx.waitTicks(20);

			server.runOnServer(this::checkRecipes);
			server.runOnServer(this::checkMiningTiers);
			server.runOnServer(this::checkOreInBiomes);
			checkOreFeaturePlaces(ctx, server);
			server.runOnServer(this::checkDurabilitySaving);
			checkResistance(ctx, server);
			checkTooltip(ctx, server);
			screenshots(ctx, server);
		}

		if (failures.isEmpty()) {
			GearExpansion.LOGGER.info("[GameTest] All Titanium checks passed");
		} else {
			failures.forEach(failure -> GearExpansion.LOGGER.error("[GameTest] FAILED: {}", failure));
			throw new AssertionError(failures.size() + " Titanium check(s) failed: " + failures);
		}
	}

	private void check(boolean condition, String description) {
		if (condition) {
			GearExpansion.LOGGER.info("[GameTest] ok: {}", description);
		} else {
			failures.add(description);
		}
	}

	private void checkRecipes(MinecraftServer server) {
		for (String recipe : List.of("titanium_sword", "titanium_pickaxe", "titanium_spear", "titanium_chestplate", "titanium_shield",
				"titanium_ingot_from_blasting_raw_titanium", "titanium_block")) {
			var key = ResourceKey.create(Registries.RECIPE, GearExpansion.id(recipe));
			check(server.getRecipeManager().byKey(key).isPresent(), "recipe exists: " + recipe);
		}
	}

	private void checkMiningTiers(MinecraftServer server) {
		var ore = titanium.deepslateOre.get().defaultBlockState();
		check(!new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(ore), "iron pickaxe cannot mine titanium ore");
		check(new ItemStack(Items.DIAMOND_PICKAXE).isCorrectToolForDrops(ore), "diamond pickaxe can mine titanium ore");
		check(new ItemStack(titanium.pickaxe.get()).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "titanium pickaxe mines obsidian (diamond tier)");
		check(new ItemStack(titanium.pickaxe.get()).isCorrectToolForDrops(ore), "titanium pickaxe mines titanium ore");
	}

	private void checkOreInBiomes(MinecraftServer server) {
		var features = server.overworld().getBiome(BlockPos.ZERO).value().getGenerationSettings().features();
		var ores = features.get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
		check(ores.stream().anyMatch(feature -> feature.is(ModOres.placedKey(titanium))), "titanium ore is added to Overworld biomes");
	}

	/** Fills a block of deepslate underground and places one titanium vein in it. */
	private void checkOreFeaturePlaces(ClientGameTestContext ctx, TestServerContext server) {
		server.runCommand("forceload add 100 100");
		ctx.waitTicks(20);
		server.runCommand("fill 100 -40 100 115 -25 115 minecraft:deepslate");
		ctx.waitTicks(5);
		for (int i = 0; i < 6; i++) {
			server.runCommand("place feature gearexpansion:ore_titanium 107 -32 107");
		}
		ctx.waitTicks(5);
		int found = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			int count = 0;
			for (BlockPos pos : BlockPos.betweenClosed(100, -40, 100, 115, -25, 115)) {
				if (level.getBlockState(pos).is(titanium.deepslateOre.get())) {
					count++;
				}
			}
			return count;
		});
		GearExpansion.LOGGER.info("[GameTest] placed titanium veins produced {} ore blocks", found);
		check(found > 0, "the titanium ore feature generates deepslate titanium ore");
	}

	private void checkDurabilitySaving(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		ServerLevel level = player.level();

		int withoutSet = damageTaken(player, level, 400);
		equipSet(player);
		int withSet = damageTaken(player, level, 400);
		unequipSet(player);

		GearExpansion.LOGGER.info("[GameTest] 400 uses cost {} durability without the set and {} with it", withoutSet, withSet);
		check(withoutSet == 400, "without the set, every use costs durability");
		check(withSet > 120 && withSet < 280, "with the full set, gear loses about 50% less durability");
	}

	private static int damageTaken(ServerPlayer player, ServerLevel level, int uses) {
		ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
		for (int i = 0; i < uses; i++) {
			pickaxe.hurtAndBreak(1, level, player, item -> { });
		}
		return pickaxe.getDamageValue();
	}

	private void checkResistance(ClientGameTestContext ctx, TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			equipSet(player);
			player.setHealth(4.0F);
		});
		ctx.waitTicks(5);
		boolean resisted = server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().hasEffect(MobEffects.RESISTANCE));
		check(resisted, "dropping below 30% health with the full set grants Resistance");
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.removeAllEffects();
			player.setHealth(player.getMaxHealth());
		});
	}

	private void checkTooltip(ClientGameTestContext ctx, TestServerContext server) {
		ctx.waitTicks(5);
		String tooltip = ctx.computeOnClient(mc -> {
			ItemStack helmet = new ItemStack(titanium.helmet.get());
			StringBuilder text = new StringBuilder();
			helmet.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL)
				.forEach(line -> text.append(line.getString()).append('\n'));
			return text.toString();
		});
		GearExpansion.LOGGER.info("[GameTest] helmet tooltip:\n{}", tooltip);
		check(tooltip.contains("Titanium Set (4/4)"), "tooltip shows full set progress");
		check(tooltip.contains("Unbreakable Will"), "tooltip describes the set bonus");
	}

	private void screenshots(ClientGameTestContext ctx, TestServerContext server) {
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p 0 -60 0 180 0");
		// A row of the new blocks and a small ore display in front of the player.
		server.runCommand("setblock -2 -60 -3 gearexpansion:titanium_ore");
		server.runCommand("setblock -1 -60 -3 gearexpansion:deepslate_titanium_ore");
		server.runCommand("setblock 0 -60 -3 gearexpansion:raw_titanium_block");
		server.runCommand("setblock 1 -60 -3 gearexpansion:titanium_block");
		server.runCommand("item replace entity @p weapon.offhand with gearexpansion:titanium_shield");
		server.runCommand("item replace entity @p weapon.mainhand with gearexpansion:titanium_sword");
		ctx.waitTicks(20);

		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		ctx.waitTicks(5);
		ctx.takeScreenshot("titanium_blocks_and_gear_first_person");

		server.runCommand("tp @p 0 -60 0 0 0");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		ctx.waitTicks(10);
		ctx.takeScreenshot("titanium_armor_front");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		ctx.waitTicks(10);
		ctx.takeScreenshot("titanium_armor_back");

		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		ctx.getInput().holdKey(options -> options.keyUse);
		ctx.waitTicks(20);
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		ctx.waitTicks(5);
		ctx.takeScreenshot("titanium_shield_blocking_front");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		ctx.waitTicks(5);
		ctx.takeScreenshot("titanium_shield_blocking_first_person");
		ctx.getInput().releaseKey(options -> options.keyUse);

		server.runCommand("item replace entity @p weapon.mainhand with gearexpansion:titanium_spear");
		ctx.waitTicks(10);
		ctx.takeScreenshot("titanium_spear_first_person");

		// Every Titanium item in the hotbar and inventory.
		server.runCommand("clear @p");
		List<String> items = List.of("titanium_sword", "titanium_pickaxe", "titanium_axe", "titanium_shovel", "titanium_hoe",
			"titanium_spear", "titanium_shield", "titanium_ingot", "raw_titanium", "titanium_nugget", "titanium_helmet",
			"titanium_chestplate", "titanium_leggings", "titanium_boots", "titanium_ore", "deepslate_titanium_ore",
			"titanium_block", "raw_titanium_block");
		for (int i = 0; i < items.size(); i++) {
			String slot = i < 9 ? "hotbar." + i : "inventory." + (i - 9);
			server.runCommand("item replace entity @p " + slot + " with gearexpansion:" + items.get(i));
		}
		server.runCommand("gamemode survival @p");
		ctx.waitTicks(10);
		ctx.getInput().pressKey(options -> options.keyInventory);
		ctx.waitTicks(10);
		ctx.takeScreenshot("titanium_inventory");
		ctx.setScreen(() -> null);

		ctx.setScreen(() -> GearExpansionClient.configScreen(null));
		ctx.waitTicks(10);
		ctx.takeScreenshot("config_screen");
		ctx.setScreen(() -> null);
		ctx.waitTicks(5);
	}

	private void equipSet(ServerPlayer player) {
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(titanium.helmet.get()));
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(titanium.chestplate.get()));
		player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(titanium.leggings.get()));
		player.setItemSlot(EquipmentSlot.FEET, new ItemStack(titanium.boots.get()));
	}

	private static void unequipSet(ServerPlayer player) {
		for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
			player.setItemSlot(slot, ItemStack.EMPTY);
		}
	}
}
