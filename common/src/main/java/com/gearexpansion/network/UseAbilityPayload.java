package com.gearexpansion.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.gearexpansion.GearExpansion;

/** Client to server: the player pressed the Set Ability key. */
public record UseAbilityPayload() implements CustomPacketPayload {
	public static final Type<UseAbilityPayload> TYPE = new Type<>(GearExpansion.id("use_ability"));
	public static final UseAbilityPayload INSTANCE = new UseAbilityPayload();
	public static final StreamCodec<ByteBuf, UseAbilityPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
