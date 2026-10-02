package com.gearexpansion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.gearexpansion.GearExpansion;
import com.gearexpansion.config.GearExpansionConfig;

/**
 * Server to client: the server's settings, as the JSON from {@link GearExpansionConfig#toSyncJson()}.
 * Sent when a player joins, so tooltips and client-side effects match what the server does.
 */
public record ConfigSyncPayload(String json) implements CustomPacketPayload {
	public static final Type<ConfigSyncPayload> TYPE = new Type<>(GearExpansion.id("config"));

	public static final StreamCodec<ByteBuf, ConfigSyncPayload> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(ConfigSyncPayload::new, ConfigSyncPayload::json);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
