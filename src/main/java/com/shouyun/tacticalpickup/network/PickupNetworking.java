package com.shouyun.tacticalpickup.network;

import com.shouyun.tacticalpickup.pickup.PickupRequestHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class PickupNetworking {
	private PickupNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(PickupRequestPayload.TYPE, PickupRequestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ExitPickupModePayload.TYPE, ExitPickupModePayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PickupRequestPayload.TYPE, (payload, context) ->
			PickupRequestHandler.handle(context.player(), payload.entityId())
		);
	}
}
