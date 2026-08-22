package com.shouyun.tacticalpickup.client.hud;

public record PickupHudPosition(int x, int y) {
	public static PickupHudPosition defaults(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
		int centerX = screenWidth / 2;
		int preferredRightX = centerX + PickupHudRenderer.CROSSHAIR_OFFSET;
		int x = preferredRightX + panelWidth <= screenWidth - PickupHudRenderer.SCREEN_MARGIN
			? preferredRightX
			: Math.max(
				PickupHudRenderer.SCREEN_MARGIN,
				centerX - PickupHudRenderer.CROSSHAIR_OFFSET - panelWidth
			);
		int centeredY = screenHeight / 2 - panelHeight / 2;
		int maxY = Math.max(PickupHudRenderer.SCREEN_MARGIN, screenHeight - panelHeight - PickupHudRenderer.SCREEN_MARGIN);
		int y = Math.max(PickupHudRenderer.SCREEN_MARGIN, Math.min(centeredY, maxY));
		return new PickupHudPosition(x, y);
	}
}
