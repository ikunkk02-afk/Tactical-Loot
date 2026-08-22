package com.shouyun.tacticalpickup.pickup;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/**
 * Immutable identity for a loot group. Stack count is deliberately excluded.
 */
public final class LootGroupKey {
	private final ItemStack identityStack;
	private final int hashCode;

	private LootGroupKey(ItemStack stack) {
		if (stack.isEmpty()) {
			throw new IllegalArgumentException("A loot group cannot use an empty stack");
		}

		this.identityStack = stack.copyWithCount(1);
		this.hashCode = Objects.hash(this.identityStack.getItem(), this.identityStack.getTag());
	}

	public static LootGroupKey of(ItemStack stack) {
		return new LootGroupKey(Objects.requireNonNull(stack, "stack"));
	}

	public boolean matches(ItemStack stack) {
		return stack != null && ItemStack.isSameItemSameTags(identityStack, stack);
	}

	@Override
	public boolean equals(Object object) {
		return this == object
			|| object instanceof LootGroupKey other
				&& ItemStack.isSameItemSameTags(identityStack, other.identityStack);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
