package com.shouyun.tacticalpickup.client.hud;

record PickupHudListWindow(int firstIndex, int visibleCount, int hiddenCount) {
	static PickupHudListWindow calculate(int groupCount, int selectedIndex, int maximumVisible) {
		if (groupCount <= 0 || maximumVisible <= 0) {
			return new PickupHudListWindow(0, 0, Math.max(0, groupCount));
		}

		int visibleCount = Math.min(groupCount, maximumVisible);
		int clampedSelectedIndex = Math.max(0, Math.min(selectedIndex, groupCount - 1));
		int firstIndex = Math.max(
			0,
			Math.min(clampedSelectedIndex - visibleCount / 2, groupCount - visibleCount)
		);
		return new PickupHudListWindow(firstIndex, visibleCount, groupCount - visibleCount);
	}
}
