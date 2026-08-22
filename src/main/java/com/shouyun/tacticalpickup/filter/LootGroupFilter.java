package com.shouyun.tacticalpickup.filter;

import com.shouyun.tacticalpickup.pickup.LootGroup;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class LootGroupFilter {
	private static final Comparator<RankedGroup> VISIBLE_GROUP_ORDER = Comparator
		.comparingInt((RankedGroup ranked) -> ranked.state().sortRank())
		.thenComparingDouble(ranked -> ranked.group().nearestDistanceSquared())
		.thenComparingInt(ranked -> ranked.group().representativeEntityId());

	private LootGroupFilter() {
	}

	public static List<LootGroup> apply(
			List<LootGroup> groups,
			Function<ResourceLocation, ItemFilterState> stateLookup
	) {
		return groups.stream()
			.map(group -> new RankedGroup(group, stateLookup.apply(itemId(group))))
			.filter(ranked -> ranked.state() != ItemFilterState.HIDDEN)
			.sorted(VISIBLE_GROUP_ORDER)
			.map(RankedGroup::group)
			.toList();
	}

	public static ResourceLocation itemId(LootGroup group) {
		return BuiltInRegistries.ITEM.getKey(group.displayStack().getItem());
	}

	private record RankedGroup(LootGroup group, ItemFilterState state) {
	}
}
