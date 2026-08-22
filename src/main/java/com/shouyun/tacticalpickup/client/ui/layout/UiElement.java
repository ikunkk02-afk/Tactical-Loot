package com.shouyun.tacticalpickup.client.ui.layout;

public enum UiElement {
	LOOT_HUD(0.60F, 1.60F, "tactical_pickup.ui_editor.element.loot_hud"),
	LOOT_SCREEN(0.75F, 1.35F, "tactical_pickup.ui_editor.element.loot_screen");

	private final float minimumScale;
	private final float maximumScale;
	private final String translationKey;

	UiElement(float minimumScale, float maximumScale, String translationKey) {
		this.minimumScale = minimumScale;
		this.maximumScale = maximumScale;
		this.translationKey = translationKey;
	}

	public float minimumScale() {
		return minimumScale;
	}

	public float maximumScale() {
		return maximumScale;
	}

	public String translationKey() {
		return translationKey;
	}

	public float clampScale(float scale) {
		if (!Float.isFinite(scale)) {
			return 1.0F;
		}
		return Math.max(minimumScale, Math.min(scale, maximumScale));
	}
}
