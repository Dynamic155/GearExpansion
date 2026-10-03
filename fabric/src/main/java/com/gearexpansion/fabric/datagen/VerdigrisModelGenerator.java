package com.gearexpansion.fabric.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.material.behavior.VerdigrisBehavior;

/**
 * Models for Verdigris tools and armor icons, one per oxidation stage. Each item definition picks
 * the model from the item's {@code gearexpansion:oxidation} component, like copper blocks have a
 * texture per stage. Fresh items have no component and use the plain texture.
 */
final class VerdigrisModelGenerator implements DataProvider {
	private final PackOutput.PathProvider itemDefinitions;
	private final PackOutput.PathProvider models;

	VerdigrisModelGenerator(FabricPackOutput output) {
		this.itemDefinitions = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
		this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
	}

	/** Verdigris gear that gets per-stage models here instead of in {@link ModelGenerator}. */
	static List<RegistrySupplier<Item>> items() {
		MaterialSet set = ModMaterials.VERDIGRIS;
		List<RegistrySupplier<Item>> items = new ArrayList<>(set.tools());
		items.addAll(set.armorPieces());
		return items;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		List<CompletableFuture<?>> writes = new ArrayList<>();
		MaterialSet set = ModMaterials.VERDIGRIS;
		for (RegistrySupplier<Item> item : items()) {
			String name = item.getId().getPath();
			boolean spear = item == set.spear;
			boolean tool = set.tools().contains(item);

			JsonArray cases = new JsonArray();
			for (int stage = 0; stage < VerdigrisBehavior.STAGES.length; stage++) {
				String suffix = stage == 0 ? "" : "_" + VerdigrisBehavior.STAGES[stage];
				if (spear) {
					writes.add(save(output, name + suffix, flat("minecraft:item/generated", name + suffix)));
					writes.add(save(output, name + "_in_hand" + suffix, flat("minecraft:item/spear_in_hand", name + "_in_hand" + suffix)));
				} else {
					String parent = item == set.scythe ? ModelGenerator.SCYTHE_PARENT : tool ? "minecraft:item/handheld" : "minecraft:item/generated";
					writes.add(save(output, name + suffix, flat(parent, name + suffix)));
				}
				if (stage > 0) {
					JsonObject entry = new JsonObject();
					entry.addProperty("when", stage);
					entry.add("model", spear ? spearModel(name, suffix) : model(name + suffix));
					cases.add(entry);
				}
			}

			JsonObject select = new JsonObject();
			select.addProperty("type", "minecraft:select");
			select.addProperty("property", "minecraft:component");
			select.addProperty("component", "gearexpansion:oxidation");
			select.add("cases", cases);
			select.add("fallback", spear ? spearModel(name, "") : model(name));
			JsonObject definition = new JsonObject();
			definition.add("model", select);
			if (spear) {
				// Same swing as vanilla spears.
				definition.addProperty("swap_animation_scale", 1.95);
			}
			writes.add(DataProvider.saveStable(output, definition, itemDefinitions.json(GearExpansion.id(name))));
		}
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	private CompletableFuture<?> save(CachedOutput output, String model, JsonObject json) {
		return DataProvider.saveStable(output, json, models.json(GearExpansion.id("item/" + model)));
	}

	private static JsonObject flat(String parent, String texture) {
		JsonObject textures = new JsonObject();
		textures.addProperty("layer0", "gearexpansion:item/" + texture);
		JsonObject json = new JsonObject();
		json.addProperty("parent", parent);
		json.add("textures", textures);
		return json;
	}

	private static JsonObject model(String name) {
		JsonObject json = new JsonObject();
		json.addProperty("type", "minecraft:model");
		json.addProperty("model", "gearexpansion:item/" + name);
		return json;
	}

	/** Like vanilla spears: the small icon in menus and on the ground, the long model when held. */
	private static JsonObject spearModel(String name, String suffix) {
		JsonObject iconCase = new JsonObject();
		JsonArray contexts = new JsonArray();
		for (String context : List.of("gui", "ground", "fixed", "on_shelf")) {
			contexts.add(context);
		}
		iconCase.add("when", contexts);
		iconCase.add("model", model(name + suffix));
		JsonArray cases = new JsonArray();
		cases.add(iconCase);

		JsonObject select = new JsonObject();
		select.addProperty("type", "minecraft:select");
		select.addProperty("property", "minecraft:display_context");
		select.add("cases", cases);
		select.add("fallback", model(name + "_in_hand" + suffix));
		return select;
	}

	@Override
	public String getName() {
		return "Gear Expansion Verdigris Models";
	}
}
