package com.shouyun.tacticalpickup.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.shouyun.tacticalpickup.client.ui.layout.UiElement;
import com.shouyun.tacticalpickup.client.ui.layout.UiPlacement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientUiConfigManagerTest {
	@TempDir
	Path temporaryDirectory;

	@Test
	void missingConfigUsesCurrentLayoutDefaults() {
		ClientUiConfigManager manager = new ClientUiConfigManager(configPath());

		for (UiElement element : UiElement.values()) {
			UiPlacement placement = manager.placement(element);
			assertFalse(placement.customized());
			assertEquals(1.0F, placement.scale());
		}
	}

	@Test
	void independentPlacementsSurviveSaveAndReload() {
		Path path = configPath();
		ClientUiConfigManager manager = new ClientUiConfigManager(path);
		manager.update(UiElement.LOOT_HUD, 0.15D, 0.25D, 0.70F);
		manager.update(UiElement.LOOT_SCREEN, 0.75D, 0.80D, 1.30F);
		manager.save();

		ClientUiConfigManager reloaded = new ClientUiConfigManager(path);
		assertPlacement(reloaded.placement(UiElement.LOOT_HUD), 0.15D, 0.25D, 0.70F);
		assertPlacement(reloaded.placement(UiElement.LOOT_SCREEN), 0.75D, 0.80D, 1.30F);
	}

	@Test
	void damagedJsonFallsBackToSafeDefaults() throws IOException {
		Path path = configPath();
		Files.writeString(path, "{ this is not valid json");

		ClientUiConfigManager manager = new ClientUiConfigManager(path);

		assertFalse(manager.placement(UiElement.LOOT_HUD).customized());
		assertFalse(manager.placement(UiElement.LOOT_SCREEN).customized());
		assertEquals(1.0F, manager.placement(UiElement.LOOT_HUD).scale());
	}

	@Test
	void outOfRangeValuesAreClampedAndMissingSectionsUseDefaults() throws IOException {
		Path path = configPath();
		Files.writeString(path, """
			{
			  "lootHud": {
			    "normalizedX": 3.5,
			    "normalizedY": -2.0,
			    "scale": 99.0,
			    "customized": true
			  }
			}
			""");

		ClientUiConfigManager manager = new ClientUiConfigManager(path);
		UiPlacement hud = manager.placement(UiElement.LOOT_HUD);

		assertEquals(1.0D, hud.normalizedX());
		assertEquals(0.0D, hud.normalizedY());
		assertEquals(UiElement.LOOT_HUD.maximumScale(), hud.scale());
		assertTrue(hud.customized());
		assertFalse(manager.placement(UiElement.LOOT_SCREEN).customized());
	}

	@Test
	void resetCanTargetOneElementOrEveryElement() {
		ClientUiConfigManager manager = new ClientUiConfigManager(configPath());
		manager.update(UiElement.LOOT_HUD, 0.2D, 0.3D, 0.8F);
		manager.update(UiElement.LOOT_SCREEN, 0.7D, 0.6D, 1.2F);

		manager.reset(UiElement.LOOT_HUD);
		assertFalse(manager.placement(UiElement.LOOT_HUD).customized());
		assertTrue(manager.placement(UiElement.LOOT_SCREEN).customized());

		manager.resetAll();
		assertFalse(manager.placement(UiElement.LOOT_HUD).customized());
		assertFalse(manager.placement(UiElement.LOOT_SCREEN).customized());
	}

	private Path configPath() {
		return temporaryDirectory.resolve(ClientUiConfigManager.CONFIG_FILE_NAME);
	}

	private static void assertPlacement(UiPlacement placement, double x, double y, float scale) {
		assertTrue(placement.customized());
		assertEquals(x, placement.normalizedX(), 0.0001D);
		assertEquals(y, placement.normalizedY(), 0.0001D);
		assertEquals(scale, placement.scale(), 0.0001F);
	}
}
