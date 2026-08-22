package com.shouyun.tacticalpickup.gametest;

import com.shouyun.tacticalpickup.TacticalPickup;
import com.shouyun.tacticalpickup.client.loot.LootScreenDragState;
import com.shouyun.tacticalpickup.client.loot.LootSelectionState;
import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TacticalPickup.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ClientStateGameTests {
	@GameTest(template = "empty")
	public void movementBelowFivePixelsRemainsAPressWithoutAction(GameTestHelper helper) {
		LootScreenDragState state = new LootScreenDragState();
		state.pressLoot(group(Items.IRON_INGOT, 1, 64), PickupRequestPayload.ALL_ITEMS, 10.0D, 10.0D);
		state.update(13.0D, 13.0D);
		helper.assertTrue(state.stage() == LootScreenDragState.Stage.PRESSED, "Movement below threshold must remain pressed");
		helper.assertTrue(state.finish() == LootScreenDragState.EmptySnapshot.INSTANCE, "Press must finish without an action");
		helper.assertTrue(state.stage() == LootScreenDragState.Stage.NONE, "Finishing must reset the state");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void fivePixelMovementProducesALootSnapshot(GameTestHelper helper) {
		LootScreenDragState state = new LootScreenDragState();
		state.pressLoot(group(Items.IRON_INGOT, 7, 64), 16, 10.0D, 10.0D);
		state.update(13.0D, 14.0D);
		LootScreenDragState.Snapshot result = state.finish();
		helper.assertTrue(result instanceof LootScreenDragState.LootSnapshot, "Threshold movement must create a loot snapshot");
		LootScreenDragState.LootSnapshot snapshot = (LootScreenDragState.LootSnapshot) result;
		helper.assertTrue(snapshot.source() == LootScreenDragState.Source.LOOT, "Snapshot source must be loot");
		helper.assertTrue(snapshot.representativeEntityId() == 7, "Representative entity must be preserved");
		helper.assertTrue(snapshot.requestedAmount() == 16, "Requested amount must be preserved");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void inventoryDragCopiesDisplayStack(GameTestHelper helper) {
		LootScreenDragState state = new LootScreenDragState();
		ItemStack source = new ItemStack(Items.ROTTEN_FLESH, 64);
		state.pressInventory(17, source, 0.0D, 0.0D);
		source.setCount(1);
		state.update(5.0D, 0.0D);
		LootScreenDragState.InventorySnapshot snapshot = (LootScreenDragState.InventorySnapshot) state.finish();
		helper.assertTrue(snapshot.source() == LootScreenDragState.Source.INVENTORY, "Snapshot source must be inventory");
		helper.assertTrue(snapshot.sourceSlot() == 17, "Source slot must be preserved");
		helper.assertTrue(snapshot.displayStack().getCount() == 64, "Display stack must be an immutable snapshot copy");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void sameSelectionClampsWhenCountDrops(GameTestHelper helper) {
		LootSelectionState state = new LootSelectionState();
		LootGroup iron128 = group(Items.IRON_INGOT, 1, 128);
		state.select(iron128);
		state.adjust(-32, 1, iron128.totalCount());
		LootGroup iron100 = group(Items.IRON_INGOT, 2, 100);
		helper.assertTrue(state.reconcile(List.of(iron100)) == iron100, "Same logical group must remain selected");
		helper.assertTrue(state.selectedAmount(100) == 96 && !state.pickupAll(), "Explicit amount must be retained");
		LootGroup iron90 = group(Items.IRON_INGOT, 3, 90);
		state.reconcile(List.of(iron90));
		helper.assertTrue(state.pickupAll() && state.selectedAmount(90) == 90, "Amount beyond total must clamp to all");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void newAndMissingSelectionResetAmount(GameTestHelper helper) {
		LootSelectionState state = new LootSelectionState();
		LootGroup iron = group(Items.IRON_INGOT, 1, 64);
		state.select(iron);
		state.adjust(-16, 1, iron.totalCount());
		state.select(group(Items.DIAMOND, 2, 8));
		helper.assertTrue(state.pickupAll(), "A new group must default to all");
		helper.assertTrue(state.reconcile(List.of(iron)) == null && state.selectedKey() == null, "Missing selection must clear");
		helper.succeed();
	}

	private static LootGroup group(Item item, int entityId, int totalCount) {
		ItemStack stack = new ItemStack(item);
		return new LootGroup(LootGroupKey.of(stack), stack, List.of(entityId), totalCount, 1.0D, entityId);
	}
}
