package com.shouyun.tacticalpickup.mixin;

import com.shouyun.tacticalpickup.pickup.PickupManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Inject(
		method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V",
		at = @At("HEAD"),
		cancellable = true
	)
	private void tacticalPickup$blockAutomaticPickup(Player player, CallbackInfo callbackInfo) {
		ItemEntity self = (ItemEntity) (Object) this;

		if (!PickupManager.isManualPickupAuthorized(self, player)) {
			callbackInfo.cancel();
		}
	}

	@ModifyArg(
		method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"
		),
		index = 1
	)
	private int tacticalPickup$correctTakeAmount(int vanillaAmount) {
		return PickupManager.getAuthorizedPickupAmount((ItemEntity) (Object) this, vanillaAmount);
	}

	@ModifyArg(
		method = "playerTouch(Lnet/minecraft/world/entity/player/Player;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;I)V"
		),
		index = 1
	)
	private int tacticalPickup$correctAwardedAmount(int vanillaAmount) {
		return PickupManager.getAuthorizedPickupAmount((ItemEntity) (Object) this, vanillaAmount);
	}
}
