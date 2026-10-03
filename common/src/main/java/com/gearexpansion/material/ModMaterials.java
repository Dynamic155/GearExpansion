package com.gearexpansion.material;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.MapColor;

import com.gearexpansion.material.behavior.AmethystBehavior;
import com.gearexpansion.material.behavior.CobaltBehavior;
import com.gearexpansion.material.behavior.BrassBehavior;
import com.gearexpansion.material.behavior.EmeraldBehavior;
import com.gearexpansion.material.behavior.InferniumBehavior;
import com.gearexpansion.material.behavior.SilverBehavior;
import com.gearexpansion.material.behavior.SteelBehavior;
import com.gearexpansion.material.behavior.TungstenBehavior;
import com.gearexpansion.material.behavior.VerdigrisBehavior;
import com.gearexpansion.registry.ModSounds;

/** All gear materials. Stats and ore placement follow ideas.md. */
public final class ModMaterials {
	public static final List<MaterialSet> ALL = new ArrayList<>();

	/**
	 * Zinc: an early metal a step above copper. Common, and galvanized: zinc gear doesn't
	 * lose durability in water.
	 */
	public static final MaterialSet ZINC = MaterialSet.builder("zinc")
		.tools(ToolTier.COPPER, 260, 5.0F, 1.0F, 13)
		.armor(13, 2, 5, 3, 1, 10, ModSounds.armorEquip("zinc"), 0.0F, 0.0F)
		.shield(400, 1.0F)
		.requiresTool(BlockTags.NEEDS_STONE_TOOL)
		.ore(9, 10, 0, 64, 0.0F)
		.galvanized()
		.colors(MapColor.CLAY, MapColor.STONE)
		.build();

	/**
	 * Verdigris: copper gear that oxidizes as it's used, trading speed for toughness. Made from
	 * Verdigris Plates (copper, honeycomb, and green dye). Waxing with honeycomb locks its stage.
	 */
	public static final MaterialSet VERDIGRIS = MaterialSet.builder("verdigris")
		.craftedMaterial("verdigris_plate")
		.tools(ToolTier.COPPER, 240, 6.5F, 1.0F, 14)
		.armor(14, 2, 5, 4, 1, 12, ModSounds.armorEquip("verdigris"), 0.0F, 0.0F)
		.shield(350, 1.0F)
		.behavior(new VerdigrisBehavior())
		.build();

	/**
	 * Rose Gold: an alloy of gold and copper made in the Alloy Forge. Gold's speed and
	 * enchantability with far better durability. Piglins treat its armor as gold.
	 */
	public static final MaterialSet ROSE_GOLD = MaterialSet.builder("rose_gold")
		.alloy()
		.tools(ToolTier.COPPER, 180, 11.0F, 0.5F, 22)
		.armor(12, 2, 5, 3, 1, 25, ModSounds.armorEquip("rose_gold"), 0.0F, 0.0F)
		.shield(336, 1.0F)
		.requiresTool(BlockTags.NEEDS_STONE_TOOL)
		.piglinSafe()
		.colors(MapColor.COLOR_PINK, MapColor.COLOR_PINK)
		.build();

	/**
	 * Aluminum: light and fast. Mines like iron with faster attacks but low durability;
	 * armor is lighter than iron and makes you quicker. Smelted from Bauxite, which is
	 * richest near the surface of badlands and savannas.
	 */
	public static final MaterialSet ALUMINUM = MaterialSet.builder("aluminum")
		.oreName("bauxite")
		.tools(ToolTier.IRON, 180, 9.0F, 1.5F, 16)
		.attackSpeedBonus(0.3F)
		.armor(12, 2, 5, 4, 1, 14, ModSounds.armorEquip("aluminum"), 0.0F, 0.0F)
		.armorBonus("speed", Attributes.MOVEMENT_SPEED, 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
		.shield(300, 1.0F)
		.blockingSpeed(1.0F)
		.requiresTool(BlockTags.NEEDS_STONE_TOOL)
		.ore(8, 5, 32, 96, 0.3F)
		.ore("badlands", BiomeTags.IS_BADLANDS, 12, 10, 48, 128, 0.0F)
		.ore("savanna", BiomeTags.IS_SAVANNA, 10, 6, 48, 128, 0.0F)
		.colors(MapColor.QUARTZ, MapColor.TERRACOTTA_ORANGE)
		.build();

	/**
	 * Brass: an alloy of copper and zinc made in the Alloy Forge. A dependable iron-tier set;
	 * its Clockwork set bonus arrives with set abilities.
	 */
	public static final MaterialSet BRASS = MaterialSet.builder("brass")
		.alloy()
		.tools(ToolTier.IRON, 300, 6.5F, 2.0F, 12)
		.armor(16, 2, 6, 5, 2, 11, ModSounds.armorEquip("brass"), 0.5F, 0.0F)
		.shield(450, 1.0F)
		.requiresTool(BlockTags.NEEDS_STONE_TOOL)
		.behavior(new BrassBehavior())
		.colors(MapColor.GOLD, MapColor.GOLD)
		.build();

	/**
	 * Silver: an iron-tier metal against the undead. Weapons hit undead harder, armor softens
	 * their attacks, and the shield throws them back. Found at middle depths.
	 */
	public static final MaterialSet SILVER = MaterialSet.builder("silver")
		.tools(ToolTier.IRON, 280, 6.5F, 2.0F, 18)
		.armor(15, 2, 6, 5, 2, 18, ModSounds.armorEquip("silver"), 0.0F, 0.0F)
		.shield(350, 1.0F)
		.requiresTool(BlockTags.NEEDS_IRON_TOOL)
		.ore(8, 6, -16, 48, 0.0F)
		.behavior(new SilverBehavior())
		.colors(MapColor.METAL, MapColor.STONE)
		.build();

	/**
	 * Emerald: gear made from emeralds, built for raids. Weapons hit illagers harder, the pickaxe
	 * finds extra experience, and each armor piece adds Luck.
	 */
	public static final MaterialSet EMERALD = MaterialSet.builder("emerald")
		.baseItem(() -> Items.EMERALD)
		.tools(ToolTier.IRON, 500, 7.0F, 2.0F, 18)
		.armor(18, 2, 6, 5, 2, 20, ModSounds.armorEquip("emerald"), 0.0F, 0.0F)
		.armorBonus("luck", Attributes.LUCK, 1.0, AttributeModifier.Operation.ADD_VALUE)
		.shield(400, 1.0F)
		.behavior(new EmeraldBehavior())
		.build();

	/**
	 * Amethyst: fragile but very enchantable crystal gear, made from Resonant Crystals
	 * (amethyst shards tuned with copper).
	 */
	public static final MaterialSet AMETHYST = MaterialSet.builder("amethyst")
		.craftedMaterial("resonant_crystal")
		.tools(ToolTier.IRON, 220, 7.0F, 2.0F, 22)
		.armor(13, 2, 6, 5, 2, 22, ModSounds.armorEquip("amethyst"), 0.0F, 0.0F)
		.shield(300, 1.0F)
		.behavior(new AmethystBehavior())
		.build();

	/**
	 * Steel: iron tempered with coal in the Alloy Forge. No gimmicks, just a sturdy step between
	 * iron and diamond: about three times iron's durability, a little toughness, and a very durable shield.
	 */
	public static final MaterialSet STEEL = MaterialSet.builder("steel")
		.alloy()
		.tools(ToolTier.IRON, 750, 6.5F, 2.5F, 10)
		.armor(25, 2, 6, 6, 2, 9, ModSounds.armorEquip("steel"), 1.0F, 0.0F)
		.shield(1200, 1.0F)
		.behavior(new SteelBehavior())
		.colors(MapColor.COLOR_GRAY, MapColor.COLOR_GRAY)
		.build();

	/**
	 * Titanium: diamond-tier speed with about twice diamond's durability, but hard to enchant.
	 * Found only deep underground in small, mostly buried veins.
	 */
	public static final MaterialSet TITANIUM = MaterialSet.builder("titanium")
		.tools(ToolTier.DIAMOND, 3000, 8.0F, 3.0F, 8)
		.armor(45, 3, 8, 6, 3, 8, ModSounds.armorEquip("titanium"), 2.5F, 0.05F)
		.shield(1000, 0.4F)
		.requiresTool(BlockTags.NEEDS_DIAMOND_TOOL)
		.ore(5, 4, -64, -16, 0.5F)
		.colors(MapColor.COLOR_LIGHT_GRAY, MapColor.TERRACOTTA_LIGHT_GRAY)
		.build();

	/**
	 * Cobalt: a deep blue Nether metal with the fastest tools in the mod and a shield that blocks
	 * the moment it's raised. Found throughout the Nether's netherrack.
	 */
	public static final MaterialSet COBALT = MaterialSet.builder("cobalt")
		.tools(ToolTier.DIAMOND, 1100, 13.0F, 2.5F, 14)
		.armor(25, 2, 6, 5, 2, 14, ModSounds.armorEquip("cobalt"), 1.0F, 0.0F)
		.shield(600, 1.0F)
		.shieldRaiseTime(0.0F)
		.requiresTool(BlockTags.NEEDS_IRON_TOOL)
		.netherOre(6, 8, 0, 128, 0.0F)
		.behavior(new CobaltBehavior())
		.colors(MapColor.COLOR_BLUE, MapColor.NETHER)
		.build();

	/**
	 * Tungsten: the tank. Very rare and very deep. Slow, hard-hitting weapons that knock targets
	 * back, armor with the most knockback resistance in the mod at the cost of some speed, and a
	 * heavy shield that is slow to raise.
	 */
	public static final MaterialSet TUNGSTEN = MaterialSet.builder("tungsten")
		.tools(ToolTier.DIAMOND, 2400, 5.0F, 4.0F, 8)
		.attackSpeedBonus(-0.2F)
		.armor(40, 3, 8, 6, 3, 8, ModSounds.armorEquip("tungsten"), 3.0F, 0.15F)
		.armorBonus("weight", Attributes.MOVEMENT_SPEED, -0.04, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
		.shield(1500, 0.3F)
		.shieldRaiseTime(0.5F)
		.requiresTool(BlockTags.NEEDS_DIAMOND_TOOL)
		.ore(4, 3, -64, -48, 0.6F)
		.behavior(new TungstenBehavior())
		.colors(MapColor.COLOR_BLACK, MapColor.DEEPSLATE)
		.build();

	/**
	 * Infernium: the Nether endgame metal. Upgraded from Titanium gear at a smithing table with
	 * an Infernium Upgrade template from Bastion and Fortress chests. Fireproof, and it burns.
	 */
	public static final MaterialSet INFERNIUM = MaterialSet.builder("infernium")
		.tools(ToolTier.NETHERITE, 2200, 9.0F, 4.0F, 15)
		.armor(40, 3, 8, 6, 3, 15, ModSounds.armorEquip("infernium"), 3.0F, 0.1F)
		.shield(900, 0.8F)
		.requiresTool(BlockTags.NEEDS_DIAMOND_TOOL)
		.netherOre(4, 4, 10, 40, 0.0F)
		.blastFurnaceOnly()
		.upgradedFrom(() -> TITANIUM)
		.fireResistant()
		.glowing()
		.behavior(new InferniumBehavior())
		.colors(MapColor.NETHER, MapColor.COLOR_RED)
		.build();

	private ModMaterials() {
	}

	/** Loads this class so every material registers its blocks and items. */
	public static void init() {
	}

	/** The material an item of gear (tool, armor piece, or shield) is made of. */
	public static Optional<MaterialSet> ofGear(ItemStack stack) {
		return ALL.stream().filter(set -> set.isGear(stack)).findFirst();
	}
}
