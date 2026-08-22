package com.shouyun.tacticalpickup.client.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LootDragStateTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void movementBelowFivePixelsRemainsASelectionClick() {
		LootDragState state = new LootDragState();
		state.press(group(1, 64), PickupRequestPayload.ALL_ITEMS, 10.0D, 10.0D);

		state.update(13.0D, 13.0D);
		assertEquals(LootDragState.Stage.PRESSED, state.stage());
		assertNull(state.release(true));
		assertEquals(LootDragState.Stage.NONE, state.stage());
	}

	@Test
	void fivePixelMovementStartsDragAndOnlyDropZoneReturnsRequest() {
		LootDragState state = new LootDragState();
		state.press(group(7, 64), 16, 10.0D, 10.0D);
		state.update(13.0D, 14.0D);

		assertEquals(LootDragState.Stage.DRAGGING, state.stage());
		LootDragState.Snapshot snapshot = state.release(true);
		assertNotNull(snapshot);
		assertEquals(7, snapshot.representativeEntityId());
		assertEquals(16, snapshot.requestedAmount());

		state.press(group(8, 64), 16, 0.0D, 0.0D);
		state.update(5.0D, 0.0D);
		assertNull(state.release(false));
	}

	private static LootGroup group(int entityId, int totalCount) {
		ItemStack stack = new ItemStack(Items.IRON_INGOT);
		return new LootGroup(
			LootGroupKey.of(stack),
			stack,
			List.of(entityId),
			totalCount,
			1.0D,
			entityId
		);
	}
}
