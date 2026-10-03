package com.gearexpansion.setbonus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import com.gearexpansion.config.GearExpansionConfig;
import com.gearexpansion.material.ModMaterials;

/**
 * Daydream: press jump again in midair for a second jump, and hold jump while falling to drift down
 * gently. The jump key is read from the input the client sends the server every tick.
 */
public final class FairyKeiSetBonus extends SetBonus {
	private static final double DOUBLE_JUMP_SPEED = 0.6;
	private static final int DRIFT_TICKS = 6;
	private static final int[] CLOUD_COLORS = {0xFFC8E8, 0xC8E8FF, 0xD8FFE0, 0xE8D8FF};

	/** Whether each wearer held jump last tick, to spot new presses. */
	private final Map<UUID, Boolean> jumpHeld = new ConcurrentHashMap<>();
	/** Wearers who've used their second jump and haven't landed since. */
	private final Set<UUID> usedDoubleJump = ConcurrentHashMap.newKeySet();

	public FairyKeiSetBonus() {
		super(ModMaterials.FAIRY_KEI);
	}

	@Override
	public boolean enabled() {
		return GearExpansionConfig.get().fairyKeiSetBonus;
	}

	@Override
	public List<Component> description() {
		return List.of(
			Component.translatable("set_bonus.gearexpansion.fairy_kei.double_jump"),
			Component.translatable("set_bonus.gearexpansion.fairy_kei.drift"));
	}

	@Override
	public void tick(ServerPlayer player) {
		boolean jumping = player.getLastClientInput().jump();
		boolean pressed = jumping && !jumpHeld.getOrDefault(player.getUUID(), false);
		jumpHeld.put(player.getUUID(), jumping);

		if (player.onGround() || player.isInWater() || player.onClimbable() || player.getAbilities().flying) {
			usedDoubleJump.remove(player.getUUID());
			return;
		}
		if (player.isFallFlying() || player.isPassenger()) {
			return;
		}
		if (pressed && usedDoubleJump.add(player.getUUID())) {
			doubleJump(player);
		} else if (jumping && player.getDeltaMovement().y < 0.0) {
			// Drift: a short Slow Falling, topped up while jump is held.
			MobEffectInstance current = player.getEffect(MobEffects.SLOW_FALLING);
			if (current == null || current.getDuration() < 3) {
				player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, DRIFT_TICKS, 0, true, false, false));
			}
		}
	}

	/** A second jump in midair, with a puff of pastel cloud. */
	public void doubleJump(ServerPlayer player) {
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, DOUBLE_JUMP_SPEED, motion.z);
		player.resetFallDistance();
		player.connection.send(new ClientboundSetEntityMotionPacket(player));

		ServerLevel level = player.level();
		for (int color : CLOUD_COLORS) {
			level.sendParticles(new DustParticleOptions(color, 1.3F), player.getX(), player.getY(), player.getZ(), 5, 0.35, 0.05, 0.35, 0.0);
		}
		level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 6, 0.3, 0.05, 0.3, 0.02);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8F, 1.6F);
	}
}
