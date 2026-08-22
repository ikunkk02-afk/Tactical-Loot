package com.shouyun.tacticalpickup.network;

import net.minecraft.network.FriendlyByteBuf;

public record PickupRequestPayload(int entityId, int requestedAmount) {
	public static final int ALL_ITEMS = 0;
	public static void encode(PickupRequestPayload message, FriendlyByteBuf buffer) { buffer.writeVarInt(message.entityId); buffer.writeVarInt(message.requestedAmount); }
	public static PickupRequestPayload decode(FriendlyByteBuf buffer) { return new PickupRequestPayload(buffer.readVarInt(), buffer.readVarInt()); }
}
