package com.shouyun.tacticalpickup.client;

import com.shouyun.tacticalpickup.TacticalPickup;
import com.shouyun.tacticalpickup.client.config.ClientUiConfigManager;
import com.shouyun.tacticalpickup.client.filter.FilterManagementScreen;
import com.shouyun.tacticalpickup.client.hud.PickupHudRenderer;
import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.loot.LootScreen;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.ui.editor.TacticalLootSettingsScreen;
import com.shouyun.tacticalpickup.filter.ItemFilterManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TacticalPickup.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TacticalPickupClient {
    private static boolean initialized;

    private TacticalPickupClient() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        ClientKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (initialized) return;
            initialized = true;
            ClientPickupManager manager = ClientPickupManager.getInstance();
            ClientUiConfigManager.initialize(FMLPaths.CONFIGDIR.get().resolve(ClientUiConfigManager.CONFIG_FILE_NAME));
            manager.initialize(new ItemFilterManager(FMLPaths.CONFIGDIR.get().resolve(ItemFilterManager.CONFIG_FILE_NAME)));
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(TacticalLootSettingsScreen::new));
            MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
        });
    }

    public static final class ForgeEvents {
        private ForgeEvents() {}

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !initialized) return;
            Minecraft client = Minecraft.getInstance();
            ClientPickupManager manager = ClientPickupManager.getInstance();
            manager.tick(client);
            handleKeys(client, manager);
        }

        @SubscribeEvent
        public static void renderHud(RenderGuiEvent.Post event) {
            if (initialized) PickupHudRenderer.render(event.getGuiGraphics(), event.getPartialTick());
        }

        @SubscribeEvent
        public static void keyInput(InputEvent.Key event) {
            if (!initialized || event.getAction() != GLFW.GLFW_PRESS) return;
            Minecraft client = Minecraft.getInstance();
            ClientPickupManager manager = ClientPickupManager.getInstance();
            if (client.options.keySwapOffhand.matches(event.getKey(), event.getScanCode()) && manager.shouldCapturePickupKey(client)) {
                while (client.options.keySwapOffhand.consumeClick()) manager.handlePickupKey(client);
            }
        }

        @SubscribeEvent
        public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
            if (initialized && ClientPickupManager.getInstance().handleScroll(Minecraft.getInstance(), 0.0D, event.getScrollDelta())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void openingScreen(ScreenEvent.Opening event) {
            ClientPickupManager manager = ClientPickupManager.getInstance();
            if (initialized && event.getCurrentScreen() == null && event.getNewScreen() instanceof PauseScreen && manager.isPickupMode()) {
                manager.exitPickupMode();
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) { resetClient(); }

        @SubscribeEvent
        public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) { resetClient(); }

        private static void resetClient() {
            Minecraft client = Minecraft.getInstance();
            LootScreen.closeIfOpen(client);
            ClientPickupManager.getInstance().reset();
        }
    }

    private static void handleKeys(Minecraft client, ClientPickupManager pickupManager) {
        while (ClientKeyMappings.EDIT_UI.consumeClick()) {
            if (client.player != null && client.level != null && client.player.isAlive() && client.screen == null && client.getOverlay() == null) {
                pickupManager.exitPickupMode();
                client.setScreen(new TacticalLootSettingsScreen(null));
            }
        }
        while (ClientKeyMappings.CYCLE_FILTER.consumeClick()) pickupManager.cycleSelectedFilter(client);
        while (ClientKeyMappings.OPEN_FILTERS.consumeClick()) {
            if (client.player != null && client.screen == null && client.getOverlay() == null) {
                pickupManager.exitPickupMode();
                client.setScreen(new FilterManagementScreen(null));
            }
        }
        while (ClientKeyMappings.OPEN_LOOT_SCREEN.consumeClick()) {
            if (client.player == null || client.level == null || !client.player.isAlive() || client.screen != null || client.getOverlay() != null) continue;
            if (!pickupManager.hasAvailableLoot(client)) {
                pickupManager.requestScan();
                client.player.displayClientMessage(Component.translatable("tactical_pickup.loot.no_nearby_items"), true);
                continue;
            }
            pickupManager.exitPickupMode();
            client.setScreen(new LootScreen());
        }
    }
}
