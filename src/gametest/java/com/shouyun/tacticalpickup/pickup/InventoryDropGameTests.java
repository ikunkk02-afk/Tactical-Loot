package com.shouyun.tacticalpickup.pickup;

import java.util.ArrayList;
import java.util.List;
import com.shouyun.tacticalpickup.TacticalPickup;
import com.shouyun.tacticalpickup.gametest.GameTestPlayers;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(TacticalPickup.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InventoryDropGameTests {
	private static final Vec3 TEST_POSITION = new Vec3(2.0, 2.0, 2.0);

	@GameTest(template = "empty")
	public void dropAFullStackMovesFromInventoryToWorld(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 64));

		int dropped = InventoryDropTransaction.tryDrop(player, 0, 0);
		List<ItemEntity> entities = droppedEntities(player);

		helper.assertTrue(dropped == 64, "Drop A returned count");
		helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "Drop A source slot must be empty");
		helper.assertTrue(groundCount(entities) == 64, "Drop A world count");
		helper.assertTrue(entities.size() == 1 && entities.get(0).hasPickUpDelay(), "Drop A must preserve Vanilla pickup delay");
		cleanup(entities);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void dropBEnchantmentComponentsArePreserved(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(Enchantments.SHARPNESS, 5);
		player.getInventory().setItem(5, sword.copy());

		InventoryDropTransaction.tryDrop(player, 5, 0);
		List<ItemEntity> entities = droppedEntities(player);

		helper.assertTrue(entities.size() == 1, "Drop B must spawn exactly one entity");
		helper.assertTrue(ItemStack.isSameItemSameTags(sword, entities.get(0).getItem()), "Drop B enchantments must match");
		cleanup(entities);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void dropCCustomNameDamageAndCustomDataArePreserved(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.setDamageValue(17);
		sword.setHoverName(Component.literal("field blade"));
		CompoundTag tag = new CompoundTag();
		tag.putString("tactical", "preserved");
		sword.getOrCreateTag().merge(tag);
		player.getInventory().setItem(5, sword.copy());

		InventoryDropTransaction.tryDrop(player, 5, 0);
		List<ItemEntity> entities = droppedEntities(player);

		helper.assertTrue(entities.size() == 1, "Drop C must spawn exactly one entity");
		helper.assertTrue(ItemStack.isSameItemSameTags(sword, entities.get(0).getItem()), "Drop C components must match");
		cleanup(entities);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void dropDInvalidSourceSlotsAreRejected(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 64));

		InventoryDropTransaction.tryDrop(player, -1, 0);
		InventoryDropTransaction.tryDrop(player, 36, 0);
		InventoryDropTransaction.tryDrop(player, 999, 0);

		helper.assertTrue(player.getInventory().getItem(0).getCount() == 64, "Drop D inventory must not change");
		helper.assertTrue(droppedEntities(player).isEmpty(), "Drop D must not spawn items");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void dropEEmptySourceSlotIsRejected(GameTestHelper helper) {
		ServerPlayer player = player(helper);

		int dropped = InventoryDropTransaction.tryDrop(player, 5, 0);

		helper.assertTrue(dropped == 0, "Drop E returned count");
		helper.assertTrue(droppedEntities(player).isEmpty(), "Drop E must not spawn items");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void dropFRepeatedRequestsCannotDuplicate(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(8, new ItemStack(Items.ROTTEN_FLESH, 64));

		InventoryDropTransaction.tryDrop(player, 8, 0);
		for (int repetition = 0; repetition < 10; repetition++) {
			InventoryDropTransaction.tryDrop(player, 8, 0);
		}
		List<ItemEntity> entities = droppedEntities(player);

		helper.assertTrue(player.getInventory().getItem(8).isEmpty(), "Drop F source slot must remain empty");
		helper.assertTrue(groundCount(entities) == 64, "Drop F world total must remain 64");
		cleanup(entities);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void dropGConservationAndSpawnFailureRollback(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.getInventory().setItem(11, new ItemStack(Items.IRON_INGOT, 32));
		int itemStatBefore = player.getStats().getValue(Stats.ITEM_DROPPED.get(Items.IRON_INGOT));
		int dropStatBefore = player.getStats().getValue(Stats.CUSTOM.get(Stats.DROP));

		int failed = InventoryDropTransaction.tryDrop(player, 11, 0, (serverPlayer, stack) -> {
			ItemEntity entity = serverPlayer.drop(stack, false, true);
			if (entity != null) {
				entity.discard();
			}
			return entity;
		});

		helper.assertTrue(failed == 0, "Drop G failed spawn result");
		helper.assertTrue(player.getInventory().getItem(11).getCount() == 32, "Drop G rollback inventory");
		helper.assertTrue(droppedEntities(player).isEmpty(), "Drop G failed spawn world");
		helper.assertTrue(player.getStats().getValue(Stats.ITEM_DROPPED.get(Items.IRON_INGOT)) == itemStatBefore, "Drop G item stat rollback");
		helper.assertTrue(player.getStats().getValue(Stats.CUSTOM.get(Stats.DROP)) == dropStatBefore, "Drop G drop stat rollback");

		InventoryDropTransaction.tryDrop(player, 11, 0);
		List<ItemEntity> entities = droppedEntities(player);
		int afterTotal = player.getInventory().getItem(11).getCount() + groundCount(entities);
		helper.assertTrue(afterTotal == 32, "Drop G conservation: expected 32, got " + afterTotal);
		cleanup(entities);
		helper.succeed();
	}

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = GameTestPlayers.create(helper);
		player.setPos(helper.absoluteVec(TEST_POSITION));
		player.getInventory().clearContent();
		return player;
	}

	private static List<ItemEntity> droppedEntities(ServerPlayer player) {
		List<ItemEntity> entities = new ArrayList<>();
		player.serverLevel().getEntities(
			EntityTypeTest.forClass(ItemEntity.class),
			player.getBoundingBox().inflate(5.0),
			entity -> entity.isAlive() && !entity.isRemoved(),
			entities,
			100
		);
		return entities;
	}

	private static int groundCount(List<ItemEntity> entities) {
		return entities.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static void cleanup(List<ItemEntity> entities) {
		entities.forEach(ItemEntity::discard);
	}
}
