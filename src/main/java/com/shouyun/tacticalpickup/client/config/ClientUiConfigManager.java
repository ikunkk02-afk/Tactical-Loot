package com.shouyun.tacticalpickup.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.shouyun.tacticalpickup.TacticalPickup;
import com.shouyun.tacticalpickup.client.ui.layout.UiElement;
import com.shouyun.tacticalpickup.client.ui.layout.UiPlacement;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;

public final class ClientUiConfigManager {
	public static final String CONFIG_FILE_NAME = "tactical-loot-client.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ClientUiConfigManager instance;

	private final Path configPath;
	private final Map<UiElement, UiPlacement> placements = new EnumMap<>(UiElement.class);

	public ClientUiConfigManager(Path configPath) {
		this.configPath = configPath.toAbsolutePath().normalize();
		resetInMemory();
		load();
	}

	public static void initialize(Path configPath) {
		instance = new ClientUiConfigManager(configPath);
	}

	public static ClientUiConfigManager getInstance() {
		if (instance == null) {
			throw new IllegalStateException("Tactical Loot client UI config has not been initialized");
		}
		return instance;
	}

	public UiPlacement placement(UiElement element) {
		return placements.get(element);
	}

	public void update(UiElement element, double normalizedX, double normalizedY, float scale) {
		placements.put(element, sanitize(
			element,
			new UiPlacement(normalizedX, normalizedY, scale, true)
		));
	}

	public void reset(UiElement element) {
		placements.put(element, UiPlacement.defaults());
	}

	public void resetAll() {
		resetInMemory();
	}

	public Path configPath() {
		return configPath;
	}

	public void save() {
		Path temporaryPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
		try {
			Path parent = configPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}

			ConfigData data = new ConfigData(
				ElementData.from(placement(UiElement.LOOT_HUD)),
				ElementData.from(placement(UiElement.LOOT_SCREEN))
			);
			try (Writer writer = Files.newBufferedWriter(temporaryPath, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}

			try {
				Files.move(temporaryPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporaryPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) {
			TacticalPickup.LOGGER.error("Could not save Tactical Loot client UI settings to {}", configPath, exception);
		} finally {
			try {
				Files.deleteIfExists(temporaryPath);
			} catch (IOException exception) {
				TacticalPickup.LOGGER.debug("Could not clean up temporary Tactical Loot UI config {}", temporaryPath, exception);
			}
		}
	}

	private void load() {
		if (!Files.exists(configPath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			ConfigData data = GSON.fromJson(reader, ConfigData.class);
			if (data == null) {
				throw new JsonParseException("Expected a Tactical Loot client UI configuration object");
			}
			placements.put(UiElement.LOOT_HUD, readPlacement(UiElement.LOOT_HUD, data.lootHud));
			placements.put(UiElement.LOOT_SCREEN, readPlacement(UiElement.LOOT_SCREEN, data.lootScreen));
		} catch (IOException | RuntimeException exception) {
			resetInMemory();
			TacticalPickup.LOGGER.error(
				"Could not load Tactical Loot client UI settings from {}. Using defaults.",
				configPath,
				exception
			);
		}
	}

	private static UiPlacement readPlacement(UiElement element, ElementData data) {
		if (data == null) {
			return UiPlacement.defaults();
		}

		boolean validPosition = data.normalizedX != null
			&& data.normalizedY != null
			&& Double.isFinite(data.normalizedX)
			&& Double.isFinite(data.normalizedY);
		double normalizedX = validPosition ? data.normalizedX : 0.5D;
		double normalizedY = validPosition ? data.normalizedY : 0.5D;
		float scale = data.scale != null && Float.isFinite(data.scale) ? data.scale : 1.0F;
		boolean customized = Boolean.TRUE.equals(data.customized) && validPosition;
		return sanitize(element, new UiPlacement(normalizedX, normalizedY, scale, customized));
	}

	private static UiPlacement sanitize(UiElement element, UiPlacement placement) {
		double normalizedX = Double.isFinite(placement.normalizedX())
			? Math.max(0.0D, Math.min(placement.normalizedX(), 1.0D))
			: 0.5D;
		double normalizedY = Double.isFinite(placement.normalizedY())
			? Math.max(0.0D, Math.min(placement.normalizedY(), 1.0D))
			: 0.5D;
		return new UiPlacement(normalizedX, normalizedY, element.clampScale(placement.scale()), placement.customized());
	}

	private void resetInMemory() {
		for (UiElement element : UiElement.values()) {
			placements.put(element, UiPlacement.defaults());
		}
	}

	private record ConfigData(ElementData lootHud, ElementData lootScreen) {
	}

	private record ElementData(Double normalizedX, Double normalizedY, Float scale, Boolean customized) {
		private static ElementData from(UiPlacement placement) {
			return new ElementData(
				placement.normalizedX(),
				placement.normalizedY(),
				placement.scale(),
				placement.customized()
			);
		}
	}
}
