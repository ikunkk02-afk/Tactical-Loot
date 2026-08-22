package com.shouyun.tacticalpickup.client.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shouyun.tacticalpickup.client.loot.LootScreenLayout.Bounds;
import org.junit.jupiter.api.Test;

class LootScreenLayoutTest {
	@Test
	void largeLayoutUsesNaturalSizeAndPlacesInventoryOnTheLeft() {
		LootScreenLayout layout = LootScreenLayout.calculate(1920, 1080);

		assertEquals(420, layout.panel().width());
		assertEquals(240, layout.panel().height());
		assertFalse(layout.stacked());
		assertTrue(layout.inventoryPanel().x() < layout.lootPanel().x());
		assertEquals(layout.inventoryPanel().x(), layout.detailPanel().x());
		assertTrue(layout.detailPanel().y() > layout.inventoryPanel().y());
		assertTrue(layout.detailPanel().width() > layout.lootPanel().width());
		assertTrue(layout.columns() >= 4 && layout.columns() <= 6);
		assertInside(layout.panel(), layout.inventoryPanel());
		assertInside(layout.panel(), layout.lootPanel());
		assertInside(layout.panel(), layout.detailPanel());
	}

	@Test
	void inventoryUsesVanillaMainThenHotbarMapping() {
		LootScreenLayout layout = LootScreenLayout.calculate(640, 400);
		Bounds slot9 = layout.inventorySlotBounds(9);
		Bounds slot17 = layout.inventorySlotBounds(17);
		Bounds slot35 = layout.inventorySlotBounds(35);
		Bounds hotbar0 = layout.inventorySlotBounds(0);

		assertEquals(slot9.y(), slot17.y());
		assertEquals(slot9.x() + 8 * LootScreenLayout.SLOT_SIZE, slot17.x());
		assertEquals(slot9.y() + 2 * LootScreenLayout.SLOT_SIZE, slot35.y());
		assertEquals(slot35.y() + LootScreenLayout.SLOT_SIZE + 4, hotbar0.y());
		for (int slot = 0; slot < 36; slot++) {
			Bounds bounds = layout.inventorySlotBounds(slot);
			assertEquals(slot, layout.inventorySlotAt(bounds.x() + 9, bounds.y() + 9).orElseThrow());
		}
	}

	@Test
	void compactLayoutsStayOnScreenWithoutOverlap() {
		for (int[] size : new int[][]{{320, 240}, {480, 270}}) {
			LootScreenLayout layout = LootScreenLayout.calculate(size[0], size[1]);
			Bounds screen = new Bounds(0, 0, size[0], size[1]);

			assertInside(screen, layout.panel());
			assertInside(layout.panel(), layout.searchBox());
			assertInside(layout.panel(), layout.inventoryPanel());
			assertInside(layout.panel(), layout.lootPanel());
			assertInside(layout.panel(), layout.detailPanel());
			assertFalse(layout.inventoryPanel().intersects(layout.lootPanel()));
			assertFalse(layout.lootPanel().intersects(layout.detailPanel()));
			assertTrue(layout.columns() >= 4 && layout.columns() <= 6);
		}
	}

	@Test
	void scrollMathUsesSlotRowsAndClamps() {
		LootScreenLayout layout = LootScreenLayout.calculate(640, 400);
		double maximum = layout.maxScroll(200);

		assertTrue(maximum > 0.0D);
		assertEquals(0.0D, layout.clampScroll(-50.0D, 200));
		assertEquals(maximum, layout.clampScroll(maximum + 50.0D, 200));
		assertEquals(LootScreenLayout.LOOT_CELL_SIZE, layout.lootSlotBounds(layout.columns(), 0).y() - layout.lootSlotBounds(0, 0).y());
	}

	private static void assertInside(Bounds outer, Bounds inner) {
		assertTrue(inner.x() >= outer.x());
		assertTrue(inner.y() >= outer.y());
		assertTrue(inner.right() <= outer.right());
		assertTrue(inner.bottom() <= outer.bottom());
	}
}
