package com.gearexpansion.fabric.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;

/**
 * Item definitions and display transforms for GeckoLib shields.
 *
 * <p>Each shield switches between an idle and a blocking model, like the vanilla shield.
 * Both are GeckoLib "special" models. The {@code transformation} cancels GeckoLib's built-in
 * offset so vanilla's shield display transforms (copied below) line up exactly.
 */
final class GeckoLibItemModelGenerator implements DataProvider {
	private final PackOutput.PathProvider itemDefinitions;
	private final PackOutput.PathProvider models;

	GeckoLibItemModelGenerator(FabricPackOutput output) {
		this.itemDefinitions = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
		this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
	}

	@Override
	public CompletableFuture<?> run(CachedOutput output) {
		List<CompletableFuture<?>> writes = new ArrayList<>();
		for (MaterialSet set : ModMaterials.ALL) {
			String shield = set.name + "_shield";
			writes.add(DataProvider.saveStable(output, shieldDefinition(shield), itemDefinitions.json(GearExpansion.id(shield))));
			writes.add(DataProvider.saveStable(output, shieldModel(shield, false), models.json(GearExpansion.id("item/" + shield))));
			writes.add(DataProvider.saveStable(output, shieldModel(shield, true), models.json(GearExpansion.id("item/" + shield + "_blocking"))));
		}
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	private static JsonObject shieldDefinition(String shield) {
		JsonObject condition = new JsonObject();
		condition.addProperty("type", "minecraft:condition");
		condition.addProperty("property", "minecraft:using_item");
		condition.add("on_false", geckoLibModel("gearexpansion:item/" + shield));
		condition.add("on_true", geckoLibModel("gearexpansion:item/" + shield + "_blocking"));
		JsonObject root = new JsonObject();
		root.add("model", condition);
		return root;
	}

	private static JsonObject geckoLibModel(String base) {
		JsonObject special = new JsonObject();
		special.addProperty("type", "geckolib:geckolib");

		JsonObject transformation = new JsonObject();
		transformation.add("translation", vector(-0.5, -0.51, -0.5));
		transformation.add("left_rotation", vector(0, 0, 0, 1));
		transformation.add("scale", vector(1, 1, 1));
		transformation.add("right_rotation", vector(0, 0, 0, 1));

		JsonObject model = new JsonObject();
		model.addProperty("type", "minecraft:special");
		model.addProperty("base", base);
		model.add("model", special);
		model.add("transformation", transformation);
		return model;
	}

	private static JsonObject shieldModel(String shield, boolean blocking) {
		JsonObject display = new JsonObject();
		if (blocking) {
			display.add("thirdperson_righthand", transform(vector(45, 155, 0), vector(-3.49, 11, -2), vector(1, 1, 1)));
			display.add("thirdperson_lefthand", transform(vector(45, 155, 0), vector(11.51, 7, 2.5), vector(1, 1, 1)));
			display.add("firstperson_righthand", transform(vector(0, 180, -5), vector(-15, 3.25, -11), vector(1.25, 1.25, 1.25)));
			display.add("firstperson_lefthand", transform(vector(0, 180, -5), vector(5, 5, -11), vector(1.25, 1.25, 1.25)));
		} else {
			display.add("thirdperson_righthand", transform(vector(0, 90, 0), vector(10, 6, -4), vector(1, 1, 1)));
			display.add("thirdperson_lefthand", transform(vector(0, 90, 0), vector(10, 6, 12), vector(1, 1, 1)));
			display.add("firstperson_righthand", transform(vector(0, 180, 5), vector(-10, 1.75, -10), vector(1.25, 1.25, 1.25)));
			display.add("firstperson_lefthand", transform(vector(0, 180, 5), vector(10, 0, -10), vector(1.25, 1.25, 1.25)));
		}
		display.add("gui", transform(vector(15, -25, -5), vector(2, 3, 0), vector(0.65, 0.65, 0.65)));
		display.add("fixed", transform(vector(0, 180, 0), vector(-4.5, 4.5, -5), vector(0.55, 0.55, 0.55)));
		display.add("on_shelf", transform(vector(0, 0, 0), vector(11, 18.5, 8.7), vector(1.4, 1.4, 1.4)));
		display.add("ground", transform(vector(0, 0, 0), vector(2, 4, 2), vector(0.25, 0.25, 0.25)));

		JsonObject textures = new JsonObject();
		textures.addProperty("particle", "gearexpansion:item/" + shield);

		JsonObject model = new JsonObject();
		model.addProperty("gui_light", "front");
		model.add("textures", textures);
		model.add("display", display);
		return model;
	}

	private static JsonObject transform(JsonArray rotation, JsonArray translation, JsonArray scale) {
		JsonObject transform = new JsonObject();
		transform.add("rotation", rotation);
		transform.add("translation", translation);
		transform.add("scale", scale);
		return transform;
	}

	private static JsonArray vector(double... values) {
		JsonArray array = new JsonArray();
		for (double value : values) {
			array.add(value);
		}
		return array;
	}

	@Override
	public String getName() {
		return "Gear Expansion GeckoLib Item Models";
	}
}
