package net.vsh33.dinosaurs.client.compat;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.config.DinoConfig;
import net.vsh33.dinosaurs.entity.ModEntities;

/** Cloth Config screen for {@link DinoConfig}. Only loaded when Cloth Config is present. */
public final class DinoConfigScreen {
	public static Screen create(Screen parent) {
		DinoConfig config = DinoConfig.get();
		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.dinosaurs.title"))
				.setSavingRunnable(DinoConfig::save);
		ConfigEntryBuilder entries = builder.entryBuilder();
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
		return builder.build();
	}

	private DinoConfigScreen() {
	}
}
