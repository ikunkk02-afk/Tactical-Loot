package com.shouyun.tacticalpickup.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Creates a server-side test player without running Forge's real login handshake.
 * The vanilla GameTest helper uses a connection with no Netty channel, which Forge
 * 47 correctly rejects while injecting its registry filters.
 */
public final class GameTestPlayers {
	private GameTestPlayers() {
	}

	public static ServerPlayer create(GameTestHelper helper) {
		return new ServerPlayer(
			helper.getLevel().getServer(),
			helper.getLevel(),
			new GameProfile(UUID.randomUUID(), "tactical-loot-test")
		);
	}
}
