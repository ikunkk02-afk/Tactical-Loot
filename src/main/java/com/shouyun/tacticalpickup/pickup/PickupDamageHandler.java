package com.shouyun.tacticalpickup.pickup;

import com.shouyun.tacticalpickup.network.ExitPickupModePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PickupDamageHandler {
	private PickupDamageHandler() {
	}

	static void sendExit(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, ExitPickupModePayload.INSTANCE);
	}
}
