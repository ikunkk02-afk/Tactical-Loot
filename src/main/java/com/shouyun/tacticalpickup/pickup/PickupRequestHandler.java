package com.shouyun.tacticalpickup.pickup;

import com.shouyun.tacticalpickup.mixin.ItemEntityAccessor;
import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;

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

		if (player == null || player.isRemoved() || !player.isAlive() || player.isSpectator()) {
			return;
		}

		Entity entity = player.serverLevel().getEntity(entityId);

		if (!(entity instanceof ItemEntity referenceEntity)
				|| !isEligibleGroupMember(player, referenceEntity, null)
				|| referenceEntity.hasPickUpDelay()) {
			return;
		}

		LootGroupKey groupKey = LootGroupKey.of(referenceEntity.getItem());
		List<ItemEntity> members = new ArrayList<>(PickupConstants.MAX_SCANNED_ENTITIES);

		player.serverLevel().getEntities(
			EntityTypeTest.forClass(ItemEntity.class),
			player.getBoundingBox().inflate(PickupConstants.DEFAULT_PICKUP_RADIUS),
			itemEntity -> isEligibleGroupMember(player, itemEntity, groupKey),
			members,
			PickupConstants.MAX_SCANNED_ENTITIES
		);

		members.sort(Comparator
			.comparingDouble((ItemEntity itemEntity) -> player.distanceToSqr(itemEntity))
			.thenComparingInt(Entity::getId));

		boolean pickupAll = requestedAmount == PickupRequestPayload.ALL_ITEMS;
		int remainingRequested = requestedAmount;

		for (ItemEntity member : members) {
			if (!pickupAll && remainingRequested <= 0) {
				break;
			}

			// Revalidate each entity independently immediately before its transaction.
			if (!isEligibleGroupMember(player, member, groupKey) || member.hasPickUpDelay()) {
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

	private static boolean isEligibleGroupMember(ServerPlayer player, ItemEntity itemEntity, LootGroupKey groupKey) {
		if (itemEntity.isRemoved()
				|| !itemEntity.isAlive()
				|| player.distanceToSqr(itemEntity) > PickupConstants.DEFAULT_PICKUP_RADIUS_SQUARED) {
			return false;
		}

		ItemStack stack = itemEntity.getItem();
		UUID target = ((ItemEntityAccessor) itemEntity).tacticalPickup$getTarget();
		return !stack.isEmpty()
			&& stack.getCount() > 0
			&& (target == null || target.equals(player.getUUID()))
			&& (groupKey == null || groupKey.matches(stack));
	}
}
