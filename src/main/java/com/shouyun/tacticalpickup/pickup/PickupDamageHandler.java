package com.shouyun.tacticalpickup.pickup;

import com.shouyun.tacticalpickup.network.ExitPickupModePayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class PickupDamageHandler {
	private PickupDamageHandler() {
	}

	public static void register() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer player && !blocked && damageTaken > 0.0F) {
				sendExit(player);
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				sendExit(player);
			}
		});
	}

	private static void sendExit(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, ExitPickupModePayload.TYPE)) {
			ServerPlayNetworking.send(player, ExitPickupModePayload.INSTANCE);
		}
	}
}
