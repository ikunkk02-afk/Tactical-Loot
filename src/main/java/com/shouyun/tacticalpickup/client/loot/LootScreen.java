package com.shouyun.tacticalpickup.client.loot;

import com.shouyun.tacticalpickup.client.config.ClientUiConfigManager;
import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.loot.LootScreenDragState.InventorySnapshot;
import com.shouyun.tacticalpickup.client.loot.LootScreenDragState.LootSnapshot;
import com.shouyun.tacticalpickup.client.loot.LootScreenDragState.Snapshot;
import com.shouyun.tacticalpickup.client.loot.LootScreenLayout.Bounds;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.ui.ItemDetailHelper;
import com.shouyun.tacticalpickup.client.ui.PixelButton;
import com.shouyun.tacticalpickup.client.ui.PixelTheme;
import com.shouyun.tacticalpickup.client.ui.animation.AnimatedFloat;
import com.shouyun.tacticalpickup.client.ui.animation.Easing;
import com.shouyun.tacticalpickup.client.ui.animation.GuiAnimation;
import com.shouyun.tacticalpickup.client.ui.layout.UiElement;
import com.shouyun.tacticalpickup.client.ui.layout.UiPlacement;
import com.shouyun.tacticalpickup.client.ui.layout.UiRect;
import com.shouyun.tacticalpickup.client.ui.layout.UiTransform;
import com.shouyun.tacticalpickup.client.ui.layout.UiTransform.UiPoint;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import com.shouyun.tacticalpickup.pickup.PickupConstants;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class LootScreen extends Screen {
	private static final int OPEN_DURATION_MS = 200;
	private static final int CLOSE_DURATION_MS = 140;
	private static final int SECTION_DURATION_MS = 110;
	private static final int INVENTORY_DELAY_MS = 30;
	private static final int LOOT_DELAY_MS = 60;
	private static final int DETAIL_DELAY_MS = 90;
	private static final int HOVER_IN_MS = 100;
	private static final int HOVER_OUT_MS = 80;
	private static final int SELECTION_DURATION_MS = 140;
	private static final int QUANTITY_PULSE_MS = 100;
	private static final int PICKUP_FLIGHT_MS = 160;
	private static final int DROP_FLIGHT_MS = 140;
	private static final int TARGET_PULSE_MS = 150;
	private static final int INVALID_PULSE_MS = 140;

	private final ClientPickupManager pickupManager = ClientPickupManager.getInstance();
	private final LootSelectionState selectionState = new LootSelectionState();
	private final LootScreenDragState dragState = new LootScreenDragState();
	private final AnimatedFloat[] inventoryHover = animations(Inventory.INVENTORY_SIZE);
	private final AnimatedFloat[] lootHover = animations(PickupConstants.MAX_SCANNED_ENTITIES);
	private final long[] inventoryPulseStart = new long[Inventory.INVENTORY_SIZE];

	private List<LootGroup> visibleGroups = List.of();
	private List<LootGroup> sourceGroups = List.of();
	private List<Component> cachedEnchantments = List.of();
	private LootGroupKey cachedEnchantmentKey;
	private LootGroupKey animatedSelectionKey;
	private LootScreenLayout layout;
	private EditBox searchBox;
	private PixelButton closeButton;
	private PixelButton minusSixteenButton;
	private PixelButton minusOneButton;
	private PixelButton allButton;
	private PixelButton plusOneButton;
	private PixelButton plusSixteenButton;
	private PixelButton pickupButton;
	private Bounds quantityDisplayBounds = Bounds.EMPTY;
	private LocalPlayer openedPlayer;
	private ResourceKey<Level> openedDimension;
	private String cachedQuery = "";
	private double scrollOffset;
	private ItemStack hoveredTooltipStack = ItemStack.EMPTY;
	private long openedAtNanos = System.nanoTime();
	private long closeStartedAtNanos;
	private long selectionStartedAtNanos;
	private long quantityPulseStartedAtNanos = Long.MIN_VALUE;
	private long invalidPulseStartedAtNanos = Long.MIN_VALUE;
	private int previousSelectedTotal = -1;
	private boolean closing;

	private ItemStack flightStack = ItemStack.EMPTY;
	private long flightStartedAtNanos;
	private int flightDurationMillis;
	private Easing flightEasing = Easing.IN_OUT_CUBIC;
	private float flightStartX;
	private float flightStartY;
	private float flightEndX;
	private float flightEndY;
	private int flightTargetSlot = -1;

	private boolean dragTrailInitialized;
	private double dragTrailX;
	private double dragTrailY;
	private long previousRenderNanos;

	public LootScreen() {
		super(Component.translatable("tactical_pickup.loot.title"));
	}

	public static void closeIfOpen(Minecraft client) {
		if (client.screen instanceof LootScreen screen) {
			screen.forceClose();
		}
	}

	@Override
	protected void init() {
		String previousQuery = searchBox == null ? cachedQuery : searchBox.getValue();
		if (openedPlayer == null && minecraft.player != null && minecraft.level != null) {
			openedPlayer = minecraft.player;
			openedDimension = minecraft.level.dimension();
		}

		layout = LootScreenLayout.calculate(width, height);
		Bounds searchBounds = layout.searchBox();
		searchBox = new EditBox(
			font,
			searchBounds.x() + 3,
			searchBounds.y() + 2,
			Math.max(1, searchBounds.width() - 6),
			Math.max(1, searchBounds.height() - 4),
			Component.translatable("tactical_pickup.loot.search")
		);
		searchBox.setBordered(false);
		searchBox.setHint(Component.empty());
		searchBox.setMaxLength(128);
		searchBox.setValue(previousQuery);
		searchBox.setResponder(query -> refreshVisibleGroups(true));
		searchBox.setFocused(false);
		addRenderableWidget(searchBox);

		Bounds closeBounds = layout.closeButton();
		closeButton = addRenderableWidget(new PixelButton(
			closeBounds.x(),
			closeBounds.y(),
			closeBounds.width(),
			closeBounds.height(),
			Component.literal("×"),
			button -> beginClose()
		));
		createDetailButtons();
		refreshVisibleGroups(true);
		setFocused(null);
	}

	private void createDetailButtons() {
		Bounds action = layout.actionPanel();
		int buttonRowY = layout.stacked() ? action.y() + 3 : action.y() + 22;
		int secondRowY = layout.stacked() ? action.y() + 25 : action.y() + 48;
		int rowWidth = Math.min(160, Math.max(108, action.width() - 10));
		int rowX = action.x() + (action.width() - rowWidth) / 2;
		int gap = 2;
		int smallOuter = Math.max(20, Math.min(30, (rowWidth - 64) / 4));
		int smallInner = Math.max(18, smallOuter - 8);
		int displayWidth = Math.max(36, rowWidth - smallOuter * 2 - smallInner * 2 - gap * 4);

		int x = rowX;
		minusSixteenButton = addRenderableWidget(quantityButton(Component.literal("−16"), x, buttonRowY, smallOuter, -1, 16));
		x += smallOuter + gap;
		minusOneButton = addRenderableWidget(quantityButton(Component.literal("−1"), x, buttonRowY, smallInner, -1, 1));
		x += smallInner + gap;
		quantityDisplayBounds = new Bounds(x, buttonRowY, displayWidth, 18);
		x += displayWidth + gap;
		plusOneButton = addRenderableWidget(quantityButton(Component.literal("+1"), x, buttonRowY, smallInner, 1, 1));
		x += smallInner + gap;
		plusSixteenButton = addRenderableWidget(quantityButton(Component.literal("+16"), x, buttonRowY, smallOuter, 1, 16));

		int secondGap = 4;
		int allWidth = Math.max(44, rowWidth * 2 / 5);
		int pickupWidth = rowWidth - allWidth - secondGap;
		allButton = addRenderableWidget(new PixelButton(
			rowX,
			secondRowY,
			allWidth,
			20,
			Component.translatable("tactical_pickup.loot.amount_all_button"),
			button -> {
				selectionState.resetAmount();
				quantityPulseStartedAtNanos = System.nanoTime();
				updateButtonState();
			}
		));
		pickupButton = addRenderableWidget(new PixelButton(
			rowX + allWidth + secondGap,
			secondRowY,
			pickupWidth,
			20,
			Component.translatable("tactical_pickup.loot.pickup"),
			button -> pickupSelected()
		).primary(true));
		updateButtonState();
	}

	private PixelButton quantityButton(Component label, int x, int y, int width, int steps, int amountPerStep) {
		return new PixelButton(x, y, width, 18, label, button -> {
			LootGroup selected = selectedGroup();
			if (selected != null) {
				selectionState.adjust(steps, amountPerStep, selected.totalCount());
				quantityPulseStartedAtNanos = System.nanoTime();
				updateButtonState();
			}
		});
	}

	private static AnimatedFloat[] animations(int size) {
		AnimatedFloat[] animations = new AnimatedFloat[size];
		for (int index = 0; index < animations.length; index++) {
			animations[index] = new AnimatedFloat(0.0F);
		}
		return animations;
	}

	@Override
	public void tick() {
		if (minecraft.player == null
				|| minecraft.level == null
				|| !minecraft.player.isAlive()
				|| minecraft.player != openedPlayer
				|| !minecraft.level.dimension().equals(openedDimension)) {
			forceClose();
			return;
		}

		refreshVisibleGroups(false);
	}

	private void refreshVisibleGroups(boolean force) {
		if (searchBox == null || layout == null) {
			return;
		}

		String query = searchBox.getValue();
		List<LootGroup> groups = pickupManager.groups();
		if (!force && groups == sourceGroups && query.equals(cachedQuery)) {
			return;
		}

		sourceGroups = groups;
		cachedQuery = query;
		visibleGroups = groups.stream()
			.filter(group -> LootSearchMatcher.matches(
				group.displayStack().getHoverName().getString(),
				LootGroupFilter.itemId(group),
				query
			))
			.toList();
		LootGroup selected = selectionState.reconcile(visibleGroups);
		scrollOffset = layout.clampScroll(scrollOffset, visibleGroups.size());
		if (selected == null) {
			previousSelectedTotal = -1;
			cachedEnchantmentKey = null;
			cachedEnchantments = List.of();
		} else if (previousSelectedTotal >= 0 && previousSelectedTotal != selected.totalCount()) {
			quantityPulseStartedAtNanos = System.nanoTime();
			previousSelectedTotal = selected.totalCount();
		}
		updateButtonState();
	}

	private LootGroup selectedGroup() {
		return selectionState.reconcile(visibleGroups);
	}

	private void selectGroup(LootGroup group, long nowNanos) {
		if (!group.key().equals(selectionState.selectedKey())) {
			animatedSelectionKey = group.key();
			selectionStartedAtNanos = nowNanos;
			quantityPulseStartedAtNanos = nowNanos;
			cachedEnchantmentKey = null;
		}
		selectionState.select(group);
		previousSelectedTotal = group.totalCount();
		updateButtonState();
	}

	private void updateButtonState() {
		LootGroup selected = selectionState.reconcile(visibleGroups);
		boolean active = selected != null && !closing;
		if (minusSixteenButton != null) {
			minusSixteenButton.active = active;
			minusOneButton.active = active;
			allButton.active = active && !selectionState.pickupAll();
			plusOneButton.active = active && !selectionState.pickupAll();
			plusSixteenButton.active = active && !selectionState.pickupAll();
			pickupButton.active = active;
			closeButton.active = !closing;
		}
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		if (closing && GuiAnimation.progress(now, closeStartedAtNanos, CLOSE_DURATION_MS) >= 1.0F) {
			finishClose();
			return;
		}

		hoveredTooltipStack = ItemStack.EMPTY;
		float visibility = screenVisibility(now);
		float closeProgress = closing
			? Easing.IN_CUBIC.apply(GuiAnimation.progress(now, closeStartedAtNanos, CLOSE_DURATION_MS))
			: 0.0F;
		graphics.fill(0, 0, width, height, PixelTheme.color(PixelTheme.WORLD_DIM, visibility));
		UiTransform uiTransform = uiTransform();
		UiPoint localMouse = uiTransform.screenToLocal(mouseX, mouseY);
		int localMouseX = (int) Math.round(localMouse.x());
		int localMouseY = (int) Math.round(localMouse.y());
		graphics.pose().pushPose();
		uiTransform.apply(graphics);
		renderOuterPanel(graphics, now, visibility, closeProgress);

		float inventoryProgress = sectionProgress(now, INVENTORY_DELAY_MS);
		float lootProgress = sectionProgress(now, LOOT_DELAY_MS);
		float detailProgress = sectionProgress(now, DETAIL_DELAY_MS);
		int inventoryY = sectionOffset(inventoryProgress, closeProgress);
		int lootY = sectionOffset(lootProgress, closeProgress);
		int detailY = sectionOffset(detailProgress, closeProgress);
		float inventoryOpacity = visibility * inventoryProgress;
		float lootOpacity = visibility * lootProgress;
		float detailOpacity = visibility * detailProgress;

		renderHeader(graphics, visibility);
		Bounds inventoryPanel = layout.inventoryPanel().offset(0, inventoryY);
		Bounds lootPanel = layout.lootPanel().offset(0, lootY);
		Bounds detailPanel = layout.detailPanel().offset(0, detailY);
		PixelTheme.drawPanel(graphics, inventoryPanel.x(), inventoryPanel.y(), inventoryPanel.width(), inventoryPanel.height(), inventoryOpacity);
		PixelTheme.drawPanel(graphics, lootPanel.x(), lootPanel.y(), lootPanel.width(), lootPanel.height(), lootOpacity);
		PixelTheme.drawPanel(graphics, detailPanel.x(), detailPanel.y(), detailPanel.width(), detailPanel.height(), detailOpacity);

		renderInventoryPanel(graphics, localMouseX, localMouseY, inventoryY, inventoryOpacity, now);
		renderLootPanel(graphics, localMouseX, localMouseY, lootY, lootOpacity, now);
		renderDetailPanel(graphics, detailY, detailOpacity, now);
		updateWidgetPresentation(lootY, detailY, visibility, lootOpacity, detailOpacity);
		super.render(graphics, localMouseX, localMouseY, partialTick);

		renderFlight(graphics, now, visibility);
		if (dragState.isDragging()) {
			renderDragGhost(graphics, localMouseX, localMouseY, now, visibility);
		} else {
			dragTrailInitialized = false;
		}
		graphics.pose().popPose();
		if (!dragState.isDragging() && !hoveredTooltipStack.isEmpty() && !closing) {
			graphics.renderTooltip(font, hoveredTooltipStack, mouseX, mouseY);
		}
		previousRenderNanos = now;
	}

	private UiTransform uiTransform() {
		Bounds panel = layout.panel();
		UiRect bounds = new UiRect(panel.x(), panel.y(), panel.width(), panel.height());
		UiPlacement placement = ClientUiConfigManager.getInstance().placement(UiElement.LOOT_SCREEN);
		return UiTransform.create(
			bounds,
			placement.desiredCenterX(bounds.centerX(), width),
			placement.desiredCenterY(bounds.centerY(), height),
			placement.scale(),
			width,
			height
		);
	}

	@Override
	public void renderBackground(GuiGraphics graphics) {
		// The dark translucent world dim is rendered explicitly in render().
	}

	private void renderOuterPanel(GuiGraphics graphics, long now, float visibility, float closeProgress) {
		float openProgress = Easing.OUT_QUART.apply(GuiAnimation.progress(now, openedAtNanos, OPEN_DURATION_MS));
		float scale = closing
			? GuiAnimation.lerp(1.0F, 0.98F, closeProgress)
			: GuiAnimation.lerp(0.96F, 1.0F, openProgress);
		float yOffset = closing
			? GuiAnimation.lerp(0.0F, 3.0F, closeProgress)
			: GuiAnimation.lerp(6.0F, 0.0F, openProgress);
		Bounds panel = layout.panel();
		float centerX = panel.x() + panel.width() / 2.0F;
		float centerY = panel.y() + panel.height() / 2.0F;
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, centerY + yOffset, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-centerX, -centerY, 0.0F);
		PixelTheme.drawPanel(graphics, panel.x(), panel.y(), panel.width(), panel.height(), visibility);
		graphics.pose().popPose();
	}

	private void renderHeader(GuiGraphics graphics, float opacity) {
		Bounds panel = layout.panel();
		graphics.drawString(font, title, panel.x() + 9, panel.y() + 8, PixelTheme.color(PixelTheme.TEXT, opacity), false);
		Component count = Component.translatable("tactical_pickup.loot.group_count", pickupManager.groups().size());
		int countRight = layout.closeButton().x() - 5;
		graphics.drawString(
			font,
			count,
			countRight - font.width(count),
			panel.y() + 8,
			PixelTheme.color(PixelTheme.MUTED_TEXT, opacity),
			false
		);
		graphics.fill(
			panel.x() + 7,
			panel.y() + 20,
			panel.right() - 7,
			panel.y() + 21,
			PixelTheme.color(PixelTheme.EDGE_MID, opacity * 0.65F)
		);
	}

	private void renderInventoryPanel(
			GuiGraphics graphics,
			int mouseX,
			int mouseY,
			int yOffset,
			float opacity,
			long now
	) {
		Bounds panel = layout.inventoryPanel().offset(0, yOffset);
		graphics.drawString(
			font,
			Component.translatable("tactical_pickup.loot.inventory"),
			panel.x() + 6,
			panel.y() + 5,
			PixelTheme.color(PixelTheme.TEXT, opacity),
			false
		);
		if (minecraft.player == null) {
			return;
		}

		Inventory inventory = minecraft.player.getInventory();
		OptionalInt hoveredSlot = layout.inventorySlotAt(mouseX, mouseY - yOffset);
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			Bounds slotBounds = layout.inventorySlotBounds(slot).offset(0, yOffset);
			if (!isFullyInside(panel, slotBounds)) {
				continue;
			}

			boolean hovered = hoveredSlot.isPresent() && hoveredSlot.getAsInt() == slot && !dragState.isDragging();
			inventoryHover[slot].setTarget(hovered ? 1.0F : 0.0F, hovered ? HOVER_IN_MS : HOVER_OUT_MS, Easing.OUT_CUBIC, now);
			float hover = inventoryHover[slot].value(now);
			PixelTheme.drawSlot(graphics, slotBounds.x(), slotBounds.y(), slotBounds.width(), slotBounds.height(), hover, opacity);
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				renderItem(graphics, stack, slotBounds.x() + 1, slotBounds.y() + 1, 1.0F, opacity);
				graphics.renderItemDecorations(font, stack, slotBounds.x() + 1, slotBounds.y() + 1);
			}

			if (hoveredSlot.isPresent() && hoveredSlot.getAsInt() == slot) {
				if (dragState.isDragging() && dragState.snapshot() instanceof LootSnapshot lootSnapshot) {
					boolean compatible = canAcceptLoot(stack, lootSnapshot.displayStack());
					PixelTheme.drawBorder(
						graphics,
						slotBounds.x(),
						slotBounds.y(),
						slotBounds.width(),
						slotBounds.height(),
						compatible ? PixelTheme.COMPATIBLE : PixelTheme.INCOMPATIBLE,
						opacity
					);
					if (!compatible) {
						graphics.drawCenteredString(
							font,
							Component.literal("×"),
							slotBounds.x() + slotBounds.width() / 2,
							slotBounds.y() + 5,
							PixelTheme.color(PixelTheme.INCOMPATIBLE, opacity)
						);
					}
				} else if (!dragState.isDragging() && !stack.isEmpty()) {
					hoveredTooltipStack = stack;
				}
			}

			float pulse = targetPulse(slot, now);
			if (pulse > 0.0F) {
				PixelTheme.drawBorder(
					graphics,
					slotBounds.x() - 1,
					slotBounds.y() - 1,
					slotBounds.width() + 2,
					slotBounds.height() + 2,
					PixelTheme.ACCENT_BRIGHT,
					opacity * pulse
				);
			}
		}
	}

	private void renderLootPanel(
			GuiGraphics graphics,
			int mouseX,
			int mouseY,
			int yOffset,
			float opacity,
			long now
	) {
		Bounds panel = layout.lootPanel().offset(0, yOffset);
		Bounds viewport = layout.lootViewport().offset(0, yOffset);
		boolean inventoryDropTarget = dragState.isDragging()
			&& dragState.snapshot() instanceof InventorySnapshot
			&& panel.contains(mouseX, mouseY);
		Component heading = inventoryDropTarget
			? Component.translatable("tactical_pickup.loot.world_drop_hint")
			: Component.translatable("tactical_pickup.loot.nearby");
		graphics.drawString(
			font,
			heading,
			panel.x() + 6,
			panel.y() + 6,
			PixelTheme.color(inventoryDropTarget ? PixelTheme.ACCENT_BRIGHT : PixelTheme.TEXT, opacity),
			false
		);

		Bounds searchBounds = layout.searchBox().offset(0, yOffset);
		PixelTheme.drawInset(graphics, searchBounds.x(), searchBounds.y(), searchBounds.width(), searchBounds.height(), opacity);
		if (searchBox.getValue().isBlank() && !searchBox.isFocused()) {
			Component hint = Component.translatable("tactical_pickup.loot.search");
			String clipped = font.plainSubstrByWidth(hint.getString(), Math.max(1, searchBounds.width() - 7));
			graphics.drawString(
				font,
				clipped,
				searchBounds.x() + 4,
				searchBounds.y() + 3,
				PixelTheme.color(PixelTheme.FAINT_TEXT, opacity),
				false
			);
		}
		if (searchBox.isFocused()) {
			PixelTheme.drawBorder(graphics, searchBounds.x(), searchBounds.y(), searchBounds.width(), searchBounds.height(), PixelTheme.ACCENT, opacity * 0.65F);
		}

		if (visibleGroups.isEmpty()) {
			Component empty = searchBox.getValue().isBlank()
				? Component.translatable("tactical_pickup.loot.empty")
				: Component.translatable("tactical_pickup.loot.no_results");
			String clipped = font.plainSubstrByWidth(empty.getString(), Math.max(1, viewport.width() - 4));
			graphics.drawCenteredString(
				font,
				clipped,
				viewport.x() + viewport.width() / 2,
				viewport.y() + Math.max(2, viewport.height() / 2 - font.lineHeight / 2),
				PixelTheme.color(PixelTheme.MUTED_TEXT, opacity)
			);
		} else {
			graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
			for (int index = 0; index < visibleGroups.size() && index < lootHover.length; index++) {
				Bounds slotBounds = layout.lootSlotBounds(index, scrollOffset).offset(0, yOffset);
				if (!slotBounds.intersects(viewport)) {
					lootHover[index].setTarget(0.0F, HOVER_OUT_MS, Easing.OUT_CUBIC, now);
					continue;
				}

				LootGroup group = visibleGroups.get(index);
				ItemFilterState state = pickupManager.filterManager().getState(LootGroupFilter.itemId(group));
				boolean selected = group.key().equals(selectionState.selectedKey());
				boolean hovered = viewport.contains(mouseX, mouseY) && slotBounds.contains(mouseX, mouseY) && !dragState.isDragging();
				lootHover[index].setTarget(hovered ? 1.0F : 0.0F, hovered ? HOVER_IN_MS : HOVER_OUT_MS, Easing.OUT_CUBIC, now);
				float hover = lootHover[index].value(now);
				PixelTheme.drawSlot(graphics, slotBounds.x(), slotBounds.y(), slotBounds.width(), slotBounds.height(), hover, opacity);
				float itemScale = selected ? selectionItemScale(group.key(), now) : 1.0F;
				renderItem(graphics, group.displayStack(), slotBounds.x() + 3, slotBounds.y() + 3, itemScale, opacity);
				graphics.renderItemDecorations(
					font,
					group.displayStack(),
					slotBounds.x() + 3,
					slotBounds.y() + 3,
					formatSlotCount(group.totalCount())
				);

				if (state == ItemFilterState.LOW_PRIORITY) {
					graphics.fill(
						slotBounds.x() + 2,
						slotBounds.y() + 2,
						slotBounds.right() - 2,
						slotBounds.bottom() - 2,
						PixelTheme.color(0x58000000, opacity)
					);
					graphics.fill(
						slotBounds.x() + 3,
						slotBounds.y() + 3,
						slotBounds.x() + 7,
						slotBounds.y() + 4,
						PixelTheme.color(PixelTheme.LOW_PRIORITY, opacity)
					);
				}
				if (selected) {
					float selectionOpacity = selectionBorderOpacity(group.key(), now);
					PixelTheme.drawBorder(
						graphics,
						slotBounds.x(),
						slotBounds.y(),
						slotBounds.width(),
						slotBounds.height(),
						PixelTheme.ACCENT_BRIGHT,
						opacity * selectionOpacity
					);
				}
				if (hovered) {
					hoveredTooltipStack = group.displayStack();
				}
			}
			graphics.disableScissor();
			renderScrollBar(graphics, yOffset, opacity);
		}

		if (inventoryDropTarget) {
			graphics.fill(
				panel.x() + 2,
				panel.y() + 20,
				panel.right() - 2,
				panel.bottom() - 2,
				PixelTheme.color(0x306F765F, opacity)
			);
			PixelTheme.drawBorder(graphics, panel.x(), panel.y(), panel.width(), panel.height(), PixelTheme.COMPATIBLE, opacity);
		}
	}

	private void renderScrollBar(GuiGraphics graphics, int yOffset, float opacity) {
		double maxScroll = layout.maxScroll(visibleGroups.size());
		if (maxScroll <= 0.0D) {
			return;
		}

		Bounds viewport = layout.lootViewport().offset(0, yOffset);
		int barX = Math.min(layout.lootPanel().right() - 4, viewport.right() + 2);
		int thumbHeight = Math.max(10, (int) Math.round(
			viewport.height() * (viewport.height() / (viewport.height() + maxScroll))
		));
		int travel = Math.max(1, viewport.height() - thumbHeight);
		int thumbY = viewport.y() + (int) Math.round(travel * (scrollOffset / maxScroll));
		graphics.fill(barX, viewport.y(), barX + 2, viewport.bottom(), PixelTheme.color(PixelTheme.EDGE_DARK, opacity));
		graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, PixelTheme.color(PixelTheme.EDGE_LIGHT, opacity));
	}

	private void renderDetailPanel(GuiGraphics graphics, int yOffset, float opacity, long now) {
		Bounds detail = layout.detailPanel().offset(0, yOffset);
		Bounds action = layout.actionPanel().offset(0, yOffset);
		if (!layout.stacked()) {
			PixelTheme.drawInset(graphics, action.x(), action.y(), action.width(), action.height(), opacity * 0.9F);
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.loot.amount_label"),
				action.x() + 7,
				action.y() + 7,
				PixelTheme.color(PixelTheme.MUTED_TEXT, opacity),
				false
			);
		}

		LootGroup selected = selectedGroup();
		if (selected == null) {
			if (!layout.stacked()) {
				graphics.drawString(
					font,
					Component.translatable("tactical_pickup.loot.select_hint"),
					detail.x() + 8,
					detail.y() + 9,
					PixelTheme.color(PixelTheme.MUTED_TEXT, opacity),
					false
				);
				graphics.drawString(
					font,
					Component.translatable("tactical_pickup.loot.drag_hint"),
					detail.x() + 8,
					detail.y() + 23,
					PixelTheme.color(PixelTheme.FAINT_TEXT, opacity),
					false
				);
			}
			renderQuantityDisplay(graphics, null, yOffset, opacity, now);
			return;
		}

		if (!layout.stacked()) {
			Bounds textPanel = layout.detailTextPanel().offset(0, yOffset);
			renderItem(graphics, selected.displayStack(), textPanel.x() + 5, textPanel.y() + 5, 2.0F, opacity);
			int textX = textPanel.x() + 43;
			int textWidth = Math.max(1, textPanel.right() - textX - 3);
			String name = font.plainSubstrByWidth(selected.displayStack().getHoverName().getString(), textWidth);
			graphics.drawString(font, name, textX, textPanel.y() + 5, PixelTheme.color(PixelTheme.TEXT, opacity), false);
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.loot.total", selected.totalCount()),
				textX,
				textPanel.y() + 17,
				PixelTheme.color(PixelTheme.MUTED_TEXT, opacity),
				false
			);
			String itemId = BuiltInRegistries.ITEM.getKey(selected.displayStack().getItem()).toString();
			graphics.drawString(
				font,
				font.plainSubstrByWidth(itemId, textWidth),
				textX,
				textPanel.y() + 29,
				PixelTheme.color(PixelTheme.FAINT_TEXT, opacity),
				false
			);
			ItemFilterState filterState = pickupManager.filterManager().getState(LootGroupFilter.itemId(selected));
			graphics.drawString(
				font,
				Component.translatable(
					"tactical_pickup.loot.filter_state",
					Component.translatable(filterState.translationKey())
				),
				textX,
				textPanel.y() + 41,
				PixelTheme.color(filterState == ItemFilterState.LOW_PRIORITY ? PixelTheme.LOW_PRIORITY : PixelTheme.MUTED_TEXT, opacity),
				false
			);

			List<Component> enchantments = enchantments(selected);
			if (!enchantments.isEmpty()) {
				graphics.drawString(
					font,
					Component.translatable("tactical_pickup.loot.enchantments"),
					textPanel.x() + 5,
					textPanel.y() + 51,
					PixelTheme.color(PixelTheme.ACCENT, opacity),
					false
				);
				int visible = Math.min(enchantments.size(), ItemDetailHelper.MAX_VISIBLE_ENCHANTMENTS);
				int enchantmentWidth = Math.max(1, textPanel.width() - 10);
				for (int index = 0; index < visible; index++) {
					String line = font.plainSubstrByWidth(enchantments.get(index).getString(), enchantmentWidth);
					graphics.drawString(
						font,
						line,
						textPanel.x() + 5,
						textPanel.y() + 61 + index * 9,
						PixelTheme.color(PixelTheme.MUTED_TEXT, opacity),
						false
					);
				}
			}
		}

		renderQuantityDisplay(graphics, selected, yOffset, opacity, now);
	}

	private List<Component> enchantments(LootGroup group) {
		if (!group.key().equals(cachedEnchantmentKey)) {
			cachedEnchantmentKey = group.key();
			cachedEnchantments = ItemDetailHelper.collectEnchantments(minecraft, group.displayStack());
		}
		return cachedEnchantments;
	}

	private void renderQuantityDisplay(GuiGraphics graphics, LootGroup selected, int yOffset, float opacity, long now) {
		Bounds display = quantityDisplayBounds.offset(0, yOffset);
		PixelTheme.drawInset(graphics, display.x(), display.y(), display.width(), display.height(), opacity);
		Component amount = selected == null
			? Component.literal("—")
			: selectionState.pickupAll()
				? Component.translatable("tactical_pickup.loot.amount_all_compact", selected.totalCount())
				: Component.translatable(
					"tactical_pickup.loot.amount_compact",
					selectionState.selectedAmount(selected.totalCount()),
					selected.totalCount()
				);
		float pulse = quantityPulse(now);
		float scale = 1.0F + 0.15F * pulse;
		float centerX = display.x() + display.width() / 2.0F;
		float centerY = display.y() + display.height() / 2.0F;
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, centerY, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-centerX, -centerY, 0.0F);
		graphics.drawCenteredString(
			font,
			amount,
			display.x() + display.width() / 2,
			display.y() + 5,
			PixelTheme.color(pulse > 0.0F ? PixelTheme.ACCENT_BRIGHT : PixelTheme.TEXT, opacity)
		);
		graphics.pose().popPose();
	}

	private void updateWidgetPresentation(
			int lootY,
			int detailY,
			float visibility,
			float lootOpacity,
			float detailOpacity
	) {
		Bounds searchBounds = layout.searchBox().offset(0, lootY);
		searchBox.setX(searchBounds.x() + 3);
		searchBox.setY(searchBounds.y() + 2);
		searchBox.setTextColor(PixelTheme.color(PixelTheme.TEXT, lootOpacity));
		searchBox.visible = lootOpacity > 0.03F;
		searchBox.active = !closing;

		Bounds closeBounds = layout.closeButton();
		closeButton.setX(closeBounds.x());
		closeButton.setY(closeBounds.y());
		closeButton.setVisualOpacity(visibility);
		closeButton.visible = visibility > 0.03F;

		Bounds action = layout.actionPanel();
		int buttonRowY = layout.stacked() ? action.y() + 3 : action.y() + 22;
		int secondRowY = layout.stacked() ? action.y() + 25 : action.y() + 48;
		positionButton(minusSixteenButton, buttonRowY + detailY, detailOpacity);
		positionButton(minusOneButton, buttonRowY + detailY, detailOpacity);
		positionButton(plusOneButton, buttonRowY + detailY, detailOpacity);
		positionButton(plusSixteenButton, buttonRowY + detailY, detailOpacity);
		positionButton(allButton, secondRowY + detailY, detailOpacity);
		positionButton(pickupButton, secondRowY + detailY, detailOpacity);
	}

	private static void positionButton(PixelButton button, int y, float opacity) {
		button.setY(y);
		button.setVisualOpacity(opacity);
		button.visible = opacity > 0.03F;
	}

	private float screenVisibility(long now) {
		if (closing) {
			return 1.0F - Easing.IN_CUBIC.apply(GuiAnimation.progress(now, closeStartedAtNanos, CLOSE_DURATION_MS));
		}
		return Easing.OUT_CUBIC.apply(GuiAnimation.progress(now, openedAtNanos, OPEN_DURATION_MS));
	}

	private float sectionProgress(long now, int delayMillis) {
		if (closing) {
			return 1.0F;
		}
		return GuiAnimation.delayedProgress(now, openedAtNanos, delayMillis, SECTION_DURATION_MS, Easing.OUT_CUBIC);
	}

	private static int sectionOffset(float progress, float closeProgress) {
		return Math.round((1.0F - progress) * 4.0F + closeProgress * 3.0F);
	}

	private float selectionBorderOpacity(LootGroupKey key, long now) {
		if (!key.equals(animatedSelectionKey)) {
			return 1.0F;
		}
		return Easing.OUT_CUBIC.apply(GuiAnimation.progress(now, selectionStartedAtNanos, 120));
	}

	private float selectionItemScale(LootGroupKey key, long now) {
		if (!key.equals(animatedSelectionKey)) {
			return 1.0F;
		}
		float progress = GuiAnimation.progress(now, selectionStartedAtNanos, SELECTION_DURATION_MS);
		if (progress < 0.5F) {
			return GuiAnimation.lerp(1.0F, 1.08F, Easing.OUT_BACK.apply(progress * 2.0F));
		}
		return GuiAnimation.lerp(1.08F, 1.0F, Easing.OUT_CUBIC.apply((progress - 0.5F) * 2.0F));
	}

	private float quantityPulse(long now) {
		if (quantityPulseStartedAtNanos == Long.MIN_VALUE) {
			return 0.0F;
		}
		float progress = GuiAnimation.progress(now, quantityPulseStartedAtNanos, QUANTITY_PULSE_MS);
		if (progress >= 1.0F) {
			quantityPulseStartedAtNanos = Long.MIN_VALUE;
			return 0.0F;
		}
		return progress < 0.5F ? Easing.OUT_CUBIC.apply(progress * 2.0F) : 1.0F - Easing.IN_CUBIC.apply((progress - 0.5F) * 2.0F);
	}

	private float targetPulse(int slot, long now) {
		long start = inventoryPulseStart[slot];
		if (start == 0L) {
			return 0.0F;
		}
		float progress = GuiAnimation.progress(now, start, TARGET_PULSE_MS);
		if (progress >= 1.0F) {
			inventoryPulseStart[slot] = 0L;
			return 0.0F;
		}
		return 1.0F - Easing.OUT_CUBIC.apply(progress);
	}

	private float invalidPulse(long now) {
		if (invalidPulseStartedAtNanos == Long.MIN_VALUE) {
			return 0.0F;
		}
		float progress = GuiAnimation.progress(now, invalidPulseStartedAtNanos, INVALID_PULSE_MS);
		if (progress >= 1.0F) {
			invalidPulseStartedAtNanos = Long.MIN_VALUE;
			return 0.0F;
		}
		return 1.0F - progress;
	}

	private void renderItem(
			GuiGraphics graphics,
			ItemStack stack,
			int x,
			int y,
			float scale,
			float opacity
	) {
		if (stack.isEmpty() || opacity <= 0.0F) {
			return;
		}

		graphics.setColor(1.0F, 1.0F, 1.0F, Easing.clamp(opacity));
		if (Float.compare(scale, 1.0F) == 0) {
			graphics.renderItem(stack, x, y);
		} else if (scale >= 1.5F) {
			graphics.pose().pushPose();
			graphics.pose().translate(x, y, 0.0F);
			graphics.pose().scale(scale, scale, 1.0F);
			graphics.renderItem(stack, 0, 0);
			graphics.pose().popPose();
		} else {
			graphics.pose().pushPose();
			graphics.pose().translate(x + 8.0F, y + 8.0F, 0.0F);
			graphics.pose().scale(scale, scale, 1.0F);
			graphics.pose().translate(-(x + 8.0F), -(y + 8.0F), 0.0F);
			graphics.renderItem(stack, x, y);
			graphics.pose().popPose();
		}
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void pickupSelected() {
		LootGroup selected = selectedGroup();
		if (selected == null) {
			return;
		}

		long now = System.nanoTime();
		Bounds source = selectedLootBounds();
		if (source == Bounds.EMPTY) {
			Bounds detail = layout.detailTextPanel();
			source = new Bounds(detail.x() + 5, detail.y() + 5, 16, 16);
		}
		requestPickupVisual(selected, selectionState.requestedAmount(), source, now);
	}

	private Bounds selectedLootBounds() {
		LootGroupKey selectedKey = selectionState.selectedKey();
		if (selectedKey == null) {
			return Bounds.EMPTY;
		}
		for (int index = 0; index < visibleGroups.size(); index++) {
			if (selectedKey.equals(visibleGroups.get(index).key())) {
				Bounds bounds = layout.lootSlotBounds(index, scrollOffset);
				return bounds.intersects(layout.lootViewport()) ? bounds : Bounds.EMPTY;
			}
		}
		return Bounds.EMPTY;
	}

	private void requestPickupVisual(LootGroup group, int requestedAmount, Bounds source, long now) {
		int targetSlot = predictedInventorySlot(group.displayStack());
		if (!pickupManager.requestPickup(group.representativeEntityId(), requestedAmount)) {
			return;
		}
		if (targetSlot < 0) {
			invalidPulseStartedAtNanos = now;
			return;
		}

		Bounds target = layout.inventorySlotBounds(targetSlot);
		startFlight(
			group.displayStack(),
			source.x() + source.width() / 2.0F - 8.0F,
			source.y() + source.height() / 2.0F - 8.0F,
			target.x() + 1.0F,
			target.y() + 1.0F,
			targetSlot,
			PICKUP_FLIGHT_MS,
			Easing.IN_OUT_CUBIC,
			now
		);
	}

	private int predictedInventorySlot(ItemStack lootStack) {
		if (minecraft.player == null) {
			return -1;
		}

		Inventory inventory = minecraft.player.getInventory();
		int firstEmpty = -1;
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack target = inventory.getItem(slot);
			if (target.isEmpty()) {
				if (firstEmpty < 0) {
					firstEmpty = slot;
				}
			} else if (ItemStack.isSameItemSameTags(target, lootStack)
					&& target.getCount() < Math.min(inventory.getMaxStackSize(), lootStack.getMaxStackSize())) {
				return slot;
			}
		}
		return firstEmpty;
	}

	private void startFlight(
			ItemStack stack,
			float startX,
			float startY,
			float endX,
			float endY,
			int targetSlot,
			int durationMillis,
			Easing easing,
			long now
	) {
		flightStack = stack.copyWithCount(1);
		flightStartX = startX;
		flightStartY = startY;
		flightEndX = endX;
		flightEndY = endY;
		flightTargetSlot = targetSlot;
		flightDurationMillis = durationMillis;
		flightEasing = easing;
		flightStartedAtNanos = now;
	}

	private void renderFlight(GuiGraphics graphics, long now, float opacity) {
		if (flightStack.isEmpty()) {
			float invalid = invalidPulse(now);
			if (invalid > 0.0F) {
				int detailY = sectionOffset(sectionProgress(now, DETAIL_DELAY_MS), closing
					? Easing.IN_CUBIC.apply(GuiAnimation.progress(now, closeStartedAtNanos, CLOSE_DURATION_MS))
					: 0.0F);
				Bounds action = layout.actionPanel().offset(0, detailY);
				PixelTheme.drawBorder(graphics, action.x(), action.y(), action.width(), action.height(), PixelTheme.INCOMPATIBLE, opacity * invalid);
			}
			return;
		}

		float rawProgress = GuiAnimation.progress(now, flightStartedAtNanos, flightDurationMillis);
		if (rawProgress >= 1.0F) {
			if (flightTargetSlot >= 0 && flightTargetSlot < inventoryPulseStart.length) {
				inventoryPulseStart[flightTargetSlot] = now;
			}
			flightStack = ItemStack.EMPTY;
			flightTargetSlot = -1;
			return;
		}

		float progress = flightEasing.apply(rawProgress);
		int x = Math.round(GuiAnimation.lerp(flightStartX, flightEndX, progress));
		int y = Math.round(GuiAnimation.lerp(flightStartY, flightEndY, progress));
		renderItem(graphics, flightStack, x, y, 1.0F + 0.05F * (1.0F - Math.abs(progress * 2.0F - 1.0F)), opacity * (1.0F - rawProgress * 0.25F));
	}

	private void renderDragGhost(GuiGraphics graphics, int mouseX, int mouseY, long now, float opacity) {
		Snapshot snapshot = dragState.snapshot();
		if (snapshot.displayStack().isEmpty()) {
			return;
		}

		if (!dragTrailInitialized) {
			dragTrailInitialized = true;
			dragTrailX = mouseX;
			dragTrailY = mouseY;
		}
		double deltaSeconds = previousRenderNanos == 0L ? 0.0D : Math.min(0.05D, (now - previousRenderNanos) / 1_000_000_000.0D);
		double follow = 1.0D - Math.exp(-deltaSeconds / 0.09D);
		dragTrailX += (mouseX - dragTrailX) * follow;
		dragTrailY += (mouseY - dragTrailY) * follow;
		renderItem(graphics, snapshot.displayStack(), (int) Math.round(dragTrailX) - 8, (int) Math.round(dragTrailY) - 8, 1.0F, opacity * 0.22F);

		Component amount = snapshot instanceof LootSnapshot lootSnapshot
			? lootSnapshot.requestedAmount() == PickupRequestPayload.ALL_ITEMS
				? Component.translatable("tactical_pickup.loot.drag_all")
				: Component.translatable("tactical_pickup.loot.card_count", lootSnapshot.requestedAmount())
			: Component.translatable("tactical_pickup.loot.card_count", snapshot.displayStack().getCount());
		String label = snapshot.displayStack().getHoverName().getString() + " " + amount.getString();
		int ghostWidth = Math.min(176, Math.max(72, font.width(label) + 28));
		int ghostX = Math.max(2, Math.min(mouseX + 10, width - ghostWidth - 2));
		int ghostY = Math.max(2, Math.min(mouseY + 10, height - 22));
		PixelTheme.drawPanel(graphics, ghostX, ghostY, ghostWidth, 20, opacity * 0.95F);
		renderItem(graphics, snapshot.displayStack(), ghostX + 2, ghostY + 2, 1.0F, opacity);
		graphics.drawString(
			font,
			font.plainSubstrByWidth(label, ghostWidth - 24),
			ghostX + 22,
			ghostY + 6,
			PixelTheme.color(PixelTheme.TEXT, opacity),
			false
		);
	}

	private boolean canAcceptLoot(ItemStack targetStack, ItemStack lootStack) {
		if (targetStack.isEmpty()) {
			return true;
		}
		return ItemStack.isSameItemSameTags(targetStack, lootStack)
			&& targetStack.getCount() < Math.min(minecraft.player.getInventory().getMaxStackSize(), lootStack.getMaxStackSize());
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

	private static boolean isFullyInside(Bounds outer, Bounds inner) {
		return inner.x() >= outer.x()
			&& inner.y() >= outer.y()
			&& inner.right() <= outer.right()
			&& inner.bottom() <= outer.bottom();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (closing) {
			return true;
		}

		UiPoint localMouse = uiTransform().screenToLocal(mouseX, mouseY);
		double localMouseX = localMouse.x();
		double localMouseY = localMouse.y();
		long now = System.nanoTime();
		int lootIndex = lootIndexAt(localMouseX, localMouseY, now);
		if (button == 1 && hasShiftDown() && lootIndex >= 0) {
			LootGroup group = visibleGroups.get(lootIndex);
			selectGroup(group, now);
			Bounds source = animatedLootSlotBounds(lootIndex, now);
			requestPickupVisual(group, PickupRequestPayload.ALL_ITEMS, source, now);
			return true;
		}

		if (super.mouseClicked(localMouseX, localMouseY, button)) {
			return true;
		}

		if (button == 0 && lootIndex >= 0) {
			LootGroup group = visibleGroups.get(lootIndex);
			selectGroup(group, now);
			dragState.pressLoot(group, selectionState.requestedAmount(), localMouseX, localMouseY);
			return true;
		}

		if (button == 0 && minecraft.player != null) {
			int inventoryY = sectionOffset(sectionProgress(now, INVENTORY_DELAY_MS), 0.0F);
			OptionalInt inventorySlot = layout.inventorySlotAt(localMouseX, localMouseY - inventoryY);
			if (inventorySlot.isPresent()) {
				ItemStack stack = minecraft.player.getInventory().getItem(inventorySlot.getAsInt());
				if (!stack.isEmpty()) {
					dragState.pressInventory(inventorySlot.getAsInt(), stack, localMouseX, localMouseY);
					return true;
				}
			}
		}
		return false;
	}

	private int lootIndexAt(double mouseX, double mouseY, long now) {
		int lootY = sectionOffset(sectionProgress(now, LOOT_DELAY_MS), 0.0F);
		Bounds viewport = layout.lootViewport().offset(0, lootY);
		if (!viewport.contains(mouseX, mouseY)) {
			return -1;
		}

		for (int index = 0; index < visibleGroups.size(); index++) {
			Bounds slotBounds = layout.lootSlotBounds(index, scrollOffset).offset(0, lootY);
			if (slotBounds.intersects(viewport) && slotBounds.contains(mouseX, mouseY)) {
				return index;
			}
		}
		return -1;
	}

	private Bounds animatedLootSlotBounds(int index, long now) {
		int lootY = sectionOffset(sectionProgress(now, LOOT_DELAY_MS), 0.0F);
		return layout.lootSlotBounds(index, scrollOffset).offset(0, lootY);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (closing) {
			return true;
		}
		UiTransform uiTransform = uiTransform();
		UiPoint localMouse = uiTransform.screenToLocal(mouseX, mouseY);
		if (button == 0 && dragState.isActive()) {
			dragState.update(localMouse.x(), localMouse.y());
			return true;
		}
		return super.mouseDragged(
			localMouse.x(),
			localMouse.y(),
			button,
			dragX / uiTransform.scale(),
			dragY / uiTransform.scale()
		);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (closing) {
			return true;
		}
		UiPoint localMouse = uiTransform().screenToLocal(mouseX, mouseY);
		double localMouseX = localMouse.x();
		double localMouseY = localMouse.y();
		if (button == 0 && dragState.isActive()) {
			long now = System.nanoTime();
			Snapshot snapshot = dragState.finish();
			if (snapshot instanceof LootSnapshot lootSnapshot && minecraft.player != null) {
				int inventoryY = sectionOffset(sectionProgress(now, INVENTORY_DELAY_MS), 0.0F);
				OptionalInt targetSlot = layout.inventorySlotAt(localMouseX, localMouseY - inventoryY);
				if (targetSlot.isPresent()
						&& canAcceptLoot(minecraft.player.getInventory().getItem(targetSlot.getAsInt()), lootSnapshot.displayStack())
						&& pickupManager.requestPickupToSlot(
							lootSnapshot.representativeEntityId(),
							lootSnapshot.requestedAmount(),
							targetSlot.getAsInt()
						)) {
					Bounds target = layout.inventorySlotBounds(targetSlot.getAsInt()).offset(0, inventoryY);
					startFlight(
						lootSnapshot.displayStack(),
						(float) localMouseX - 8.0F,
						(float) localMouseY - 8.0F,
						target.x() + 1.0F,
						target.y() + 1.0F,
						targetSlot.getAsInt(),
						PICKUP_FLIGHT_MS,
						Easing.IN_OUT_CUBIC,
						now
					);
				} else if (targetSlot.isPresent()) {
					invalidPulseStartedAtNanos = now;
				}
			} else if (snapshot instanceof InventorySnapshot inventorySnapshot) {
				int lootY = sectionOffset(sectionProgress(now, LOOT_DELAY_MS), 0.0F);
				Bounds lootPanel = layout.lootPanel().offset(0, lootY);
				if (lootPanel.contains(localMouseX, localMouseY)
						&& pickupManager.requestDropInventorySlot(inventorySnapshot.sourceSlot())) {
					startFlight(
						inventorySnapshot.displayStack(),
						(float) localMouseX - 8.0F,
						(float) localMouseY - 8.0F,
						lootPanel.x() + 11.0F,
						lootPanel.y() + 29.0F,
						-1,
						DROP_FLIGHT_MS,
						Easing.OUT_CUBIC,
						now
					);
				}
			}
			dragTrailInitialized = false;
			return true;
		}
		return super.mouseReleased(localMouseX, localMouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
		if (closing) {
			return true;
		}
		UiPoint localMouse = uiTransform().screenToLocal(mouseX, mouseY);
		long now = System.nanoTime();
		int lootY = sectionOffset(sectionProgress(now, LOOT_DELAY_MS), 0.0F);
		if (layout.lootPanel().offset(0, lootY).contains(localMouse.x(), localMouse.y())) {
			scrollOffset = layout.clampScroll(
				scrollOffset - vertical * LootScreenLayout.LOOT_CELL_SIZE,
				visibleGroups.size()
			);
			return true;
		}
		return super.mouseScrolled(localMouse.x(), localMouse.y(), vertical);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (closing) {
			return true;
		}
		if (ClientKeyMappings.OPEN_LOOT_SCREEN.matches(keyCode, scanCode)) {
			beginClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		beginClose();
	}

	private void beginClose() {
		if (!closing) {
			closing = true;
			closeStartedAtNanos = System.nanoTime();
			searchBox.setFocused(false);
			setFocused(null);
			updateButtonState();
		}
	}

	private void finishClose() {
		clearTransientState();
		if (minecraft != null && minecraft.screen == this) {
			minecraft.setScreen(null);
		}
	}

	private void forceClose() {
		clearTransientState();
		if (minecraft != null && minecraft.screen == this) {
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
		sourceGroups = List.of();
		cachedEnchantments = List.of();
		cachedEnchantmentKey = null;
		animatedSelectionKey = null;
		flightStack = ItemStack.EMPTY;
		scrollOffset = 0.0D;
		hoveredTooltipStack = ItemStack.EMPTY;
		dragTrailInitialized = false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
