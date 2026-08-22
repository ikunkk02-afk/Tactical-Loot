package com.shouyun.tacticalpickup.client.loot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class LootSearchMatcherTest {
	private static final ResourceLocation DIAMOND_SWORD = ResourceLocation.fromNamespaceAndPath(
		"minecraft",
		"diamond_sword"
	);

	@Test
	void matchesLocalizedNameFullIdAndReadablePath() {
		assertTrue(LootSearchMatcher.matches("钻石剑", DIAMOND_SWORD, "钻石"));
		assertTrue(LootSearchMatcher.matches("Diamond Sword", DIAMOND_SWORD, "minecraft:diamond_sword"));
		assertTrue(LootSearchMatcher.matches("Diamond Sword", DIAMOND_SWORD, "diamond sword"));
	}

	@Test
	void matchingIsCaseInsensitiveAndRequiresEveryToken() {
		assertTrue(LootSearchMatcher.matches("Diamond Sword", DIAMOND_SWORD, "MINECRAFT SWORD"));
		assertFalse(LootSearchMatcher.matches("Diamond Sword", DIAMOND_SWORD, "diamond pickaxe"));
	}

	@Test
	void emptyQueryMatchesEverything() {
		assertTrue(LootSearchMatcher.matches("Diamond Sword", DIAMOND_SWORD, "   "));
	}
}
