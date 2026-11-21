package net.vsh33.dinosaurs.client.book;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.dinosaurs.entity.ModEntities;
import net.vsh33.dinosaurs.item.DinosaurBoneBowItem;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * Chapters, entries and embedded recipe layouts of the dinosaur field guide.
 * <p>
 * Minecraft 26.x does not sync the full {@code RecipeManager} to the client (only
 * recipe-book displays), so the grids are embedded here. They match the recipe JSON
 * in the data pack exactly (checked by the server game test).
 */
public final class GuideBookContents {
	public enum Section {
		INDEX,
		DINO_LIST,
		ITEM_LIST,
		SETTINGS,
		ENTRY
	}

	public record Location(Section section, String entryId) {
		public static Location index() {
			return new Location(Section.INDEX, null);
		}

		public static Location dinoList() {
			return new Location(Section.DINO_LIST, null);
		}

		public static Location itemList() {
			return new Location(Section.ITEM_LIST, null);
		}

		public static Location settings() {
			return new Location(Section.SETTINGS, null);
		}

		public static Location entry(String id) {
			return new Location(Section.ENTRY, id);
		}
	}

	/** 3×3 grid; each slot lists alternatives that the book cycles through (tag ingredients). */
	public record RecipeGrid(boolean shaped, List<List<ItemStack>> slots, ItemStack result) {
	}

	/**
	 * One page spread. Dinosaur entries have an {@code entity} (rendered as a picture) and
	 * a habitat text; item entries have recipes and "dropped by" links.
	 */
	public record Entry(String id, ItemStack icon, Component title, Component body, EntityType<? extends Mob> entity,
			Component habitat, List<ItemStack> drops, List<RecipeGrid> recipes, List<EntityType<? extends Mob>> droppedBy,
			List<StatRow> stats) {
		public boolean isDino() {
			return entity != null;
		}
	}

	/** One line of an armor stats table: this item's value next to the netherite helmet's. */
	public record StatRow(Component label, String value, String netherite) {
	}

	/** Item the helmet stats table compares against. */
	public static final ItemStack STATS_REFERENCE = new ItemStack(Items.NETHERITE_HELMET);
	/** Item the bow stats table compares against. */
	public static final ItemStack BOW_REFERENCE = new ItemStack(Items.BOW);

	/** The vanilla item an entry's stats table is compared against (second value column). */
	public static ItemStack statsReference(Entry entry) {
		return entry.icon().is(ModItems.DINOSAUR_BONE_BOW) ? BOW_REFERENCE : STATS_REFERENCE;
	}

	public record Category(String id, Location target, ItemStack icon, Component title) {
	}

	/** Picture framing per species: pixels per block and vertical nudge (blocks), tuned on screenshots. */
	public record Framing(float size, float offsetX, boolean grounded) {
	}

	private final List<Category> categories;
	private final List<Entry> dinoEntries;
	private final List<Entry> itemEntries;
	private final List<Location> readingOrder;

	private GuideBookContents(List<Category> categories, List<Entry> dinos, List<Entry> items, List<Location> readingOrder) {
		this.categories = categories;
		this.dinoEntries = dinos;
		this.itemEntries = items;
		this.readingOrder = readingOrder;
	}

	/** Items any of which can be the third ingredient of the guide book (tag {@code #dinosaurs:guide_book_trophies}). */
	public static List<ItemStack> trophies() {
		return List.of(new ItemStack(ModItems.PARASAUROLOPHUS_CREST), new ItemStack(ModItems.BRACHIOSAURUS_HIDE),
				new ItemStack(ModItems.RAPTOR_CLAW), new ItemStack(ModItems.T_REX_TOOTH), new ItemStack(ModItems.MEGALODON_TOOTH));
	}

	public static GuideBookContents create() {
		ItemStack bone = new ItemStack(Items.BONE);
		ItemStack dinoBones = new ItemStack(ModItems.DINOSAUR_BONES);
		ItemStack meal = new ItemStack(Items.BONE_MEAL);
		List<Entry> dinos = List.of(
				dino("t_rex", ModEntities.T_REX, dinoBones, meal, new ItemStack(ModItems.T_REX_TOOTH), new ItemStack(ModItems.T_REX_SKULL)),
				dino("velociraptor", ModEntities.VELOCIRAPTOR, bone, new ItemStack(ModItems.RAPTOR_CLAW)),
				dino("spinosaurus", ModEntities.SPINOSAURUS, dinoBones, meal),
				dino("megalodon", ModEntities.MEGALODON, bone, meal, new ItemStack(ModItems.MEGALODON_TOOTH)),
				dino("triceratops", ModEntities.TRICERATOPS, dinoBones, meal),
				dino("stegosaurus", ModEntities.STEGOSAURUS, dinoBones, meal),
				dino("ankylosaurus", ModEntities.ANKYLOSAURUS, dinoBones, meal),
				dino("brachiosaurus", ModEntities.BRACHIOSAURUS, dinoBones, meal, new ItemStack(ModItems.BRACHIOSAURUS_HIDE)),
				dino("parasaurolophus", ModEntities.PARASAUROLOPHUS, dinoBones, new ItemStack(ModItems.PARASAUROLOPHUS_CREST)),
				dino("pteranodon", ModEntities.PTERANODON, bone));

		RecipeGrid amulet = shaped(new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION),
				Items.STRING, Items.GOLD_INGOT, Items.STRING,
				ModItems.RAPTOR_CLAW, ModItems.T_REX_TOOTH, ModItems.RAPTOR_CLAW,
				null, ModItems.MEGALODON_TOOTH, null);
		RecipeGrid saddle = shaped(new ItemStack(ModItems.DINOSAUR_SADDLE),
				ModItems.BRACHIOSAURUS_HIDE, Items.LEATHER, ModItems.BRACHIOSAURUS_HIDE,
				Items.STRING, ModItems.PARASAUROLOPHUS_CREST, Items.STRING,
				null, Items.IRON_INGOT, null);
		RecipeGrid helmet = shapeless(new ItemStack(ModItems.T_REX_SKULL_HELMET), List.of(new ItemStack(ModItems.T_REX_SKULL)),
				List.of(new ItemStack(Items.LEATHER)), List.of(new ItemStack(Items.NETHERITE_INGOT)));
		RecipeGrid boneBow = shaped(new ItemStack(ModItems.DINOSAUR_BONE_BOW),
				null, ModItems.DINOSAUR_BONES, null,
				ModItems.DINOSAUR_BONES, Items.BOW, ModItems.DINOSAUR_BONES,
				null, Items.NETHERITE_INGOT, null);
		RecipeGrid boneMeal = shapeless(new ItemStack(Items.BONE_MEAL, 4), List.of(new ItemStack(ModItems.DINOSAUR_BONES)));
		RecipeGrid leather = shapeless(new ItemStack(Items.LEATHER, 2), List.of(new ItemStack(ModItems.BRACHIOSAURUS_HIDE)));

		List<Entry> items = List.of(
				item("guide_book", ModItems.GUIDE_BOOK, List.of(guideBookRecipe()), List.of()),
				item("amulet_of_dino_protection", ModItems.AMULET_OF_DINO_PROTECTION, List.of(amulet), List.of()),
				item("dinosaur_saddle", ModItems.DINOSAUR_SADDLE, List.of(saddle), List.of()),
				item("t_rex_skull_helmet", ModItems.T_REX_SKULL_HELMET, List.of(helmet), List.of(), helmetStats()),
				item("dinosaur_bone_bow", ModItems.DINOSAUR_BONE_BOW, List.of(boneBow), List.of(), bowStats()),
				item("dinosaur_bones", ModItems.DINOSAUR_BONES, List.of(boneBow, boneMeal), BONE_DROPPERS),
				item("t_rex_tooth", ModItems.T_REX_TOOTH, List.of(amulet), List.of(ModEntities.T_REX)),
				item("t_rex_skull", ModItems.T_REX_SKULL, List.of(helmet), List.of(ModEntities.T_REX)),
				item("raptor_claw", ModItems.RAPTOR_CLAW, List.of(amulet), List.of(ModEntities.VELOCIRAPTOR)),
				item("megalodon_tooth", ModItems.MEGALODON_TOOTH, List.of(amulet), List.of(ModEntities.MEGALODON)),
				item("brachiosaurus_hide", ModItems.BRACHIOSAURUS_HIDE, List.of(saddle, leather), List.of(ModEntities.BRACHIOSAURUS)),
				item("parasaurolophus_crest", ModItems.PARASAUROLOPHUS_CREST, List.of(saddle), List.of(ModEntities.PARASAUROLOPHUS)));

		List<Category> categories = List.of(
				new Category("dinosaurs", Location.dinoList(), new ItemStack(ModItems.TAB_ICON),
						Component.translatable("book.dinosaurs.category.dinosaurs")),
				new Category("items", Location.itemList(), new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION),
						Component.translatable("book.dinosaurs.category.items")),
				new Category("settings", Location.settings(), new ItemStack(Items.REDSTONE),
						Component.translatable("book.dinosaurs.category.settings")));

		List<Location> order = new ArrayList<>();
		order.add(Location.index());
		order.add(Location.dinoList());
		for (Entry entry : dinos) {
			order.add(Location.entry(entry.id()));
		}
		order.add(Location.itemList());
		for (Entry entry : items) {
			order.add(Location.entry(entry.id()));
		}
		order.add(Location.settings());
		return new GuideBookContents(categories, dinos, items, List.copyOf(order));
	}

	/** Picture framing per species, tuned on screenshots. Ground dwellers stand on the grass strip; flyers and swimmers float. */
	public static Framing framing(EntityType<?> type) {
		if (type == ModEntities.T_REX) return new Framing(14.5F, 0.0F, true);
		if (type == ModEntities.SPINOSAURUS) return new Framing(15.0F, 0.0F, true);
		if (type == ModEntities.VELOCIRAPTOR) return new Framing(48.0F, 0.0F, true);
		if (type == ModEntities.TRICERATOPS) return new Framing(20.0F, 0.0F, true);
		if (type == ModEntities.STEGOSAURUS) return new Framing(17.0F, 0.0F, true);
		if (type == ModEntities.ANKYLOSAURUS) return new Framing(23.0F, 0.0F, true);
		if (type == ModEntities.BRACHIOSAURUS) return new Framing(11.0F, 0.0F, true);
		if (type == ModEntities.PARASAUROLOPHUS) return new Framing(19.0F, 0.0F, true);
		if (type == ModEntities.MEGALODON) return new Framing(15.0F, 0.5F, false);
		return new Framing(24.0F, 0.0F, false);
	}

	public List<Category> categories() {
		return categories;
	}

	public List<Entry> dinoEntries() {
		return dinoEntries;
	}

	public List<Entry> itemEntries() {
		return itemEntries;
	}

	public List<Location> readingOrder() {
		return readingOrder;
	}

	public static RecipeGrid guideBookRecipe() {
		return shapeless(new ItemStack(ModItems.GUIDE_BOOK), List.of(new ItemStack(Items.BOOK)), List.of(new ItemStack(Items.LEATHER)), trophies());
	}

	public Entry entry(String id) {
		for (Entry entry : dinoEntries) {
			if (entry.id().equals(id)) {
				return entry;
			}
		}
		for (Entry entry : itemEntries) {
			if (entry.id().equals(id)) {
				return entry;
			}
		}
		return null;
	}

	/** Entry describing this item or species (for clickable slots and drop icons), or null. */
	public Entry entryFor(ItemStack stack) {
		for (Entry entry : itemEntries) {
			if (!stack.isEmpty() && entry.icon().getItem() == stack.getItem()) {
				return entry;
			}
		}
		for (Entry entry : dinoEntries) {
			if (!stack.isEmpty() && entry.icon().getItem() == stack.getItem()) {
				return entry;
			}
		}
		return null;
	}

	public Entry entryFor(EntityType<?> type) {
		for (Entry entry : dinoEntries) {
			if (entry.entity() == type) {
				return entry;
			}
		}
		return null;
	}

	public int indexOf(Location location) {
		for (int i = 0; i < readingOrder.size(); i++) {
			if (same(readingOrder.get(i), location)) {
				return i;
			}
		}
		return 0;
	}

	public static boolean same(Location a, Location b) {
		if (a.section() != b.section()) {
			return false;
		}
		if (a.entryId() == null) {
			return b.entryId() == null;
		}
		return a.entryId().equals(b.entryId());
	}

	private static Entry dino(String id, EntityType<? extends Mob> type, ItemStack... drops) {
		return new Entry(id, new ItemStack(ModItems.spawnEgg(type)), type.getDescription(),
				Component.translatable("book.dinosaurs.dino." + id), type,
				Component.translatable("book.dinosaurs.habitat." + id), List.of(drops), List.of(), List.of(), List.of());
	}

	private static Entry item(String id, Item item, List<RecipeGrid> recipes, List<EntityType<? extends Mob>> droppedBy) {
		return item(id, item, recipes, droppedBy, List.of());
	}

	private static Entry item(String id, Item item, List<RecipeGrid> recipes, List<EntityType<? extends Mob>> droppedBy, List<StatRow> stats) {
		ItemStack icon = new ItemStack(item);
		return new Entry(id, icon, icon.getHoverName(), Component.translatable("book.dinosaurs.item." + id), null, null,
				List.of(), recipes, droppedBy, stats);
	}

	/**
	 * Helmet vs. netherite helmet, read from the real items so the book can never disagree with the game.
	 * Knockback resistance is shown x10, as in the vanilla tooltip (netherite: +1).
	 */
	public static List<StatRow> helmetStats() {
		ItemStack ours = new ItemStack(ModItems.T_REX_SKULL_HELMET);
		return List.of(
				new StatRow(Component.translatable("book.dinosaurs.stats.armor"), num(attr(ours, Attributes.ARMOR)), num(attr(STATS_REFERENCE, Attributes.ARMOR))),
				new StatRow(Component.translatable("book.dinosaurs.stats.toughness"), num(attr(ours, Attributes.ARMOR_TOUGHNESS)),
						num(attr(STATS_REFERENCE, Attributes.ARMOR_TOUGHNESS))),
				new StatRow(Component.translatable("book.dinosaurs.stats.knockback"), num(10 * attr(ours, Attributes.KNOCKBACK_RESISTANCE)),
						num(10 * attr(STATS_REFERENCE, Attributes.KNOCKBACK_RESISTANCE))),
				new StatRow(Component.translatable("book.dinosaurs.stats.durability"), Integer.toString(ours.getMaxDamage()),
						Integer.toString(STATS_REFERENCE.getMaxDamage())));
	}

	/** Large dinosaurs whose bones are Dinosaur Bones (must match DINO_BONES in tools/gen_data.py). */
	public static final List<EntityType<? extends Mob>> BONE_DROPPERS = List.of(ModEntities.T_REX, ModEntities.SPINOSAURUS,
			ModEntities.BRACHIOSAURUS, ModEntities.TRICERATOPS, ModEntities.STEGOSAURUS, ModEntities.ANKYLOSAURUS,
			ModEntities.PARASAUROLOPHUS);

	/** Bone bow vs. vanilla bow, from the bow's own constants. Damage is a full-charge arrow without Power. */
	public static List<StatRow> bowStats() {
		ItemStack ours = new ItemStack(ModItems.DINOSAUR_BONE_BOW);
		return List.of(
				new StatRow(Component.translatable("book.dinosaurs.stats.arrow_damage"),
						Integer.toString((int) Math.ceil(DinosaurBoneBowItem.VELOCITY * 2.0)), Integer.toString((int) Math.ceil(3.0F * 2.0))),
				new StatRow(Component.translatable("book.dinosaurs.stats.arrow_speed"), num(DinosaurBoneBowItem.VELOCITY), num(3.0)),
				new StatRow(Component.translatable("book.dinosaurs.stats.draw_ticks"), Integer.toString(DinosaurBoneBowItem.DRAW_TICKS), "20"),
				new StatRow(Component.translatable("book.dinosaurs.stats.durability"), Integer.toString(ours.getMaxDamage()),
						Integer.toString(BOW_REFERENCE.getMaxDamage())));
	}

	private static double attr(ItemStack stack, Holder<Attribute> attribute) {
		double[] total = {0};
		stack.forEachModifier(EquipmentSlot.HEAD, (holder, modifier) -> {
			if (holder.equals(attribute) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				total[0] += modifier.amount();
			}
		});
		return total[0];
	}

	private static String num(double value) {
		double rounded = Math.round(value * 100.0) / 100.0;
		return rounded == Math.rint(rounded) ? Long.toString(Math.round(rounded)) : Double.toString(rounded);
	}

	private static RecipeGrid shaped(ItemStack result, Item... pattern) {
		List<List<ItemStack>> slots = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			Item item = i < pattern.length ? pattern[i] : null;
			slots.add(item == null ? List.of() : List.of(new ItemStack(item)));
		}
		return new RecipeGrid(true, List.copyOf(slots), result);
	}

	@SafeVarargs
	private static RecipeGrid shapeless(ItemStack result, List<ItemStack>... ingredients) {
		List<List<ItemStack>> slots = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) {
			slots.add(i < ingredients.length ? ingredients[i] : List.of());
		}
		return new RecipeGrid(false, List.copyOf(slots), result);
	}

}
