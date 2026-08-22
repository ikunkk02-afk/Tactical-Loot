package com.shouyun.tacticalpickup.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Inject(
		method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void tacticalPickup$blockAutomaticPickup(Player player, CallbackInfo callbackInfo) {
		callbackInfo.cancel();
	}
}
