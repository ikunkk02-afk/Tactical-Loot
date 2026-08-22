package com.shouyun.tacticalpickup.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PickupHudPositionTest {
	@Test
	void defaultPositionMatchesTheExistingRightOfCrosshairLayout() {
		PickupHudPosition position = PickupHudPosition.defaults(640, 360, 180, 120);

		assertEquals(344, position.x());
		assertEquals(120, position.y());
	}

	@Test
	void defaultPositionFallsBackToTheLeftWhenTheRightSideDoesNotFit() {
		PickupHudPosition position = PickupHudPosition.defaults(320, 240, 150, 100);

		assertEquals(8, position.x());
		assertEquals(70, position.y());
	}
}
