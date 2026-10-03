package com.gearexpansion.material.behavior;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.item.GearCombat;
import com.gearexpansion.material.GearBehavior;
import com.gearexpansion.material.MaterialSet;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.setbonus.SetBonuses;

/**
 * Pastel Princess: Charm weapons sometimes make a mob leave you alone for a few seconds. Each armor
 * piece earns a villager discount (Royal Favor), and the shield heals your pets when it blocks.
 */
public final class PastelPrincessBehavior implements GearBehavior {
	/** The Ender Dragon and the Wither can't be charmed. */
	public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses"));
	private static final double SHIELD_PET_RANGE = 8.0;
	private static final int SHIELD_REGENERATION_TICKS = 100;

	/** Mobs a princess weapon has charmed: who charmed them, and the game time it wears off. */
	private static final Map<UUID, Charm> CHARMED = new ConcurrentHashMap<>();

	@Override
	public void onHurtEnemy(MaterialSet set, ItemStack weapon, LivingEntity target, LivingEntity attacker) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (!set.isWeapon(weapon) || !(target instanceof Mob mob) || !mob.isAlive() || mob.is(BOSSES)
				|| !(target.level() instanceof ServerLevel level) || attacker.getRandom().nextInt(100) >= config.pastelPrincessCharmChance) {
			return;
		}
		long now = level.getGameTime();
		CHARMED.values().removeIf(charm -> charm.until() < now);
		CHARMED.put(mob.getUUID(), new Charm(attacker.getUUID(), now + config.pastelPrincessCharmSeconds * 20L));
		if (mob.getTarget() == attacker) {
			mob.setTarget(null);
		}
		level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
		level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
	}

	/** Whether {@code mob} is charmed by {@code target} and so won't attack them. */
	public static boolean isCharmedBy(Mob mob, LivingEntity target) {
		Charm charm = CHARMED.get(mob.getUUID());
		if (charm == null) {
			return false;
		}
		if (mob.level().getGameTime() >= charm.until()) {
			CHARMED.remove(mob.getUUID());
			return false;
		}
		return charm.charmer().equals(target.getUUID());
	}

	@Override
	public void onShieldBlock(MaterialSet set, LivingEntity defender, LivingEntity attacker, ItemStack shield, DamageSource source, float damage) {
		if (!(defender.level() instanceof ServerLevel level)) {
			return;
		}
		for (LivingEntity pet : level.getEntitiesOfClass(LivingEntity.class, defender.getBoundingBox().inflate(SHIELD_PET_RANGE),
				entity -> entity instanceof OwnableEntity ownable && ownable.getOwner() == defender)) {
			pet.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SHIELD_REGENERATION_TICKS, 0), defender);
			level.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + pet.getBbHeight(), pet.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
		}
	}

	/**
	 * The share villagers take off their prices for {@code player}: some for each armor piece, and more
	 * for the full set (Royal Court).
	 */
	public static double villagerDiscount(Player player) {
		GearExpansionConfig config = GearExpansionConfig.get();
		int pieces = GearCombat.piecesWorn(player, ModMaterials.PASTEL_PRINCESS);
		double discount = pieces * config.pastelPrincessDiscount / 100.0;
		if (SetBonuses.PASTEL_PRINCESS.isActive(player)) {
			discount += config.pastelPrincessCourtDiscount / 100.0;
		}
		return Math.min(discount, 0.9);
	}

	@Override
	public void appendTooltip(MaterialSet set, ItemStack stack, List<Component> lines) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (set.isWeapon(stack)) {
			lines.add(Component.translatable("trait.gearexpansion.charm", config.pastelPrincessCharmChance, config.pastelPrincessCharmSeconds)
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (stack.is(set.shield.get())) {
			lines.add(Component.translatable("trait.gearexpansion.royal_guard").withStyle(ChatFormatting.LIGHT_PURPLE));
		} else if (set.armorPieces().stream().anyMatch(piece -> stack.is(piece.get())) && config.pastelPrincessDiscount > 0) {
			lines.add(Component.translatable("trait.gearexpansion.royal_favor", config.pastelPrincessDiscount).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	private record Charm(UUID charmer, long until) {
	}
}
