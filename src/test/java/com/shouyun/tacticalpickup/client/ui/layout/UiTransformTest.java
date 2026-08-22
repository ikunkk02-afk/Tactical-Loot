package com.shouyun.tacticalpickup.client.ui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shouyun.tacticalpickup.client.ui.layout.UiTransform.UiPoint;
import org.junit.jupiter.api.Test;

class UiTransformTest {
	private static final double EPSILON = 0.0001D;

	@Test
	void screenAndLocalCoordinatesRoundTripAtNonDefaultScale() {
		UiRect localBounds = new UiRect(110.0D, 60.0D, 420.0D, 240.0D);
		UiTransform transform = UiTransform.create(localBounds, 510.0D, 270.0D, 1.25F, 1280, 720);

		UiPoint screen = transform.localToScreen(222.0D, 144.0D);
		UiPoint local = transform.screenToLocal(screen.x(), screen.y());

		assertEquals(222.0D, local.x(), EPSILON);
		assertEquals(144.0D, local.y(), EPSILON);
	}

	@Test
	void scalingKeepsTheRequestedCenterFixed() {
		UiRect localBounds = new UiRect(0.0D, 0.0D, 160.0D, 90.0D);
		UiTransform small = UiTransform.create(localBounds, 400.0D, 250.0D, 0.75F, 1000, 600);
		UiTransform large = UiTransform.create(localBounds, 400.0D, 250.0D, 1.50F, 1000, 600);

		assertEquals(small.centerX(), large.centerX(), EPSILON);
		assertEquals(small.centerY(), large.centerY(), EPSILON);
	}

	@Test
	void transformedBoundsAreClampedInsideEveryScreenEdge() {
		UiRect localBounds = new UiRect(0.0D, 0.0D, 190.0D, 150.0D);
		for (double[] center : new double[][]{{-100.0D, -100.0D}, {2000.0D, -100.0D}, {-100.0D, 2000.0D}, {2000.0D, 2000.0D}}) {
			UiTransform transform = UiTransform.create(localBounds, center[0], center[1], 1.6F, 640, 360);
			UiRect screenBounds = transform.screenBounds();
			assertTrue(screenBounds.x() >= UiTransform.SCREEN_MARGIN - EPSILON);
			assertTrue(screenBounds.y() >= UiTransform.SCREEN_MARGIN - EPSILON);
			assertTrue(screenBounds.x() + screenBounds.width() <= 640 - UiTransform.SCREEN_MARGIN + EPSILON);
			assertTrue(screenBounds.y() + screenBounds.height() <= 360 - UiTransform.SCREEN_MARGIN + EPSILON);
		}
	}

	@Test
	void oversizedScaleIsReducedOnlyEnoughToFitTheScreen() {
		UiRect localBounds = new UiRect(0.0D, 0.0D, 420.0D, 240.0D);
		UiTransform transform = UiTransform.create(localBounds, 160.0D, 120.0D, 1.35F, 320, 240);
		UiRect screenBounds = transform.screenBounds();

		assertTrue(transform.scale() < 1.0F);
		assertTrue(screenBounds.width() <= 304.0D + EPSILON);
		assertTrue(screenBounds.height() <= 224.0D + EPSILON);
	}
}
