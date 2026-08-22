package com.shouyun.tacticalpickup.network;

import com.shouyun.tacticalpickup.TacticalPickup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DropInventorySlotPayload(int sourceSlot, int requestedAmount) implements CustomPacketPayload {
	public static final int ALL_ITEMS = 0;
	public static final Type<DropInventorySlotPayload> TYPE = new Type<>(
		TacticalPickup.id("drop_inventory_slot")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, DropInventorySlotPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT,
		DropInventorySlotPayload::sourceSlot,
		ByteBufCodecs.VAR_INT,
		DropInventorySlotPayload::requestedAmount,
		DropInventorySlotPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
