package com.shouyun.tacticalpickup.client.loot;

import com.shouyun.tacticalpickup.network.DropInventorySlotPayload;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import net.minecraft.world.item.ItemStack;

public final class LootScreenDragState {
	public static final double DRAG_THRESHOLD = 5.0D;
	private static final double DRAG_THRESHOLD_SQUARED = DRAG_THRESHOLD * DRAG_THRESHOLD;

	private Stage stage = Stage.NONE;
	private Snapshot snapshot = EmptySnapshot.INSTANCE;
	private double startX;
	private double startY;

	public void pressLoot(LootGroup group, int requestedAmount, double mouseX, double mouseY) {
		press(new LootSnapshot(group.key(), group.displayStack(), group.representativeEntityId(), requestedAmount), mouseX, mouseY);
	}

	public void pressInventory(int sourceSlot, ItemStack stack, double mouseX, double mouseY) {
		press(new InventorySnapshot(sourceSlot, stack, DropInventorySlotPayload.ALL_ITEMS), mouseX, mouseY);
	}

	private void press(Snapshot nextSnapshot, double mouseX, double mouseY) {
		stage = Stage.PRESSED;
		snapshot = nextSnapshot;
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

	public Snapshot finish() {
		Snapshot finished = stage == Stage.DRAGGING ? snapshot : EmptySnapshot.INSTANCE;
		clear();
		return finished;
	}

	public void clear() {
		stage = Stage.NONE;
		snapshot = EmptySnapshot.INSTANCE;
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

	public enum Source {
		NONE,
		LOOT,
		INVENTORY
	}

	public sealed interface Snapshot permits EmptySnapshot, LootSnapshot, InventorySnapshot {
		Source source();

		ItemStack displayStack();

		int requestedAmount();
	}

	public enum EmptySnapshot implements Snapshot {
		INSTANCE;

		@Override
		public Source source() {
			return Source.NONE;
		}

		@Override
		public ItemStack displayStack() {
			return ItemStack.EMPTY;
		}

		@Override
		public int requestedAmount() {
			return 0;
		}
	}

	public record LootSnapshot(
		LootGroupKey key,
		ItemStack displayStack,
		int representativeEntityId,
		int requestedAmount
	) implements Snapshot {
		public LootSnapshot {
			displayStack = displayStack.copy();
		}

		@Override
		public Source source() {
			return Source.LOOT;
		}
	}

	public record InventorySnapshot(
		int sourceSlot,
		ItemStack displayStack,
		int requestedAmount
	) implements Snapshot {
		public InventorySnapshot {
			displayStack = displayStack.copy();
		}

		@Override
		public Source source() {
			return Source.INVENTORY;
		}
	}
}
