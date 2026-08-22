package com.shouyun.tacticalpickup.client.loot;

public record LootScreenLayout(
	Bounds panel,
	Bounds searchBox,
	Bounds lootPanel,
	Bounds lootViewport,
	Bounds inventoryPanel,
	Bounds detailPanel,
	boolean stacked,
	int columns
) {
	public static final int CARD_HEIGHT = 36;
	public static final int CARD_GAP = 4;
	public static final int SLOT_SIZE = 18;
	private static final int MAX_WIDTH = 960;
	private static final int MAX_HEIGHT = 620;
	private static final int SCREEN_MARGIN = 6;
	private static final int PANEL_GAP = 6;
	private static final int INVENTORY_WIDTH = SLOT_SIZE * 9 + 8;
	private static final int INVENTORY_MIN_HEIGHT = SLOT_SIZE * 4 + 18;
	private static final int MIN_CARD_WIDTH = 94;

	public static LootScreenLayout calculate(int screenWidth, int screenHeight) {
		int panelWidth = Math.max(1, Math.min(MAX_WIDTH, Math.min(
			Math.max(1, screenWidth - SCREEN_MARGIN * 2),
			(int) Math.floor(screenWidth * 0.9D)
		)));
		int panelHeight = Math.max(1, Math.min(MAX_HEIGHT, Math.min(
			Math.max(1, screenHeight - SCREEN_MARGIN * 2),
			(int) Math.floor(screenHeight * 0.9D)
		)));
		int panelX = (screenWidth - panelWidth) / 2;
		int panelY = (screenHeight - panelHeight) / 2;
		Bounds panel = new Bounds(panelX, panelY, panelWidth, panelHeight);

		boolean compactHeader = panelWidth < 520;
		int headerHeight = compactHeader ? 44 : 28;
		Bounds searchBox = compactHeader
			? new Bounds(panelX + 6, panelY + 21, Math.max(1, panelWidth - 12), 18)
			: new Bounds(panelX + panelWidth / 2 - 110, panelY + 4, 220, 20);

		int desiredDetailHeight = clamp(panelHeight / 5, 50, 112);
		int availableAfterHeader = Math.max(1, panelHeight - headerHeight - PANEL_GAP * 2);
		int detailHeight = Math.min(desiredDetailHeight, Math.max(36, availableAfterHeader - INVENTORY_MIN_HEIGHT));
		int detailY = panel.bottom() - detailHeight;
		Bounds detailPanel = new Bounds(panelX, detailY, panelWidth, detailHeight);
		int contentY = panelY + headerHeight;
		int contentHeight = Math.max(1, detailY - PANEL_GAP - contentY);

		boolean stacked = panelWidth < INVENTORY_WIDTH + MIN_CARD_WIDTH + PANEL_GAP;
		Bounds lootPanel;
		Bounds inventoryPanel;
		if (stacked) {
			int inventoryHeight = Math.min(INVENTORY_MIN_HEIGHT, Math.max(1, contentHeight / 2));
			int lootHeight = Math.max(1, contentHeight - inventoryHeight - PANEL_GAP);
			lootPanel = new Bounds(panelX, contentY, panelWidth, lootHeight);
			inventoryPanel = new Bounds(panelX, contentY + lootHeight + PANEL_GAP, panelWidth, inventoryHeight);
		} else {
			int lootWidth = Math.max(1, panelWidth - INVENTORY_WIDTH - PANEL_GAP);
			lootPanel = new Bounds(panelX, contentY, lootWidth, contentHeight);
			inventoryPanel = new Bounds(panelX + lootWidth + PANEL_GAP, contentY, INVENTORY_WIDTH, contentHeight);
		}

		Bounds lootViewport = new Bounds(
			lootPanel.x() + 4,
			lootPanel.y() + 17,
			Math.max(1, lootPanel.width() - 8),
			Math.max(1, lootPanel.height() - 21)
		);
		int columns = clamp(
			(lootViewport.width() + CARD_GAP) / (MIN_CARD_WIDTH + CARD_GAP),
			1,
			4
		);
		return new LootScreenLayout(
			panel,
			searchBox,
			lootPanel,
			lootViewport,
			inventoryPanel,
			detailPanel,
			stacked,
			columns
		);
	}

	public Bounds cardBounds(int index, double scrollOffset) {
		int column = Math.floorMod(index, columns);
		int row = index / columns;
		int cardWidth = (lootViewport.width() - CARD_GAP * (columns - 1)) / columns;
		int usedWidth = cardWidth * columns + CARD_GAP * (columns - 1);
		int centeredX = lootViewport.x() + Math.max(0, (lootViewport.width() - usedWidth) / 2);
		return new Bounds(
			centeredX + column * (cardWidth + CARD_GAP),
			lootViewport.y() + row * (CARD_HEIGHT + CARD_GAP) - (int) Math.round(scrollOffset),
			cardWidth,
			CARD_HEIGHT
		);
	}

	public double maxScroll(int itemCount) {
		int rows = itemCount <= 0 ? 0 : (itemCount + columns - 1) / columns;
		int contentHeight = Math.max(0, rows * (CARD_HEIGHT + CARD_GAP) - CARD_GAP);
		return Math.max(0, contentHeight - lootViewport.height());
	}

	public double clampScroll(double scrollOffset, int itemCount) {
		return Math.max(0.0D, Math.min(scrollOffset, maxScroll(itemCount)));
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(value, maximum));
	}

	public record Bounds(int x, int y, int width, int height) {
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
