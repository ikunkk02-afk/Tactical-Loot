package com.shouyun.tacticalpickup.client;

import com.shouyun.tacticalpickup.client.hud.PickupHudRenderer;
import com.shouyun.tacticalpickup.client.network.ClientPickupNetworking;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class TacticalPickupClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPickupManager pickupManager = ClientPickupManager.getInstance();
		ClientPickupNetworking.register();
		ClientTickEvents.END_CLIENT_TICK.register(pickupManager::tick);
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> pickupManager.reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> pickupManager.reset());
		HudRenderCallback.EVENT.register(PickupHudRenderer::render);
	}
}
