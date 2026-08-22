package com.shouyun.tacticalpickup.client.loot;

import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.loot.LootScreenDragState.InventorySnapshot;
import com.shouyun.tacticalpickup.client.loot.LootScreenDragState.LootSnapshot;
import com.shouyun.tacticalpickup.client.loot.LootScreenDragState.Snapshot;
import com.shouyun.tacticalpickup.client.loot.LootScreenLayout.Bounds;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.ui.ItemDetailHelper;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class LootScreen extends Screen {
	private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
	private static final int WORLD_DIM_COLOR = 0x60000000;
	private static final int PANEL_COLOR = 0xFFC6C6C6;
	private static final int PANEL_HIGHLIGHT_COLOR = 0xFFFFFFFF;
	private static final int PANEL_MID_COLOR = 0xFF8B8B8B;
	private static final int PANEL_SHADOW_COLOR = 0xFF373737;
	private static final int TEXT_COLOR = 0xFF404040;
	private static final int MUTED_TEXT_COLOR = 0xFF666666;
	private static final int LOW_PRIORITY_OVERLAY = 0x58000000;
	private static final int LOW_PRIORITY_MARKER = 0xFF6A604D;
	private static final int SELECTED_COLOR = 0xFFF5F5F5;
	private static final int COMPATIBLE_COLOR = 0xFFD8E4D0;
	private static final int INCOMPATIBLE_COLOR = 0xFF6F3E3E;
	private static final int DROP_OVERLAY_COLOR = 0x307A886F;
	private static final int BUTTON_GAP = 2;

	private final ClientPickupManager pickupManager = ClientPickupManager.getInstance();
	private final LootSelectionState selectionState = new LootSelectionState();
	private final LootScreenDragState dragState = new LootScreenDragState();
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
	private ItemStack hoveredTooltipStack = ItemStack.EMPTY;

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
		int[] widths = {22, 18, 30, 18, 22};
		int quantityWidth = 0;
		for (int width : widths) {
			quantityWidth += width;
		}
		quantityWidth += BUTTON_GAP * (widths.length - 1);

		int pickupWidth = 46;
		boolean singleRow = detail.width() >= quantityWidth + 4 + pickupWidth + 6;
		int quantityX = detail.x() + Math.max(3, (detail.width() - (singleRow ? quantityWidth + 4 + pickupWidth : quantityWidth)) / 2);
		int quantityY = singleRow ? Math.max(detail.y() + 2, detail.bottom() - 23) : detail.y() + 2;
		int x = quantityX;
		minusSixteenButton = addRenderableWidget(quantityButton(Component.literal("-16"), x, quantityY, widths[0], -1, 16));
		x += widths[0] + BUTTON_GAP;
		minusOneButton = addRenderableWidget(quantityButton(Component.literal("-1"), x, quantityY, widths[1], -1, 1));
		x += widths[1] + BUTTON_GAP;
		allButton = addRenderableWidget(Button.builder(
			Component.translatable("tactical_pickup.loot.amount_all_button"),
			button -> {
				selectionState.resetAmount();
				updateButtonState();
			}
		).bounds(x, quantityY, widths[2], 20).build());
		x += widths[2] + BUTTON_GAP;
		plusOneButton = addRenderableWidget(quantityButton(Component.literal("+1"), x, quantityY, widths[3], 1, 1));
		x += widths[3] + BUTTON_GAP;
		plusSixteenButton = addRenderableWidget(quantityButton(Component.literal("+16"), x, quantityY, widths[4], 1, 16));

		int pickupX = singleRow ? quantityX + quantityWidth + 4 : detail.x() + Math.max(3, (detail.width() - pickupWidth) / 2);
		int pickupY = singleRow ? quantityY : Math.min(detail.bottom() - 21, quantityY + 22);
		pickupButton = addRenderableWidget(Button.builder(
			Component.translatable("tactical_pickup.loot.pickup"),
			button -> pickupSelected()
		).bounds(pickupX, pickupY, pickupWidth, 20).build());
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
		hoveredTooltipStack = ItemStack.EMPTY;
		graphics.fill(0, 0, width, height, WORLD_DIM_COLOR);
		renderRaisedPanel(graphics, layout.panel());
		renderRaisedPanel(graphics, layout.inventoryPanel());
		renderRaisedPanel(graphics, layout.lootPanel());
		renderRaisedPanel(graphics, layout.detailPanel());
		renderHeader(graphics);
		renderInventoryPanel(graphics, mouseX, mouseY);
		renderLootPanel(graphics, mouseX, mouseY);
		renderDetailPanel(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);

		if (dragState.isDragging()) {
			renderDragGhost(graphics, mouseX, mouseY);
		} else if (!hoveredTooltipStack.isEmpty()) {
			graphics.renderTooltip(font, hoveredTooltipStack, mouseX, mouseY);
		}
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// A light pixel-sharp dim is rendered before the Vanilla-style panels above.
	}

	private void renderHeader(GuiGraphics graphics) {
		Bounds panel = layout.panel();
		graphics.drawString(font, title, panel.x() + 8, panel.y() + 8, TEXT_COLOR, false);
		Component count = Component.translatable("tactical_pickup.loot.group_count", pickupManager.groups().size());
		graphics.drawString(
			font,
			count,
			panel.right() - font.width(count) - 8,
			panel.y() + 8,
			MUTED_TEXT_COLOR,
			false
		);
	}

	private void renderInventoryPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		Bounds panel = layout.inventoryPanel();
		graphics.drawString(
			font,
			Component.translatable("tactical_pickup.loot.inventory"),
			panel.x() + 5,
			panel.y() + 5,
			TEXT_COLOR,
			false
		);
		if (minecraft.player == null) {
			return;
		}

		Inventory inventory = minecraft.player.getInventory();
		OptionalInt hoveredSlot = layout.inventorySlotAt(mouseX, mouseY);
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			Bounds slotBounds = layout.inventorySlotBounds(slot);
			if (!isFullyInside(panel, slotBounds)) {
				continue;
			}

			graphics.blitSprite(SLOT_SPRITE, slotBounds.x(), slotBounds.y(), slotBounds.width(), slotBounds.height());
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				graphics.renderItem(stack, slotBounds.x() + 1, slotBounds.y() + 1);
				graphics.renderItemDecorations(font, stack, slotBounds.x() + 1, slotBounds.y() + 1);
			}

			if (hoveredSlot.isPresent() && hoveredSlot.getAsInt() == slot) {
				if (dragState.isDragging() && dragState.snapshot() instanceof LootSnapshot lootSnapshot) {
					boolean compatible = canAcceptLoot(stack, lootSnapshot.displayStack());
					drawBorder(graphics, slotBounds, compatible ? COMPATIBLE_COLOR : INCOMPATIBLE_COLOR);
					if (!compatible) {
						graphics.drawCenteredString(font, Component.literal("×"), slotBounds.x() + 9, slotBounds.y() + 5, INCOMPATIBLE_COLOR);
					}
				} else if (!dragState.isDragging()) {
					AbstractContainerScreen.renderSlotHighlight(graphics, slotBounds.x() + 1, slotBounds.y() + 1, 250);
					if (!stack.isEmpty()) {
						hoveredTooltipStack = stack;
					}
				}
			}
		}

		if (!layout.stacked()) {
			Component help = Component.translatable("tactical_pickup.loot.inventory_drag_hint");
			int helpY = panel.bottom() + 8;
			int helpWidth = Math.max(1, panel.width() - 8);
			String clipped = font.plainSubstrByWidth(help.getString(), helpWidth);
			graphics.drawString(font, clipped, panel.x() + 4, helpY, MUTED_TEXT_COLOR, false);
		}
	}

	private boolean canAcceptLoot(ItemStack targetStack, ItemStack lootStack) {
		if (targetStack.isEmpty()) {
			return true;
		}
		return ItemStack.isSameItemSameComponents(targetStack, lootStack)
			&& targetStack.getCount() < minecraft.player.getInventory().getMaxStackSize(lootStack);
	}

	private void renderLootPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		Bounds panel = layout.lootPanel();
		Bounds viewport = layout.lootViewport();
		boolean inventoryDropTarget = dragState.isDragging()
			&& dragState.snapshot() instanceof InventorySnapshot
			&& panel.contains(mouseX, mouseY);
		Component heading = inventoryDropTarget
			? Component.translatable("tactical_pickup.loot.world_drop_hint")
			: Component.translatable("tactical_pickup.loot.nearby");
		graphics.drawString(font, heading, panel.x() + 5, panel.y() + 5, TEXT_COLOR, false);

		if (visibleGroups.isEmpty()) {
			Component empty = searchBox.getValue().isBlank()
				? Component.translatable("tactical_pickup.loot.empty")
				: Component.translatable("tactical_pickup.loot.no_results");
			graphics.drawCenteredString(
				font,
				empty,
				viewport.x() + viewport.width() / 2,
				viewport.y() + Math.max(2, viewport.height() / 2 - font.lineHeight / 2),
				MUTED_TEXT_COLOR
			);
		} else {
			graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
			for (int index = 0; index < visibleGroups.size(); index++) {
				Bounds slotBounds = layout.lootSlotBounds(index, scrollOffset);
				if (!slotBounds.intersects(viewport)) {
					continue;
				}

				LootGroup group = visibleGroups.get(index);
				ItemFilterState state = pickupManager.filterManager().getState(LootGroupFilter.itemId(group));
				boolean selected = group.key().equals(selectionState.selectedKey());
				boolean hovered = viewport.contains(mouseX, mouseY) && slotBounds.contains(mouseX, mouseY);
				graphics.blitSprite(SLOT_SPRITE, slotBounds.x(), slotBounds.y(), slotBounds.width(), slotBounds.height());
				ItemStack displayStack = group.displayStack().copyWithCount(1);
				graphics.renderItem(displayStack, slotBounds.x() + 1, slotBounds.y() + 1);
				graphics.renderItemDecorations(
					font,
					displayStack,
					slotBounds.x() + 1,
					slotBounds.y() + 1,
					formatSlotCount(group.totalCount())
				);

				if (state == ItemFilterState.LOW_PRIORITY) {
					graphics.fill(slotBounds.x() + 1, slotBounds.y() + 1, slotBounds.right() - 1, slotBounds.bottom() - 1, LOW_PRIORITY_OVERLAY);
					graphics.fill(slotBounds.x() + 2, slotBounds.y() + 2, slotBounds.x() + 5, slotBounds.y() + 3, LOW_PRIORITY_MARKER);
				}
				if (selected) {
					drawBorder(graphics, slotBounds, SELECTED_COLOR);
				}
				if (hovered && !dragState.isDragging()) {
					AbstractContainerScreen.renderSlotHighlight(graphics, slotBounds.x() + 1, slotBounds.y() + 1, 250);
					hoveredTooltipStack = group.displayStack();
				}
			}
			graphics.disableScissor();
			renderScrollBar(graphics);
		}

		if (inventoryDropTarget) {
			graphics.fill(panel.x() + 2, panel.y() + 20, panel.right() - 2, panel.bottom() - 2, DROP_OVERLAY_COLOR);
			drawBorder(graphics, panel, COMPATIBLE_COLOR);
		}
	}

	private String formatSlotCount(int count) {
		String exact = Integer.toString(count);
		if (font.width(exact) <= 20) {
			return exact;
		}

		double divisor = count >= 1_000_000 ? 1_000_000.0D : 1_000.0D;
		String suffix = count >= 1_000_000 ? "M" : "k";
		String compact = String.format(Locale.ROOT, "%.1f%s", count / divisor, suffix);
		return compact.replace(".0" + suffix, suffix);
	}

	private void renderScrollBar(GuiGraphics graphics) {
		double maxScroll = layout.maxScroll(visibleGroups.size());
		if (maxScroll <= 0.0D) {
			return;
		}

		Bounds viewport = layout.lootViewport();
		int barX = Math.min(layout.lootPanel().right() - 4, viewport.right() + 2);
		int thumbHeight = Math.max(12, (int) Math.round(
			viewport.height() * (viewport.height() / (viewport.height() + maxScroll))
		));
		int travel = Math.max(1, viewport.height() - thumbHeight);
		int thumbY = viewport.y() + (int) Math.round(travel * (scrollOffset / maxScroll));
		graphics.fill(barX, viewport.y(), barX + 2, viewport.bottom(), PANEL_SHADOW_COLOR);
		graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, PANEL_HIGHLIGHT_COLOR);
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

		int textX = detail.x() + 7;
		int textY = detail.y() + 6;
		int textWidth = Math.max(1, detail.width() - 14);
		int buttonsY = minusOneButton == null ? detail.bottom() : Math.min(minusOneButton.getY(), pickupButton.getY());
		String selectedName = font.plainSubstrByWidth(selected.displayStack().getHoverName().getString(), textWidth);
		graphics.drawString(font, selectedName, textX, textY, TEXT_COLOR, false);
		Component amount = selectionState.pickupAll()
			? Component.translatable("tactical_pickup.loot.amount_all", selected.totalCount())
			: Component.translatable(
				"tactical_pickup.loot.amount",
				selectionState.selectedAmount(selected.totalCount()),
				selected.totalCount()
			);
		ItemFilterState filterState = pickupManager.filterManager().getState(LootGroupFilter.itemId(selected));
		if (textY + 12 + font.lineHeight <= buttonsY) {
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.loot.total", selected.totalCount()),
				textX,
				textY + 12,
				MUTED_TEXT_COLOR,
				false
			);
		}
		if (textY + 23 + font.lineHeight <= buttonsY) {
			graphics.drawString(font, amount, textX, textY + 23, TEXT_COLOR, false);
		}
		if (textY + 34 + font.lineHeight <= buttonsY) {
			graphics.drawString(
				font,
				Component.translatable(
					"tactical_pickup.loot.filter_state",
					Component.translatable(filterState.translationKey())
				),
				textX,
				textY + 34,
				filterState == ItemFilterState.LOW_PRIORITY ? LOW_PRIORITY_MARKER : MUTED_TEXT_COLOR,
				false
			);
		}

		List<Component> enchantments = ItemDetailHelper.collectEnchantments(minecraft, selected.displayStack());
		if (!enchantments.isEmpty()) {
			int enchantmentY = textY + 46;
			int availableLines = Math.max(0, (buttonsY - enchantmentY - font.lineHeight - 2) / (font.lineHeight + 1));
			int visibleCount = Math.min(
				Math.min(enchantments.size(), ItemDetailHelper.MAX_VISIBLE_ENCHANTMENTS),
				availableLines
			);
			if (visibleCount > 0) {
				graphics.drawString(
					font,
					Component.translatable("tactical_pickup.loot.enchantments"),
					textX,
					enchantmentY,
					TEXT_COLOR,
					false
				);
				for (int index = 0; index < visibleCount; index++) {
					String line = font.plainSubstrByWidth(enchantments.get(index).getString(), textWidth);
					graphics.drawString(
						font,
						line,
						textX,
						enchantmentY + font.lineHeight + 2 + index * (font.lineHeight + 1),
						MUTED_TEXT_COLOR,
						false
					);
				}
			}
		}
	}

	private void renderDragGhost(GuiGraphics graphics, int mouseX, int mouseY) {
		Snapshot snapshot = dragState.snapshot();
		if (snapshot.displayStack().isEmpty()) {
			return;
		}

		Component amount = snapshot instanceof LootSnapshot lootSnapshot
			? lootSnapshot.requestedAmount() == PickupRequestPayload.ALL_ITEMS
				? Component.translatable("tactical_pickup.loot.drag_all")
				: Component.translatable("tactical_pickup.loot.card_count", lootSnapshot.requestedAmount())
			: Component.translatable("tactical_pickup.loot.card_count", snapshot.displayStack().getCount());
		String label = snapshot.displayStack().getHoverName().getString() + " " + amount.getString();
		int ghostWidth = Math.min(190, Math.max(72, font.width(label) + 30));
		int ghostX = Math.max(2, Math.min(mouseX + 10, width - ghostWidth - 2));
		int ghostY = Math.max(2, Math.min(mouseY + 10, height - 24));
		Bounds ghost = new Bounds(ghostX, ghostY, ghostWidth, 22);
		renderRaisedPanel(graphics, ghost);
		graphics.renderItem(snapshot.displayStack().copyWithCount(1), ghostX + 3, ghostY + 3);
		String clipped = font.plainSubstrByWidth(label, ghostWidth - 26);
		graphics.drawString(font, clipped, ghostX + 23, ghostY + 7, TEXT_COLOR, false);
	}

	private static void renderRaisedPanel(GuiGraphics graphics, Bounds bounds) {
		graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), PANEL_COLOR);
		graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, PANEL_HIGHLIGHT_COLOR);
		graphics.fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.bottom(), PANEL_HIGHLIGHT_COLOR);
		graphics.fill(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), PANEL_SHADOW_COLOR);
		graphics.fill(bounds.right() - 1, bounds.y(), bounds.right(), bounds.bottom(), PANEL_SHADOW_COLOR);
		if (bounds.width() > 3 && bounds.height() > 3) {
			graphics.fill(bounds.x() + 1, bounds.bottom() - 2, bounds.right() - 1, bounds.bottom() - 1, PANEL_MID_COLOR);
			graphics.fill(bounds.right() - 2, bounds.y() + 1, bounds.right() - 1, bounds.bottom() - 1, PANEL_MID_COLOR);
		}
	}

	private static void drawBorder(GuiGraphics graphics, Bounds bounds, int color) {
		graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.y() + 1, color);
		graphics.fill(bounds.x(), bounds.bottom() - 1, bounds.right(), bounds.bottom(), color);
		graphics.fill(bounds.x(), bounds.y(), bounds.x() + 1, bounds.bottom(), color);
		graphics.fill(bounds.right() - 1, bounds.y(), bounds.right(), bounds.bottom(), color);
	}

	private static boolean isFullyInside(Bounds outer, Bounds inner) {
		return inner.x() >= outer.x()
			&& inner.y() >= outer.y()
			&& inner.right() <= outer.right()
			&& inner.bottom() <= outer.bottom();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int lootIndex = lootIndexAt(mouseX, mouseY);
		if (button == 1 && hasShiftDown() && lootIndex >= 0) {
			LootGroup group = visibleGroups.get(lootIndex);
			selectionState.select(group);
			updateButtonState();
			pickupManager.requestPickup(group.representativeEntityId(), PickupRequestPayload.ALL_ITEMS);
			return true;
		}

		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}

		if (button == 0 && lootIndex >= 0) {
			LootGroup group = visibleGroups.get(lootIndex);
			selectionState.select(group);
			dragState.pressLoot(group, selectionState.requestedAmount(), mouseX, mouseY);
			updateButtonState();
			return true;
		}

		if (button == 0 && minecraft.player != null) {
			OptionalInt inventorySlot = layout.inventorySlotAt(mouseX, mouseY);
			if (inventorySlot.isPresent()) {
				ItemStack stack = minecraft.player.getInventory().getItem(inventorySlot.getAsInt());
				if (!stack.isEmpty()) {
					dragState.pressInventory(inventorySlot.getAsInt(), stack, mouseX, mouseY);
					return true;
				}
			}
		}

		return false;
	}

	private int lootIndexAt(double mouseX, double mouseY) {
		if (!layout.lootViewport().contains(mouseX, mouseY)) {
			return -1;
		}

		for (int index = 0; index < visibleGroups.size(); index++) {
			Bounds slotBounds = layout.lootSlotBounds(index, scrollOffset);
			if (slotBounds.intersects(layout.lootViewport()) && slotBounds.contains(mouseX, mouseY)) {
				return index;
			}
		}

		return -1;
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
			Snapshot snapshot = dragState.finish();
			if (snapshot instanceof LootSnapshot lootSnapshot && minecraft.player != null) {
				OptionalInt targetSlot = layout.inventorySlotAt(mouseX, mouseY);
				if (targetSlot.isPresent()
						&& canAcceptLoot(minecraft.player.getInventory().getItem(targetSlot.getAsInt()), lootSnapshot.displayStack())) {
					pickupManager.requestPickupToSlot(
						lootSnapshot.representativeEntityId(),
						lootSnapshot.requestedAmount(),
						targetSlot.getAsInt()
					);
				}
			} else if (snapshot instanceof InventorySnapshot inventorySnapshot
					&& layout.lootPanel().contains(mouseX, mouseY)) {
				pickupManager.requestDropInventorySlot(inventorySnapshot.sourceSlot());
			}
			return true;
		}

		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (layout.lootPanel().contains(mouseX, mouseY)) {
			scrollOffset = layout.clampScroll(
				scrollOffset - vertical * LootScreenLayout.SLOT_SIZE,
				visibleGroups.size()
			);
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
		hoveredTooltipStack = ItemStack.EMPTY;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
