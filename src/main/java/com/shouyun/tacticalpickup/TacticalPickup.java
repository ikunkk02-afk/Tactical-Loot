package com.shouyun.tacticalpickup;

import com.shouyun.tacticalpickup.network.PickupNetworking;
import com.shouyun.tacticalpickup.pickup.PickupDamageHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class TacticalPickup implements ModInitializer {
	public static final String MOD_ID = "tactical_pickup";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PickupNetworking.register();
		PickupDamageHandler.register();
		LOGGER.info("Tactical Pickup initialized");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
