package com.shouyun.tacticalpickup.client.pickup;

import net.minecraft.world.item.ItemStack;

public record PickupEntry(int entityId, ItemStack itemStack, double distanceSquared) {
}
