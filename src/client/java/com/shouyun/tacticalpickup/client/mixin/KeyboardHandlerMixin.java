package com.shouyun.tacticalpickup.client.mixin;

import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), cancellable = true)
	private void tacticalPickup$exitOnEscape(
		long window,
		int key,
		int scanCode,
		int action,
		int modifiers,
		CallbackInfo callbackInfo
	) {
		Minecraft client = Minecraft.getInstance();
		ClientPickupManager manager = ClientPickupManager.getInstance();

		if (window == client.getWindow().getWindow()
				&& key == GLFW.GLFW_KEY_ESCAPE
				&& action == GLFW.GLFW_PRESS
				&& client.screen == null
				&& client.getOverlay() == null
				&& manager.isPickupMode()) {
			manager.exitPickupMode();
			callbackInfo.cancel();
		}
	}
}
