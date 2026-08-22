package com.shouyun.tacticalpickup.client.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

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

class LootScreenDragStateTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void movementBelowFivePixelsRemainsAPressWithoutAction() {
		LootScreenDragState state = new LootScreenDragState();
		state.pressLoot(group(1, 64), PickupRequestPayload.ALL_ITEMS, 10.0D, 10.0D);

		state.update(13.0D, 13.0D);
		assertEquals(LootScreenDragState.Stage.PRESSED, state.stage());
		assertSame(LootScreenDragState.EmptySnapshot.INSTANCE, state.finish());
		assertEquals(LootScreenDragState.Stage.NONE, state.stage());
	}

	@Test
	void fivePixelMovementProducesALootSnapshot() {
		LootScreenDragState state = new LootScreenDragState();
		state.pressLoot(group(7, 64), 16, 10.0D, 10.0D);
		state.update(13.0D, 14.0D);

		LootScreenDragState.LootSnapshot snapshot = assertInstanceOf(
			LootScreenDragState.LootSnapshot.class,
			state.finish()
		);
		assertEquals(LootScreenDragState.Source.LOOT, snapshot.source());
		assertEquals(7, snapshot.representativeEntityId());
		assertEquals(16, snapshot.requestedAmount());
	}

	@Test
	void inventorySourceIsDistinctAndCopiesTheDisplayStack() {
		LootScreenDragState state = new LootScreenDragState();
		ItemStack source = new ItemStack(Items.ROTTEN_FLESH, 64);
		state.pressInventory(17, source, 0.0D, 0.0D);
		source.setCount(1);
		state.update(5.0D, 0.0D);

		LootScreenDragState.InventorySnapshot snapshot = assertInstanceOf(
			LootScreenDragState.InventorySnapshot.class,
			state.finish()
		);
		assertEquals(LootScreenDragState.Source.INVENTORY, snapshot.source());
		assertEquals(17, snapshot.sourceSlot());
		assertEquals(64, snapshot.displayStack().getCount());
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
