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
import net.minecraft.util.Mth;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.entity.ModEntities;

/**
 * Mod settings, stored in {@code config/dinosaurs.json}.
 * <ul>
 *     <li>The on/off toggles ({@link #spawning}, {@link #hostileSpawnInDaylight}) are read by the spawn rules every time a
 *     natural spawn is attempted, so they apply immediately.</li>
 *     <li>The spawn weights ({@link #spawnWeights}, {@link #spawnFrequencyPercent}) go into the biomes' spawn lists, which
 *     the game builds when a world is loaded: they apply after leaving and re-joining the world (or restarting the server).</li>
 * </ul>
 */
public final class DinoConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dinosaurs.json");
	private static DinoConfig instance = new DinoConfig();

	/** Range of the per-species weight (vanilla: sheep/cow/pig 8-12, zombies 95, wolves 8, mooshrooms 8). */
	public static final int MAX_WEIGHT = 100;
	/** Range of the global multiplier, in percent. */
	public static final int MAX_FREQUENCY_PERCENT = 500;

	/** When false, hostile dinosaurs only spawn naturally at night and in darkness (light level 7 or less). */
	public boolean hostileSpawnInDaylight = true;
	/** Natural spawning per species (entity id path -> enabled). Spawn eggs, spawners and commands always work. */
	public Map<String, Boolean> spawning = new LinkedHashMap<>();
	/** Global spawn frequency in percent, multiplies every species' weight (100 = default, 0 = no natural dinosaur spawns). */
	public int spawnFrequencyPercent = 100;
	/** Spawn weight per species (entity id path -> weight). Higher = more common in its biomes; 0 = never. */
	public Map<String, Integer> spawnWeights = new LinkedHashMap<>();

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

	/** The species' own weight from the config (its default if missing). */
	public int spawnWeight(EntityType<?> type) {
		Integer value = spawnWeights.get(key(type));
		return value == null ? ModEntities.defaultSpawnWeight(type) : Mth.clamp(value, 0, MAX_WEIGHT);
	}

	public void setSpawnWeight(EntityType<?> type, int weight) {
		spawnWeights.put(key(type), Mth.clamp(weight, 0, MAX_WEIGHT));
	}

	/**
	 * Weight that goes into the biome spawn lists: the species' weight times the global frequency, rounded. A non-zero
	 * result is at least 1, so a low multiplier never silently removes a species. The on/off toggle is not part of it:
	 * it is checked live by the spawn rules, so switching a species back on works without reloading the world.
	 */
	public int effectiveSpawnWeight(EntityType<?> type) {
		int base = spawnWeight(type);
		int percent = Mth.clamp(spawnFrequencyPercent, 0, MAX_FREQUENCY_PERCENT);
		if (base == 0 || percent == 0) {
			return 0;
		}
		return Math.max(1, Math.round(base * percent / 100.0F));
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
		Map<String, Integer> weights = new LinkedHashMap<>();
		for (EntityType<? extends Mob> type : ModEntities.ALL) {
			Boolean value = spawning == null ? null : spawning.get(key(type));
			ordered.put(key(type), value == null || value);
			Integer weight = spawnWeights == null ? null : spawnWeights.get(key(type));
			weights.put(key(type), weight == null ? ModEntities.defaultSpawnWeight(type) : Mth.clamp(weight, 0, MAX_WEIGHT));
		}
		spawning = ordered;
		spawnWeights = weights;
		spawnFrequencyPercent = Mth.clamp(spawnFrequencyPercent, 0, MAX_FREQUENCY_PERCENT);
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
