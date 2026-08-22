package com.shouyun.tacticalpickup.pickup;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public final class PickupEvents {
	private PickupEvents() {
	}

	@SubscribeEvent
	public static void blockAutomaticPickup(ItemEntityPickupEvent.Pre event) {
		event.setCanPickup(TriState.FALSE);
	}

	@SubscribeEvent
	public static void exitAfterDamage(LivingDamageEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0.0F) {
			PickupDamageHandler.sendExit(player);
		}
	}

	@SubscribeEvent
	public static void exitAfterDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			PickupDamageHandler.sendExit(player);
		}
	}
}
