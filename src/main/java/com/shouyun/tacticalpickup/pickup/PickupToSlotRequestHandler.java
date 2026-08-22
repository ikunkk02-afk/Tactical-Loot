package com.shouyun.tacticalpickup.pickup;

import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PickupToSlotRequestHandler {
	private PickupToSlotRequestHandler() {
	}

	public static void handle(ServerPlayer player, int entityId, int requestedAmount, int targetSlot) {
		if (requestedAmount < PickupRequestPayload.ALL_ITEMS
				|| targetSlot < 0
				|| targetSlot >= Inventory.INVENTORY_SIZE) {
			return;
		}

		ServerLootGroupResolver.ResolvedGroup group = ServerLootGroupResolver.resolve(player, entityId);
		if (group == null || group.members().isEmpty()) {
			return;
		}

		ItemStack referenceStack = group.members().get(0).getItem();
		ItemStack targetStack = player.getInventory().getItem(targetSlot);
		if (!targetStack.isEmpty()
				&& (!ItemStack.isSameItemSameTags(targetStack, referenceStack)
					|| targetStack.getCount() >= Math.min(player.getInventory().getMaxStackSize(), referenceStack.getMaxStackSize()))) {
			return;
		}

		boolean pickupAll = requestedAmount == PickupRequestPayload.ALL_ITEMS;
		int remainingRequested = requestedAmount;
		for (ItemEntity member : group.members()) {
			if (!pickupAll && remainingRequested <= 0) {
				break;
			}

			if (!ServerLootGroupResolver.isEligibleGroupMember(player, member, group.key())
					|| member.hasPickUpDelay()) {
				continue;
			}

			int insertedCount = SingleEntityPickupTransaction.tryPickupSingleEntityToSlot(
				player,
				member,
				pickupAll ? member.getItem().getCount() : remainingRequested,
				targetSlot
			);
			if (insertedCount == 0) {
				break;
			}

			if (!pickupAll) {
				remainingRequested -= insertedCount;
			}
		}
	}
}
