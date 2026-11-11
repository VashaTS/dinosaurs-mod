package net.vsh33.dinosaurs.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.entity.ModEntities;

/**
 * Spawn eggs for every mob plus the dinosaur drops and the items crafted from them,
 * shown in a "Dinosaurs" creative tab (spawn eggs also in the vanilla Spawn Eggs tab).
 */
public final class ModItems {
	public static final List<Item> SPAWN_EGGS = new ArrayList<>();
	/** Drops and crafted items, in creative-tab order. */
	public static final List<Item> ITEMS = new ArrayList<>();

	// Unique drops of the hostile dinosaurs -> Amulet of Dino Protection
	public static final Item T_REX_TOOTH = register("t_rex_tooth", Item::new, new Item.Properties());
	public static final Item RAPTOR_CLAW = register("raptor_claw", Item::new, new Item.Properties());
	public static final Item MEGALODON_TOOTH = register("megalodon_tooth", Item::new, new Item.Properties());
	public static final Item AMULET_OF_DINO_PROTECTION = register("amulet_of_dino_protection", Item::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	// Unique drops of the passive dinosaurs -> Dinosaur Saddle
	public static final Item BRACHIOSAURUS_HIDE = register("brachiosaurus_hide", Item::new, new Item.Properties());
	public static final Item PARASAUROLOPHUS_CREST = register("parasaurolophus_crest", Item::new, new Item.Properties());
	public static final Item DINOSAUR_SADDLE = register("dinosaur_saddle", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

	/** Creative tab icon only (Spinosaurus head); not listed in any tab. */
	public static final Item TAB_ICON = registerHidden("tab_icon");

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Dinosaurs.id("dinosaurs"));

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Dinosaurs.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.add(item);
		return item;
	}

	private static Item registerHidden(String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Dinosaurs.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
	}

	public static void initialize() {
		for (EntityType<? extends Mob> type : ModEntities.ALL) {
			String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + "_spawn_egg";
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Dinosaurs.id(name));
			Item egg = new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(key));
			SPAWN_EGGS.add(Registry.register(BuiltInRegistries.ITEM, key, egg));
		}

		CreativeModeTab tab = FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(TAB_ICON))
				.title(Component.translatable("itemGroup.dinosaurs.dinosaurs"))
				.displayItems((params, output) -> {
					SPAWN_EGGS.forEach(output::accept);
					ITEMS.forEach(output::accept);
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> SPAWN_EGGS.forEach(output::accept));
	}

	private ModItems() {
	}
}
