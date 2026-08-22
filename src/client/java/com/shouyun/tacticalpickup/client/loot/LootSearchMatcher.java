package com.shouyun.tacticalpickup.client.loot;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

public final class LootSearchMatcher {
	private LootSearchMatcher() {
	}

	public static boolean matches(String localizedName, ResourceLocation itemId, String query) {
		String normalizedQuery = normalize(query);
		if (normalizedQuery.isEmpty()) {
			return true;
		}

		String searchable = normalize(
			localizedName + " " + itemId + " " + itemId.getPath().replace('_', ' ')
		);
		for (String token : normalizedQuery.split("\\s+")) {
			if (!searchable.contains(token)) {
				return false;
			}
		}

		return true;
	}

	public static String normalize(String value) {
		return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
	}
}
