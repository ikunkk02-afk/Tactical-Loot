package com.shouyun.tacticalpickup.client;

import com.shouyun.tacticalpickup.TacticalPickup;
import com.shouyun.tacticalpickup.client.hud.PickupHudRenderer;
import com.shouyun.tacticalpickup.client.config.ClientUiConfigManager;
import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.loot.LootScreen;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.ui.editor.TacticalLootSettingsScreen;
import com.shouyun.tacticalpickup.client.filter.FilterManagementScreen;
import com.shouyun.tacticalpickup.filter.ItemFilterManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = TacticalPickup.MOD_ID, dist = Dist.CLIENT)
public final class TacticalPickupClient {
	public TacticalPickupClient(ModContainer container) {
		ClientPickupManager pickupManager = ClientPickupManager.getInstance();
		ClientUiConfigManager.initialize(
			FMLPaths.CONFIGDIR.get().resolve(ClientUiConfigManager.CONFIG_FILE_NAME)
		);
		pickupManager.initialize(new ItemFilterManager(
			FMLPaths.CONFIGDIR.get().resolve(ItemFilterManager.CONFIG_FILE_NAME)
		));
		container.registerExtensionPoint(
			IConfigScreenFactory.class,
			(IConfigScreenFactory) (modContainer, parent) -> new TacticalLootSettingsScreen(parent)
		);
		NeoForge.EVENT_BUS.addListener(this::onClientTick);
		NeoForge.EVENT_BUS.addListener(this::onLogin);
		NeoForge.EVENT_BUS.addListener(this::onLogout);
		NeoForge.EVENT_BUS.addListener(this::onRenderGui);
		NeoForge.EVENT_BUS.addListener(this::onMouseScroll);
	}

	private void onClientTick(ClientTickEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		ClientPickupManager pickupManager = ClientPickupManager.getInstance();
		pickupManager.tick(client);
		handleKeys(client, pickupManager);
	}

	private void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
		resetClientState();
	}

	private void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		resetClientState();
	}

	private void resetClientState() {
		Minecraft client = Minecraft.getInstance();
		LootScreen.closeIfOpen(client);
		ClientPickupManager.getInstance().reset();
	}

	private void onRenderGui(RenderGuiEvent.Post event) {
		PickupHudRenderer.render(event.getGuiGraphics(), event.getPartialTick());
	}

	private void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		if (ClientPickupManager.getInstance().handleScroll(
			Minecraft.getInstance(),
			event.getScrollDeltaX(),
			event.getScrollDeltaY()
		)) {
			event.setCanceled(true);
		}
	}

	private static void handleKeys(Minecraft client, ClientPickupManager pickupManager) {
		while (ClientKeyMappings.EDIT_UI.consumeClick()) {
			if (client.player != null
					&& client.level != null
					&& client.player.isAlive()
					&& client.screen == null
					&& client.getOverlay() == null) {
				pickupManager.exitPickupMode();
				client.setScreen(new TacticalLootSettingsScreen(null));
			}
		}

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
