package com.shouyun.tacticalpickup.client.pickup;

import com.shouyun.tacticalpickup.network.PickupRequestPayload;

/**
 * Shared client-side quantity semantics for HUD and screen pickup requests.
 */
public final class PickupQuantityState {
	private boolean pickupAll = true;
	private int selectedAmount = 1;

	public boolean pickupAll() {
		return pickupAll;
	}

	public int selectedAmount(int totalCount) {
		return pickupAll ? Math.max(1, totalCount) : selectedAmount;
	}

	public int requestedAmount() {
		return pickupAll ? PickupRequestPayload.ALL_ITEMS : selectedAmount;
	}

	public void adjust(int steps, int amountPerStep, int totalCount) {
		if (steps == 0 || amountPerStep <= 0 || totalCount <= 0) {
			return;
		}

		int currentAmount = pickupAll ? totalCount : selectedAmount;
		long adjustedAmount = currentAmount + (long) steps * amountPerStep;

		if (adjustedAmount >= totalCount) {
			reset();
		} else {
			pickupAll = false;
			selectedAmount = (int) Math.max(1L, adjustedAmount);
		}
	}

	public void reconcile(int totalCount) {
		if (totalCount <= 0 || pickupAll || selectedAmount >= totalCount) {
			reset();
		} else {
			selectedAmount = Math.max(1, selectedAmount);
		}
	}

	public void reset() {
		pickupAll = true;
		selectedAmount = 1;
	}
}
