package com.shouyun.tacticalpickup.client.ui.layout;

public record UiRect(double x, double y, double width, double height) {
	public double centerX() {
		return x + width / 2.0D;
	}

	public double centerY() {
		return y + height / 2.0D;
	}
}
