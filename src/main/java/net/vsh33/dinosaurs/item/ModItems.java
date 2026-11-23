package net.vsh33.dinosaurs.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import java.util.EnumMap;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
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

	/**
	 * Dino Collar (1.7.0): used on a T. rex by a player carrying the Amulet of Dino Protection, it makes the T. rex theirs
	 * (follows, fights for them, never despawns; see HostileDino). 3 T-Rex Teeth / Leather, Dinosaur Bones, Leather / Iron Chain.
	 */
	public static final Item DINO_COLLAR = register("dino_collar", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

	// Rare T. rex drop (killed by a player: 4%, up to 10% with Looting) -> T-Rex Skull Helmet
	public static final Item T_REX_SKULL = register("t_rex_skull", Item::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
	/** Netherite ingot or another skull repair the helmet (#dinosaurs:repairs_t_rex_skull_helmet). */
	public static final TagKey<Item> REPAIRS_T_REX_SKULL_HELMET = TagKey.create(Registries.ITEM, Dinosaurs.id("repairs_t_rex_skull_helmet"));
	/**
	 * Helmet stats, above netherite (3 armor, 3 toughness, 0.1 knockback resistance, 37 x 11 = 407 durability,
	 * enchantability 15) and at least the Dinosaur Armor tier it completes: 4 armor, toughness 4, knockback resistance 0.2,
	 * enchantability 20, 46 x 11 = 506 durability. Not an ArmorItem, so the head layer draws the 3D item model on the head.
	 */
	public static final Item T_REX_SKULL_HELMET = register("t_rex_skull_helmet", TRexSkullHelmetItem::new, new Item.Properties()
			.durability(ArmorItem.Type.HELMET.getDurability(46))
			.attributes(TRexSkullHelmetItem.createAttributes())
			.fireResistant()
			.rarity(Rarity.EPIC));

	// Large dinosaurs drop Dinosaur Bones instead of vanilla bones -> Dinosaur Bone Bow (also 1 -> 4 bone meal)
	public static final Item DINOSAUR_BONES = register("dinosaur_bones", Item::new, new Item.Properties());
	/** Faster draw, 1.5x arrow speed and damage, 3x durability; Infinity + Mending allowed together (see BoneBowEnchantments). */
	public static final Item DINOSAUR_BONE_BOW = register("dinosaur_bone_bow", DinosaurBoneBowItem::new, new Item.Properties()
			.durability(DinosaurBoneBowItem.DURABILITY)
			.fireResistant()
			.rarity(Rarity.EPIC));

	// Dinosaur Claw (1.6.0): a clawed netherite gauntlet worn on the fist, a melee weapon one step above the netherite
	// sword. Raptor Claw, T-Rex Tooth, Raptor Claw / Netherite Ingot, Dinosaur Bones, Netherite Ingot / stick.
	/** Repair ingredient of the Dinosaur Claw (#dinosaurs:repairs_dinosaur_claw: Dinosaur Bones). */
	public static final TagKey<Item> REPAIRS_DINOSAUR_CLAW = TagKey.create(Registries.ITEM, Dinosaurs.id("repairs_dinosaur_claw"));
	/**
	 * Above netherite by what netherite is above diamond (diamond 1561 / 3 / 10, netherite 2031 / 4 / 15):
	 * durability 2501, attack damage bonus 6, enchantability 20. Mines like netherite.
	 */
	public static final Tier DINOSAUR_TOOL_MATERIAL = new Tier() {
		@Override public int getUses() { return 2501; }
		@Override public float getSpeed() { return 9.0F; }
		@Override public float getAttackDamageBonus() { return 6.0F; }
		@Override public TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
		@Override public int getEnchantmentValue() { return 20; }
		@Override public Ingredient getRepairIngredient() { return Ingredient.of(REPAIRS_DINOSAUR_CLAW); }
	};
	/** Attack damage of the claw (player base 1 + modifier 10); the netherite sword does 8. */
	public static final double DINOSAUR_CLAW_DAMAGE = 11.0;
	/** Reach change while the claw is in the main hand: a fist weapon, so 0.5 blocks shorter than a sword (3 -> 2.5). */
	public static final double DINOSAUR_CLAW_REACH = -0.5;
	public static final ResourceLocation DINOSAUR_CLAW_REACH_ID = Dinosaurs.id("dinosaur_claw_reach");
	/** A sword (sweeping, sword enchantments, cobwebs) with 11 attack damage, speed 1.6 and entity interaction range -0.5. */
	public static final Item DINOSAUR_CLAW = register("dinosaur_claw", p -> new SwordItem(DINOSAUR_TOOL_MATERIAL, p), new Item.Properties()
			.attributes(ItemAttributeModifiers.builder()
					.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, DINOSAUR_CLAW_DAMAGE - 1.0,
							AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4,
							AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(DINOSAUR_CLAW_REACH_ID, DINOSAUR_CLAW_REACH,
							AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
					.build())
			.fireResistant().rarity(Rarity.EPIC));

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
	public static final Holder<ArmorMaterial> DINOSAUR_ARMOR_MATERIAL = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
			Dinosaurs.id("dinosaur"), new ArmorMaterial(
					Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
						m.put(ArmorItem.Type.BOOTS, 4);
						m.put(ArmorItem.Type.LEGGINGS, 8);
						m.put(ArmorItem.Type.CHESTPLATE, 10);
						m.put(ArmorItem.Type.HELMET, 3);
						m.put(ArmorItem.Type.BODY, 19);
					}), 20, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(REPAIRS_DINOSAUR_ARMOR),
					List.of(new ArmorMaterial.Layer(Dinosaurs.id("dinosaur"))), 4.0F, 0.2F));
	/** Durability multiplier of the Dinosaur Armor (netherite 37). */
	public static final int DINOSAUR_ARMOR_DURABILITY = 41;
	/** Smithing template: Netherite Upgrade surrounded by 8 Dinosaur Bones (or duplicated with 7 bones + a bone block). */
	public static final Item DINOSAUR_UPGRADE_SMITHING_TEMPLATE = register("dinosaur_upgrade_smithing_template",
			DinosaurUpgradeTemplate::create, new Item.Properties().rarity(Rarity.RARE));
	public static final Item DINOSAUR_CHESTPLATE = registerArmor("dinosaur_chestplate", ArmorItem.Type.CHESTPLATE);
	public static final Item DINOSAUR_LEGGINGS = registerArmor("dinosaur_leggings", ArmorItem.Type.LEGGINGS);
	public static final Item DINOSAUR_BOOTS = registerArmor("dinosaur_boots", ArmorItem.Type.BOOTS);

	private static Item registerArmor(String name, ArmorItem.Type type) {
		return register(name, p -> new ArmorItem(DINOSAUR_ARMOR_MATERIAL, type, p), new Item.Properties()
				.durability(type.getDurability(DINOSAUR_ARMOR_DURABILITY)).fireResistant().rarity(Rarity.EPIC));
	}

	/** Illustrated field guide (Book + Leather + any dinosaur trophy). */
	public static final Item GUIDE_BOOK = register("guide_book", GuideBookItem::new, new Item.Properties().stacksTo(16));

	/** Creative tab icon only (Spinosaurus head); not listed in any tab. */
	public static final Item TAB_ICON = registerHidden("tab_icon");

	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Dinosaurs.id("dinosaurs"));

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		Item item = Registry.register(BuiltInRegistries.ITEM, Dinosaurs.id(name), factory.apply(properties));
		ITEMS.add(item);
		return item;
	}

	private static Item registerHidden(String name) {
		return Registry.register(BuiltInRegistries.ITEM, Dinosaurs.id(name), new Item(new Item.Properties()));
	}

	public static void initialize() {
		for (EntityType<? extends Mob> type : ModEntities.ALL) {
			String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + "_spawn_egg";
			// The egg models use their own textures (no tint layers), so the colours are unused.
			Item egg = new SpawnEggItem(type, 0xFFFFFF, 0xFFFFFF, new Item.Properties());
			SPAWN_EGGS.add(Registry.register(BuiltInRegistries.ITEM, Dinosaurs.id(name), egg));
		}

		CreativeModeTab tab = FabricItemGroup.builder()
				.icon(() -> new ItemStack(TAB_ICON))
				.title(Component.translatable("itemGroup.dinosaurs.dinosaurs"))
				.displayItems((params, output) -> {
					SPAWN_EGGS.forEach(output::accept);
					ITEMS.forEach(output::accept);
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> SPAWN_EGGS.forEach(output::accept));
	}

	/** Spawn egg of a species (registered in {@link #initialize()}, same order as {@link ModEntities#ALL}). */
	public static Item spawnEgg(EntityType<?> type) {
		return SPAWN_EGGS.get(ModEntities.ALL.indexOf(type));
	}

	private ModItems() {
	}
}
