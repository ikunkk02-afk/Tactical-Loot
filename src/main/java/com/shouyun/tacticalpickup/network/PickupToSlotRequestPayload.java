package com.shouyun.tacticalpickup.network;

import com.shouyun.tacticalpickup.TacticalPickup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PickupToSlotRequestPayload(int entityId, int requestedAmount, int targetSlot)
		implements CustomPacketPayload {
	public static final Type<PickupToSlotRequestPayload> TYPE = new Type<>(
		TacticalPickup.id("pickup_to_slot_request")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, PickupToSlotRequestPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		PickupToSlotRequestPayload::entityId,
		ByteBufCodecs.VAR_INT,
		PickupToSlotRequestPayload::requestedAmount,
		ByteBufCodecs.VAR_INT,
		PickupToSlotRequestPayload::targetSlot,
		PickupToSlotRequestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
