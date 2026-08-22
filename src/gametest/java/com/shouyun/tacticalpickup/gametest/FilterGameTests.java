package com.shouyun.tacticalpickup.gametest;

import com.shouyun.tacticalpickup.filter.ItemFilterState;
import com.shouyun.tacticalpickup.filter.LootGroupFilter;
import com.shouyun.tacticalpickup.pickup.LootGroup;
import com.shouyun.tacticalpickup.pickup.LootGroupAggregator;
import com.shouyun.tacticalpickup.pickup.LootGroupMember;
import java.util.List;
import com.shouyun.tacticalpickup.TacticalPickup;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@GameTestHolder(TacticalPickup.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FilterGameTests {
	@GameTest(template = "empty")
	public void filterCSameItemIdSharesStateAcrossComponents(GameTestHelper helper) {
		ItemStack ordinarySword = new ItemStack(Items.DIAMOND_SWORD);
		ItemStack damagedSword = new ItemStack(Items.DIAMOND_SWORD);
		damagedSword.setDamageValue(50);
		List<LootGroup> groups = LootGroupAggregator.group(List.of(
			new LootGroupMember(1, ordinarySword, 1.0D),
			new LootGroupMember(2, damagedSword, 2.0D)
		));
		ResourceLocation diamondSwordId = LootGroupFilter.itemId(groups.get(0));
		List<LootGroup> visible = LootGroupFilter.apply(groups, itemId ->
			itemId.equals(diamondSwordId) ? ItemFilterState.LOW_PRIORITY : ItemFilterState.NORMAL
		);

		helper.assertTrue(groups.size() == 2, "Different components must remain separate LootGroups");
		helper.assertTrue(visible.size() == 2, "Both LootGroups must inherit the Item ID filter state");
		helper.assertTrue(
			visible.stream().allMatch(group -> LootGroupFilter.itemId(group).equals(diamondSwordId)),
			"Both filtered groups must use minecraft:diamond_sword"
		);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void filterDHiddenGroupsAreExcluded(GameTestHelper helper) {
		List<LootGroup> groups = groupedMembers(
			new LootGroupMember(1, new ItemStack(Items.ROTTEN_FLESH), 1.0D),
			new LootGroupMember(2, new ItemStack(Items.IRON_INGOT), 2.0D)
		);
		ResourceLocation hiddenId = LootGroupFilter.itemId(groups.stream()
			.filter(group -> group.displayStack().is(Items.ROTTEN_FLESH))
			.findFirst()
			.orElseThrow());
		List<LootGroup> visible = LootGroupFilter.apply(groups, itemId ->
			itemId.equals(hiddenId) ? ItemFilterState.HIDDEN : ItemFilterState.NORMAL
		);

		helper.assertTrue(visible.size() == 1, "HIDDEN must not enter visible groups");
		helper.assertTrue(visible.get(0).displayStack().is(Items.IRON_INGOT), "Normal iron must remain visible");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void filterELowPriorityAlwaysFollowsNormal(GameTestHelper helper) {
		List<LootGroup> groups = groupedMembers(
			new LootGroupMember(1, new ItemStack(Items.ROTTEN_FLESH), 1.0D),
			new LootGroupMember(2, new ItemStack(Items.DIAMOND), 16.0D)
		);
		ResourceLocation lowId = groups.stream()
			.filter(group -> group.displayStack().is(Items.ROTTEN_FLESH))
			.map(LootGroupFilter::itemId)
			.findFirst()
			.orElseThrow();
		List<LootGroup> visible = LootGroupFilter.apply(groups, itemId ->
			itemId.equals(lowId) ? ItemFilterState.LOW_PRIORITY : ItemFilterState.NORMAL
		);

		helper.assertTrue(visible.get(0).displayStack().is(Items.DIAMOND), "Distant NORMAL must precede nearby LOW_PRIORITY");
		helper.assertTrue(visible.get(visible.size() - 1).displayStack().is(Items.ROTTEN_FLESH), "LOW_PRIORITY must be after all NORMAL groups");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public void filterFSamePriorityUsesDistanceThenEntityId(GameTestHelper helper) {
		List<LootGroup> groups = groupedMembers(
			new LootGroupMember(30, new ItemStack(Items.COBBLESTONE), 9.0D),
			new LootGroupMember(20, new ItemStack(Items.ROTTEN_FLESH), 1.0D),
			new LootGroupMember(10, new ItemStack(Items.DIRT), 9.0D)
		);
		List<LootGroup> visible = LootGroupFilter.apply(groups, itemId -> ItemFilterState.LOW_PRIORITY);

		helper.assertTrue(visible.get(0).displayStack().is(Items.ROTTEN_FLESH), "Nearest same-priority group must be first");
		helper.assertTrue(visible.get(1).representativeEntityId() == 10, "Entity ID must break equal-distance ties");
		helper.assertTrue(visible.get(2).representativeEntityId() == 30, "Larger entity ID must follow equal-distance peer");
		helper.succeed();
	}

	private static List<LootGroup> groupedMembers(LootGroupMember... members) {
		return LootGroupAggregator.group(List.of(members));
	}
}
