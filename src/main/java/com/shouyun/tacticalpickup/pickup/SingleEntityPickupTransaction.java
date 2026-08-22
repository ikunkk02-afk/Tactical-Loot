package com.shouyun.tacticalpickup.pickup;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Applies the already validated inventory transaction for exactly one ground entity.
 */
public final class SingleEntityPickupTransaction {
	private SingleEntityPickupTransaction() {
	}

	public static int tryPickupSingleEntity(ServerPlayer player, ItemEntity itemEntity) {
		ItemStack groundStack = itemEntity.getItem();
		return tryPickupSingleEntity(player, itemEntity, groundStack.getCount());
	}

	public static int tryPickupSingleEntity(ServerPlayer player, ItemEntity itemEntity, int maxAmount) {
		ItemStack groundStack = itemEntity.getItem();

		if (groundStack.isEmpty() || groundStack.getCount() <= 0 || maxAmount <= 0) {
			return 0;
		}

		int originalGroundCount = groundStack.getCount();
		int attemptCount = Math.min(originalGroundCount, maxAmount);
		ItemStack insertionStack = groundStack.copyWithCount(attemptCount);
		Inventory inventory = player.getInventory();
		int matchingCountBefore = countMatching(inventory, groundStack);

		inventory.add(insertionStack);

		if (player.hasInfiniteMaterials()) {
			int actualInventoryIncrease = countMatching(inventory, groundStack) - matchingCountBefore;

			// Inventory.add deliberately consumes an uninserted remainder for creative
			// players. Reconstruct the copy's remainder from the real inventory increase.
			if (actualInventoryIncrease >= 0 && actualInventoryIncrease <= attemptCount) {
				insertionStack.setCount(attemptCount - actualInventoryIncrease);
			}
		}

		int insertedCount = attemptCount - insertionStack.getCount();

		if (insertedCount <= 0) {
			return 0;
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
		return insertedCount;
	}

	public static int tryPickupSingleEntityToSlot(
			ServerPlayer player,
			ItemEntity itemEntity,
			int maxAmount,
			int targetSlot
	) {
		if (targetSlot < 0 || targetSlot >= Inventory.INVENTORY_SIZE || maxAmount <= 0) {
			return 0;
		}

		ItemStack groundStack = itemEntity.getItem();
		if (groundStack.isEmpty() || groundStack.getCount() <= 0) {
			return 0;
		}

		Inventory inventory = player.getInventory();
		ItemStack targetStack = inventory.getItem(targetSlot);
		if (!targetStack.isEmpty() && !ItemStack.isSameItemSameComponents(targetStack, groundStack)) {
			return 0;
		}

		int slotLimit = inventory.getMaxStackSize(groundStack);
		int currentCount = targetStack.isEmpty() ? 0 : targetStack.getCount();
		int insertedCount = Math.min(Math.min(maxAmount, groundStack.getCount()), slotLimit - currentCount);
		if (insertedCount <= 0) {
			return 0;
		}

		int originalGroundCount = groundStack.getCount();
		if (targetStack.isEmpty()) {
			inventory.setItem(targetSlot, groundStack.copyWithCount(insertedCount));
		} else {
			targetStack.grow(insertedCount);
			inventory.setItem(targetSlot, targetStack);
		}
		inventory.setChanged();

		player.take(itemEntity, insertedCount);
		int remainingGroundCount = originalGroundCount - insertedCount;
		if (remainingGroundCount == 0) {
			itemEntity.discard();
		} else {
			itemEntity.setItem(groundStack.copyWithCount(remainingGroundCount));
		}

		player.awardStat(Stats.ITEM_PICKED_UP.get(groundStack.getItem()), insertedCount);
		player.onItemPickup(itemEntity);
		return insertedCount;
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
