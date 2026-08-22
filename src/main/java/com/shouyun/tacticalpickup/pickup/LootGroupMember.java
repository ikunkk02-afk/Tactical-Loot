package com.shouyun.tacticalpickup.pickup;

import net.minecraft.world.item.ItemStack;

/**
 * A scan snapshot. It intentionally contains no live ItemEntity reference.
 */
public record LootGroupMember(int entityId, ItemStack itemStack, double distanceSquared) {
	public LootGroupMember {
		if (itemStack == null || itemStack.isEmpty()) {
			throw new IllegalArgumentException("A loot group member must have a non-empty stack");
		}

		itemStack = itemStack.copy();
	}
}
