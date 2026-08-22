package com.shouyun.tacticalpickup.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ItemFilterManagerTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void filterADefaultStateIsNormal() {
		ItemFilterManager manager = manager();
		assertEquals(ItemFilterState.NORMAL, manager.getState(id("minecraft", "rotten_flesh")));
	}

	@Test
	void filterBCycleOrderIsStable() {
		assertEquals(ItemFilterState.LOW_PRIORITY, ItemFilterState.NORMAL.next());
		assertEquals(ItemFilterState.HIDDEN, ItemFilterState.LOW_PRIORITY.next());
		assertEquals(ItemFilterState.NORMAL, ItemFilterState.HIDDEN.next());
	}

	@Test
	void filterGConfigurationRoundTripsOnlyNonNormalStates() throws IOException {
		Path configPath = configPath();
		ResourceLocation rottenFlesh = id("minecraft", "rotten_flesh");
		ResourceLocation cobblestone = id("minecraft", "cobblestone");
		ItemFilterManager manager = new ItemFilterManager(configPath);
		manager.setState(rottenFlesh, ItemFilterState.HIDDEN);
		manager.setState(cobblestone, ItemFilterState.LOW_PRIORITY);

		ItemFilterManager reloaded = new ItemFilterManager(configPath);
		assertEquals(ItemFilterState.HIDDEN, reloaded.getState(rottenFlesh));
		assertEquals(ItemFilterState.LOW_PRIORITY, reloaded.getState(cobblestone));
		String json = Files.readString(configPath);
		assertTrue(json.contains("hiddenItems"));
		assertTrue(json.contains("lowPriorityItems"));
		assertFalse(json.contains("NORMAL"));
	}

	@Test
	void filterHAnIdCannotBelongToBothStates() throws IOException {
		Path configPath = configPath();
		ResourceLocation rottenFlesh = id("minecraft", "rotten_flesh");
		ItemFilterManager manager = new ItemFilterManager(configPath);
		manager.setState(rottenFlesh, ItemFilterState.LOW_PRIORITY);
		manager.setState(rottenFlesh, ItemFilterState.HIDDEN);

		assertFalse(manager.getLowPriorityItems().contains(rottenFlesh));
		assertTrue(manager.getHiddenItems().contains(rottenFlesh));

		Files.writeString(configPath, """
			{
			  "lowPriorityItems": ["minecraft:rotten_flesh"],
			  "hiddenItems": ["minecraft:rotten_flesh"]
			}
			""");
		ItemFilterManager normalized = new ItemFilterManager(configPath);
		assertEquals(ItemFilterState.HIDDEN, normalized.getState(rottenFlesh));
		assertFalse(normalized.getLowPriorityItems().contains(rottenFlesh));
	}

	@Test
	void filterIUnknownModIdSurvivesWithoutRegistryLookup() {
		Path configPath = configPath();
		ResourceLocation missingItem = id("missing_namespace", "trash");
		ItemFilterManager manager = new ItemFilterManager(configPath);
		manager.setState(missingItem, ItemFilterState.HIDDEN);

		ItemFilterManager reloaded = new ItemFilterManager(configPath);
		assertEquals(ItemFilterState.HIDDEN, reloaded.getState(missingItem));
		assertTrue(reloaded.getHiddenItems().contains(missingItem));
	}

	private ItemFilterManager manager() {
		return new ItemFilterManager(configPath());
	}

	private Path configPath() {
		return temporaryDirectory.resolve(ItemFilterManager.CONFIG_FILE_NAME);
	}

	private static ResourceLocation id(String namespace, String path) {
		return ResourceLocation.fromNamespaceAndPath(namespace, path);
	}
}
