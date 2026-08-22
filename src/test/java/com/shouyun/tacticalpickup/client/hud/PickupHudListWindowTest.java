package com.shouyun.tacticalpickup.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PickupHudListWindowTest {
	@Test
	void displaysEveryGroupUntilTheSixEntryLimit() {
		PickupHudListWindow window = PickupHudListWindow.calculate(5, 2, 6);

		assertEquals(0, window.firstIndex());
		assertEquals(5, window.visibleCount());
		assertEquals(0, window.hiddenCount());
	}

	@Test
	void limitsTheListAndReportsTheHiddenGroupCount() {
		PickupHudListWindow window = PickupHudListWindow.calculate(9, 0, 6);

		assertEquals(0, window.firstIndex());
		assertEquals(6, window.visibleCount());
		assertEquals(3, window.hiddenCount());
	}

	@Test
	void keepsALateSelectionInsideTheVisibleWindow() {
		PickupHudListWindow window = PickupHudListWindow.calculate(9, 8, 6);

		assertEquals(3, window.firstIndex());
		assertEquals(6, window.visibleCount());
		assertEquals(3, window.hiddenCount());
	}

	@Test
	void handlesAnEmptyList() {
		PickupHudListWindow window = PickupHudListWindow.calculate(0, 0, 6);

		assertEquals(0, window.firstIndex());
		assertEquals(0, window.visibleCount());
		assertEquals(0, window.hiddenCount());
	}
}
