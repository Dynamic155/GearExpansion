package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Unbreakable Will: gear loses durability more slowly, and dropping to low health
 * grants a short burst of Resistance on a cooldown.
 */
public final class TitaniumSetBonus extends SetBonus {
	/** Game time when each player's Resistance bonus is ready again. Not saved; resets on restart. */
	private final Map<UUID, Long> readyAt = new WeakHashMap<>();

	public TitaniumSetBonus() {
		super(ModMaterials.TITANIUM);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().titaniumSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.titanium.durability", config.titaniumDurabilitySaving),
			Component.translatable("set_bonus.gearexpansion.titanium.resistance",
				config.titaniumResistanceThreshold, config.titaniumResistanceSeconds, config.titaniumResistanceCooldown)
		);
	}

	@Override
	public int modifyDurabilityLoss(int amount, LivingEntity wearer, ServerLevel level) {
		float saving = Mth.clamp(GearExpansionConfig.get().titaniumDurabilitySaving, 0, 100) / 100.0F;
		float scaled = amount * (1.0F - saving);
		int whole = Mth.floor(scaled);
		// Round the leftover fraction randomly, so single points of damage are also reduced on average.
		return whole + (level.getRandom().nextFloat() < scaled - whole ? 1 : 0);
	}

	@Override
	public void tick(ServerPlayer player) {
		GearExpansionConfig config = GearExpansionConfig.get();
		if (player.getHealth() > player.getMaxHealth() * config.titaniumResistanceThreshold / 100.0F || player.isDeadOrDying()) {
			return;
		}

		ServerLevel level = player.level();
		long now = level.getGameTime();
		if (now < readyAt.getOrDefault(player.getUUID(), 0L)) {
			return;
		}
		readyAt.put(player.getUUID(), now + config.titaniumResistanceCooldown * 20L);

		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, config.titaniumResistanceSeconds * 20, 0));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.6F);
		level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.1);
		player.sendSystemMessage(Component.translatable("set_bonus.gearexpansion.titanium.triggered").withStyle(ChatFormatting.GRAY), true);
	}
}
