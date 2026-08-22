package com.shouyun.tacticalpickup.gametest;

import com.shouyun.tacticalpickup.pickup.PickupToSlotRequestHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class TargetedPickupGameTests implements FabricGameTest {
	private static final Vec3 TEST_POSITION = new Vec3(2.0, 2.0, 2.0);

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void slotAEmptyTargetAcceptsFullStack(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemEntity ground = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupToSlotRequestHandler.handle(player, ground.getId(), 0, 17);

		assertCount(helper, player.getInventory().getItem(17), 64, "Slot A inventory");
		assertGround(helper, ground, 0, "Slot A ground");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void slotBMatchingTargetOnlyAcceptsRemainingCapacity(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(17, new ItemStack(Items.IRON_INGOT, 63));
		ItemEntity ground = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupToSlotRequestHandler.handle(player, ground.getId(), 0, 17);

		assertCount(helper, player.getInventory().getItem(17), 64, "Slot B inventory");
		assertGround(helper, ground, 63, "Slot B ground");
		ground.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void slotCIncompatibleTargetRejectsWithoutMutation(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(17, new ItemStack(Items.DIAMOND));
		ItemEntity ground = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupToSlotRequestHandler.handle(player, ground.getId(), 0, 17);

		assertCount(helper, player.getInventory().getItem(17), 1, "Slot C inventory");
		helper.assertTrue(player.getInventory().getItem(17).is(Items.DIAMOND), "Slot C item must remain diamond");
		assertGround(helper, ground, 64, "Slot C ground");
		ground.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void slotDGroupedGroundNeverOverflowsOneSlot(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemEntity first = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);
		ItemEntity second = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.75);

		PickupToSlotRequestHandler.handle(player, first.getId(), 0, 17);

		assertCount(helper, player.getInventory().getItem(17), 64, "Slot D inventory");
		int groundTotal = remaining(first) + remaining(second);
		helper.assertTrue(groundTotal == 64, "Slot D ground: expected 64, got " + groundTotal);
		if (!first.isRemoved()) first.discard();
		if (!second.isRemoved()) second.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void slotECustomAmountUsesRequestedMaximum(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemEntity ground = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupToSlotRequestHandler.handle(player, ground.getId(), 16, 17);

		assertCount(helper, player.getInventory().getItem(17), 16, "Slot E inventory");
		assertGround(helper, ground, 48, "Slot E ground");
		ground.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void slotFDifferentComponentsDoNotMerge(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack named = new ItemStack(Items.IRON_INGOT, 63);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("named"));
		player.getInventory().setItem(17, named);
		ItemEntity ground = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupToSlotRequestHandler.handle(player, ground.getId(), 0, 17);

		assertCount(helper, player.getInventory().getItem(17), 63, "Slot F inventory");
		assertGround(helper, ground, 64, "Slot F ground");
		ground.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void slotGInvalidIndicesAreRejected(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemEntity ground = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupToSlotRequestHandler.handle(player, ground.getId(), 0, -1);
		PickupToSlotRequestHandler.handle(player, ground.getId(), 0, 36);
		PickupToSlotRequestHandler.handle(player, ground.getId(), 0, 999);

		assertGround(helper, ground, 64, "Slot G ground");
		helper.assertTrue(player.getInventory().isEmpty(), "Slot G inventory must remain empty");
		ground.discard();
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void slotHRepeatedRequestsCannotDuplicate(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemEntity ground = spawn(helper, new ItemStack(Items.IRON_INGOT, 64), 0.25);

		PickupToSlotRequestHandler.handle(player, ground.getId(), 0, 17);
		for (int repetition = 0; repetition < 10; repetition++) {
			PickupToSlotRequestHandler.handle(player, ground.getId(), 0, 17);
		}

		assertCount(helper, player.getInventory().getItem(17), 64, "Slot H inventory");
		assertGround(helper, ground, 0, "Slot H ground");
		helper.succeed();
	}

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(TEST_POSITION));
		player.getInventory().clearContent();
		return player;
	}

	private static ItemEntity spawn(GameTestHelper helper, ItemStack stack, double xOffset) {
		Vec3 position = helper.absoluteVec(TEST_POSITION).add(xOffset, 0.0, 0.0);
		ItemEntity entity = new ItemEntity(helper.getLevel(), position.x, position.y, position.z, stack);
		entity.setDeltaMovement(Vec3.ZERO);
		entity.setPickUpDelay(0);
		helper.getLevel().addFreshEntity(entity);
		return entity;
	}

	private static int remaining(ItemEntity entity) {
		return entity.isRemoved() ? 0 : entity.getItem().getCount();
	}

	private static void assertCount(GameTestHelper helper, ItemStack stack, int expected, String label) {
		helper.assertTrue(stack.getCount() == expected, label + ": expected " + expected + ", got " + stack.getCount());
	}

	private static void assertGround(GameTestHelper helper, ItemEntity entity, int expected, String label) {
		int actual = remaining(entity);
		helper.assertTrue(actual == expected, label + ": expected " + expected + ", got " + actual);
	}
}
