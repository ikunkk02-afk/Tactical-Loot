package com.shouyun.tacticalpickup.client.hud;

import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.ui.ItemDetailHelper;
import com.shouyun.tacticalpickup.client.ui.PixelTheme;
import com.shouyun.tacticalpickup.client.ui.animation.AnimatedFloat;
import com.shouyun.tacticalpickup.client.ui.animation.Easing;
import com.shouyun.tacticalpickup.client.ui.animation.GuiAnimation;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class PickupHudRenderer {
	private static final int MIN_PANEL_WIDTH = 118;
	private static final int MAX_PANEL_WIDTH = 190;
	private static final int PANEL_PADDING = 5;
	private static final int ROW_HEIGHT = 22;
	private static final int SCREEN_MARGIN = 8;
	private static final int CROSSHAIR_OFFSET = 24;
	private static final int ENTER_DURATION_MS = 140;
	private static final int EXIT_DURATION_MS = 100;
	private static final int SWITCH_DURATION_MS = 120;
	private static final int QUANTITY_PULSE_MS = 100;
	private static final HudState STATE = new HudState();

	private PickupHudRenderer() {
	}

	public static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		ClientPickupManager manager = ClientPickupManager.getInstance();
		long now = System.nanoTime();
		boolean hardHidden = client.player == null || client.level == null || client.options.hideGui;
		if (hardHidden) {
			STATE.clear(now);
			return;
		}

		List<LootGroup> groups = manager.groups();
		boolean shouldShow = !groups.isEmpty() && client.screen == null && client.getOverlay() == null;
		STATE.setVisible(shouldShow, now);
		if (shouldShow) {
			boolean pickupMode = manager.isPickupMode();
			LootGroup group = pickupMode ? manager.selectedGroup() : groups.getFirst();
			if (group != null) {
				STATE.updateContent(
					client,
					manager,
					group,
					pickupMode,
					pickupMode ? manager.selectedIndex() : 0,
					groups.size(),
					now
				);
			}
		}

		float opacity = STATE.opacity(now);
		if (opacity <= 0.001F || STATE.current == null) {
			return;
		}

		Font font = client.font;
		int panelWidth = STATE.panelWidth(font, client.getWindow().getGuiScaledWidth());
		int panelHeight = STATE.panelHeight(font);
		int centerX = graphics.guiWidth() / 2;
		int preferredRightX = centerX + CROSSHAIR_OFFSET;
		int x = preferredRightX + panelWidth <= graphics.guiWidth() - SCREEN_MARGIN
			? preferredRightX
			: Math.max(SCREEN_MARGIN, centerX - CROSSHAIR_OFFSET - panelWidth);
		int centeredY = graphics.guiHeight() / 2 - panelHeight / 2;
		int maxY = Math.max(SCREEN_MARGIN, graphics.guiHeight() - panelHeight - SCREEN_MARGIN);
		int baseY = Math.max(SCREEN_MARGIN, Math.min(centeredY, maxY));
		float yOffset = STATE.targetVisible ? (1.0F - opacity) * 4.0F : -(1.0F - opacity) * 2.0F;
		float scale = STATE.targetVisible ? 0.97F + opacity * 0.03F : 1.0F;

		graphics.pose().pushPose();
		graphics.pose().translate(x + panelWidth / 2.0F, baseY + panelHeight / 2.0F + yOffset, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-(x + panelWidth / 2.0F), -(baseY + panelHeight / 2.0F), 0.0F);
		renderPanel(graphics, font, manager, x, baseY, panelWidth, panelHeight, opacity, now);
		graphics.pose().popPose();
	}

	private static void renderPanel(
			GuiGraphics graphics,
			Font font,
			ClientPickupManager manager,
			int x,
			int y,
			int panelWidth,
			int panelHeight,
			float opacity,
			long now
	) {
		PixelTheme.drawPanel(graphics, x, y, panelWidth, panelHeight, opacity * 0.96F);
		int cursorY = y + PANEL_PADDING;
		if (STATE.pickupMode) {
			Component title = Component.translatable(
				"tactical_pickup.hud.position",
				STATE.groupIndex + 1,
				STATE.groupCount
			);
			graphics.drawString(font, title, x + PANEL_PADDING, cursorY, PixelTheme.color(PixelTheme.ACCENT, opacity), true);
			cursorY += font.lineHeight + 3;
		}

		renderSwitchingContent(graphics, font, x, cursorY, panelWidth, opacity, now);
		cursorY += ROW_HEIGHT + 3;
		if (!STATE.pickupMode) {
			Component controls = passiveControls(STATE.groupCount);
			drawClipped(graphics, font, controls, x + PANEL_PADDING, cursorY, panelWidth - PANEL_PADDING * 2, PixelTheme.MUTED_TEXT, opacity);
			return;
		}

		if (!STATE.enchantments.isEmpty()) {
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.hud.enchantments"),
				x + PANEL_PADDING,
				cursorY,
				PixelTheme.color(PixelTheme.ACCENT, opacity),
				true
			);
			cursorY += font.lineHeight + 1;
			int visible = Math.min(STATE.enchantments.size(), ItemDetailHelper.MAX_VISIBLE_ENCHANTMENTS);
			for (int index = 0; index < visible; index++) {
				drawClipped(
					graphics,
					font,
					STATE.enchantments.get(index),
					x + PANEL_PADDING,
					cursorY,
					panelWidth - PANEL_PADDING * 2,
					PixelTheme.MUTED_TEXT,
					opacity
				);
				cursorY += font.lineHeight + 1;
			}
			cursorY += 2;
		}

		Component amount = manager.pickupAll()
			? Component.translatable("tactical_pickup.hud.amount_all", STATE.current.count)
			: Component.translatable("tactical_pickup.hud.amount", manager.selectedAmount(), STATE.current.count);
		drawClipped(graphics, font, amount, x + PANEL_PADDING, cursorY, panelWidth - PANEL_PADDING * 2, PixelTheme.TEXT, opacity);
		cursorY += font.lineHeight + 3;
		cursorY = drawInstruction(
			graphics,
			font,
			Component.translatable("tactical_pickup.hud.controls.selection"),
			x,
			cursorY,
			panelWidth,
			opacity
		);
		cursorY = drawInstruction(
			graphics,
			font,
			Component.translatable(
				"tactical_pickup.hud.controls.actions",
				ClientKeyMappings.CYCLE_FILTER.getTranslatedKeyMessage(),
				ClientKeyMappings.OPEN_LOOT_SCREEN.getTranslatedKeyMessage()
			),
			x,
			cursorY,
			panelWidth,
			opacity
		);
		drawInstruction(
			graphics,
			font,
			Component.translatable("tactical_pickup.hud.controls.exit"),
			x,
			cursorY,
			panelWidth,
			opacity
		);
	}

	private static void renderSwitchingContent(
			GuiGraphics graphics,
			Font font,
			int x,
			int y,
			int panelWidth,
			float opacity,
			long now
	) {
		float switchProgress = GuiAnimation.progress(now, STATE.switchStartedAtNanos, SWITCH_DURATION_MS);
		if (STATE.previous != null && switchProgress < 1.0F) {
			float oldProgress = Easing.IN_CUBIC.apply(switchProgress);
			renderContentRow(
				graphics,
				font,
				STATE.previous,
				x,
				y - Math.round(4.0F * oldProgress),
				panelWidth,
				opacity * (1.0F - oldProgress),
				0.0F
			);
			float newProgress = Easing.OUT_CUBIC.apply(switchProgress);
			renderContentRow(
				graphics,
				font,
				STATE.current,
				x,
				y + Math.round(4.0F * (1.0F - newProgress)),
				panelWidth,
				opacity * newProgress,
				STATE.quantityPulse(now)
			);
			return;
		}

		STATE.previous = null;
		renderContentRow(graphics, font, STATE.current, x, y, panelWidth, opacity, STATE.quantityPulse(now));
	}

	private static void renderContentRow(
			GuiGraphics graphics,
			Font font,
			HudContent content,
			int x,
			int y,
			int panelWidth,
			float opacity,
			float quantityPulse
	) {
		int slotX = x + PANEL_PADDING;
		PixelTheme.drawSlot(graphics, slotX, y, ROW_HEIGHT, ROW_HEIGHT, 0.0F, opacity);
		graphics.setColor(1.0F, 1.0F, 1.0F, Easing.clamp(opacity));
		graphics.renderItem(content.stack, slotX + 3, y + 3);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		int textX = slotX + ROW_HEIGHT + 4;
		Component count = Component.literal("×" + content.count);
		int countWidth = font.width(count);
		int availableNameWidth = Math.max(1, x + panelWidth - PANEL_PADDING - textX - countWidth - 3);
		String name = font.plainSubstrByWidth(content.name, availableNameWidth);
		int color = content.filterState == ItemFilterState.LOW_PRIORITY ? PixelTheme.LOW_PRIORITY : PixelTheme.TEXT;
		graphics.drawString(font, name, textX, y + 6, PixelTheme.color(color, opacity), true);

		float scale = 1.0F + quantityPulse * 0.15F;
		int countX = x + panelWidth - PANEL_PADDING - countWidth;
		float centerX = countX + countWidth / 2.0F;
		float centerY = y + 10.0F;
		graphics.pose().pushPose();
		graphics.pose().translate(centerX, centerY, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-centerX, -centerY, 0.0F);
		graphics.drawString(
			font,
			count,
			countX,
			y + 6,
			PixelTheme.color(quantityPulse > 0.0F ? PixelTheme.ACCENT_BRIGHT : color, opacity),
			true
		);
		graphics.pose().popPose();
	}

	private static void drawClipped(
			GuiGraphics graphics,
			Font font,
			Component component,
			int x,
			int y,
			int maxWidth,
			int color,
			float opacity
	) {
		String clipped = font.plainSubstrByWidth(component.getString(), Math.max(1, maxWidth));
		graphics.drawString(font, clipped, x, y, PixelTheme.color(color, opacity), true);
	}

	private static int drawInstruction(
			GuiGraphics graphics,
			Font font,
			Component component,
			int x,
			int y,
			int panelWidth,
			float opacity
	) {
		drawClipped(
			graphics,
			font,
			component,
			x + PANEL_PADDING,
			y,
			panelWidth - PANEL_PADDING * 2,
			PixelTheme.MUTED_TEXT,
			opacity
		);
		return y + font.lineHeight + 1;
	}

	private static Component passiveControls(int groupCount) {
		return groupCount > 1
			? Component.translatable(
				"tactical_pickup.hud.controls.passive_more",
				ClientKeyMappings.OPEN_LOOT_SCREEN.getTranslatedKeyMessage(),
				groupCount - 1
			)
			: Component.translatable(
				"tactical_pickup.hud.controls.passive",
				ClientKeyMappings.OPEN_LOOT_SCREEN.getTranslatedKeyMessage()
			);
	}

	private static final class HudState {
		private final AnimatedFloat visibility = new AnimatedFloat(0.0F);
		private boolean targetVisible;
		private HudContent current;
		private HudContent previous;
		private LootGroupKey enchantmentKey;
		private List<Component> enchantments = List.of();
		private boolean pickupMode;
		private int groupIndex;
		private int groupCount;
		private long switchStartedAtNanos = Long.MIN_VALUE;
		private long quantityPulseStartedAtNanos = Long.MIN_VALUE;

		private void setVisible(boolean visible, long now) {
			if (visible == targetVisible) {
				return;
			}
			targetVisible = visible;
			visibility.setTarget(visible ? 1.0F : 0.0F, visible ? ENTER_DURATION_MS : EXIT_DURATION_MS, Easing.OUT_CUBIC, now);
		}

		private float opacity(long now) {
			return visibility.value(now);
		}

		private void clear(long now) {
			targetVisible = false;
			visibility.snap(0.0F, now);
			current = null;
			previous = null;
			enchantments = List.of();
			enchantmentKey = null;
		}

		private void updateContent(
				Minecraft client,
				ClientPickupManager manager,
				LootGroup group,
				boolean nextPickupMode,
				int nextGroupIndex,
				int nextGroupCount,
				long now
		) {
			ItemFilterState state = manager.filterManager().getState(LootGroupFilter.itemId(group));
			if (current == null || !current.key.equals(group.key())) {
				previous = current;
				current = HudContent.of(group, state);
				switchStartedAtNanos = now;
				quantityPulseStartedAtNanos = now;
			} else if (current.count != group.totalCount() || current.filterState != state) {
				if (current.count != group.totalCount()) {
					quantityPulseStartedAtNanos = now;
				}
				current = HudContent.of(group, state);
			}

			pickupMode = nextPickupMode;
			groupIndex = nextGroupIndex;
			groupCount = nextGroupCount;
			if (pickupMode && !group.key().equals(enchantmentKey)) {
				enchantmentKey = group.key();
				enchantments = ItemDetailHelper.collectEnchantments(client, group.displayStack());
			} else if (!pickupMode) {
				enchantmentKey = null;
				enchantments = List.of();
			}
		}

		private float quantityPulse(long now) {
			if (quantityPulseStartedAtNanos == Long.MIN_VALUE) {
				return 0.0F;
			}
			float progress = GuiAnimation.progress(now, quantityPulseStartedAtNanos, QUANTITY_PULSE_MS);
			if (progress >= 1.0F) {
				quantityPulseStartedAtNanos = Long.MIN_VALUE;
				return 0.0F;
			}
			return progress < 0.5F
				? Easing.OUT_CUBIC.apply(progress * 2.0F)
				: 1.0F - Easing.IN_CUBIC.apply((progress - 0.5F) * 2.0F);
		}

		private int panelWidth(Font font, int screenWidth) {
			int width = ROW_HEIGHT + 4 + font.width(current.name) + font.width(" ×" + current.count);
			if (pickupMode) {
				width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.position", groupIndex + 1, groupCount)));
				for (Component enchantment : enchantments) {
					width = Math.max(width, font.width(enchantment));
				}
				width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.controls.selection")));
			} else {
				width = Math.max(width, font.width(passiveControls(groupCount)));
			}
			int screenLimit = Math.max(1, screenWidth - SCREEN_MARGIN * 2);
			return Math.min(Math.max(width + PANEL_PADDING * 2, MIN_PANEL_WIDTH), Math.min(MAX_PANEL_WIDTH, screenLimit));
		}

		private int panelHeight(Font font) {
			int height = PANEL_PADDING + ROW_HEIGHT + 3;
			if (pickupMode) {
				height += font.lineHeight + 3;
				if (!enchantments.isEmpty()) {
					height += font.lineHeight + 1;
					height += Math.min(enchantments.size(), ItemDetailHelper.MAX_VISIBLE_ENCHANTMENTS) * (font.lineHeight + 1);
					height += 2;
				}
				height += font.lineHeight + 3;
				height += 3 * (font.lineHeight + 1);
			} else {
				height += font.lineHeight;
			}
			return height + PANEL_PADDING;
		}
	}

	private record HudContent(
		LootGroupKey key,
		ItemStack stack,
		String name,
		int count,
		ItemFilterState filterState
	) {
		private static HudContent of(LootGroup group, ItemFilterState state) {
			return new HudContent(
				group.key(),
				group.displayStack().copyWithCount(1),
				group.displayStack().getHoverName().getString(),
				group.totalCount(),
				state
			);
		}
	}
}
