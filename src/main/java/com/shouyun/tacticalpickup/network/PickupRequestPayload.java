package com.shouyun.tacticalpickup.network;

import com.shouyun.tacticalpickup.TacticalPickup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PickupRequestPayload(int entityId, int requestedAmount) implements CustomPacketPayload {
	public static final int ALL_ITEMS = 0;
	public static final Type<PickupRequestPayload> TYPE = new Type<>(TacticalPickup.id("pickup_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PickupRequestPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		PickupRequestPayload::entityId,
		ByteBufCodecs.VAR_INT,
		PickupRequestPayload::requestedAmount,
		PickupRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
