package com.gearexpansion.fabric.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/** English (en_us) names and text. Item names are built from each material's name. */
final class LanguageGenerator extends FabricLanguageProvider {
	private static final String CONFIG = "yacl3.config.gearexpansion:config";

	LanguageGenerator(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder t) {
		t.add("itemGroup.gearexpansion", "Gear Expansion");
		t.add("tooltip.gearexpansion.set", "%s Set (%s/%s)");
		t.add("trait.gearexpansion.corrosion_proof", "Corrosion-proof: no durability loss in water");
		t.add("trait.gearexpansion.lightweight_shield", "Lightweight: no slowdown while blocking");

		for (MaterialSet set : ModMaterials.ALL) {
			String name = title(set.name);
			String ore = title(set.oreName);
			t.add(set.ore.get(), ore + " Ore");
			t.add(set.deepslateOre.get(), "Deepslate " + ore + " Ore");
			t.add(set.storageBlock.get(), "Block of " + name);
			t.add(set.rawStorageBlock.get(), "Block of Raw " + ore);
			t.add(set.rawItem.get(), "Raw " + ore);
			t.add(set.ingot.get(), name + " Ingot");
			t.add(set.nugget.get(), name + " Nugget");
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
		aluminum(t);
		titanium(t);

		t.add(CONFIG + ".title", "Gear Expansion");
		t.add(CONFIG + ".category.zinc", "Zinc");
		t.add(CONFIG + ".category.zinc.group.gear", "Gear");
		t.add(CONFIG + ".category.zinc.group.set_bonus", "Set Bonus: Galvanized");
		t.add(CONFIG + ".category.aluminum", "Aluminum");
		t.add(CONFIG + ".category.aluminum.group.set_bonus", "Set Bonus: Featherweight");
		t.add(CONFIG + ".category.titanium", "Titanium");
		t.add(CONFIG + ".category.titanium.group.set_bonus", "Set Bonus: Unbreakable Will");
	}

	private static void zinc(TranslationBuilder t) {
		t.add("set_bonus.gearexpansion.zinc", "Zinc");
		t.add("set_bonus.gearexpansion.zinc.effects", "Galvanized: Poison, Hunger, and Nausea last %s%% shorter");

		option(t, "zincCorrosionProof", "Corrosion-proof Gear", "Whether zinc tools, armor, and shields lose no durability while their user is in water.");
		option(t, "zincSetBonus", "Enable Set Bonus", "Whether wearing the full Zinc set grants Galvanized.");
		option(t, "zincEffectReduction", "Effect Reduction", "How much shorter Poison, Hunger, and Nausea last while the full set is worn.");
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

	private static String title(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}
}
