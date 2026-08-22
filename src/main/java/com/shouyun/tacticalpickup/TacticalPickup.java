package com.shouyun.tacticalpickup;

import com.shouyun.tacticalpickup.network.PickupNetworking;
import com.shouyun.tacticalpickup.pickup.PickupDamageHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TacticalPickup.MOD_ID)
public class TacticalPickup {
	public static final String MOD_ID = "tactical_pickup";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public TacticalPickup(FMLJavaModLoadingContext context) {
		PickupNetworking.register();
		MinecraftForge.EVENT_BUS.addListener(this::blockAutomaticPickup);
		MinecraftForge.EVENT_BUS.register(PickupDamageHandler.class);
		LOGGER.info("Tactical Loot initialized");
	}

	private void blockAutomaticPickup(EntityItemPickupEvent event) {
		event.setCanceled(true);
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
