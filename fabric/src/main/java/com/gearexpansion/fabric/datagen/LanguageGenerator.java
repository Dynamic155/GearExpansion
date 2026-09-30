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

		for (MaterialSet set : ModMaterials.ALL) {
			String name = title(set.name);
			t.add(set.ore.get(), name + " Ore");
			t.add(set.deepslateOre.get(), "Deepslate " + name + " Ore");
			t.add(set.storageBlock.get(), "Block of " + name);
			t.add(set.rawStorageBlock.get(), "Block of Raw " + name);
			t.add(set.rawItem.get(), "Raw " + name);
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

		titanium(t);

		t.add(CONFIG + ".title", "Gear Expansion");
		t.add(CONFIG + ".category.titanium", "Titanium");
		t.add(CONFIG + ".category.titanium.group.set_bonus", "Set Bonus: Unbreakable Will");
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
