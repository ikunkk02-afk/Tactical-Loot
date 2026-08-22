package com.shouyun.tacticalpickup.gametest;

import com.shouyun.tacticalpickup.pickup.PickupRequestHandler;
import java.util.UUID;
import com.shouyun.tacticalpickup.TacticalPickup;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(TacticalPickup.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ManualPickupGameTests {
	private static final Vec3 TEST_POSITION = new Vec3(2.0, 2.0, 2.0);

	@SuppressWarnings("removal")
	@GameTest(template = "empty", timeoutTicks = 100)
	public void manualPickupConservesItems(GameTestHelper helper) {
		ServerPlayer player = GameTestPlayers.create(helper);
		player.setPos(helper.absoluteVec(TEST_POSITION));

		testA(helper, player);
		testB(helper, player);
		testC(helper, player);
		testD(helper, player);
		testE(helper, player);
		testF(helper, player);
		testG(helper, player);
		testH(helper, player);
		testI(helper, player);
		testRepeatedRequests(helper, player);
		testCreativeMode(helper, player);
		testVanillaCollisionRemainsBlocked(helper, player);
		testServerValidation(helper, player);
		testPersistenceRoundTrip(helper, player);

		helper.succeed();
	}

	private static void testA(GameTestHelper helper, ServerPlayer player) {
		fillInventory(player.getInventory());
		assertPickup(helper, player, new ItemStack(Items.IRON_INGOT, 64), 0, 64, "Test A");
	}

	private static void testB(GameTestHelper helper, ServerPlayer player) {
		fillInventory(player.getInventory());
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 63));
		assertPickup(helper, player, new ItemStack(Items.IRON_INGOT, 64), 64, 63, "Test B");
	}

	private static void testC(GameTestHelper helper, ServerPlayer player) {
		fillInventory(player.getInventory());
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 60));
		assertPickup(helper, player, new ItemStack(Items.IRON_INGOT, 64), 64, 60, "Test C");
	}

	private static void testD(GameTestHelper helper, ServerPlayer player) {
		fillInventory(player.getInventory());
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 63));
		player.getInventory().setItem(1, new ItemStack(Items.IRON_INGOT, 61));

		ItemEntity itemEntity = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 64));
		int beforeTotal = countItem(player.getInventory(), Items.IRON_INGOT) + itemEntity.getItem().getCount();
		int statBefore = player.getStats().getValue(Stats.ITEM_PICKED_UP.get(Items.IRON_INGOT));

		PickupRequestHandler.handle(player, itemEntity.getId());

		assertEquals(helper, player.getInventory().getItem(0).getCount(), 64, "Test D slot A");
		assertEquals(helper, player.getInventory().getItem(1).getCount(), 64, "Test D slot B");
		assertEquals(helper, itemEntity.getItem().getCount(), 60, "Test D ground remainder");
		assertConserved(helper, player, itemEntity, Items.IRON_INGOT, beforeTotal, "Test D");
		assertEquals(helper, player.getStats().getValue(Stats.ITEM_PICKED_UP.get(Items.IRON_INGOT)) - statBefore, 4, "Test D statistic");
		itemEntity.discard();
	}

	private static void testE(GameTestHelper helper, ServerPlayer player) {
		player.getInventory().clearContent();
		assertPickup(helper, player, new ItemStack(Items.IRON_INGOT, 64), 64, 0, "Test E");
	}

	private static void testF(GameTestHelper helper, ServerPlayer player) {
		player.getInventory().clearContent();
		assertPickup(helper, player, new ItemStack(Items.IRON_INGOT, 32), 32, 0, "Test F");
	}

	private static void testG(GameTestHelper helper, ServerPlayer player) {
		fillInventory(player.getInventory());
		assertPickup(helper, player, new ItemStack(Items.IRON_INGOT), 0, 1, "Test G");
	}

	private static void testH(GameTestHelper helper, ServerPlayer player) {
		fillInventory(player.getInventory());
		assertPickup(helper, player, new ItemStack(Items.DIAMOND_SWORD), 0, 1, "Test H");
	}

	private static void testI(GameTestHelper helper, ServerPlayer player) {
		ItemStack damagedInventorySword = new ItemStack(Items.DIAMOND_SWORD);
		damagedInventorySword.setDamageValue(1);
		ItemStack damagedGroundSword = new ItemStack(Items.DIAMOND_SWORD);
		damagedGroundSword.setDamageValue(2);
		assertComponentDifferenceNotMerged(helper, player, damagedInventorySword, damagedGroundSword, "Test I durability");

		ItemStack namedInventoryStack = new ItemStack(Items.IRON_INGOT, 63);
		namedInventoryStack.setHoverName(Component.literal("inventory"));
		ItemStack namedGroundStack = new ItemStack(Items.IRON_INGOT, 64);
		namedGroundStack.setHoverName(Component.literal("ground"));
		assertComponentDifferenceNotMerged(helper, player, namedInventoryStack, namedGroundStack, "Test I custom name");

		ItemStack enchantedInventoryStack = new ItemStack(Items.IRON_INGOT, 63);
		enchantedInventoryStack.enchant(Enchantments.SHARPNESS, 1);
		ItemStack enchantedGroundStack = new ItemStack(Items.IRON_INGOT, 64);
		assertComponentDifferenceNotMerged(helper, player, enchantedInventoryStack, enchantedGroundStack, "Test I enchantment");

		ItemStack waterPotions = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
		ItemStack healingPotion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING);
		assertComponentDifferenceNotMerged(helper, player, waterPotions, healingPotion, "Test I potion");

		ItemStack customDataInventoryStack = new ItemStack(Items.IRON_INGOT, 63);
		CompoundTag inventoryData = new CompoundTag();
		inventoryData.putString("test", "inventory");
		customDataInventoryStack.getOrCreateTag().merge(inventoryData);
		ItemStack customDataGroundStack = new ItemStack(Items.IRON_INGOT, 64);
		CompoundTag groundData = new CompoundTag();
		groundData.putString("test", "ground");
		customDataGroundStack.getOrCreateTag().merge(groundData);
		assertComponentDifferenceNotMerged(helper, player, customDataInventoryStack, customDataGroundStack, "Test I custom data");
	}

	private static void testRepeatedRequests(GameTestHelper helper, ServerPlayer player) {
		fillInventory(player.getInventory());
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 60));
		ItemEntity itemEntity = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 64));

		PickupRequestHandler.handle(player, itemEntity.getId());

		for (int request = 0; request < 10; request++) {
			PickupRequestHandler.handle(player, itemEntity.getId());
		}

		assertEquals(helper, player.getInventory().getItem(0).getCount(), 64, "Repeated requests inventory");
		assertEquals(helper, itemEntity.getItem().getCount(), 60, "Repeated requests ground");
		itemEntity.discard();
	}

	private static void testCreativeMode(GameTestHelper helper, ServerPlayer player) {
		player.getAbilities().instabuild = true;
		fillInventory(player.getInventory());
		assertPickup(helper, player, new ItemStack(Items.IRON_INGOT, 64), 0, 64, "Creative full inventory");

		fillInventory(player.getInventory());
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 63));
		assertPickup(helper, player, new ItemStack(Items.IRON_INGOT, 64), 64, 63, "Creative partial capacity");
		player.getAbilities().instabuild = false;
	}

	private static void testVanillaCollisionRemainsBlocked(GameTestHelper helper, ServerPlayer player) {
		player.getInventory().clearContent();
		ItemEntity itemEntity = spawnItem(helper, new ItemStack(Items.IRON_INGOT));

		itemEntity.playerTouch(player);

		assertEquals(helper, countItem(player.getInventory(), Items.IRON_INGOT), 0, "Vanilla collision inventory");
		assertEquals(helper, itemEntity.getItem().getCount(), 1, "Vanilla collision ground");
		itemEntity.discard();
	}

	private static void testServerValidation(GameTestHelper helper, ServerPlayer player) {
		player.getInventory().clearContent();
		ItemEntity itemEntity = spawnItem(helper, new ItemStack(Items.IRON_INGOT));
		itemEntity.setTarget(UUID.randomUUID());
		PickupRequestHandler.handle(player, itemEntity.getId());
		assertEquals(helper, itemEntity.getItem().getCount(), 1, "Target owner validation");

		itemEntity.setTarget(null);
		itemEntity.setPickUpDelay(20);
		PickupRequestHandler.handle(player, itemEntity.getId());
		assertEquals(helper, itemEntity.getItem().getCount(), 1, "Pickup delay validation");

		itemEntity.setPickUpDelay(0);
		player.setPos(itemEntity.getX() + 10.0, itemEntity.getY(), itemEntity.getZ());
		PickupRequestHandler.handle(player, itemEntity.getId());
		assertEquals(helper, itemEntity.getItem().getCount(), 1, "Distance validation");
		player.setPos(helper.absoluteVec(TEST_POSITION));
		itemEntity.discard();
	}

	private static void testPersistenceRoundTrip(GameTestHelper helper, ServerPlayer player) {
		fillInventory(player.getInventory());
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 63));
		ItemEntity itemEntity = spawnItem(helper, new ItemStack(Items.IRON_INGOT, 64));
		PickupRequestHandler.handle(player, itemEntity.getId());

		ListTag savedInventory = player.getInventory().save(new ListTag());
		Inventory loadedInventory = new Inventory(player);
		loadedInventory.load(savedInventory);
		CompoundTag savedEntity = itemEntity.saveWithoutId(new CompoundTag());
		ItemEntity loadedEntity = new ItemEntity(EntityType.ITEM, helper.getLevel());
		loadedEntity.load(savedEntity);

		assertEquals(helper, countItem(loadedInventory, Items.IRON_INGOT), 64, "Persisted inventory");
		assertEquals(helper, loadedEntity.getItem().getCount(), 63, "Persisted ground entity");
		itemEntity.discard();
	}

	private static void assertComponentDifferenceNotMerged(
			GameTestHelper helper,
			ServerPlayer player,
			ItemStack inventoryStack,
			ItemStack groundStack,
			String label
	) {
		fillInventory(player.getInventory());
		player.getInventory().setItem(0, inventoryStack.copy());
		ItemEntity itemEntity = spawnItem(helper, groundStack.copy());
		int beforeTotal = countItem(player.getInventory(), groundStack.getItem()) + groundStack.getCount();

		PickupRequestHandler.handle(player, itemEntity.getId());

		assertEquals(helper, itemEntity.getItem().getCount(), groundStack.getCount(), label + " ground");
		assertEquals(helper, countItem(player.getInventory(), groundStack.getItem()) + itemEntity.getItem().getCount(), beforeTotal, label + " conservation");
		itemEntity.discard();
	}

	private static void assertPickup(
			GameTestHelper helper,
			ServerPlayer player,
			ItemStack groundStack,
			int expectedInventoryCount,
			int expectedGroundCount,
			String label
	) {
		Item item = groundStack.getItem();
		ItemEntity itemEntity = spawnItem(helper, groundStack.copy());
		ItemStack originalGroundReference = itemEntity.getItem();
		int beforeTotal = countItem(player.getInventory(), item) + groundStack.getCount();
		int inventoryBefore = countItem(player.getInventory(), item);
		int statBefore = player.getStats().getValue(Stats.ITEM_PICKED_UP.get(item));

		PickupRequestHandler.handle(player, itemEntity.getId());

		int inventoryAfter = countItem(player.getInventory(), item);
		int actualGroundCount = itemEntity.isRemoved() ? 0 : itemEntity.getItem().getCount();
		assertEquals(helper, inventoryAfter, expectedInventoryCount, label + " inventory");
		assertEquals(helper, actualGroundCount, expectedGroundCount, label + " ground");
		assertEquals(helper, inventoryAfter + actualGroundCount, beforeTotal, label + " conservation");
		assertEquals(helper, player.getStats().getValue(Stats.ITEM_PICKED_UP.get(item)) - statBefore, inventoryAfter - inventoryBefore, label + " statistic");
		assertEquals(helper, originalGroundReference.getCount(), groundStack.getCount(), label + " original ground reference");

		if (expectedGroundCount > 0 && expectedGroundCount < groundStack.getCount()) {
			helper.assertTrue(itemEntity.getItem() != originalGroundReference, label + " must publish a fresh remainder stack");
		}

		if (!itemEntity.isRemoved()) {
			itemEntity.discard();
		}
	}

	private static ItemEntity spawnItem(GameTestHelper helper, ItemStack itemStack) {
		Vec3 position = helper.absoluteVec(TEST_POSITION);
		ItemEntity itemEntity = new ItemEntity(helper.getLevel(), position.x, position.y, position.z, itemStack);
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

	private static int countItem(Inventory inventory, Item item) {
		int count = 0;

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack itemStack = inventory.getItem(slot);

			if (itemStack.is(item)) {
				count += itemStack.getCount();
			}
		}

		return count;
	}

	private static void assertConserved(
			GameTestHelper helper,
			ServerPlayer player,
			ItemEntity itemEntity,
			Item item,
			int beforeTotal,
			String label
	) {
		int groundCount = itemEntity.isRemoved() ? 0 : itemEntity.getItem().getCount();
		assertEquals(helper, countItem(player.getInventory(), item) + groundCount, beforeTotal, label + " conservation");
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String label) {
		helper.assertTrue(actual == expected, label + ": expected " + expected + ", got " + actual);
	}
}
