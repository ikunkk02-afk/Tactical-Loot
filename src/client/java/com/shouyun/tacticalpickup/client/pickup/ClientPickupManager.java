package com.shouyun.tacticalpickup.client.pickup;

import com.shouyun.tacticalpickup.filter.ItemFilterManager;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.network.PickupRequestPayload;
import com.shouyun.tacticalpickup.network.PickupToSlotRequestPayload;
import com.shouyun.tacticalpickup.network.DropInventorySlotPayload;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupAggregator;
import com.shouyun.tacticalpickup.pickup.LootGroupKey;
import com.shouyun.tacticalpickup.pickup.LootGroupMember;
import com.shouyun.tacticalpickup.pickup.PickupConstants;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
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
	private final PickupQuantityState quantityState = new PickupQuantityState();
	private double accumulatedScroll;
	private ScrollMode scrollMode = ScrollMode.NONE;
	private ItemFilterManager filterManager;

	private ClientPickupManager() {
	}

	public static ClientPickupManager getInstance() {
		return INSTANCE;
	}

	public void initialize(ItemFilterManager filterManager) {
		this.filterManager = filterManager;
		forceScan = true;
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
				quantityState.reset();
				resetScroll();
			}

			return;
		}

		LootGroup selected = selectedGroup();

		if (selected == null || !isGroupRepresentativeValid(client, selected)) {
			forceScan = true;
			return;
		}

		requestPickup(selected.representativeEntityId(), quantityState.requestedAmount());
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

		ScrollMode nextScrollMode = Screen.hasShiftDown()
			? (Screen.hasControlDown() ? ScrollMode.QUANTITY_FAST : ScrollMode.QUANTITY)
			: ScrollMode.SELECTION;
		if (scrollMode != nextScrollMode) {
			accumulatedScroll = 0.0D;
			scrollMode = nextScrollMode;
		}

		if (accumulatedScroll != 0.0D && Math.signum(scrollAmount) != Math.signum(accumulatedScroll)) {
			accumulatedScroll = 0.0D;
		}

		accumulatedScroll += scrollAmount;
		int steps = (int) accumulatedScroll;

		if (steps != 0) {
			accumulatedScroll -= steps;

			if (scrollMode == ScrollMode.QUANTITY || scrollMode == ScrollMode.QUANTITY_FAST) {
				adjustAmount(steps, scrollMode == ScrollMode.QUANTITY_FAST ? 16 : 1);
			} else {
				moveSelection(-steps);
			}
		}

		return true;
	}

	public void exitPickupMode() {
		pickupMode = false;
		resetScroll();
	}

	public boolean cycleSelectedFilter(Minecraft client) {
		if (!pickupMode || !isNormalGameplay(client) || filterManager == null) {
			return false;
		}

		LootGroup selected = selectedGroup();
		if (selected == null) {
			return false;
		}

		ResourceLocation itemId = LootGroupFilter.itemId(selected);
		ItemFilterState nextState = filterManager.cycleState(itemId);
		client.player.displayClientMessage(
			Component.translatable(
				"tactical_pickup.filter.changed",
				selected.displayStack().getHoverName(),
				Component.translatable(nextState.translationKey())
			),
			true
		);
		scan(client);
		ticksUntilScan = PickupConstants.CLIENT_SCAN_INTERVAL_TICKS - 1;
		forceScan = false;
		return true;
	}

	public void requestScan() {
		forceScan = true;
	}

	public boolean requestPickup(int representativeEntityId, int requestedAmount) {
		if (requestedAmount < PickupRequestPayload.ALL_ITEMS
				|| !ClientPlayNetworking.canSend(PickupRequestPayload.TYPE)) {
			return false;
		}

		ClientPlayNetworking.send(new PickupRequestPayload(representativeEntityId, requestedAmount));
		requestScan();
		return true;
	}

	public boolean requestPickupToSlot(int representativeEntityId, int requestedAmount, int targetSlot) {
		if (requestedAmount < PickupRequestPayload.ALL_ITEMS
				|| targetSlot < 0
				|| targetSlot >= Inventory.INVENTORY_SIZE
				|| !ClientPlayNetworking.canSend(PickupToSlotRequestPayload.TYPE)) {
			return false;
		}

		ClientPlayNetworking.send(new PickupToSlotRequestPayload(
			representativeEntityId,
			requestedAmount,
			targetSlot
		));
		requestScan();
		return true;
	}

	public boolean requestDropInventorySlot(int sourceSlot) {
		if (sourceSlot < 0
				|| sourceSlot >= Inventory.INVENTORY_SIZE
				|| !ClientPlayNetworking.canSend(DropInventorySlotPayload.TYPE)) {
			return false;
		}

		ClientPlayNetworking.send(new DropInventorySlotPayload(sourceSlot, DropInventorySlotPayload.ALL_ITEMS));
		requestScan();
		return true;
	}

	public boolean hasAvailableLoot(Minecraft client) {
		return hasValidCachedEntry(client);
	}

	public LootGroup findGroup(LootGroupKey key) {
		int index = indexOfGroup(key);
		return index >= 0 ? groups.get(index) : null;
	}

	public void reset() {
		groups = List.of();
		observedPlayer = null;
		observedDimension = null;
		selectedIndex = 0;
		ticksUntilScan = 0;
		forceScan = true;
		quantityState.reset();
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

	public LootGroup selectedGroup() {
		return selectedIndex >= 0 && selectedIndex < groups.size() ? groups.get(selectedIndex) : null;
	}

	public ItemFilterManager filterManager() {
		if (filterManager == null) {
			throw new IllegalStateException("Client pickup manager has not been initialized");
		}

		return filterManager;
	}

	public boolean pickupAll() {
		return quantityState.pickupAll();
	}

	public int selectedAmount() {
		LootGroup selected = selectedGroup();
		return selected == null ? 1 : quantityState.selectedAmount(selected.totalCount());
	}

	private void resetForWorld(LocalPlayer player, ResourceKey<Level> dimension) {
		groups = List.of();
		observedPlayer = player;
		observedDimension = dimension;
		selectedIndex = 0;
		ticksUntilScan = 0;
		forceScan = true;
		quantityState.reset();
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
		List<LootGroup> aggregatedGroups = LootGroupAggregator.group(members);
		groups = filterManager == null
			? aggregatedGroups
			: LootGroupFilter.apply(aggregatedGroups, filterManager::getState);

		if (groups.isEmpty()) {
			selectedIndex = 0;
			quantityState.reset();
			exitPickupMode();
			return;
		}

		int retainedIndex = indexOfGroup(previousKey);
		selectedIndex = retainedIndex >= 0 ? retainedIndex : Math.min(previousIndex, groups.size() - 1);

		if (retainedIndex >= 0) {
			quantityState.reconcile(groups.get(selectedIndex).totalCount());
		} else {
			quantityState.reset();
		}
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
			int nextIndex = Math.floorMod(selectedIndex + amount, groups.size());

			if (nextIndex != selectedIndex) {
				selectedIndex = nextIndex;
				quantityState.reset();
			}
		}
	}

	private void adjustAmount(int scrollSteps, int amountPerStep) {
		LootGroup selected = selectedGroup();
		if (selected == null || selected.totalCount() <= 0) {
			return;
		}

		quantityState.adjust(scrollSteps, amountPerStep, selected.totalCount());
	}

	private void resetScroll() {
		accumulatedScroll = 0.0D;
		scrollMode = ScrollMode.NONE;
	}

	private enum ScrollMode {
		NONE,
		SELECTION,
		QUANTITY,
		QUANTITY_FAST
	}
}
