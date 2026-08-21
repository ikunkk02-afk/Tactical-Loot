package com.shouyun.tacticalpickup.pickup;

import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public final class PickupManager {
	private static final ThreadLocal<ManualPickupAuthorization> AUTHORIZATION = new ThreadLocal<>();

	private PickupManager() {
	}

	public static boolean consumeManualPickupAuthorization(ItemEntity itemEntity, Player player) {
		ManualPickupAuthorization authorization = AUTHORIZATION.get();

		if (authorization == null
				|| authorization.entityId() != itemEntity.getId()
				|| !authorization.playerId().equals(player.getUUID())) {
			return false;
		}

		AUTHORIZATION.remove();
		return true;
	}

	public static void performManualPickup(Player player, ItemEntity itemEntity) {
		if (AUTHORIZATION.get() != null) {
			throw new IllegalStateException("Nested manual pickup authorization");
		}

		AUTHORIZATION.set(new ManualPickupAuthorization(player.getUUID(), itemEntity.getId()));

		try {
			itemEntity.playerTouch(player);
		} finally {
			AUTHORIZATION.remove();
		}
	}

	private record ManualPickupAuthorization(UUID playerId, int entityId) {
	}
}
