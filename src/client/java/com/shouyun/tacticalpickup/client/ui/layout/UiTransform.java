package com.shouyun.tacticalpickup.client.ui.layout;

import net.minecraft.client.gui.GuiGraphics;

public final class UiTransform {
	public static final int SCREEN_MARGIN = 8;

	private final UiRect localBounds;
	private final double centerX;
	private final double centerY;
	private final float scale;

	private UiTransform(UiRect localBounds, double centerX, double centerY, float scale) {
		this.localBounds = localBounds;
		this.centerX = centerX;
		this.centerY = centerY;
		this.scale = scale;
	}

	public static UiTransform create(
			UiRect localBounds,
			double desiredCenterX,
			double desiredCenterY,
			float requestedScale,
			int screenWidth,
			int screenHeight
	) {
		double usableWidth = Math.max(1.0D, screenWidth - SCREEN_MARGIN * 2.0D);
		double usableHeight = Math.max(1.0D, screenHeight - SCREEN_MARGIN * 2.0D);
		float safeRequestedScale = Float.isFinite(requestedScale) && requestedScale > 0.0F
			? requestedScale
			: 1.0F;
		float fittedScale = (float) Math.min(
			safeRequestedScale,
			Math.min(usableWidth / Math.max(1.0D, localBounds.width()), usableHeight / Math.max(1.0D, localBounds.height()))
		);
		fittedScale = Math.max(0.01F, fittedScale);

		double halfWidth = localBounds.width() * fittedScale / 2.0D;
		double halfHeight = localBounds.height() * fittedScale / 2.0D;
		double centerX = clampCenter(desiredCenterX, halfWidth, screenWidth);
		double centerY = clampCenter(desiredCenterY, halfHeight, screenHeight);
		return new UiTransform(localBounds, centerX, centerY, fittedScale);
	}

	private static double clampCenter(double center, double halfExtent, int screenExtent) {
		double minimum = SCREEN_MARGIN + halfExtent;
		double maximum = screenExtent - SCREEN_MARGIN - halfExtent;
		if (minimum > maximum) {
			return screenExtent / 2.0D;
		}
		return Math.max(minimum, Math.min(center, maximum));
	}

	public void apply(GuiGraphics graphics) {
		graphics.pose().translate(centerX, centerY, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-localBounds.centerX(), -localBounds.centerY(), 0.0F);
	}

	public UiPoint screenToLocal(double screenX, double screenY) {
		return new UiPoint(
			localBounds.centerX() + (screenX - centerX) / scale,
			localBounds.centerY() + (screenY - centerY) / scale
		);
	}

	public UiPoint localToScreen(double localX, double localY) {
		return new UiPoint(
			centerX + (localX - localBounds.centerX()) * scale,
			centerY + (localY - localBounds.centerY()) * scale
		);
	}

	public UiRect screenBounds() {
		return new UiRect(
			centerX - localBounds.width() * scale / 2.0D,
			centerY - localBounds.height() * scale / 2.0D,
			localBounds.width() * scale,
			localBounds.height() * scale
		);
	}

	public boolean containsScreen(double screenX, double screenY) {
		UiRect bounds = screenBounds();
		return screenX >= bounds.x()
			&& screenX < bounds.x() + bounds.width()
			&& screenY >= bounds.y()
			&& screenY < bounds.y() + bounds.height();
	}

	public double centerX() {
		return centerX;
	}

	public double centerY() {
		return centerY;
	}

	public float scale() {
		return scale;
	}

	public record UiPoint(double x, double y) {
	}
}
