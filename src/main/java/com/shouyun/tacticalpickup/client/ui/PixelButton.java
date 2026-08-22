package com.shouyun.tacticalpickup.client.ui;

import com.shouyun.tacticalpickup.client.ui.animation.AnimatedFloat;
import com.shouyun.tacticalpickup.client.ui.animation.Easing;
import com.shouyun.tacticalpickup.client.ui.animation.GuiAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public final class PixelButton extends AbstractButton {
	private static final int PRESS_DURATION_MS = 120;

	private final OnPress onPress;
	private final AnimatedFloat hover = new AnimatedFloat(0.0F);
	private long pressStartNanos = Long.MIN_VALUE;
	private float visualOpacity = 1.0F;
	private boolean primary;

	public PixelButton(int x, int y, int width, int height, Component message, OnPress onPress) {
		super(x, y, width, height, message);
		this.onPress = onPress;
	}

	public PixelButton primary(boolean value) {
		primary = value;
		return this;
	}

	public void setVisualOpacity(float opacity) {
		visualOpacity = Easing.clamp(opacity);
	}

	@Override
	public void onPress() {
		pressStartNanos = System.nanoTime();
		onPress.onPress(this);
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		hover.setTarget(isHoveredOrFocused() && active ? 1.0F : 0.0F, isHoveredOrFocused() ? 80 : 80, Easing.OUT_CUBIC, now);
		float hoverValue = hover.value(now);
		float pressScale = pressScale(now);
		int visualWidth = Math.max(1, Math.round(getWidth() * pressScale));
		int visualHeight = Math.max(1, Math.round(getHeight() * pressScale));
		int visualX = getX() + (getWidth() - visualWidth) / 2;
		int visualY = getY() + (getHeight() - visualHeight) / 2;

		PixelTheme.drawInset(graphics, visualX, visualY, visualWidth, visualHeight, visualOpacity);
		int baseColor = primary ? PixelTheme.ACCENT : PixelTheme.EDGE_MID;
		float highlightOpacity = active ? 0.28F + hoverValue * 0.32F : 0.08F;
		graphics.fill(
			visualX + 2,
			visualY + 2,
			visualX + visualWidth - 2,
			visualY + visualHeight - 2,
			PixelTheme.color(baseColor, visualOpacity * highlightOpacity)
		);
		if (primary && active) {
			PixelTheme.drawBorder(
				graphics,
				visualX,
				visualY,
				visualWidth,
				visualHeight,
				PixelTheme.ACCENT,
				visualOpacity * (0.45F + hoverValue * 0.4F)
			);
		}

		int textColor = active ? PixelTheme.TEXT : PixelTheme.FAINT_TEXT;
		graphics.drawCenteredString(
			Minecraft.getInstance().font,
			getMessage(),
			getX() + getWidth() / 2,
			getY() + (getHeight() - 8) / 2,
			PixelTheme.color(textColor, visualOpacity)
		);
	}

	private float pressScale(long nowNanos) {
		if (pressStartNanos == Long.MIN_VALUE) {
			return 1.0F;
		}

		float progress = GuiAnimation.progress(nowNanos, pressStartNanos, PRESS_DURATION_MS);
		if (progress >= 1.0F) {
			pressStartNanos = Long.MIN_VALUE;
			return 1.0F;
		}

		if (progress < 1.0F / 3.0F) {
			return GuiAnimation.lerp(1.0F, 0.96F, Easing.IN_CUBIC.apply(progress * 3.0F));
		}
		return GuiAnimation.lerp(0.96F, 1.0F, Easing.OUT_CUBIC.apply((progress - 1.0F / 3.0F) * 1.5F));
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}

	@FunctionalInterface
	public interface OnPress {
		void onPress(PixelButton button);
	}
}
