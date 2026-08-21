package com.shouyun.tacticalpickup.pickup;

import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public final class PickupManager {
	private static final ThreadLocal<ManualPickupAuthorization> AUTHORIZATION = new ThreadLocal<>();

	private PickupManager() {
	}

	public static boolean isManualPickupAuthorized(ItemEntity itemEntity, Player player) {
		ManualPickupAuthorization authorization = AUTHORIZATION.get();

		return authorization != null
				&& authorization.entityId() == itemEntity.getId()
				&& authorization.playerId().equals(player.getUUID());
	}

	public static int getAuthorizedPickupAmount(ItemEntity itemEntity, int vanillaAmount) {
		ManualPickupAuthorization authorization = AUTHORIZATION.get();

		if (authorization == null || authorization.entityId() != itemEntity.getId()) {
			return vanillaAmount;
		}

		int insertedCount = authorization.originalCount() - itemEntity.getItem().getCount();
		return insertedCount > 0 ? insertedCount : vanillaAmount;
	}

	public static void performManualPickup(Player player, ItemEntity itemEntity) {
		if (AUTHORIZATION.get() != null) {
			throw new IllegalStateException("Nested manual pickup authorization");
		}

		AUTHORIZATION.set(new ManualPickupAuthorization(
				player.getUUID(),
				itemEntity.getId(),
				itemEntity.getItem().getCount()
		));

		try {
			itemEntity.playerTouch(player);
		} finally {
			AUTHORIZATION.remove();
		}
	}

	private record ManualPickupAuthorization(UUID playerId, int entityId, int originalCount) {
	}
}
