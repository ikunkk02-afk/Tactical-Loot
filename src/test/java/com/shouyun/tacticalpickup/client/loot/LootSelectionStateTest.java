package com.shouyun.tacticalpickup.client.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LootSelectionStateTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void sameKeyRetainsAmountAndClampsWhenCountDrops() {
		LootSelectionState state = new LootSelectionState();
		LootGroup iron128 = group(Items.IRON_INGOT, 1, 128);
		state.select(iron128);
		state.adjust(-32, 1, iron128.totalCount());

		LootGroup iron100 = group(Items.IRON_INGOT, 2, 100);
		assertEquals(iron100, state.reconcile(List.of(iron100)));
		assertEquals(96, state.selectedAmount(iron100.totalCount()));
		assertFalse(state.pickupAll());

		LootGroup iron90 = group(Items.IRON_INGOT, 3, 90);
		state.reconcile(List.of(iron90));
		assertTrue(state.pickupAll());
		assertEquals(90, state.selectedAmount(iron90.totalCount()));
	}

	@Test
	void newGroupDefaultsToAllAndMissingSelectionClears() {
		LootSelectionState state = new LootSelectionState();
		LootGroup iron = group(Items.IRON_INGOT, 1, 64);
		state.select(iron);
		state.adjust(-16, 1, iron.totalCount());

		state.select(group(Items.DIAMOND, 2, 8));
		assertTrue(state.pickupAll());
		assertNull(state.reconcile(List.of(iron)));
		assertNull(state.selectedKey());
	}

	private static LootGroup group(Item item, int entityId, int totalCount) {
		ItemStack stack = new ItemStack(item);
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
