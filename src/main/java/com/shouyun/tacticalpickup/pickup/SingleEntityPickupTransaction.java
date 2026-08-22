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

		if (groundStack.isEmpty() || groundStack.getCount() <= 0) {
			return 0;
		}

		int originalGroundCount = groundStack.getCount();
		ItemStack insertionStack = groundStack.copy();
		Inventory inventory = player.getInventory();
		int matchingCountBefore = countMatching(inventory, groundStack);

		inventory.add(insertionStack);

		if (player.hasInfiniteMaterials()) {
			int actualInventoryIncrease = countMatching(inventory, groundStack) - matchingCountBefore;

			// Inventory.add deliberately consumes an uninserted remainder for creative
			// players. Reconstruct the copy's remainder from the real inventory increase.
			if (actualInventoryIncrease >= 0 && actualInventoryIncrease <= originalGroundCount) {
				insertionStack.setCount(originalGroundCount - actualInventoryIncrease);
			}
		}

		int insertedCount = originalGroundCount - insertionStack.getCount();

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
