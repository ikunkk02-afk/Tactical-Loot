package com.shouyun.tacticalpickup.mixin.client;

import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "handleKeybinds()V", at = @At("HEAD"))
	private void tacticalPickup$handlePickupKey(CallbackInfo callbackInfo) {
		Minecraft client = (Minecraft) (Object) this;
		ClientPickupManager manager = ClientPickupManager.getInstance();

		if (manager.shouldCapturePickupKey(client)) {
			while (client.options.keySwapOffhand.consumeClick()) {
				manager.handlePickupKey(client);
			}
		}
	}
}
