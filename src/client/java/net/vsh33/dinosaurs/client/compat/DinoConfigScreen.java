package net.vsh33.dinosaurs.client.compat;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.config.DinoConfig;
import net.vsh33.dinosaurs.entity.ModEntities;

/** Cloth Config screen for {@link DinoConfig}. Only loaded when Cloth Config is present. */
public final class DinoConfigScreen {
	public static Screen create(Screen parent) {
		return create(parent, false);
	}

	/** @param frequencyTab open on the "Spawn frequency" tab instead of "Spawning" (used by the screenshot test). */
	public static Screen create(Screen parent, boolean frequencyTab) {
		DinoConfig config = DinoConfig.get();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.dinosaurs.title"))
				.setSavingRunnable(DinoConfig::save);
		ConfigEntryBuilder entries = builder.entryBuilder();

		// On/off switches: checked live by the spawn rules.
		ConfigCategory spawning = builder.getOrCreateCategory(Component.translatable("config.dinosaurs.category.spawning"));
		spawning.addEntry(entries.startBooleanToggle(Component.translatable("config.dinosaurs.hostile_daylight"), config.hostileSpawnInDaylight)
				.setDefaultValue(true)
				.setTooltip(Component.translatable("config.dinosaurs.hostile_daylight.tooltip"))
				.setSaveConsumer(value -> config.hostileSpawnInDaylight = value)
				.build());
		for (EntityType<? extends Mob> type : ModEntities.ALL) {
			Component name = Component.translatable("config.dinosaurs.spawn", type.getDescription());
			spawning.addEntry(entries.startBooleanToggle(name, config.isSpawnEnabled(type))
					.setDefaultValue(true)
					.setTooltip(Component.translatable("config.dinosaurs.spawn.tooltip"))
					.setSaveConsumer(value -> config.setSpawnEnabled(type, value))
					.build());
		}

		// Spawn frequency: global multiplier and per-species weights, used when the biomes' spawn lists are built (world load).
		ConfigCategory frequency = builder.getOrCreateCategory(Component.translatable("config.dinosaurs.category.frequency"));
		frequency.addEntry(entries.startTextDescription(Component.translatable("config.dinosaurs.restart_note").withStyle(ChatFormatting.GOLD)).build());
		frequency.addEntry(entries.startIntSlider(Component.translatable("config.dinosaurs.frequency"), config.spawnFrequencyPercent, 0, DinoConfig.MAX_FREQUENCY_PERCENT)
				.setDefaultValue(100)
				.setTextGetter(value -> Component.literal(value + "%"))
				.setTooltip(Component.translatable("config.dinosaurs.frequency.tooltip"))
				.setSaveConsumer(value -> config.spawnFrequencyPercent = value)
				.build());
		for (EntityType<? extends Mob> type : ModEntities.ALL) {
			int defaultWeight = ModEntities.defaultSpawnWeight(type);
			frequency.addEntry(entries.startIntSlider(type.getDescription(), config.spawnWeight(type), 0, DinoConfig.MAX_WEIGHT)
					.setDefaultValue(defaultWeight)
					.setTextGetter(value -> Component.translatable("config.dinosaurs.weight", value))
					.setTooltip(Component.translatable("config.dinosaurs.weight.tooltip", defaultWeight))
					.setSaveConsumer(value -> config.setSpawnWeight(type, value))
					.build());
		}
		if (frequencyTab) {
			builder.setFallbackCategory(frequency);
		}
		Screen screen = builder.build();
		// The fallback category only highlights the tab; the listed entries follow selectedCategoryIndex.
		if (frequencyTab && screen instanceof me.shedaniel.clothconfig2.gui.AbstractConfigScreen config2) {
			config2.selectedCategoryIndex = 1;
		}
		return screen;
	}

	private DinoConfigScreen() {
	}
}
