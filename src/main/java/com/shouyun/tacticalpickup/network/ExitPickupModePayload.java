package com.shouyun.tacticalpickup.network;

import com.shouyun.tacticalpickup.TacticalPickup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ExitPickupModePayload() implements CustomPacketPayload {
	public static final ExitPickupModePayload INSTANCE = new ExitPickupModePayload();
	public static final Type<ExitPickupModePayload> TYPE = new Type<>(TacticalPickup.id("exit_pickup_mode"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ExitPickupModePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
