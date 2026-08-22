package com.shouyun.tacticalpickup.client.loot;

import com.shouyun.tacticalpickup.client.pickup.PickupQuantityState;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import java.util.List;

public final class LootSelectionState {
	private final PickupQuantityState quantityState = new PickupQuantityState();
	private LootGroupKey selectedKey;

	public void select(LootGroup group) {
		if (group == null) {
			clear();
			return;
		}

		if (!group.key().equals(selectedKey)) {
			selectedKey = group.key();
			quantityState.reset();
		}
		quantityState.reconcile(group.totalCount());
	}

	public LootGroup reconcile(List<LootGroup> visibleGroups) {
		if (selectedKey == null) {
			return null;
		}

		for (LootGroup group : visibleGroups) {
			if (selectedKey.equals(group.key())) {
				quantityState.reconcile(group.totalCount());
				return group;
			}
		}

		clear();
		return null;
	}

	public void adjust(int steps, int amountPerStep, int totalCount) {
		quantityState.adjust(steps, amountPerStep, totalCount);
	}

	public boolean pickupAll() {
		return quantityState.pickupAll();
	}

	public int selectedAmount(int totalCount) {
		return quantityState.selectedAmount(totalCount);
	}

	public int requestedAmount() {
		return quantityState.requestedAmount();
	}

	public void resetAmount() {
		quantityState.reset();
	}

	public LootGroupKey selectedKey() {
		return selectedKey;
	}

	public void clear() {
		selectedKey = null;
		quantityState.reset();
	}
}
