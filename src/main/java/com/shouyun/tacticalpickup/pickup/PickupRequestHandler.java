package com.shouyun.tacticalpickup.pickup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PickupRequestHandler {
	private PickupRequestHandler() {
	}

	public static void handle(ServerPlayer player, int entityId) {
		if (player.isRemoved() || !player.isAlive() || player.isSpectator()) {
			return;
		}

		Entity entity = player.serverLevel().getEntity(entityId);

		if (!(entity instanceof ItemEntity itemEntity)
				|| itemEntity.isRemoved()
				|| !itemEntity.isAlive()
				|| player.distanceToSqr(itemEntity) > PickupConstants.DEFAULT_PICKUP_RADIUS_SQUARED
				|| itemEntity.hasPickUpDelay()) {
			return;
		}

		ItemStack itemStack = itemEntity.getItem();

		if (itemStack.isEmpty() || itemStack.getCount() <= 0 || !hasInventorySpace(player.getInventory(), itemStack)) {
			return;
		}

		int previousCount = itemStack.getCount();
		PickupManager.performManualPickup(player, itemEntity);

		// Inventory.add mutates the ItemStack stored by ItemEntity. When only part of the
		// stack fits, publish a fresh value so SynchedEntityData sends the new count.
		if (!itemEntity.isRemoved() && !itemStack.isEmpty() && itemStack.getCount() != previousCount) {
			itemEntity.setItem(itemStack.copy());
		}
	}

	private static boolean hasInventorySpace(Inventory inventory, ItemStack itemStack) {
		return inventory.getFreeSlot() >= 0 || inventory.getSlotWithRemainingSpace(itemStack) >= 0;
	}
}
