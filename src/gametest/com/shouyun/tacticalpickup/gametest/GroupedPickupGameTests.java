package com.shouyun.tacticalpickup.gametest;

import com.shouyun.tacticalpickup.TacticalPickup;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupAggregator;
import com.shouyun.tacticalpickup.pickup.LootGroupMember;
import com.shouyun.tacticalpickup.pickup.PickupRequestHandler;
import java.util.List;
import java.util.UUID;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(TacticalPickup.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GroupedPickupGameTests {
	private static final Vec3 PLAYER_POSITION = new Vec3(2.0, 2.0, 2.0);

	@GameTest(template = "empty")
	public void groupAOrdinaryAggregation(GameTestHelper helper) {
		List<LootGroup> groups = group(
			new ItemStack(Items.IRON_INGOT, 10),
			new ItemStack(Items.IRON_INGOT, 15),
			new ItemStack(Items.IRON_INGOT, 4)
		);
		assertEquals(helper, groups.size(), 1, "Group A count");
		assertEquals(helper, groups.getFirst().totalCount(), 29, "Group A total");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void groupBMultipleGroups(GameTestHelper helper) {
		List<LootGroup> groups = group(
			new ItemStack(Items.IRON_INGOT, 10),
			new ItemStack(Items.IRON_INGOT, 15),
			new ItemStack(Items.DIAMOND, 2),
			new ItemStack(Items.ROTTEN_FLESH, 8)
		);
		assertEquals(helper, groups.size(), 3, "Group B count");
		assertGroupTotal(helper, groups, new ItemStack(Items.IRON_INGOT), 25, "Group B iron");
		assertGroupTotal(helper, groups, new ItemStack(Items.DIAMOND), 2, "Group B diamond");
		assertGroupTotal(helper, groups, new ItemStack(Items.ROTTEN_FLESH), 8, "Group B rotten flesh");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void groupCCustomNamesStaySeparate(GameTestHelper helper) {
		ItemStack first = namedIron("A", 10);
		ItemStack second = namedIron("B", 10);
		assertEquals(helper, group(first, second).size(), 2, "Group C custom names");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void groupDDamageStaysSeparate(GameTestHelper helper) {
		ItemStack first = new ItemStack(Items.DIAMOND_SWORD);
		first.setDamageValue(20);
		ItemStack second = new ItemStack(Items.DIAMOND_SWORD);
		second.setDamageValue(50);
		assertEquals(helper, group(first, second).size(), 2, "Group D damage");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void groupEEnchantmentsStaySeparate(GameTestHelper helper) {
		ItemStack sharpness = enchantedIron(helper, Enchantments.SHARPNESS);
		ItemStack unbreaking = enchantedIron(helper, Enchantments.UNBREAKING);
		assertEquals(helper, group(sharpness, unbreaking).size(), 2, "Group E enchantments");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void groupFPotionsStaySeparate(GameTestHelper helper) {
		ItemStack healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
		ItemStack strength = PotionContents.createItemStack(Items.POTION, Potions.STRENGTH);
		assertEquals(helper, group(healing, strength).size(), 2, "Group F potions");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void groupGCustomDataStaysSeparate(GameTestHelper helper) {
		ItemStack first = customDataIron("A", 10);
		ItemStack second = customDataIron("B", 10);
		assertEquals(helper, group(first, second).size(), 2, "Group G custom data");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void groupHPicksUpEntireGroup(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		List<ItemEntity> entities = spawnGroup(helper, 10, 15, 4);
		int before = matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT)) + groundCount(entities);

		PickupRequestHandler.handle(player, entities.getFirst().getId());

		assertEquals(helper, matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT)), 29, "Group H inventory");
		assertEquals(helper, groundCount(entities), 0, "Group H ground");
		assertEquals(helper, matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT)) + groundCount(entities), before, "Group H conservation");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void groupIPartialCapacityConservesItems(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		fillInventory(player.getInventory());
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 44));
		List<ItemEntity> entities = spawnGroup(helper, 30, 30, 30);
		int inventoryBefore = matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT));
		int before = inventoryBefore + groundCount(entities);

		PickupRequestHandler.handle(player, entities.getFirst().getId());

		int inventoryAfter = matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT));
		assertEquals(helper, inventoryAfter - inventoryBefore, 20, "Group I inserted");
		assertEquals(helper, groundCount(entities), 70, "Group I ground remainder");
		assertEquals(helper, inventoryAfter + groundCount(entities), before, "Group I conservation");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void groupJFullInventoryChangesNothing(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		fillInventory(player.getInventory());
		List<ItemEntity> entities = spawnGroup(helper, 64, 64, 32);

		PickupRequestHandler.handle(player, entities.getFirst().getId());

		assertEquals(helper, matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT)), 0, "Group J inventory");
		assertEquals(helper, groundCount(entities), 160, "Group J ground");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void groupKOnlyMatchingComponentsArePickedUp(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity ordinary = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 20), 0.2);
		ItemEntity named = spawnItem(helper, namedIron("named", 20), 0.4);
		ItemEntity custom = spawnItem(helper, customDataIron("custom", 20), 0.6);

		PickupRequestHandler.handle(player, ordinary.getId());

		assertEquals(helper, matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT)), 20, "Group K ordinary inventory");
		assertEquals(helper, remaining(named), 20, "Group K named ground");
		assertEquals(helper, remaining(custom), 20, "Group K custom ground");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void groupLPickupDelayIsCheckedPerMember(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity ready = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 10), 0.2);
		ItemEntity delayed = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 10), 0.4);
		delayed.setPickUpDelay(20);

		PickupRequestHandler.handle(player, ready.getId());

		assertEquals(helper, matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT)), 10, "Group L inventory");
		assertEquals(helper, remaining(delayed), 10, "Group L delayed ground");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void groupMOwnerIsCheckedPerMember(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		ItemEntity allowed = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 10), 0.2);
		ItemEntity owned = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 10), 0.4);
		owned.setTarget(UUID.randomUUID());

		PickupRequestHandler.handle(player, allowed.getId());

		assertEquals(helper, matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT)), 10, "Group M inventory");
		assertEquals(helper, remaining(owned), 10, "Group M owned ground");
		helper.succeed();
	}

	@SuppressWarnings("removal")
	@GameTest(template = "empty")
	public void groupNRepeatedRequestsRemainConservative(GameTestHelper helper) {
		ServerPlayer player = makePlayer(helper);
		List<ItemEntity> entities = spawnGroup(helper, 10, 15, 4);
		int before = groundCount(entities);

		for (int request = 0; request < 10; request++) {
			PickupRequestHandler.handle(player, entities.getFirst().getId());
		}

		int inventory = matchingInventoryCount(player.getInventory(), new ItemStack(Items.IRON_INGOT));
		assertEquals(helper, inventory, 29, "Group N inventory");
		assertEquals(helper, groundCount(entities), 0, "Group N ground");
		assertEquals(helper, inventory + groundCount(entities), before, "Group N conservation");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void identicalNonStackableItemsAggregate(GameTestHelper helper) {
		List<LootGroup> groups = group(
			new ItemStack(Items.DIAMOND_SWORD),
			new ItemStack(Items.DIAMOND_SWORD),
			new ItemStack(Items.DIAMOND_SWORD)
		);
		assertEquals(helper, groups.size(), 1, "Non-stackable group count");
		assertEquals(helper, groups.getFirst().totalCount(), 3, "Non-stackable total");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void groupTotalsAreNotClampedToStackSize(GameTestHelper helper) {
		List<LootGroup> groups = group(
			new ItemStack(Items.IRON_INGOT, 64),
			new ItemStack(Items.IRON_INGOT, 64),
			new ItemStack(Items.IRON_INGOT, 64)
		);
		assertEquals(helper, groups.getFirst().totalCount(), 192, "Large group total");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void groupIdentitySurvivesCountAndRepresentativeChanges(GameTestHelper helper) {
		LootGroup first = LootGroupAggregator.group(List.of(
			new LootGroupMember(100, new ItemStack(Items.IRON_INGOT, 20), 1.0),
			new LootGroupMember(101, new ItemStack(Items.IRON_INGOT, 20), 2.0)
		)).getFirst();
		LootGroup merged = LootGroupAggregator.group(List.of(
			new LootGroupMember(200, new ItemStack(Items.IRON_INGOT, 40), 1.5)
		)).getFirst();

		helper.assertTrue(first.key().equals(merged.key()), "Group identity must ignore count and representative entity ID");
		assertEquals(helper, first.totalCount(), merged.totalCount(), "Merged group total");
		helper.succeed();
	}

	private static List<LootGroup> group(ItemStack... stacks) {
		java.util.ArrayList<LootGroupMember> members = new java.util.ArrayList<>();

		for (int index = 0; index < stacks.length; index++) {
			members.add(new LootGroupMember(index + 1, stacks[index], index + 1.0));
		}

		return LootGroupAggregator.group(members);
	}

	private static void assertGroupTotal(GameTestHelper helper, List<LootGroup> groups, ItemStack reference, int expected, String label) {
		LootGroup group = groups.stream().filter(candidate -> candidate.key().matches(reference)).findFirst().orElse(null);
		helper.assertTrue(group != null, label + " missing");
		assertEquals(helper, group.totalCount(), expected, label + " total");
	}

	private static ItemStack namedIron(String name, int count) {
		ItemStack stack = new ItemStack(Items.IRON_INGOT, count);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		return stack;
	}

	private static ItemStack customDataIron(String value, int count) {
		ItemStack stack = new ItemStack(Items.IRON_INGOT, count);
		CompoundTag data = new CompoundTag();
		data.putString("group", value);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		return stack;
	}

	private static ItemStack enchantedIron(GameTestHelper helper, net.minecraft.resources.ResourceKey<Enchantment> enchantmentKey) {
		Holder<Enchantment> enchantment = helper.getLevel()
			.registryAccess()
			.registryOrThrow(Registries.ENCHANTMENT)
			.getHolderOrThrow(enchantmentKey);
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchantments.set(enchantment, 1);
		ItemStack stack = new ItemStack(Items.IRON_INGOT);
		stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
		return stack;
	}

	@SuppressWarnings("removal")
	private static ServerPlayer makePlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(PLAYER_POSITION));
		player.getInventory().clearContent();
		return player;
	}

	private static List<ItemEntity> spawnGroup(GameTestHelper helper, int... counts) {
		java.util.ArrayList<ItemEntity> entities = new java.util.ArrayList<>();

		for (int index = 0; index < counts.length; index++) {
			entities.add(spawnItem(helper, new ItemStack(Items.IRON_INGOT, counts[index]), 0.2 + index * 0.2));
		}

		return entities;
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

	private static int matchingInventoryCount(Inventory inventory, ItemStack reference) {
		int count = 0;

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);

			if (ItemStack.isSameItemSameComponents(stack, reference)) {
				count += stack.getCount();
			}
		}

		return count;
	}

	private static int groundCount(List<ItemEntity> entities) {
		return entities.stream().mapToInt(GroupedPickupGameTests::remaining).sum();
	}

	private static int remaining(ItemEntity itemEntity) {
		return itemEntity.isRemoved() ? 0 : itemEntity.getItem().getCount();
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String label) {
		helper.assertTrue(actual == expected, label + ": expected " + expected + ", got " + actual);
	}
}
