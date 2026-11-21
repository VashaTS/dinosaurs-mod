package net.vsh33.dinosaurs.test;

import eu.pb4.trinkets.api.DefaultTrinketSlotTags;
import eu.pb4.trinkets.api.TrinketInventory;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.block.Blocks;
import net.vsh33.dinosaurs.config.DinoConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.entity.ModEntities;
import net.vsh33.dinosaurs.entity.Triceratops;
import net.vsh33.dinosaurs.item.ModItems;
import net.vsh33.dinosaurs.item.DinosaurBoneBowItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Map;

/** Server game tests (./gradlew runGameTest): amulet logic, saddle/riding logic, recipes and advancements. */
public class DinosaursServerTest {
	/** Survival mock player (the in-level mock player is creative, which no mob ever targets). */
	static Player survivalPlayer(GameTestHelper helper) {
		helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		return helper.makeMockPlayer(GameType.SURVIVAL);
	}

	private static void checkAmulet(GameTestHelper helper, Mob dino) {
		Player player = survivalPlayer(helper);
		player.snapTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
		helper.assertTrue(dino.canAttack(player), "dino should be able to attack a player without the amulet");
		player.getInventory().add(new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION));
		helper.assertFalse(dino.canAttack(player), "dino must not attack a player carrying the amulet");
		dino.setTarget(player);
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(dino.getTarget() == null, "protected player must be dropped as target, got " + dino.getTarget());
			dino.hurtServer(helper.getLevel(), player.damageSources().playerAttack(player), 1.0F);
			helper.assertTrue(dino.canAttack(player), "after being attacked the dino may retaliate");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void raptorAmulet(GameTestHelper helper) {
		checkAmulet(helper, helper.spawn(ModEntities.VELOCIRAPTOR, new BlockPos(4, 1, 4)));
	}

	@GameTest(maxTicks = 60)
	public void tRexAmulet(GameTestHelper helper) {
		checkAmulet(helper, helper.spawn(ModEntities.T_REX, new BlockPos(4, 1, 4)));
	}

	@GameTest(maxTicks = 60)
	public void megalodonAmulet(GameTestHelper helper) {
		checkAmulet(helper, helper.spawn(ModEntities.MEGALODON, new BlockPos(4, 1, 4)));
	}

	@GameTest(maxTicks = 40)
	public void saddleAndRide(GameTestHelper helper) {
		Player player = survivalPlayer(helper);
		player.snapTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
		Triceratops trike = helper.spawn(ModEntities.TRICERATOPS, new BlockPos(4, 1, 4));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DINOSAUR_SADDLE));
		trike.interact(player, InteractionHand.MAIN_HAND, trike.position());
		helper.assertTrue(trike.isSaddled(), "saddle should be applied");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "saddle item should be consumed");
		trike.interact(player, InteractionHand.MAIN_HAND, trike.position());
		helper.assertTrue(player.getVehicle() == trike, "player should be riding");
		helper.assertTrue(trike.getControllingPassenger() == player, "rider should control the triceratops");
		helper.succeed();
	}

	/** Only meaningful with Trinkets installed: ./gradlew runGameTest -PwithTrinkets=true */
	@GameTest(maxTicks = 40)
	public void trinketsNecklaceSlot(GameTestHelper helper) {
		if (!FabricLoader.getInstance().isModLoaded("trinkets")) {
			Dinosaurs.LOGGER.info("[test] Trinkets not installed, skipping necklace slot test");
			helper.succeed();
			return;
		}
		TrinketsCheck.run(helper);
	}

	private static final class TrinketsCheck {
		static void run(GameTestHelper helper) {
			Player player = survivalPlayer(helper);
			player.snapTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
			Mob raptor = helper.spawn(ModEntities.VELOCIRAPTOR, new BlockPos(4, 1, 4));
			ItemStack amulet = new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION);
			helper.assertTrue(amulet.typeHolder().is(DefaultTrinketSlotTags.CHEST_NECKLACE), "amulet must be tagged #trinkets:chest/necklace");
			var groups = TrinketsApi.getAttachment(player).getInventory();
			helper.assertTrue(groups.containsKey("chest") && groups.get("chest").containsKey("necklace"), "player needs a chest/necklace slot, got " + groups.keySet());
			TrinketInventory necklace = groups.get("chest").get("necklace");
			necklace.setItem(0, amulet);
			helper.assertTrue(player.getInventory().isEmpty(), "main inventory should be empty");
			helper.assertFalse(raptor.canAttack(player), "amulet in the necklace slot must protect the player");
			Dinosaurs.LOGGER.info("[test] Trinkets necklace slot OK");
			helper.succeed();
		}
	}

	@GameTest(skyAccess = true)
	public void configSpawnToggles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos rel = new BlockPos(3, 2, 3);
		helper.setBlock(rel.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(rel, Blocks.AIR);
		BlockPos pos = helper.absolutePos(rel);
		DinoConfig config = DinoConfig.get();
		boolean before = SpawnPlacements.checkSpawnRules(ModEntities.TRICERATOPS, level, EntitySpawnReason.NATURAL, pos, level.getRandom());
		config.setSpawnEnabled(ModEntities.TRICERATOPS, false);
		boolean disabled = SpawnPlacements.checkSpawnRules(ModEntities.TRICERATOPS, level, EntitySpawnReason.NATURAL, pos, level.getRandom());
		boolean egg = SpawnPlacements.checkSpawnRules(ModEntities.TRICERATOPS, level, EntitySpawnReason.SPAWN_ITEM_USE, pos, level.getRandom());
		config.setSpawnEnabled(ModEntities.TRICERATOPS, true);
		Dinosaurs.LOGGER.info("[test] triceratops natural spawn: enabled={} disabled={} spawnEgg(disabled)={}", before, disabled, egg);
		helper.assertTrue(before, "baseline natural spawn should be allowed on grass in daylight");
		helper.assertFalse(disabled, "disabled species must not spawn naturally");
		helper.assertTrue(egg, "spawn eggs must ignore the config");
		helper.succeed();
	}

	@GameTest
	public void recipesAndAdvancements(GameTestHelper helper) {
		for (String id : new String[]{"amulet_of_dino_protection", "dinosaur_saddle", "leather_from_brachiosaurus_hide", "t_rex_skull_helmet", "dinosaur_bone_bow", "bone_meal_from_dinosaur_bones"}) {
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Dinosaurs.id(id))).isPresent(), "missing recipe " + id);
		}
		for (String id : new String[]{"amulet", "ride_triceratops", "recipes/equipment/dinosaur_saddle", "recipes/equipment/amulet_of_dino_protection", "recipes/misc/leather_from_brachiosaurus_hide", "recipes/equipment/dinosaur_bone_bow", "recipes/misc/bone_meal_from_dinosaur_bones"}) {
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Dinosaurs.id(id)) != null, "missing advancement " + id);
		}
		helper.succeed();
	}

	private static List<String> craftingMatches(GameTestHelper helper, CraftingInput input) {
		var level = helper.getLevel();
		List<String> matches = new ArrayList<>();
		for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
			if (holder.value().getType() != RecipeType.CRAFTING) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Recipe<CraftingInput> recipe = (Recipe<CraftingInput>) holder.value();
			if (recipe.matches(input, level)) {
				matches.add(holder.id().identifier().toString());
			}
		}
		return matches;
	}

	/**
	 * Guide book: Book + Leather + any dinosaur trophy is the ONLY recipe for that grid; Book + Leather alone
	 * (left free for other mods' guide books) and Book + Leather + Bone do not make it.
	 */
	@GameTest
	public void guideBookRecipe(GameTestHelper helper) {
		TagKey<Item> trophies = TagKey.create(Registries.ITEM, Dinosaurs.id("guide_book_trophies"));
		Item[] expected = {ModItems.PARASAUROLOPHUS_CREST, ModItems.BRACHIOSAURUS_HIDE, ModItems.RAPTOR_CLAW, ModItems.T_REX_TOOTH, ModItems.MEGALODON_TOOTH};
		int tagged = 0;
		for (Item item : BuiltInRegistries.ITEM) {
			if (new ItemStack(item).is(trophies)) {
				tagged++;
			}
		}
		helper.assertTrue(tagged == expected.length, "trophy tag should hold " + expected.length + " items, has " + tagged);
		CraftingRecipe recipe = (CraftingRecipe) helper.getLevel().getServer().getRecipeManager()
				.byKey(ResourceKey.create(Registries.RECIPE, Dinosaurs.id("guide_book"))).orElseThrow().value();
		for (Item trophy : expected) {
			helper.assertTrue(new ItemStack(trophy).is(trophies), trophy + " must be a guide book trophy");
			CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(trophy), new ItemStack(Items.BOOK), new ItemStack(Items.LEATHER)));
			List<String> matches = craftingMatches(helper, input);
			helper.assertTrue(matches.equals(List.of("dinosaurs:guide_book")), "book + leather + " + trophy + " matches " + matches);
			ItemStack out = recipe.assemble(input);
			helper.assertTrue(out.is(ModItems.GUIDE_BOOK) && out.getCount() == 1, "should craft one guide book, got " + out);
		}
		List<String> plain = craftingMatches(helper, CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.LEATHER))));
		helper.assertTrue(plain.isEmpty(), "book + leather must stay free, matches " + plain);
		List<String> bone = craftingMatches(helper, CraftingInput.of(3, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.LEATHER), new ItemStack(Items.BONE))));
		helper.assertTrue(bone.isEmpty(), "book + leather + bone must not match, got " + bone);
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Dinosaurs.id("recipes/misc/guide_book")) != null, "missing guide book recipe advancement");
		Dinosaurs.LOGGER.info("[test] guide book: {} trophies, no conflicts, book + leather left free", expected.length);
		helper.succeed();
	}

	/** T-Rex Skull + Leather + Netherite Ingot (any order) makes the helmet, and nothing else uses that grid. */
	@GameTest
	public void skullHelmetRecipe(GameTestHelper helper) {
		CraftingRecipe recipe = (CraftingRecipe) helper.getLevel().getServer().getRecipeManager()
				.byKey(ResourceKey.create(Registries.RECIPE, Dinosaurs.id("t_rex_skull_helmet"))).orElseThrow().value();
		List<List<ItemStack>> orders = List.of(
				List.of(new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.LEATHER), new ItemStack(Items.NETHERITE_INGOT)),
				List.of(new ItemStack(Items.NETHERITE_INGOT), new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.LEATHER)));
		for (List<ItemStack> items : orders) {
			CraftingInput input = CraftingInput.of(3, 1, items);
			List<String> matches = craftingMatches(helper, input);
			helper.assertTrue(matches.equals(List.of("dinosaurs:t_rex_skull_helmet")), "skull + leather + netherite matches " + matches);
			ItemStack out = recipe.assemble(input);
			helper.assertTrue(out.is(ModItems.T_REX_SKULL_HELMET) && out.getCount() == 1, "should craft one helmet, got " + out);
		}
		CraftingInput tall = CraftingInput.of(1, 3, List.of(new ItemStack(Items.LEATHER), new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.NETHERITE_INGOT)));
		helper.assertTrue(craftingMatches(helper, tall).equals(List.of("dinosaurs:t_rex_skull_helmet")), "shapeless recipe should work in a column too");
		for (List<ItemStack> partial : List.of(
				List.of(new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.LEATHER)),
				List.of(new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.NETHERITE_INGOT)),
				List.of(new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.BOOK), new ItemStack(Items.LEATHER)))) {
			List<String> matches = craftingMatches(helper, CraftingInput.of(partial.size(), 1, partial));
			helper.assertTrue(matches.isEmpty(), partial + " must not craft anything, got " + matches);
		}
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Dinosaurs.id("recipes/equipment/t_rex_skull_helmet")) != null,
				"missing helmet recipe advancement");
		helper.succeed();
	}

	/** Above netherite in every stat, fire resistant, a head item, repairable with netherite or a skull, and wearable. */
	@GameTest(maxTicks = 40)
	public void skullHelmetStats(GameTestHelper helper) {
		ItemStack helmet = new ItemStack(ModItems.T_REX_SKULL_HELMET);
		ItemStack netherite = new ItemStack(Items.NETHERITE_HELMET);
		double[] ours = headStats(helmet);
		double[] neth = headStats(netherite);
		Dinosaurs.LOGGER.info("[test] helmet armor/toughness/knockback {}/{}/{} durability {} (netherite {}/{}/{} {})",
				ours[0], ours[1], ours[2], helmet.getMaxDamage(), neth[0], neth[1], neth[2], netherite.getMaxDamage());
		for (int i = 0; i < 3; i++) {
			helper.assertTrue(ours[i] > neth[i], "stat " + i + " should beat netherite: " + ours[i] + " vs " + neth[i]);
		}
		helper.assertTrue(helmet.getMaxDamage() > netherite.getMaxDamage(), "durability should beat netherite");
		helper.assertTrue(helmet.get(net.minecraft.core.component.DataComponents.ENCHANTABLE).value()
				>= netherite.get(net.minecraft.core.component.DataComponents.ENCHANTABLE).value(), "enchantability at least netherite");
		helper.assertTrue(helmet.has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT), "helmet must be fire resistant");
		helper.assertTrue(helmet.is(net.minecraft.tags.ItemTags.HEAD_ARMOR), "helmet must be #minecraft:head_armor (helmet enchantments)");
		var repairable = helmet.get(net.minecraft.core.component.DataComponents.REPAIRABLE);
		helper.assertTrue(repairable != null && repairable.isValidRepairItem(new ItemStack(Items.NETHERITE_INGOT))
				&& repairable.isValidRepairItem(new ItemStack(ModItems.T_REX_SKULL)), "repair with netherite ingot or T-Rex skull");
		var equippable = helmet.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
		helper.assertTrue(equippable != null && equippable.slot() == net.minecraft.world.entity.EquipmentSlot.HEAD && equippable.assetId().isEmpty(),
				"head slot, no flat armor layer (3D model is drawn instead)");
		Player player = survivalPlayer(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, helmet.copy());
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.T_REX_SKULL_HELMET), "right-click should put it on");
		// Attribute modifiers from equipment are applied on the wearer's next tick (the mock player is not ticked).
		var stand = helper.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new BlockPos(2, 1, 2));
		stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet.copy());
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(stand.getArmorValue() == (int) ours[0], "worn helmet should give " + ours[0] + " armor, got " + stand.getArmorValue());
			helper.succeed();
		});
	}

	private static double[] headStats(ItemStack stack) {
		double[] stats = new double[3];
		stack.forEachModifier(net.minecraft.world.entity.EquipmentSlot.HEAD, (attribute, modifier) -> {
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)) stats[0] += modifier.amount();
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)) stats[1] += modifier.amount();
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) stats[2] += modifier.amount();
		});
		return stats;
	}

	/**
	 * T. rex loot table, rolled 4000 times: the skull drops only when a player killed it, about 4% of the time,
	 * about 10% with Looting III; the tooth still drops every time.
	 */
	@GameTest(maxTicks = 100)
	public void tRexSkullLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var table = level.getServer().reloadableRegistries().getLootTable(
				ResourceKey.create(Registries.LOOT_TABLE, Dinosaurs.id("entities/t_rex")));
		Mob rex = helper.spawn(ModEntities.T_REX, new BlockPos(4, 1, 4));
		Player player = survivalPlayer(helper);
		int rolls = 4000;
		int noPlayer = skulls(table, level, rex, null, rolls);
		int plain = skulls(table, level, rex, player, rolls);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), 3);
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		int looting = skulls(table, level, rex, player, rolls);
		Dinosaurs.LOGGER.info("[test] T-Rex skull drops in {} rolls: no player {}, player {}, Looting III {}", rolls, noPlayer, plain, looting);
		helper.assertTrue(noPlayer == 0, "skull must need a player kill, dropped " + noPlayer);
		helper.assertTrue(plain >= 110 && plain <= 220, "about 4% expected (160), got " + plain);
		helper.assertTrue(looting >= 320 && looting <= 480, "about 10% with Looting III expected (400), got " + looting);
		rex.discard();
		helper.succeed();
	}

	private static int skulls(net.minecraft.world.level.storage.loot.LootTable table, ServerLevel level, Mob rex, Player player, int rolls) {
		var source = player != null ? level.damageSources().playerAttack(player) : level.damageSources().generic();
		var builder = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, rex)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, rex.position())
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, source)
				.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, player);
		if (player != null) {
			builder.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, player);
		}
		var params = builder.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
		int skulls = 0;
		int teeth = 0;
		for (int i = 0; i < rolls; i++) {
			for (ItemStack stack : table.getRandomItems(params)) {
				if (stack.is(ModItems.T_REX_SKULL)) skulls += stack.getCount();
				if (stack.is(ModItems.T_REX_TOOTH)) teeth++;
			}
		}
		if (teeth != rolls) {
			throw new AssertionError("every roll should still drop teeth, got " + teeth + "/" + rolls);
		}
		return skulls;
	}

	// ---- 1.4.0: Dinosaur Bones and the Dinosaur Bone Bow

	/** Bow + 3 Dinosaur Bones + Netherite Ingot makes ONLY the bone bow; near-miss grids make nothing of ours. */
	@GameTest
	public void boneBowRecipe(GameTestHelper helper) {
		ItemStack e = ItemStack.EMPTY;
		ItemStack b = new ItemStack(ModItems.DINOSAUR_BONES);
		ItemStack w = new ItemStack(Items.BOW);
		ItemStack n = new ItemStack(Items.NETHERITE_INGOT);
		CraftingInput full = CraftingInput.of(3, 3, List.of(e, b, e, b, w, b, e, n, e));
		List<String> matches = craftingMatches(helper, full);
		helper.assertTrue(matches.equals(List.of("dinosaurs:dinosaur_bone_bow")), "bone bow grid matches " + matches);
		ItemStack out = ((CraftingRecipe) helper.getLevel().getServer().getRecipeManager()
				.byKey(ResourceKey.create(Registries.RECIPE, Dinosaurs.id("dinosaur_bone_bow"))).orElseThrow().value())
				.assemble(full);
		helper.assertTrue(out.is(ModItems.DINOSAUR_BONE_BOW) && out.getCount() == 1, "result " + out);
		// Missing netherite / vanilla bones instead of dinosaur bones: no match at all.
		helper.assertTrue(craftingMatches(helper, CraftingInput.of(3, 3, List.of(e, b, e, b, w, b, e, e, e))).isEmpty(), "no-netherite grid must not craft");
		ItemStack vb = new ItemStack(Items.BONE);
		helper.assertTrue(craftingMatches(helper, CraftingInput.of(3, 3, List.of(e, vb, e, vb, w, vb, e, n, e))).isEmpty(), "vanilla bones must not craft the bow");
		// Bone meal: 1 Dinosaur Bones -> 4 bone meal, the only match.
		CraftingInput meal = CraftingInput.of(1, 1, List.of(b));
		List<String> mealMatches = craftingMatches(helper, meal);
		helper.assertTrue(mealMatches.equals(List.of("dinosaurs:bone_meal_from_dinosaur_bones")), "bones alone match " + mealMatches);
		// Vanilla recipes keep working: one vanilla bone still makes 3 bone meal, sticks + string still a bow.
		helper.assertTrue(craftingMatches(helper, CraftingInput.of(1, 1, List.of(vb))).equals(List.of("minecraft:bone_meal")), "vanilla bone meal");
		ItemStack st = new ItemStack(Items.STICK);
		ItemStack str = new ItemStack(Items.STRING);
		helper.assertTrue(craftingMatches(helper, CraftingInput.of(3, 3, List.of(e, st, str, st, e, str, e, st, str))).equals(List.of("minecraft:bow")), "vanilla bow");
		helper.succeed();
	}

	/** Item properties, then a real shot: a 15-tick draw fires a full-power arrow at 1.5x vanilla speed. */
	@GameTest(maxTicks = 40)
	public void boneBowStats(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack bow = new ItemStack(ModItems.DINOSAUR_BONE_BOW);
		helper.assertTrue(bow.getMaxDamage() >= 1000, "durability " + bow.getMaxDamage());
		helper.assertTrue(bow.getMaxDamage() > new ItemStack(Items.BOW).getMaxDamage() * 2, "durability vs bow");
		helper.assertTrue(bow.has(DataComponents.DAMAGE_RESISTANT), "bone bow must be fire resistant");
		helper.assertTrue(bow.isValidRepairItem(new ItemStack(ModItems.DINOSAUR_BONES)), "repairable with dinosaur bones");
		helper.assertFalse(bow.isValidRepairItem(new ItemStack(Items.STICK)), "not repairable with sticks");
		helper.assertTrue(bow.is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE), "enchantable/bow tag");
		helper.assertTrue(bow.is(net.minecraft.tags.ItemTags.DURABILITY_ENCHANTABLE), "enchantable/durability tag");
		var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		for (var key : List.of(Enchantments.POWER, Enchantments.PUNCH, Enchantments.FLAME, Enchantments.INFINITY, Enchantments.UNBREAKING, Enchantments.MENDING)) {
			helper.assertTrue(enchants.getOrThrow(key).value().canEnchant(bow), "bow must accept " + key.identifier());
		}
		helper.assertTrue(DinosaurBoneBowItem.getPowerForTime(DinosaurBoneBowItem.DRAW_TICKS) >= 1.0F, "full power at 15 ticks");
		helper.assertTrue(net.minecraft.world.item.BowItem.getPowerForTime(DinosaurBoneBowItem.DRAW_TICKS) < 1.0F, "vanilla is not full at 15 ticks");

		double bone = shoot(helper, bow, 15);
		double vanilla = shoot(helper, new ItemStack(Items.BOW), 20);
		Dinosaurs.LOGGER.info("[test] arrow speed: bone bow (15 ticks) {}, vanilla bow (20 ticks) {}", bone, vanilla);
		helper.assertTrue(Math.abs(bone - DinosaurBoneBowItem.VELOCITY) < 0.15, "bone bow arrow speed " + bone);
		helper.assertTrue(Math.abs(vanilla - 3.0) < 0.15, "vanilla arrow speed " + vanilla);
		helper.assertTrue(bone / vanilla > 1.4, "bone bow must be ~1.5x faster: " + bone + " vs " + vanilla);
		helper.succeed();
	}

	private static double shoot(GameTestHelper helper, ItemStack bow, int ticksHeld) {
		ServerLevel level = helper.getLevel();
		Player player = survivalPlayer(helper);
		player.snapTo(helper.absoluteVec(new Vec3(2.5, 2, 2.5)), 0.0F, 0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, bow);
		player.getInventory().add(new ItemStack(Items.ARROW, 16));
		AABB box = new AABB(player.blockPosition()).inflate(8);
		level.getEntitiesOfClass(AbstractArrow.class, box).forEach(net.minecraft.world.entity.Entity::discard);
		bow.getItem().releaseUsing(bow, level, player, bow.getUseDuration(player) - ticksHeld);
		List<AbstractArrow> arrows = level.getEntitiesOfClass(AbstractArrow.class, box);
		helper.assertTrue(arrows.size() == 1, "expected one arrow, got " + arrows.size());
		AbstractArrow arrow = arrows.get(0);
		double speed = arrow.getDeltaMovement().length();
		helper.assertTrue(arrow.isCritArrow(), "full draw should give a critical arrow");
		arrow.discard();
		return speed;
	}

	private static ItemStack enchanted(GameTestHelper helper, Item item, net.minecraft.resources.ResourceKey<Enchantment> key) {
		ItemStack stack = new ItemStack(item);
		stack.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), 1);
		return stack;
	}

	private static ItemStack anvil(GameTestHelper helper, ItemStack left, ItemStack right) {
		Player player = survivalPlayer(helper);
		AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		return menu.getSlot(2).getItem().copy();
	}

	private static boolean hasBoth(GameTestHelper helper, ItemStack stack) {
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		return EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.INFINITY), stack) == 1
				&& EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.MENDING), stack) == 1;
	}

	/** Anvil: Infinity + Mending combine on the bone bow (both orders); vanilla bows still refuse. */
	@GameTest
	public void boneBowInfinityMendingAnvil(GameTestHelper helper) {
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> infinity = enchants.getOrThrow(Enchantments.INFINITY);
		Holder<Enchantment> mending = enchants.getOrThrow(Enchantments.MENDING);
		helper.assertFalse(Enchantment.areCompatible(infinity, mending), "outside the bone bow the vanilla rule must stay");

		ItemStack a = anvil(helper, enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.INFINITY), enchanted(helper, Items.ENCHANTED_BOOK, Enchantments.MENDING));
		helper.assertTrue(a.is(ModItems.DINOSAUR_BONE_BOW) && hasBoth(helper, a), "Infinity bone bow + Mending book: " + a + " " + a.getEnchantments());
		ItemStack b = anvil(helper, enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.MENDING), enchanted(helper, Items.ENCHANTED_BOOK, Enchantments.INFINITY));
		helper.assertTrue(b.is(ModItems.DINOSAUR_BONE_BOW) && hasBoth(helper, b), "Mending bone bow + Infinity book: " + b + " " + b.getEnchantments());
		ItemStack c = anvil(helper, enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.INFINITY), enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.MENDING));
		helper.assertTrue(hasBoth(helper, c), "two bone bows combine: " + c.getEnchantments());
		ItemStack v = anvil(helper, enchanted(helper, Items.BOW, Enchantments.INFINITY), enchanted(helper, Items.ENCHANTED_BOOK, Enchantments.MENDING));
		helper.assertFalse(hasBoth(helper, v), "vanilla bow must not get both: " + v.getEnchantments());
		helper.assertFalse(Enchantment.areCompatible(infinity, mending), "the anvil must not leak the exception");
		helper.succeed();
	}

	/** /enchant: Mending on an Infinity bone bow works, on an Infinity vanilla bow it fails. */
	@GameTest(maxTicks = 40)
	public void boneBowInfinityMendingCommand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ArmorStand boneStand = helper.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
		boneStand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.INFINITY));
		boneStand.addTag("dino_bone_bow_test");
		ArmorStand plainStand = helper.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new BlockPos(3, 1, 1));
		plainStand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, enchanted(helper, Items.BOW, Enchantments.INFINITY));
		plainStand.addTag("dino_plain_bow_test");
		var server = level.getServer();
		var source = server.createCommandSourceStack().withLevel(level).withPosition(boneStand.position()).withSuppressedOutput();
		server.getCommands().performPrefixedCommand(source, "enchant @e[tag=dino_bone_bow_test,limit=1] minecraft:mending");
		server.getCommands().performPrefixedCommand(source, "enchant @e[tag=dino_plain_bow_test,limit=1] minecraft:mending");
		ItemStack bone = boneStand.getMainHandItem();
		ItemStack plain = plainStand.getMainHandItem();
		helper.assertTrue(hasBoth(helper, bone), "/enchant bone bow: " + bone.getEnchantments());
		helper.assertFalse(hasBoth(helper, plain), "/enchant vanilla bow must fail: " + plain.getEnchantments());
		var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		helper.assertFalse(Enchantment.areCompatible(enchants.getOrThrow(Enchantments.INFINITY), enchants.getOrThrow(Enchantments.MENDING)), "/enchant must not leak the exception");
		boneStand.discard();
		plainStand.discard();
		helper.succeed();
	}

	/** Large dinosaurs drop Dinosaur Bones instead of bones; Velociraptor, Pteranodon and Megalodon keep vanilla bones. */
	@GameTest(maxTicks = 100)
	public void dinosaurBonesLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Player player = survivalPlayer(helper);
		Map<EntityType<? extends Mob>, int[]> expected = new java.util.LinkedHashMap<>();
		expected.put(ModEntities.T_REX, new int[]{2, 4});
		expected.put(ModEntities.SPINOSAURUS, new int[]{2, 4});
		expected.put(ModEntities.BRACHIOSAURUS, new int[]{2, 5});
		expected.put(ModEntities.TRICERATOPS, new int[]{1, 3});
		expected.put(ModEntities.STEGOSAURUS, new int[]{1, 3});
		expected.put(ModEntities.ANKYLOSAURUS, new int[]{1, 3});
		expected.put(ModEntities.PARASAUROLOPHUS, new int[]{1, 2});
		StringBuilder log = new StringBuilder();
		int x = 1;
		for (EntityType<? extends Mob> type : ModEntities.ALL) {
			var table = level.getServer().reloadableRegistries().getLootTable(type.getDefaultLootTable().orElseThrow());
			Mob mob = helper.spawn(type, new BlockPos(x, 1, 4));
			int rolls = 300;
			int min = Integer.MAX_VALUE, max = 0, vanilla = 0;
			for (int i = 0; i < rolls; i++) {
				int count = 0;
				for (ItemStack stack : table.getRandomItems(lootParams(level, mob, player))) {
					if (stack.is(ModItems.DINOSAUR_BONES)) count += stack.getCount();
					if (stack.is(Items.BONE)) vanilla += stack.getCount();
				}
				min = Math.min(min, count);
				max = Math.max(max, count);
			}
			String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
			log.append(' ').append(name).append(' ').append(min).append('-').append(max).append(" vanilla=").append(vanilla);
			int[] range = expected.get(type);
			if (range != null) {
				helper.assertTrue(min == range[0] && max == range[1], name + " dinosaur bones " + min + "-" + max + ", expected " + range[0] + "-" + range[1]);
				helper.assertTrue(vanilla == 0, name + " must not drop vanilla bones any more, got " + vanilla);
			} else {
				helper.assertTrue(max == 0, name + " must not drop dinosaur bones, got up to " + max);
			}
			mob.discard();
			x += 1;
		}
		Dinosaurs.LOGGER.info("[test] dinosaur bones per kill (no Looting):{}", log);
		helper.succeed();
	}

	private static net.minecraft.world.level.storage.loot.LootParams lootParams(ServerLevel level, Mob mob, Player player) {
		return new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, mob)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, mob.position())
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
				.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ATTACKING_ENTITY, player)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, player)
				.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
	}
}
