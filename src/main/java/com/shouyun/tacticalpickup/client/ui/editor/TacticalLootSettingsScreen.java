package com.shouyun.tacticalpickup.client.ui.editor;

import com.shouyun.tacticalpickup.client.filter.FilterManagementScreen;
import com.shouyun.tacticalpickup.client.ui.PixelButton;
import com.shouyun.tacticalpickup.client.ui.PixelTheme;
import com.shouyun.tacticalpickup.client.ui.layout.UiElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TacticalLootSettingsScreen extends Screen {
	private static final int PANEL_WIDTH = 286;
	private static final int PANEL_HEIGHT = 150;
	private final Screen parent;

	public TacticalLootSettingsScreen(Screen parent) {
		super(Component.translatable("tactical_pickup.settings.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int buttonWidth = Math.min(250, Math.max(120, width - 40));
		int buttonX = (width - buttonWidth) / 2;
		int firstY = height / 2 - 43;
		addRenderableWidget(new PixelButton(
			buttonX,
			firstY,
			buttonWidth,
			20,
			Component.translatable("tactical_pickup.settings.edit_loot_hud"),
			button -> minecraft.setScreen(new TacticalLootHudEditorScreen(this, UiElement.LOOT_HUD))
		));
		addRenderableWidget(new PixelButton(
			buttonX,
			firstY + 24,
			buttonWidth,
			20,
			Component.translatable("tactical_pickup.settings.edit_loot_screen"),
			button -> minecraft.setScreen(new TacticalLootHudEditorScreen(this, UiElement.LOOT_SCREEN))
		));
		addRenderableWidget(new PixelButton(
			buttonX,
			firstY + 48,
			buttonWidth,
			20,
			Component.translatable("tactical_pickup.settings.manage_filters"),
			button -> minecraft.setScreen(new FilterManagementScreen(this))
		));
		addRenderableWidget(new PixelButton(
			buttonX,
			firstY + 78,
			buttonWidth,
			20,
			Component.translatable("gui.done"),
			button -> onClose()
		).primary(true));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, width, height, 0x66000000);
		int panelWidth = Math.min(PANEL_WIDTH, Math.max(140, width - 24));
		int panelHeight = Math.min(PANEL_HEIGHT, Math.max(120, height - 24));
		int panelX = (width - panelWidth) / 2;
		int panelY = (height - panelHeight) / 2;
		PixelTheme.drawPanel(graphics, panelX, panelY, panelWidth, panelHeight, 0.98F);
		graphics.drawCenteredString(font, title, width / 2, panelY + 12, PixelTheme.TEXT);
		graphics.drawCenteredString(
			font,
			Component.translatable("tactical_pickup.settings.ui_hint"),
			width / 2,
			panelY + 27,
			PixelTheme.MUTED_TEXT
		);
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void renderBackground(GuiGraphics graphics) {
		// Preserve the current world or menu as the background.
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
