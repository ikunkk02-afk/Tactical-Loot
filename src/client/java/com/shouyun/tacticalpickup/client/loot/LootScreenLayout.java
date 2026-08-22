package com.shouyun.tacticalpickup.client.loot;

import java.util.OptionalInt;

public record LootScreenLayout(
	Bounds panel,
	Bounds searchBox,
	Bounds inventoryPanel,
	Bounds lootPanel,
	Bounds lootViewport,
	Bounds detailPanel,
	boolean stacked,
	int columns
) {
	public static final int SLOT_SIZE = 18;
	public static final int NATURAL_WIDTH = 420;
	public static final int NATURAL_HEIGHT = 240;
	private static final int SCREEN_MARGIN = 6;
	private static final int OUTER_PADDING = 7;
	private static final int HEADER_HEIGHT = 22;
	private static final int PANEL_GAP = 6;
	private static final int INVENTORY_GRID_WIDTH = SLOT_SIZE * 9;
	private static final int INVENTORY_PANEL_HEIGHT = 99;
	private static final int NORMAL_MIN_COLUMNS = 6;
	private static final int MAX_COLUMNS = 9;

	public static LootScreenLayout calculate(int screenWidth, int screenHeight) {
		int panelWidth = Math.max(1, Math.min(NATURAL_WIDTH, screenWidth - SCREEN_MARGIN * 2));
		int panelHeight = Math.max(1, Math.min(NATURAL_HEIGHT, screenHeight - SCREEN_MARGIN * 2));
		int panelX = (screenWidth - panelWidth) / 2;
		int panelY = (screenHeight - panelHeight) / 2;
		Bounds panel = new Bounds(panelX, panelY, panelWidth, panelHeight);

		int contentX = panelX + Math.min(OUTER_PADDING, Math.max(0, panelWidth - 1));
		int contentY = panelY + Math.min(HEADER_HEIGHT, Math.max(0, panelHeight - 1));
		int contentWidth = Math.max(1, panelWidth - OUTER_PADDING * 2);
		int contentHeight = Math.max(1, panelHeight - HEADER_HEIGHT - OUTER_PADDING);
		boolean stacked = panelWidth < 320;

		Bounds inventoryPanel;
		Bounds lootPanel;
		Bounds detailPanel;
		if (stacked) {
			int inventoryHeight = Math.min(INVENTORY_PANEL_HEIGHT, Math.max(1, contentHeight / 2));
			int remainingHeight = Math.max(1, contentHeight - inventoryHeight - PANEL_GAP * 2);
			int detailHeight = Math.min(54, Math.max(1, remainingHeight / 2));
			int lootHeight = Math.max(1, remainingHeight - detailHeight);
			inventoryPanel = new Bounds(contentX, contentY, contentWidth, inventoryHeight);
			lootPanel = new Bounds(contentX, inventoryPanel.bottom() + PANEL_GAP, contentWidth, lootHeight);
			detailPanel = new Bounds(contentX, lootPanel.bottom() + PANEL_GAP, contentWidth, detailHeight);
		} else {
			int availableWidth = Math.max(1, contentWidth - PANEL_GAP);
			int inventoryWidth = availableWidth / 2;
			int lootWidth = availableWidth - inventoryWidth;
			int detailHeight = Math.min(96, Math.max(74, contentHeight * 2 / 5));
			int lootHeight = Math.max(1, contentHeight - detailHeight - PANEL_GAP);
			inventoryPanel = new Bounds(
				contentX,
				contentY,
				inventoryWidth,
				Math.min(INVENTORY_PANEL_HEIGHT, contentHeight)
			);
			int rightX = contentX + inventoryWidth + PANEL_GAP;
			lootPanel = new Bounds(rightX, contentY, lootWidth, lootHeight);
			detailPanel = new Bounds(rightX, lootPanel.bottom() + PANEL_GAP, lootWidth, detailHeight);
		}

		boolean compactLootHeader = lootPanel.width() < 180;
		int lootHeaderHeight = compactLootHeader ? 40 : 21;
		int searchWidth = compactLootHeader
			? Math.max(1, lootPanel.width() - 8)
			: Math.min(96, Math.max(1, lootPanel.width() / 2));
		Bounds searchBox = compactLootHeader
			? new Bounds(lootPanel.x() + 4, lootPanel.y() + 18, searchWidth, 18)
			: new Bounds(lootPanel.right() - searchWidth - 4, lootPanel.y() + 2, searchWidth, 18);

		int availableGridWidth = Math.max(1, lootPanel.width() - 12);
		int availableColumns = Math.max(1, availableGridWidth / SLOT_SIZE);
		int columns = availableColumns >= NORMAL_MIN_COLUMNS
			? clamp(availableColumns, NORMAL_MIN_COLUMNS, MAX_COLUMNS)
			: Math.min(MAX_COLUMNS, availableColumns);
		int gridWidth = Math.max(1, columns * SLOT_SIZE);
		int gridX = lootPanel.x() + Math.max(0, (lootPanel.width() - gridWidth) / 2);
		Bounds lootViewport = new Bounds(
			gridX,
			lootPanel.y() + Math.min(lootHeaderHeight, Math.max(0, lootPanel.height() - 1)),
			Math.min(gridWidth, lootPanel.width()),
			Math.max(1, lootPanel.height() - lootHeaderHeight - 4)
		);

		return new LootScreenLayout(
			panel,
			searchBox,
			inventoryPanel,
			lootPanel,
			lootViewport,
			detailPanel,
			stacked,
			columns
		);
	}

	public Bounds lootSlotBounds(int index, double scrollOffset) {
		int column = Math.floorMod(index, columns);
		int row = index / columns;
		return new Bounds(
			lootViewport.x() + column * SLOT_SIZE,
			lootViewport.y() + row * SLOT_SIZE - (int) Math.round(scrollOffset),
			SLOT_SIZE,
			SLOT_SIZE
		);
	}

	public Bounds inventorySlotBounds(int inventorySlot) {
		if (inventorySlot < 0 || inventorySlot >= 36) {
			return Bounds.EMPTY;
		}

		int gridX = inventoryPanel.x() + Math.max(0, (inventoryPanel.width() - INVENTORY_GRID_WIDTH) / 2);
		int gridY = inventoryPanel.y() + 17;
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
		return Math.max(0, rows * SLOT_SIZE - lootViewport.height());
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

		public boolean contains(double pointX, double pointY) {
			return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
		}

		public boolean intersects(Bounds other) {
			return right() > other.x && x < other.right() && bottom() > other.y && y < other.bottom();
		}
	}
}
