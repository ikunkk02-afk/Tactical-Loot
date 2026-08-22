package com.shouyun.tacticalpickup.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
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

	public static void register() {
		KeyBindingHelper.registerKeyBinding(CYCLE_FILTER);
		KeyBindingHelper.registerKeyBinding(OPEN_FILTERS);
		KeyBindingHelper.registerKeyBinding(OPEN_LOOT_SCREEN);
		KeyBindingHelper.registerKeyBinding(EDIT_UI);
	}
}
