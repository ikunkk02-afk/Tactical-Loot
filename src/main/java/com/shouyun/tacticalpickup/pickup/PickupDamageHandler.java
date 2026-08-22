package com.shouyun.tacticalpickup.pickup;

import com.shouyun.tacticalpickup.network.ExitPickupModePayload;
import com.shouyun.tacticalpickup.network.PickupNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PickupDamageHandler {
	private PickupDamageHandler() {
	}

	@SubscribeEvent
	public static void afterDamage(LivingDamageEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
			sendExit(player);
		}
	}

	@SubscribeEvent
	public static void afterDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			sendExit(player);
		}
	}

	private static void sendExit(ServerPlayer player) {
		PickupNetworking.sendExit(player);
	}
}
