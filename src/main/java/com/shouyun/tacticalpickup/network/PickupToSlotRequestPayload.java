package com.shouyun.tacticalpickup.network;

import net.minecraft.network.FriendlyByteBuf;

public record PickupToSlotRequestPayload(int entityId, int requestedAmount, int targetSlot) {
	public static void encode(PickupToSlotRequestPayload message, FriendlyByteBuf buffer) { buffer.writeVarInt(message.entityId); buffer.writeVarInt(message.requestedAmount); buffer.writeVarInt(message.targetSlot); }
	public static PickupToSlotRequestPayload decode(FriendlyByteBuf buffer) { return new PickupToSlotRequestPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()); }
}
