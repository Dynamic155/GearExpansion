package com.gearexpansion.material;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.item.GearArmorItem;
import com.gearexpansion.item.GearShieldItem;
import com.gearexpansion.item.GearToolItem;
import com.gearexpansion.registry.ModBlocks;
import com.gearexpansion.registry.ModItems;

/**
 * Everything one gear material adds: its material item (usually an ingot, plus a nugget and a
 * storage block), ores if it's mined, six tools, four armor pieces, and a shield. Each material
 * is defined once with a {@link Builder}; registration, data generation, and world generation all
 * read from it.
 */
public final class MaterialSet {
	/** Vanilla shields slow the player to 20% speed while blocking. */
	public static final float VANILLA_BLOCKING_SPEED = 0.2F;
	public static final float VANILLA_SHIELD_RAISE_SECONDS = 0.25F;

	/** Where a material's ore generates. */
	public enum OreKind {
		/** Not mined (alloys and materials made from other items). */
		NONE,
		/** Stone and deepslate ores in the Overworld. */
		OVERWORLD,
		/** A netherrack ore in the Nether. */
		NETHER,
		/** An ore inside packed ice and blue ice. */
		ICE
	}

	public final String name;
	/** Name used for the ore, raw item, and raw block. Usually the material name; Aluminum uses "bauxite". */
	public final String oreName;
	public final OreKind oreKind;
	public final ToolMaterial toolMaterial;
	public final ArmorMaterial armorMaterial;
	public final TagKey<Block> requiredToolTag;
	public final List<OreGeneration> oreGeneration;
	public final TagKey<Item> repairMaterials;
	public final GearBehavior behavior;
	/** Galvanized gear doesn't lose durability while its user is in water (see {@code GearDurability}). */
	public final boolean galvanized;
	public final float blockingSpeed;
	public final float shieldRaiseSeconds;
	/** Raw ore and ore only smelt in a blast furnace, not a regular furnace. */
	public final boolean blastFurnaceOnly;
	/** Piglins stay neutral toward players wearing this armor, like gold. */
	public final boolean piglinSafe;
	/** Items don't burn in fire or lava, like netherite. */
	public final boolean fireResistant;
	/** Dropped items aren't destroyed by explosions, like netherite in lava. */
	public final boolean blastResistant;
	/** The 3D armor and shield have a glowing layer ({@code <texture>_glowmask.png}). */
	public final boolean glowing;
	/** Gear is made by upgrading another material's gear at a smithing table, like netherite. */
	public final @Nullable Supplier<MaterialSet> upgradedFrom;

	/** Whether the material is mined. When false, the ore, raw item, and raw block fields are null. */
	public final boolean hasOre;
	public final @Nullable RegistrySupplier<Block> ore;
	/** Null for Nether ores, which have no deepslate variant. */
	public final @Nullable RegistrySupplier<Block> deepslateOre;
	public final @Nullable RegistrySupplier<Block> rawStorageBlock;
	public final @Nullable RegistrySupplier<Item> rawItem;

	/**
	 * What the gear is crafted from: usually this material's ingot, but it can be an existing item
	 * (Emerald uses the vanilla emerald) or a crafted item with its own name (Amethyst's Resonant Crystal).
	 */
	public final Supplier<Item> ingot;
	/** The material item this mod registers, or null when it's an existing item. */
	public final @Nullable RegistrySupplier<Item> registeredIngot;
	/** Registry name of the material item, e.g. "titanium_ingot" or "resonant_crystal". */
	public final String ingotName;
	/** Whether the material item is a metal ingot with a nugget and a storage block (c:ingots, c:nuggets). */
	public final boolean isMetal;
	public final @Nullable RegistrySupplier<Item> nugget;
	public final @Nullable RegistrySupplier<Block> storageBlock;
	/** The smithing template needed to upgrade into this material, if it's an upgrade material. */
	public final @Nullable RegistrySupplier<Item> upgradeTemplate;

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
		this.oreName = b.oreName;
		this.oreKind = b.oreKind;
		this.repairMaterials = b.repairMaterials;
		this.toolMaterial = b.toolMaterial;
		this.armorMaterial = b.armorMaterial;
		this.requiredToolTag = b.requiredToolTag;
		this.oreGeneration = List.copyOf(b.oreGeneration);
		this.behavior = b.behavior;
		this.galvanized = b.galvanized;
		this.blockingSpeed = b.blockingSpeed;
		this.shieldRaiseSeconds = b.shieldRaiseSeconds;
		this.blastFurnaceOnly = b.blastFurnaceOnly;
		this.piglinSafe = b.piglinSafe;
		this.fireResistant = b.fireResistant;
		this.blastResistant = b.blastResistant;
		this.glowing = b.glowing;
		this.upgradedFrom = b.upgradedFrom;
		this.hasOre = oreKind != OreKind.NONE;
		UnaryOperator<Item.Properties> fireproof = p -> fireResistant ? p.fireResistant()
			: blastResistant ? p.delayedComponent(DataComponents.DAMAGE_RESISTANT, context -> new DamageResistant(context.getOrThrow(DamageTypeTags.IS_EXPLOSION))) : p;

		// Ores and raw material.
		if (oreKind == OreKind.OVERWORLD) {
			this.ore = ModBlocks.register(oreName + "_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p), BlockBehaviour.Properties.of()
				.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F));
			this.deepslateOre = ModBlocks.register("deepslate_" + oreName + "_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p), BlockBehaviour.Properties.of()
				.mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE));
		} else if (oreKind == OreKind.ICE) {
			// Slippery like packed ice, but needs a proper pickaxe.
			this.ore = ModBlocks.register(oreName + "_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p), BlockBehaviour.Properties.of()
				.mapColor(MapColor.ICE).instrument(NoteBlockInstrument.CHIME).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
				.friction(0.98F).sound(SoundType.GLASS));
			this.deepslateOre = null;
		} else if (oreKind == OreKind.NETHER) {
			this.ore = ModBlocks.register(oreName + "_ore", p -> new DropExperienceBlock(ConstantInt.of(0), p), BlockBehaviour.Properties.of()
				.mapColor(MapColor.NETHER).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F).sound(SoundType.NETHER_ORE));
			this.deepslateOre = null;
		} else {
			this.ore = null;
			this.deepslateOre = null;
		}
		this.rawStorageBlock = hasOre ? ModBlocks.register("raw_" + oreName + "_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(b.rawColor).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)) : null;

		// The material item.
		this.isMetal = b.baseItem == null && b.craftedMaterial == null;
		this.ingotName = b.baseItem != null ? "" : b.craftedMaterial != null ? b.craftedMaterial : name + "_ingot";
		this.storageBlock = isMetal ? ModBlocks.register(name + "_block", Block::new, BlockBehaviour.Properties.of()
			.mapColor(b.metalColor).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON)) : null;
		blocks().forEach(block -> ModItems.register(block.getId().getPath(), p -> new BlockItem(block.get(), p),
			p -> fireproof.apply(p.useBlockDescriptionPrefix())));

		this.rawItem = hasOre ? ModItems.register("raw_" + oreName, Item::new, fireproof) : null;
		this.registeredIngot = b.baseItem == null ? ModItems.register(ingotName, Item::new, fireproof) : null;
		this.ingot = b.baseItem != null ? b.baseItem : registeredIngot;
		this.nugget = isMetal ? ModItems.register(name + "_nugget", Item::new, fireproof) : null;
		this.upgradeTemplate = upgradedFrom != null ? ModItems.register(name + "_upgrade_smithing_template",
			this::createUpgradeTemplate, fireproof) : null;

		// Tools. Attack damage and speed baselines follow the vanilla tier; the tool material adds its damage bonus on top.
		ToolMaterial tools = b.toolMaterial;
		ToolTier tier = b.toolTier;
		float speed = b.attackSpeedBonus;
		this.sword = tool("sword", p -> p.sword(tools, 3.0F, -2.4F + speed), fireproof);
		this.shovel = tool("shovel", p -> p.shovel(tools, 1.5F, -3.0F + speed), fireproof);
		this.pickaxe = tool("pickaxe", p -> p.pickaxe(tools, 1.0F, -2.8F + speed), fireproof);
		this.axe = tool("axe", p -> p.axe(tools, tier.axeDamage, tier.axeSpeed + speed), fireproof);
		this.hoe = tool("hoe", p -> p.hoe(tools, tier.hoeDamage, tier.hoeSpeed + speed), fireproof);
		float[] s = tier.spear;
		// A spear's attack speed is 1 / attackDuration - 4, so shorten the duration to add the same bonus.
		float spearDuration = 1.0F / (1.0F / s[0] + speed);
		this.spear = tool("spear", p -> p.spear(tools, spearDuration, s[1], s[2], s[3], s[4], s[5], s[6], s[7], s[8]), fireproof);

		this.helmet = armor(b, ArmorType.HELMET, fireproof);
		this.chestplate = armor(b, ArmorType.CHESTPLATE, fireproof);
		this.leggings = armor(b, ArmorType.LEGGINGS, fireproof);
		this.boots = armor(b, ArmorType.BOOTS, fireproof);

		ShieldStats shieldStats = b.shieldStats;
		this.shield = ModItems.register(name + "_shield", p -> b.shieldFactory.apply(p, this), p -> {
			p.durability(shieldStats.durability())
				.repairable(b.repairMaterials)
				.equippableUnswappable(EquipmentSlot.OFFHAND)
				.delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
					b.shieldRaiseSeconds,
					shieldStats.disableCooldownScale(),
					b.shieldBlocksAllExplosions
						// Explosions are blocked from every direction, not just the front.
						? List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F),
							new BlocksAttacks.DamageReduction(360.0F, Optional.of(context.getOrThrow(DamageTypeTags.IS_EXPLOSION)), 0.0F, 1.0F))
						: List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
					new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
					Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
					Optional.of(SoundEvents.SHIELD_BLOCK),
					Optional.of(SoundEvents.SHIELD_BREAK)
				))
				.component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
				.enchantable(b.toolMaterial.enchantmentValue());
			if (b.blockingSpeed != VANILLA_BLOCKING_SPEED) {
				p.component(DataComponents.USE_EFFECTS, new UseEffects(false, true, b.blockingSpeed));
			}
			return fireproof.apply(p);
		});
	}

	private RegistrySupplier<Item> tool(String kind, UnaryOperator<Item.Properties> stats, UnaryOperator<Item.Properties> fireproof) {
		return ModItems.register(name + "_" + kind, p -> new GearToolItem(p, this), p -> fireproof.apply(stats.apply(p)));
	}

	private RegistrySupplier<Item> armor(Builder b, ArmorType type, UnaryOperator<Item.Properties> fireproof) {
		String piece = switch (type) {
			case HELMET -> "helmet";
			case CHESTPLATE -> "chestplate";
			case LEGGINGS -> "leggings";
			case BOOTS -> "boots";
			case BODY -> throw new IllegalArgumentException("Body armor is not part of a material set");
		};
		return ModItems.register(name + "_" + piece, p -> b.armorFactory.apply(p, this), p -> {
			p.humanoidArmor(b.armorMaterial, type);
			if (!b.armorBonuses.isEmpty()) {
				ItemAttributeModifiers attributes = b.armorMaterial.createAttributes(type);
				EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(type.getSlot());
				for (ArmorBonus bonus : b.armorBonuses) {
					var id = GearExpansion.id("armor." + piece + "." + bonus.key());
					attributes = attributes.withModifierAdded(bonus.attribute(), new AttributeModifier(id, bonus.amount(), bonus.operation()), slot);
				}
				p.attributes(attributes);
			}
			return fireproof.apply(p);
		});
	}

	/** A smithing template like netherite's, upgrading {@link #upgradedFrom} gear into this material. */
	private SmithingTemplateItem createUpgradeTemplate(Item.Properties properties) {
		String prefix = "item.gearexpansion.smithing_template." + name + "_upgrade.";
		return new SmithingTemplateItem(
			Component.translatable(prefix + "applies_to").withStyle(ChatFormatting.BLUE),
			Component.translatable(prefix + "ingredients").withStyle(ChatFormatting.BLUE),
			Component.translatable(prefix + "base_slot_description"),
			Component.translatable(prefix + "additions_slot_description"),
			List.of("helmet", "sword", "chestplate", "pickaxe", "leggings", "axe", "boots", "hoe", "shovel", "spear").stream()
				.map(slot -> Identifier.withDefaultNamespace("container/slot/" + slot)).toList(),
			List.of(Identifier.withDefaultNamespace("container/slot/ingot")),
			properties
		);
	}

	public List<RegistrySupplier<Item>> tools() {
		return List.of(sword, pickaxe, axe, shovel, hoe, spear);
	}

	public List<RegistrySupplier<Item>> armorPieces() {
		return List.of(helmet, chestplate, leggings, boots);
	}

	/** The material's blocks: ores and raw block (if mined), then the storage block (if it's a metal). */
	public List<RegistrySupplier<Block>> blocks() {
		List<RegistrySupplier<Block>> blocks = new ArrayList<>();
		if (ore != null) {
			blocks.add(ore);
		}
		if (deepslateOre != null) {
			blocks.add(deepslateOre);
		}
		if (rawStorageBlock != null) {
			blocks.add(rawStorageBlock);
		}
		if (storageBlock != null) {
			blocks.add(storageBlock);
		}
		return blocks;
	}

	/** Items this mod registers for the material, in the order a player gets them: raw item, nugget, material item, upgrade template. */
	public List<RegistrySupplier<Item>> ingredients() {
		List<RegistrySupplier<Item>> items = new ArrayList<>();
		if (rawItem != null) {
			items.add(rawItem);
		}
		if (nugget != null) {
			items.add(nugget);
		}
		if (registeredIngot != null) {
			items.add(registeredIngot);
		}
		if (upgradeTemplate != null) {
			items.add(upgradeTemplate);
		}
		return items;
	}

	/** Tools, armor, and the shield: every item that has durability. */
	public List<RegistrySupplier<Item>> gear() {
		List<RegistrySupplier<Item>> gear = new ArrayList<>(tools());
		gear.addAll(armorPieces());
		gear.add(shield);
		return gear;
	}

	public boolean isGear(ItemStack stack) {
		return gear().stream().anyMatch(item -> stack.is(item.get()));
	}

	public boolean isWeapon(ItemStack stack) {
		return stack.is(sword.get()) || stack.is(spear.get()) || stack.is(axe.get());
	}

	/** Names for common {@code c:} ore and raw material tags, e.g. both "bauxite" and "aluminum". */
	public List<String> oreTagNames() {
		return List.copyOf(new LinkedHashSet<>(List.of(oreName, name)));
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
	 * One kind of ore vein. {@code airExposureDiscard} is the chance an ore block touching air
	 * is skipped, which makes ores mostly buried. {@code id} tells apart several placements for
	 * one material (empty for the main one).
	 */
	public record OreGeneration(String id, TagKey<Biome> biomes, int veinSize, int veinsPerChunk, int minY, int maxY, float airExposureDiscard) {
	}

	/** An extra attribute each armor piece gives, e.g. movement speed. */
	public record ArmorBonus(String key, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
	}

	public static final class Builder {
		private final String name;
		private final TagKey<Item> repairMaterials;
		private String oreName;
		private OreKind oreKind = OreKind.NONE;
		private ToolTier toolTier;
		private ToolMaterial toolMaterial;
		private float attackSpeedBonus;
		private ArmorMaterial armorMaterial;
		private final List<ArmorBonus> armorBonuses = new ArrayList<>();
		private ShieldStats shieldStats = new ShieldStats(336, 1.0F);
		private float blockingSpeed = VANILLA_BLOCKING_SPEED;
		private float shieldRaiseSeconds = VANILLA_SHIELD_RAISE_SECONDS;
		private boolean blastFurnaceOnly;
		private TagKey<Block> requiredToolTag = BlockTags.NEEDS_IRON_TOOL;
		private final List<OreGeneration> oreGeneration = new ArrayList<>();
		private GearBehavior behavior = GearBehavior.NONE;
		private boolean galvanized;
		private boolean piglinSafe;
		private boolean fireResistant;
		private boolean blastResistant;
		private boolean shieldBlocksAllExplosions;
		private boolean glowing;
		private @Nullable Supplier<Item> baseItem;
		private @Nullable String craftedMaterial;
		private @Nullable Supplier<MaterialSet> upgradedFrom;
		private MapColor metalColor = MapColor.METAL;
		private MapColor rawColor = MapColor.RAW_IRON;
		private BiFunction<Item.Properties, MaterialSet, Item> armorFactory = GearArmorItem::new;
		private BiFunction<Item.Properties, MaterialSet, Item> shieldFactory = GearShieldItem::new;

		private Builder(String name) {
			this.name = name;
			this.oreName = name;
			this.repairMaterials = TagKey.create(Registries.ITEM, GearExpansion.id(name + "_repair_materials"));
		}

		/** Tool stats. The tier sets what the tools can mine and vanilla's per-tool damage and speed baselines. */
		public Builder tools(ToolTier tier, int durability, float miningSpeed, float attackDamageBonus, int enchantability) {
			this.toolTier = tier;
			this.toolMaterial = new ToolMaterial(tier.incorrectBlocksForDrops, durability, miningSpeed, attackDamageBonus, enchantability, repairMaterials);
			return this;
		}

		/** Added to every tool's attack speed (vanilla swords have -2.4, so +0.3 attacks about 19% faster). */
		public Builder attackSpeedBonus(float bonus) {
			this.attackSpeedBonus = bonus;
			return this;
		}

		/** Armor stats. Durability is multiplied per piece like vanilla (iron is 15, diamond 33, netherite 37). */
		public Builder armor(int durabilityMultiplier, int helmet, int chestplate, int leggings, int boots,
				int enchantability, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance) {
			ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, GearExpansion.id(name));
			Map<ArmorType, Integer> defense = Map.of(
				ArmorType.HELMET, helmet, ArmorType.CHESTPLATE, chestplate, ArmorType.LEGGINGS, leggings, ArmorType.BOOTS, boots, ArmorType.BODY, chestplate);
			this.armorMaterial = new ArmorMaterial(durabilityMultiplier, defense, enchantability, equipSound, toughness, knockbackResistance, repairMaterials, asset);
			return this;
		}

		/** An attribute every armor piece adds on top of its defense, e.g. movement speed. */
		public Builder armorBonus(String key, Holder<Attribute> attribute, double perPiece, AttributeModifier.Operation operation) {
			this.armorBonuses.add(new ArmorBonus(key, attribute, perPiece, operation));
			return this;
		}

		public Builder shield(int durability, float disableCooldownScale) {
			this.shieldStats = new ShieldStats(durability, disableCooldownScale);
			return this;
		}

		/** Movement speed while blocking, as a fraction of normal speed. Vanilla shields use 0.2. */
		public Builder blockingSpeed(float speed) {
			this.blockingSpeed = speed;
			return this;
		}

		/** The raw ore and ore only smelt in a blast furnace, like Infernium's. */
		public Builder blastFurnaceOnly() {
			this.blastFurnaceOnly = true;
			return this;
		}

		/** Seconds after raising the shield before it blocks. Vanilla shields use 0.25; 0 blocks instantly. */
		public Builder shieldRaiseTime(float seconds) {
			this.shieldRaiseSeconds = seconds;
			return this;
		}

		/** The pickaxe tier needed to mine this material's ores and blocks, e.g. {@code BlockTags.NEEDS_DIAMOND_TOOL}. */
		public Builder requiresTool(TagKey<Block> tag) {
			this.requiredToolTag = tag;
			return this;
		}

		/** Names the ore, raw item, and raw block differently from the metal, e.g. Bauxite for Aluminum. */
		public Builder oreName(String oreName) {
			this.oreName = oreName;
			return this;
		}

		/** The main ore vein, generated in every Overworld biome as stone and deepslate ores. */
		public Builder ore(int veinSize, int veinsPerChunk, int minY, int maxY, float airExposureDiscard) {
			return ore("", BiomeTags.IS_OVERWORLD, veinSize, veinsPerChunk, minY, maxY, airExposureDiscard);
		}

		/** An extra Overworld ore vein limited to some biomes, e.g. richer surface deposits in badlands. */
		public Builder ore(String id, TagKey<Biome> biomes, int veinSize, int veinsPerChunk, int minY, int maxY, float airExposureDiscard) {
			this.oreKind = OreKind.OVERWORLD;
			this.oreGeneration.add(new OreGeneration(id, biomes, veinSize, veinsPerChunk, minY, maxY, airExposureDiscard));
			return this;
		}

		/** A netherrack ore vein in the Nether. */
		public Builder netherOre(int veinSize, int veinsPerChunk, int minY, int maxY, float airExposureDiscard) {
			this.oreKind = OreKind.NETHER;
			this.oreGeneration.add(new OreGeneration("", BiomeTags.IS_NETHER, veinSize, veinsPerChunk, minY, maxY, airExposureDiscard));
			return this;
		}

		/** An ore that grows inside packed ice and blue ice, in {@code biomes}. */
		public Builder iceOre(TagKey<Biome> biomes, int veinSize, int veinsPerChunk, int minY, int maxY, float airExposureDiscard) {
			this.oreKind = OreKind.ICE;
			this.oreGeneration.add(new OreGeneration("", biomes, veinSize, veinsPerChunk, minY, maxY, airExposureDiscard));
			return this;
		}

		/** Marks an alloy, made in the Alloy Forge. Alloys aren't mined, so they have no ore; this is for readability. */
		public Builder alloy() {
			return this;
		}

		/** Gear is crafted from an existing item instead of an ingot, e.g. the vanilla emerald. */
		public Builder baseItem(Supplier<Item> item) {
			this.baseItem = item;
			return this;
		}

		/** Gear is crafted from a new item with its own name instead of an ingot, e.g. "resonant_crystal". */
		public Builder craftedMaterial(String itemName) {
			this.craftedMaterial = itemName;
			return this;
		}

		/** Gear is made at a smithing table from another material's gear, the ingot, and an upgrade template. */
		public Builder upgradedFrom(Supplier<MaterialSet> base) {
			this.upgradedFrom = base;
			return this;
		}

		public Builder behavior(GearBehavior behavior) {
			this.behavior = behavior;
			return this;
		}

		/** Gear doesn't lose durability while its user is in water. */
		public Builder galvanized() {
			this.galvanized = true;
			return this;
		}

		/** Piglins treat this armor like gold and stay neutral. */
		public Builder piglinSafe() {
			this.piglinSafe = true;
			return this;
		}

		/** Items don't burn in fire or lava. */
		public Builder fireResistant() {
			this.fireResistant = true;
			return this;
		}

		/** Dropped items survive explosions. */
		public Builder blastResistant() {
			this.blastResistant = true;
			return this;
		}

		/** The shield blocks explosions from every direction while raised, not just from the front. */
		public Builder shieldBlocksAllExplosions() {
			this.shieldBlocksAllExplosions = true;
			return this;
		}

		/** The 3D armor and shield get a glowing layer from {@code <texture>_glowmask.png}. */
		public Builder glowing() {
			this.glowing = true;
			return this;
		}

		public Builder colors(MapColor metal, MapColor raw) {
			this.metalColor = metal;
			this.rawColor = raw;
			return this;
		}

		/** Item class for armor pieces. Defaults to {@link GearArmorItem}, which draws the material's 3D model. */
		public Builder armorItem(BiFunction<Item.Properties, MaterialSet, Item> factory) {
			this.armorFactory = factory;
			return this;
		}

		/** Item class for the shield. Defaults to {@link GearShieldItem}. */
		public Builder shieldItem(BiFunction<Item.Properties, MaterialSet, Item> factory) {
			this.shieldFactory = factory;
			return this;
		}

		public MaterialSet build() {
			if (toolMaterial == null || armorMaterial == null) {
				throw new IllegalStateException("Material " + name + " needs tool and armor stats");
			}
			if (baseItem != null && craftedMaterial != null) {
				throw new IllegalStateException("Material " + name + " can't have both a base item and a crafted material");
			}
			MaterialSet set = new MaterialSet(this);
			ModMaterials.ALL.add(set);
			return set;
		}
	}
}
