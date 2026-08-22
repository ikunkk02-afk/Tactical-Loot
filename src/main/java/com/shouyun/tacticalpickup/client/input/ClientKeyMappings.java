package com.shouyun.tacticalpickup.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.shouyun.tacticalpickup.TacticalPickup;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TacticalPickup.MOD_ID, value = Dist.CLIENT)
public final class ClientKeyMappings {
	public static final String CATEGORY = "key.categories.tactical_pickup";
	public static final KeyMapping CYCLE_FILTER = new KeyMapping(
		"key.tactical_pickup.cycle_filter",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_X,
		CATEGORY
	);
	public static final KeyMapping OPEN_FILTERS = new KeyMapping(
		"key.tactical_pickup.open_filters",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_O,
		CATEGORY
	);
	public static final KeyMapping OPEN_LOOT_SCREEN = new KeyMapping(
		"key.tactical_pickup.open_loot_screen",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_H,
		CATEGORY
	);
	public static final KeyMapping EDIT_UI = new KeyMapping(
		"key.tactical_pickup.edit_ui",
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_U,
		CATEGORY
	);

	private ClientKeyMappings() {
	}

	@SubscribeEvent
	public static void register(RegisterKeyMappingsEvent event) {
		event.register(CYCLE_FILTER);
		event.register(OPEN_FILTERS);
		event.register(OPEN_LOOT_SCREEN);
		event.register(EDIT_UI);
	}
}
