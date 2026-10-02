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

	public static GearExpansionConfig get() {
		return instance;
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
				JsonObject object = GSON.fromJson(json, JsonObject.class);
				for (Field field : fields()) {
					JsonElement value = object == null ? null : object.get(field.getName());
					if (value != null) {
						field.set(loaded, GSON.fromJson(value, field.getType()));
					}
				}
			} catch (IOException | RuntimeException | IllegalAccessException e) {
				GearExpansion.LOGGER.error("Couldn't read {}; using default settings", path, e);
			}
		}
		instance = loaded;
		save();
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
