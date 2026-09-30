package com.gearexpansion.material;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.item.GearArmorItem;
import com.gearexpansion.item.GearShieldItem;
import com.gearexpansion.registry.ModBlocks;
import com.gearexpansion.registry.ModItems;

/**
 * Everything one gear material adds: ores, raw and refined items, storage blocks,
 * six tools, four armor pieces, and a shield. Each material is defined once with a
 * {@link Builder}; registration, data generation, and world generation all read from it.
 */
public final class MaterialSet {
	public final String name;
	public final ToolMaterial toolMaterial;
	public final ArmorMaterial armorMaterial;
	public final TagKey<Block> requiredToolTag;
	public final OreGeneration oreGeneration;
	public final TagKey<Item> repairMaterials;

	public final RegistrySupplier<Block> ore;
	public final RegistrySupplier<Block> deepslateOre;
	public final RegistrySupplier<Block> storageBlock;
	public final RegistrySupplier<Block> rawStorageBlock;

	public final RegistrySupplier<Item> rawItem;
	public final RegistrySupplier<Item> ingot;
	public final RegistrySupplier<Item> nugget;

	public final RegistrySupplier<Item> sword;
	public final RegistrySupplier<Item> pickaxe;
	public final RegistrySupplier<Item> axe;
	public final RegistrySupplier<Item> shovel;
	public final RegistrySupplier<Item> hoe;
	public final RegistrySupplier<Item> spear;

	public final RegistrySupplier<Item> helmet;
	public final RegistrySupplier<Item> chestplate;
	public final RegistrySupplier<Item> leggings;
	public final RegistrySupplier<Item> boots;

	public final RegistrySupplier<Item> shield;

	private MaterialSet(Builder b) {
		this.name = b.name;
		this.repairMaterials = b.repairMaterials;
		this.toolMaterial = b.toolMaterial;
		this.armorMaterial = b.armorMaterial;
		this.requiredToolTag = b.requiredToolTag;
		this.oreGeneration = b.oreGeneration;

		this.ore = ModBlocks.register(name + "_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p), BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F));
		this.deepslateOre = ModBlocks.register("deepslate_" + name + "_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p), BlockBehaviour.Properties.of()
			.mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE));
		this.storageBlock = ModBlocks.register(name + "_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(b.metalColor).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON));
		this.rawStorageBlock = ModBlocks.register("raw_" + name + "_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(b.rawColor).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F));

		blockItem(ore);
		blockItem(deepslateOre);
		blockItem(rawStorageBlock);
		blockItem(storageBlock);

		this.rawItem = ModItems.register("raw_" + name, Item::new, p -> p);
		this.ingot = ModItems.register(name + "_ingot", Item::new, p -> p);
		this.nugget = ModItems.register(name + "_nugget", Item::new, p -> p);

		// Attack damage and speed baselines match vanilla diamond tools; the tool material adds its damage bonus on top.
		ToolMaterial tools = b.toolMaterial;
		this.sword = ModItems.register(name + "_sword", Item::new, p -> p.sword(tools, 3.0F, -2.4F));
		this.shovel = ModItems.register(name + "_shovel", Item::new, p -> p.shovel(tools, 1.5F, -3.0F));
		this.pickaxe = ModItems.register(name + "_pickaxe", Item::new, p -> p.pickaxe(tools, 1.0F, -2.8F));
		this.axe = ModItems.register(name + "_axe", Item::new, p -> p.axe(tools, 5.0F, -3.0F));
		this.hoe = ModItems.register(name + "_hoe", Item::new, p -> p.hoe(tools, -3.0F, 0.0F));
		// Spear timings match the vanilla diamond spear; damage scales with the tool material.
		this.spear = ModItems.register(name + "_spear", Item::new,
			p -> p.spear(tools, 1.05F, 1.075F, 0.5F, 3.0F, 10.0F, 6.5F, 5.1F, 10.0F, 4.6F));

		this.helmet = armor(b, ArmorType.HELMET);
		this.chestplate = armor(b, ArmorType.CHESTPLATE);
		this.leggings = armor(b, ArmorType.LEGGINGS);
		this.boots = armor(b, ArmorType.BOOTS);

		ShieldStats shieldStats = b.shieldStats;
		this.shield = ModItems.register(name + "_shield", b.shieldFactory, p -> p
			.durability(shieldStats.durability())
			.repairable(b.repairMaterials)
			.equippableUnswappable(EquipmentSlot.OFFHAND)
			.delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
				0.25F,
				shieldStats.disableCooldownScale(),
				List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
				new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
				Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
				Optional.of(SoundEvents.SHIELD_BLOCK),
				Optional.of(SoundEvents.SHIELD_BREAK)
			))
			.component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
			.enchantable(b.toolMaterial.enchantmentValue()));
	}

	private RegistrySupplier<Item> armor(Builder b, ArmorType type) {
		String piece = switch (type) {
			case HELMET -> "helmet";
			case CHESTPLATE -> "chestplate";
			case LEGGINGS -> "leggings";
			case BOOTS -> "boots";
			case BODY -> throw new IllegalArgumentException("Body armor is not part of a material set");
		};
		return ModItems.register(name + "_" + piece, b.armorFactory, p -> p.humanoidArmor(b.armorMaterial, type));
	}

	private static void blockItem(RegistrySupplier<Block> block) {
		ModItems.register(block.getId().getPath(), p -> new BlockItem(block.get(), p), Item.Properties::useBlockDescriptionPrefix);
	}

	public List<RegistrySupplier<Item>> tools() {
		return List.of(sword, pickaxe, axe, shovel, hoe, spear);
	}

	public List<RegistrySupplier<Item>> armorPieces() {
		return List.of(helmet, chestplate, leggings, boots);
	}

	public List<RegistrySupplier<Block>> blocks() {
		return List.of(ore, deepslateOre, rawStorageBlock, storageBlock);
	}

	/** The item for each armor slot, used to check whether someone is wearing the full set. */
	public Map<EquipmentSlot, RegistrySupplier<Item>> armorBySlot() {
		return Map.of(EquipmentSlot.HEAD, helmet, EquipmentSlot.CHEST, chestplate, EquipmentSlot.LEGS, leggings, EquipmentSlot.FEET, boots);
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	/** How much a shield can take and how long axes disable it (vanilla shield: 336 durability, scale 1.0). */
	public record ShieldStats(int durability, float disableCooldownScale) {
	}

	/**
	 * Where the ore generates. Veins use vanilla's ore feature; {@code airExposureDiscard}
	 * is the chance an ore block touching air is skipped, which makes ores mostly buried.
	 */
	public record OreGeneration(int veinSize, int veinsPerChunk, int minY, int maxY, float airExposureDiscard) {
	}

	public static final class Builder {
		private final String name;
		private final TagKey<Item> repairMaterials;
		private ToolMaterial toolMaterial;
		private ArmorMaterial armorMaterial;
		private ShieldStats shieldStats = new ShieldStats(336, 1.0F);
		private TagKey<Block> requiredToolTag = BlockTags.NEEDS_IRON_TOOL;
		private OreGeneration oreGeneration;
		private MapColor metalColor = MapColor.METAL;
		private MapColor rawColor = MapColor.RAW_IRON;
		private Function<Item.Properties, Item> armorFactory;
		private Function<Item.Properties, Item> shieldFactory;

		private Builder(String name) {
			this.name = name;
			this.repairMaterials = TagKey.create(Registries.ITEM, GearExpansion.id(name + "_repair_materials"));
			this.armorFactory = properties -> new GearArmorItem(properties, name);
			this.shieldFactory = properties -> new GearShieldItem(properties, name);
		}

		/** Tool stats. {@code incorrectBlocksForDrops} sets the mining tier, e.g. {@code BlockTags.INCORRECT_FOR_DIAMOND_TOOL}. */
		public Builder tools(TagKey<Block> incorrectBlocksForDrops, int durability, float miningSpeed, float attackDamageBonus, int enchantability) {
			this.toolMaterial = new ToolMaterial(incorrectBlocksForDrops, durability, miningSpeed, attackDamageBonus, enchantability, repairMaterials);
			return this;
		}

		/** Armor stats. Durability is multiplied per piece like vanilla (diamond is 33, netherite 37). */
		public Builder armor(int durabilityMultiplier, int helmet, int chestplate, int leggings, int boots,
				int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance) {
			ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, GearExpansion.id(name));
			Map<ArmorType, Integer> defense = Map.of(
				ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chestplate, ArmorType.LEGGINGS, leggings, ArmorType.BOOTS, boots, ArmorType.BODY, chestplate);
			this.armorMaterial = new ArmorMaterial(durabilityMultiplier, defense, enchantability, equipSound, toughness, knockbackResistance, repairMaterials, asset);
			return this;
		}

		public Builder shield(int durability, float disableCooldownScale) {
			this.shieldStats = new ShieldStats(durability, disableCooldownScale);
			return this;
		}

		/** The pickaxe tier needed to mine this material's ores and blocks, e.g. {@code BlockTags.NEEDS_DIAMOND_TOOL}. */
		public Builder requiresTool(TagKey<Block> tag) {
			this.requiredToolTag = tag;
			return this;
		}

		public Builder ore(int veinSize, int veinsPerChunk, int minY, int maxY, float airExposureDiscard) {
			this.oreGeneration = new OreGeneration(veinSize, veinsPerChunk, minY, maxY, airExposureDiscard);
			return this;
		}

		public Builder colors(MapColor metal, MapColor raw) {
			this.metalColor = metal;
			this.rawColor = raw;
			return this;
		}

		/** Item class for armor pieces. Defaults to {@link GearArmorItem}, which draws the material's 3D model. */
		public Builder armorItem(Function<Item.Properties, Item> factory) {
			this.armorFactory = factory;
			return this;
		}

		/** Item class for the shield. Defaults to {@link GearShieldItem}. */
		public Builder shieldItem(Function<Item.Properties, Item> factory) {
			this.shieldFactory = factory;
			return this;
		}

		public MaterialSet build() {
			if (toolMaterial == null || armorMaterial == null || oreGeneration == null) {
				throw new IllegalStateException("Material " + name + " needs tools, armor, and ore settings");
			}
			MaterialSet set = new MaterialSet(this);
			ModMaterials.ALL.add(set);
			return set;
		}
	}
}
