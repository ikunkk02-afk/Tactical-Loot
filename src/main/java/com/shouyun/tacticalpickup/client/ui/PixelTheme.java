package com.shouyun.tacticalpickup.client.ui;

import com.shouyun.tacticalpickup.client.ui.animation.GuiAnimation;
import net.minecraft.client.gui.GuiGraphics;

public final class PixelTheme {
	public static final int WORLD_DIM = 0x70000000;
	public static final int PANEL = 0xF01B1A18;
	public static final int PANEL_INNER = 0xF024221F;
	public static final int INSET = 0xF0121211;
	public static final int EDGE_DARK = 0xF0080808;
	public static final int EDGE_SHADOW = 0xE0100F0E;
	public static final int EDGE_MID = 0xD034312D;
	public static final int EDGE_LIGHT = 0xC0524D45;
	public static final int SLOT = 0xF0161513;
	public static final int SLOT_HOVER = 0xF02C2924;
	public static final int TEXT = 0xFFE7E0D0;
	public static final int MUTED_TEXT = 0xFFAAA398;
	public static final int FAINT_TEXT = 0xFF777168;
	public static final int ACCENT = 0xFFE2C27A;
	public static final int ACCENT_BRIGHT = 0xFFF1D99D;
	public static final int LOW_PRIORITY = 0xFF8E8064;
	public static final int COMPATIBLE = 0xFFB9C5A7;
	public static final int INCOMPATIBLE = 0xFFC07B70;

	private PixelTheme() {
	}

	public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, float opacity) {
		if (width <= 0 || height <= 0 || opacity <= 0.0F) {
			return;
		}

		fill(graphics, x, y, x + width, y + height, EDGE_DARK, opacity);
		fill(graphics, x + 1, y + 1, x + width - 1, y + height - 1, PANEL, opacity);
		fill(graphics, x + 2, y + 2, x + width - 2, y + height - 2, PANEL_INNER, opacity);
		fill(graphics, x + 1, y + 1, x + width - 1, y + 2, EDGE_LIGHT, opacity);
		fill(graphics, x + 1, y + 1, x + 2, y + height - 1, EDGE_LIGHT, opacity);
		fill(graphics, x + 1, y + height - 2, x + width - 1, y + height - 1, EDGE_SHADOW, opacity);
		fill(graphics, x + width - 2, y + 1, x + width - 1, y + height - 1, EDGE_SHADOW, opacity);
		drawSparseTexture(graphics, x + 3, y + 3, width - 6, height - 6, opacity);
	}

	public static void drawInset(GuiGraphics graphics, int x, int y, int width, int height, float opacity) {
		if (width <= 0 || height <= 0 || opacity <= 0.0F) {
			return;
		}

		fill(graphics, x, y, x + width, y + height, EDGE_DARK, opacity);
		fill(graphics, x + 1, y + 1, x + width - 1, y + height - 1, INSET, opacity);
		fill(graphics, x + 1, y + 1, x + width - 1, y + 2, EDGE_SHADOW, opacity);
		fill(graphics, x + 1, y + 1, x + 2, y + height - 1, EDGE_SHADOW, opacity);
		fill(graphics, x + 1, y + height - 2, x + width - 1, y + height - 1, EDGE_MID, opacity);
		fill(graphics, x + width - 2, y + 1, x + width - 1, y + height - 1, EDGE_MID, opacity);
	}

	public static void drawSlot(
			GuiGraphics graphics,
			int x,
			int y,
			int width,
			int height,
			float hover,
			float opacity
	) {
		int expansion = hover > 0.58F ? 1 : 0;
		int left = x - expansion;
		int top = y - expansion;
		int right = x + width + expansion;
		int bottom = y + height + expansion;
		fill(graphics, left, top, right, bottom, EDGE_DARK, opacity);
		fill(graphics, left + 1, top + 1, right - 1, bottom - 1, hover > 0.01F ? SLOT_HOVER : SLOT, opacity);
		fill(graphics, left + 1, top + 1, right - 1, top + 2, EDGE_SHADOW, opacity);
		fill(graphics, left + 1, top + 1, left + 2, bottom - 1, EDGE_SHADOW, opacity);
		fill(graphics, left + 1, bottom - 2, right - 1, bottom - 1, EDGE_MID, opacity);
		fill(graphics, right - 2, top + 1, right - 1, bottom - 1, EDGE_MID, opacity);
		if (hover > 0.01F) {
			fill(graphics, left + 2, top + 2, right - 2, bottom - 2, 0x20FFFFFF, opacity * hover);
		}
	}

	public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color, float opacity) {
		fill(graphics, x, y, x + width, y + 1, color, opacity);
		fill(graphics, x, y + height - 1, x + width, y + height, color, opacity);
		fill(graphics, x, y, x + 1, y + height, color, opacity);
		fill(graphics, x + width - 1, y, x + width, y + height, color, opacity);
	}

	public static int color(int color, float opacity) {
		return GuiAnimation.multiplyAlpha(color, opacity);
	}

	private static void drawSparseTexture(
			GuiGraphics graphics,
			int x,
			int y,
			int width,
			int height,
			float opacity
	) {
		if (width < 8 || height < 8) {
			return;
		}

		int samples = Math.min(18, Math.max(4, width * height / 1800));
		for (int index = 0; index < samples; index++) {
			int pixelX = x + Math.floorMod(index * 37 + width * 3, width);
			int pixelY = y + Math.floorMod(index * 23 + height * 5, height);
			int color = (index & 1) == 0 ? 0x0CFFFFFF : 0x10000000;
			fill(graphics, pixelX, pixelY, pixelX + 1, pixelY + 1, color, opacity);
		}
	}

	private static void fill(
			GuiGraphics graphics,
			int left,
			int top,
			int right,
			int bottom,
			int color,
			float opacity
	) {
		if (right > left && bottom > top) {
			graphics.fill(left, top, right, bottom, GuiAnimation.multiplyAlpha(color, opacity));
		}
	}
}
