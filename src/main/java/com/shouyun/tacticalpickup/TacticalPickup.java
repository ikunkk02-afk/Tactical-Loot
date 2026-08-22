package com.shouyun.tacticalpickup;

import com.shouyun.tacticalpickup.network.PickupNetworking;
import com.shouyun.tacticalpickup.pickup.PickupEvents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TacticalPickup.MOD_ID)
public final class TacticalPickup {
	public static final String MOD_ID = "tactical_pickup";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public TacticalPickup(IEventBus modEventBus) {
		modEventBus.addListener(PickupNetworking::registerPayloads);
		NeoForge.EVENT_BUS.register(PickupEvents.class);
		LOGGER.info("Tactical Loot initialized");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
