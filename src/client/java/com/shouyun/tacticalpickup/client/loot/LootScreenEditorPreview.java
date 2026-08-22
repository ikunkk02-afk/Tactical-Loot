package com.shouyun.tacticalpickup.client.loot;

import com.shouyun.tacticalpickup.client.loot.LootScreenLayout.Bounds;
import com.shouyun.tacticalpickup.client.ui.PixelTheme;
import com.shouyun.tacticalpickup.client.ui.layout.UiRect;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LootScreenEditorPreview {
	private static final List<ItemStack> INVENTORY_ITEMS = List.of(
		new ItemStack(Items.DIAMOND_PICKAXE),
		new ItemStack(Items.BREAD, 12),
		new ItemStack(Items.TORCH, 48)
	);
	private static final List<ItemStack> LOOT_ITEMS = List.of(
		new ItemStack(Items.QUARTZ, 14),
		new ItemStack(Items.OAK_LOG, 32),
		new ItemStack(Items.IRON_INGOT, 8),
		new ItemStack(Items.REDSTONE, 24),
		new ItemStack(Items.GOLD_NUGGET, 6),
		new ItemStack(Items.LAPIS_LAZULI, 18)
	);

	private LootScreenEditorPreview() {
	}

	public static UiRect bounds(int screenWidth, int screenHeight) {
		Bounds panel = LootScreenLayout.calculate(screenWidth, screenHeight).panel();
		return new UiRect(panel.x(), panel.y(), panel.width(), panel.height());
	}

	public static void render(GuiGraphics graphics, Font font, int screenWidth, int screenHeight) {
		LootScreenLayout layout = LootScreenLayout.calculate(screenWidth, screenHeight);
		Bounds panel = layout.panel();
		PixelTheme.drawPanel(graphics, panel.x(), panel.y(), panel.width(), panel.height(), 0.98F);
		graphics.drawString(
			font,
			Component.translatable("tactical_pickup.loot.title"),
			panel.x() + 9,
			panel.y() + 8,
			PixelTheme.TEXT,
			false
		);
		Component count = Component.translatable("tactical_pickup.loot.group_count", LOOT_ITEMS.size());
		graphics.drawString(
			font,
			count,
			layout.closeButton().x() - 5 - font.width(count),
			panel.y() + 8,
			PixelTheme.MUTED_TEXT,
			false
		);
		graphics.drawCenteredString(font, "×", layout.closeButton().x() + 6, layout.closeButton().y() + 2, PixelTheme.TEXT);
		graphics.fill(panel.x() + 7, panel.y() + 20, panel.right() - 7, panel.y() + 21, PixelTheme.EDGE_MID);

		PixelTheme.drawPanel(
			graphics,
			layout.inventoryPanel().x(),
			layout.inventoryPanel().y(),
			layout.inventoryPanel().width(),
			layout.inventoryPanel().height(),
			1.0F
		);
		PixelTheme.drawPanel(
			graphics,
			layout.lootPanel().x(),
			layout.lootPanel().y(),
			layout.lootPanel().width(),
			layout.lootPanel().height(),
			1.0F
		);
		PixelTheme.drawPanel(
			graphics,
			layout.detailPanel().x(),
			layout.detailPanel().y(),
			layout.detailPanel().width(),
			layout.detailPanel().height(),
			1.0F
		);

		renderInventory(graphics, font, layout);
		renderLoot(graphics, font, layout);
		renderDetails(graphics, font, layout);
	}

	private static void renderInventory(GuiGraphics graphics, Font font, LootScreenLayout layout) {
		Bounds panel = layout.inventoryPanel();
		graphics.drawString(font, Component.translatable("tactical_pickup.loot.inventory"), panel.x() + 6, panel.y() + 5, PixelTheme.TEXT, false);
		for (int slot = 0; slot < 36; slot++) {
			Bounds bounds = layout.inventorySlotBounds(slot);
			if (!inside(panel, bounds)) {
				continue;
			}
			PixelTheme.drawSlot(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0.0F, 1.0F);
		}
		int[] sampleSlots = {0, 9, 10};
		for (int index = 0; index < sampleSlots.length; index++) {
			Bounds bounds = layout.inventorySlotBounds(sampleSlots[index]);
			if (inside(panel, bounds)) {
				ItemStack stack = INVENTORY_ITEMS.get(index);
				graphics.renderItem(stack, bounds.x() + 1, bounds.y() + 1);
				graphics.renderItemDecorations(font, stack, bounds.x() + 1, bounds.y() + 1);
			}
		}
	}

	private static void renderLoot(GuiGraphics graphics, Font font, LootScreenLayout layout) {
		Bounds panel = layout.lootPanel();
		Bounds viewport = layout.lootViewport();
		graphics.drawString(font, Component.translatable("tactical_pickup.loot.nearby"), panel.x() + 6, panel.y() + 6, PixelTheme.TEXT, false);
		Bounds search = layout.searchBox();
		PixelTheme.drawInset(graphics, search.x(), search.y(), search.width(), search.height(), 1.0F);
		Component searchText = Component.translatable("tactical_pickup.loot.search");
		graphics.drawString(
			font,
			font.plainSubstrByWidth(searchText.getString(), Math.max(1, search.width() - 7)),
			search.x() + 4,
			search.y() + 3,
			PixelTheme.FAINT_TEXT,
			false
		);

		for (int index = 0; index < LOOT_ITEMS.size(); index++) {
			Bounds slot = layout.lootSlotBounds(index, 0.0D);
			if (!slot.intersects(viewport)) {
				continue;
			}
			PixelTheme.drawSlot(graphics, slot.x(), slot.y(), slot.width(), slot.height(), index == 0 ? 1.0F : 0.0F, 1.0F);
			ItemStack stack = LOOT_ITEMS.get(index);
			graphics.renderItem(stack, slot.x() + 3, slot.y() + 3);
			graphics.renderItemDecorations(font, stack, slot.x() + 3, slot.y() + 3, Integer.toString(stack.getCount()));
			if (index == 0) {
				PixelTheme.drawBorder(graphics, slot.x(), slot.y(), slot.width(), slot.height(), PixelTheme.ACCENT_BRIGHT, 1.0F);
			}
		}
	}

	private static void renderDetails(GuiGraphics graphics, Font font, LootScreenLayout layout) {
		Bounds detail = layout.detailPanel();
		Bounds action = layout.actionPanel();
		if (!layout.stacked()) {
			ItemStack selected = LOOT_ITEMS.getFirst();
			Bounds text = layout.detailTextPanel();
			graphics.renderItem(selected, text.x() + 5, text.y() + 5);
			graphics.drawString(font, selected.getHoverName(), text.x() + 27, text.y() + 6, PixelTheme.TEXT, false);
			graphics.drawString(
				font,
				Component.translatable("tactical_pickup.loot.total", selected.getCount()),
				text.x() + 27,
				text.y() + 18,
				PixelTheme.MUTED_TEXT,
				false
			);
			PixelTheme.drawInset(graphics, action.x(), action.y(), action.width(), action.height(), 0.9F);
		}

		int rowWidth = Math.min(160, Math.max(108, action.width() - 10));
		int rowX = action.x() + (action.width() - rowWidth) / 2;
		int buttonY = layout.stacked() ? action.y() + 3 : action.y() + 22;
		int secondY = layout.stacked() ? action.y() + 25 : action.y() + 48;
		PixelTheme.drawInset(graphics, rowX, buttonY, rowWidth, 18, 1.0F);
		graphics.drawCenteredString(font, Component.literal("−16    14 / 14    +16"), rowX + rowWidth / 2, buttonY + 5, PixelTheme.TEXT);
		int gap = 4;
		int allWidth = Math.max(44, rowWidth * 2 / 5);
		PixelTheme.drawInset(graphics, rowX, secondY, allWidth, 20, 1.0F);
		PixelTheme.drawInset(graphics, rowX + allWidth + gap, secondY, rowWidth - allWidth - gap, 20, 1.0F);
		graphics.drawCenteredString(
			font,
			Component.translatable("tactical_pickup.loot.amount_all_button"),
			rowX + allWidth / 2,
			secondY + 6,
			PixelTheme.TEXT
		);
		graphics.drawCenteredString(
			font,
			Component.translatable("tactical_pickup.loot.pickup"),
			rowX + allWidth + gap + (rowWidth - allWidth - gap) / 2,
			secondY + 6,
			PixelTheme.ACCENT_BRIGHT
		);
	}

	private static boolean inside(Bounds outer, Bounds inner) {
		return inner.x() >= outer.x()
			&& inner.y() >= outer.y()
			&& inner.right() <= outer.right()
			&& inner.bottom() <= outer.bottom();
	}
}
