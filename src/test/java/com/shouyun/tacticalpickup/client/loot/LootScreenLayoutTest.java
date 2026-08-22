package com.shouyun.tacticalpickup.client.loot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shouyun.tacticalpickup.client.loot.LootScreenLayout.Bounds;
import org.junit.jupiter.api.Test;

class LootScreenLayoutTest {
	@Test
	void largeLayoutHonorsMaximumAndUsesMultipleColumns() {
		LootScreenLayout layout = LootScreenLayout.calculate(1920, 1080);

		assertTrue(layout.panel().width() <= 960);
		assertTrue(layout.panel().height() <= 620);
		assertTrue(layout.columns() >= 2 && layout.columns() <= 4);
		assertInside(layout.panel(), layout.lootPanel());
		assertInside(layout.panel(), layout.inventoryPanel());
		assertInside(layout.panel(), layout.detailPanel());
		assertFalse(layout.lootPanel().intersects(layout.inventoryPanel()));
		assertFalse(layout.lootPanel().intersects(layout.detailPanel()));
	}

	@Test
	void compactLayoutKeepsEveryRegionOnScreen() {
		LootScreenLayout layout = LootScreenLayout.calculate(320, 240);
		Bounds screen = new Bounds(0, 0, 320, 240);

		assertInside(screen, layout.panel());
		assertInside(layout.panel(), layout.searchBox());
		assertInside(layout.panel(), layout.lootPanel());
		assertInside(layout.panel(), layout.inventoryPanel());
		assertInside(layout.panel(), layout.detailPanel());
		assertTrue(layout.columns() >= 1 && layout.columns() <= 4);
	}

	@Test
	void scrollAndDropZoneMathClampSafely() {
		LootScreenLayout layout = LootScreenLayout.calculate(640, 360);
		double maximum = layout.maxScroll(50);

		assertTrue(maximum > 0.0D);
		assertTrue(layout.clampScroll(-50.0D, 50) == 0.0D);
		assertTrue(layout.clampScroll(maximum + 50.0D, 50) == maximum);
		assertTrue(layout.inventoryPanel().contains(
			layout.inventoryPanel().x() + 1,
			layout.inventoryPanel().y() + 1
		));
		assertFalse(layout.inventoryPanel().contains(
			layout.inventoryPanel().right(),
			layout.inventoryPanel().bottom()
		));
	}

	private static void assertInside(Bounds outer, Bounds inner) {
		assertTrue(inner.x() >= outer.x());
		assertTrue(inner.y() >= outer.y());
		assertTrue(inner.right() <= outer.right());
		assertTrue(inner.bottom() <= outer.bottom());
	}
}
