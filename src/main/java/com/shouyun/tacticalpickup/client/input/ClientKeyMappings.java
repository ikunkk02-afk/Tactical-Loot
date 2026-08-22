package com.shouyun.tacticalpickup.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

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

	public static void register(RegisterKeyMappingsEvent event) {
		event.register(CYCLE_FILTER);
		event.register(OPEN_FILTERS);
		event.register(OPEN_LOOT_SCREEN);
		event.register(EDIT_UI);
	}
}
