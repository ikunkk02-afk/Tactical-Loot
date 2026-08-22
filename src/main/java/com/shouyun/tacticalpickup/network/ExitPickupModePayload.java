package com.shouyun.tacticalpickup.network;

import net.minecraft.network.FriendlyByteBuf;

public record ExitPickupModePayload() {
	public static final ExitPickupModePayload INSTANCE = new ExitPickupModePayload();
	public static void encode(ExitPickupModePayload message, FriendlyByteBuf buffer) {}
	public static ExitPickupModePayload decode(FriendlyByteBuf buffer) { return INSTANCE; }
}
