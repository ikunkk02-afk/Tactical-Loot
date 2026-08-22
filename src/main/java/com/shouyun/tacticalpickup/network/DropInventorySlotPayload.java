package com.shouyun.tacticalpickup.network;

import net.minecraft.network.FriendlyByteBuf;

public record DropInventorySlotPayload(int sourceSlot, int requestedAmount) {
	public static final int ALL_ITEMS = 0;
	public static void encode(DropInventorySlotPayload message, FriendlyByteBuf buffer) { buffer.writeVarInt(message.sourceSlot); buffer.writeVarInt(message.requestedAmount); }
	public static DropInventorySlotPayload decode(FriendlyByteBuf buffer) { return new DropInventorySlotPayload(buffer.readVarInt(), buffer.readVarInt()); }
}
