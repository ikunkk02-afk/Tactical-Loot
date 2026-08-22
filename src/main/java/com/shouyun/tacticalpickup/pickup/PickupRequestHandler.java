package com.shouyun.tacticalpickup.pickup;

import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

public final class PickupRequestHandler {
	private PickupRequestHandler() {
	}

	public static void handle(ServerPlayer player, int entityId) {
		handle(player, entityId, PickupRequestPayload.ALL_ITEMS);
	}

	public static void handle(ServerPlayer player, int entityId, int requestedAmount) {
		if (requestedAmount < PickupRequestPayload.ALL_ITEMS) {
			return;
		}

		ServerLootGroupResolver.ResolvedGroup group = ServerLootGroupResolver.resolve(player, entityId);
		if (group == null) {
			return;
		}

		boolean pickupAll = requestedAmount == PickupRequestPayload.ALL_ITEMS;
		int remainingRequested = requestedAmount;

		for (ItemEntity member : group.members()) {
			if (!pickupAll && remainingRequested <= 0) {
				break;
			}

			// Revalidate each entity independently immediately before its transaction.
			if (!ServerLootGroupResolver.isEligibleGroupMember(player, member, group.key()) || member.hasPickUpDelay()) {
				continue;
			}

			int insertedCount = pickupAll
				? SingleEntityPickupTransaction.tryPickupSingleEntity(player, member)
				: SingleEntityPickupTransaction.tryPickupSingleEntity(player, member, remainingRequested);

			if (insertedCount == 0) {
				// Remaining members have the same Item + Components, so the inventory
				// cannot accept any useful amount from them either.
				break;
			}

			if (!pickupAll) {
				remainingRequested -= insertedCount;
			}
		}
	}

}
