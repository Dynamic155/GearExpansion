package com.gearexpansion.config;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import dev.architectury.platform.Platform;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.autogen.AutoGen;
import dev.isxander.yacl3.config.v2.api.autogen.IntSlider;
import dev.isxander.yacl3.config.v2.api.autogen.TickBox;

import com.gearexpansion.GearExpansion;

/**
 * Gear Expansion settings, saved to {@code config/gearexpansion.json5}.
 *
 * <p>Gear Expansion reads and writes this file itself, so YACL is optional. When YACL is
 * installed, it builds the in-game settings screen from the annotations below (see
 * {@code client.YaclConfigScreen}); without it, the file can still be edited by hand.
 * The YACL annotations are only read by YACL, so they're harmless when it's missing.
 */
public final class GearExpansionConfig {
	public static final String FILE_NAME = "gearexpansion.json5";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile GearExpansionConfig instance = new GearExpansionConfig();
	// The server's settings while connected to one, so gameplay and tooltips match it. Never saved.
	private static volatile GearExpansionConfig serverOverride;

	// Zinc

	@AutoGen(category = "zinc", group = "gear")
	@TickBox
	@SerialEntry
	@Comment("Whether zinc gear is corrosion-proof: it loses no durability while its user is in water.")
	public boolean zincCorrosionProof = true;

	@AutoGen(category = "zinc", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Zinc set grants its Galvanized bonus.")
	public boolean zincSetBonus = true;

	@AutoGen(category = "zinc", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Percent shorter that Poison, Hunger, and Nausea last while wearing the full set.")
	public int zincEffectReduction = 50;

	// Verdigris

	@AutoGen(category = "verdigris", group = "gear")
	@IntSlider(min = 1, max = 120, step = 1, format = "%d min")
	@SerialEntry
	@Comment("Average minutes of use for verdigris gear to oxidize one stage.")
	public int verdigrisMinutesPerStage = 20;

	@AutoGen(category = "verdigris", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Verdigris set grants its Conductive bonus.")
	public boolean verdigrisSetBonus = true;

	@AutoGen(category = "verdigris", group = "set_bonus")
	@IntSlider(min = 0, max = 64, step = 4)
	@SerialEntry
	@Comment("Blocks within which lightning is drawn to the wearer.")
	public int verdigrisLightningRange = 16;

	@AutoGen(category = "verdigris", group = "set_bonus")
	@IntSlider(min = 10, max = 600, step = 10, format = "%ds")
	@SerialEntry
	@Comment("Seconds before a lightning strike can charge the wearer again.")
	public int verdigrisChargeCooldown = 120;

	// Rose Gold

	@AutoGen(category = "rose_gold", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Rose Gold set grants its Lucky Charm bonus.")
	public boolean roseGoldSetBonus = true;

	@AutoGen(category = "rose_gold", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Percent more experience from experience orbs while wearing the full set.")
	public int roseGoldExperienceBonus = 25;

	@AutoGen(category = "rose_gold", group = "set_bonus")
	@IntSlider(min = 0, max = 15, step = 1)
	@SerialEntry
	@Comment("Extra bookshelves an enchanting table counts while wearing the full set (15 is the useful maximum).")
	public int roseGoldEnchantingBookshelves = 3;

	// Aluminum

	@AutoGen(category = "aluminum", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Aluminum set grants its Featherweight bonus.")
	public boolean aluminumSetBonus = true;

	@AutoGen(category = "aluminum", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Percent less fall damage taken while wearing the full set.")
	public int aluminumFallDamageReduction = 50;

	@AutoGen(category = "aluminum", group = "set_bonus")
	@IntSlider(min = 0, max = 20, step = 1, format = "%d%%")
	@SerialEntry
	@Comment("Percent stronger jumps while wearing the full set. 10% or more lets players jump over fences.")
	public int aluminumJumpBoost = 5;

	@AutoGen(category = "aluminum", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 1, format = "%d%%")
	@SerialEntry
	@Comment("Extra movement speed in water while wearing the full set. Depth Strider I is 33%.")
	public int aluminumWaterSpeed = 33;

	// Brass

	@AutoGen(category = "brass", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Brass set grants its Clockwork bonus and Spring Release ability.")
	public boolean brassSetBonus = true;

	@AutoGen(category = "brass", group = "set_bonus")
	@IntSlider(min = 5, max = 120, step = 5, format = "%ds")
	@SerialEntry
	@Comment("Seconds of walking to fully wind the spring. Hits and blocks wind it faster.")
	public int brassWindUpSeconds = 30;

	@AutoGen(category = "brass", group = "set_bonus")
	@IntSlider(min = 1, max = 20, step = 1)
	@SerialEntry
	@Comment("Damage dealt to each mob hit by Spring Release.")
	public int brassReleaseDamage = 6;

	@AutoGen(category = "brass", group = "gear")
	@TickBox
	@SerialEntry
	@Comment("Whether consecutive hits with brass weapons attack faster (up to 3 stacks).")
	public boolean brassCombo = true;

	// Silver

	@AutoGen(category = "silver", group = "gear")
	@IntSlider(min = 0, max = 20, step = 1)
	@SerialEntry
	@Comment("Extra damage silver weapons deal to undead.")
	public int silverUndeadDamageBonus = 4;

	@AutoGen(category = "silver", group = "gear")
	@IntSlider(min = 0, max = 25, step = 1, format = "%d%%")
	@SerialEntry
	@Comment("Less damage taken from undead for each silver armor piece worn.")
	public int silverUndeadProtection = 6;

	@AutoGen(category = "silver", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Silver set grants its Blessed bonus.")
	public boolean silverSetBonus = true;

	@AutoGen(category = "silver", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("How much shorter Wither lasts while wearing the full set.")
	public int silverWitherReduction = 50;

	@AutoGen(category = "silver", group = "set_bonus")
	@IntSlider(min = 0, max = 32, step = 1)
	@SerialEntry
	@Comment("Blocks within which undead glow while wearing the full set. 0 turns it off.")
	public int silverUndeadSenseRange = 8;

	// Emerald

	@AutoGen(category = "emerald", group = "gear")
	@IntSlider(min = 0, max = 200, step = 10, format = "%d%%")
	@SerialEntry
	@Comment("Extra damage emerald weapons deal to illagers and other raiders.")
	public int emeraldRaiderDamageBonus = 50;

	@AutoGen(category = "emerald", group = "gear")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Chance an emerald pickaxe drops bonus experience from ores.")
	public int emeraldBonusExperienceChance = 25;

	@AutoGen(category = "emerald", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Emerald set grants Hero of the Village (Merchant's Favor).")
	public boolean emeraldSetBonus = true;

	// Amethyst

	@AutoGen(category = "amethyst", group = "gear")
	@IntSlider(min = 0, max = 10, step = 1)
	@SerialEntry
	@Comment("Damage a critical hit with an amethyst sword deals to other mobs nearby.")
	public int amethystShatterDamage = 3;

	@AutoGen(category = "amethyst", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Amethyst set grants its Shatterguard crystal shell.")
	public boolean amethystSetBonus = true;

	@AutoGen(category = "amethyst", group = "set_bonus")
	@IntSlider(min = 5, max = 300, step = 5, format = "%ds")
	@SerialEntry
	@Comment("Seconds for the crystal shell to regrow after it absorbs a hit.")
	public int amethystShellRegrowSeconds = 45;

	// Sakura

	@AutoGen(category = "sakura", group = "gear")
	@IntSlider(min = 0, max = 10, step = 1)
	@SerialEntry
	@Comment("Health (in half hearts) a sakura weapon restores when it finishes off a mob.")
	public int sakuraKillHeal = 2;

	@AutoGen(category = "sakura", group = "gear")
	@IntSlider(min = 0, max = 10, step = 1)
	@SerialEntry
	@Comment("Health (in half hearts) the sakura shield restores when it blocks, at most once a second.")
	public int sakuraBlockHeal = 1;

	@AutoGen(category = "sakura", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Sakura set grants its Hanami bonus.")
	public boolean sakuraSetBonus = true;

	@AutoGen(category = "sakura", group = "set_bonus")
	@IntSlider(min = 0, max = 8, step = 1)
	@SerialEntry
	@Comment("Blocks within which flowers, cherry leaves, or pink petals give Regeneration. 0 turns it off.")
	public int sakuraFlowerRange = 3;

	@AutoGen(category = "sakura", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Sakura set leaves a trail of falling cherry petals as you walk.")
	public boolean sakuraPetalTrail = true;

	// Steel

	@AutoGen(category = "steel", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Steel set grants its Hardened bonus.")
	public boolean steelSetBonus = true;

	@AutoGen(category = "steel", group = "set_bonus")
	@IntSlider(min = 0, max = 8, step = 1)
	@SerialEntry
	@Comment("Extra armor toughness while wearing the full set.")
	public int steelToughnessBonus = 2;

	// Titanium

	@AutoGen(category = "titanium", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Titanium set grants its Unbreakable Will bonus.")
	public boolean titaniumSetBonus = true;

	@AutoGen(category = "titanium", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Percent less durability gear loses while wearing the full set.")
	public int titaniumDurabilitySaving = 50;

	@AutoGen(category = "titanium", group = "set_bonus")
	@IntSlider(min = 10, max = 90, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Percent of max health below which Resistance kicks in.")
	public int titaniumResistanceThreshold = 30;

	@AutoGen(category = "titanium", group = "set_bonus")
	@IntSlider(min = 1, max = 20, step = 1, format = "%ds")
	@SerialEntry
	@Comment("How many seconds of Resistance I are granted.")
	public int titaniumResistanceSeconds = 5;

	@AutoGen(category = "titanium", group = "set_bonus")
	@IntSlider(min = 10, max = 600, step = 10, format = "%ds")
	@SerialEntry
	@Comment("Seconds before the Resistance bonus can trigger again.")
	public int titaniumResistanceCooldown = 60;

	// Cobalt

	@AutoGen(category = "cobalt", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Cobalt set grants its Overdrive bonus.")
	public boolean cobaltSetBonus = true;

	@AutoGen(category = "cobalt", group = "set_bonus")
	@IntSlider(min = 1, max = 32, step = 1)
	@SerialEntry
	@Comment("Blocks broken in a row for each level of Haste (up to Haste II).")
	public int cobaltOverdriveBlocks = 6;

	@AutoGen(category = "cobalt", group = "set_bonus")
	@IntSlider(min = 1, max = 10, step = 1, format = "%ds")
	@SerialEntry
	@Comment("Most seconds between blocks before the chain breaks. Haste also lasts this long.")
	public int cobaltOverdriveWindow = 2;

	// Tungsten

	@AutoGen(category = "tungsten", group = "gear")
	@IntSlider(min = 0, max = 20, step = 1)
	@SerialEntry
	@Comment("Extra knockback from tungsten axes and spears, in tenths (6 = 0.6, like Knockback I is 0.5).")
	public int tungstenKnockback = 6;

	@AutoGen(category = "tungsten", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Tungsten set grants its Immovable bonus and Ground Slam ability.")
	public boolean tungstenSetBonus = true;

	@AutoGen(category = "tungsten", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Less damage from explosions while wearing the full set.")
	public int tungstenExplosionReduction = 40;

	@AutoGen(category = "tungsten", group = "set_bonus")
	@IntSlider(min = 1, max = 20, step = 1)
	@SerialEntry
	@Comment("Damage Ground Slam deals to each mob nearby.")
	public int tungstenSlamDamage = 4;

	@AutoGen(category = "tungsten", group = "set_bonus")
	@IntSlider(min = 5, max = 120, step = 5, format = "%ds")
	@SerialEntry
	@Comment("Seconds before Ground Slam can be used again.")
	public int tungstenSlamCooldown = 20;

	// Obsidian

	@AutoGen(category = "obsidian", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Obsidian set grants its Blastproof bonus.")
	public boolean obsidianSetBonus = true;

	@AutoGen(category = "obsidian", group = "set_bonus")
	@IntSlider(min = 0, max = 32, step = 1)
	@SerialEntry
	@Comment("Explosions within this many blocks of a wearer break no blocks. 0 turns it off.")
	public int obsidianBlastproofRange = 8;

	@AutoGen(category = "obsidian", group = "set_bonus")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Less damage from explosions while wearing the full set.")
	public int obsidianExplosionReduction = 60;

	// Prismarine

	@AutoGen(category = "prismarine", group = "gear")
	@IntSlider(min = 0, max = 20, step = 1)
	@SerialEntry
	@Comment("Extra damage prismarine weapons deal to sea creatures (Impaling V is 12.5).")
	public int prismarineAquaticDamageBonus = 3;

	@AutoGen(category = "prismarine", group = "gear")
	@IntSlider(min = 0, max = 10, step = 1)
	@SerialEntry
	@Comment("Damage the prismarine shield deals back to melee attackers when it blocks.")
	public int prismarineShieldThorns = 2;

	@AutoGen(category = "prismarine", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Prismarine set grants Conduit Power in water or rain (Tidebound).")
	public boolean prismarineSetBonus = true;

	// Echo

	@AutoGen(category = "echo", group = "gear")
	@IntSlider(min = 0, max = 20, step = 1, format = "%ds")
	@SerialEntry
	@Comment("Seconds of Darkness the echo shield gives melee attackers when it blocks.")
	public int echoShieldDarknessSeconds = 3;

	@AutoGen(category = "echo", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Echo set grants its Silence bonus.")
	public boolean echoSetBonus = true;

	@AutoGen(category = "echo", group = "set_bonus")
	@IntSlider(min = 0, max = 90, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("How much closer hostile mobs must be to notice the wearer.")
	public int echoDetectionReduction = 50;

	// Frostite

	@AutoGen(category = "frostite", group = "gear")
	@IntSlider(min = 0, max = 140, step = 10)
	@SerialEntry
	@Comment("Freezing a frostite weapon adds per hit, in ticks. A mob is fully frozen at 140, like powder snow.")
	public int frostiteFreezeTicks = 40;

	@AutoGen(category = "frostite", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Frostite set grants its Permafrost bonus.")
	public boolean frostiteSetBonus = true;

	@AutoGen(category = "frostite", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether sprinting in the full set freezes water underfoot, like a weaker Frost Walker.")
	public boolean frostiteFreezeWater = true;

	// Fulgurite

	@AutoGen(category = "fulgurite", group = "gear")
	@IntSlider(min = 0, max = 100, step = 5, format = "%d%%")
	@SerialEntry
	@Comment("Chance each sand block around a lightning strike becomes Fulgurite. The struck block always does.")
	public int fulguriteFormChance = 40;

	@AutoGen(category = "fulgurite", group = "gear")
	@IntSlider(min = 0, max = 20, step = 1)
	@SerialEntry
	@Comment("Damage each chain of lightning from a fulgurite weapon's critical hit deals.")
	public int fulguriteChainDamage = 5;

	@AutoGen(category = "fulgurite", group = "gear")
	@IntSlider(min = 0, max = 10, step = 1)
	@SerialEntry
	@Comment("Damage the fulgurite shield deals to melee attackers when it blocks (doubled in thunderstorms).")
	public int fulguriteShieldShock = 2;

	@AutoGen(category = "fulgurite", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Fulgurite set grants its Stormcaller bonus.")
	public boolean fulguriteSetBonus = true;

	// Verdantite

	@AutoGen(category = "verdantite", group = "gear")
	@TickBox
	@SerialEntry
	@Comment("Whether verdantite hoes and shovels work on 3x3 areas (sneak for a single block).")
	public boolean verdantiteWideTools = true;

	@AutoGen(category = "verdantite", group = "gear")
	@TickBox
	@SerialEntry
	@Comment("Whether the verdantite axe plants a sapling where it fells the bottom log of a tree.")
	public boolean verdantiteReplanting = true;

	@AutoGen(category = "verdantite", group = "gear")
	@TickBox
	@SerialEntry
	@Comment("Whether bees leave alone anyone wearing a piece of verdantite armor.")
	public boolean verdantiteBeeFriend = true;

	@AutoGen(category = "verdantite", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Verdantite set grants its Overgrowth bonus.")
	public boolean verdantiteSetBonus = true;

	@AutoGen(category = "verdantite", group = "set_bonus")
	@IntSlider(min = 0, max = 50, step = 1, format = "%d%%")
	@SerialEntry
	@Comment("Chance each second that a growing plant near the wearer gets an extra growth tick.")
	public int verdantiteGrowthChance = 5;

	// Infernium

	@AutoGen(category = "infernium", group = "gear")
	@IntSlider(min = 0, max = 10, step = 1)
	@SerialEntry
	@Comment("Extra damage infernium weapons deal to burning targets.")
	public int infernumBurningDamageBonus = 3;

	@AutoGen(category = "infernium", group = "set_bonus")
	@TickBox
	@SerialEntry
	@Comment("Whether the full Infernium set grants its Heat Core bonus and Eruption ability.")
	public boolean inferniumSetBonus = true;

	@AutoGen(category = "infernium", group = "set_bonus")
	@IntSlider(min = 0, max = 60, step = 1, format = "%ds")
	@SerialEntry
	@Comment("Seconds the wearer can stay in lava unharmed before the heat gauge fills.")
	public int inferniumLavaSeconds = 10;

	@AutoGen(category = "infernium", group = "set_bonus")
	@IntSlider(min = 5, max = 300, step = 5, format = "%ds")
	@SerialEntry
	@Comment("Seconds before Eruption can be used again.")
	public int inferniumEruptionCooldown = 30;

	/** The settings in effect: the server's while connected to a remote server, otherwise this game's file. */
	public static GearExpansionConfig get() {
		GearExpansionConfig override = serverOverride;
		return override != null ? override : instance;
	}

	/** This game's settings, as a JSON object, to send to connecting players. */
	public static String toSyncJson() {
		JsonObject object = new JsonObject();
		for (Field field : fields()) {
			try {
				object.add(field.getName(), GSON.toJsonTree(field.get(instance)));
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		return GSON.toJson(object);
	}

	/** Uses the settings a server sent until {@link #clearServerSettings()}. The settings file is left alone. */
	public static void useServerSettings(String json) {
		try {
			serverOverride = read(GSON.fromJson(json, JsonObject.class));
			GearExpansion.LOGGER.info("Using the server's Gear Expansion settings");
		} catch (RuntimeException | IllegalAccessException e) {
			GearExpansion.LOGGER.error("Couldn't read the server's Gear Expansion settings; using this game's", e);
		}
	}

	/** Goes back to this game's own settings, after leaving a server. */
	public static void clearServerSettings() {
		serverOverride = null;
	}

	public static Path path() {
		return Platform.getConfigFolder().resolve(FILE_NAME);
	}

	/** Loads the settings file, keeping defaults for anything missing, then saves it so new settings appear. */
	public static void load() {
		GearExpansionConfig loaded = new GearExpansionConfig();
		Path path = path();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				// Lenient reading accepts the // comments the file is written with.
				JsonReader json = new JsonReader(reader);
				json.setStrictness(Strictness.LENIENT);
				loaded = read(GSON.fromJson(json, JsonObject.class));
			} catch (IOException | RuntimeException | IllegalAccessException e) {
				GearExpansion.LOGGER.error("Couldn't read {}; using default settings", path, e);
			}
		}
		instance = loaded;
		save();
	}

	/** Settings from a JSON object, keeping defaults for anything missing. */
	private static GearExpansionConfig read(JsonObject object) throws IllegalAccessException {
		GearExpansionConfig config = new GearExpansionConfig();
		for (Field field : fields()) {
			JsonElement value = object == null ? null : object.get(field.getName());
			if (value != null) {
				field.set(config, GSON.fromJson(value, field.getType()));
			}
		}
		return config;
	}

	/** Writes the current settings, with a comment above each one explaining it. */
	public static void save() {
		StringBuilder out = new StringBuilder("{\n");
		var settings = fields();
		for (int i = 0; i < settings.size(); i++) {
			Field field = settings.get(i);
			String comment = comment(field);
			if (!comment.isEmpty()) {
				out.append("\t// ").append(comment).append('\n');
			}
			try {
				out.append('\t').append(GSON.toJson(field.getName())).append(": ").append(GSON.toJson(field.get(instance)));
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
			out.append(i < settings.size() - 1 ? ",\n" : "\n");
		}
		out.append("}\n");
		try {
			Files.createDirectories(path().getParent());
			Files.writeString(path(), out);
		} catch (IOException e) {
			GearExpansion.LOGGER.error("Couldn't save {}", path(), e);
		}
	}

	/** The setting fields, in the order they're declared. */
	private static java.util.List<Field> fields() {
		java.util.List<Field> fields = new java.util.ArrayList<>();
		for (Field field : GearExpansionConfig.class.getDeclaredFields()) {
			int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
				fields.add(field);
			}
		}
		return fields;
	}

	private static String comment(Field field) {
		Comment comment = field.getAnnotation(Comment.class);
		return comment == null ? "" : comment.value();
	}
}
