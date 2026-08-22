package com.shouyun.tacticalpickup.network;

import com.shouyun.tacticalpickup.client.network.ClientPickupNetworking;
import com.shouyun.tacticalpickup.pickup.PickupRequestHandler;
import com.shouyun.tacticalpickup.pickup.PickupToSlotRequestHandler;
import com.shouyun.tacticalpickup.pickup.InventoryDropTransaction;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PickupNetworking {
	private PickupNetworking() {
	}

	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		registrar.playToServer(PickupRequestPayload.TYPE, PickupRequestPayload.STREAM_CODEC, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player) {
				PickupRequestHandler.handle(player, payload.entityId(), payload.requestedAmount());
			}
		});
		registrar.playToServer(PickupToSlotRequestPayload.TYPE, PickupToSlotRequestPayload.STREAM_CODEC, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player) {
				PickupToSlotRequestHandler.handle(player, payload.entityId(), payload.requestedAmount(), payload.targetSlot());
			}
		});
		registrar.playToServer(DropInventorySlotPayload.TYPE, DropInventorySlotPayload.STREAM_CODEC, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player) {
				InventoryDropTransaction.tryDrop(player, payload.sourceSlot(), payload.requestedAmount());
			}
		});
		registrar.playToClient(ExitPickupModePayload.TYPE, ExitPickupModePayload.STREAM_CODEC, (payload, context) -> {
			if (FMLEnvironment.dist == Dist.CLIENT) {
				ClientPickupNetworking.handleExitPayload();
			}
		});
	}
}
