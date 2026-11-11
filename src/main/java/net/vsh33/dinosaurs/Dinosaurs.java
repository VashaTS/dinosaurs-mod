package net.vsh33.dinosaurs;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import net.vsh33.dinosaurs.entity.ModEntities;
import net.vsh33.dinosaurs.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dinosaurs implements ModInitializer {
	public static final String MOD_ID = "dinosaurs";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.initialize();
		ModItems.initialize();
		LOGGER.info("Dinosaurs loaded: {} species", ModEntities.ALL.size());
	}
}
