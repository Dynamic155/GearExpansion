package com.gearexpansion.fabric.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.architectury.registry.registries.RegistrySupplier;

import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.Direction;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import dev.architectury.networking.NetworkManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.network.ConfigSyncPayload;
import com.gearexpansion.block.entity.AlloyForgeBlockEntity;
import com.gearexpansion.client.ClientGearState;
import com.gearexpansion.client.screen.AlloyForgeScreen;
import com.gearexpansion.item.GearCombat;
import com.gearexpansion.material.behavior.VerdigrisBehavior;
import com.gearexpansion.menu.AlloyForgeMenu;
import com.gearexpansion.network.UseAbilityPayload;
import com.gearexpansion.recipe.AlloyingRecipeInput;
import com.gearexpansion.recipe.ModRecipes;
import com.gearexpansion.registry.ModBlocks;
import com.gearexpansion.registry.ModItems;
import com.gearexpansion.setbonus.BrassSetBonus;
import com.gearexpansion.setbonus.SetBonuses;
import com.gearexpansion.client.GearExpansionClient;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.MaterialSet.OreGeneration;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.registry.ModTabs;
import com.gearexpansion.setbonus.EnchantingBonus;
import com.gearexpansion.worldgen.ModOres;

/**
 * End-to-end checks for every material in a real game: recipes, mining tiers, ore generation,
 * material traits, set bonuses, tooltips, creative tabs, screenshots of the gear, and the
 * Alloy Forge category in JEI or REI (see {@link RecipeViewerChecks}).
 *
 * <p>Run with {@code ./gradlew :fabric:runGameTest}, which loads JEI; add {@code -Precipe_viewer=rei}
 * to check REI instead. Screenshots are saved to {@code fabric/build/gametest/screenshots}.
 * Failures are collected and reported together.
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
			checkRoseGold(ctx, server);
			checkAluminum(ctx, server);
			server.runOnServer(this::checkTitaniumDurability);
			checkTitaniumResistance(ctx, server);
			checkTooltips(ctx, server);
			checkSettingsSync(ctx);
			checkEquipSounds(ctx);
			checkAlloyForge(ctx, server);
			checkBrass(ctx, server);
			checkSilver(ctx, server);
			checkEmerald(ctx, server);
			checkAmethyst(ctx, server);
			checkVerdigris(ctx, server);
			checkSakura(ctx, server);
			checkSteel(ctx, server);
			checkCobalt(ctx, server);
			checkTungsten(ctx, server);
			checkObsidian(ctx, server);
			checkPrismarine(ctx, server);
			checkEcho(ctx, server);
			checkFrostite(ctx, server);
			checkFulgurite(ctx, server);
			checkVerdantite(ctx, server);
			checkInfernium(ctx, server);
			WeaponChecks.run(ctx, server, this::check);
			KawaiiChecks.run(ctx, server, this::check);

			screenshots(ctx, server);
			RecipeViewerChecks.run(ctx, this::check);
			JadeChecks.run(ctx, server, this::check);
			GuidebookChecks.run(ctx, server, this::check);
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
		String suffix = set.upgradedFrom != null ? "_smithing" : "";
		List<String> recipes = new ArrayList<>();
		for (String gear : List.of("sword", "pickaxe", "spear", "chestplate", "shield")) {
			recipes.add(set.name + "_" + gear + suffix);
		}
		if (set.storageBlock != null) {
			recipes.add(set.name + "_block");
		}
		if (set.hasOre) {
			recipes.add(set.ingotName + "_from_blasting_raw_" + set.oreName);
		}
		for (String recipe : recipes) {
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
			if (!set.hasOre) {
				continue;
			}
			boolean deep = set.oreGeneration.getFirst().maxY() <= 0;
			int y = deep ? -40 : 20;
			String stone = switch (set.oreKind) {
				case NETHER -> "minecraft:netherrack";
				case ICE -> "minecraft:packed_ice";
				default -> deep ? "minecraft:deepslate" : "minecraft:stone";
			};
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
					if (state.is(set.ore.get()) || (set.deepslateOre != null && state.is(set.deepslateOre.get()))) {
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
		check(!canMine(Items.IRON_PICKAXE, ModMaterials.INFERNIUM.ore.get().defaultBlockState()), "an iron pickaxe can't mine infernium ore");
		check(canMine(titanium.pickaxe.get(), ModMaterials.INFERNIUM.ore.get().defaultBlockState()), "a titanium pickaxe mines infernium ore");
		check(!canMine(Items.STONE_PICKAXE, ModMaterials.SILVER.ore.get().defaultBlockState()), "a stone pickaxe can't mine silver ore");
		check(canMine(Items.IRON_PICKAXE, ModMaterials.SILVER.deepslateOre.get().defaultBlockState()), "an iron pickaxe mines silver ore");
		check(canMine(ModMaterials.SILVER.pickaxe.get(), Blocks.DIAMOND_ORE.defaultBlockState()), "a silver pickaxe mines diamond ore (iron tier)");
		check(!canMine(Items.STONE_PICKAXE, ModMaterials.COBALT.ore.get().defaultBlockState()), "a stone pickaxe can't mine cobalt ore");
		check(canMine(Items.IRON_PICKAXE, ModMaterials.COBALT.ore.get().defaultBlockState()), "an iron pickaxe mines cobalt ore");
		check(canMine(ModMaterials.COBALT.pickaxe.get(), Blocks.OBSIDIAN.defaultBlockState()), "a cobalt pickaxe mines obsidian (diamond tier)");
		check(!canMine(Items.IRON_PICKAXE, ModMaterials.TUNGSTEN.deepslateOre.get().defaultBlockState()), "an iron pickaxe can't mine tungsten ore");
		check(canMine(Items.DIAMOND_PICKAXE, ModMaterials.TUNGSTEN.deepslateOre.get().defaultBlockState()), "a diamond pickaxe mines tungsten ore");
		check(canMine(Items.WOODEN_PICKAXE, ModBlocks.ALLOY_FORGE.get().defaultBlockState()), "a pickaxe mines the alloy forge");
		check(!canMine(Items.STICK, ModBlocks.ALLOY_FORGE.get().defaultBlockState()), "the alloy forge needs a pickaxe to drop");
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

	// Rose Gold -----------------------------------------------------------------------

	private void checkRoseGold(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet roseGold = ModMaterials.ROSE_GOLD;
		int without = experienceFromOrb(ctx, server, 100);
		server.runOnServer(s -> equip(player(s), roseGold));
		int with = experienceFromOrb(ctx, server, 100);
		GearExpansion.LOGGER.info("[GameTest] an orb worth 100 gives {} experience without the rose gold set and {} with it", without, with);
		check(without == 100 && with == 125, "the full rose gold set gives 25% more experience");

		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ItemStack book = new ItemStack(Items.BOOK);
			int plain = EnchantmentHelper.getEnchantmentCost(RandomSource.create(7), 2, 0, book);
			EnchantingBonus.begin(player);
			int boosted = EnchantmentHelper.getEnchantmentCost(RandomSource.create(7), 2, 0, book);
			EnchantingBonus.end();
			GearExpansion.LOGGER.info("[GameTest] top enchanting offer with no bookshelves: {} normally, {} with the rose gold set", plain, boosted);
			check(boosted > plain && boosted >= 6, "the full rose gold set counts as extra bookshelves for enchanting");
			unequip(player);
		});
		check(new ItemStack(roseGold.chestplate.get()).is(ItemTags.PIGLIN_SAFE_ARMOR), "piglins treat rose gold armor like gold");
	}

	/** Drops an experience orb on the player and returns how much experience they gained from it. */
	private static int experienceFromOrb(ClientGameTestContext ctx, TestServerContext server, int value) {
		int before = server.computeOnServer(s -> player(s).totalExperience);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.level().addFreshEntity(new ExperienceOrb(player.level(), player.getX(), player.getY(), player.getZ(), value));
		});
		ctx.waitTicks(20);
		return server.computeOnServer(s -> player(s).totalExperience) - before;
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

	/** A server's settings take over while connected to it, and this game's come back after leaving. */
	private void checkSettingsSync(ClientGameTestContext ctx) {
		JsonObject serverSettings = JsonParser.parseString(GearExpansionConfig.toSyncJson()).getAsJsonObject();
		serverSettings.addProperty("zincEffectReduction", 75);
		String json = serverSettings.toString();

		// In singleplayer this game is the server, so a settings packet changes nothing.
		ctx.runOnClient(mc -> ClientGearState.receiveServerSettings(new ConfigSyncPayload(json)));
		check(GearExpansionConfig.get().zincEffectReduction == 50, "singleplayer keeps its own settings");

		ctx.runOnClient(mc -> GearExpansionConfig.useServerSettings(json));
		check(GearExpansionConfig.get().zincEffectReduction == 75, "a server's settings are used while connected");
		check(tooltip(ctx, ModMaterials.ZINC.helmet.get()).contains("75% shorter"), "tooltips show the server's settings");
		ctx.runOnClient(mc -> GearExpansionConfig.clearServerSettings());
		check(GearExpansionConfig.get().zincEffectReduction == 50, "this game's settings come back after leaving a server");
	}

	/** Every armor set has its own equip sound, and the client knows how to play it. */
	private void checkEquipSounds(ClientGameTestContext ctx) {
		for (MaterialSet set : ModMaterials.ALL) {
			Identifier sound = new ItemStack(set.helmet.get()).get(DataComponents.EQUIPPABLE).equipSound().value().location();
			check(sound.equals(GearExpansion.id("item.armor.equip_" + set.name)), set.name + " armor has its own equip sound");
			check(ctx.computeOnClient(mc -> mc.getSoundManager().getSoundEvent(sound) != null), set.name + " equip sound is defined in sounds.json");
		}
	}

	static String tooltip(ClientGameTestContext ctx, Item item) {
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
			set.ingredients().forEach(item -> items.add(item.getId().toString()));
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
		showcase(ctx, server);

		ctx.setScreen(() -> GearExpansionClient.configScreen(null));
		ctx.waitTicks(10);
		ctx.takeScreenshot("config_screen");
		ctx.setScreen(() -> null);
		ctx.waitTicks(5);
	}

	/** Clean pictures of each set, with the HUD hidden, for the README (docs/images). */
	private void showcase(ClientGameTestContext ctx, TestServerContext server) {
		server.runCommand("gamemode creative @p");
		server.runCommand("clear @p");
		ctx.runOnClient(mc -> {
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		});
		for (MaterialSet set : ModMaterials.ALL) {
			server.runOnServer(s -> {
				ServerPlayer player = player(s);
				equip(player, set);
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(set.sword.get()));
				player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(set.shield.get()));
			});
			server.runCommand("tp @p 0 -60 0 20 10");
			ctx.waitTicks(15);
			ctx.takeScreenshot("showcase_" + set.name);
			if (set == ModMaterials.FAIRY_KEI) {
				// Fairy Kei's wings are on the back.
				ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
				ctx.waitTicks(5);
				ctx.takeScreenshot("showcase_back_" + set.name);
				ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			}
		}
		ctx.runOnClient(mc -> {
			mc.gui.hud.toggle();
			mc.options.setCameraType(CameraType.FIRST_PERSON);
		});
		server.runOnServer(s -> unequip(player(s)));
		server.runCommand("clear @p");
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
		check(tabs.stream().allMatch(tab -> tab.get().getDisplayItems().size() == expectedCount(tab)),
			"each creative tab holds the expected items for every material");
		check(ModTabs.BLOCKS.get().getDisplayItems().iterator().next().is(ModItems.ALLOY_FORGE.get()), "the alloy forge comes first in the blocks tab");
		check(ModTabs.COMBAT.get().getDisplayItems().stream().anyMatch(stack -> stack.is(ModMaterials.ZINC.shield.get()))
			&& ModTabs.TOOLS.get().getDisplayItems().stream().anyMatch(stack -> stack.is(ModMaterials.ALUMINUM.pickaxe.get())),
			"tools and combat gear are sorted into the right tabs");
	}

	private static int expectedCount(RegistrySupplier<CreativeModeTab> tab) {
		// The Blocks tab also holds the Alloy Forge and Fulgurite.
		int count = tab == ModTabs.BLOCKS ? 2 : 0;
		for (MaterialSet set : ModMaterials.ALL) {
			if (tab == ModTabs.BLOCKS) {
				count += set.blocks().size();
			} else if (tab == ModTabs.TOOLS) {
				count += 4;
			} else if (tab == ModTabs.COMBAT) {
				count += 9;
			} else {
				count += set.ingredients().size();
			}
		}
		return count;
	}

	// Alloy Forge -----------------------------------------------------------------------

	// Forges in a row in front of the player at 0 -60 0, facing them.
	private static final BlockPos COAL_FORGE = new BlockPos(-2, -60, 3);
	private static final BlockPos BOOSTED_FORGE = new BlockPos(0, -60, 3);
	private static final BlockPos LAVA_FORGE = new BlockPos(2, -60, 3);
	private static final BlockPos SHORT_FORGE = new BlockPos(-4, -60, 3);
	private static final BlockPos SHIFT_CLICK_FORGE = new BlockPos(10, -60, 3);
	// Raised by one so a hopper fits underneath.
	private static final BlockPos HOPPER_FORGE = new BlockPos(6, -59, 3);
	private static final List<BlockPos> FORGES = List.of(COAL_FORGE, BOOSTED_FORGE, LAVA_FORGE, SHORT_FORGE, SHIFT_CLICK_FORGE, HOPPER_FORGE);

	private void checkAlloyForge(ClientGameTestContext ctx, TestServerContext server) {
		server.runOnServer(s -> {
			for (String recipe : List.of("alloy_forge", "brass_ingot_from_alloying", "rose_gold_ingot_from_alloying")) {
				check(s.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, GearExpansion.id(recipe))).isPresent(), "recipe exists: " + recipe);
			}
		});
		server.runCommand("gamemode creative @p");
		server.runCommand("tp @p 0 -60 0 0 25");
		for (BlockPos pos : FORGES) {
			server.runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " gearexpansion:alloy_forge[facing=north]");
		}
		ctx.waitTicks(2);

		server.runOnServer(s -> {
			// 5 copper and 1 zinc make one batch of brass and leave 2 copper.
			fill(s, COAL_FORGE, new ItemStack(Items.COPPER_INGOT, 5), new ItemStack(ModMaterials.ZINC.ingot.get()), new ItemStack(Items.COAL));
			// Blaze powder doubles the speed: three batches of rose gold in 300 ticks instead of 600.
			fill(s, BOOSTED_FORGE, new ItemStack(Items.GOLD_INGOT, 9), new ItemStack(Items.COPPER_INGOT, 3), new ItemStack(Items.BLAZE_POWDER, 2));
			fill(s, LAVA_FORGE, new ItemStack(Items.COPPER_INGOT, 3), new ItemStack(ModMaterials.ZINC.ingot.get()), new ItemStack(Items.LAVA_BUCKET));
			// One copper short of a batch.
			fill(s, SHORT_FORGE, new ItemStack(Items.COPPER_INGOT, 2), new ItemStack(ModMaterials.ZINC.ingot.get()), new ItemStack(Items.COAL));
		});
		ctx.waitTicks(5);
		server.runOnServer(s -> {
			check(isLit(s, COAL_FORGE), "the alloy forge lights with coal and a matching recipe");
			check(forge(s, LAVA_FORGE).getItem(AlloyForgeBlockEntity.FUEL_SLOT).is(Items.BUCKET), "a lava bucket leaves an empty bucket in the fuel slot");
			check(forge(s, LAVA_FORGE).isBoosted() && forge(s, BOOSTED_FORGE).isBoosted(), "lava and blaze powder are boost fuels");
			check(!forge(s, COAL_FORGE).isBoosted(), "coal is not a boost fuel");
			check(!isLit(s, SHORT_FORGE), "the alloy forge doesn't light without enough ingredients");
		});

		checkForgeHoppers(ctx, server);
		checkForgeShiftClick(server);

		// Screenshots while the forges are lit: the blocks in the world, then the screen mid-alloy.
		ctx.waitTicks(60);
		ctx.takeScreenshot("alloy_forge_lit");
		server.runOnServer(s -> player(s).openMenu(forge(s, BOOSTED_FORGE)));
		ctx.waitForScreen(AlloyForgeScreen.class);
		ctx.waitTicks(20);
		ctx.takeScreenshot("alloy_forge_screen");
		ctx.setScreen(() -> null);

		ctx.waitTicks(200);
		server.runOnServer(s -> {
			AlloyForgeBlockEntity coal = forge(s, COAL_FORGE);
			GearExpansion.LOGGER.info("[GameTest] coal forge holds {}", contents(coal));
			ItemStack brass = coal.getItem(AlloyForgeBlockEntity.RESULT_SLOT);
			check(brass.is(ModMaterials.BRASS.ingot.get()) && brass.getCount() == 4, "3 copper and 1 zinc make 4 brass ingots");
			check(inputCount(coal, Items.COPPER_INGOT) == 2 && inputCount(coal, ModMaterials.ZINC.ingot.get()) == 0, "alloying uses exactly 3 copper and 1 zinc");
			check(coal.getItem(AlloyForgeBlockEntity.FUEL_SLOT).isEmpty(), "the coal was burned");

			AlloyForgeBlockEntity boosted = forge(s, BOOSTED_FORGE);
			GearExpansion.LOGGER.info("[GameTest] boosted forge holds {}", contents(boosted));
			ItemStack roseGold = boosted.getItem(AlloyForgeBlockEntity.RESULT_SLOT);
			check(roseGold.is(ModMaterials.ROSE_GOLD.ingot.get()) && roseGold.getCount() == 6, "blaze powder alloys 3 batches of rose gold in the time coal does 1.5");
			check(forge(s, SHORT_FORGE).getItem(AlloyForgeBlockEntity.RESULT_SLOT).isEmpty(), "too few ingredients make nothing");

			// Taking the output with a shift-click gives the stored experience: 3 batches at 0.7 each.
			ServerPlayer player = player(s);
			player.getInventory().clearContent();
			player.openMenu(boosted);
			AlloyForgeMenu menu = (AlloyForgeMenu) player.containerMenu;
			menu.quickMoveStack(player, AlloyForgeMenu.RESULT_SLOT);
			int xp = s.overworld().getEntitiesOfClass(ExperienceOrb.class, new AABB(player.blockPosition()).inflate(3)).stream()
				.mapToInt(ExperienceOrb::getValue)
				.sum();
			GearExpansion.LOGGER.info("[GameTest] taking 3 batches of rose gold dropped {} experience", xp);
			check(xp >= 2 && xp <= 3, "taking alloys from the forge gives their experience");
			check(player.getInventory().countItem(ModMaterials.ROSE_GOLD.ingot.get()) == 6, "shift-clicking the output moves it to the player");
			player.closeContainer();
		});

		// Clean up, so the forges don't show in later screenshots.
		server.runOnServer(s -> FORGES.forEach(pos -> forge(s, pos).clearContent()));
		server.runCommand("fill -5 -60 3 11 -58 4 minecraft:air");
		server.runCommand("clear @p");
		ctx.waitTicks(2);
		server.runCommand("kill @e[type=item]");
		server.runCommand("kill @e[type=experience_orb]");
		server.runCommand("gamemode survival @p");
	}

	/** A hopper on top feeds the inputs, one on the side feeds fuel, and one below takes the output. */
	private void checkForgeHoppers(ClientGameTestContext ctx, TestServerContext server) {
		BlockPos pos = HOPPER_FORGE;
		String top = pos.getX() + " " + (pos.getY() + 1) + " " + pos.getZ();
		String side = (pos.getX() + 1) + " " + pos.getY() + " " + pos.getZ();
		String below = pos.getX() + " " + (pos.getY() - 1) + " " + pos.getZ();
		server.runCommand("setblock " + top + " minecraft:hopper[facing=down]");
		server.runCommand("setblock " + side + " minecraft:hopper[facing=west]");
		server.runCommand("setblock " + below + " minecraft:hopper[facing=down]");
		server.runCommand("item replace block " + top + " container.0 with minecraft:copper_ingot 4");
		server.runCommand("item replace block " + top + " container.1 with gearexpansion:zinc_ingot 1");
		server.runCommand("item replace block " + top + " container.2 with minecraft:dirt 1");
		server.runCommand("item replace block " + side + " container.0 with minecraft:coal 1");
		server.runOnServer(s -> forge(s, pos).setItem(AlloyForgeBlockEntity.RESULT_SLOT, new ItemStack(ModMaterials.BRASS.ingot.get(), 2)));
		ctx.waitTicks(80);
		server.runOnServer(s -> {
			AlloyForgeBlockEntity forge = forge(s, pos);
			GearExpansion.LOGGER.info("[GameTest] hopper-fed forge holds {}", contents(forge));
			check(forge.getItem(0).is(Items.COPPER_INGOT) && forge.getItem(0).getCount() == 4, "a hopper on top puts copper in the first input");
			check(forge.getItem(1).is(ModMaterials.ZINC.ingot.get()) && forge.getItem(2).isEmpty(), "a hopper on top puts zinc in its own input slot");
			HopperBlockEntity topHopper = (HopperBlockEntity) s.overworld().getBlockEntity(pos.above());
			check(topHopper.getItem(2).is(Items.DIRT), "hoppers can't put non-ingredients in the inputs");
			check(isLit(s, pos), "a hopper on the side fuels the forge");
			HopperBlockEntity bottomHopper = (HopperBlockEntity) s.overworld().getBlockEntity(pos.below());
			check(forge.getItem(AlloyForgeBlockEntity.RESULT_SLOT).isEmpty() && bottomHopper.getItem(0).is(ModMaterials.BRASS.ingot.get()),
				"a hopper below takes the output");
		});
	}

	/** Shift-clicking from the player's inventory sends ingredients to the inputs and fuel to the fuel slot. */
	private void checkForgeShiftClick(TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.getInventory().clearContent();
			player.getInventory().setItem(9, new ItemStack(Items.GOLD_INGOT, 3));
			player.getInventory().setItem(10, new ItemStack(Items.COAL, 2));
			player.getInventory().setItem(11, new ItemStack(Items.DIRT));
			player.openMenu(forge(s, SHIFT_CLICK_FORGE));
			AlloyForgeMenu menu = (AlloyForgeMenu) player.containerMenu;
			// Menu slots 5 to 31 are the main inventory (inventory slots 9 to 35).
			menu.quickMoveStack(player, 5);
			menu.quickMoveStack(player, 6);
			menu.quickMoveStack(player, 7);
			AlloyForgeBlockEntity forge = forge(s, SHIFT_CLICK_FORGE);
			check(forge.getItem(0).is(Items.GOLD_INGOT) && forge.getItem(0).getCount() == 3, "shift-clicking an ingredient moves it to the inputs");
			check(forge.getItem(AlloyForgeBlockEntity.FUEL_SLOT).is(Items.COAL), "shift-clicking fuel moves it to the fuel slot");
			check(player.getInventory().getItem(0).is(Items.DIRT), "shift-clicking anything else moves it to the hotbar");
			player.closeContainer();
			player.getInventory().clearContent();
		});
	}

	private static AlloyForgeBlockEntity forge(MinecraftServer server, BlockPos pos) {
		return (AlloyForgeBlockEntity) server.overworld().getBlockEntity(pos);
	}

	private static boolean isLit(MinecraftServer server, BlockPos pos) {
		return server.overworld().getBlockState(pos).getValue(AbstractFurnaceBlock.LIT);
	}

	private static void fill(MinecraftServer server, BlockPos pos, ItemStack first, ItemStack second, ItemStack fuel) {
		AlloyForgeBlockEntity forge = forge(server, pos);
		forge.setItem(0, first);
		forge.setItem(1, second);
		forge.setItem(AlloyForgeBlockEntity.FUEL_SLOT, fuel);
	}

	private static int inputCount(AlloyForgeBlockEntity forge, Item item) {
		int count = 0;
		for (int slot = 0; slot < AlloyForgeBlockEntity.INPUT_SLOTS; slot++) {
			if (forge.getItem(slot).is(item)) {
				count += forge.getItem(slot).getCount();
			}
		}
		return count;
	}

	private static String contents(AlloyForgeBlockEntity forge) {
		List<String> slots = new ArrayList<>();
		for (int slot = 0; slot < forge.getContainerSize(); slot++) {
			slots.add(forge.getItem(slot).toString());
		}
		return String.join(", ", slots);
	}

	// Brass ---------------------------------------------------------------------------

	/** Winds the spring by blocking, then lets it go with the Set Ability key sent from the client. */
	private void checkBrass(ClientGameTestContext ctx, TestServerContext server) {
		BrassSetBonus brass = (BrassSetBonus) SetBonuses.BRASS;
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runCommand("summon minecraft:zombie 0 -60 2 {NoAI:1b}");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			equip(player, ModMaterials.BRASS);
			for (int i = 0; i < 7; i++) {
				brass.onShieldBlock(player, player, s.overworld().damageSources().generic(), 1.0F);
			}
			check(brass.spring(player) >= BrassSetBonus.FULL, "blocking winds the brass spring");
		});
		ctx.waitTicks(25);
		check(ctx.computeOnClient(mc -> ClientGearState.hud.springMax() == BrassSetBonus.FULL && ClientGearState.hud.spring() >= BrassSetBonus.FULL),
			"the client HUD shows the wound spring");
		check(server.computeOnServer(s -> player(s).hasEffect(MobEffects.HASTE)), "a wound spring gives Haste");
		float zombieHealth = server.computeOnServer(s -> zombie(s).getHealth());
		// Press the ability key the way the client does: send the packet.
		ctx.runOnClient(mc -> NetworkManager.sendToServer(UseAbilityPayload.INSTANCE));
		ctx.waitTicks(5);
		server.runOnServer(s -> {
			// The release's own hit winds the spring a little, so it isn't exactly empty.
			check(brass.spring(player(s)) < BrassSetBonus.FULL / 2, "Spring Release lets the spring go");
			check(zombie(s).getHealth() < zombieHealth, "Spring Release hits mobs in front");
			unequip(player(s));
			player(s).removeAllEffects();
		});
		server.runCommand("kill @e[type=zombie]");
	}

	private static Zombie zombie(MinecraftServer server) {
		// Skip zombies still playing their death animation from an earlier check.
		return server.overworld().getEntitiesOfClass(Zombie.class, new AABB(-20, -64, -20, 20, -40, 20), Zombie::isAlive).getFirst();
	}

	// Emerald -------------------------------------------------------------------------

	private void checkSilver(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet silver = ModMaterials.SILVER;
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			Pillager pillager = EntityTypes.PILLAGER.create(level, EntitySpawnReason.COMMAND);
			// The bonus reads the weapon from the attacker's hand.
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(silver.sword.get()));
			DamageSource source = level.damageSources().playerAttack(player);
			check(silver.sword.get().getAttackDamageBonus(zombie, 10.0F, source) == 4.0F, "silver weapons deal 4 extra damage to undead");
			check(silver.sword.get().getAttackDamageBonus(pillager, 10.0F, source) == 0.0F, "silver weapons deal normal damage to the living");
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

			// Armor: 6% less damage from undead per piece, so 24% for the full set.
			equip(player, silver);
			float fromZombie = GearCombat.modifyIncomingDamage(player, level.damageSources().mobAttack(zombie), 10.0F);
			float fromPillager = GearCombat.modifyIncomingDamage(player, level.damageSources().mobAttack(pillager), 10.0F);
			check(Math.abs(fromZombie - 7.6F) < 0.01F, "the full silver set takes 24% less damage from undead (took " + fromZombie + ")");
			check(fromPillager == 10.0F, "silver armor doesn't protect against the living");

			// Undead glow nearby: one zombie 5 blocks away, one 14 blocks away.
			for (int distance : new int[] {5, 14}) {
				Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
				target.setNoAi(true);
				target.setPos(player.getX() + distance, player.getY(), player.getZ());
				target.addTag("silver_test_" + distance);
				level.addFreshEntity(target);
			}
			player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200));
			check(player.getEffect(MobEffects.WITHER).getDuration() == 100, "the full silver set halves Wither");
		});
		ctx.waitTicks(25);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			boolean nearGlows = level.getEntitiesOfClass(Zombie.class, player(s).getBoundingBox().inflate(20), z -> z.entityTags().contains("silver_test_5")).stream()
				.anyMatch(z -> z.hasEffect(MobEffects.GLOWING));
			boolean farGlows = level.getEntitiesOfClass(Zombie.class, player(s).getBoundingBox().inflate(20), z -> z.entityTags().contains("silver_test_14")).stream()
				.anyMatch(z -> z.hasEffect(MobEffects.GLOWING));
			check(nearGlows, "undead within 8 blocks glow while wearing the full silver set");
			check(!farGlows, "undead farther away don't glow");
		});
		check(tooltip(ctx, silver.sword.get()).contains("Hallowed"), "silver weapon tooltip shows Hallowed");
		check(tooltip(ctx, silver.chestplate.get()).contains("Blessed"), "silver armor tooltip shows its set bonus");
		server.runCommand("kill @e[type=zombie]");
		server.runOnServer(s -> {
			unequip(player(s));
			player(s).removeAllEffects();
		});
	}

	// Steel, Cobalt, Tungsten ----------------------------------------------------------

	private void checkSakura(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet sakura = ModMaterials.SAKURA;
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			// One iron ingot and four pink petals make a sakura ingot.
			AlloyingRecipeInput input = new AlloyingRecipeInput(List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.PINK_PETALS, 4), ItemStack.EMPTY));
			var recipe = s.getRecipeManager().getRecipeFor(ModRecipes.ALLOYING.get(), input, level);
			check(recipe.isPresent() && recipe.get().value().result().item().value() == sakura.ingot.get(), "iron and pink petals alloy into sakura");

			// Blossoming: finishing off a mob heals 2 health (one heart).
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zombie.setHealth(0.0F);
			player.setHealth(10.0F);
			sakura.behavior.onHurtEnemy(sakura, new ItemStack(sakura.sword.get()), zombie, player);
			check(player.getHealth() == 12.0F, "finishing off a mob with a sakura weapon heals one heart (health " + player.getHealth() + ")");

			// Petal Guard: blocking heals 1 health, at most once a second.
			DamageSource source = level.damageSources().mobAttack(zombie);
			sakura.behavior.onShieldBlock(sakura, player, zombie, new ItemStack(sakura.shield.get()), source, 4.0F);
			sakura.behavior.onShieldBlock(sakura, player, zombie, new ItemStack(sakura.shield.get()), source, 4.0F);
			check(player.getHealth() == 13.0F, "blocking with the sakura shield heals half a heart, once a second (health " + player.getHealth() + ")");
			player.setHealth(player.getMaxHealth());

			equip(player, sakura);
			player.removeAllEffects();
		});
		ctx.waitTicks(25);
		check(!server.computeOnServer(s -> player(s).hasEffect(MobEffects.REGENERATION)), "Hanami gives nothing away from flowers");
		server.runCommand("setblock 2 -60 0 minecraft:pink_petals");
		ctx.waitTicks(25);
		check(server.computeOnServer(s -> player(s).hasEffect(MobEffects.REGENERATION)), "the full sakura set gives Regeneration near pink petals");
		server.runCommand("setblock 2 -60 0 minecraft:air");
		check(tooltip(ctx, sakura.sword.get()).contains("Blossoming"), "sakura weapon tooltip shows Blossoming");
		check(tooltip(ctx, sakura.chestplate.get()).contains("Hanami"), "sakura armor tooltip shows its set bonus");
		server.runOnServer(s -> {
			unequip(player(s));
			player(s).removeAllEffects();
		});
	}

	private void checkSteel(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet steel = ModMaterials.STEEL;
		server.runOnServer(s -> {
			// One iron ingot and two coal (or charcoal) make a steel ingot.
			AlloyingRecipeInput input = new AlloyingRecipeInput(List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.CHARCOAL, 2), ItemStack.EMPTY));
			var recipe = s.getRecipeManager().getRecipeFor(ModRecipes.ALLOYING.get(), input, s.overworld());
			check(recipe.isPresent() && recipe.get().value().result().item().value() == steel.ingot.get(), "iron and charcoal alloy into steel");
			AlloyingRecipeInput tooLittleCoal = new AlloyingRecipeInput(List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COAL), ItemStack.EMPTY));
			check(s.getRecipeManager().getRecipeFor(ModRecipes.ALLOYING.get(), tooLittleCoal, s.overworld()).isEmpty(), "steel needs two coal");
			equip(player(s), steel);
		});
		ctx.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
			check(toughness == 6.0, "steel armor gives 1 toughness per piece, plus 2 from Hardened (got " + toughness + ")");
			unequip(player);
		});
		check(tooltip(ctx, steel.shield.get()).contains("Reinforced"), "steel shield tooltip shows Reinforced");
	}

	private void checkCobalt(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet cobalt = ModMaterials.COBALT;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(new ItemStack(cobalt.shield.get()).get(DataComponents.BLOCKS_ATTACKS).blockDelaySeconds() == 0.0F, "the cobalt shield blocks the moment it's raised");
			Item cobaltPickaxe = cobalt.pickaxe.get();
			Item roseGoldPickaxe = ModMaterials.ROSE_GOLD.pickaxe.get();
			check(cobaltPickaxe.getDestroySpeed(new ItemStack(cobaltPickaxe), Blocks.STONE.defaultBlockState())
				> roseGoldPickaxe.getDestroySpeed(new ItemStack(roseGoldPickaxe), Blocks.STONE.defaultBlockState()),
				"the cobalt pickaxe is the fastest in the mod");
			equip(player, cobalt);
			player.removeAllEffects();
			// Overdrive: 6 blocks in a row for Haste I, 12 for Haste II.
			for (int i = 0; i < 6; i++) {
				SetBonuses.onBlockBroken(player, Blocks.STONE.defaultBlockState(), player.blockPosition());
			}
			MobEffectInstance haste = player.getEffect(MobEffects.HASTE);
			check(haste != null && haste.getAmplifier() == 0, "6 blocks in a row give Haste I with the full cobalt set");
			for (int i = 0; i < 6; i++) {
				SetBonuses.onBlockBroken(player, Blocks.STONE.defaultBlockState(), player.blockPosition());
			}
			haste = player.getEffect(MobEffects.HASTE);
			check(haste != null && haste.getAmplifier() == 1, "12 blocks in a row give Haste II");
		});
		ctx.waitTicks(50);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			SetBonuses.onBlockBroken(player, Blocks.STONE.defaultBlockState(), player.blockPosition());
			check(SetBonuses.COBALT.chainLength(player) == 1, "the Overdrive chain breaks after 2 seconds without mining");
			unequip(player);
			player.removeAllEffects();
		});
		check(tooltip(ctx, cobalt.pickaxe.get()).contains("Swift"), "cobalt tool tooltip shows Swift");
	}

	private void checkTungsten(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet tungsten = ModMaterials.TUNGSTEN;
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(new ItemStack(tungsten.shield.get()).get(DataComponents.BLOCKS_ATTACKS).blockDelaySeconds() == 0.5F, "the tungsten shield is slow to raise");
			equip(player, tungsten);
		});
		ctx.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			double knockback = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
			check(Math.abs(knockback - 0.6) < 0.001, "tungsten armor gives 0.15 knockback resistance per piece (got " + knockback + ")");
			double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED) / player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
			check(Math.abs(speed - 0.84) < 0.001, "tungsten armor makes the wearer 4% slower per piece (speed x" + speed + ")");
			float explosion = GearCombat.modifyIncomingDamage(player, s.overworld().damageSources().explosion(null, null), 10.0F);
			check(Math.abs(explosion - 6.0F) < 0.01F, "the full tungsten set takes 40% less explosion damage (took " + explosion + ")");
			player.setShiftKeyDown(true);
		});
		ctx.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= 1.0, "sneaking in the full tungsten set stops all knockback");
			player.setShiftKeyDown(false);

			// Ground Slam hurts, knocks back, and slows mobs nearby.
			ServerLevel level = s.overworld();
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zombie.setNoAi(true);
			zombie.setPos(player.getX() + 3, player.getY(), player.getZ());
			level.addFreshEntity(zombie);
			SetBonuses.useAbility(player);
			check(zombie.getHealth() < zombie.getMaxHealth(), "Ground Slam hurts nearby mobs");
			check(zombie.hasEffect(MobEffects.SLOWNESS), "Ground Slam slows nearby mobs");
		});
		ctx.waitTicks(2);
		server.runOnServer(s -> check(player(s).getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) < 1.0, "knockback resistance goes back to normal after sneaking"));
		check(tooltip(ctx, tungsten.axe.get()).contains("Crushing"), "tungsten axe tooltip shows Crushing");
		check(tooltip(ctx, tungsten.chestplate.get()).contains("Immovable"), "tungsten armor tooltip shows its set bonus");
		server.runCommand("kill @e[type=zombie]");
		server.runOnServer(s -> {
			unequip(player(s));
			player(s).removeAllEffects();
		});
	}

	// Obsidian, Prismarine, Echo -------------------------------------------------------

	private void checkObsidian(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet obsidian = ModMaterials.OBSIDIAN;
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runCommand("gamemode creative @p");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			AlloyingRecipeInput input = new AlloyingRecipeInput(List.of(new ItemStack(Items.OBSIDIAN, 2), new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY));
			var recipe = s.getRecipeManager().getRecipeFor(ModRecipes.ALLOYING.get(), input, level);
			check(recipe.isPresent() && recipe.get().value().result().item().value() == obsidian.ingot.get(), "obsidian and iron alloy into Reinforced Obsidian");
			check(new ItemStack(obsidian.sword.get()).has(DataComponents.DAMAGE_RESISTANT), "obsidian gear survives explosions when dropped");
			check(new ItemStack(obsidian.shield.get()).get(DataComponents.BLOCKS_ATTACKS).damageReductions().size() == 2, "the obsidian shield blocks explosions from every side");

			equip(player, obsidian);
			float explosion = GearCombat.modifyIncomingDamage(player, level.damageSources().explosion(null, null), 10.0F);
			check(Math.abs(explosion - 4.0F) < 0.01F, "the full obsidian set takes 60% less explosion damage (took " + explosion + ")");

			// An explosion near the wearer leaves the ground intact; one far away doesn't.
			level.explode(null, 4.5, -60.0, 0.5, 3.0F, Level.ExplosionInteraction.TNT);
			check(!level.getBlockState(new BlockPos(4, -61, 0)).isAir(), "an explosion near the full obsidian set breaks no blocks");
			level.explode(null, 24.5, -60.0, 0.5, 3.0F, Level.ExplosionInteraction.TNT);
			check(level.getBlockState(new BlockPos(24, -61, 0)).isAir(), "an explosion far from the wearer breaks blocks as normal");
			unequip(player);
		});
		server.runCommand("fill 20 -63 -4 28 -60 4 minecraft:grass_block replace minecraft:air");
		server.runCommand("kill @e[type=item]");
		server.runCommand("gamemode survival @p");
		check(tooltip(ctx, obsidian.shield.get()).contains("Blast Wall"), "obsidian shield tooltip shows Blast Wall");
	}

	private void checkPrismarine(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet prismarine = ModMaterials.PRISMARINE;
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			check(s.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, GearExpansion.id("prismarine_scale"))).isPresent(), "recipe exists: prismarine_scale");
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(prismarine.sword.get()));
			DamageSource source = level.damageSources().playerAttack(player);
			var cod = EntityTypes.COD.create(level, EntitySpawnReason.COMMAND);
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			check(prismarine.sword.get().getAttackDamageBonus(cod, 10.0F, source) == 3.0F, "prismarine weapons deal 3 extra damage to sea creatures");
			check(prismarine.sword.get().getAttackDamageBonus(zombie, 10.0F, source) == 0.0F, "prismarine weapons deal normal damage on land");
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

			// Spined: melee attackers take damage when the shield blocks.
			zombie.setNoAi(true);
			zombie.setPos(player.getX() + 2, player.getY(), player.getZ());
			level.addFreshEntity(zombie);
			prismarine.behavior.onShieldBlock(prismarine, player, zombie, new ItemStack(prismarine.shield.get()), level.damageSources().mobAttack(zombie), 4.0F);
			check(zombie.getHealth() < zombie.getMaxHealth(), "the prismarine shield pricks melee attackers");
			equip(player, prismarine);
		});
		server.runCommand("weather rain");
		// Rain fades in over about a second before it counts.
		ctx.waitTicks(60);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			double mining = player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
			check(Math.abs(mining - 1.0) < 0.001, "the full prismarine set mines at full speed underwater (got " + mining + ")");
			check(player.getAttributeValue(Attributes.OXYGEN_BONUS) == 4.0, "each prismarine piece works like a level of Respiration");
			check(player.hasEffect(MobEffects.CONDUIT_POWER), "the full prismarine set grants Conduit Power in the rain");
			unequip(player);
			player.removeAllEffects();
		});
		server.runCommand("weather clear");
		server.runCommand("kill @e[type=zombie]");
		check(tooltip(ctx, prismarine.sword.get()).contains("Tidal"), "prismarine weapon tooltip shows Tidal");
	}

	private void checkEcho(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet echo = ModMaterials.ECHO;
		// Away from where earlier checks fight and kill mobs, so the sensor only hears the player.
		BlockPos sensor = new BlockPos(3, -60, 40);
		server.runCommand("tp @p 0 -60 40 0 0");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			AlloyingRecipeInput input = new AlloyingRecipeInput(List.of(new ItemStack(Items.ECHO_SHARD), new ItemStack(Items.SCULK, 4), new ItemStack(Items.IRON_INGOT, 2)));
			var recipe = s.getRecipeManager().getRecipeFor(ModRecipes.ALLOYING.get(), input, level);
			check(recipe.isPresent() && recipe.get().value().result().item().value() == echo.ingot.get(), "echo shards, sculk, and iron alloy into echo");

			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(echo.pickaxe.get()));
			check(SetBonuses.ECHO.muffles(player, GameEvent.BLOCK_DESTROY), "mining with an echo tool makes no vibrations");
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			check(!SetBonuses.ECHO.muffles(player, GameEvent.BLOCK_DESTROY), "mining with other tools still makes vibrations");
			// Putting boots on is itself a vibration, so do it before the sensor is placed.
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(echo.boots.get()));
		});
		ctx.waitTicks(20);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			s.overworld().setBlockAndUpdate(sensor, Blocks.SCULK_SENSOR.defaultBlockState());
			s.overworld().gameEvent(player, GameEvent.STEP, player.position());
		});
		ctx.waitTicks(10);
		check(server.computeOnServer(s -> SculkSensorBlock.getPhase(s.overworld().getBlockState(sensor)) == SculkSensorPhase.INACTIVE),
			"a sculk sensor doesn't hear footsteps in echo boots");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			s.overworld().gameEvent(player, GameEvent.STEP, player.position());
		});
		ctx.waitTicks(10);
		check(server.computeOnServer(s -> SculkSensorBlock.getPhase(s.overworld().getBlockState(sensor)) != SculkSensorPhase.INACTIVE),
			"a sculk sensor hears ordinary footsteps");
		server.runCommand("setblock 3 -60 40 minecraft:air");
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			equip(player, echo);
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			check(Math.abs(player.getVisibilityPercent(level, zombie) - 0.5) < 0.001, "hostile mobs notice the full echo set from half as far");
			player.setShiftKeyDown(true);
			check(SetBonuses.ECHO.muffles(player, GameEvent.ENTITY_ACTION), "sneaking in the full echo set makes no vibrations");
			player.setShiftKeyDown(false);
			check(!SetBonuses.ECHO.muffles(player, GameEvent.ENTITY_ACTION), "walking normally in the full echo set still makes some vibrations");

			zombie.setNoAi(true);
			zombie.setPos(player.getX() + 2, player.getY(), player.getZ());
			level.addFreshEntity(zombie);
			echo.behavior.onShieldBlock(echo, player, zombie, new ItemStack(echo.shield.get()), level.damageSources().mobAttack(zombie), 4.0F);
			check(zombie.hasEffect(MobEffects.DARKNESS), "the echo shield gives melee attackers Darkness");
			unequip(player);
		});
		server.runCommand("kill @e[type=zombie]");
		server.runCommand("tp @p 0 -60 0 0 0");
		check(tooltip(ctx, echo.pickaxe.get()).contains("Muffled"), "echo tool tooltip shows Muffled");
		check(tooltip(ctx, echo.chestplate.get()).contains("Silence"), "echo armor tooltip shows its set bonus");
	}

	// Frostite, Fulgurite, Verdantite --------------------------------------------------

	private void checkFrostite(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet frostite = ModMaterials.FROSTITE;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			check(!canMine(Items.STONE_PICKAXE, frostite.ore.get().defaultBlockState()), "a stone pickaxe can't mine frostite ore");
			check(canMine(Items.IRON_PICKAXE, frostite.ore.get().defaultBlockState()), "an iron pickaxe mines frostite ore");

			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			frostite.behavior.onHurtEnemy(frostite, new ItemStack(frostite.sword.get()), zombie, player);
			check(zombie.hasEffect(MobEffects.SLOWNESS) && zombie.getTicksFrozen() == 40, "frostite weapons slow and start to freeze their targets");

			check(!PowderSnowBlock.canEntityWalkOnPowderSnow(player), "players sink into powder snow without the right boots");
			player.setItemSlot(EquipmentSlot.FEET, new ItemStack(frostite.boots.get()));
			check(PowderSnowBlock.canEntityWalkOnPowderSnow(player), "frostite boots walk on powder snow");
			check(!player.canFreeze(), "frostite armor keeps the wearer from freezing");
			player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			equip(player, frostite);

			GearCombat.modifyIncomingDamage(player, level.damageSources().mobAttack(zombie), 2.0F);
			check(zombie.getEffect(MobEffects.SLOWNESS) != null, "the full frostite set slows melee attackers");
		});
		// Sprinting next to still water in the full set freezes it.
		server.runCommand("fill 30 -61 0 34 -61 4 minecraft:water");
		server.runCommand("setblock 32 -61 2 minecraft:stone");
		server.runCommand("tp @p 32.5 -60 2.5");
		ctx.waitTicks(3);
		server.runOnServer(s -> player(s).setSprinting(true));
		ctx.waitTicks(3);
		check(server.computeOnServer(s -> s.overworld().getBlockState(new BlockPos(31, -61, 2)).is(Blocks.FROSTED_ICE)),
			"sprinting in the full frostite set freezes water underfoot");
		server.runOnServer(s -> {
			player(s).setSprinting(false);
			unequip(player(s));
			player(s).removeAllEffects();
		});
		server.runCommand("fill 30 -61 0 34 -61 4 minecraft:grass_block");
		server.runCommand("tp @p 0 -60 0 0 0");
		check(tooltip(ctx, frostite.sword.get()).contains("Frostbite"), "frostite weapon tooltip shows Frostbite");
	}

	private void checkFulgurite(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet fulgurite = ModMaterials.FULGURITE;
		// Lightning striking sand fuses it into Fulgurite.
		server.runCommand("fill 39 -63 -1 41 -61 1 minecraft:sand");
		server.runCommand("summon minecraft:lightning_bolt 40 -60 0");
		ctx.waitTicks(5);
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = player(s);
			check(level.getBlockState(new BlockPos(40, -61, 0)).is(ModBlocks.FULGURITE.get()), "lightning striking sand fuses it into Fulgurite");
			var drops = Block.getDrops(ModBlocks.FULGURITE.get().defaultBlockState(), level, player.blockPosition(), null, player, new ItemStack(Items.IRON_PICKAXE));
			int shards = drops.stream().filter(stack -> stack.is(fulgurite.ingot.get())).mapToInt(ItemStack::getCount).sum();
			check(shards >= 2 && shards <= 4, "fulgurite breaks into 2 to 4 shards (got " + shards + ")");

			equip(player, fulgurite);
			float lightning = GearCombat.modifyIncomingDamage(player, level.damageSources().lightningBolt(), 10.0F);
			check(lightning == 0.0F, "the full fulgurite set takes no lightning damage (took " + lightning + ")");
		});
		server.runCommand("fill 39 -63 -1 41 -60 1 minecraft:grass_block");
		server.runCommand("fill 39 -60 -1 41 -59 1 minecraft:air");
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runCommand("weather thunder");
		// Rain fades in over about a second before it counts.
		ctx.waitTicks(60);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			check(player.hasEffect(MobEffects.SPEED) && player.hasEffect(MobEffects.STRENGTH), "the full fulgurite set grants Speed and Strength in a storm");

			// A falling (critical) hit in a thunderstorm arcs to nearby mobs.
			Zombie first = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			Zombie second = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			for (Zombie zombie : List.of(first, second)) {
				zombie.setNoAi(true);
				level.addFreshEntity(zombie);
			}
			first.setPos(player.getX() + 2, player.getY(), player.getZ());
			second.setPos(player.getX() + 4, player.getY(), player.getZ());
			player.fallDistance = 1.0F;
			player.setOnGround(false);
			fulgurite.behavior.onHurtEnemy(fulgurite, new ItemStack(fulgurite.sword.get()), first, player);
			player.fallDistance = 0.0F;
			player.setOnGround(true);
			check(second.getHealth() < second.getMaxHealth(), "fulgurite critical hits chain lightning to nearby mobs in a storm");

			fulgurite.behavior.onShieldBlock(fulgurite, player, first, new ItemStack(fulgurite.shield.get()), level.damageSources().mobAttack(first), 4.0F);
			check(first.getHealth() < first.getMaxHealth(), "the fulgurite shield shocks melee attackers");
			unequip(player);
			player.removeAllEffects();
		});
		server.runCommand("weather clear");
		server.runCommand("kill @e[type=zombie]");
		server.runCommand("kill @e[type=lightning_bolt]");
		check(tooltip(ctx, fulgurite.sword.get()).contains("Chain Lightning"), "fulgurite weapon tooltip shows Chain Lightning");
	}

	private void checkVerdantite(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet verdantite = ModMaterials.VERDANTITE;
		server.runCommand("tp @p 50 -60 4 180 60");
		server.runCommand("fill 47 -60 -3 57 -58 3 minecraft:air");
		server.runCommand("fill 47 -61 -3 57 -61 3 minecraft:grass_block");
		server.runCommand("setblock 55 -60 0 minecraft:oak_log");
		ctx.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			check(canMine(Items.STONE_PICKAXE, verdantite.ore.get().defaultBlockState()), "a stone pickaxe mines verdantite ore");

			// The wide hoe tills a 3x3 patch.
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(verdantite.hoe.get()));
			BlockPos center = new BlockPos(50, -61, 0);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(center).relative(Direction.UP, 0.5), Direction.UP, center, false);
			verdantite.hoe.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
			long farmland = BlockPos.betweenClosedStream(center.offset(-1, 0, -1), center.offset(1, 0, 1))
				.filter(pos -> level.getBlockState(pos).is(Blocks.FARMLAND)).count();
			check(farmland == 9, "the verdantite hoe tills a 3x3 patch (tilled " + farmland + ")");

			// The axe replants the bottom log of a tree.
			BlockPos log = new BlockPos(55, -60, 0);
			ItemStack axe = new ItemStack(verdantite.axe.get());
			player.setItemSlot(EquipmentSlot.MAINHAND, axe);
			verdantite.behavior.onBlockBroken(verdantite, player, axe, level.getBlockState(log), log);
			level.removeBlock(log, false);
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

			// Bees leave verdantite wearers alone.
			Bee bee = EntityTypes.BEE.create(level, EntitySpawnReason.COMMAND);
			bee.setTarget(player);
			check(bee.getTarget() == player, "bees can target players normally");
			bee.setTarget(null);
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(verdantite.helmet.get()));
			bee.setTarget(player);
			check(bee.getTarget() == null, "bees leave anyone wearing verdantite armor alone");

			// Regrowth: the shield mends while held.
			ItemStack shield = new ItemStack(verdantite.shield.get());
			shield.setDamageValue(10);
			player.setItemSlot(EquipmentSlot.OFFHAND, shield);

			// Overgrowth: wheat planted around the farmland grows faster; tested at a high chance so it's quick.
			BlockPos.betweenClosed(center.offset(-1, 1, -1), center.offset(1, 1, 1)).forEach(pos -> level.setBlockAndUpdate(pos, Blocks.WHEAT.defaultBlockState()));
			equip(player, verdantite);
			GearExpansionConfig.get().verdantiteGrowthChance = 100;
		});
		ctx.waitTicks(105);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			BlockState replanted = level.getBlockState(new BlockPos(55, -60, 0));
			check(replanted.is(Blocks.OAK_SAPLING), "the verdantite axe replants an oak sapling (found " + replanted + ")");
			check(player.getItemBySlot(EquipmentSlot.OFFHAND).getDamageValue() < 10, "the verdantite shield slowly repairs itself while held");
			BlockPos center = new BlockPos(50, -61, 0);
			int age = BlockPos.betweenClosedStream(center.offset(-1, 1, -1), center.offset(1, 1, 1))
				.mapToInt(pos -> level.getBlockState(pos).getValue(CropBlock.AGE)).sum();
			check(age > 0, "crops near the full verdantite set grow faster (total age " + age + ")");
			check(SetBonuses.VERDANTITE.isTending(player), "the full verdantite set counts as tending the fields near farmland");
			// Enough exhaustion to cost a point of saturation on the next tick, if it were counted.
			player.getFoodData().setSaturation(5.0F);
			player.causeFoodExhaustion(8.0F);
		});
		ctx.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.getFoodData().getSaturationLevel() == 5.0F, "working the fields in the full verdantite set costs no hunger");
			GearExpansionConfig.get().verdantiteGrowthChance = 5;
			unequip(player);
			player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
			player.removeAllEffects();
		});
		server.runCommand("fill 47 -60 -3 57 -58 3 minecraft:air");
		server.runCommand("fill 47 -61 -3 57 -61 3 minecraft:grass_block");
		server.runCommand("kill @e[type=item]");
		server.runCommand("tp @p 0 -60 0 0 0");
		check(tooltip(ctx, verdantite.hoe.get()).contains("Wide"), "verdantite hoe tooltip shows Wide");
		check(tooltip(ctx, verdantite.chestplate.get()).contains("Overgrowth"), "verdantite armor tooltip shows its set bonus");
	}

	private void checkEmerald(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet emerald = ModMaterials.EMERALD;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();
			Pillager pillager = EntityTypes.PILLAGER.create(level, EntitySpawnReason.COMMAND);
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(emerald.sword.get()));
			DamageSource source = level.damageSources().playerAttack(player);
			Item sword = emerald.sword.get();
			check(sword.getAttackDamageBonus(pillager, 10.0F, source) == 5.0F, "emerald weapons deal 50% more damage to illagers");
			check(sword.getAttackDamageBonus(zombie, 10.0F, source) == 0.0F, "emerald weapons deal normal damage to other mobs");
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			check(player.getAttributeValue(Attributes.LUCK) == 0, "luck starts at zero");
			equip(player, emerald);
		});
		ctx.waitTicks(25);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			check(player.getAttributeValue(Attributes.LUCK) == 4, "each emerald armor piece adds 1 Luck");
			check(player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE), "the full emerald set grants Hero of the Village");
			unequip(player);
			player.removeAllEffects();
		});
	}

	// Amethyst ------------------------------------------------------------------------

	private void checkAmethyst(ClientGameTestContext ctx, TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			equip(player, ModMaterials.AMETHYST);
			player.setHealth(player.getMaxHealth());
			boolean hurt = player.hurtServer(s.overworld(), s.overworld().damageSources().generic(), 4.0F);
			check(!hurt && player.getHealth() == player.getMaxHealth(), "the amethyst crystal shell absorbs the first hit");
		});
		ctx.waitTicks(25);
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			player.hurtServer(s.overworld(), s.overworld().damageSources().generic(), 4.0F);
			check(player.getHealth() < player.getMaxHealth(), "once shattered, the shell lets hits through until it regrows");
			player.setHealth(player.getMaxHealth());
			unequip(player);
		});
	}

	// Verdigris -----------------------------------------------------------------------

	private void checkVerdigris(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet verdigris = ModMaterials.VERDIGRIS;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ItemStack pickaxe = new ItemStack(verdigris.pickaxe.get());
			float freshSpeed = pickaxe.getDestroySpeed(Blocks.STONE.defaultBlockState());
			VerdigrisBehavior.setStage(pickaxe, VerdigrisBehavior.OXIDIZED);
			check(pickaxe.getDestroySpeed(Blocks.STONE.defaultBlockState()) < freshSpeed, "oxidized verdigris tools mine slower");
			check(attackSpeed(pickaxe) < attackSpeed(new ItemStack(verdigris.pickaxe.get())), "oxidized verdigris tools attack slower");

			ItemStack chestplate = new ItemStack(verdigris.chestplate.get());
			double freshArmor = armor(chestplate);
			VerdigrisBehavior.setStage(chestplate, VerdigrisBehavior.OXIDIZED);
			check(armor(chestplate) > freshArmor, "oxidized verdigris armor protects more");

			ItemStack honeycomb = new ItemStack(Items.HONEYCOMB, 2);
			check(verdigris.behavior.onStackedOn(verdigris, pickaxe, honeycomb, player) && VerdigrisBehavior.waxed(pickaxe) && honeycomb.getCount() == 1,
				"honeycomb waxes verdigris gear");
			ItemStack axe = new ItemStack(Items.IRON_AXE);
			verdigris.behavior.onStackedOn(verdigris, pickaxe, axe, player);
			check(!VerdigrisBehavior.waxed(pickaxe) && VerdigrisBehavior.stage(pickaxe) == VerdigrisBehavior.OXIDIZED, "an axe scrapes the wax off first");
			verdigris.behavior.onStackedOn(verdigris, pickaxe, axe, player);
			check(VerdigrisBehavior.stage(pickaxe) == VerdigrisBehavior.OXIDIZED - 1, "then an axe scrapes back one oxidation stage");

			equip(player, verdigris);
			BlockPos nearby = player.blockPosition().offset(5, 0, 5);
			check(SetBonuses.VERDIGRIS.redirectLightning(s.overworld(), nearby).equals(player.blockPosition()), "lightning nearby is drawn to the full verdigris set");
			boolean hurt = player.hurtServer(s.overworld(), s.overworld().damageSources().lightningBolt(), 5.0F);
			check(!hurt && player.hasEffect(MobEffects.SPEED) && player.hasEffect(MobEffects.STRENGTH), "lightning charges the verdigris wearer instead of hurting them");
			unequip(player);
			player.removeAllEffects();
		});
	}

	private static double armor(ItemStack stack) {
		var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
		return modifiers == null ? 0 : modifiers.modifiers().stream()
			.filter(entry -> entry.attribute().is(Attributes.ARMOR) || entry.attribute().is(Attributes.ARMOR_TOUGHNESS))
			.mapToDouble(entry -> entry.modifier().amount())
			.sum();
	}

	// Infernium -----------------------------------------------------------------------

	private void checkInfernium(ClientGameTestContext ctx, TestServerContext server) {
		MaterialSet infernium = ModMaterials.INFERNIUM;
		server.runOnServer(s -> {
			ServerPlayer player = player(s);
			ServerLevel level = s.overworld();

			var drops = Block.getDrops(Blocks.IRON_ORE.defaultBlockState(), level, player.blockPosition(), null, player, new ItemStack(infernium.pickaxe.get()));
			check(drops.stream().anyMatch(stack -> stack.is(Items.IRON_INGOT)), "infernium pickaxes smelt what they mine");
			check(new ItemStack(infernium.sword.get()).has(DataComponents.DAMAGE_RESISTANT), "infernium gear doesn't burn");

			// Templates turn up in bastion treasure chests about half the time.
			LootTable treasure = s.reloadableRegistries().getLootTable(BuiltInLootTables.BASTION_TREASURE);
			LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, player.position()).create(LootContextParamSets.CHEST);
			int found = 0;
			for (int i = 0; i < 40; i++) {
				found += treasure.getRandomItems(params).stream().filter(stack -> stack.is(infernium.upgradeTemplate.get())).count() > 0 ? 1 : 0;
			}
			GearExpansion.LOGGER.info("[GameTest] infernium templates in {} of 40 bastion treasure chests", found);
			check(found > 5 && found < 35, "infernium upgrade templates appear in bastion treasure chests");

			equip(player, infernium);
			boolean burned = player.hurtServer(level, level.damageSources().lava(), 4.0F);
			check(!burned, "the full infernium set protects from lava while the heat gauge isn't full");
		});
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runCommand("summon minecraft:zombie 2 -60 0 {NoAI:1b}");
		ctx.waitTicks(5);
		server.runOnServer(s -> {
			SetBonuses.useAbility(player(s));
			check(zombie(s).isOnFire(), "Eruption sets nearby mobs alight");
			unequip(player(s));
		});
		server.runCommand("kill @e[type=zombie]");
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
