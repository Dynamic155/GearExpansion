package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.network.GearHudPayload;

/**
 * Transformation: the Set Ability key transforms the wearer for a while, with Strength, Speed,
 * Resistance, and Slow Falling, wrapped in sparkles. It has a long cooldown.
 */
public final class MagicalGirlSetBonus extends SetBonus {
	private static final int[] SPARKLE_COLORS = {0xFF7AD9, 0xFFE066, 0x8FD8FF, 0xC59BFF};

	private final Map<UUID, Long> readyAt = new ConcurrentHashMap<>();
	private final Map<UUID, Long> transformedUntil = new ConcurrentHashMap<>();

	public MagicalGirlSetBonus() {
		super(ModMaterials.MAGICAL_GIRL);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().magicalGirlSetBonus;
	}

	@Override
	public List<Component> description() {
		GearExpansionConfig config = GearExpansionConfig.get();
		return List.of(
			Component.translatable("set_bonus.gearexpansion.magical_girl.transform", Component.keybind("key.gearexpansion.set_ability"),
				config.magicalGirlTransformSeconds),
			Component.translatable("set_bonus.gearexpansion.magical_girl.effects"),
			Component.translatable("set_bonus.gearexpansion.magical_girl.cooldown", config.magicalGirlTransformCooldown));
	}

	@Override
	public boolean hasAbility() {
		return true;
	}

	@Override
	public void useAbility(ServerPlayer player) {
		ServerLevel level = player.level();
		GearExpansionConfig config = GearExpansionConfig.get();
		long now = level.getGameTime();
		long ready = readyAt.getOrDefault(player.getUUID(), 0L);
		if (now < ready) {
			player.sendSystemMessage(Component.translatable("ability.gearexpansion.cooldown", (ready - now + 19) / 20).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		int ticks = config.magicalGirlTransformSeconds * 20;
		readyAt.put(player.getUUID(), now + config.magicalGirlTransformCooldown * 20L);
		transformedUntil.put(player.getUUID(), now + ticks);
		for (var effect : List.of(MobEffects.STRENGTH, MobEffects.SPEED, MobEffects.RESISTANCE, MobEffects.SLOW_FALLING)) {
			player.addEffect(new MobEffectInstance(effect, ticks, 0, false, false, true), player);
		}
		for (int color : SPARKLE_COLORS) {
			level.sendParticles(new DustParticleOptions(color, 1.2F), player.getX(), player.getY(1.0), player.getZ(), 25, 0.6, 1.0, 0.6, 0.0);
		}
		level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(0.5), player.getZ(), 40, 0.4, 0.8, 0.4, 0.15);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.6F);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.2F);
		player.sendSystemMessage(Component.translatable("set_bonus.gearexpansion.magical_girl.transformed").withStyle(ChatFormatting.LIGHT_PURPLE), true);
	}

	/** Whether the player is transformed right now. */
	public boolean isTransformed(ServerPlayer player) {
		return player.level().getGameTime() < transformedUntil.getOrDefault(player.getUUID(), 0L);
	}

	@Override
	public void tick(ServerPlayer player) {
		if (player.tickCount % 4 == 0 && isTransformed(player)) {
			int color = SPARKLE_COLORS[(player.tickCount / 4) % SPARKLE_COLORS.length];
			player.level().sendParticles(new DustParticleOptions(color, 0.9F), player.getX(), player.getY(0.6), player.getZ(), 2, 0.4, 0.6, 0.4, 0.0);
		}
	}

	@Override
	public void fillHud(ServerPlayer player, GearHudPayload.Builder hud) {
		long left = readyAt.getOrDefault(player.getUUID(), 0L) - player.level().getGameTime();
		hud.cooldown((int) Math.max(0, left), GearExpansionConfig.get().magicalGirlTransformCooldown * 20);
	}
}
