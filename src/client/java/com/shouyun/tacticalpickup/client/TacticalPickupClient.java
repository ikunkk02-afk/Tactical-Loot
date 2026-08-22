package com.shouyun.tacticalpickup.client;

import com.shouyun.tacticalpickup.client.hud.PickupHudRenderer;
import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.loot.LootScreen;
import com.shouyun.tacticalpickup.client.network.ClientPickupNetworking;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.filter.FilterManagementScreen;
import com.shouyun.tacticalpickup.filter.ItemFilterManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TacticalPickupClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPickupManager pickupManager = ClientPickupManager.getInstance();
		pickupManager.initialize(new ItemFilterManager(
			FabricLoader.getInstance().getConfigDir().resolve(ItemFilterManager.CONFIG_FILE_NAME)
		));
		ClientKeyMappings.register();
		ClientPickupNetworking.register();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			pickupManager.tick(client);
			handleKeys(client, pickupManager);
		});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			LootScreen.closeIfOpen(client);
			pickupManager.reset();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			LootScreen.closeIfOpen(client);
			pickupManager.reset();
		});
		HudRenderCallback.EVENT.register(PickupHudRenderer::render);
	}

	private static void handleKeys(Minecraft client, ClientPickupManager pickupManager) {
		while (ClientKeyMappings.CYCLE_FILTER.consumeClick()) {
			pickupManager.cycleSelectedFilter(client);
		}

		while (ClientKeyMappings.OPEN_FILTERS.consumeClick()) {
			if (client.player != null && client.screen == null && client.getOverlay() == null) {
				pickupManager.exitPickupMode();
				client.setScreen(new FilterManagementScreen(null));
			}
		}

		while (ClientKeyMappings.OPEN_LOOT_SCREEN.consumeClick()) {
			if (client.player == null
					|| client.level == null
					|| !client.player.isAlive()
					|| client.screen != null
					|| client.getOverlay() != null) {
				continue;
			}

			if (!pickupManager.hasAvailableLoot(client)) {
				pickupManager.requestScan();
				client.player.displayClientMessage(
					Component.translatable("tactical_pickup.loot.no_nearby_items"),
					true
				);
				continue;
			}

			pickupManager.exitPickupMode();
			client.setScreen(new LootScreen());
		}
	}
}
