package com.shouyun.tacticalpickup.client.mixin;

import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
	private void tacticalPickup$handlePickupScroll(long window, double horizontal, double vertical, CallbackInfo callbackInfo) {
		Minecraft client = Minecraft.getInstance();

		if (window == client.getWindow().getWindow()
				&& ClientPickupManager.getInstance().handleScroll(client, horizontal, vertical)) {
			callbackInfo.cancel();
		}
	}
}
