package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;
import com.gearexpansion.network.GearHudPayload;

/** Shatterguard: a crystal shell absorbs one hit completely, shatters, and regrows. */
public final class AmethystSetBonus extends SetBonus {
	/** Game time each wearer's shell is whole again. Missing means whole. Not saved. */
	private final Map<UUID, Long> regrownAt = new ConcurrentHashMap<>();

	public AmethystSetBonus() {
		super(ModMaterials.AMETHYST);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().amethystSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(Component.translatable("set_bonus.gearexpansion.amethyst.shell", GearExpansionConfig.get().amethystShellRegrowSeconds));
	}

	private int regrowTicks() {
		return Math.max(1, GearExpansionConfig.get().amethystShellRegrowSeconds) * 20;
	}

	@Override
	public boolean cancelsDamage(LivingEntity wearer, ServerLevel level, DamageSource source, float damage) {
		// Ignore the repeated hits vanilla already ignores right after being hurt.
		if (damage <= 0 || wearer.getInvulnerableTime() > 10 || regrownAt.containsKey(wearer.getUUID())) {
			return false;
		}
		regrownAt.put(wearer.getUUID(), level.getGameTime() + regrowTicks());
		level.playSound(null, wearer.getX(), wearer.getY(), wearer.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2F, 0.8F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
			wearer.getX(), wearer.getY(1.0), wearer.getZ(), 40, 0.5, 0.8, 0.5, 0.2);
		return true;
	}

	@Override
	public void tick(ServerPlayer player) {
		Long regrown = regrownAt.get(player.getUUID());
		if (regrown != null && player.level().getGameTime() >= regrown) {
			regrownAt.remove(player.getUUID());
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.2F);
		}
	}

	@Override
	public void fillHud(ServerPlayer player, GearHudPayload.Builder hud) {
		Long regrown = regrownAt.get(player.getUUID());
		int max = regrowTicks();
		int left = regrown == null ? 0 : (int) Math.max(0, regrown - player.level().getGameTime());
		hud.shell(max - Math.min(max, left), max);
	}
}
