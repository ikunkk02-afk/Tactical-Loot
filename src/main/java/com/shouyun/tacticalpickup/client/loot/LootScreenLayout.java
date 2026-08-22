package com.shouyun.tacticalpickup.client.loot;

import java.util.OptionalInt;

public record LootScreenLayout(
	Bounds panel,
	Bounds header,
	Bounds closeButton,
	Bounds searchBox,
	Bounds inventoryPanel,
	Bounds lootPanel,
	Bounds lootViewport,
	Bounds detailPanel,
	Bounds detailTextPanel,
	Bounds actionPanel,
	boolean stacked,
	int columns
) {
	public static final int SLOT_SIZE = 18;
	public static final int LOOT_CELL_SIZE = 24;
	public static final int LOOT_SLOT_SIZE = 22;
	public static final int NATURAL_WIDTH = 420;
	public static final int NATURAL_HEIGHT = 240;
	private static final int SCREEN_MARGIN = 6;
	private static final int OUTER_PADDING = 7;
	private static final int HEADER_HEIGHT = 22;
	private static final int PANEL_GAP = 6;
	private static final int INVENTORY_GRID_WIDTH = SLOT_SIZE * 9;
	private static final int NORMAL_TOP_HEIGHT = 95;
	private static final int NORMAL_INVENTORY_WIDTH = 176;
	private static final int MIN_COLUMNS = 4;
	private static final int MAX_COLUMNS = 6;

	public static LootScreenLayout calculate(int screenWidth, int screenHeight) {
		int panelWidth = Math.max(1, Math.min(NATURAL_WIDTH, screenWidth - SCREEN_MARGIN * 2));
		int panelHeight = Math.max(1, Math.min(NATURAL_HEIGHT, screenHeight - SCREEN_MARGIN * 2));
		int panelX = (screenWidth - panelWidth) / 2;
		int panelY = (screenHeight - panelHeight) / 2;
		Bounds panel = new Bounds(panelX, panelY, panelWidth, panelHeight);
		Bounds header = new Bounds(panelX + 4, panelY + 3, Math.max(1, panelWidth - 8), HEADER_HEIGHT - 3);
		Bounds closeButton = new Bounds(panel.right() - 18, panel.y() + 5, 12, 12);

		int contentX = panelX + OUTER_PADDING;
		int contentY = panelY + HEADER_HEIGHT;
		int contentWidth = Math.max(1, panelWidth - OUTER_PADDING * 2);
		int contentHeight = Math.max(1, panelHeight - HEADER_HEIGHT - OUTER_PADDING);
		boolean stacked = panelWidth < 360;

		Bounds inventoryPanel;
		Bounds lootPanel;
		Bounds detailPanel;
		Bounds detailTextPanel;
		Bounds actionPanel;
		if (stacked) {
			int gap = Math.min(4, PANEL_GAP);
			int inventoryHeight = Math.min(95, Math.max(76, contentHeight / 2));
			int remaining = Math.max(2, contentHeight - inventoryHeight - gap * 2);
			int lootHeight = Math.max(24, remaining / 2);
			int detailHeight = Math.max(1, remaining - lootHeight);
			inventoryPanel = new Bounds(contentX, contentY, contentWidth, inventoryHeight);
			lootPanel = new Bounds(contentX, inventoryPanel.bottom() + gap, contentWidth, lootHeight);
			detailPanel = new Bounds(contentX, lootPanel.bottom() + gap, contentWidth, detailHeight);
			detailTextPanel = detailPanel;
			actionPanel = detailPanel;
		} else {
			int topHeight = Math.min(NORMAL_TOP_HEIGHT, Math.max(1, contentHeight - PANEL_GAP - 58));
			int detailHeight = Math.max(1, contentHeight - topHeight - PANEL_GAP);
			int inventoryWidth = Math.min(NORMAL_INVENTORY_WIDTH, Math.max(1, contentWidth - PANEL_GAP - 120));
			int lootWidth = Math.max(1, contentWidth - inventoryWidth - PANEL_GAP);
			inventoryPanel = new Bounds(contentX, contentY, inventoryWidth, topHeight);
			lootPanel = new Bounds(inventoryPanel.right() + PANEL_GAP, contentY, lootWidth, topHeight);
			detailPanel = new Bounds(contentX, inventoryPanel.bottom() + PANEL_GAP, contentWidth, detailHeight);
			int actionWidth = Math.min(172, Math.max(128, detailPanel.width() * 2 / 5));
			actionPanel = new Bounds(
				detailPanel.right() - actionWidth - 4,
				detailPanel.y() + 4,
				actionWidth,
				Math.max(1, detailPanel.height() - 8)
			);
			detailTextPanel = new Bounds(
				detailPanel.x() + 4,
				detailPanel.y() + 4,
				Math.max(1, actionPanel.x() - detailPanel.x() - 8),
				Math.max(1, detailPanel.height() - 8)
			);
		}

		int searchWidth = Math.min(92, Math.max(56, lootPanel.width() / 2));
		Bounds searchBox = new Bounds(
			lootPanel.right() - searchWidth - 5,
			lootPanel.y() + 4,
			searchWidth,
			14
		);

		int availableGridWidth = Math.max(1, lootPanel.width() - 12);
		int availableColumns = Math.max(1, availableGridWidth / LOOT_CELL_SIZE);
		int columns = availableColumns >= MIN_COLUMNS
			? clamp(availableColumns, MIN_COLUMNS, MAX_COLUMNS)
			: availableColumns;
		int gridWidth = Math.max(1, columns * LOOT_CELL_SIZE);
		Bounds lootViewport = new Bounds(
			lootPanel.x() + 6,
			lootPanel.y() + Math.min(21, Math.max(0, lootPanel.height() - 1)),
			Math.min(gridWidth, Math.max(1, lootPanel.width() - 10)),
			Math.max(1, lootPanel.height() - 25)
		);

		return new LootScreenLayout(
			panel,
			header,
			closeButton,
			searchBox,
			inventoryPanel,
			lootPanel,
			lootViewport,
			detailPanel,
			detailTextPanel,
			actionPanel,
			stacked,
			columns
		);
	}

	public Bounds lootSlotBounds(int index, double scrollOffset) {
		int column = Math.floorMod(index, columns);
		int row = index / columns;
		return new Bounds(
			lootViewport.x() + column * LOOT_CELL_SIZE,
			lootViewport.y() + row * LOOT_CELL_SIZE - (int) Math.round(scrollOffset),
			LOOT_SLOT_SIZE,
			LOOT_SLOT_SIZE
		);
	}

	public Bounds inventorySlotBounds(int inventorySlot) {
		if (inventorySlot < 0 || inventorySlot >= 36) {
			return Bounds.EMPTY;
		}

		int gridX = inventoryPanel.x() + Math.max(0, (inventoryPanel.width() - INVENTORY_GRID_WIDTH) / 2);
		int gridY = inventoryPanel.y() + 15;
		if (inventorySlot < 9) {
			return new Bounds(gridX + inventorySlot * SLOT_SIZE, gridY + SLOT_SIZE * 3 + 4, SLOT_SIZE, SLOT_SIZE);
		}

		int mainIndex = inventorySlot - 9;
		return new Bounds(
			gridX + Math.floorMod(mainIndex, 9) * SLOT_SIZE,
			gridY + mainIndex / 9 * SLOT_SIZE,
			SLOT_SIZE,
			SLOT_SIZE
		);
	}

	public OptionalInt inventorySlotAt(double mouseX, double mouseY) {
		if (!inventoryPanel.contains(mouseX, mouseY)) {
			return OptionalInt.empty();
		}

		for (int slot = 0; slot < 36; slot++) {
			if (inventorySlotBounds(slot).contains(mouseX, mouseY)) {
				return OptionalInt.of(slot);
			}
		}
		return OptionalInt.empty();
	}

	public double maxScroll(int itemCount) {
		int rows = itemCount <= 0 ? 0 : (itemCount + columns - 1) / columns;
		return Math.max(0, rows * LOOT_CELL_SIZE - lootViewport.height());
	}

	public double clampScroll(double scrollOffset, int itemCount) {
		return Math.max(0.0D, Math.min(scrollOffset, maxScroll(itemCount)));
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(value, maximum));
	}

	public record Bounds(int x, int y, int width, int height) {
		public static final Bounds EMPTY = new Bounds(0, 0, 0, 0);

		public int right() {
			return x + width;
		}

		public int bottom() {
			return y + height;
		}

		public Bounds offset(int offsetX, int offsetY) {
			return offsetX == 0 && offsetY == 0
				? this
				: new Bounds(x + offsetX, y + offsetY, width, height);
		}

		public boolean contains(double pointX, double pointY) {
			return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
		}

		public boolean intersects(Bounds other) {
			return right() > other.x && x < other.right() && bottom() > other.y && y < other.bottom();
		}
	}
}
