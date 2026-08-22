package com.shouyun.tacticalpickup.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class ItemFilterConfig {
	List<String> lowPriorityItems = new ArrayList<>();
	List<String> hiddenItems = new ArrayList<>();

	ItemFilterConfig() {
	}

	ItemFilterConfig(Collection<String> lowPriorityItems, Collection<String> hiddenItems) {
		this.lowPriorityItems = new ArrayList<>(lowPriorityItems);
		this.hiddenItems = new ArrayList<>(hiddenItems);
	}
}
