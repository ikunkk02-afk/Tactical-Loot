package com.shouyun.tacticalpickup.client.loot;

import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.loot.LootScreenLayout.Bounds;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.ui.ItemDetailHelper;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class LootScreen extends Screen {
	private static final int BACKDROP_COLOR = 0x98000000;
	private static final int PANEL_COLOR = 0xE0101217;
	private static final int INSET_COLOR = 0xB0181B21;
	private static final int CARD_COLOR = 0xC021252C;
	private static final int CARD_HOVER_COLOR = 0xD02C333D;
	private static final int SELECTED_COLOR = 0xE03D5265;
	private static final int DROP_HIGHLIGHT_COLOR = 0x705F9B73;
	private static final int BORDER_COLOR = 0xFF59636E;
	private static final int ACCENT_COLOR = 0xFF78A8C8;
	private static final int TEXT_COLOR = 0xFFF2F4F7;
	private static final int MUTED_TEXT_COLOR = 0xFF9DA5AF;
	private static final int LOW_PRIORITY_COLOR = 0xFFD0A66A;
	private static final int SLOT_COLOR = 0xB0090B0E;
	private static final int SLOT_BORDER_COLOR = 0xFF3B4149;
	private static final int BUTTON_GAP = 3;

	private final ClientPickupManager pickupManager = ClientPickupManager.getInstance();
	private final LootSelectionState selectionState = new LootSelectionState();
	private final LootDragState dragState = new LootDragState();
	private List<LootGroup> visibleGroups = List.of();
	private LootScreenLayout layout;
	private EditBox searchBox;
	private Button minusSixteenButton;
	private Button minusOneButton;
	private Button allButton;
	private Button plusOneButton;
	private Button plusSixteenButton;
	private Button pickupButton;
	private LocalPlayer openedPlayer;
	private ResourceKey<Level> openedDimension;
	private double scrollOffset;
	private LootGroup hoveredTooltipGroup;

	public LootScreen() {
		super(Component.translatable("tactical_pickup.loot.title"));
	}

	public static void closeIfOpen(Minecraft client) {
		if (client.screen instanceof LootScreen screen) {
			screen.onClose();
		}
	}

	@Override
	protected void init() {
		String previousQuery = searchBox == null ? "" : searchBox.getValue();
		if (openedPlayer == null && minecraft.player != null && minecraft.level != null) {
			openedPlayer = minecraft.player;
			openedDimension = minecraft.level.dimension();
		}

		layout = LootScreenLayout.calculate(width, height);
		Bounds searchBounds = layout.searchBox();
		searchBox = new EditBox(
			font,
			searchBounds.x(),
			searchBounds.y(),
			searchBounds.width(),
			searchBounds.height(),
			Component.translatable("tactical_pickup.loot.search")
		);
		searchBox.setHint(Component.translatable("tactical_pickup.loot.search"));
		searchBox.setMaxLength(128);
		searchBox.setValue(previousQuery);
		searchBox.setResponder(query -> refreshVisibleGroups());
		addRenderableWidget(searchBox);
		createDetailButtons();
		refreshVisibleGroups();
		setInitialFocus(searchBox);
	}

	private void createDetailButtons() {
		Bounds detail = layout.detailPanel();
		int[] widths = {36, 30, 44, 30, 36, 56};
		int totalWidth = 0;
		for (int buttonWidth : widths) {
			totalWidth += buttonWidth;
		}
		totalWidth += BUTTON_GAP * (widths.length - 1);
		int x = detail.x() + Math.max(5, (detail.width() - totalWidth) / 2);
		int y = Math.max(detail.y() + 2, detail.bottom() - 23);

		minusSixteenButton = addRenderableWidget(quantityButton(
			Component.literal("-16"),
			x,
			y,
			widths[0],
			-1,
			16
		));
		x += widths[0] + BUTTON_GAP;
		minusOneButton = addRenderableWidget(quantityButton(
			Component.literal("-1"),
			x,
			y,
			widths[1],
			-1,
			1
		));
		x += widths[1] + BUTTON_GAP;
		allButton = addRenderableWidget(Button.builder(
			Component.translatable("tactical_pickup.loot.amount_all_button"),
			button -> {
				selectionState.resetAmount();
				updateButtonState();
			}
		).bounds(x, y, widths[2], 20).build());
		x += widths[2] + BUTTON_GAP;
		plusOneButton = addRenderableWidget(quantityButton(
			Component.literal("+1"),
			x,
			y,
			widths[3],
			1,
			1
		));
		x += widths[3] + BUTTON_GAP;
		plusSixteenButton = addRenderableWidget(quantityButton(
			Component.literal("+16"),
			x,
			y,
			widths[4],
			1,
			16
		));
		x += widths[4] + BUTTON_GAP;
		pickupButton = addRenderableWidget(Button.builder(
			Component.translatable("tactical_pickup.loot.pickup"),
			button -> pickupSelected()
		).bounds(x, y, widths[5], 20).build());
		updateButtonState();
	}

	private Button quantityButton(Component label, int x, int y, int width, int steps, int amountPerStep) {
		return Button.builder(label, button -> {
			LootGroup selected = selectedGroup();
			if (selected != null) {
				selectionState.adjust(steps, amountPerStep, selected.totalCount());
				updateButtonState();
			}
		}).bounds(x, y, width, 20).build();
	}

	private void pickupSelected() {
		LootGroup selected = selectedGroup();
		if (selected != null) {
			pickupManager.requestPickup(selected.representativeEntityId(), selectionState.requestedAmount());
		}
	}

	@Override
	public void tick() {
		if (minecraft.player == null
				|| minecraft.level == null
				|| !minecraft.player.isAlive()
				|| minecraft.player != openedPlayer
				|| !minecraft.level.dimension().equals(openedDimension)) {
			onClose();
			return;
		}

		refreshVisibleGroups();
	}

	private void refreshVisibleGroups() {
		if (searchBox == null || layout == null) {
			return;
		}

		String query = searchBox.getValue();
		visibleGroups = pickupManager.groups().stream()
			.filter(group -> LootSearchMatcher.matches(
				group.displayStack().getHoverName().getString(),
				LootGroupFilter.itemId(group),
				query
			))
			.toList();
		selectionState.reconcile(visibleGroups);
		scrollOffset = layout.clampScroll(scrollOffset, visibleGroups.size());
		updateButtonState();
	}

	private LootGroup selectedGroup() {
		return selectionState.reconcile(visibleGroups);
	}

	private void updateButtonState() {
		LootGroup selected = selectionState.reconcile(visibleGroups);
		boolean active = selected != null;
		if (minusSixteenButton != null) {
			minusSixteenButton.active = active;
			minusOneButton.active = active;
			allButton.active = active && !selectionState.pickupAll();
			plusOneButton.active = active && !selectionState.pickupAll();
			plusSixteenButton.active = active && !selectionState.pickupAll();
			pickupButton.active = active;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		hoveredTooltipGroup = null;
		graphics.fill(0, 0, width, height, BACKDROP_COLOR);
		fillPanel(graphics, layout.panel(), PANEL_COLOR, ACCENT_COLOR);
		renderHeader(graphics);
		fillPanel(graphics, layout.lootPanel(), INSET_COLOR, BORDER_COLOR);
		fillPanel(graphics, layout.inventoryPanel(), INSET_COLOR, BORDER_COLOR);
		fillPanel(graphics, layout.detailPanel(), INSET_COLOR, BORDER_COLOR);
		renderLootPanel(graphics, mouseX, mouseY);
		renderInventoryPanel(graphics, mouseX, mouseY);
		renderDetailPanel(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);

		if (dragState.isDragging()) {
			renderDragGhost(graphics, mouseX, mouseY);
		} else if (hoveredTooltipGroup != null) {
			graphics.renderTooltip(font, hoveredTooltipGroup.displayStack(), mouseX, mouseY);
		}
	}

	private void renderHeader(GuiGraphics graphics) {
		Bounds panel = layout.panel();
		graphics.drawString(font, title, panel.x() + 7, panel.y() + 8, TEXT_COLOR, true);
		Component count = Component.translatable("tactical_pickup.loot.group_count", pickupManager.groups().size());
		graphics.drawString(
			font,
			count,
			panel.right() - font.width(count) - 7,
			panel.y() + 8,
			MUTED_TEXT_COLOR,
			false
		);
	}

	private void renderLootPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		Bounds panel = layout.lootPanel();
		Bounds viewport = layout.lootViewport();
		graphics.drawString(
			font,
			Component.translatable("tactical_pickup.loot.nearby"),
			panel.x() + 5,
			panel.y() + 5,
			TEXT_COLOR,
			false
		);

		if (visibleGroups.isEmpty()) {
			Component empty = searchBox.getValue().isBlank()
				? Component.translatable("tactical_pickup.loot.empty")
				: Component.translatable("tactical_pickup.loot.no_results");
			graphics.drawCenteredString(
				font,
				empty,
				viewport.x() + viewport.width() / 2,
				viewport.y() + Math.max(4, viewport.height() / 2 - font.lineHeight / 2),
				MUTED_TEXT_COLOR
			);
			return;
		}

		graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
		for (int index = 0; index < visibleGroups.size(); index++) {
			Bounds card = layout.cardBounds(index, scrollOffset);
			if (!card.intersects(viewport)) {
				continue;
			}

			LootGroup group = visibleGroups.get(index);
			boolean selected = group.key().equals(selectionState.selectedKey());
			boolean hovered = viewport.contains(mouseX, mouseY) && card.contains(mouseX, mouseY);
			ItemFilterState state = pickupManager.filterManager().getState(LootGroupFilter.itemId(group));
			int cardColor = selected ? SELECTED_COLOR : hovered ? CARD_HOVER_COLOR : CARD_COLOR;
			graphics.fill(card.x(), card.y(), card.right(), card.bottom(), cardColor);
			int borderColor = state == ItemFilterState.LOW_PRIORITY ? LOW_PRIORITY_COLOR : BORDER_COLOR;
			drawBorder(graphics, card, borderColor);

			int itemX = card.x() + 5;
			int itemY = card.y() + 10;
			graphics.renderItem(group.displayStack(), itemX, itemY);
			int textX = card.x() + 26;
			int textWidth = Math.max(1, card.width() - 30);
			String name = font.plainSubstrByWidth(group.displayStack().getHoverName().getString(), textWidth);
			int nameColor = state == ItemFilterState.LOW_PRIORITY && !selected ? MUTED_TEXT_COLOR : TEXT_COLOR;
			graphics.drawString(font, name, textX, card.y() + 6, nameColor, true);
			Component count = state == ItemFilterState.LOW_PRIORITY
				? Component.translatable("tactical_pickup.loot.card_count_low", group.totalCount())
				: Component.translatable("tactical_pickup.loot.card_count", group.totalCount());
			String countText = font.plainSubstrByWidth(count.getString(), textWidth);
			graphics.drawString(
				font,
				countText,
				textX,
				card.y() + 20,
				state == ItemFilterState.LOW_PRIORITY ? LOW_PRIORITY_COLOR : MUTED_TEXT_COLOR,
				false
			);

			if (hovered && mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
				hoveredTooltipGroup = group;
			}
		}
		graphics.disableScissor();
		renderScrollBar(graphics);
	}

	private void renderScrollBar(GuiGraphics graphics) {
		double maxScroll = layout.maxScroll(visibleGroups.size());
		if (maxScroll <= 0.0D) {
			return;
		}

		Bounds viewport = layout.lootViewport();
		int barX = viewport.right() - 2;
		int thumbHeight = Math.max(12, (int) Math.round(
			viewport.height() * (viewport.height() / (viewport.height() + maxScroll))
		));
		int travel = Math.max(1, viewport.height() - thumbHeight);
		int thumbY = viewport.y() + (int) Math.round(travel * (scrollOffset / maxScroll));
		graphics.fill(barX, viewport.y(), barX + 2, viewport.bottom(), 0x70282D34);
		graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, ACCENT_COLOR);
	}

	private void renderInventoryPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		Bounds panel = layout.inventoryPanel();
		boolean dropTarget = dragState.isDragging() && panel.contains(mouseX, mouseY);
		if (dropTarget) {
			graphics.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.bottom() - 1, DROP_HIGHLIGHT_COLOR);
		}

		Component heading = dropTarget
			? Component.translatable("tactical_pickup.loot.drop_hint")
			: Component.translatable("tactical_pickup.loot.inventory");
		graphics.drawString(
			font,
			heading,
			panel.x() + 5,
			panel.y() + 5,
			dropTarget ? TEXT_COLOR : MUTED_TEXT_COLOR,
			false
		);
		if (minecraft.player == null) {
			return;
		}

		Inventory inventory = minecraft.player.getInventory();
		int gridWidth = LootScreenLayout.SLOT_SIZE * 9;
		int gridX = panel.x() + Math.max(4, (panel.width() - gridWidth) / 2);
		int gridY = panel.y() + 17;
		for (int row = 0; row < 4; row++) {
			for (int column = 0; column < 9; column++) {
				int slotX = gridX + column * LootScreenLayout.SLOT_SIZE;
				int slotY = gridY + row * LootScreenLayout.SLOT_SIZE;
				if (slotX + LootScreenLayout.SLOT_SIZE > panel.right()
						|| slotY + LootScreenLayout.SLOT_SIZE > panel.bottom()) {
					continue;
				}

				graphics.fill(slotX, slotY, slotX + 18, slotY + 18, SLOT_BORDER_COLOR);
				graphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, SLOT_COLOR);
				int inventorySlot = row < 3 ? 9 + row * 9 + column : column;
				ItemStack stack = inventory.getItem(inventorySlot);
				if (!stack.isEmpty()) {
					graphics.renderItem(stack, slotX + 1, slotY + 1);
					graphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);
				}
			}
		}
	}

	private void renderDetailPanel(GuiGraphics graphics) {
		Bounds detail = layout.detailPanel();
		LootGroup selected = selectedGroup();
		if (selected == null) {
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.loot.select_hint"),
				detail.x() + 7,
				detail.y() + 7,
				MUTED_TEXT_COLOR,
				false
			);
			return;
		}

		int basicX = detail.x() + 7;
		int basicY = detail.y() + 6;
		int basicWidth = detail.width() >= 520 ? detail.width() / 2 - 12 : detail.width() - 14;
		String selectedName = font.plainSubstrByWidth(selected.displayStack().getHoverName().getString(), basicWidth);
		graphics.drawString(font, selectedName, basicX, basicY, TEXT_COLOR, true);
		graphics.drawString(
			font,
			Component.translatable("tactical_pickup.loot.total", selected.totalCount()),
			basicX,
			basicY + 12,
			MUTED_TEXT_COLOR,
			false
		);
		Component amount = selectionState.pickupAll()
			? Component.translatable("tactical_pickup.loot.amount_all", selected.totalCount())
			: Component.translatable(
				"tactical_pickup.loot.amount",
				selectionState.selectedAmount(selected.totalCount()),
				selected.totalCount()
			);
		graphics.drawString(font, amount, basicX, basicY + 23, TEXT_COLOR, false);
		ItemFilterState filterState = pickupManager.filterManager().getState(LootGroupFilter.itemId(selected));
		graphics.drawString(
			font,
			Component.translatable(
				"tactical_pickup.loot.filter_state",
				Component.translatable(filterState.translationKey())
			),
			basicX,
			basicY + 34,
			filterState == ItemFilterState.LOW_PRIORITY ? LOW_PRIORITY_COLOR : MUTED_TEXT_COLOR,
			false
		);

		List<Component> enchantments = ItemDetailHelper.collectEnchantments(minecraft, selected.displayStack());
		if (!enchantments.isEmpty()) {
			int enchantmentX = detail.width() >= 520 ? detail.x() + detail.width() / 2 : basicX;
			int enchantmentY = detail.width() >= 520 ? basicY : basicY + 46;
			int enchantmentWidth = detail.width() >= 520 ? detail.width() / 2 - 10 : detail.width() - 14;
			int buttonsY = minusOneButton == null ? detail.bottom() : minusOneButton.getY();
			int availableLines = Math.max(0, (buttonsY - enchantmentY - font.lineHeight - 2) / (font.lineHeight + 1));
			int visibleCount = Math.min(
				Math.min(enchantments.size(), ItemDetailHelper.MAX_VISIBLE_ENCHANTMENTS),
				availableLines
			);
			if (visibleCount > 0) {
				graphics.drawString(
					font,
					Component.translatable("tactical_pickup.loot.enchantments"),
					enchantmentX,
					enchantmentY,
					TEXT_COLOR,
					false
				);
				for (int index = 0; index < visibleCount; index++) {
					String text = font.plainSubstrByWidth(enchantments.get(index).getString(), enchantmentWidth);
					graphics.drawString(
						font,
						text,
						enchantmentX,
						enchantmentY + font.lineHeight + 2 + index * (font.lineHeight + 1),
						MUTED_TEXT_COLOR,
						false
					);
				}
			}
		}
	}

	private void renderDragGhost(GuiGraphics graphics, int mouseX, int mouseY) {
		LootDragState.Snapshot snapshot = dragState.snapshot();
		if (snapshot == null) {
			return;
		}

		Component amount = snapshot.requestedAmount() == PickupRequestPayload.ALL_ITEMS
			? Component.translatable("tactical_pickup.loot.drag_all")
			: Component.translatable("tactical_pickup.loot.card_count", snapshot.requestedAmount());
		String label = snapshot.displayStack().getHoverName().getString() + " " + amount.getString();
		int ghostWidth = Math.min(190, Math.max(72, font.width(label) + 30));
		int ghostX = Math.max(2, Math.min(mouseX + 10, width - ghostWidth - 2));
		int ghostY = Math.max(2, Math.min(mouseY + 10, height - 24));
		graphics.fill(ghostX, ghostY, ghostX + ghostWidth, ghostY + 22, 0xE0191D22);
		drawBorder(graphics, new Bounds(ghostX, ghostY, ghostWidth, 22), ACCENT_COLOR);
		graphics.renderItem(snapshot.displayStack(), ghostX + 3, ghostY + 3);
		String clipped = font.plainSubstrByWidth(label, ghostWidth - 26);
		graphics.drawString(font, clipped, ghostX + 23, ghostY + 7, TEXT_COLOR, true);
	}

	private static void fillPanel(GuiGraphics graphics, Bounds bounds, int fillColor, int borderColor) {
		graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), fillColor);
		drawBorder(graphics, bounds, borderColor);
	}

	private static void drawBorder(GuiGraphics graphics, Bounds bounds, int color) {
		graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, color);
		graphics.fill(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), color);
		graphics.fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.bottom(), color);
		graphics.fill(bounds.right() - 1, bounds.y(), bounds.right(), bounds.bottom(), color);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		if (button == 0 && layout.lootViewport().contains(mouseX, mouseY)) {
			for (int index = 0; index < visibleGroups.size(); index++) {
				Bounds card = layout.cardBounds(index, scrollOffset);
				if (card.contains(mouseX, mouseY)) {
					LootGroup group = visibleGroups.get(index);
					selectionState.select(group);
					dragState.press(group, selectionState.requestedAmount(), mouseX, mouseY);
					updateButtonState();
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == 0 && dragState.isActive()) {
			dragState.update(mouseX, mouseY);
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && dragState.isActive()) {
			LootDragState.Snapshot snapshot = dragState.release(layout.inventoryPanel().contains(mouseX, mouseY));
			if (snapshot != null) {
				pickupManager.requestPickup(snapshot.representativeEntityId(), snapshot.requestedAmount());
			}
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (layout.lootPanel().contains(mouseX, mouseY)) {
			scrollOffset = layout.clampScroll(scrollOffset - vertical * 22.0D, visibleGroups.size());
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ClientKeyMappings.OPEN_LOOT_SCREEN.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		clearTransientState();
		if (minecraft != null) {
			minecraft.setScreen(null);
		}
	}

	@Override
	public void removed() {
		clearTransientState();
		super.removed();
	}

	private void clearTransientState() {
		dragState.clear();
		selectionState.clear();
		visibleGroups = List.of();
		scrollOffset = 0.0D;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
