package net.vsh33.dinosaurs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.entity.ModEntities;

/**
 * Mod settings, stored in {@code config/dinosaurs.json}. Read by the spawn rules every time a natural spawn is attempted,
 * so changes made in the Mod Menu screen apply immediately (no restart needed).
 */
public final class DinoConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dinosaurs.json");
	private static DinoConfig instance = new DinoConfig();

	/** When false, hostile dinosaurs only spawn naturally at night and in darkness (light level 7 or less). */
	public boolean hostileSpawnInDaylight = true;
	/** Natural spawning per species (entity id path -> enabled). Spawn eggs, spawners and commands always work. */
	public Map<String, Boolean> spawning = new LinkedHashMap<>();

	public static DinoConfig get() {
		return instance;
	}

	public static String key(EntityType<?> type) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
	}

	public boolean isSpawnEnabled(EntityType<?> type) {
		return spawning.getOrDefault(key(type), true);
	}

	public void setSpawnEnabled(EntityType<?> type, boolean enabled) {
		spawning.put(key(type), enabled);
	}

	public static void load() {
		DinoConfig loaded = null;
		if (Files.exists(FILE)) {
			try (Reader reader = Files.newBufferedReader(FILE)) {
				loaded = GSON.fromJson(reader, DinoConfig.class);
			} catch (Exception e) {
				Dinosaurs.LOGGER.error("Could not read {}, using defaults", FILE, e);
			}
		}
		instance = loaded != null ? loaded : new DinoConfig();
		instance.fillDefaults();
		save();
	}

	private void fillDefaults() {
		Map<String, Boolean> ordered = new LinkedHashMap<>();
		for (EntityType<? extends Mob> type : ModEntities.ALL) {
			Boolean value = spawning == null ? null : spawning.get(key(type));
			ordered.put(key(type), value == null || value);
		}
		spawning = ordered;
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			Dinosaurs.LOGGER.error("Could not write {}", FILE, e);
		}
	}
}
