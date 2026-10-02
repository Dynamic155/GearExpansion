package com.gearexpansion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.gearexpansion.GearExpansion;

/**
 * Server to client: the set bonus meters shown above the hotbar. A max of 0 hides that meter.
 *
 * @param spring Brass spring wind-up
 * @param heat Infernium heat gauge (fills while in lava)
 * @param shell Amethyst crystal shell regrowth (full means ready)
 * @param cooldown time left before the set ability can be used again
 */
public record GearHudPayload(int spring, int springMax, int heat, int heatMax, int shell, int shellMax, int cooldown, int cooldownMax)
		implements CustomPacketPayload {
	public static final Type<GearHudPayload> TYPE = new Type<>(GearExpansion.id("hud"));
	public static final GearHudPayload EMPTY = new GearHudPayload(0, 0, 0, 0, 0, 0, 0, 0);

	public static final StreamCodec<ByteBuf, GearHudPayload> STREAM_CODEC = StreamCodec.of(
		(buf, hud) -> {
			for (int value : new int[] {hud.spring, hud.springMax, hud.heat, hud.heatMax, hud.shell, hud.shellMax, hud.cooldown, hud.cooldownMax}) {
				ByteBufCodecs.VAR_INT.encode(buf, value);
			}
		},
		buf -> new GearHudPayload(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
			ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf))
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** Collects meter values from set bonuses each tick. */
	public static final class Builder {
		int spring;
		int springMax;
		int heat;
		int heatMax;
		int shell;
		int shellMax;
		int cooldown;
		int cooldownMax;

		public Builder spring(int value, int max) {
			this.spring = value;
			this.springMax = max;
			return this;
		}

		public Builder heat(int value, int max) {
			this.heat = value;
			this.heatMax = max;
			return this;
		}

		public Builder shell(int value, int max) {
			this.shell = value;
			this.shellMax = max;
			return this;
		}

		public Builder cooldown(int value, int max) {
			this.cooldown = value;
			this.cooldownMax = max;
			return this;
		}

		public GearHudPayload build() {
			return new GearHudPayload(spring, springMax, heat, heatMax, shell, shellMax, cooldown, cooldownMax);
		}
	}
}
