package com.shouyun.tacticalpickup.client.hud;

import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.ui.ItemDetailHelper;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

public final class PickupHudRenderer {
	private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
	private static final int MIN_PANEL_WIDTH = 112;
	private static final int MAX_PANEL_WIDTH = 190;
	private static final int PANEL_PADDING = 4;
	private static final int ITEM_SIZE = 16;
	private static final int ROW_HEIGHT = 18;
	private static final int SECTION_GAP = 3;
	private static final int SCREEN_MARGIN = 8;
	private static final int CROSSHAIR_OFFSET = 24;
	private static final int PASSIVE_MAX_ENTRIES = 3;
	private static final int PICKUP_MAX_ENTRIES = 5;
	private static final int BACKGROUND_COLOR = 0x98101010;
	private static final int BORDER_COLOR = 0xB0000000;
	private static final int BORDER_HIGHLIGHT_COLOR = 0x58FFFFFF;
	private static final int SELECTED_COLOR = 0x38FFFFFF;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int MUTED_TEXT_COLOR = 0xFFB8B8B8;

	private PickupHudRenderer() {
	}

	public static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		ClientPickupManager manager = ClientPickupManager.getInstance();
		List<LootGroup> groups = manager.groups();

		if (client.player == null || client.level == null || client.options.hideGui || groups.isEmpty()) {
			return;
		}

		Font font = client.font;
		boolean pickupMode = manager.isPickupMode();
		LootGroup selectedGroup = pickupMode ? manager.selectedGroup() : null;
		List<Component> enchantments = selectedGroup == null
			? List.of()
			: ItemDetailHelper.collectEnchantments(client, selectedGroup.displayStack());
		int visibleEnchantments = Math.min(enchantments.size(), ItemDetailHelper.MAX_VISIBLE_ENCHANTMENTS);
		int hiddenEnchantments = enchantments.size() - visibleEnchantments;
		Component amountText = selectedGroup == null ? null : amountText(manager, selectedGroup);
		int visibleCount = calculateVisibleCount(
			font,
			graphics.guiHeight(),
			groups.size(),
			pickupMode,
			visibleEnchantments,
			hiddenEnchantments
		);
		int firstVisible = pickupMode
			? Math.max(0, Math.min(manager.selectedIndex() - visibleCount / 2, groups.size() - visibleCount))
			: 0;
		int hiddenCount = groups.size() - visibleCount;
		int panelHeight = calculatePanelHeight(
			font,
			visibleCount,
			hiddenCount,
			pickupMode,
			visibleEnchantments,
			hiddenEnchantments
		);
		int panelWidth = calculatePanelWidth(
			client,
			groups,
			firstVisible,
			visibleCount,
			enchantments,
			visibleEnchantments,
			hiddenEnchantments,
			amountText,
			pickupMode,
			hiddenCount
		);
		int centerX = graphics.guiWidth() / 2;
		int preferredRightX = centerX + CROSSHAIR_OFFSET;
		int x = preferredRightX + panelWidth <= graphics.guiWidth() - SCREEN_MARGIN
			? preferredRightX
			: Math.max(SCREEN_MARGIN, centerX - CROSSHAIR_OFFSET - panelWidth);
		int centeredY = graphics.guiHeight() / 2 - panelHeight / 2;
		int maxY = Math.max(SCREEN_MARGIN, graphics.guiHeight() - panelHeight - SCREEN_MARGIN);
		int y = Math.max(SCREEN_MARGIN, Math.min(centeredY, maxY));

		renderPanel(graphics, x, y, panelWidth, panelHeight);
		int cursorY = y + PANEL_PADDING;
		if (pickupMode) {
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.hud.title"),
				x + PANEL_PADDING,
				cursorY,
				TEXT_COLOR,
				true
			);
			cursorY += font.lineHeight + SECTION_GAP;
		}

		int markerWidth = pickupMode ? 7 : 0;
		for (int offset = 0; offset < visibleCount; offset++) {
			int entryIndex = firstVisible + offset;
			LootGroup group = groups.get(entryIndex);
			boolean selected = pickupMode && entryIndex == manager.selectedIndex();
			ItemFilterState filterState = manager.filterManager().getState(LootGroupFilter.itemId(group));
			if (selected) {
				graphics.fill(x + 1, cursorY, x + panelWidth - 1, cursorY + ROW_HEIGHT, SELECTED_COLOR);
				graphics.drawString(font, Component.literal(">"), x + 3, cursorY + 4, TEXT_COLOR, true);
			}

			int itemX = x + PANEL_PADDING + markerWidth;
			graphics.blitSprite(SLOT_SPRITE, itemX, cursorY, ROW_HEIGHT, ROW_HEIGHT);
			graphics.renderItem(group.displayStack(), itemX + 1, cursorY + 1);
			String label = groupLabel(group, filterState);
			int textX = itemX + ITEM_SIZE + 4;
			int availableTextWidth = x + panelWidth - PANEL_PADDING - textX;
			String clippedLabel = font.plainSubstrByWidth(label, Math.max(1, availableTextWidth));
			int rowTextColor = filterState == ItemFilterState.LOW_PRIORITY && !selected ? MUTED_TEXT_COLOR : TEXT_COLOR;
			graphics.drawString(font, clippedLabel, textX, cursorY + 5, rowTextColor, true);
			cursorY += ROW_HEIGHT;
		}

		if (hiddenCount > 0) {
			drawClippedComponent(
				graphics,
				font,
				Component.translatable("tactical_pickup.hud.more", hiddenCount),
				x + PANEL_PADDING + markerWidth,
				cursorY,
				panelWidth - PANEL_PADDING * 2 - markerWidth,
				MUTED_TEXT_COLOR
			);
			cursorY += font.lineHeight + 1;
		}

		cursorY += SECTION_GAP;
		if (!pickupMode) {
			drawClippedComponent(
				graphics,
				font,
				passiveActions(),
				x + PANEL_PADDING,
				cursorY,
				panelWidth - PANEL_PADDING * 2,
				MUTED_TEXT_COLOR
			);
			return;
		}

		if (visibleEnchantments > 0) {
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.hud.enchantments"),
				x + PANEL_PADDING,
				cursorY,
				TEXT_COLOR,
				true
			);
			cursorY += font.lineHeight + 1;
			for (int index = 0; index < visibleEnchantments; index++) {
				drawClippedComponent(
					graphics,
					font,
					enchantments.get(index),
					x + PANEL_PADDING,
					cursorY,
					panelWidth - PANEL_PADDING * 2,
					MUTED_TEXT_COLOR
				);
				cursorY += font.lineHeight + 1;
			}

			if (hiddenEnchantments > 0) {
				drawClippedComponent(
					graphics,
					font,
					Component.translatable("tactical_pickup.hud.more_enchantments", hiddenEnchantments),
					x + PANEL_PADDING,
					cursorY,
					panelWidth - PANEL_PADDING * 2,
					MUTED_TEXT_COLOR
				);
				cursorY += font.lineHeight + 1;
			}
			cursorY += SECTION_GAP;
		}

		drawClippedComponent(
			graphics,
			font,
			amountText,
			x + PANEL_PADDING,
			cursorY,
			panelWidth - PANEL_PADDING * 2,
			TEXT_COLOR
		);
		cursorY += font.lineHeight + 1 + SECTION_GAP;
		cursorY = drawInstruction(
			graphics,
			font,
			Component.translatable("tactical_pickup.hud.controls.selection"),
			x,
			cursorY,
			panelWidth
		);
		cursorY = drawInstruction(graphics, font, pickupActions(), x, cursorY, panelWidth);
		drawInstruction(
			graphics,
			font,
			Component.translatable("tactical_pickup.hud.controls.exit"),
			x,
			cursorY,
			panelWidth
		);
	}

	private static Component amountText(ClientPickupManager manager, LootGroup selectedGroup) {
		return manager.pickupAll()
			? Component.translatable("tactical_pickup.hud.amount_all", selectedGroup.totalCount())
			: Component.translatable("tactical_pickup.hud.amount", manager.selectedAmount(), selectedGroup.totalCount());
	}

	private static Component passiveActions() {
		return Component.translatable(
			"tactical_pickup.hud.controls.passive",
			ClientKeyMappings.OPEN_LOOT_SCREEN.getTranslatedKeyMessage()
		);
	}

	private static Component pickupActions() {
		return Component.translatable(
			"tactical_pickup.hud.controls.actions",
			ClientKeyMappings.CYCLE_FILTER.getTranslatedKeyMessage(),
			ClientKeyMappings.OPEN_LOOT_SCREEN.getTranslatedKeyMessage()
		);
	}

	private static int calculateVisibleCount(
			Font font,
			int screenHeight,
			int groupCount,
			boolean pickupMode,
			int visibleEnchantments,
			int hiddenEnchantments
	) {
		int maximumEntries = pickupMode ? PICKUP_MAX_ENTRIES : PASSIVE_MAX_ENTRIES;
		int visibleCount = Math.min(groupCount, maximumEntries);
		int availableHeight = Math.max(1, screenHeight - SCREEN_MARGIN * 2);

		while (visibleCount > 1 && calculatePanelHeight(
				font,
				visibleCount,
				groupCount - visibleCount,
				pickupMode,
				visibleEnchantments,
				hiddenEnchantments
			) > availableHeight) {
			visibleCount--;
		}
		return visibleCount;
	}

	private static int calculatePanelHeight(
			Font font,
			int visibleCount,
			int hiddenCount,
			boolean pickupMode,
			int visibleEnchantments,
			int hiddenEnchantments
	) {
		int lineStep = font.lineHeight + 1;
		int height = PANEL_PADDING + visibleCount * ROW_HEIGHT;
		if (pickupMode) {
			height += font.lineHeight + SECTION_GAP;
		}
		height += hiddenCount > 0 ? lineStep : 0;
		height += SECTION_GAP;

		if (pickupMode) {
			if (visibleEnchantments > 0) {
				height += font.lineHeight + 1;
				height += visibleEnchantments * lineStep;
				height += hiddenEnchantments > 0 ? lineStep : 0;
				height += SECTION_GAP;
			}
			height += lineStep + SECTION_GAP;
			height += 3 * lineStep;
		} else {
			height += font.lineHeight;
		}
		return height + PANEL_PADDING;
	}

	private static int calculatePanelWidth(
			Minecraft client,
			List<LootGroup> groups,
			int firstVisible,
			int visibleCount,
			List<Component> enchantments,
			int visibleEnchantments,
			int hiddenEnchantments,
			Component amountText,
			boolean pickupMode,
			int hiddenCount
	) {
		Font font = client.font;
		int markerWidth = pickupMode ? 7 : 0;
		int width = pickupMode ? font.width(Component.translatable("tactical_pickup.hud.title")) : 0;

		for (int offset = 0; offset < visibleCount; offset++) {
			LootGroup group = groups.get(firstVisible + offset);
			ItemFilterState state = ClientPickupManager.getInstance().filterManager().getState(LootGroupFilter.itemId(group));
			width = Math.max(width, markerWidth + ITEM_SIZE + 4 + font.width(groupLabel(group, state)));
		}
		if (hiddenCount > 0) {
			width = Math.max(width, markerWidth + font.width(Component.translatable("tactical_pickup.hud.more", hiddenCount)));
		}

		if (pickupMode) {
			for (int index = 0; index < visibleEnchantments; index++) {
				width = Math.max(width, font.width(enchantments.get(index)));
			}
			if (hiddenEnchantments > 0) {
				width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.more_enchantments", hiddenEnchantments)));
			}
			if (amountText != null) {
				width = Math.max(width, font.width(amountText));
			}
			width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.enchantments")));
			width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.controls.selection")));
			width = Math.max(width, font.width(pickupActions()));
			width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.controls.exit")));
		} else {
			width = Math.max(width, font.width(passiveActions()));
		}

		int screenLimit = Math.max(1, client.getWindow().getGuiScaledWidth() - SCREEN_MARGIN * 2);
		return Math.min(Math.max(width + PANEL_PADDING * 2, MIN_PANEL_WIDTH), Math.min(MAX_PANEL_WIDTH, screenLimit));
	}

	private static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.fill(x, y, x + width, y + height, BORDER_COLOR);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND_COLOR);
		graphics.fill(x + 1, y + 1, x + width - 1, y + 2, BORDER_HIGHLIGHT_COLOR);
		graphics.fill(x + 1, y + 1, x + 2, y + height - 1, BORDER_HIGHLIGHT_COLOR);
	}

	private static void drawClippedComponent(
			GuiGraphics graphics,
			Font font,
			Component component,
			int x,
			int y,
			int maxWidth,
			int fallbackColor
	) {
		List<FormattedCharSequence> lines = font.split(component, Math.max(1, maxWidth));
		if (!lines.isEmpty()) {
			graphics.drawString(font, lines.getFirst(), x, y, fallbackColor, true);
		}
	}

	private static int drawInstruction(
			GuiGraphics graphics,
			Font font,
			Component component,
			int x,
			int y,
			int panelWidth
	) {
		drawClippedComponent(
			graphics,
			font,
			component,
			x + PANEL_PADDING,
			y,
			panelWidth - PANEL_PADDING * 2,
			MUTED_TEXT_COLOR
		);
		return y + font.lineHeight + 1;
	}

	private static String groupLabel(LootGroup group, ItemFilterState state) {
		String label = group.displayStack().getHoverName().getString() + " ×" + group.totalCount();
		return state == ItemFilterState.LOW_PRIORITY
			? label + " · " + Component.translatable("tactical_pickup.hud.low_priority").getString()
			: label;
	}
}
