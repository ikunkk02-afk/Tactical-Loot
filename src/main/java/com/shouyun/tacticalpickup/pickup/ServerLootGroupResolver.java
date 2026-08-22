package com.shouyun.tacticalpickup.pickup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import org.jetbrains.annotations.Nullable;

final class ServerLootGroupResolver {
	private ServerLootGroupResolver() {
	}

	@Nullable
	static ResolvedGroup resolve(ServerPlayer player, int entityId) {
		if (player == null || player.isRemoved() || !player.isAlive() || player.isSpectator()) {
			return null;
		}

		Entity entity = player.serverLevel().getEntity(entityId);
		if (!(entity instanceof ItemEntity referenceEntity)
				|| !isEligibleGroupMember(player, referenceEntity, null)
				|| referenceEntity.hasPickUpDelay()) {
			return null;
		}

		LootGroupKey groupKey = LootGroupKey.of(referenceEntity.getItem());
		List<ItemEntity> members = new ArrayList<>(PickupConstants.MAX_SCANNED_ENTITIES);
		player.serverLevel().getEntities(
			EntityTypeTest.forClass(ItemEntity.class),
			player.getBoundingBox().inflate(PickupConstants.DEFAULT_PICKUP_RADIUS),
			itemEntity -> isEligibleGroupMember(player, itemEntity, groupKey),
			members,
			PickupConstants.MAX_SCANNED_ENTITIES
		);
		members.sort(Comparator
			.comparingDouble((ItemEntity itemEntity) -> player.distanceToSqr(itemEntity))
			.thenComparingInt(Entity::getId));
		return new ResolvedGroup(groupKey, List.copyOf(members));
	}

	static boolean isEligibleGroupMember(ServerPlayer player, ItemEntity itemEntity, @Nullable LootGroupKey groupKey) {
		if (itemEntity.isRemoved()
				|| !itemEntity.isAlive()
				|| player.distanceToSqr(itemEntity) > PickupConstants.DEFAULT_PICKUP_RADIUS_SQUARED) {
			return false;
		}

		ItemStack stack = itemEntity.getItem();
		UUID target = itemEntity.target;
		return !stack.isEmpty()
			&& stack.getCount() > 0
			&& (target == null || target.equals(player.getUUID()))
			&& (groupKey == null || groupKey.matches(stack));
	}

	record ResolvedGroup(LootGroupKey key, List<ItemEntity> members) {
	}
}
