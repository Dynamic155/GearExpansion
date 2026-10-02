package com.gearexpansion.setbonus;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.network.GearHudPayload;

/**
 * A bonus granted for wearing all four armor pieces of one material.
 * Subclasses override only the hooks they need.
 */
public abstract class SetBonus {
	public static final int PIECES = 4;

	public final MaterialSet material;

	protected SetBonus(MaterialSet material) {
		this.material = material;
	}

	/** Whether the bonus is switched on in the config. */
	public abstract boolean enabled();

	/** Tooltip lines describing the bonus, shown under each armor piece. */
	public abstract List<Component> description();

	/** Called every tick for each player wearing the full set, on the server. */
	public void tick(ServerPlayer player) {
	}

	/** Lets the bonus change how much durability an item loses. Called only while the full set is worn. */
	public int modifyDurabilityLoss(int amount, LivingEntity wearer, ServerLevel level) {
		return amount;
	}

	/** Lets the bonus change a potion effect as it is applied to the wearer. Called only while the full set is worn. */
	public MobEffectInstance modifyNewEffect(MobEffectInstance effect, LivingEntity wearer) {
		return effect;
	}

	/**
	 * Whether the bonus stops a hit entirely, e.g. Amethyst's crystal shell. Called only while the
	 * full set is worn, before the damage is applied. Damage that bypasses invulnerability is never offered.
	 */
	public boolean cancelsDamage(LivingEntity wearer, ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	/** Changes damage the wearer takes, before armor. Called only while the full set is worn. */
	public float modifyIncomingDamage(LivingEntity wearer, DamageSource source, float damage) {
		return damage;
	}

	/** When the wearer hurts {@code target}. Called only while the full set is worn. */
	public void onAttack(LivingEntity wearer, LivingEntity target, DamageSource source, float damage) {
	}

	/** Whether this set has an ability for the Set Ability key. */
	public boolean hasAbility() {
		return false;
	}

	/** The Set Ability key was pressed while wearing the full set. */
	public void useAbility(ServerPlayer player) {
	}

	/** Adds this set's meters to the HUD. Called only while the full set is worn. */
	public void fillHud(ServerPlayer player, GearHudPayload.Builder hud) {
	}

	/** When the wearer blocks an attack with any shield. Called only while the full set is worn. */
	public void onShieldBlock(LivingEntity wearer, LivingEntity attacker, DamageSource source, float damage) {
	}

	/** Lets the bonus change how much experience an orb gives the wearer. Called only while the full set is worn. */
	public int modifyExperience(int amount, Player wearer) {
		return amount;
	}

	/** Extra bookshelves an enchanting table counts for the wearer (vanilla caps the useful total at 15). */
	public int extraEnchantingBookshelves(Player wearer) {
		return 0;
	}

	/** Attribute modifiers applied to players while the full set is worn, and removed when it isn't. */
	public List<AttributeBonus> attributeBonuses() {
		return List.of();
	}

	public Component name() {
		return Component.translatable("set_bonus.gearexpansion." + material.name);
	}

	public int piecesWorn(LivingEntity entity) {
		int worn = 0;
		for (var entry : material.armorBySlot().entrySet()) {
			if (entity.getItemBySlot(entry.getKey()).is(entry.getValue().get())) {
				worn++;
			}
		}
		return worn;
	}

	public boolean isActive(LivingEntity entity) {
		return enabled() && piecesWorn(entity) == PIECES;
	}

	/** An attribute modifier granted by a set bonus. The id must stay the same so it can be removed later. */
	public record AttributeBonus(Identifier id, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
		public AttributeModifier modifier() {
			return new AttributeModifier(id, amount, operation);
		}
	}

	/** Whether the item is one of this set's armor pieces. */
	public boolean isPiece(ItemStack stack) {
		return material.armorPieces().stream().anyMatch(piece -> stack.is(piece.get()));
	}
}
