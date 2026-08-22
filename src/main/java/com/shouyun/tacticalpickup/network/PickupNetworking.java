package com.shouyun.tacticalpickup.network;

import com.shouyun.tacticalpickup.TacticalPickup;
import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.pickup.InventoryDropTransaction;
import com.shouyun.tacticalpickup.pickup.PickupRequestHandler;
import com.shouyun.tacticalpickup.pickup.PickupToSlotRequestHandler;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PickupNetworking {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        TacticalPickup.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    private PickupNetworking() {}

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, PickupRequestPayload.class, PickupRequestPayload::encode,
            PickupRequestPayload::decode, PickupNetworking::handlePickup,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, PickupToSlotRequestPayload.class, PickupToSlotRequestPayload::encode,
            PickupToSlotRequestPayload::decode, PickupNetworking::handlePickupToSlot,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, DropInventorySlotPayload.class, DropInventorySlotPayload::encode,
            DropInventorySlotPayload::decode, PickupNetworking::handleDrop,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id, ExitPickupModePayload.class, ExitPickupModePayload::encode,
            ExitPickupModePayload::decode, PickupNetworking::handleExit,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendPickup(PickupRequestPayload message) { CHANNEL.sendToServer(message); }
    public static void sendPickupToSlot(PickupToSlotRequestPayload message) { CHANNEL.sendToServer(message); }
    public static void sendDrop(DropInventorySlotPayload message) { CHANNEL.sendToServer(message); }
    public static void sendExit(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), ExitPickupModePayload.INSTANCE);
    }

    private static void handlePickup(PickupRequestPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> PickupRequestHandler.handle(context.getSender(), message.entityId(), message.requestedAmount()));
        context.setPacketHandled(true);
    }
    private static void handlePickupToSlot(PickupToSlotRequestPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> PickupToSlotRequestHandler.handle(context.getSender(), message.entityId(), message.requestedAmount(), message.targetSlot()));
        context.setPacketHandled(true);
    }
    private static void handleDrop(DropInventorySlotPayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> InventoryDropTransaction.tryDrop(context.getSender(), message.sourceSlot(), message.requestedAmount()));
        context.setPacketHandled(true);
    }
    private static void handleExit(ExitPickupModePayload message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPickupManager.getInstance().exitPickupMode()));
        context.setPacketHandled(true);
    }
}
