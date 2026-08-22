package com.shouyun.tacticalpickup.pickup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LootGroupAggregator {
	private static final Comparator<LootGroupMember> MEMBER_ORDER = Comparator
		.comparingDouble(LootGroupMember::distanceSquared)
		.thenComparingInt(LootGroupMember::entityId);
	private static final Comparator<LootGroup> GROUP_ORDER = Comparator
		.comparingDouble(LootGroup::nearestDistanceSquared)
		.thenComparingInt(LootGroup::representativeEntityId);

	private LootGroupAggregator() {
	}

	public static List<LootGroup> group(List<LootGroupMember> members) {
		Map<LootGroupKey, MutableGroup> groups = new LinkedHashMap<>();

		for (LootGroupMember member : members) {
			LootGroupKey key = LootGroupKey.of(member.itemStack());
			groups.computeIfAbsent(key, MutableGroup::new).add(member);
		}

		return groups.values().stream()
			.map(MutableGroup::build)
			.sorted(GROUP_ORDER)
			.toList();
	}

	private static final class MutableGroup {
		private final LootGroupKey key;
		private final List<Integer> entityIds = new ArrayList<>();
		private LootGroupMember representative;
		private int totalCount;

		private MutableGroup(LootGroupKey key) {
			this.key = key;
		}

		private void add(LootGroupMember member) {
			entityIds.add(member.entityId());
			totalCount = Math.addExact(totalCount, member.itemStack().getCount());

			if (representative == null || MEMBER_ORDER.compare(member, representative) < 0) {
				representative = member;
			}
		}

		private LootGroup build() {
			return new LootGroup(
				key,
				representative.itemStack(),
				entityIds,
				totalCount,
				representative.distanceSquared(),
				representative.entityId()
			);
		}
	}
}
