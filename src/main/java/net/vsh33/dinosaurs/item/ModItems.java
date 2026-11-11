package net.vsh33.dinosaurs.item;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.entity.ModEntities;

/** Spawn eggs for every mob, shown in a "Dinosaurs" creative tab and in the vanilla Spawn Eggs tab. */
public final class ModItems {
	public static final List<Item> SPAWN_EGGS = new ArrayList<>();

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Dinosaurs.id("dinosaurs"));

	public static void initialize() {
		for (EntityType<? extends Mob> type : ModEntities.ALL) {
			String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + "_spawn_egg";
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Dinosaurs.id(name));
			Item egg = new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key));
			SPAWN_EGGS.add(Registry.register(BuiltInRegistries.ITEM, key, egg));
		}

		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(SPAWN_EGGS.get(0)))
				.title(Component.translatable("itemGroup.dinosaurs.dinosaurs"))
				.displayItems((params, output) -> SPAWN_EGGS.forEach(output::accept))
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> SPAWN_EGGS.forEach(output::accept));
	}

	private ModItems() {
	}
}
