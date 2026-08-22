package com.shouyun.tacticalpickup.client.filter;

import com.shouyun.tacticalpickup.client.pickup.ClientPickupManager;
import com.shouyun.tacticalpickup.filter.ItemFilterManager;
import com.shouyun.tacticalpickup.filter.ItemFilterState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FilterManagementScreen extends Screen {
	private static final int CONTENT_WIDTH = 560;
	private static final int ROW_HEIGHT = 42;
	private static final int MAX_NORMAL_SEARCH_RESULTS = 60;
	private static final int PANEL_COLOR = 0xD0121317;
	private static final int PANEL_INSET_COLOR = 0x701C1E23;
	private static final int ACCENT_COLOR = 0xFFBE8B55;
	private static final int TEXT_COLOR = 0xFFF3F3F3;
	private static final int MUTED_TEXT_COLOR = 0xFF9B9DA3;
	private static final int NORMAL_COLOR = 0xFFB8BAC0;
	private static final int LOW_PRIORITY_COLOR = 0xFFD0A66A;
	private static final int HIDDEN_COLOR = 0xFFC87373;

	private final Screen parent;
	private EditBox searchBox;
	private FilterList filterList;
	private Button resetAllButton;
	private int contentLeft;
	private int contentWidth;

	public FilterManagementScreen(Screen parent) {
		super(Component.translatable("tactical_pickup.filter.screen.title"));
		this.parent = parent;
		ClientPickupManager.getInstance().exitPickupMode();
	}

	@Override
	protected void init() {
		String previousQuery = searchBox == null ? "" : searchBox.getValue();
		contentWidth = Math.min(CONTENT_WIDTH, width - 28);
		contentLeft = (width - contentWidth) / 2;

		searchBox = new EditBox(
			font,
			contentLeft + 8,
			43,
			contentWidth - 16,
			20,
			Component.translatable("tactical_pickup.filter.screen.search")
		);
		searchBox.setHint(Component.translatable("tactical_pickup.filter.screen.search"));
		searchBox.setMaxLength(128);
		searchBox.setValue(previousQuery);
		addRenderableWidget(searchBox);

		filterList = new FilterList(minecraft, contentWidth - 12, Math.max(40, height - 118), 70, ROW_HEIGHT);
		filterList.setLeftPos(contentLeft + 6);
		filterList.refresh(previousQuery);
		addRenderableWidget(filterList);
		searchBox.setResponder(filterList::refresh);
		setInitialFocus(searchBox);

		int footerY = height - 30;
		resetAllButton = Button.builder(Component.translatable("tactical_pickup.filter.screen.reset_all"), button -> confirmReset())
			.bounds(width / 2 - 155, footerY, 150, 20)
			.build();
		addRenderableWidget(resetAllButton);
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(width / 2 + 5, footerY, 150, 20)
			.build());
		updateResetButton();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderBackground(graphics);
		int panelRight = contentLeft + contentWidth;
		graphics.fill(contentLeft - 4, 7, panelRight + 4, height - 37, PANEL_COLOR);
		graphics.fill(contentLeft - 4, 7, panelRight + 4, 9, ACCENT_COLOR);
		graphics.fill(contentLeft + 4, 40, panelRight - 4, 66, PANEL_INSET_COLOR);
		super.render(graphics, mouseX, mouseY, partialTick);

		graphics.drawString(font, title, contentLeft + 8, 14, TEXT_COLOR, true);
		graphics.drawString(
			font,
			Component.translatable("tactical_pickup.filter.screen.summary", filters().getConfiguredItems().size()),
			contentLeft + 8,
			28,
			MUTED_TEXT_COLOR,
			false
		);

		if (filterList != null && filterList.isEmpty()) {
			Component emptyText = filterList.isSearchActive()
				? Component.translatable("tactical_pickup.filter.screen.no_results")
				: Component.translatable("tactical_pickup.filter.screen.empty");
			graphics.drawCenteredString(font, emptyText, width / 2, height / 2, MUTED_TEXT_COLOR);
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private ItemFilterManager filters() {
		return ClientPickupManager.getInstance().filterManager();
	}

	private void setFilterState(ResourceLocation itemId, ItemFilterState state) {
		if (filters().setState(itemId, state)) {
			ClientPickupManager.getInstance().requestScan();
		}
		filterList.refresh(searchBox.getValue());
		updateResetButton();
	}

	private void updateResetButton() {
		if (resetAllButton != null) {
			resetAllButton.active = !filters().getConfiguredItems().isEmpty();
		}
	}

	private void confirmReset() {
		minecraft.setScreen(new ConfirmScreen(
			confirmed -> {
				if (confirmed && filters().resetAll()) {
					ClientPickupManager.getInstance().requestScan();
				}
				minecraft.setScreen(this);
			},
			Component.translatable("tactical_pickup.filter.screen.confirm_reset.title"),
			Component.translatable("tactical_pickup.filter.screen.confirm_reset")
		));
	}

	private final class FilterList extends ContainerObjectSelectionList<FilterEntry> {
		private boolean searchActive;

		private FilterList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, y + height, y, y + height, itemHeight);
		}

		private void refresh(String query) {
			clearEntries();
			String normalizedQuery = normalize(query);
			searchActive = !normalizedQuery.isEmpty();

			if (searchActive) {
				SearchResults normalResults = normalSearchResults(normalizedQuery);
				addSection(ItemFilterState.NORMAL, normalResults.items());
				addSection(ItemFilterState.LOW_PRIORITY, configuredItems(ItemFilterState.LOW_PRIORITY, normalizedQuery));
				addSection(ItemFilterState.HIDDEN, configuredItems(ItemFilterState.HIDDEN, normalizedQuery));
				if (normalResults.omittedCount() > 0) {
					addEntry(new InfoEntry(Component.translatable(
						"tactical_pickup.filter.screen.more_results",
						normalResults.omittedCount()
					)));
				}
			} else {
				addSection(ItemFilterState.LOW_PRIORITY, configuredItems(ItemFilterState.LOW_PRIORITY, ""));
				addSection(ItemFilterState.HIDDEN, configuredItems(ItemFilterState.HIDDEN, ""));
			}

			setScrollAmount(0.0D);
		}

		private void addSection(ItemFilterState state, List<FilterItem> items) {
			if (items.isEmpty()) {
				return;
			}

			addEntry(new SectionEntry(Component.translatable(state.translationKey()), items.size(), stateColor(state)));
			for (FilterItem item : items) {
				addEntry(new ItemEntry(item));
			}
		}

		private List<FilterItem> configuredItems(ItemFilterState state, String query) {
			List<FilterItem> items = new ArrayList<>();
			for (ResourceLocation itemId : filters().getConfiguredItems()) {
				if (filters().getState(itemId) != state) {
					continue;
				}

				FilterItem item = createItem(itemId, state);
				if (query.isEmpty() || matches(item, query)) {
					items.add(item);
				}
			}

			items.sort(itemOrder());
			return items;
		}

		private SearchResults normalSearchResults(String query) {
			List<FilterItem> matches = new ArrayList<>();
			Set<ResourceLocation> configured = filters().getConfiguredItems();

			for (Item item : BuiltInRegistries.ITEM) {
				ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
				if (configured.contains(itemId)) {
					continue;
				}

				ItemStack stack = new ItemStack(item);
				FilterItem filterItem = new FilterItem(itemId, stack, stack.getHoverName().getString(), ItemFilterState.NORMAL);
				if (!filterItem.stack().isEmpty() && matches(filterItem, query)) {
					matches.add(filterItem);
				}
			}

			matches.sort(Comparator
				.comparingInt((FilterItem item) -> matchRank(item, query))
				.thenComparing(itemOrder()));
			int omittedCount = Math.max(0, matches.size() - MAX_NORMAL_SEARCH_RESULTS);
			return new SearchResults(List.copyOf(matches.subList(0, Math.min(matches.size(), MAX_NORMAL_SEARCH_RESULTS))), omittedCount);
		}

		private boolean isEmpty() {
			return getItemCount() == 0;
		}

		private boolean isSearchActive() {
			return searchActive;
		}

		@Override
		public int getRowWidth() {
			return Math.min(528, getWidth() - 14);
		}
	}

	private abstract static class FilterEntry extends ContainerObjectSelectionList.Entry<FilterEntry> {
	}

	private final class SectionEntry extends FilterEntry {
		private final Component label;
		private final int count;
		private final int color;

		private SectionEntry(Component label, int count, int color) {
			this.label = label;
			this.count = count;
			this.color = color;
		}

		@Override
		public void render(
				GuiGraphics graphics,
				int index,
				int top,
				int left,
				int width,
				int height,
				int mouseX,
				int mouseY,
				boolean hovered,
				float partialTick
		) {
			int lineY = top + height / 2;
			graphics.fill(left + 4, lineY, left + width - 4, lineY + 1, 0xFF34363C);
			graphics.fill(left + 4, top + 9, left + 7, top + height - 9, color);
			graphics.fill(left + 11, top + 8, left + 18 + font.width(label), top + 22, PANEL_COLOR);
			graphics.drawString(font, label, left + 14, top + 11, color, true);
			String countText = Integer.toString(count);
			int countX = left + width - font.width(countText) - 12;
			graphics.fill(countX - 4, top + 8, left + width - 4, top + 22, PANEL_COLOR);
			graphics.drawString(font, countText, countX, top + 11, MUTED_TEXT_COLOR, false);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}
	}

	private final class InfoEntry extends FilterEntry {
		private final Component message;

		private InfoEntry(Component message) {
			this.message = message;
		}

		@Override
		public void render(
				GuiGraphics graphics,
				int index,
				int top,
				int left,
				int width,
				int height,
				int mouseX,
				int mouseY,
				boolean hovered,
				float partialTick
		) {
			graphics.drawCenteredString(font, message, left + width / 2, top + 14, MUTED_TEXT_COLOR);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}
	}

	private final class ItemEntry extends FilterEntry {
		private final FilterItem item;
		private final Button primaryButton;
		private final Button secondaryButton;

		private ItemEntry(FilterItem item) {
			this.item = item;
			ItemFilterState primaryState = primaryTarget(item.state());
			ItemFilterState secondaryState = secondaryTarget(item.state());
			primaryButton = stateButton(item.itemId(), item.state(), primaryState);
			secondaryButton = stateButton(item.itemId(), item.state(), secondaryState);
		}

		private Button stateButton(ResourceLocation itemId, ItemFilterState currentState, ItemFilterState targetState) {
			return Button.builder(
				Component.translatable(actionTranslationKey(currentState, targetState)),
				button -> setFilterState(itemId, targetState)
			)
				.size(96, 20)
				.build();
		}

		@Override
		public void render(
				GuiGraphics graphics,
				int index,
				int top,
				int left,
				int width,
				int height,
				int mouseX,
				int mouseY,
				boolean hovered,
				float partialTick
		) {
			int rowColor = hovered ? 0xB02A2C32 : 0x80202227;
			graphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, rowColor);
			graphics.fill(left + 2, top + 2, left + 5, top + height - 2, stateColor(item.state()));
			graphics.fill(left + 9, top + 8, left + 31, top + 32, 0xA0101114);

			if (!item.stack().isEmpty()) {
				graphics.renderItem(item.stack(), left + 12, top + 11);
			} else {
				graphics.drawCenteredString(font, "?", left + 20, top + 15, HIDDEN_COLOR);
			}

			int buttonY = top + 11;
			secondaryButton.setX(left + width - secondaryButton.getWidth() - 8);
			secondaryButton.setY(buttonY);
			primaryButton.setX(secondaryButton.getX() - primaryButton.getWidth() - 5);
			primaryButton.setY(buttonY);
			int textRight = primaryButton.getX() - 8;
			int availableTextWidth = Math.max(24, textRight - left - 39);
			String clippedName = font.plainSubstrByWidth(item.localizedName(), availableTextWidth);
			String clippedId = font.plainSubstrByWidth(item.itemId().toString(), availableTextWidth);
			graphics.drawString(font, clippedName, left + 38, top + 8, TEXT_COLOR, true);
			graphics.drawString(font, clippedId, left + 38, top + 23, MUTED_TEXT_COLOR, false);
			primaryButton.render(graphics, mouseX, mouseY, partialTick);
			secondaryButton.render(graphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(primaryButton, secondaryButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(primaryButton, secondaryButton);
		}
	}

	private FilterItem createItem(ResourceLocation itemId, ItemFilterState state) {
		ItemStack stack = BuiltInRegistries.ITEM.getOptional(itemId)
			.map(ItemStack::new)
			.orElse(ItemStack.EMPTY);
		String localizedName = stack.isEmpty()
			? Component.translatable("tactical_pickup.filter.screen.missing_item").getString()
			: stack.getHoverName().getString();
		return new FilterItem(itemId, stack, localizedName, state);
	}

	private static boolean matches(FilterItem item, String query) {
		String searchable = normalize(item.localizedName() + " " + item.itemId() + " " + item.itemId().getPath().replace('_', ' '));
		for (String token : query.split("\\s+")) {
			if (!searchable.contains(token)) {
				return false;
			}
		}
		return true;
	}

	private static int matchRank(FilterItem item, String query) {
		String name = normalize(item.localizedName());
		String itemId = item.itemId().toString();
		String path = item.itemId().getPath();
		if (name.equals(query) || itemId.equals(query) || path.equals(query)) {
			return 0;
		}
		if (name.startsWith(query) || path.startsWith(query)) {
			return 1;
		}
		return 2;
	}

	private static Comparator<FilterItem> itemOrder() {
		return Comparator
			.comparing(FilterItem::localizedName, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(item -> item.itemId().toString());
	}

	private static String normalize(String value) {
		return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
	}

	private static ItemFilterState primaryTarget(ItemFilterState current) {
		return switch (current) {
			case NORMAL -> ItemFilterState.LOW_PRIORITY;
			case LOW_PRIORITY -> ItemFilterState.HIDDEN;
			case HIDDEN -> ItemFilterState.LOW_PRIORITY;
		};
	}

	private static ItemFilterState secondaryTarget(ItemFilterState current) {
		return current == ItemFilterState.NORMAL ? ItemFilterState.HIDDEN : ItemFilterState.NORMAL;
	}

	private static String actionTranslationKey(ItemFilterState current, ItemFilterState target) {
		return switch (target) {
			case NORMAL -> "tactical_pickup.filter.action.restore_normal";
			case HIDDEN -> "tactical_pickup.filter.action.hide_item";
			case LOW_PRIORITY -> current == ItemFilterState.NORMAL
				? "tactical_pickup.filter.action.lower_priority"
				: "tactical_pickup.filter.action.set_low_priority";
		};
	}

	private static int stateColor(ItemFilterState state) {
		return switch (state) {
			case NORMAL -> NORMAL_COLOR;
			case LOW_PRIORITY -> LOW_PRIORITY_COLOR;
			case HIDDEN -> HIDDEN_COLOR;
		};
	}

	private record FilterItem(
		ResourceLocation itemId,
		ItemStack stack,
		String localizedName,
		ItemFilterState state
	) {
	}

	private record SearchResults(List<FilterItem> items, int omittedCount) {
	}
}
