package com.shouyun.tacticalpickup.client.network;

import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.network.ExitPickupModePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientPickupNetworking {
	private ClientPickupNetworking() {
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ExitPickupModePayload.TYPE, (payload, context) ->
			ClientPickupManager.getInstance().exitPickupMode()
		);
	}
}
