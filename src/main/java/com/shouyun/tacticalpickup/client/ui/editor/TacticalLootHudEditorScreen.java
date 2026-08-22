package com.shouyun.tacticalpickup.client.ui.editor;

import com.shouyun.tacticalpickup.client.config.ClientUiConfigManager;
import com.shouyun.tacticalpickup.client.hud.PickupHudPosition;
import com.shouyun.tacticalpickup.client.hud.PickupHudRenderer;
import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.loot.LootScreenEditorPreview;
import com.shouyun.tacticalpickup.client.ui.PixelButton;
import com.shouyun.tacticalpickup.client.ui.PixelTheme;
import com.shouyun.tacticalpickup.client.ui.animation.AnimatedFloat;
import com.shouyun.tacticalpickup.client.ui.animation.Easing;
import com.shouyun.tacticalpickup.client.ui.layout.UiElement;
import com.shouyun.tacticalpickup.client.ui.layout.UiPlacement;
import com.shouyun.tacticalpickup.client.ui.layout.UiRect;
import com.shouyun.tacticalpickup.client.ui.layout.UiTransform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class TacticalLootHudEditorScreen extends Screen {
	private static final int SCALE_DURATION_MS = 100;
	private static final int DRAG_DURATION_MS = 70;
	private static final float SCALE_STEP = 0.05F;

	private final ClientUiConfigManager config = ClientUiConfigManager.getInstance();
	private final Screen parent;
	private final UiElement element;
	private final EditableElement editable;
	private PixelButton resetCurrentButton;
	private PixelButton resetAllButton;
	private PixelButton confirmResetButton;
	private PixelButton cancelResetButton;
	private boolean confirmResetAll;
	private boolean dragging;
	private double dragOffsetX;
	private double dragOffsetY;
	private boolean saved;

	public TacticalLootHudEditorScreen(Screen parent, UiElement element) {
		super(Component.translatable("tactical_pickup.ui_editor.title"));
		this.parent = parent;
		this.element = element;
		editable = new EditableElement(config.placement(element).scale());
	}

	@Override
	protected void init() {
		int buttonY = Math.max(8, height - 43);
		resetCurrentButton = addRenderableWidget(new PixelButton(
			width / 2 - 126,
			buttonY,
			122,
			20,
			Component.translatable("tactical_pickup.ui_editor.reset_current"),
			button -> resetCurrent()
		));
		resetAllButton = addRenderableWidget(new PixelButton(
			width / 2 + 4,
			buttonY,
			122,
			20,
			Component.translatable("tactical_pickup.ui_editor.reset_all"),
			button -> setConfirmResetAll(true)
		));

		int confirmY = height / 2 + 18;
		confirmResetButton = addRenderableWidget(new PixelButton(
			width / 2 - 84,
			confirmY,
			80,
			20,
			Component.translatable("gui.yes"),
			button -> confirmResetAll()
		).primary(true));
		cancelResetButton = addRenderableWidget(new PixelButton(
			width / 2 + 4,
			confirmY,
			80,
			20,
			Component.translatable("gui.no"),
			button -> setConfirmResetAll(false)
		));
		updateButtonVisibility();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		graphics.fill(0, 0, width, height, 0x48000000);
		graphics.drawCenteredString(font, title, width / 2, 9, PixelTheme.TEXT);

		renderElement(graphics, element, now);
		renderSelection(graphics, now);

		Component hint = Component.translatable("tactical_pickup.ui_editor.hint");
		graphics.drawCenteredString(font, hint, width / 2, height - 14, PixelTheme.MUTED_TEXT);
		if (confirmResetAll) {
			renderConfirmation(graphics);
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void renderBackground(GuiGraphics graphics) {
		// Keep the live game world visible behind the editor.
	}

	private void renderElement(GuiGraphics graphics, UiElement element, long now) {
		UiTransform transform = transform(element, now);
		graphics.pose().pushPose();
		transform.apply(graphics);
		if (element == UiElement.LOOT_HUD) {
			PickupHudRenderer.renderEditorPreview(graphics, font);
		} else {
			LootScreenEditorPreview.render(graphics, font, width, height);
		}
		graphics.pose().popPose();
	}

	private void renderSelection(GuiGraphics graphics, long now) {
		UiTransform transform = transform(element, now);
		UiRect bounds = transform.screenBounds();
		int x = (int) Math.floor(bounds.x());
		int y = (int) Math.floor(bounds.y());
		int boxWidth = Math.max(1, (int) Math.ceil(bounds.width()));
		int boxHeight = Math.max(1, (int) Math.ceil(bounds.height()));
		PixelTheme.drawBorder(graphics, x, y, boxWidth, boxHeight, PixelTheme.EDGE_LIGHT, 0.9F);

		Component name = Component.translatable(element.translationKey());
		int percentage = Math.round(editable.targetScale * 100.0F);
		Component label = Component.translatable("tactical_pickup.ui_editor.selection", name, percentage);
		int labelWidth = font.width(label) + 8;
		int labelX = Math.max(2, Math.min(x, width - labelWidth - 2));
		int labelY = y >= 16 ? y - 14 : Math.min(height - 14, y + 3);
		PixelTheme.drawPanel(graphics, labelX, labelY, labelWidth, 13, 0.96F);
		graphics.drawString(font, label, labelX + 4, labelY + 3, PixelTheme.TEXT, false);
	}

	private void renderConfirmation(GuiGraphics graphics) {
		int panelWidth = Math.min(280, Math.max(180, width - 24));
		int panelHeight = 70;
		int x = (width - panelWidth) / 2;
		int y = height / 2 - panelHeight / 2;
		PixelTheme.drawPanel(graphics, x, y, panelWidth, panelHeight, 1.0F);
		graphics.drawCenteredString(
			font,
			Component.translatable("tactical_pickup.ui_editor.confirm_reset_all"),
			width / 2,
			y + 13,
			PixelTheme.TEXT
		);
	}

	private UiTransform transform(UiElement element, long now) {
		UiRect bounds = localBounds(element);
		UiPlacement placement = config.placement(element);
		double defaultCenterX;
		double defaultCenterY;
		if (element == UiElement.LOOT_HUD) {
			PickupHudPosition defaultPosition = PickupHudPosition.defaults(
				width,
				height,
				(int) Math.round(bounds.width()),
				(int) Math.round(bounds.height())
			);
			defaultCenterX = defaultPosition.x() + bounds.width() / 2.0D;
			defaultCenterY = defaultPosition.y() + bounds.height() / 2.0D;
		} else {
			defaultCenterX = bounds.centerX();
			defaultCenterY = bounds.centerY();
		}

		float renderedScale = editable.scale.value(now) * editable.dragScale.value(now);
		return UiTransform.create(
			bounds,
			placement.desiredCenterX(defaultCenterX, width),
			placement.desiredCenterY(defaultCenterY, height),
			renderedScale,
			width,
			height
		);
	}

	private UiRect localBounds(UiElement element) {
		return element == UiElement.LOOT_HUD
			? PickupHudRenderer.editorPreviewBounds(font)
			: LootScreenEditorPreview.bounds(width, height);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (confirmResetAll || button != 0) {
			return confirmResetAll;
		}

		long now = System.nanoTime();
		UiTransform transform = transform(element, now);
		if (!transform.containsScreen(mouseX, mouseY)) {
			return false;
		}
		dragOffsetX = mouseX - transform.centerX();
		dragOffsetY = mouseY - transform.centerY();
		dragging = true;
		editable.dragScale.setTarget(1.02F, DRAG_DURATION_MS, Easing.OUT_CUBIC, now);
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (!dragging || button != 0 || confirmResetAll) {
			return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}

		long now = System.nanoTime();
		UiRect bounds = localBounds(element);
		float renderedScale = editable.scale.value(now) * editable.dragScale.value(now);
		UiTransform clamped = UiTransform.create(
			bounds,
			mouseX - dragOffsetX,
			mouseY - dragOffsetY,
			renderedScale,
			width,
			height
		);
		config.update(
			element,
			clamped.centerX() / Math.max(1.0D, width),
			clamped.centerY() / Math.max(1.0D, height),
			editable.targetScale
		);
		saved = false;
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && dragging) {
			dragging = false;
			editable.dragScale.setTarget(1.0F, DRAG_DURATION_MS, Easing.OUT_CUBIC, System.nanoTime());
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
		if (confirmResetAll || vertical == 0.0D) {
			return confirmResetAll || super.mouseScrolled(mouseX, mouseY, vertical);
		}

		long now = System.nanoTime();
		UiTransform current = transform(element, now);
		if (!current.containsScreen(mouseX, mouseY)) {
			return super.mouseScrolled(mouseX, mouseY, vertical);
		}

		float direction = vertical > 0.0D ? 1.0F : -1.0F;
		float nextScale = element.clampScale(Math.round((editable.targetScale + direction * SCALE_STEP) * 20.0F) / 20.0F);
		if (Float.compare(nextScale, editable.targetScale) == 0) {
			return true;
		}

		editable.targetScale = nextScale;
		editable.scale.setTarget(nextScale, SCALE_DURATION_MS, Easing.OUT_CUBIC, now);
		config.update(
			element,
			current.centerX() / Math.max(1.0D, width),
			current.centerY() / Math.max(1.0D, height),
			nextScale
		);
		saved = false;
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_R && !confirmResetAll) {
			resetCurrent();
			return true;
		}
		if (ClientKeyMappings.EDIT_UI.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	private void resetCurrent() {
		config.reset(element);
		long now = System.nanoTime();
		editable.targetScale = 1.0F;
		editable.scale.setTarget(1.0F, SCALE_DURATION_MS, Easing.OUT_CUBIC, now);
		editable.dragScale.setTarget(1.0F, DRAG_DURATION_MS, Easing.OUT_CUBIC, now);
		dragging = false;
		saved = false;
	}

	private void confirmResetAll() {
		config.resetAll();
		long now = System.nanoTime();
		editable.targetScale = 1.0F;
		editable.scale.setTarget(1.0F, SCALE_DURATION_MS, Easing.OUT_CUBIC, now);
		editable.dragScale.setTarget(1.0F, DRAG_DURATION_MS, Easing.OUT_CUBIC, now);
		dragging = false;
		saved = false;
		setConfirmResetAll(false);
	}

	private void setConfirmResetAll(boolean confirm) {
		confirmResetAll = confirm;
		if (confirm) {
			dragging = false;
			editable.dragScale.setTarget(1.0F, DRAG_DURATION_MS, Easing.OUT_CUBIC, System.nanoTime());
		}
		updateButtonVisibility();
	}

	private void updateButtonVisibility() {
		if (resetCurrentButton == null) {
			return;
		}
		resetCurrentButton.visible = !confirmResetAll;
		resetCurrentButton.active = !confirmResetAll;
		resetAllButton.visible = !confirmResetAll;
		resetAllButton.active = !confirmResetAll;
		confirmResetButton.visible = confirmResetAll;
		confirmResetButton.active = confirmResetAll;
		cancelResetButton.visible = confirmResetAll;
		cancelResetButton.active = confirmResetAll;
	}

	@Override
	public void onClose() {
		config.save();
		saved = true;
		minecraft.setScreen(parent);
	}

	@Override
	public void removed() {
		if (!saved) {
			config.save();
			saved = true;
		}
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static final class EditableElement {
		private final AnimatedFloat scale;
		private final AnimatedFloat dragScale = new AnimatedFloat(1.0F);
		private float targetScale;

		private EditableElement(float scale) {
			this.scale = new AnimatedFloat(scale);
			targetScale = scale;
		}
	}
}
