package com.shouyun.tacticalpickup.gametest;

import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import com.shouyun.tacticalpickup.pickup.PickupRequestHandler;
import java.util.List;
import com.shouyun.tacticalpickup.TacticalPickup;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(TacticalPickup.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AmountPickupGameTests {
	private static final Vec3 PLAYER_POSITION = new Vec3(2.0, 2.0, 2.0);

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountARequestsOne(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity ground = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupRequestHandler.handle(player, ground.getId(), 1);

		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 1, "Amount A inventory");
		assertEquals(helper, remaining(ground), 63, "Amount A ground");
		assertConserved(helper, player, List.of(ground), Items.IRON_INGOT, 64, "Amount A");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountBRequestsSixteen(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity ground = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupRequestHandler.handle(player, ground.getId(), 16);

		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 16, "Amount B inventory");
		assertEquals(helper, remaining(ground), 48, "Amount B ground");
		assertConserved(helper, player, List.of(ground), Items.IRON_INGOT, 64, "Amount B");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountCCrossesMultipleEntities(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity first = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 10), 0.25);
		ItemEntity second = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 20), 0.75);
		ItemEntity third = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 30), 1.25);
		List<ItemEntity> group = List.of(first, second, third);

		PickupRequestHandler.handle(player, first.getId(), 25);

		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 25, "Amount C inventory");
		assertEquals(helper, remaining(first), 0, "Amount C first entity");
		assertEquals(helper, remaining(second), 5, "Amount C second entity");
		assertEquals(helper, remaining(third), 30, "Amount C third entity");
		assertEquals(helper, countGround(group, Items.IRON_INGOT), 35, "Amount C ground total");
		assertConserved(helper, player, group, Items.IRON_INGOT, 60, "Amount C");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountDRequestCannotExceedReality(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity ground = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 40), 0.25);

		PickupRequestHandler.handle(player, ground.getId(), 1000);

		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 40, "Amount D inventory");
		assertEquals(helper, remaining(ground), 0, "Amount D ground");
		assertConserved(helper, player, List.of(ground), Items.IRON_INGOT, 40, "Amount D");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountEZeroMeansAll(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity first = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 10), 0.25);
		ItemEntity second = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 20), 0.75);
		List<ItemEntity> group = List.of(first, second);

		PickupRequestHandler.handle(player, first.getId(), PickupRequestPayload.ALL_ITEMS);

		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 30, "Amount E inventory");
		assertEquals(helper, countGround(group, Items.IRON_INGOT), 0, "Amount E ground");
		assertConserved(helper, player, group, Items.IRON_INGOT, 30, "Amount E");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountFNegativeIsRejected(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity ground = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 32), 0.25);

		PickupRequestHandler.handle(player, ground.getId(), -1);

		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 0, "Amount F inventory");
		assertEquals(helper, remaining(ground), 32, "Amount F ground");
		assertConserved(helper, player, List.of(ground), Items.IRON_INGOT, 32, "Amount F");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountGPartialCapacityOnlyRemovesInsertedItems(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		fillInventory(player.getInventory());
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 58));
		ItemEntity ground = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);
		int beforeTotal = countInventory(player.getInventory(), Items.IRON_INGOT) + remaining(ground);

		PickupRequestHandler.handle(player, ground.getId(), 20);

		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 64, "Amount G inventory");
		assertEquals(helper, remaining(ground), 58, "Amount G ground");
		assertConserved(helper, player, List.of(ground), Items.IRON_INGOT, beforeTotal, "Amount G");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountHNonStackableItemsRespectLimit(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity first = spawnItem(helper, new ItemStack(Items.DIAMOND_SWORD), 0.25);
		ItemEntity second = spawnItem(helper, new ItemStack(Items.DIAMOND_SWORD), 0.5);
		ItemEntity third = spawnItem(helper, new ItemStack(Items.DIAMOND_SWORD), 0.75);
		ItemEntity fourth = spawnItem(helper, new ItemStack(Items.DIAMOND_SWORD), 1.0);
		ItemEntity fifth = spawnItem(helper, new ItemStack(Items.DIAMOND_SWORD), 1.25);
		List<ItemEntity> group = List.of(first, second, third, fourth, fifth);

		PickupRequestHandler.handle(player, first.getId(), 2);

		assertEquals(helper, countInventory(player.getInventory(), Items.DIAMOND_SWORD), 2, "Amount H inventory");
		assertEquals(helper, countGround(group, Items.DIAMOND_SWORD), 3, "Amount H ground");
		assertConserved(helper, player, group, Items.DIAMOND_SWORD, 5, "Amount H");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void amountIRepeatedRequestsUseCurrentState(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity ground = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 25), 0.25);

		PickupRequestHandler.handle(player, ground.getId(), 10);
		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 10, "Amount I first inventory");
		assertEquals(helper, remaining(ground), 15, "Amount I first ground");

		PickupRequestHandler.handle(player, ground.getId(), 10);
		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 20, "Amount I second inventory");
		assertEquals(helper, remaining(ground), 5, "Amount I second ground");

		PickupRequestHandler.handle(player, ground.getId(), 10);
		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 25, "Amount I third inventory");
		assertEquals(helper, remaining(ground), 0, "Amount I third ground");

		PickupRequestHandler.handle(player, ground.getId(), 10);
		assertEquals(helper, countInventory(player.getInventory(), Items.IRON_INGOT), 25, "Amount I stale repeat inventory");
		assertEquals(helper, remaining(ground), 0, "Amount I stale repeat ground");
		assertConserved(helper, player, List.of(ground), Items.IRON_INGOT, 25, "Amount I");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	private static ServerPlayer makePlayer(GameTestHelper helper) {
		ServerPlayer player = GameTestPlayers.create(helper);
		player.setPos(helper.absoluteVec(PLAYER_POSITION));
		player.getInventory().clearContent();
		return player;
	}

	private static ItemEntity spawnItem(GameTestHelper helper, ItemStack stack, double xOffset) {
		Vec3 position = helper.absoluteVec(PLAYER_POSITION).add(xOffset, 0.0, 0.0);
		ItemEntity itemEntity = new ItemEntity(helper.getLevel(), position.x, position.y, position.z, stack);
		itemEntity.setDeltaMovement(Vec3.ZERO);
		itemEntity.setPickUpDelay(0);
		helper.getLevel().addFreshEntity(itemEntity);
		return itemEntity;
	}

	private static void fillInventory(Inventory inventory) {
		inventory.clearContent();

		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		}
	}

	private static int countInventory(Inventory inventory, Item item) {
		int total = 0;

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(item)) {
				total += stack.getCount();
			}
		}

		return total;
	}

	private static int countGround(List<ItemEntity> entities, Item item) {
		return entities.stream()
			.filter(entity -> !entity.isRemoved() && entity.getItem().is(item))
			.mapToInt(entity -> entity.getItem().getCount())
			.sum();
	}

	private static int remaining(ItemEntity entity) {
		return entity.isRemoved() ? 0 : entity.getItem().getCount();
	}

	private static void assertConserved(
			GameTestHelper helper,
			ServerPlayer player,
			List<ItemEntity> entities,
			Item item,
			int expectedTotal,
			String label
	) {
		assertEquals(
			helper,
			countInventory(player.getInventory(), item) + countGround(entities, item),
			expectedTotal,
			label + " conservation"
		);
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String label) {
		helper.assertTrue(actual == expected, label + ": expected " + expected + ", got " + actual);
	}
}
