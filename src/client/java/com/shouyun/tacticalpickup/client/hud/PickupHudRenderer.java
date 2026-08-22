package com.shouyun.tacticalpickup.client.hud;

import com.shouyun.tacticalpickup.client.config.ClientUiConfigManager;
import com.shouyun.tacticalpickup.client.input.ClientKeyMappings;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.client.ui.ItemDetailHelper;
import com.shouyun.tacticalpickup.client.ui.PixelTheme;
import com.shouyun.tacticalpickup.client.ui.animation.AnimatedFloat;
import com.shouyun.tacticalpickup.client.ui.animation.Easing;
import com.shouyun.tacticalpickup.client.ui.animation.GuiAnimation;
import com.shouyun.tacticalpickup.client.ui.layout.UiElement;
import com.shouyun.tacticalpickup.client.ui.layout.UiPlacement;
import com.shouyun.tacticalpickup.client.ui.layout.UiRect;
import com.shouyun.tacticalpickup.client.ui.layout.UiTransform;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import com.shouyun.tacticalpickup.pickup.PickupConstants;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PickupHudRenderer {
	private static final int MIN_PANEL_WIDTH = 118;
	private static final int MAX_PANEL_WIDTH = 190;
	private static final int PANEL_PADDING = 5;
	private static final int ROW_HEIGHT = 22;
	private static final int ROW_GAP = 2;
	private static final int SECTION_GAP = 3;
	static final int SCREEN_MARGIN = 8;
	static final int CROSSHAIR_OFFSET = 24;
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
		PickupHudPosition defaultPosition = PickupHudPosition.defaults(
			graphics.guiWidth(),
			graphics.guiHeight(),
			panelWidth,
			panelHeight
		);
		int x = defaultPosition.x();
		int baseY = defaultPosition.y();
		UiRect panelBounds = new UiRect(x, baseY, panelWidth, panelHeight);
		UiPlacement placement = ClientUiConfigManager.getInstance().placement(UiElement.LOOT_HUD);
		UiTransform uiTransform = UiTransform.create(
			panelBounds,
			placement.desiredCenterX(panelBounds.centerX(), graphics.guiWidth()),
			placement.desiredCenterY(panelBounds.centerY(), graphics.guiHeight()),
			placement.scale(),
			graphics.guiWidth(),
			graphics.guiHeight()
		);
		float yOffset = STATE.targetVisible ? (1.0F - opacity) * 4.0F : -(1.0F - opacity) * 2.0F;
		float scale = STATE.targetVisible ? 0.97F + opacity * 0.03F : 1.0F;

		graphics.pose().pushPose();
		uiTransform.apply(graphics);
		graphics.pose().translate(x + panelWidth / 2.0F, baseY + panelHeight / 2.0F + yOffset, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.pose().translate(-(x + panelWidth / 2.0F), -(baseY + panelHeight / 2.0F), 0.0F);
		renderPanel(graphics, font, manager, x, baseY, panelWidth, panelHeight, opacity, now);
		graphics.pose().popPose();
	}

	public static UiRect editorPreviewBounds(Font font) {
		List<HudContent> contents = editorPreviewContents();
		int width = MIN_PANEL_WIDTH;
		for (HudContent content : contents) {
			width = Math.max(width, ROW_HEIGHT + 4 + font.width(content.name) + font.width(" ×" + content.count) + PANEL_PADDING * 2);
		}
		width = Math.min(MAX_PANEL_WIDTH, Math.max(width, font.width(passiveControls()) + PANEL_PADDING * 2));
		int height = PANEL_PADDING
			+ contents.size() * ROW_HEIGHT
			+ Math.max(0, contents.size() - 1) * ROW_GAP
			+ SECTION_GAP
			+ font.lineHeight
			+ PANEL_PADDING;
		return new UiRect(0.0D, 0.0D, width, height);
	}

	public static void renderEditorPreview(GuiGraphics graphics, Font font) {
		List<HudContent> contents = editorPreviewContents();
		UiRect bounds = editorPreviewBounds(font);
		int width = (int) bounds.width();
		int height = (int) bounds.height();
		PixelTheme.drawPanel(graphics, 0, 0, width, height, 0.98F);
		int y = PANEL_PADDING;
		for (int index = 0; index < contents.size(); index++) {
			renderContentRow(graphics, font, contents.get(index), 0, y, width, 1.0F, false, 1.0F, 0.0F);
			y += ROW_HEIGHT;
			if (index + 1 < contents.size()) {
				y += ROW_GAP;
			}
		}
		y += SECTION_GAP;
		drawClipped(graphics, font, passiveControls(), PANEL_PADDING, y, width - PANEL_PADDING * 2, PixelTheme.MUTED_TEXT, 1.0F);
	}

	private static List<HudContent> editorPreviewContents() {
		return List.of(
			HudContent.preview(new ItemStack(Items.QUARTZ), 14),
			HudContent.preview(new ItemStack(Items.OAK_LOG), 32),
			HudContent.preview(new ItemStack(Items.IRON_INGOT), 8)
		);
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

		cursorY = renderLootList(graphics, font, x, cursorY, panelWidth, opacity, now);
		if (STATE.hiddenGroupCount > 0) {
			drawClipped(
				graphics,
				font,
				Component.translatable("tactical_pickup.hud.more", STATE.hiddenGroupCount),
				x + PANEL_PADDING,
				cursorY,
				panelWidth - PANEL_PADDING * 2,
				PixelTheme.MUTED_TEXT,
				opacity
			);
			cursorY += font.lineHeight + SECTION_GAP;
		}
		if (!STATE.pickupMode) {
			Component controls = passiveControls();
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

	private static int renderLootList(
			GuiGraphics graphics,
			Font font,
			int x,
			int y,
			int panelWidth,
			float opacity,
			long now
	) {
		float selectionProgress = Easing.OUT_CUBIC.apply(
			GuiAnimation.progress(now, STATE.switchStartedAtNanos, SWITCH_DURATION_MS)
		);
		for (int index = 0; index < STATE.visibleContents.size(); index++) {
			HudContent content = STATE.visibleContents.get(index);
			boolean selected = STATE.pickupMode && content.key.equals(STATE.current.key);
			float rowOpacity = selected || !STATE.pickupMode ? opacity : opacity * 0.72F;
			int rowY = selected ? y + Math.round(2.0F * (1.0F - selectionProgress)) : y;
			renderContentRow(
				graphics,
				font,
				content,
				x,
				rowY,
				panelWidth,
				rowOpacity,
				selected,
				selected ? selectionProgress : 1.0F,
				selected ? STATE.quantityPulse(now) : 0.0F
			);
			y += ROW_HEIGHT;
			if (index + 1 < STATE.visibleContents.size()) {
				y += ROW_GAP;
			}
		}
		return y + SECTION_GAP;
	}

	private static void renderContentRow(
			GuiGraphics graphics,
			Font font,
			HudContent content,
			int x,
			int y,
			int panelWidth,
			float opacity,
			boolean selected,
			float selectionProgress,
			float quantityPulse
	) {
		if (selected) {
			graphics.fill(
				x + 2,
				y,
				x + panelWidth - 2,
				y + ROW_HEIGHT,
				PixelTheme.color(0x30E2C27A, opacity * selectionProgress)
			);
		}
		int slotX = x + PANEL_PADDING;
		PixelTheme.drawSlot(graphics, slotX, y, ROW_HEIGHT, ROW_HEIGHT, selected ? selectionProgress : 0.0F, opacity);
		graphics.setColor(1.0F, 1.0F, 1.0F, Easing.clamp(opacity));
		graphics.renderItem(content.stack, slotX + 3, y + 3);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

		int textX = slotX + ROW_HEIGHT + 4;
		Component count = Component.literal("×" + content.count);
		int countWidth = font.width(count);
		int availableNameWidth = Math.max(1, x + panelWidth - PANEL_PADDING - textX - countWidth - 3);
		String name = font.plainSubstrByWidth(content.name, availableNameWidth);
		int color = selected
			? PixelTheme.ACCENT_BRIGHT
			: content.filterState == ItemFilterState.LOW_PRIORITY ? PixelTheme.LOW_PRIORITY : PixelTheme.TEXT;
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

	private static Component passiveControls() {
		return Component.translatable(
			"tactical_pickup.hud.controls.passive",
			ClientKeyMappings.OPEN_LOOT_SCREEN.getTranslatedKeyMessage()
		);
	}

	private static final class HudState {
		private final AnimatedFloat visibility = new AnimatedFloat(0.0F);
		private boolean targetVisible;
		private HudContent current;
		private List<LootGroup> sourceGroups = List.of();
		private List<HudContent> visibleContents = List.of();
		private LootGroupKey enchantmentKey;
		private List<Component> enchantments = List.of();
		private boolean pickupMode;
		private int groupIndex;
		private int groupCount;
		private int firstVisibleIndex;
		private int hiddenGroupCount;
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
			sourceGroups = List.of();
			visibleContents = List.of();
			enchantments = List.of();
			enchantmentKey = null;
			firstVisibleIndex = 0;
			hiddenGroupCount = 0;
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

			int maximumVisible = Math.min(nextGroupCount, PickupConstants.MAX_HUD_ENTRIES);
			int availableHeight = Math.max(1, client.getWindow().getGuiScaledHeight() - SCREEN_MARGIN * 2);
			while (maximumVisible > 1
					&& calculatePanelHeight(client.font, maximumVisible, nextGroupCount - maximumVisible) > availableHeight) {
				maximumVisible--;
			}
			PickupHudListWindow window = PickupHudListWindow.calculate(
				nextGroupCount,
				nextPickupMode ? nextGroupIndex : 0,
				maximumVisible
			);
			List<LootGroup> groups = manager.groups();
			if (groups != sourceGroups
					|| window.firstIndex() != firstVisibleIndex
					|| window.visibleCount() != visibleContents.size()) {
				sourceGroups = groups;
				firstVisibleIndex = window.firstIndex();
				int endIndex = firstVisibleIndex + window.visibleCount();
				visibleContents = groups.subList(firstVisibleIndex, endIndex).stream()
					.map(visibleGroup -> HudContent.of(
						visibleGroup,
						manager.filterManager().getState(LootGroupFilter.itemId(visibleGroup))
					))
					.toList();
			}
			hiddenGroupCount = window.hiddenCount();
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
			int width = 0;
			for (HudContent content : visibleContents) {
				width = Math.max(
					width,
					ROW_HEIGHT + 4 + font.width(content.name) + font.width(" ×" + content.count)
				);
			}
			if (hiddenGroupCount > 0) {
				width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.more", hiddenGroupCount)));
			}
			if (pickupMode) {
				width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.position", groupIndex + 1, groupCount)));
				for (Component enchantment : enchantments) {
					width = Math.max(width, font.width(enchantment));
				}
				width = Math.max(width, font.width(Component.translatable("tactical_pickup.hud.controls.selection")));
			} else {
				width = Math.max(width, font.width(passiveControls()));
			}
			int screenLimit = Math.max(1, screenWidth - SCREEN_MARGIN * 2);
			return Math.min(Math.max(width + PANEL_PADDING * 2, MIN_PANEL_WIDTH), Math.min(MAX_PANEL_WIDTH, screenLimit));
		}

		private int panelHeight(Font font) {
			return calculatePanelHeight(font, visibleContents.size(), hiddenGroupCount);
		}

		private int calculatePanelHeight(Font font, int visibleCount, int hiddenCount) {
			int height = PANEL_PADDING;
			if (pickupMode) {
				height += font.lineHeight + 3;
				if (!enchantments.isEmpty()) {
					height += font.lineHeight + 1;
					height += Math.min(enchantments.size(), ItemDetailHelper.MAX_VISIBLE_ENCHANTMENTS) * (font.lineHeight + 1);
					height += 2;
				}
			}
			height += visibleCount * ROW_HEIGHT;
			height += Math.max(0, visibleCount - 1) * ROW_GAP;
			height += SECTION_GAP;
			if (hiddenCount > 0) {
				height += font.lineHeight + SECTION_GAP;
			}
			if (pickupMode) {
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

		private static HudContent preview(ItemStack stack, int count) {
			return new HudContent(
				LootGroupKey.of(stack),
				stack.copyWithCount(1),
				stack.getHoverName().getString(),
				count,
				ItemFilterState.NORMAL
			);
		}
	}
}
