package com.shouyun.tacticalpickup.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.shouyun.tacticalpickup.TacticalPickup;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public final class ItemFilterManager {
	public static final String CONFIG_FILE_NAME = "tactical_pickup_filters.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configPath;
	private final Set<ResourceLocation> lowPriorityItems = new LinkedHashSet<>();
	private final Set<ResourceLocation> hiddenItems = new LinkedHashSet<>();

	public ItemFilterManager(Path configPath) {
		this.configPath = configPath.toAbsolutePath().normalize();
		load();
	}

	public ItemFilterState getState(Item item) {
		return getState(BuiltInRegistries.ITEM.getKey(item));
	}

	public ItemFilterState getState(ResourceLocation itemId) {
		if (hiddenItems.contains(itemId)) {
			return ItemFilterState.HIDDEN;
		}

		return lowPriorityItems.contains(itemId) ? ItemFilterState.LOW_PRIORITY : ItemFilterState.NORMAL;
	}

	public boolean setState(ResourceLocation itemId, ItemFilterState state) {
		if (itemId == null || state == null || getState(itemId) == state) {
			return false;
		}

		lowPriorityItems.remove(itemId);
		hiddenItems.remove(itemId);

		switch (state) {
			case LOW_PRIORITY -> lowPriorityItems.add(itemId);
			case HIDDEN -> hiddenItems.add(itemId);
			case NORMAL -> {
			}
		}

		save();
		return true;
	}

	public ItemFilterState cycleState(ResourceLocation itemId) {
		ItemFilterState next = getState(itemId).next();
		setState(itemId, next);
		return next;
	}

	public Set<ResourceLocation> getLowPriorityItems() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(lowPriorityItems));
	}

	public Set<ResourceLocation> getHiddenItems() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(hiddenItems));
	}

	public Set<ResourceLocation> getConfiguredItems() {
		Set<ResourceLocation> configured = new LinkedHashSet<>(lowPriorityItems);
		configured.addAll(hiddenItems);
		return Collections.unmodifiableSet(configured);
	}

	public boolean resetAll() {
		if (lowPriorityItems.isEmpty() && hiddenItems.isEmpty()) {
			return false;
		}

		lowPriorityItems.clear();
		hiddenItems.clear();
		save();
		return true;
	}

	public Path configPath() {
		return configPath;
	}

	private void load() {
		lowPriorityItems.clear();
		hiddenItems.clear();

		if (!Files.exists(configPath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			ItemFilterConfig config = GSON.fromJson(reader, ItemFilterConfig.class);
			if (config == null || config.lowPriorityItems == null || config.hiddenItems == null) {
				throw new JsonParseException("Expected lowPriorityItems and hiddenItems arrays");
			}

			boolean normalized = addValidIds(config.lowPriorityItems, lowPriorityItems, "lowPriorityItems");
			normalized |= addValidIds(config.hiddenItems, hiddenItems, "hiddenItems");
			if (lowPriorityItems.removeAll(hiddenItems)) {
				normalized = true;
				TacticalPickup.LOGGER.warn("Duplicate Tactical Loot filter entries were resolved in favor of HIDDEN");
			}

			if (normalized) {
				save();
			}
		} catch (IOException | RuntimeException exception) {
			lowPriorityItems.clear();
			hiddenItems.clear();
			TacticalPickup.LOGGER.error(
				"Could not load Tactical Loot filters from {}. Using an empty filter configuration.",
				configPath,
				exception
			);
		}
	}

	private boolean addValidIds(Collection<String> values, Set<ResourceLocation> destination, String fieldName) {
		boolean normalized = false;

		for (String value : values) {
			ResourceLocation itemId = value == null ? null : ResourceLocation.tryParse(value);
			if (itemId == null) {
				normalized = true;
				TacticalPickup.LOGGER.warn("Ignoring invalid item ID '{}' in Tactical Loot filter field {}", value, fieldName);
				continue;
			}

			if (!destination.add(itemId)) {
				normalized = true;
			}
		}

		return normalized;
	}

	private void save() {
		Path temporaryPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");

		try {
			Path parent = configPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			ItemFilterConfig config = new ItemFilterConfig(toSortedStrings(lowPriorityItems), toSortedStrings(hiddenItems));
			try (Writer writer = Files.newBufferedWriter(temporaryPath, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}

			try {
				Files.move(temporaryPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			TacticalPickup.LOGGER.error("Could not save Tactical Loot filters to {}", configPath, exception);
		} finally {
			try {
				Files.deleteIfExists(temporaryPath);
			} catch (IOException exception) {
				TacticalPickup.LOGGER.debug("Could not clean up temporary Tactical Loot filter file {}", temporaryPath, exception);
			}
		}
	}

	private static Set<String> toSortedStrings(Set<ResourceLocation> itemIds) {
		Set<String> sorted = new TreeSet<>();
		for (ResourceLocation itemId : itemIds) {
			sorted.add(itemId.toString());
		}
		return sorted;
	}
}
