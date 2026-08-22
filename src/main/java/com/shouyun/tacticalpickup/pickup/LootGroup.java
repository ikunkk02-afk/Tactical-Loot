package com.shouyun.tacticalpickup.pickup;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public record LootGroup(
	LootGroupKey key,
	ItemStack displayStack,
	List<Integer> entityIds,
	int totalCount,
	double nearestDistanceSquared,
	int representativeEntityId
) {
	public LootGroup {
		if (key == null || displayStack == null || displayStack.isEmpty()) {
			throw new IllegalArgumentException("A loot group must have an identity and display stack");
		}

		displayStack = displayStack.copy();
		entityIds = List.copyOf(entityIds);
	}
}
