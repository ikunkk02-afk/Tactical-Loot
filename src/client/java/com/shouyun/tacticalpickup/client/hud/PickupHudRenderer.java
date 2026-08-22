package com.shouyun.tacticalpickup.client.hud;

import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.ui.ItemDetailHelper;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.PickupConstants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class PickupHudRenderer {
	private static final int MIN_PANEL_WIDTH = 150;
	private static final int MAX_PANEL_WIDTH = 240;
	private static final int PANEL_PADDING = 6;
	private static final int ITEM_SIZE = 16;
	private static final int ROW_HEIGHT = 18;
	private static final int SECTION_GAP = 5;
	private static final int SCREEN_MARGIN = 8;
	private static final int BACKGROUND_COLOR = 0xB0101010;
	private static final int SELECTED_COLOR = 0x906080A0;
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
		int visibleCount = calculateVisibleCount(font, graphics.guiHeight(), groups.size(), pickupMode, visibleEnchantments, hiddenEnchantments);
		int firstVisible = pickupMode
			? Math.max(0, Math.min(manager.selectedIndex() - visibleCount / 2, groups.size() - visibleCount))
			: 0;
		int hiddenCount = groups.size() - visibleCount;
		int panelHeight = calculatePanelHeight(font, visibleCount, hiddenCount, pickupMode, visibleEnchantments, hiddenEnchantments);
		int panelWidth = calculatePanelWidth(
			client,
			groups,
			firstVisible,
			visibleCount,
			enchantments,
			visibleEnchantments,
			hiddenEnchantments,
			amountText
		);
		int x = Math.max(SCREEN_MARGIN, graphics.guiWidth() - panelWidth - 12);
		int centeredY = (graphics.guiHeight() - panelHeight) / 2;
		int maxY = Math.max(SCREEN_MARGIN, graphics.guiHeight() - panelHeight - SCREEN_MARGIN);
		int y = Math.max(SCREEN_MARGIN, Math.min(centeredY, maxY));

		graphics.fill(x, y, x + panelWidth, y + panelHeight, BACKGROUND_COLOR);
		int cursorY = y + PANEL_PADDING;
		graphics.drawString(font, Component.translatable("tactical_pickup.hud.title"), x + PANEL_PADDING, cursorY, TEXT_COLOR, true);
		cursorY += font.lineHeight + 4;

		for (int offset = 0; offset < visibleCount; offset++) {
			int entryIndex = firstVisible + offset;
			LootGroup group = groups.get(entryIndex);
			boolean selected = pickupMode && entryIndex == manager.selectedIndex();
			ItemFilterState filterState = manager.filterManager().getState(LootGroupFilter.itemId(group));
			if (selected) {
				graphics.fill(x + 2, cursorY - 1, x + panelWidth - 2, cursorY + ROW_HEIGHT - 1, SELECTED_COLOR);
			}

			graphics.renderItem(group.displayStack(), x + PANEL_PADDING, cursorY);
			String label = groupLabel(group, filterState);
			int availableTextWidth = panelWidth - PANEL_PADDING * 3 - ITEM_SIZE;
			String clippedLabel = font.plainSubstrByWidth(label, availableTextWidth);
			int rowTextColor = filterState == ItemFilterState.LOW_PRIORITY && !selected ? MUTED_TEXT_COLOR : TEXT_COLOR;
			graphics.drawString(font, clippedLabel, x + PANEL_PADDING * 2 + ITEM_SIZE, cursorY + 4, rowTextColor, true);
			cursorY += ROW_HEIGHT;
		}

		if (hiddenCount > 0) {
			graphics.drawString(font, Component.translatable("tactical_pickup.hud.more", hiddenCount), x + PANEL_PADDING, cursorY, MUTED_TEXT_COLOR, true);
			cursorY += font.lineHeight + 2;
		}

		cursorY += SECTION_GAP;
		if (pickupMode) {
			if (visibleEnchantments > 0) {
				graphics.drawString(font, Component.translatable("tactical_pickup.hud.enchantments"), x + PANEL_PADDING, cursorY, TEXT_COLOR, true);
				cursorY += font.lineHeight + 2;

				for (int index = 0; index < visibleEnchantments; index++) {
					drawClippedComponent(
						graphics,
						font,
						enchantments.get(index),
						x + PANEL_PADDING,
						cursorY,
						panelWidth - PANEL_PADDING * 2,
						TEXT_COLOR
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

			drawClippedComponent(graphics, font, amountText, x + PANEL_PADDING, cursorY, panelWidth - PANEL_PADDING * 2, TEXT_COLOR);
			cursorY += font.lineHeight + 1 + SECTION_GAP;
			cursorY = drawInstruction(graphics, font, "tactical_pickup.hud.scroll", x, cursorY);
			cursorY = drawInstruction(graphics, font, "tactical_pickup.hud.adjust_amount", x, cursorY);
			cursorY = drawInstruction(graphics, font, "tactical_pickup.hud.adjust_amount_fast", x, cursorY);
			cursorY = drawInstructionComponent(
				graphics,
				font,
				Component.translatable(
					"tactical_pickup.hud.cycle_filter",
					ClientKeyMappings.CYCLE_FILTER.getTranslatedKeyMessage()
				),
				x,
				cursorY
			);
			cursorY = drawInstruction(graphics, font, "tactical_pickup.hud.pickup", x, cursorY);
			cursorY = drawOpenLootInstruction(graphics, font, x, cursorY);
			drawInstruction(graphics, font, "tactical_pickup.hud.exit", x, cursorY);
		} else {
			cursorY = drawInstruction(graphics, font, "tactical_pickup.hud.enter", x, cursorY);
			drawOpenLootInstruction(graphics, font, x, cursorY);
		}
	}

	private static Component amountText(ClientPickupManager manager, LootGroup selectedGroup) {
		return manager.pickupAll()
			? Component.translatable("tactical_pickup.hud.amount_all", selectedGroup.totalCount())
			: Component.translatable("tactical_pickup.hud.amount", manager.selectedAmount(), selectedGroup.totalCount());
	}

	private static int calculateVisibleCount(
			Font font,
			int screenHeight,
			int groupCount,
			boolean pickupMode,
			int visibleEnchantments,
			int hiddenEnchantments
	) {
		int visibleCount = Math.min(groupCount, PickupConstants.MAX_HUD_ENTRIES);
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
		int height = PANEL_PADDING + font.lineHeight + 4 + visibleCount * ROW_HEIGHT;
		height += hiddenCount > 0 ? font.lineHeight + 2 : 0;
		height += SECTION_GAP;

		if (pickupMode) {
			if (visibleEnchantments > 0) {
				height += font.lineHeight + 2;
				height += visibleEnchantments * lineStep;
				height += hiddenEnchantments > 0 ? lineStep : 0;
				height += SECTION_GAP;
			}

			height += lineStep + SECTION_GAP;
			height += 7 * lineStep;
		} else {
			height += 2 * lineStep;
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
			Component amountText
	) {
		Font font = client.font;
		int width = font.width(Component.translatable("tactical_pickup.hud.title"));

		for (int offset = 0; offset < visibleCount; offset++) {
			LootGroup group = groups.get(firstVisible + offset);
			ItemFilterState state = ClientPickupManager.getInstance().filterManager().getState(LootGroupFilter.itemId(group));
			String label = groupLabel(group, state);
			width = Math.max(width, ITEM_SIZE + PANEL_PADDING + font.width(label));
		}

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
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.scroll")));
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.adjust_amount")));
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.adjust_amount_fast")));
		width = Math.max(width, font.width(Component.translatable(
			"tactical_pickup.hud.cycle_filter",
			ClientKeyMappings.CYCLE_FILTER.getTranslatedKeyMessage()
		)));
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.pickup")));
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.exit")));
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.enter")));
		width = Math.max(width, font.width(Component.translatable(
			"tactical_pickup.hud.open_loot_screen",
			ClientKeyMappings.OPEN_LOOT_SCREEN.getTranslatedKeyMessage()
		)));
		int screenLimit = Math.max(MIN_PANEL_WIDTH, client.getWindow().getGuiScaledWidth() / 2 - 16);
		return Math.min(Math.max(width + PANEL_PADDING * 2, MIN_PANEL_WIDTH), Math.min(MAX_PANEL_WIDTH, screenLimit));
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

	private static int drawInstruction(GuiGraphics graphics, Font font, String translationKey, int x, int y) {
		return drawInstructionComponent(graphics, font, Component.translatable(translationKey), x, y);
	}

	private static int drawInstructionComponent(GuiGraphics graphics, Font font, Component component, int x, int y) {
		graphics.drawString(font, component, x + PANEL_PADDING, y, MUTED_TEXT_COLOR, true);
		return y + font.lineHeight + 1;
	}

	private static int drawOpenLootInstruction(GuiGraphics graphics, Font font, int x, int y) {
		return drawInstructionComponent(
			graphics,
			font,
			Component.translatable(
				"tactical_pickup.hud.open_loot_screen",
				ClientKeyMappings.OPEN_LOOT_SCREEN.getTranslatedKeyMessage()
			),
			x,
			y
		);
	}

	private static String groupLabel(LootGroup group, ItemFilterState state) {
		String label = group.displayStack().getHoverName().getString() + " ×" + group.totalCount();
		return state == ItemFilterState.LOW_PRIORITY
			? label + "  " + Component.translatable("tactical_pickup.hud.low_priority").getString()
			: label;
	}
}
