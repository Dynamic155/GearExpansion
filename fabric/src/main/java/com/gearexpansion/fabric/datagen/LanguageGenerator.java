package com.gearexpansion.fabric.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.registry.ModBlocks;

/** English (en_us) names and text. Item names are built from each material's name. */
final class LanguageGenerator extends FabricLanguageProvider {
	private static final String CONFIG = "yacl3.config.gearexpansion:config";

	LanguageGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder t) {
		t.add("itemGroup.gearexpansion.blocks", "Gear Expansion: Blocks");
		t.add("itemGroup.gearexpansion.tools", "Gear Expansion: Tools");
		t.add("itemGroup.gearexpansion.combat", "Gear Expansion: Combat");
		t.add("itemGroup.gearexpansion.ingredients", "Gear Expansion: Ingredients");
		t.add("tooltip.gearexpansion.set", "%s Set (%s/%s)");
		t.add("trait.gearexpansion.corrosion_proof", "Corrosion-proof: no durability loss in water");
		t.add("trait.gearexpansion.lightweight_shield", "Lightweight: no slowdown while blocking");

		t.add(ModBlocks.ALLOY_FORGE.get(), "Alloy Forge");
		t.add("container.gearexpansion.alloy_forge", "Alloy Forge");

		for (MaterialSet set : ModMaterials.ALL) {
			String name = title(set.name);
			String ore = title(set.oreName);
			if (set.isMetal) {
				t.add(set.storageBlock.get(), "Block of " + name);
				t.add(set.registeredIngot.get(), name + " Ingot");
				t.add(set.nugget.get(), name + " Nugget");
			} else if (set.registeredIngot != null) {
				t.add(set.registeredIngot.get(), title(set.ingotName));
			}
			if (set.hasOre) {
				t.add(set.ore.get(), ore + " Ore");
				if (set.deepslateOre != null) {
					t.add(set.deepslateOre.get(), "Deepslate " + ore + " Ore");
				}
				t.add(set.rawStorageBlock.get(), "Block of Raw " + ore);
				t.add(set.rawItem.get(), "Raw " + ore);
			}
			if (set.upgradeTemplate != null) {
				MaterialSet base = set.upgradedFrom.get();
				String prefix = "item.gearexpansion.smithing_template." + set.name + "_upgrade.";
				t.add(set.upgradeTemplate.get(), "Smithing Template");
				t.add(prefix + "applies_to", title(base.name) + " Equipment");
				t.add(prefix + "ingredients", name + " Ingot");
				t.add(prefix + "base_slot_description", "Add " + title(base.name).toLowerCase() + " armor, weapon, or tool");
				t.add(prefix + "additions_slot_description", "Add " + name + " Ingot");
			}
			t.add(set.sword.get(), name + " Sword");
			t.add(set.pickaxe.get(), name + " Pickaxe");
			t.add(set.axe.get(), name + " Axe");
			t.add(set.shovel.get(), name + " Shovel");
			t.add(set.hoe.get(), name + " Hoe");
			t.add(set.spear.get(), name + " Spear");
			t.add(set.helmet.get(), name + " Helmet");
			t.add(set.chestplate.get(), name + " Chestplate");
			t.add(set.leggings.get(), name + " Leggings");
			t.add(set.boots.get(), name + " Boots");
			t.add(set.shield.get(), name + " Shield");
		}

		zinc(t);
		verdigris(t);
		roseGold(t);
		aluminum(t);
		brass(t);
		emerald(t);
		amethyst(t);
		titanium(t);
		infernium(t);
		abilities(t);
		advancements(t);

		t.add(CONFIG + ".title", "Gear Expansion");
		t.add(CONFIG + ".category.zinc", "Zinc");
		t.add(CONFIG + ".category.zinc.group.gear", "Gear");
		t.add(CONFIG + ".category.zinc.group.set_bonus", "Set Bonus: Galvanized");
		t.add(CONFIG + ".category.rose_gold", "Rose Gold");
		t.add(CONFIG + ".category.rose_gold.group.set_bonus", "Set Bonus: Lucky Charm");
		t.add(CONFIG + ".category.aluminum", "Aluminum");
		t.add(CONFIG + ".category.aluminum.group.set_bonus", "Set Bonus: Featherweight");
		t.add(CONFIG + ".category.titanium", "Titanium");
		t.add(CONFIG + ".category.titanium.group.set_bonus", "Set Bonus: Unbreakable Will");
		t.add(CONFIG + ".category.verdigris", "Verdigris");
		t.add(CONFIG + ".category.verdigris.group.gear", "Gear");
		t.add(CONFIG + ".category.verdigris.group.set_bonus", "Set Bonus: Conductive");
		t.add(CONFIG + ".category.brass", "Brass");
		t.add(CONFIG + ".category.brass.group.gear", "Gear");
		t.add(CONFIG + ".category.brass.group.set_bonus", "Set Bonus: Clockwork");
		t.add(CONFIG + ".category.emerald", "Emerald");
		t.add(CONFIG + ".category.emerald.group.gear", "Gear");
		t.add(CONFIG + ".category.emerald.group.set_bonus", "Set Bonus: Merchant's Favor");
		t.add(CONFIG + ".category.amethyst", "Amethyst");
		t.add(CONFIG + ".category.amethyst.group.gear", "Gear");
		t.add(CONFIG + ".category.amethyst.group.set_bonus", "Set Bonus: Shatterguard");
		t.add(CONFIG + ".category.infernium", "Infernium");
		t.add(CONFIG + ".category.infernium.group.gear", "Gear");
		t.add(CONFIG + ".category.infernium.group.set_bonus", "Set Bonus: Heat Core");
	}

	private static void verdigris(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.verdigris", "Verdigris");
		t.add("set_bonus.gearexpansion.verdigris.rod", "Conductive: lightning within %s blocks strikes you, harmlessly");
		t.add("set_bonus.gearexpansion.verdigris.charge", "A strike gives Speed and Strength for 10s (%ss cooldown)");
		t.add("set_bonus.gearexpansion.verdigris.charged", "Charged!");
		t.add("trait.gearexpansion.oxidation", "Oxidation: %s");
		t.add("trait.gearexpansion.oxidation_waxed", "Oxidation: %s (waxed)");
		t.add("trait.gearexpansion.oxidation.fresh", "Fresh");
		t.add("trait.gearexpansion.oxidation.exposed", "Exposed");
		t.add("trait.gearexpansion.oxidation.weathered", "Weathered");
		t.add("trait.gearexpansion.oxidation.oxidized", "Oxidized");
		t.add("trait.gearexpansion.oxidation_hint", "Oxidizes with use: slower but tougher. Right-click honeycomb onto it to wax, an axe to scrape.");

		option(t, "verdigrisMinutesPerStage", "Minutes per Stage", "Average minutes of use for verdigris gear to oxidize one stage.");
		option(t, "verdigrisSetBonus", "Enable Set Bonus", "Whether wearing the full Verdigris set grants Conductive.");
		option(t, "verdigrisLightningRange", "Lightning Range", "Blocks within which lightning is drawn to the wearer.");
		option(t, "verdigrisChargeCooldown", "Charge Cooldown", "Seconds before a strike can charge the wearer again.");
	}

	private static void brass(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.brass", "Brass");
		t.add("set_bonus.gearexpansion.brass.wind", "Clockwork: walking, hitting, and blocking wind a spring; wound, it gives Haste");
		t.add("set_bonus.gearexpansion.brass.release", "Press %s when wound: Spring Release dashes and knocks back mobs ahead");
		t.add("trait.gearexpansion.momentum", "Momentum: each hit in a combo attacks faster");

		option(t, "brassSetBonus", "Enable Set Bonus", "Whether wearing the full Brass set grants Clockwork and Spring Release.");
		option(t, "brassWindUpSeconds", "Wind-up Time", "Seconds of walking to fully wind the spring.");
		option(t, "brassReleaseDamage", "Release Damage", "Damage Spring Release deals to each mob it hits.");
		option(t, "brassCombo", "Combo Attack Speed", "Whether consecutive hits with brass weapons attack faster.");
	}

	private static void emerald(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.emerald", "Emerald");
		t.add("set_bonus.gearexpansion.emerald.hero", "Merchant's Favor: Hero of the Village while worn");
		t.add("trait.gearexpansion.illagers_bane", "Illager's Bane: %s%% more damage to raiders");
		t.add("trait.gearexpansion.prospector", "Prospector: ores sometimes give bonus experience");
		t.add("trait.gearexpansion.raider_knockback", "Blocking a raider knocks it back hard");

		option(t, "emeraldRaiderDamageBonus", "Raider Damage Bonus", "Extra damage emerald weapons deal to illagers and other raiders.");
		option(t, "emeraldBonusExperienceChance", "Bonus Experience Chance", "Chance an emerald pickaxe drops bonus experience from ores.");
		option(t, "emeraldSetBonus", "Enable Set Bonus", "Whether wearing the full Emerald set grants Hero of the Village.");
	}

	private static void amethyst(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.amethyst", "Amethyst");
		t.add("set_bonus.gearexpansion.amethyst.shell", "Shatterguard: a crystal shell absorbs one hit, then regrows over %ss");
		t.add("trait.gearexpansion.shatter", "Shatter: critical hits burst crystal over nearby mobs");
		t.add("trait.gearexpansion.resonance", "Resonance: mined ores chime louder when more are nearby");
		t.add("trait.gearexpansion.perfect_block", "Perfect Block: blocking right as you raise it knocks attackers back");

		option(t, "amethystShatterDamage", "Shatter Damage", "Damage a critical hit with an amethyst sword deals to nearby mobs.");
		option(t, "amethystSetBonus", "Enable Set Bonus", "Whether wearing the full Amethyst set grants Shatterguard.");
		option(t, "amethystShellRegrowSeconds", "Shell Regrow Time", "Seconds for the crystal shell to regrow.");
	}

	private static void infernium(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.infernium", "Infernium");
		t.add("set_bonus.gearexpansion.infernium.lava", "Heat Core: walk and swim in lava for %ss before the heat gauge fills");
		t.add("set_bonus.gearexpansion.infernium.retaliate", "Melee attackers catch fire");
		t.add("set_bonus.gearexpansion.infernium.eruption", "Press %s: Eruption sets nearby mobs alight (%ss cooldown)");
		t.add("trait.gearexpansion.searing", "Searing: sets targets alight; +%s damage to burning targets");
		t.add("trait.gearexpansion.auto_smelt", "Smelts what it mines");
		t.add("trait.gearexpansion.burning_shield", "Blocking a melee attack sets the attacker alight");
		t.add("trait.gearexpansion.fire_ward", "Fire Ward: %s%% less fire damage per piece");

		option(t, "infernumBurningDamageBonus", "Burning Damage Bonus", "Extra damage infernium weapons deal to burning targets.");
		option(t, "inferniumSetBonus", "Enable Set Bonus", "Whether wearing the full Infernium set grants Heat Core and Eruption.");
		option(t, "inferniumLavaSeconds", "Lava Time", "Seconds the wearer can stay in lava before the heat gauge fills.");
		option(t, "inferniumEruptionCooldown", "Eruption Cooldown", "Seconds before Eruption can be used again.");
	}

	private static void advancements(TranslationBuilder t) {
		advancement(t, "root", "Gear Expansion", "Find or make a new metal, crystal, or alloy");
		advancement(t, "alloy_forge", "Two Metals Are Better Than One", "Build an Alloy Forge");
		advancement(t, "zinc", "Galvanized", "Smelt a zinc ingot");
		advancement(t, "verdigris", "Patina", "Craft a Verdigris Plate");
		advancement(t, "rose_gold", "Rose-Tinted", "Alloy gold and copper into rose gold");
		advancement(t, "aluminum", "Lighter Than Air", "Smelt aluminum from bauxite");
		advancement(t, "brass", "Wound Up", "Alloy copper and zinc into brass");
		advancement(t, "emerald", "Merchant's Arsenal", "Craft any emerald gear");
		advancement(t, "amethyst", "Good Vibrations", "Craft a Resonant Crystal");
		advancement(t, "titanium", "Unbreakable", "Smelt a titanium ingot");
		advancement(t, "infernium_template", "Hot Off the Press", "Find an Infernium Upgrade template in the Nether");
		advancement(t, "infernium", "Forged in Fire", "Upgrade titanium gear to infernium");
	}

	private static void advancement(TranslationBuilder t, String name, String title, String description) {
		t.add("advancements.gearexpansion." + name + ".title", title);
		t.add("advancements.gearexpansion." + name + ".description", description);
	}

	private static void abilities(TranslationBuilder t) {
		t.add("key.gearexpansion.set_ability", "Set Ability");
		t.add("key.category.gearexpansion.main", "Gear Expansion");
		t.add("ability.gearexpansion.none", "No set ability: wear a full Brass or Infernium set");
		t.add("ability.gearexpansion.cooldown", "Ready in %ss");
		t.add("ability.gearexpansion.brass.not_wound", "The spring isn't fully wound yet");
		t.add("hud.gearexpansion.spring", "Spring");
		t.add("hud.gearexpansion.heat", "Heat");
		t.add("hud.gearexpansion.shell", "Shell");
		t.add("hud.gearexpansion.cooldown", "Ability");
	}

	private static void zinc(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.zinc", "Zinc");
		t.add("set_bonus.gearexpansion.zinc.effects", "Galvanized: Poison, Hunger, and Nausea last %s%% shorter");

		option(t, "zincCorrosionProof", "Corrosion-proof Gear", "Whether zinc tools, armor, and shields lose no durability while their user is in water.");
		option(t, "zincSetBonus", "Enable Set Bonus", "Whether wearing the full Zinc set grants Galvanized.");
		option(t, "zincEffectReduction", "Effect Reduction", "How much shorter Poison, Hunger, and Nausea last while the full set is worn.");
	}

	private static void roseGold(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.rose_gold", "Rose Gold");
		t.add("set_bonus.gearexpansion.rose_gold.experience", "Lucky Charm: %s%% more experience");
		t.add("set_bonus.gearexpansion.rose_gold.enchanting", "Better enchanting table offers");

		option(t, "roseGoldSetBonus", "Enable Set Bonus", "Whether wearing the full Rose Gold set grants Lucky Charm.");
		option(t, "roseGoldExperienceBonus", "Experience Bonus", "How much more experience orbs give the wearer.");
		option(t, "roseGoldEnchantingBookshelves", "Extra Bookshelves", "How many extra bookshelves an enchanting table counts for the wearer.");
	}

	private static void aluminum(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.aluminum", "Aluminum");
		t.add("set_bonus.gearexpansion.aluminum.fall", "Featherweight: %s%% less fall damage");
		t.add("set_bonus.gearexpansion.aluminum.jump", "%s%% stronger jumps");
		t.add("set_bonus.gearexpansion.aluminum.water", "%s%% faster in water");

		option(t, "aluminumSetBonus", "Enable Set Bonus", "Whether wearing the full Aluminum set grants Featherweight.");
		option(t, "aluminumFallDamageReduction", "Fall Damage Reduction", "How much less fall damage the wearer takes.");
		option(t, "aluminumJumpBoost", "Jump Boost", "How much stronger the wearer's jumps are. 10% or more clears fences.");
		option(t, "aluminumWaterSpeed", "Water Speed", "Extra movement speed in water. Depth Strider I is 33%.");
	}

	private static void titanium(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.titanium", "Titanium");
		t.add("set_bonus.gearexpansion.titanium.durability", "Unbreakable Will: gear loses %s%% less durability");
		t.add("set_bonus.gearexpansion.titanium.resistance", "Below %s%% health, gain Resistance I for %ss (%ss cooldown)");
		t.add("set_bonus.gearexpansion.titanium.triggered", "Unbreakable Will");

		option(t, "titaniumSetBonus", "Enable Set Bonus", "Whether wearing the full Titanium set grants Unbreakable Will.");
		option(t, "titaniumDurabilitySaving", "Durability Saving", "How much less durability gear loses while the full set is worn.");
		option(t, "titaniumResistanceThreshold", "Resistance Health Threshold", "Percent of max health below which Resistance is granted.");
		option(t, "titaniumResistanceSeconds", "Resistance Duration", "Seconds of Resistance I granted.");
		option(t, "titaniumResistanceCooldown", "Resistance Cooldown", "Seconds before Resistance can be granted again.");
	}

	private static void option(TranslationBuilder t, String field, String name, String description) {
		t.add(CONFIG + "." + field, name);
		t.add(CONFIG + "." + field + ".desc", description);
	}

	/** "rose_gold" becomes "Rose Gold". */
	private static String title(String name) {
		StringBuilder title = new StringBuilder();
		for (String word : name.split("_")) {
			title.append(title.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return title.toString();
	}
}
