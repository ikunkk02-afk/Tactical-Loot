package com.shouyun.tacticalpickup.client.pickup;

import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupAggregator;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import com.shouyun.tacticalpickup.pickup.LootGroupMember;
import com.shouyun.tacticalpickup.pickup.PickupConstants;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;

public final class ClientPickupManager {
	private static final ClientPickupManager INSTANCE = new ClientPickupManager();

	private List<LootGroup> groups = List.of();
	private LocalPlayer observedPlayer;
	private ResourceKey<Level> observedDimension;
	private boolean pickupMode;
	private int selectedIndex;
	private int ticksUntilScan;
	private boolean forceScan = true;
	private double accumulatedScroll;

	private ClientPickupManager() {
	}

	public static ClientPickupManager getInstance() {
		return INSTANCE;
	}

	public void tick(Minecraft client) {
		if (client.player == null || client.level == null) {
			reset();
			return;
		}

		if (observedPlayer != client.player || observedDimension != client.level.dimension()) {
			resetForWorld(client.player, client.level.dimension());
		}

		if (!client.player.isAlive()) {
			groups = List.of();
			exitPickupMode();
			return;
		}

		if (forceScan || ticksUntilScan-- <= 0) {
			scan(client);
			ticksUntilScan = PickupConstants.CLIENT_SCAN_INTERVAL_TICKS - 1;
			forceScan = false;
		}
	}

	public boolean shouldCapturePickupKey(Minecraft client) {
		if (!isNormalGameplay(client)) {
			return false;
		}

		return pickupMode || hasValidCachedEntry(client);
	}

	public void handlePickupKey(Minecraft client) {
		if (!isNormalGameplay(client)) {
			return;
		}

		if (!pickupMode) {
			if (hasValidCachedEntry(client)) {
				pickupMode = true;
				accumulatedScroll = 0.0D;
			}

			return;
		}

		LootGroup selected = selectedGroup();

		if (selected == null || !isGroupRepresentativeValid(client, selected)) {
			forceScan = true;
			return;
		}

		if (ClientPlayNetworking.canSend(PickupRequestPayload.TYPE)) {
			ClientPlayNetworking.send(new PickupRequestPayload(selected.representativeEntityId()));
			forceScan = true;
		}
	}

	public boolean handleScroll(Minecraft client, double horizontal, double vertical) {
		if (!pickupMode || !isNormalGameplay(client)) {
			return false;
		}

		boolean discrete = client.options.discreteMouseScroll().get();
		double sensitivity = client.options.mouseWheelSensitivity().get();
		double scaledHorizontal = (discrete ? Math.signum(horizontal) : horizontal) * sensitivity;
		double scaledVertical = (discrete ? Math.signum(vertical) : vertical) * sensitivity;
		double scrollAmount = scaledVertical != 0.0D ? scaledVertical : -scaledHorizontal;

		if (scrollAmount == 0.0D) {
			return true;
		}

		if (accumulatedScroll != 0.0D && Math.signum(scrollAmount) != Math.signum(accumulatedScroll)) {
			accumulatedScroll = 0.0D;
		}

		accumulatedScroll += scrollAmount;
		int steps = (int) accumulatedScroll;

		if (steps != 0) {
			accumulatedScroll -= steps;
			moveSelection(-steps);
		}

		return true;
	}

	public void exitPickupMode() {
		pickupMode = false;
		accumulatedScroll = 0.0D;
	}

	public void reset() {
		groups = List.of();
		observedPlayer = null;
		observedDimension = null;
		selectedIndex = 0;
		ticksUntilScan = 0;
		forceScan = true;
		exitPickupMode();
	}

	public boolean isPickupMode() {
		return pickupMode;
	}

	public List<LootGroup> groups() {
		return groups;
	}

	public int selectedIndex() {
		return selectedIndex;
	}

	private void resetForWorld(LocalPlayer player, ResourceKey<Level> dimension) {
		groups = List.of();
		observedPlayer = player;
		observedDimension = dimension;
		selectedIndex = 0;
		ticksUntilScan = 0;
		forceScan = true;
		exitPickupMode();
	}

	private void scan(Minecraft client) {
		int previousIndex = selectedIndex;
		LootGroupKey previousKey = selectedGroup() == null ? null : selectedGroup().key();
		List<ItemEntity> found = new ArrayList<>(PickupConstants.MAX_SCANNED_ENTITIES);

		client.level.getEntities(
			EntityTypeTest.forClass(ItemEntity.class),
			client.player.getBoundingBox().inflate(PickupConstants.DEFAULT_PICKUP_RADIUS),
			itemEntity -> itemEntity.isAlive()
				&& !itemEntity.isRemoved()
				&& !itemEntity.getItem().isEmpty()
				&& client.player.distanceToSqr(itemEntity) <= PickupConstants.DEFAULT_PICKUP_RADIUS_SQUARED,
			found,
			PickupConstants.MAX_SCANNED_ENTITIES
		);

		List<LootGroupMember> members = found.stream()
			.map(itemEntity -> new LootGroupMember(
				itemEntity.getId(),
				itemEntity.getItem().copy(),
				client.player.distanceToSqr(itemEntity)
			))
			.toList();
		groups = LootGroupAggregator.group(members);

		if (groups.isEmpty()) {
			selectedIndex = 0;
			exitPickupMode();
			return;
		}

		int retainedIndex = indexOfGroup(previousKey);
		selectedIndex = retainedIndex >= 0 ? retainedIndex : Math.min(previousIndex, groups.size() - 1);
	}

	private boolean hasValidCachedEntry(Minecraft client) {
		for (LootGroup group : groups) {
			if (isGroupRepresentativeValid(client, group)) {
				return true;
			}
		}

		return false;
	}

	private boolean isGroupRepresentativeValid(Minecraft client, LootGroup group) {
		if (client.level == null || client.player == null) {
			return false;
		}

		Entity entity = client.level.getEntity(group.representativeEntityId());
		return entity instanceof ItemEntity itemEntity
			&& itemEntity.isAlive()
			&& !itemEntity.isRemoved()
			&& !itemEntity.getItem().isEmpty()
			&& group.key().matches(itemEntity.getItem())
			&& client.player.distanceToSqr(itemEntity) <= PickupConstants.DEFAULT_PICKUP_RADIUS_SQUARED;
	}

	private boolean isNormalGameplay(Minecraft client) {
		return client.player != null
			&& client.level != null
			&& client.player.isAlive()
			&& client.screen == null
			&& client.getOverlay() == null;
	}

	private LootGroup selectedGroup() {
		return selectedIndex >= 0 && selectedIndex < groups.size() ? groups.get(selectedIndex) : null;
	}

	private int indexOfGroup(LootGroupKey key) {
		if (key == null) {
			return -1;
		}

		for (int index = 0; index < groups.size(); index++) {
			if (groups.get(index).key().equals(key)) {
				return index;
			}
		}

		return -1;
	}

	private void moveSelection(int amount) {
		if (!groups.isEmpty()) {
			selectedIndex = Math.floorMod(selectedIndex + amount, groups.size());
		}
	}
}
