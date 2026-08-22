package com.shouyun.tacticalpickup.client.pickup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import org.junit.jupiter.api.Test;

class PickupQuantityStateTest {
	@Test
	void allUsesSharedWireValueAndMinusStartsFromTotal() {
		PickupQuantityState state = new PickupQuantityState();

		assertTrue(state.pickupAll());
		assertEquals(PickupRequestPayload.ALL_ITEMS, state.requestedAmount());

		state.adjust(-1, 1, 64);
		assertFalse(state.pickupAll());
		assertEquals(63, state.selectedAmount(64));
		assertEquals(63, state.requestedAmount());
	}

	@Test
	void adjustmentClampsAtOneAndReturnsToAllAtTotal() {
		PickupQuantityState state = new PickupQuantityState();
		state.adjust(-100, 1, 64);
		assertEquals(1, state.selectedAmount(64));

		state.adjust(4, 16, 64);
		assertTrue(state.pickupAll());
		assertEquals(PickupRequestPayload.ALL_ITEMS, state.requestedAmount());
	}

	@Test
	void reconcilePreservesValidAmountAndUsesAllWhenTotalDrops() {
		PickupQuantityState state = new PickupQuantityState();
		state.adjust(-32, 1, 128);
		state.reconcile(100);
		assertEquals(96, state.selectedAmount(100));
		assertFalse(state.pickupAll());

		state.reconcile(90);
		assertTrue(state.pickupAll());
		assertEquals(90, state.selectedAmount(90));
	}
}
