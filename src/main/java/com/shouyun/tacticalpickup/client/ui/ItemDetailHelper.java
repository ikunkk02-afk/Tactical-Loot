package com.shouyun.tacticalpickup.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ItemDetailHelper {
	public static final int MAX_VISIBLE_ENCHANTMENTS = 5;

	private ItemDetailHelper() {
	}

	public static List<Component> collectEnchantments(Minecraft client, ItemStack stack) {
		List<Component> enchantments = new ArrayList<>();
		for (var entry : EnchantmentHelper.getEnchantments(stack).entrySet()) {
			enchantments.add(entry.getKey().getFullname(entry.getValue()));
		}
		return List.copyOf(enchantments);
	}
}
