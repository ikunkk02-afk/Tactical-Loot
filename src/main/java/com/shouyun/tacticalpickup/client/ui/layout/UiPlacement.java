package com.shouyun.tacticalpickup.client.ui.layout;

public record UiPlacement(double normalizedX, double normalizedY, float scale, boolean customized) {
	public static UiPlacement defaults() {
		return new UiPlacement(0.5D, 0.5D, 1.0F, false);
	}

	public double desiredCenterX(double defaultCenterX, int screenWidth) {
		return customized ? normalizedX * screenWidth : defaultCenterX;
	}

	public double desiredCenterY(double defaultCenterY, int screenHeight) {
		return customized ? normalizedY * screenHeight : defaultCenterY;
	}
}
