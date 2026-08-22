package com.shouyun.tacticalpickup.filter;

public enum ItemFilterState {
	NORMAL(0, "tactical_pickup.filter.normal"),
	LOW_PRIORITY(1, "tactical_pickup.filter.low_priority"),
	HIDDEN(2, "tactical_pickup.filter.hidden");

	private final int sortRank;
	private final String translationKey;

	ItemFilterState(int sortRank, String translationKey) {
		this.sortRank = sortRank;
		this.translationKey = translationKey;
	}

	public ItemFilterState next() {
		return switch (this) {
			case NORMAL -> LOW_PRIORITY;
			case LOW_PRIORITY -> HIDDEN;
			case HIDDEN -> NORMAL;
		};
	}

	public int sortRank() {
		return sortRank;
	}

	public String translationKey() {
		return translationKey;
	}
}
