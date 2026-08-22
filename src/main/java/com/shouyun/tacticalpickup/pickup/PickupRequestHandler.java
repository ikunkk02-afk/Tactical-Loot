package com.shouyun.tacticalpickup.pickup;

import com.shouyun.tacticalpickup.mixin.ItemEntityAccessor;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PickupRequestHandler {
	private PickupRequestHandler() {
	}

	public static void handle(ServerPlayer player, int entityId) {
		if (player == null || player.isRemoved() || !player.isAlive() || player.isSpectator()) {
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

		ItemStack groundStack = itemEntity.getItem();
		UUID target = ((ItemEntityAccessor) itemEntity).tacticalPickup$getTarget();

		if (groundStack.isEmpty()
				|| groundStack.getCount() <= 0
				|| (target != null && !target.equals(player.getUUID()))) {
			return;
		}

		int originalGroundCount = groundStack.getCount();
		ItemStack insertionStack = groundStack.copy();
		Inventory inventory = player.getInventory();
		int matchingCountBefore = countMatching(inventory, groundStack);

		inventory.add(insertionStack);

		if (player.hasInfiniteMaterials()) {
			int actualInventoryIncrease = countMatching(inventory, groundStack) - matchingCountBefore;

			// Inventory.add deliberately consumes an uninserted remainder for creative
			// players. Restore the copy's remainder from the inventory's real increase so
			// the ground entity is never reduced for items the inventory did not receive.
			if (actualInventoryIncrease >= 0 && actualInventoryIncrease <= originalGroundCount) {
				insertionStack.setCount(originalGroundCount - actualInventoryIncrease);
			}
		}

		int insertedCount = originalGroundCount - insertionStack.getCount();

		if (insertedCount <= 0) {
			return;
		}

		player.take(itemEntity, insertedCount);

		int remainingGroundCount = originalGroundCount - insertedCount;

		if (remainingGroundCount == 0) {
			itemEntity.discard();
		} else {
			itemEntity.setItem(groundStack.copyWithCount(remainingGroundCount));
		}

		player.awardStat(Stats.ITEM_PICKED_UP.get(groundStack.getItem()), insertedCount);
		player.onItemPickup(itemEntity);
	}

	private static int countMatching(Inventory inventory, ItemStack referenceStack) {
		int count = 0;

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack inventoryStack = inventory.getItem(slot);

			if (ItemStack.isSameItemSameComponents(inventoryStack, referenceStack)) {
				count += inventoryStack.getCount();
			}
		}

		return count;
	}
}
