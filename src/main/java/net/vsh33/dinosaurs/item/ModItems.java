package net.vsh33.dinosaurs.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
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

	// Rare T. rex drop (killed by a player: 4%, up to 10% with Looting) -> T-Rex Skull Helmet
	public static final Item T_REX_SKULL = register("t_rex_skull", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
	/** Netherite ingot or another skull repair the helmet (#dinosaurs:repairs_t_rex_skull_helmet). */
	public static final TagKey<Item> REPAIRS_T_REX_SKULL_HELMET = TagKey.create(Registries.ITEM, Dinosaurs.id("repairs_t_rex_skull_helmet"));
	/**
	 * Helmet stats, above netherite (3 armor, 3 toughness, 0.1 knockback resistance, 37 x 11 = 407 durability,
	 * enchantability 15) and at least the Dinosaur Armor tier it completes (1.5.0: toughness 4, knockback resistance 0.2,
	 * enchantability 20 like the armor; it keeps its 4 armor and 46 x 11 = 506 durability). Only the helmet exists;
	 * the asset id is never rendered.
	 */
	public static final ArmorMaterial T_REX_SKULL_MATERIAL = new ArmorMaterial(46, Map.of(ArmorType.HELMET, 4), 20,
			SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.2F, REPAIRS_T_REX_SKULL_HELMET,
			ResourceKey.create(EquipmentAssets.ROOT_ID, Dinosaurs.id("t_rex_skull")));
	/**
	 * Worn helmet. Its Equippable has no equipment asset, so instead of a flat armor layer the game draws the
	 * 3D item model on the head (CustomHeadLayer, like a carved pumpkin): players, armor stands, zombies...
	 */
	public static final Item T_REX_SKULL_HELMET = register("t_rex_skull_helmet", Item::new, new Item.Properties()
			.durability(ArmorType.HELMET.getDurability(T_REX_SKULL_MATERIAL.durability()))
			.attributes(T_REX_SKULL_MATERIAL.createAttributes(ArmorType.HELMET))
			.enchantable(T_REX_SKULL_MATERIAL.enchantmentValue())
			.component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setEquipSound(T_REX_SKULL_MATERIAL.equipSound()).build())
			.repairable(REPAIRS_T_REX_SKULL_HELMET)
			.fireResistant()
			.rarity(Rarity.EPIC));

	// Large dinosaurs drop Dinosaur Bones instead of vanilla bones -> Dinosaur Bone Bow (also 1 -> 4 bone meal)
	public static final Item DINOSAUR_BONES = register("dinosaur_bones", Item::new, new Item.Properties());
	/** Faster draw, 1.5x arrow speed and damage, 3x durability; Infinity + Mending allowed together (see BoneBowEnchantments). */
	public static final Item DINOSAUR_BONE_BOW = register("dinosaur_bone_bow", DinosaurBoneBowItem::new, new Item.Properties()
			.durability(DinosaurBoneBowItem.DURABILITY)
			.enchantable(DinosaurBoneBowItem.ENCHANTABILITY)
			.repairable(DINOSAUR_BONES)
			.fireResistant()
			.rarity(Rarity.EPIC));

	// Dinosaur Claw (1.6.0): a sword one step above netherite. Raptor Claw, T-Rex Tooth, Raptor Claw / Netherite Ingot,
	// Dinosaur Bones, Netherite Ingot / stick.
	/** Repair ingredient of the Dinosaur Claw (#dinosaurs:repairs_dinosaur_claw: Dinosaur Bones). */
	public static final TagKey<Item> REPAIRS_DINOSAUR_CLAW = TagKey.create(Registries.ITEM, Dinosaurs.id("repairs_dinosaur_claw"));
	/**
	 * Above netherite by what netherite is above diamond (diamond 1561 / 3 / 10, netherite 2031 / 4 / 15):
	 * durability 2501, attack damage bonus 6 (so the sword does 10, netherite 8), enchantability 20. Mines like netherite.
	 */
	public static final ToolMaterial DINOSAUR_TOOL_MATERIAL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2501, 9.0F, 6.0F, 20,
			REPAIRS_DINOSAUR_CLAW);
	/** Sword stats like vanilla swords (base 3, speed -2.4 -> 1.6): 10 attack damage. In #minecraft:swords (sweeping, sword enchantments). */
	public static final Item DINOSAUR_CLAW = register("dinosaur_claw", Item::new, new Item.Properties()
			.sword(DINOSAUR_TOOL_MATERIAL, 3.0F, -2.4F).fireResistant().rarity(Rarity.EPIC));

	// Dinosaur Armor: netherite chestplate / leggings / boots upgraded on the smithing table with the Dinosaur Upgrade
	// template and a trophy (Megalodon Tooth / Parasaurolophus Crest / Raptor Claw). The T-Rex Skull Helmet is the set's helmet.
	/** Repair ingredient of the Dinosaur Armor (#dinosaurs:repairs_dinosaur_armor: Dinosaur Bones). */
	public static final TagKey<Item> REPAIRS_DINOSAUR_ARMOR = TagKey.create(Registries.ITEM, Dinosaurs.id("repairs_dinosaur_armor"));
	/**
	 * One step above netherite by the same amount netherite is above diamond (26.x values):
	 * durability multiplier 33 -> 37 -> 41, toughness 2 -> 3 -> 4, knockback resistance 0 -> 0.1 -> 0.2,
	 * enchantability 10 -> 15 -> 20. Armor points are equal for diamond and netherite; by request they are raised to
	 * boots 4 / leggings 8 / chestplate 10 (netherite 3 / 6 / 8). Helmet value unused (the T-Rex Skull Helmet is the set's helmet).
	 */
	public static final ArmorMaterial DINOSAUR_ARMOR_MATERIAL = new ArmorMaterial(41,
			Map.of(ArmorType.BOOTS, 4, ArmorType.LEGGINGS, 8, ArmorType.CHESTPLATE, 10, ArmorType.HELMET, 3, ArmorType.BODY, 19), 20,
			SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.2F, REPAIRS_DINOSAUR_ARMOR,
			ResourceKey.create(EquipmentAssets.ROOT_ID, Dinosaurs.id("dinosaur")));
	/** Smithing template: Netherite Upgrade surrounded by 8 Dinosaur Bones (or duplicated with 7 bones + a bone block). */
	public static final Item DINOSAUR_UPGRADE_SMITHING_TEMPLATE = register("dinosaur_upgrade_smithing_template",
			DinosaurUpgradeTemplate::create, new Item.Properties().rarity(Rarity.RARE));
	public static final Item DINOSAUR_CHESTPLATE = register("dinosaur_chestplate", Item::new, new Item.Properties()
			.humanoidArmor(DINOSAUR_ARMOR_MATERIAL, ArmorType.CHESTPLATE).fireResistant().rarity(Rarity.EPIC));
	public static final Item DINOSAUR_LEGGINGS = register("dinosaur_leggings", Item::new, new Item.Properties()
			.humanoidArmor(DINOSAUR_ARMOR_MATERIAL, ArmorType.LEGGINGS).fireResistant().rarity(Rarity.EPIC));
	public static final Item DINOSAUR_BOOTS = register("dinosaur_boots", Item::new, new Item.Properties()
			.humanoidArmor(DINOSAUR_ARMOR_MATERIAL, ArmorType.BOOTS).fireResistant().rarity(Rarity.EPIC));

	/** Illustrated field guide (Book + Leather + any dinosaur trophy). */
	public static final Item GUIDE_BOOK = register("guide_book", GuideBookItem::new, new Item.Properties().stacksTo(16));

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

	/** Spawn egg of a species (registered in {@link #initialize()}, same order as {@link ModEntities#ALL}). */
	public static Item spawnEgg(EntityType<?> type) {
		return SPAWN_EGGS.get(ModEntities.ALL.indexOf(type));
	}

	private ModItems() {
	}
}
