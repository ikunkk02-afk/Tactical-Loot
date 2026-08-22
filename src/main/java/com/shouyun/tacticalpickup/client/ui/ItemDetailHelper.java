package com.shouyun.tacticalpickup.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class ItemDetailHelper {
	public static final int MAX_VISIBLE_ENCHANTMENTS = 5;

	private ItemDetailHelper() {
	}

	public static List<Component> collectEnchantments(Minecraft client, ItemStack stack) {
		List<Component> enchantments = new ArrayList<>();
		Item.TooltipContext context = Item.TooltipContext.of(client.level);
		addEnchantments(stack.get(DataComponents.STORED_ENCHANTMENTS), context, enchantments);
		addEnchantments(stack.get(DataComponents.ENCHANTMENTS), context, enchantments);
		return List.copyOf(enchantments);
	}

	private static void addEnchantments(
			ItemEnchantments itemEnchantments,
			Item.TooltipContext context,
			List<Component> output
	) {
		if (itemEnchantments != null && !itemEnchantments.isEmpty()) {
			itemEnchantments.addToTooltip(context, output::add, TooltipFlag.NORMAL);
		}
	}
}
