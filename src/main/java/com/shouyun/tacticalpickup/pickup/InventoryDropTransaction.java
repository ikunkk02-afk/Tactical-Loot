package com.shouyun.tacticalpickup.pickup;

import com.shouyun.tacticalpickup.network.DropInventorySlotPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class InventoryDropTransaction {
	private InventoryDropTransaction() {
	}

	public static int tryDrop(ServerPlayer player, int sourceSlot, int requestedAmount) {
		return tryDrop(player, sourceSlot, requestedAmount, (serverPlayer, stack) ->
			serverPlayer.drop(stack, false, true)
		);
	}

	static int tryDrop(ServerPlayer player, int sourceSlot, int requestedAmount, DropSpawner spawner) {
		if (player == null
				|| player.isRemoved()
				|| !player.isAlive()
				|| player.isSpectator()
				|| sourceSlot < 0
				|| sourceSlot >= Inventory.INVENTORY_SIZE
				|| requestedAmount < DropInventorySlotPayload.ALL_ITEMS) {
			return 0;
		}

		Inventory inventory = player.getInventory();
		ItemStack sourceStack = inventory.getItem(sourceSlot);
		if (sourceStack.isEmpty() || sourceStack.getCount() <= 0) {
			return 0;
		}

		int dropCount = requestedAmount == DropInventorySlotPayload.ALL_ITEMS
			? sourceStack.getCount()
			: Math.min(sourceStack.getCount(), requestedAmount);
		if (dropCount <= 0) {
			return 0;
		}

		ItemStack originalStack = sourceStack.copy();
		int droppedItemStatBefore = player.getStats().getValue(Stats.ITEM_DROPPED.get(sourceStack.getItem()));
		int dropStatBefore = player.getStats().getValue(Stats.CUSTOM.get(Stats.DROP));
		ItemStack droppedStack = sourceStack.split(dropCount);
		inventory.setItem(sourceSlot, sourceStack.isEmpty() ? ItemStack.EMPTY : sourceStack);
		inventory.setChanged();

		ItemEntity droppedEntity = spawner.drop(player, droppedStack);
		boolean spawned = droppedEntity != null
			&& !droppedEntity.isRemoved()
			&& player.serverLevel().getEntity(droppedEntity.getId()) == droppedEntity;
		if (!spawned) {
			if (droppedEntity != null && !droppedEntity.isRemoved()) {
				droppedEntity.discard();
			}
			inventory.setItem(sourceSlot, originalStack);
			inventory.setChanged();
			player.getStats().setValue(player, Stats.ITEM_DROPPED.get(originalStack.getItem()), droppedItemStatBefore);
			player.getStats().setValue(player, Stats.CUSTOM.get(Stats.DROP), dropStatBefore);
			synchronizeInventory(player);
			return 0;
		}

		synchronizeInventory(player);
		return dropCount;
	}

	private static void synchronizeInventory(ServerPlayer player) {
		player.containerMenu.broadcastChanges();
		if (player.inventoryMenu != player.containerMenu) {
			player.inventoryMenu.broadcastChanges();
		}
	}

	@FunctionalInterface
	interface DropSpawner {
		@Nullable
		ItemEntity drop(ServerPlayer player, ItemStack stack);
	}
}
