package com.shouyun.tacticalpickup.client.hud;

import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.pickup.PickupEntry;
import com.shouyun.tacticalpickup.pickup.PickupConstants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class PickupHudRenderer {
	private static final int MIN_PANEL_WIDTH = 150;
	private static final int MAX_PANEL_WIDTH = 240;
	private static final int PANEL_PADDING = 6;
	private static final int ITEM_SIZE = 16;
	private static final int ROW_HEIGHT = 18;
	private static final int BACKGROUND_COLOR = 0xB0101010;
	private static final int SELECTED_COLOR = 0x906080A0;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int MUTED_TEXT_COLOR = 0xFFB8B8B8;

	private PickupHudRenderer() {
	}

	public static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		ClientPickupManager manager = ClientPickupManager.getInstance();
		List<PickupEntry> entries = manager.entries();

		if (client.player == null || client.level == null || client.options.hideGui || entries.isEmpty()) {
			return;
		}

		Font font = client.font;
		boolean pickupMode = manager.isPickupMode();
		int visibleCount = Math.min(entries.size(), PickupConstants.MAX_HUD_ENTRIES);
		int firstVisible = pickupMode
			? Math.max(0, Math.min(manager.selectedIndex() - visibleCount / 2, entries.size() - visibleCount))
			: 0;
		int hiddenCount = entries.size() - visibleCount;
		int instructionLines = pickupMode ? 3 : 1;
		int panelHeight = PANEL_PADDING + font.lineHeight + 4 + visibleCount * ROW_HEIGHT
			+ (hiddenCount > 0 ? font.lineHeight + 2 : 0)
			+ 4 + instructionLines * (font.lineHeight + 1) + PANEL_PADDING;
		int panelWidth = calculatePanelWidth(client, entries, firstVisible, visibleCount);
		int x = Math.max(8, graphics.guiWidth() - panelWidth - 12);
		int y = Math.max(8, (graphics.guiHeight() - panelHeight) / 2);

		graphics.fill(x, y, x + panelWidth, y + panelHeight, BACKGROUND_COLOR);
		int cursorY = y + PANEL_PADDING;
		graphics.drawString(font, Component.translatable("tactical_pickup.hud.title"), x + PANEL_PADDING, cursorY, TEXT_COLOR, true);
		cursorY += font.lineHeight + 4;

		for (int offset = 0; offset < visibleCount; offset++) {
			int entryIndex = firstVisible + offset;
			PickupEntry entry = entries.get(entryIndex);
			if (pickupMode && entryIndex == manager.selectedIndex()) {
				graphics.fill(x + 2, cursorY - 1, x + panelWidth - 2, cursorY + ROW_HEIGHT - 1, SELECTED_COLOR);
			}

			graphics.renderItem(entry.itemStack(), x + PANEL_PADDING, cursorY);
			String label = entry.itemStack().getHoverName().getString() + " ×" + entry.itemStack().getCount();
			int availableTextWidth = panelWidth - PANEL_PADDING * 3 - ITEM_SIZE;
			String clippedLabel = font.plainSubstrByWidth(label, availableTextWidth);
			graphics.drawString(font, clippedLabel, x + PANEL_PADDING * 2 + ITEM_SIZE, cursorY + 4, TEXT_COLOR, true);
			cursorY += ROW_HEIGHT;
		}

		if (hiddenCount > 0) {
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.hud.more", hiddenCount),
				x + PANEL_PADDING,
				cursorY,
				MUTED_TEXT_COLOR,
				true
			);
			cursorY += font.lineHeight + 2;
		}

		cursorY += 4;
		if (pickupMode) {
			cursorY = drawInstruction(graphics, font, "tactical_pickup.hud.scroll", x, cursorY);
			cursorY = drawInstruction(graphics, font, "tactical_pickup.hud.pickup", x, cursorY);
			drawInstruction(graphics, font, "tactical_pickup.hud.exit", x, cursorY);
		} else {
			drawInstruction(graphics, font, "tactical_pickup.hud.enter", x, cursorY);
		}
	}

	private static int calculatePanelWidth(Minecraft client, List<PickupEntry> entries, int firstVisible, int visibleCount) {
		Font font = client.font;
		int width = font.width(Component.translatable("tactical_pickup.hud.title"));

		for (int offset = 0; offset < visibleCount; offset++) {
			PickupEntry entry = entries.get(firstVisible + offset);
			String label = entry.itemStack().getHoverName().getString() + " ×" + entry.itemStack().getCount();
			width = Math.max(width, ITEM_SIZE + PANEL_PADDING + font.width(label));
		}

		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.scroll")));
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.pickup")));
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.exit")));
		width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.enter")));
		int screenLimit = Math.max(MIN_PANEL_WIDTH, client.getWindow().getGuiScaledWidth() / 2 - 16);
		return Math.min(Math.max(width + PANEL_PADDING * 2, MIN_PANEL_WIDTH), Math.min(MAX_PANEL_WIDTH, screenLimit));
	}

	private static int drawInstruction(GuiGraphics graphics, Font font, String translationKey, int x, int y) {
		graphics.drawString(font, Component.translatable(translationKey), x + PANEL_PADDING, y, MUTED_TEXT_COLOR, true);
		return y + font.lineHeight + 1;
	}
}
