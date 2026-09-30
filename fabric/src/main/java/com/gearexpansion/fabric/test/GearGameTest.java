package com.gearexpansion.fabric.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.architectury.registry.registries.RegistrySupplier;

import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.client.GearExpansionClient;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.MaterialSet.OreGeneration;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.registry.ModTabs;
import com.gearexpansion.worldgen.ModOres;

/**
 * End-to-end checks for every material in a real game: recipes, mining tiers, ore generation,
 * material traits, set bonuses, tooltips, creative tabs, and screenshots of the gear.
 *
 * <p>Run with {@code ./gradlew :fabric:runGameTest}. Screenshots are saved to
 * {@code fabric/build/gametest/screenshots}. Failures are collected and reported together.
 */
public final class GearGameTest implements FabricClientGameTest {
	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("gamemode survival @p");
			ctx.waitTicks(20);

			for (MaterialSet set : ModMaterials.ALL) {
				server.runOnServer(s -> checkRecipes(s, set));
				server.runOnServer(s -> checkOreInBiomes(s, set));
			}
			checkOreFeaturesPlace(ctx, server);
			server.runOnServer(this::checkMiningTiers);

			checkZinc(ctx, server);
			checkAluminum(ctx, server);
			server.runOnServer(this::checkTitaniumDurability);
			checkTitaniumResistance(ctx, server);
			checkTooltips(ctx, server);

			screenshots(ctx, server);
		}

		if (failures.isEmpty()) {
			GearExpansion.LOGGER.info("[GameTest] All Gear Expansion checks passed");
		} else {
			failures.forEach(failure -> GearExpansion.LOGGER.error("[GameTest] FAILED: {}", failure));
			throw new AssertionError(failures.size() + " Gear Expansion check(s) failed: " + failures);
		}
	}

	private void check(boolean condition, String description) {
		if (condition) {
			GearExpansion.LOGGER.info("[GameTest] ok: {}", description);
		} else {
			failures.add(description);
		}
	}

	// Every material ------------------------------------------------------------------

	private void checkRecipes(MinecraftServer server, MaterialSet set) {
		for (String recipe : List.of(set.name + "_sword", set.name + "_pickaxe", set.name + "_spear", set.name + "_chestplate",
				set.name + "_shield", set.name + "_ingot_from_blasting_raw_" + set.oreName, set.name + "_block")) {
			var key = ResourceKey.create(Registries.RECIPE, GearExpansion.id(recipe));
			check(server.getRecipeManager().byKey(key).isPresent(), "recipe exists: " + recipe);
		}
	}

	/** Each ore vein is added to a biome from its biome tag. */
	private void checkOreInBiomes(MinecraftServer server, MaterialSet set) {
		var biomes = server.registryAccess().lookupOrThrow(Registries.BIOME);
		for (OreGeneration ore : set.oreGeneration) {
			var biome = biomes.get(ore.biomes()).flatMap(tag -> tag.stream().findFirst());
			boolean added = biome.map(holder -> hasFeature(holder.value(), set, ore)).orElse(false);
			check(added, ModOres.name(set, ore) + " is added to " + ore.biomes().location() + " biomes");
		}
	}

	private static boolean hasFeature(Biome biome, MaterialSet set, OreGeneration ore) {
		var step = biome.getGenerationSettings().features().get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal());
		return step.stream().anyMatch(feature -> feature.is(ModOres.placedKey(set, ore)));
	}

	/** Fills a block of stone (or deepslate for deep ores) and places the main vein in it a few times. */
	private void checkOreFeaturesPlace(ClientGameTestContext ctx, TestServerContext server) {
		int x = 100;
		for (MaterialSet set : ModMaterials.ALL) {
			boolean deep = set.oreGeneration.getFirst().maxY() <= 0;
			int y = deep ? -40 : 20;
			String stone = deep ? "minecraft:deepslate" : "minecraft:stone";
			server.runCommand("forceload add " + x + " 100");
			ctx.waitTicks(20);
			server.runCommand("fill " + x + " " + y + " 100 " + (x + 15) + " " + (y + 15) + " 115 " + stone);
			ctx.waitTicks(5);
			for (int i = 0; i < 6; i++) {
				server.runCommand("place feature gearexpansion:" + ModOres.name(set, set.oreGeneration.getFirst()) + " " + (x + 7) + " " + (y + 8) + " 107");
			}
			ctx.waitTicks(5);
			int minX = x;
			int found = server.computeOnServer(s -> {
				ServerLevel level = s.overworld();
				int count = 0;
				for (BlockPos pos : BlockPos.betweenClosed(minX, y, 100, minX + 15, y + 15, 115)) {
					BlockState state = level.getBlockState(pos);
					if (state.is(set.ore.get()) || state.is(set.deepslateOre.get())) {
						count++;
					}
				}
				return count;
			});
			GearExpansion.LOGGER.info("[GameTest] {} veins produced {} ore blocks", set.name, found);
			check(found > 0, "the " + set.oreName + " ore feature generates ore");
			x += 32;
		}
	}

	private void checkMiningTiers(MinecraftServer server) {
		MaterialSet zinc = ModMaterials.ZINC;
		MaterialSet aluminum = ModMaterials.ALUMINUM;
		MaterialSet titanium = ModMaterials.TITANIUM;
		check(!canMine(Items.WOODEN_PICKAXE, zinc.ore.get().defaultBlockState()), "a wooden pickaxe can't mine zinc ore");
		check(canMine(Items.STONE_PICKAXE, zinc.ore.get().defaultBlockState()), "a stone pickaxe mines zinc ore");
		check(canMine(Items.STONE_PICKAXE, aluminum.ore.get().defaultBlockState()), "a stone pickaxe mines bauxite ore");
		check(!canMine(Items.IRON_PICKAXE, titanium.deepslateOre.get().defaultBlockState()), "an iron pickaxe can't mine titanium ore");
		check(canMine(Items.DIAMOND_PICKAXE, titanium.deepslateOre.get().defaultBlockState()), "a diamond pickaxe mines titanium ore");

		check(canMine(zinc.pickaxe.get(), zinc.ore.get().defaultBlockState()), "a zinc pickaxe mines zinc ore");
		check(canMine(zinc.pickaxe.get(), Blocks.IRON_ORE.defaultBlockState()), "a zinc pickaxe mines iron ore");
		check(!canMine(zinc.pickaxe.get(), Blocks.DIAMOND_ORE.defaultBlockState()), "a zinc pickaxe can't mine diamond ore (copper tier)");
		check(canMine(aluminum.pickaxe.get(), Blocks.DIAMOND_ORE.defaultBlockState()), "an aluminum pickaxe mines diamond ore (iron tier)");
		check(!canMine(aluminum.pickaxe.get(), Blocks.OBSIDIAN.defaultBlockState()), "an aluminum pickaxe can't mine obsidian");
		check(canMine(titanium.pickaxe.get(), Blocks.OBSIDIAN.defaultBlockState()), "a titanium pickaxe mines obsidian (diamond tier)");
	}

	private static boolean canMine(Item pickaxe, BlockState state) {
		return new ItemStack(pickaxe).isCorrectToolForDrops(state);
	}

	// Zinc ----------------------------------------------------------------------------

	private void checkZinc(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet zinc = ModMaterials.ZINC;
		// Stand in water to test corrosion-proof gear.
		server.runCommand("tp @p 0 -60 20");
		server.runCommand("fill -1 -61 19 1 -58 21 minecraft:glass hollow");
		server.runCommand("fill 0 -60 20 0 -59 20 minecraft:water");
		ctx.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.isInWater(), "the test player is standing in water");
			check(damageTaken(player, zinc.pickaxe.get(), 50) == 0, "zinc tools lose no durability in water");
			check(damageTaken(player, Items.IRON_PICKAXE, 50) == 50, "other tools still lose durability in water");
		});
		server.runCommand("fill -1 -61 19 1 -58 21 minecraft:air");
		server.runCommand("tp @p 0 -60 0");
		ctx.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(damageTaken(player, zinc.pickaxe.get(), 50) == 50, "zinc tools lose durability normally out of water");

			player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
			int without = player.getEffect(MobEffects.POISON).getDuration();
			player.removeAllEffects();
			equip(player, zinc);
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
			int with = player.getEffect(MobEffects.POISON).getDuration();
			player.removeAllEffects();
			unequip(player);
			GearExpansion.LOGGER.info("[GameTest] Poison for 200 ticks lasts {} without the zinc set and {} with it", without, with);
			check(without == 200 && with == 100, "the full zinc set halves Poison");
		});
	}

	// Aluminum ------------------------------------------------------------------------

	private void checkAluminum(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet aluminum = ModMaterials.ALUMINUM;
		double baseSpeed = server.computeOnServer(s -> player(s).getAttributeValue(Attributes.MOVEMENT_SPEED));
		server.runOnServer(s -> equip(player(s), aluminum));
		ctx.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			check(speed > baseSpeed * 1.1, "aluminum armor makes the wearer about 12% faster");
			check(Math.abs(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) - 0.5) < 0.001, "the full aluminum set halves fall damage");
			check(player.getAttributeValue(Attributes.JUMP_STRENGTH) > 0.42, "the full aluminum set gives stronger jumps");
			check(player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) > 0.3, "the full aluminum set gives faster water movement");
			unequip(player);
		});
		ctx.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) == 1.0, "set bonus attributes are removed with the armor");

			UseEffects blocking = new ItemStack(aluminum.shield.get()).get(DataComponents.USE_EFFECTS);
			check(blocking != null && blocking.speedMultiplier() == 1.0F, "the aluminum shield doesn't slow the player while blocking");

			double aluminumSpeed = attackSpeed(new ItemStack(aluminum.sword.get()));
			double ironSpeed = attackSpeed(new ItemStack(Items.IRON_SWORD));
			check(aluminumSpeed > ironSpeed, "aluminum swords attack faster than iron swords");
		});
	}

	private static double attackSpeed(ItemStack stack) {
		var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
		return modifiers == null ? 0 : modifiers.modifiers().stream()
			.filter(entry -> entry.attribute().is(Attributes.ATTACK_SPEED))
			.mapToDouble(entry -> entry.modifier().amount())
			.sum();
	}

	// Titanium ------------------------------------------------------------------------

	private void checkTitaniumDurability(MinecraftServer server) {
		ServerPlayer player = player(server);
		int withoutSet = damageTaken(player, Items.DIAMOND_PICKAXE, 400);
		equip(player, ModMaterials.TITANIUM);
		int withSet = damageTaken(player, Items.DIAMOND_PICKAXE, 400);
		unequip(player);

		GearExpansion.LOGGER.info("[GameTest] 400 uses cost {} durability without the titanium set and {} with it", withoutSet, withSet);
		check(withoutSet == 400, "without the set, every use costs durability");
		check(withSet > 120 && withSet < 280, "with the full titanium set, gear loses about 50% less durability");
	}

	private void checkTitaniumResistance(ClientGameTestContext ctx, TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			equip(player, ModMaterials.TITANIUM);
			player.setHealth(4.0F);
		});
		ctx.waitTicks(5);
		boolean resisted = server.computeOnServer(s -> player(s).hasEffect(MobEffects.RESISTANCE));
		check(resisted, "dropping below 30% health with the full titanium set grants Resistance");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.removeAllEffects();
			player.setHealth(player.getMaxHealth());
		});
	}

	// Tooltips and screenshots -----------------------------------------------------------

	private void checkTooltips(ClientGameTestContext ctx, TestServerContext server) {
		ctx.waitTicks(5);
		// The titanium set is still worn from the Resistance check.
		String helmet = tooltip(ctx, ModMaterials.TITANIUM.helmet.get());
		GearExpansion.LOGGER.info("[GameTest] titanium helmet tooltip:\n{}", helmet);
		check(helmet.contains("Titanium Set (4/4)"), "tooltip shows full set progress");
		check(helmet.contains("Unbreakable Will"), "tooltip describes the titanium set bonus");

		String zincHelmet = tooltip(ctx, ModMaterials.ZINC.helmet.get());
		check(zincHelmet.contains("Zinc Set (0/4)") && zincHelmet.contains("Galvanized"), "zinc armor tooltip shows its set bonus");
		check(tooltip(ctx, ModMaterials.ZINC.pickaxe.get()).contains("Corrosion-proof"), "zinc tool tooltip shows it's corrosion-proof");
		check(tooltip(ctx, ModMaterials.ALUMINUM.shield.get()).contains("Lightweight"), "aluminum shield tooltip shows it's lightweight");
		check(tooltip(ctx, ModMaterials.ALUMINUM.boots.get()).contains("Featherweight"), "aluminum armor tooltip shows its set bonus");
		server.runOnServer(s -> unequip(player(s)));
	}

	private static String tooltip(ClientGameTestContext ctx, Item item) {
		return ctx.computeOnClient(mc -> {
			StringBuilder text = new StringBuilder();
			new ItemStack(item).getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL)
				.forEach(line -> text.append(line.getString()).append('\n'));
			return text.toString();
		});
	}

	private void screenshots(ClientGameTestContext ctx, TestServerContext server) {
		server.runCommand("gamemode creative @p");
		// A row of every material's blocks behind the player, in material order.
		int x = -6;
		for (MaterialSet set : ModMaterials.ALL) {
			for (var block : set.blocks()) {
				server.runCommand("setblock " + x + " -60 -3 " + block.getId());
				x++;
			}
		}

		for (MaterialSet set : ModMaterials.ALL) {
			server.runOnServer(s -> equip(player(s), set));
			server.runCommand("item replace entity @p weapon.offhand with " + set.shield.getId());
			server.runCommand("item replace entity @p weapon.mainhand with " + set.sword.getId());
			server.runCommand("tp @p 0 -60 0 0 0");
			ctx.waitTicks(10);
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			ctx.waitTicks(10);
			ctx.takeScreenshot(set.name + "_armor_front");
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			ctx.waitTicks(10);
			ctx.takeScreenshot(set.name + "_armor_back");
		}
		server.runOnServer(s -> unequip(player(s)));
		server.runCommand("tp @p 0 -60 0 180 0");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		ctx.waitTicks(10);
		ctx.takeScreenshot("blocks");

		server.runCommand("tp @p 0 -60 0 0 0");
		server.runCommand("item replace entity @p weapon.offhand with " + ModMaterials.ALUMINUM.shield.getId());
		server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		ctx.getInput().holdKey(options -> options.keyUse);
		ctx.waitTicks(20);
		ctx.takeScreenshot("aluminum_shield_blocking_first_person");
		ctx.getInput().releaseKey(options -> options.keyUse);

		// Each material's items in the inventory.
		for (MaterialSet set : ModMaterials.ALL) {
			server.runCommand("clear @p");
			List<String> items = new ArrayList<>();
			set.tools().forEach(item -> items.add(item.getId().toString()));
			items.add(set.shield.getId().toString());
			items.add(set.ingot.getId().toString());
			items.add(set.rawItem.getId().toString());
			items.add(set.nugget.getId().toString());
			set.armorPieces().forEach(item -> items.add(item.getId().toString()));
			set.blocks().forEach(block -> items.add(block.getId().toString()));
			for (int i = 0; i < items.size(); i++) {
				String slot = i < 9 ? "hotbar." + i : "inventory." + (i - 9);
				server.runCommand("item replace entity @p " + slot + " with " + items.get(i));
			}
			server.runCommand("gamemode survival @p");
			ctx.waitTicks(10);
			ctx.getInput().pressKey(options -> options.keyInventory);
			ctx.waitTicks(10);
			ctx.takeScreenshot(set.name + "_inventory");
			ctx.setScreen(() -> null);
			server.runCommand("gamemode creative @p");
		}

		creativeTabs(ctx, server);

		ctx.setScreen(() -> GearExpansionClient.configScreen(null));
		ctx.waitTicks(10);
		ctx.takeScreenshot("config_screen");
		ctx.setScreen(() -> null);
		ctx.waitTicks(5);
	}

	/** Opens the creative inventory on each of our tabs, and checks every item is in exactly one of them. */
	private void creativeTabs(ClientGameTestContext ctx, TestServerContext server) {
		server.runCommand("clear @p");
		server.runCommand("gamemode creative @p");
		ctx.waitTicks(10);
		ctx.getInput().pressKey(options -> options.keyInventory);
		ctx.waitForScreen(CreativeModeInventoryScreen.class);
		ctx.waitTicks(5);

		List<RegistrySupplier<CreativeModeTab>> tabs = List.of(ModTabs.BLOCKS, ModTabs.TOOLS, ModTabs.COMBAT, ModTabs.INGREDIENTS);
		Map<Item, Integer> appearances = new HashMap<>();
		for (RegistrySupplier<CreativeModeTab> tab : tabs) {
			ctx.runOnClient(mc -> {
				// Modded tabs are on later pages, so switch to the tab's page before selecting it.
				FabricCreativeModeInventoryScreen screen = (FabricCreativeModeInventoryScreen) mc.gui.screen();
				screen.switchToPage(screen.getPage(tab.get()));
				screen.setSelectedTab(tab.get());
			});
			ctx.waitTicks(5);
			ctx.takeScreenshot("creative_tab_" + tab.getId().getPath());
			tab.get().getDisplayItems().forEach(stack -> appearances.merge(stack.getItem(), 1, Integer::sum));
		}
		ctx.setScreen(() -> null);

		List<Item> ours = BuiltInRegistries.ITEM.stream()
			.filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(GearExpansion.MOD_ID))
			.toList();
		check(ours.stream().allMatch(item -> appearances.getOrDefault(item, 0) == 1),
			"every Gear Expansion item appears in exactly one creative tab");
		check(tabs.stream().allMatch(tab -> tab.get().getDisplayItems().size() == ModMaterials.ALL.size() * expectedPerMaterial(tab)),
			"each creative tab holds the expected items for every material");
		check(ModTabs.COMBAT.get().getDisplayItems().stream().anyMatch(stack -> stack.is(ModMaterials.ZINC.shield.get()))
			&& ModTabs.TOOLS.get().getDisplayItems().stream().anyMatch(stack -> stack.is(ModMaterials.ALUMINUM.pickaxe.get())),
			"tools and combat gear are sorted into the right tabs");
	}

	private static int expectedPerMaterial(RegistrySupplier<CreativeModeTab> tab) {
		if (tab == ModTabs.BLOCKS) {
			return 4;
		} else if (tab == ModTabs.TOOLS) {
			return 4;
		} else if (tab == ModTabs.COMBAT) {
			return 7;
		}
		return 3;
	}

	// Helpers -------------------------------------------------------------------------

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static int damageTaken(ServerPlayer player, Item item, int uses) {
		ItemStack stack = new ItemStack(item);
		for (int i = 0; i < uses; i++) {
			stack.hurtAndBreak(1, player.level(), player, broken -> { });
		}
		return stack.getDamageValue();
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
