package com.shouyun.tacticalpickup.client.loot;

import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import net.minecraft.world.item.ItemStack;

public final class LootDragState {
	public static final double DRAG_THRESHOLD = 5.0D;
	private static final double DRAG_THRESHOLD_SQUARED = DRAG_THRESHOLD * DRAG_THRESHOLD;

	private Stage stage = Stage.NONE;
	private Snapshot snapshot;
	private double startX;
	private double startY;

	public void press(LootGroup group, int requestedAmount, double mouseX, double mouseY) {
		stage = Stage.PRESSED;
		snapshot = new Snapshot(
			group.key(),
			group.displayStack(),
			group.representativeEntityId(),
			requestedAmount
		);
		startX = mouseX;
		startY = mouseY;
	}

	public boolean update(double mouseX, double mouseY) {
		if (stage == Stage.PRESSED) {
			double deltaX = mouseX - startX;
			double deltaY = mouseY - startY;
			if (deltaX * deltaX + deltaY * deltaY >= DRAG_THRESHOLD_SQUARED) {
				stage = Stage.DRAGGING;
			}
		}

		return stage == Stage.DRAGGING;
	}

	public Snapshot release(boolean inDropZone) {
		Snapshot released = stage == Stage.DRAGGING && inDropZone ? snapshot : null;
		clear();
		return released;
	}

	public void clear() {
		stage = Stage.NONE;
		snapshot = null;
		startX = 0.0D;
		startY = 0.0D;
	}

	public Stage stage() {
		return stage;
	}

	public Snapshot snapshot() {
		return snapshot;
	}

	public boolean isActive() {
		return stage != Stage.NONE;
	}

	public boolean isDragging() {
		return stage == Stage.DRAGGING;
	}

	public enum Stage {
		NONE,
		PRESSED,
		DRAGGING
	}

	public record Snapshot(
		LootGroupKey key,
		ItemStack displayStack,
		int representativeEntityId,
		int requestedAmount
	) {
		public Snapshot {
			displayStack = displayStack.copy();
		}
	}
}
