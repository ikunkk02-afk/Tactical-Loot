package com.shouyun.tacticalpickup.client.network;

import com.shouyun.tacticalpickup.client.loot.LootScreen;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import net.minecraft.client.Minecraft;

public final class ClientPickupNetworking {
	private ClientPickupNetworking() {
	}

	public static void handleExitPayload() {
		Minecraft client = Minecraft.getInstance();
		ClientPickupManager.getInstance().exitPickupMode();
		LootScreen.closeIfOpen(client);
	}
}
