package net.vsh33.dinosaurs.test;

import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.entity.ModEntities;
import net.vsh33.dinosaurs.entity.Triceratops;
import net.vsh33.dinosaurs.entity.HostileDino;
import net.minecraft.world.InteractionResult;
import net.vsh33.dinosaurs.item.ModItems;
import net.vsh33.dinosaurs.item.DinosaurBoneBowItem;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import java.util.HashMap;
import java.util.Map;

/** Server game tests (./gradlew runGameTest): amulet logic, saddle/riding logic, recipes and advancements. */
public class DinosaursServerTest implements FabricGameTest {
	// ---- 1.20.1 stand-ins for the 1.21 recipe inputs
	private static final net.minecraft.world.inventory.AbstractContainerMenu DUMMY_MENU = new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
		@Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
		@Override public boolean stillValid(Player player) { return true; }
	};

	static CraftingContainer craftInput(int w, int h, List<ItemStack> items) {
		CraftingContainer c = new TransientCraftingContainer(DUMMY_MENU, w, h);
		for (int i = 0; i < items.size(); i++) c.setItem(i, items.get(i));
		return c;
	}

	static Container smithInput(ItemStack template, ItemStack base, ItemStack addition) {
		return new SimpleContainer(template, base, addition);
	}

	/** Survival mock player (the in-level mock player is creative, which no mob ever targets). */
	static Player survivalPlayer(GameTestHelper helper) {
		helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
		// 1.20.1's makeMockPlayer() is always creative; this is the same mock, in survival.
		return new Player(helper.getLevel(), BlockPos.ZERO, 0.0F, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-mock-player")) {
			@Override public boolean isSpectator() { return false; }
			@Override public boolean isCreative() { return false; }
			@Override public boolean isLocalPlayer() { return true; }
		};
	}

	private static void checkAmulet(GameTestHelper helper, Mob dino) {
		Player player = survivalPlayer(helper);
		player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
		helper.assertTrue(dino.canAttack(player), "dino should be able to attack a player without the amulet");
		player.getInventory().add(new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION));
		helper.assertFalse(dino.canAttack(player), "dino must not attack a player carrying the amulet");
		dino.setTarget(player);
		helper.runAfterDelay(12, () -> {
			helper.assertTrue(dino.getTarget() == null, "protected player must be dropped as target, got " + dino.getTarget());
			dino.hurt(player.damageSources().playerAttack(player), 1.0F);
			helper.assertTrue(dino.canAttack(player), "after being attacked the dino may retaliate");
			helper.succeed();
		});
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 60)
	public void raptorAmulet(GameTestHelper helper) {
		checkAmulet(helper, helper.spawn(ModEntities.VELOCIRAPTOR, new BlockPos(4, 1, 4)));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 60)
	public void tRexAmulet(GameTestHelper helper) {
		checkAmulet(helper, helper.spawn(ModEntities.T_REX, new BlockPos(4, 1, 4)));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 60)
	public void megalodonAmulet(GameTestHelper helper) {
		checkAmulet(helper, helper.spawn(ModEntities.MEGALODON, new BlockPos(4, 1, 4)));
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
	public void saddleAndRide(GameTestHelper helper) {
		Player player = survivalPlayer(helper);
		player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
		Triceratops trike = helper.spawn(ModEntities.TRICERATOPS, new BlockPos(4, 1, 4));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DINOSAUR_SADDLE));
		trike.interact(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(trike.isSaddled(), "saddle should be applied");
		helper.assertTrue(player.getMainHandItem().isEmpty(), "saddle item should be consumed");
		trike.interact(player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getVehicle() == trike, "player should be riding");
		helper.assertTrue(trike.getControllingPassenger() == player, "rider should control the triceratops");
		helper.succeed();
	}

	/** Only meaningful with Trinkets installed: ./gradlew runGameTest -PwithTrinkets=true */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
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
			player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
			Mob raptor = helper.spawn(ModEntities.VELOCIRAPTOR, new BlockPos(4, 1, 4));
			ItemStack amulet = new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION);
			helper.assertTrue(amulet.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("trinkets", "chest/necklace"))), "amulet must be tagged #trinkets:chest/necklace");
			var groups = TrinketsApi.getTrinketComponent(player).orElseThrow().getInventory();
			helper.assertTrue(groups.containsKey("chest") && groups.get("chest").containsKey("necklace"), "player needs a chest/necklace slot, got " + groups.keySet());
			TrinketInventory necklace = groups.get("chest").get("necklace");
			necklace.setItem(0, amulet);
			helper.assertTrue(player.getInventory().isEmpty(), "main inventory should be empty");
			helper.assertFalse(raptor.canAttack(player), "amulet in the necklace slot must protect the player");
			Dinosaurs.LOGGER.info("[test] Trinkets necklace slot OK");
			helper.succeed();
		}
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void configSpawnToggles(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos rel = new BlockPos(3, 2, 3);
		helper.setBlock(rel.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(rel, Blocks.AIR);
		BlockPos pos = helper.absolutePos(rel);
		DinoConfig config = DinoConfig.get();
		boolean before = SpawnPlacements.checkSpawnRules(ModEntities.TRICERATOPS, level, MobSpawnType.NATURAL, pos, level.getRandom());
		config.setSpawnEnabled(ModEntities.TRICERATOPS, false);
		boolean disabled = SpawnPlacements.checkSpawnRules(ModEntities.TRICERATOPS, level, MobSpawnType.NATURAL, pos, level.getRandom());
		boolean egg = SpawnPlacements.checkSpawnRules(ModEntities.TRICERATOPS, level, MobSpawnType.SPAWN_EGG, pos, level.getRandom());
		config.setSpawnEnabled(ModEntities.TRICERATOPS, true);
		Dinosaurs.LOGGER.info("[test] triceratops natural spawn: enabled={} disabled={} spawnEgg(disabled)={}", before, disabled, egg);
		helper.assertTrue(before, "baseline natural spawn should be allowed on grass in daylight");
		helper.assertFalse(disabled, "disabled species must not spawn naturally");
		helper.assertTrue(egg, "spawn eggs must ignore the config");
		helper.succeed();
	}

	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void recipesAndAdvancements(GameTestHelper helper) {
		for (String id : new String[]{"dino_collar", "amulet_of_dino_protection", "dinosaur_saddle", "leather_from_brachiosaurus_hide", "t_rex_skull_helmet", "dinosaur_bone_bow", "bone_meal_from_dinosaur_bones"}) {
			helper.assertTrue(helper.getLevel().getRecipeManager().byKey(Dinosaurs.id(id)).isPresent(), "missing recipe " + id);
		}
		for (String id : new String[]{"amulet", "ride_triceratops", "recipes/equipment/dinosaur_saddle", "recipes/equipment/amulet_of_dino_protection", "recipes/misc/leather_from_brachiosaurus_hide", "recipes/equipment/dinosaur_bone_bow", "recipes/misc/bone_meal_from_dinosaur_bones"}) {
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().getAdvancement(Dinosaurs.id(id)) != null, "missing advancement " + id);
		}
		helper.succeed();
	}

	private static List<String> craftingMatches(GameTestHelper helper, CraftingContainer input) {
		var level = helper.getLevel();
		List<String> matches = new ArrayList<>();
		for (Recipe<?> holder : level.getServer().getRecipeManager().getRecipes()) {
			if (holder.getType() != RecipeType.CRAFTING) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Recipe<CraftingContainer> recipe = (Recipe<CraftingContainer>) holder;
			if (recipe.matches(input, level)) {
				matches.add(holder.getId().toString());
			}
		}
		return matches;
	}

	/**
	 * Guide book: Book + Leather + any dinosaur trophy is the ONLY recipe for that grid; Book + Leather alone
	 * (left free for other mods' guide books) and Book + Leather + Bone do not make it.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
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
				.byKey(Dinosaurs.id("guide_book")).orElseThrow();
		for (Item trophy : expected) {
			helper.assertTrue(new ItemStack(trophy).is(trophies), trophy + " must be a guide book trophy");
			CraftingContainer input = craftInput(3, 1, List.of(new ItemStack(trophy), new ItemStack(Items.BOOK), new ItemStack(Items.LEATHER)));
			List<String> matches = craftingMatches(helper, input);
			helper.assertTrue(matches.equals(List.of("dinosaurs:guide_book")), "book + leather + " + trophy + " matches " + matches);
			ItemStack out = recipe.assemble(input, net.minecraft.core.RegistryAccess.EMPTY);
			helper.assertTrue(out.is(ModItems.GUIDE_BOOK) && out.getCount() == 1, "should craft one guide book, got " + out);
		}
		List<String> plain = craftingMatches(helper, craftInput(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.LEATHER))));
		helper.assertTrue(plain.isEmpty(), "book + leather must stay free, matches " + plain);
		List<String> bone = craftingMatches(helper, craftInput(3, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.LEATHER), new ItemStack(Items.BONE))));
		helper.assertTrue(bone.isEmpty(), "book + leather + bone must not match, got " + bone);
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().getAdvancement(Dinosaurs.id("recipes/misc/guide_book")) != null, "missing guide book recipe advancement");
		Dinosaurs.LOGGER.info("[test] guide book: {} trophies, no conflicts, book + leather left free", expected.length);
		helper.succeed();
	}

	/** T-Rex Skull + Leather + Netherite Ingot (any order) makes the helmet, and nothing else uses that grid. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void skullHelmetRecipe(GameTestHelper helper) {
		CraftingRecipe recipe = (CraftingRecipe) helper.getLevel().getServer().getRecipeManager()
				.byKey(Dinosaurs.id("t_rex_skull_helmet")).orElseThrow();
		List<List<ItemStack>> orders = List.of(
				List.of(new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.LEATHER), new ItemStack(Items.NETHERITE_INGOT)),
				List.of(new ItemStack(Items.NETHERITE_INGOT), new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.LEATHER)));
		for (List<ItemStack> items : orders) {
			CraftingContainer input = craftInput(3, 1, items);
			List<String> matches = craftingMatches(helper, input);
			helper.assertTrue(matches.equals(List.of("dinosaurs:t_rex_skull_helmet")), "skull + leather + netherite matches " + matches);
			ItemStack out = recipe.assemble(input, net.minecraft.core.RegistryAccess.EMPTY);
			helper.assertTrue(out.is(ModItems.T_REX_SKULL_HELMET) && out.getCount() == 1, "should craft one helmet, got " + out);
		}
		CraftingContainer tall = craftInput(1, 3, List.of(new ItemStack(Items.LEATHER), new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.NETHERITE_INGOT)));
		helper.assertTrue(craftingMatches(helper, tall).equals(List.of("dinosaurs:t_rex_skull_helmet")), "shapeless recipe should work in a column too");
		for (List<ItemStack> partial : List.of(
				List.of(new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.LEATHER)),
				List.of(new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.NETHERITE_INGOT)),
				List.of(new ItemStack(ModItems.T_REX_SKULL), new ItemStack(Items.BOOK), new ItemStack(Items.LEATHER)))) {
			List<String> matches = craftingMatches(helper, craftInput(partial.size(), 1, partial));
			helper.assertTrue(matches.isEmpty(), partial + " must not craft anything, got " + matches);
		}
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().getAdvancement(Dinosaurs.id("recipes/equipment/t_rex_skull_helmet")) != null,
				"missing helmet recipe advancement");
		helper.succeed();
	}

	/** Above netherite in every stat, fire resistant, a head item, repairable with netherite or a skull, and wearable. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
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
		helper.assertTrue(helmet.getItem().getEnchantmentValue()
				>= netherite.getItem().getEnchantmentValue(), "enchantability at least netherite");
		helper.assertTrue(helmet.getItem().isFireResistant(), "helmet must be fire resistant");
		helper.assertTrue(Enchantments.RESPIRATION.canEnchant(helmet) && Enchantments.ALL_DAMAGE_PROTECTION.canEnchant(helmet), "helmet must take helmet enchantments (HelmetEnchantMixin)");
		helper.assertTrue(helmet.getItem().isValidRepairItem(helmet, new ItemStack(Items.NETHERITE_INGOT))
				&& helmet.getItem().isValidRepairItem(helmet, new ItemStack(ModItems.T_REX_SKULL)), "repair with netherite ingot or T-Rex skull");
		helper.assertTrue(helmet.getItem() instanceof net.minecraft.world.item.Equipable e && e.getEquipmentSlot() == net.minecraft.world.entity.EquipmentSlot.HEAD
				&& !(helmet.getItem() instanceof net.minecraft.world.item.ArmorItem), "head slot, no flat armor layer (3D model is drawn instead)");
		Player player = survivalPlayer(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, helmet.copy());
		player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(ModItems.T_REX_SKULL_HELMET), "right-click should put it on");
		// Attribute modifiers from equipment are applied on the wearer's next tick (the mock player is not ticked).
		var stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 1, 2));
		stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet.copy());
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(stand.getArmorValue() == (int) ours[0], "worn helmet should give " + ours[0] + " armor, got " + stand.getArmorValue());
			helper.succeed();
		});
	}

	private static double[] headStats(ItemStack stack) {
		double[] stats = new double[3];
		for (var __e : stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.HEAD).entries()) {
			var attribute = __e.getKey();
			var modifier = __e.getValue();
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)) stats[0] += modifier.getAmount();
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)) stats[1] += modifier.getAmount();
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) stats[2] += modifier.getAmount();
		}
		return stats;
	}

	/**
	 * T. rex loot table, rolled 4000 times: the skull drops only when a player killed it, about 4% of the time,
	 * about 10% with Looting III; the tooth still drops every time.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
	public void tRexSkullLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var table = level.getServer().getLootData().getLootTable(
				Dinosaurs.id("entities/t_rex"));
		Mob rex = helper.spawn(ModEntities.T_REX, new BlockPos(4, 1, 4));
		Player player = survivalPlayer(helper);
		int rolls = 4000;
		int noPlayer = skulls(table, level, rex, null, rolls);
		int plain = skulls(table, level, rex, player, rolls);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING, 3);
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
				.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.KILLER_ENTITY, player);
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
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void boneBowRecipe(GameTestHelper helper) {
		ItemStack e = ItemStack.EMPTY;
		ItemStack b = new ItemStack(ModItems.DINOSAUR_BONES);
		ItemStack w = new ItemStack(Items.BOW);
		ItemStack n = new ItemStack(Items.NETHERITE_INGOT);
		CraftingContainer full = craftInput(3, 3, List.of(e, b, e, b, w, b, e, n, e));
		List<String> matches = craftingMatches(helper, full);
		helper.assertTrue(matches.equals(List.of("dinosaurs:dinosaur_bone_bow")), "bone bow grid matches " + matches);
		ItemStack out = ((CraftingRecipe) helper.getLevel().getServer().getRecipeManager()
				.byKey(Dinosaurs.id("dinosaur_bone_bow")).orElseThrow())
				.assemble(full, net.minecraft.core.RegistryAccess.EMPTY);
		helper.assertTrue(out.is(ModItems.DINOSAUR_BONE_BOW) && out.getCount() == 1, "result " + out);
		// Missing netherite / vanilla bones instead of dinosaur bones: no match at all.
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(e, b, e, b, w, b, e, e, e))).isEmpty(), "no-netherite grid must not craft");
		ItemStack vb = new ItemStack(Items.BONE);
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(e, vb, e, vb, w, vb, e, n, e))).isEmpty(), "vanilla bones must not craft the bow");
		// Bone meal: 1 Dinosaur Bones -> 4 bone meal, the only match.
		CraftingContainer meal = craftInput(1, 1, List.of(b));
		List<String> mealMatches = craftingMatches(helper, meal);
		helper.assertTrue(mealMatches.equals(List.of("dinosaurs:bone_meal_from_dinosaur_bones")), "bones alone match " + mealMatches);
		// Vanilla recipes keep working: one vanilla bone still makes 3 bone meal, sticks + string still a bow.
		helper.assertTrue(craftingMatches(helper, craftInput(1, 1, List.of(vb))).equals(List.of("minecraft:bone_meal")), "vanilla bone meal");
		ItemStack st = new ItemStack(Items.STICK);
		ItemStack str = new ItemStack(Items.STRING);
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(e, st, str, st, e, str, e, st, str))).equals(List.of("minecraft:bow")), "vanilla bow");
		helper.succeed();
	}

	/** Item properties, then a real shot: a 15-tick draw fires a full-power arrow at 1.5x vanilla speed. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
	public void boneBowStats(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack bow = new ItemStack(ModItems.DINOSAUR_BONE_BOW);
		helper.assertTrue(bow.getMaxDamage() >= 1000, "durability " + bow.getMaxDamage());
		helper.assertTrue(bow.getMaxDamage() > new ItemStack(Items.BOW).getMaxDamage() * 2, "durability vs bow");
		helper.assertTrue(bow.getItem().isFireResistant(), "bone bow must be fire resistant");
		helper.assertTrue(bow.getItem().isValidRepairItem(bow, new ItemStack(ModItems.DINOSAUR_BONES)), "repairable with dinosaur bones");
		helper.assertFalse(bow.getItem().isValidRepairItem(bow, new ItemStack(Items.STICK)), "not repairable with sticks");
		var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		for (var key : List.of(Enchantments.POWER_ARROWS, Enchantments.PUNCH_ARROWS, Enchantments.FLAMING_ARROWS, Enchantments.INFINITY_ARROWS, Enchantments.UNBREAKING, Enchantments.MENDING)) {
			helper.assertTrue(key.canEnchant(bow), "bow must accept " + BuiltInRegistries.ENCHANTMENT.getKey(key));
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
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
		player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, bow);
		player.getInventory().add(new ItemStack(Items.ARROW, 16));
		AABB box = new AABB(player.blockPosition()).inflate(8);
		level.getEntitiesOfClass(AbstractArrow.class, box).forEach(net.minecraft.world.entity.Entity::discard);
		bow.getItem().releaseUsing(bow, level, player, bow.getUseDuration() - ticksHeld);
		List<AbstractArrow> arrows = level.getEntitiesOfClass(AbstractArrow.class, box);
		helper.assertTrue(arrows.size() == 1, "expected one arrow, got " + arrows.size());
		AbstractArrow arrow = arrows.get(0);
		double speed = arrow.getDeltaMovement().length();
		helper.assertTrue(arrow.isCritArrow(), "full draw should give a critical arrow");
		arrow.discard();
		return speed;
	}

	private static ItemStack enchanted(GameTestHelper helper, Item item, Enchantment key) {
		ItemStack stack = new ItemStack(item);
		stack.enchant(key, 1);
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
		return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) == 1
				&& EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, stack) == 1;
	}

	/** Anvil: Infinity + Mending combine on the bone bow (both orders); vanilla bows still refuse. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void boneBowInfinityMendingAnvil(GameTestHelper helper) {
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Enchantment infinity = Enchantments.INFINITY_ARROWS;
		Enchantment mending = Enchantments.MENDING;
		helper.assertFalse(infinity.isCompatibleWith(mending), "outside the bone bow the vanilla rule must stay");

		ItemStack a = anvil(helper, enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.INFINITY_ARROWS), enchanted(helper, Items.ENCHANTED_BOOK, Enchantments.MENDING));
		helper.assertTrue(a.is(ModItems.DINOSAUR_BONE_BOW) && hasBoth(helper, a), "Infinity bone bow + Mending book: " + a + " " + EnchantmentHelper.getEnchantments(a));
		ItemStack b = anvil(helper, enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.MENDING), enchanted(helper, Items.ENCHANTED_BOOK, Enchantments.INFINITY_ARROWS));
		helper.assertTrue(b.is(ModItems.DINOSAUR_BONE_BOW) && hasBoth(helper, b), "Mending bone bow + Infinity book: " + b + " " + EnchantmentHelper.getEnchantments(b));
		ItemStack c = anvil(helper, enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.INFINITY_ARROWS), enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.MENDING));
		helper.assertTrue(hasBoth(helper, c), "two bone bows combine: " + EnchantmentHelper.getEnchantments(c));
		ItemStack v = anvil(helper, enchanted(helper, Items.BOW, Enchantments.INFINITY_ARROWS), enchanted(helper, Items.ENCHANTED_BOOK, Enchantments.MENDING));
		helper.assertFalse(hasBoth(helper, v), "vanilla bow must not get both: " + EnchantmentHelper.getEnchantments(v));
		helper.assertFalse(infinity.isCompatibleWith(mending), "the anvil must not leak the exception");
		helper.succeed();
	}

	/** /enchant: Mending on an Infinity bone bow works, on an Infinity vanilla bow it fails. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
	public void boneBowInfinityMendingCommand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ArmorStand boneStand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
		boneStand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, enchanted(helper, ModItems.DINOSAUR_BONE_BOW, Enchantments.INFINITY_ARROWS));
		boneStand.addTag("dino_bone_bow_test");
		ArmorStand plainStand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(3, 1, 1));
		plainStand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, enchanted(helper, Items.BOW, Enchantments.INFINITY_ARROWS));
		plainStand.addTag("dino_plain_bow_test");
		var server = level.getServer();
		var source = server.createCommandSourceStack().withLevel(level).withPosition(boneStand.position()).withSuppressedOutput();
		server.getCommands().performPrefixedCommand(source, "enchant @e[tag=dino_bone_bow_test,limit=1] minecraft:mending");
		server.getCommands().performPrefixedCommand(source, "enchant @e[tag=dino_plain_bow_test,limit=1] minecraft:mending");
		ItemStack bone = boneStand.getMainHandItem();
		ItemStack plain = plainStand.getMainHandItem();
		helper.assertTrue(hasBoth(helper, bone), "/enchant bone bow: " + EnchantmentHelper.getEnchantments(bone));
		helper.assertFalse(hasBoth(helper, plain), "/enchant vanilla bow must fail: " + EnchantmentHelper.getEnchantments(plain));
		var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		helper.assertFalse(Enchantments.INFINITY_ARROWS.isCompatibleWith(Enchantments.MENDING), "/enchant must not leak the exception");
		boneStand.discard();
		plainStand.discard();
		helper.succeed();
	}

	/** Large dinosaurs drop Dinosaur Bones instead of bones; Velociraptor, Pteranodon and Megalodon keep vanilla bones. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
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
			var table = level.getServer().getLootData().getLootTable(type.getDefaultLootTable());
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
				.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.KILLER_ENTITY, player)
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.LAST_DAMAGE_PLAYER, player)
				.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
	}

	/** 1.4.1: crafting the bone bow from an enchanted, named, damaged bow keeps its data; a plain bow gives a plain bone bow. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void boneBowKeepsEnchantments(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		ItemStack bow = new ItemStack(Items.BOW);
		bow.enchant(Enchantments.POWER_ARROWS, 5);
		bow.enchant(Enchantments.INFINITY_ARROWS, 1);
		bow.enchant(Enchantments.UNBREAKING, 3);
		bow.setHoverName(net.minecraft.network.chat.Component.literal("Old Faithful"));
		bow.setRepairCost(5);
		bow.setDamageValue(100);

		ItemStack out = craftBoneBow(helper, bow);
		helper.assertTrue(out.is(ModItems.DINOSAUR_BONE_BOW), "result " + out);
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, out) == 5, "Power V kept: " + EnchantmentHelper.getEnchantments(out));
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, out) == 1, "Infinity kept: " + EnchantmentHelper.getEnchantments(out));
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, out) == 3, "Unbreaking III kept: " + EnchantmentHelper.getEnchantments(out));
		helper.assertTrue(EnchantmentHelper.getEnchantments(out).size() == 3, "exactly 3 enchantments: " + EnchantmentHelper.getEnchantments(out));
		helper.assertTrue("Old Faithful".equals(out.getHoverName().getString()), "name kept: " + out.getHoverName().getString());
		helper.assertTrue(out.getBaseRepairCost() == 5, "repair cost kept: " + out.getBaseRepairCost());
		helper.assertTrue(out.getMaxDamage() == 1152, "max durability is the bone bow's: " + out.getMaxDamage());
		// 100/384 damaged -> 300/1152: the same fraction of durability, never a free repair.
		helper.assertTrue(out.getDamageValue() == 300, "damage scaled 100/384 -> 300/1152, got " + out.getDamageValue());
		helper.assertTrue(out.getItem().isFireResistant(), "still fire resistant");
		helper.assertTrue(bow.getDamageValue() == 100 && bow.is(Items.BOW), "input stack untouched");

		ItemStack plain = craftBoneBow(helper, new ItemStack(Items.BOW));
		helper.assertTrue(ItemStack.isSameItemSameTags(plain, new ItemStack(ModItems.DINOSAUR_BONE_BOW)), "plain bow -> plain bone bow, got " + plain.getTag());

		ItemStack nearlyBroken = new ItemStack(Items.BOW);
		nearlyBroken.setDamageValue(383);
		ItemStack worn = craftBoneBow(helper, nearlyBroken);
		helper.assertTrue(worn.getDamageValue() == 1149, "1/384 left -> 3/1152 left: " + worn.getDamageValue());
		Dinosaurs.LOGGER.info("[test] bone bow from enchanted bow: {} {} damage {}/{}", out.getHoverName().getString(), EnchantmentHelper.getEnchantments(out), out.getDamageValue(), out.getMaxDamage());
		helper.succeed();
	}

	// ---- 1.5.0: Dinosaur Armor

	private static final Item[][] ARMOR = {
			{ModItems.DINOSAUR_CHESTPLATE, Items.NETHERITE_CHESTPLATE, ModItems.MEGALODON_TOOTH, Items.DIAMOND_CHESTPLATE},
			{ModItems.DINOSAUR_LEGGINGS, Items.NETHERITE_LEGGINGS, ModItems.PARASAUROLOPHUS_CREST, Items.DIAMOND_LEGGINGS},
			{ModItems.DINOSAUR_BOOTS, Items.NETHERITE_BOOTS, ModItems.RAPTOR_CLAW, Items.DIAMOND_BOOTS}};

	private static net.minecraft.world.entity.EquipmentSlot slotOf(Item item) {
		return ((net.minecraft.world.item.Equipable) item).getEquipmentSlot();
	}

	/** armor, toughness, knockback resistance of one piece in its own slot. */
	private static double[] pieceStats(ItemStack stack) {
		double[] stats = new double[3];
		for (var __e : stack.getAttributeModifiers(slotOf(stack.getItem())).entries()) {
			var attribute = __e.getKey();
			var modifier = __e.getValue();
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)) stats[0] += modifier.getAmount();
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)) stats[1] += modifier.getAmount();
			if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) stats[2] += modifier.getAmount();
		}
		return stats;
	}

	private static int enchantability(ItemStack stack) {
		return stack.getItem().getEnchantmentValue();
	}

	/**
	 * Each piece is above netherite by exactly what netherite is above diamond (toughness, knockback resistance,
	 * durability multiplier, enchantability); armor points 10 / 8 / 4 (netherite 8 / 6 / 3).
	 * Fire resistant, armor tags (enchantments, trims), repaired with Dinosaur Bones, worn with the dinosaur armor layer.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
	public void dinosaurArmorStats(GameTestHelper helper) {
		StringBuilder log = new StringBuilder();
		for (Item[] row : ARMOR) {
			ItemStack ours = new ItemStack(row[0]), neth = new ItemStack(row[1]), dia = new ItemStack(row[3]);
			double[] o = pieceStats(ours), n = pieceStats(neth), d = pieceStats(dia);
			double armor = row[0] == ModItems.DINOSAUR_CHESTPLATE ? 10 : row[0] == ModItems.DINOSAUR_LEGGINGS ? 8 : 4;
			helper.assertTrue(o[0] == armor && o[0] > n[0], row[0] + " armor " + o[0] + ", expected " + armor + " (netherite " + n[0] + ")");
			for (int i = 1; i < 3; i++) {
				helper.assertTrue(Math.abs(o[i] - (2 * n[i] - d[i])) < 1e-4, row[0] + " stat " + i + ": " + o[i] + ", expected " + (2 * n[i] - d[i]));
			}
			helper.assertTrue(o[1] > n[1] && o[2] > n[2], row[0] + " toughness and knockback resistance must beat netherite");
			int slotMul = ours.getMaxDamage() / 41;
			helper.assertTrue(ours.getMaxDamage() == slotMul * 41 && neth.getMaxDamage() == slotMul * 37 && dia.getMaxDamage() == slotMul * 33,
					row[0] + " durability " + ours.getMaxDamage() + " should be 41/37/33 x " + slotMul);
			helper.assertTrue(enchantability(ours) - enchantability(neth) == enchantability(neth) - enchantability(dia),
					row[0] + " enchantability " + enchantability(ours));
			helper.assertTrue(ours.getItem().isFireResistant(), row[0] + " must be fire resistant");
			helper.assertTrue(ours.is(net.minecraft.tags.ItemTags.TRIMMABLE_ARMOR), row[0] + " must be trimmable armor");
			helper.assertTrue(ours.getItem().isValidRepairItem(ours, new ItemStack(ModItems.DINOSAUR_BONES))
					&& !ours.getItem().isValidRepairItem(ours, new ItemStack(Items.NETHERITE_INGOT)), row[0] + " repaired with dinosaur bones");
			helper.assertTrue(slotOf(row[0]) == slotOf(row[1]) && ((net.minecraft.world.item.ArmorItem) row[0]).getMaterial().getName().equals("dinosaurs_dinosaur"),
					row[0] + " worn layer minecraft:textures/models/armor/dinosaurs_dinosaur_layer_N.png in " + slotOf(row[1]));
			log.append(String.format(" %s %.0f/%.1f/%.1f %d ench %d (netherite %.0f/%.1f/%.1f %d %d, diamond %.0f/%.1f/%.1f %d %d)",
					BuiltInRegistries.ITEM.getKey(row[0]).getPath(), o[0], o[1], o[2], ours.getMaxDamage(), enchantability(ours),
					n[0], n[1], n[2], neth.getMaxDamage(), enchantability(neth), d[0], d[1], d[2], dia.getMaxDamage(), enchantability(dia)));
		}
		helper.assertTrue(Enchantments.ALL_DAMAGE_PROTECTION.canEnchant(new ItemStack(ModItems.DINOSAUR_CHESTPLATE))
				&& Enchantments.SWIFT_SNEAK.canEnchant(new ItemStack(ModItems.DINOSAUR_LEGGINGS))
				&& Enchantments.FALL_PROTECTION.canEnchant(new ItemStack(ModItems.DINOSAUR_BOOTS)), "armor enchantments");
		// The set's helmet (T-Rex Skull Helmet) is at least the new tier in every stat.
		ItemStack helmet = new ItemStack(ModItems.T_REX_SKULL_HELMET);
		double[] h = headStats(helmet);
		helper.assertTrue(h[0] >= 3 && h[1] >= 4 && h[2] >= 0.2 - 1e-6 && helmet.getMaxDamage() >= 11 * 41 && enchantability(helmet) >= 20,
				"skull helmet should match the dinosaur tier: " + h[0] + "/" + h[1] + "/" + h[2] + " " + helmet.getMaxDamage() + " " + enchantability(helmet));
		Dinosaurs.LOGGER.info("[test] dinosaur armor:{}", log);
		// Full set worn by an armor stand: 3 + 8 + 6 + 3... armor stand with skull helmet + the three pieces.
		var stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(2, 1, 2));
		stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet.copy());
		for (Item[] row : ARMOR) {
			stand.setItemSlot(slotOf(row[0]), new ItemStack(row[0]));
		}
		helper.runAfterDelay(3, () -> {
			double toughness = stand.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
			double knockback = stand.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
			Dinosaurs.LOGGER.info("[test] full set (skull helmet + dinosaur armor): armor {} toughness {} knockback {}", stand.getArmorValue(), toughness, knockback);
			helper.assertTrue(stand.getArmorValue() == 26, "full set armor 4 + 10 + 8 + 4 = 26, got " + stand.getArmorValue());
			helper.assertTrue(Math.abs(toughness - 16.0) < 1e-4, "full set toughness 16, got " + toughness);
			helper.succeed();
		});
	}

	/** Template recipes: Netherite Upgrade + 8 Dinosaur Bones, and the duplication (2 out); nothing else uses those grids. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void dinosaurUpgradeRecipes(GameTestHelper helper) {
		ItemStack b = new ItemStack(ModItems.DINOSAUR_BONES);
		ItemStack nt = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
		ItemStack dt = new ItemStack(ModItems.DINOSAUR_UPGRADE_SMITHING_TEMPLATE);
		CraftingContainer make = craftInput(3, 3, List.of(b, b, b, b, nt, b, b, b, b));
		List<String> matches = craftingMatches(helper, make);
		helper.assertTrue(matches.equals(List.of("dinosaurs:dinosaur_upgrade_smithing_template")), "template grid matches " + matches);
		var level = helper.getLevel();
		ItemStack out = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, make, level).orElseThrow().assemble(make, net.minecraft.core.RegistryAccess.EMPTY);
		helper.assertTrue(out.is(ModItems.DINOSAUR_UPGRADE_SMITHING_TEMPLATE) && out.getCount() == 1, "one template, got " + out);
		CraftingContainer copy = craftInput(3, 3, List.of(b, dt, b, b, new ItemStack(Items.BONE_BLOCK), b, b, b, b));
		matches = craftingMatches(helper, copy);
		helper.assertTrue(matches.equals(List.of("dinosaurs:dinosaur_upgrade_smithing_template_duplication")), "duplication grid matches " + matches);
		out = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, copy, level).orElseThrow().assemble(copy, net.minecraft.core.RegistryAccess.EMPTY);
		helper.assertTrue(out.is(ModItems.DINOSAUR_UPGRADE_SMITHING_TEMPLATE) && out.getCount() == 2, "duplication gives 2, got " + out);
		// Vanilla bones instead of dinosaur bones: nothing.
		ItemStack bone = new ItemStack(Items.BONE);
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(bone, bone, bone, bone, nt, bone, bone, bone, bone))).isEmpty(), "vanilla bones must not work");
		for (String id : new String[]{"dinosaur_upgrade_smithing_template", "dinosaur_upgrade_smithing_template_duplication",
				"dinosaur_chestplate_smithing", "dinosaur_leggings_smithing", "dinosaur_boots_smithing"}) {
			helper.assertTrue(level.getRecipeManager().byKey(Dinosaurs.id(id)).isPresent(), "missing recipe " + id);
		}
		for (String id : new String[]{"recipes/misc/dinosaur_upgrade_smithing_template", "recipes/misc/dinosaur_upgrade_smithing_template_duplication",
				"recipes/equipment/dinosaur_chestplate_smithing", "recipes/equipment/dinosaur_leggings_smithing", "recipes/equipment/dinosaur_boots_smithing"}) {
			helper.assertTrue(level.getServer().getAdvancements().getAdvancement(Dinosaurs.id(id)) != null, "missing advancement " + id);
		}
		helper.succeed();
	}

	private static List<String> smithingMatches(GameTestHelper helper, Container input) {
		var level = helper.getLevel();
		List<String> matches = new ArrayList<>();
		for (Recipe<?> holder : level.getServer().getRecipeManager().getRecipes()) {
			if (holder.getType() != RecipeType.SMITHING) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Recipe<Container> recipe = (Recipe<Container>) holder;
			if (recipe.matches(input, level)) {
				matches.add(holder.getId().toString());
			}
		}
		return matches;
	}

	/**
	 * Smithing table: Dinosaur Upgrade + netherite piece + its trophy -> the dinosaur piece, keeping enchantments, name,
	 * trim and damage (like the netherite upgrade). Wrong trophy, a diamond piece or the netherite template: no match.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void dinosaurArmorSmithing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		ItemStack template = new ItemStack(ModItems.DINOSAUR_UPGRADE_SMITHING_TEMPLATE);
		StringBuilder log = new StringBuilder();
		for (int r = 0; r < ARMOR.length; r++) {
			Item[] row = ARMOR[r];
			ItemStack base = new ItemStack(row[1]);
			base.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
			base.enchant(Enchantments.MENDING, 1);
			base.setHoverName(net.minecraft.network.chat.Component.literal("Rex Guard " + r));
			base.setDamageValue(57);
			var input = smithInput(template, base, new ItemStack(row[2]));
			List<String> matches = smithingMatches(helper, input);
			String id = "dinosaurs:" + BuiltInRegistries.ITEM.getKey(row[0]).getPath() + "_smithing";
			helper.assertTrue(matches.equals(List.of(id)), row[0] + " smithing matches " + matches);
			@SuppressWarnings("unchecked")
			Recipe<Container> recipe = (Recipe<Container>)
					level.getServer().getRecipeManager().byKey(Dinosaurs.id(id.substring(10))).orElseThrow();
			ItemStack out = recipe.assemble(input, net.minecraft.core.RegistryAccess.EMPTY);
			helper.assertTrue(out.is(row[0]) && out.getCount() == 1, "result " + out);
			helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, out) == 4
					&& EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, out) == 1, "enchantments kept: " + EnchantmentHelper.getEnchantments(out));
			helper.assertTrue(out.getHoverName().getString().equals("Rex Guard " + r), "name kept: " + out.getHoverName().getString());
			helper.assertTrue(out.getDamageValue() == 57, "damage kept: " + out.getDamageValue());
			log.append(' ').append(out.getHoverName().getString()).append('=').append(BuiltInRegistries.ITEM.getKey(out.getItem())).append(' ').append(EnchantmentHelper.getEnchantments(out));
			// Wrong trophy (the next piece's), the diamond piece, or the vanilla netherite template: nothing.
			Item wrong = ARMOR[(r + 1) % ARMOR.length][2];
			helper.assertTrue(smithingMatches(helper, smithInput(template, base, new ItemStack(wrong))).isEmpty(),
					row[1] + " + " + wrong + " must not match");
			helper.assertTrue(smithingMatches(helper, smithInput(template, new ItemStack(row[3]), new ItemStack(row[2]))).isEmpty(),
					row[3] + " must not be upgraded directly");
			helper.assertTrue(smithingMatches(helper, smithInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base,
					new ItemStack(row[2]))).isEmpty(), "netherite template + trophy must not match");
		}
		// No helmet recipe: the T-Rex Skull Helmet is the set's helmet.
		helper.assertTrue(smithingMatches(helper, smithInput(template, new ItemStack(Items.NETHERITE_HELMET),
				new ItemStack(ModItems.T_REX_TOOTH))).isEmpty(), "no dinosaur helmet upgrade");
		Dinosaurs.LOGGER.info("[test] dinosaur smithing:{}", log);
		helper.succeed();
	}

	/**
	 * The three trophies used by the armor still drop as before (no Looting): Raptor Claw 0-1 from the Velociraptor,
	 * Parasaurolophus Crest 0-1, Megalodon Tooth 1-3; Looting III raises the maximum by 3.
	 */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
	public void armorTrophyLoot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Player player = survivalPlayer(helper);
		Object[][] cases = {{ModEntities.VELOCIRAPTOR, ModItems.RAPTOR_CLAW, 0, 1}, {ModEntities.PARASAUROLOPHUS, ModItems.PARASAUROLOPHUS_CREST, 0, 1},
				{ModEntities.MEGALODON, ModItems.MEGALODON_TOOTH, 1, 3}};
		ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
		sword.enchant(Enchantments.MOB_LOOTING, 3);
		StringBuilder log = new StringBuilder();
		int x = 1;
		for (Object[] c : cases) {
			@SuppressWarnings("unchecked")
			EntityType<? extends Mob> type = (EntityType<? extends Mob>) c[0];
			Item item = (Item) c[1];
			var table = level.getServer().getLootData().getLootTable(type.getDefaultLootTable());
			Mob mob = helper.spawn(type, new BlockPos(x, 1, 4));
			int[] plain = roll(table, level, mob, player, item, 400);
			player.setItemInHand(InteractionHand.MAIN_HAND, sword.copy());
			int[] looted = roll(table, level, mob, player, item, 400);
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
			log.append(String.format(" %s %s %d-%d (avg %.2f), Looting III %d-%d (avg %.2f);", name, BuiltInRegistries.ITEM.getKey(item).getPath(),
					plain[0], plain[1], plain[2] / 400.0, looted[0], looted[1], looted[2] / 400.0));
			helper.assertTrue(plain[0] == (int) c[2] && plain[1] == (int) c[3], name + " " + item + " " + plain[0] + "-" + plain[1]);
			helper.assertTrue(looted[1] == (int) c[3] + 3, name + " Looting III max " + looted[1]);
			mob.discard();
			x += 2;
		}
		Dinosaurs.LOGGER.info("[test] armor trophies per kill:{}", log);
		helper.succeed();
	}

	private static int[] roll(net.minecraft.world.level.storage.loot.LootTable table, ServerLevel level, Mob mob, Player player, Item item, int rolls) {
		int min = Integer.MAX_VALUE, max = 0, total = 0;
		for (int i = 0; i < rolls; i++) {
			int count = 0;
			for (ItemStack stack : table.getRandomItems(lootParams(level, mob, player))) {
				if (stack.is(item)) count += stack.getCount();
			}
			min = Math.min(min, count);
			max = Math.max(max, count);
			total += count;
		}
		return new int[]{min, max, total};
	}

	/** Same lookup the crafting table does: the matching recipe's assemble() on a 3x3 grid. */
	private static ItemStack craftBoneBow(GameTestHelper helper, ItemStack bow) {
		ServerLevel level = helper.getLevel();
		ItemStack e = ItemStack.EMPTY;
		ItemStack b = new ItemStack(ModItems.DINOSAUR_BONES);
		ItemStack n = new ItemStack(Items.NETHERITE_INGOT);
		CraftingContainer input = craftInput(3, 3, List.of(e, b, e, b, bow, b, e, n, e));
		var holder = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		helper.assertTrue(holder.getId().equals(Dinosaurs.id("dinosaur_bone_bow")), "matched " + holder.getId());
		return holder.assemble(input, net.minecraft.core.RegistryAccess.EMPTY);
	}

	// ---- 1.6.0: Dinosaur Claw

	/** Raptor Claw, T-Rex Tooth, Raptor Claw / Netherite Ingot, Dinosaur Bones, Netherite Ingot / stick: ONLY the claw. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void dinosaurClawRecipe(GameTestHelper helper) {
		ItemStack e = ItemStack.EMPTY;
		ItemStack c = new ItemStack(ModItems.RAPTOR_CLAW);
		ItemStack t = new ItemStack(ModItems.T_REX_TOOTH);
		ItemStack n = new ItemStack(Items.NETHERITE_INGOT);
		ItemStack b = new ItemStack(ModItems.DINOSAUR_BONES);
		ItemStack s = new ItemStack(Items.STICK);
		CraftingContainer full = craftInput(3, 3, List.of(c, t, c, n, b, n, e, s, e));
		List<String> matches = craftingMatches(helper, full);
		helper.assertTrue(matches.equals(List.of("dinosaurs:dinosaur_claw")), "claw grid matches " + matches);
		ItemStack out = ((CraftingRecipe) helper.getLevel().getServer().getRecipeManager()
				.byKey(Dinosaurs.id("dinosaur_claw")).orElseThrow()).assemble(full, net.minecraft.core.RegistryAccess.EMPTY);
		helper.assertTrue(out.is(ModItems.DINOSAUR_CLAW) && out.getCount() == 1, "result " + out);
		// Near misses make nothing: Megalodon Tooth for the T-Rex Tooth, vanilla bones, no stick, no netherite.
		ItemStack m = new ItemStack(ModItems.MEGALODON_TOOTH);
		ItemStack vb = new ItemStack(Items.BONE);
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(c, m, c, n, b, n, e, s, e))).isEmpty(), "megalodon tooth must not craft the claw");
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(c, t, c, n, vb, n, e, s, e))).isEmpty(), "vanilla bone must not craft the claw");
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(c, t, c, n, b, n, e, e, e))).isEmpty(), "no stick must not craft");
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(c, t, c, e, b, e, e, s, e))).isEmpty(), "no netherite must not craft");
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().getAdvancement(Dinosaurs.id("recipes/equipment/dinosaur_claw")) != null,
				"missing recipe advancement");
		helper.succeed();
	}

	private static double handModifier(ItemStack stack, net.minecraft.world.entity.ai.attributes.Attribute attribute) {
		double[] total = {0};
		for (var __e : stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).entries()) {
			var holder = __e.getKey();
			var modifier = __e.getValue();
			if (holder.equals(attribute)) {
				total[0] += modifier.getAmount();
			}
		}
		return total[0];
	}

	/** 11 damage vs the netherite sword's 8, same speed, more durability and enchantability, reach modifier -0.5; a sword for
	 * sweeping and enchantments; fire resistant; repaired with Dinosaur Bones. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void dinosaurClawStats(GameTestHelper helper) {
		var damage = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE;
		var speed = net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED;
		ItemStack claw = new ItemStack(ModItems.DINOSAUR_CLAW);
		ItemStack sword = new ItemStack(Items.NETHERITE_SWORD);
		double clawDamage = 1 + handModifier(claw, damage), swordDamage = 1 + handModifier(sword, damage);
		double clawSpeed = 4 + handModifier(claw, speed), swordSpeed = 4 + handModifier(sword, speed);
		helper.assertTrue(clawDamage == 11 && swordDamage == 8, "damage " + clawDamage + " (netherite sword " + swordDamage + ")");
		helper.assertTrue(Math.abs(clawSpeed - 1.6) < 1e-4 && Math.abs(clawSpeed - swordSpeed) < 1e-4, "speed " + clawSpeed + " (netherite " + swordSpeed + ")");
		helper.assertTrue(claw.getMaxDamage() == 2501 && claw.getMaxDamage() > sword.getMaxDamage(), "durability " + claw.getMaxDamage());
		int enchantable = claw.getItem().getEnchantmentValue();
		helper.assertTrue(enchantable == 20, "enchantability " + enchantable);
		helper.assertTrue(claw.getItem().isFireResistant(), "fire resistant");
		helper.assertTrue(claw.is(net.minecraft.tags.ItemTags.SWORDS), "in #minecraft:swords (sweeping attack)");
		helper.assertTrue(claw.getItem() instanceof net.minecraft.world.item.SwordItem && claw.getDestroySpeed(Blocks.COBWEB.defaultBlockState()) > 1.0F, "sword (cuts cobwebs)");
		var enchants = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		for (var key : List.of(Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS, Enchantments.SWEEPING_EDGE,
				Enchantments.MOB_LOOTING, Enchantments.FIRE_ASPECT, Enchantments.KNOCKBACK, Enchantments.UNBREAKING, Enchantments.MENDING)) {
			helper.assertTrue(key.canEnchant(claw), "should take " + BuiltInRegistries.ENCHANTMENT.getKey(key));
		}
		// Repair: Dinosaur Bones yes, netherite ingot no (anvil).
		ItemStack worn = claw.copy();
		worn.setDamageValue(2000);
		ItemStack fixed = anvil(helper, worn, new ItemStack(ModItems.DINOSAUR_BONES));
		helper.assertTrue(fixed.is(ModItems.DINOSAUR_CLAW) && fixed.getDamageValue() < 2000, "bones repair the claw: " + fixed + " damage " + fixed.getDamageValue());
		ItemStack none = anvil(helper, worn.copy(), new ItemStack(Items.NETHERITE_INGOT));
		helper.assertTrue(none.isEmpty(), "netherite ingot must not repair the claw: " + none);
		// A real hit: a zombie takes 11 (netherite sword: 8) from a fully charged player attack.
		ServerLevel level = helper.getLevel();
		double[] dealt = new double[2];
		int i = 0;
		for (ItemStack weapon : List.of(claw, sword)) {
			net.minecraft.world.entity.monster.Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1 + 2 * i, 1, 1));
			float before = zombie.getHealth();
			Player player = survivalPlayer(helper);
			player.setItemInHand(InteractionHand.MAIN_HAND, weapon.copy());
			float attack = (float) (1 + handModifier(weapon, damage));
			zombie.hurt(level.damageSources().playerAttack(player), attack);
			dealt[i++] = before - zombie.getHealth();
		}
		helper.assertTrue(dealt[0] > dealt[1], "claw hit " + dealt[0] + " vs netherite sword " + dealt[1]);
		Dinosaurs.LOGGER.info("[test] dinosaur claw: damage {} speed {} durability {} ench {} (netherite sword {} / {} / {} / 15); hit {} vs {}",
				clawDamage, clawSpeed, claw.getMaxDamage(), enchantable, swordDamage, swordSpeed, sword.getMaxDamage(), dealt[0], dealt[1]);
		helper.succeed();
	}


	// ---- 1.7.0: Dino Collar, Megalodon size, spawn weights

	private static HostileDino collaredTRex(GameTestHelper helper, Player owner) {
		HostileDino rex = helper.spawn(ModEntities.T_REX, new BlockPos(4, 1, 4));
		owner.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DINO_COLLAR));
		owner.getInventory().add(new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION)); // after the collar: add() fills the hand slot first
		InteractionResult result = rex.interact(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(result.consumesAction(), "collaring should succeed, got " + result);
		return rex;
	}

	/** Without the amulet the collar does nothing (and is not used up); with it the player becomes the owner. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
	public void dinoCollarNeedsAmulet(GameTestHelper helper) {
		Player player = survivalPlayer(helper);
		player.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
		HostileDino rex = helper.spawn(ModEntities.T_REX, new BlockPos(4, 1, 4));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DINO_COLLAR));
		rex.interact(player, InteractionHand.MAIN_HAND);
		helper.assertFalse(rex.isCollared(), "no amulet: the T-Rex must not take the collar");
		helper.assertTrue(player.getMainHandItem().is(ModItems.DINO_COLLAR), "no amulet: the collar must not be used up");
		// Only the T-Rex takes a collar.
		Player owner = survivalPlayer(helper);
		owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DINO_COLLAR));
		owner.getInventory().add(new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION)); // after the collar: add() fills the hand slot first
		HostileDino spino = helper.spawn(ModEntities.SPINOSAURUS, new BlockPos(1, 1, 6));
		spino.interact(owner, InteractionHand.MAIN_HAND);
		helper.assertFalse(spino.isCollared(), "a Spinosaurus must not take the collar");
		// With the amulet: collared, owner saved, collar used up, persistent.
		rex.interact(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(rex.isCollared(), "amulet: the T-Rex should be collared");
		helper.assertTrue(rex.isOwnedBy(owner) && rex.getOwner() == owner, "owner should be the player who used the collar");
		helper.assertTrue(owner.getMainHandItem().isEmpty(), "collar should be used up");
		helper.assertTrue(rex.isPersistenceRequired(), "collared T-Rex must be persistent");
		// A second player cannot take it over.
		Player thief = survivalPlayer(helper);
		thief.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DINO_COLLAR));
		thief.getInventory().add(new ItemStack(ModItems.AMULET_OF_DINO_PROTECTION)); // after the collar: add() fills the hand slot first
		rex.interact(thief, InteractionHand.MAIN_HAND);
		helper.assertTrue(rex.isOwnedBy(owner), "a collared T-Rex keeps its owner");
		helper.assertTrue(thief.getMainHandItem().is(ModItems.DINO_COLLAR), "the second collar must not be used up");
		helper.succeed();
	}

	/** Never the owner (even after being hit by them), not hunting other players, but the wild one does. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 60)
	public void dinoCollarNeverAttacksOwner(GameTestHelper helper) {
		Player owner = survivalPlayer(helper);
		HostileDino rex = collaredTRex(helper, owner);
		Player other = survivalPlayer(helper);
		other.moveTo(helper.absoluteVec(new Vec3(6.5, 1, 6.5)));
		helper.assertFalse(rex.canAttack(owner), "collared T-Rex must never attack its owner (amulet or not)");
		rex.hurt(owner.damageSources().playerAttack(owner), 1.0F);
		helper.assertFalse(rex.canAttack(owner), "...not even after the owner hit it");
		rex.setTarget(owner);
		helper.assertTrue(rex.isAlliedTo(owner), "owner is an ally");
		HostileDino wild = helper.spawn(ModEntities.T_REX, new BlockPos(1, 1, 7));
		helper.assertTrue(wild.canAttack(other) && wild.huntsPlayers(), "a wild T-Rex hunts players");
		helper.assertFalse(rex.huntsPlayers(), "a collared T-Rex is neutral to other players");
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(rex.getTarget() == null, "owner must be dropped as target, got " + rex.getTarget());
			helper.succeed();
		});
	}

	/** Fights for the owner: whoever hurts the owner, and whatever the owner attacks. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 80)
	public void dinoCollarDefendsOwner(GameTestHelper helper) {
		Player owner = survivalPlayer(helper);
		HostileDino rex = collaredTRex(helper, owner);
		Player attacker = survivalPlayer(helper);
		attacker.moveTo(helper.absoluteVec(new Vec3(6.5, 1, 1.5)));
		helper.assertTrue(rex.getTarget() == null, "no target before the attack");
		owner.tickCount = 100; // mock players never tick; the goals compare the owner's hurt timestamps (tickCount)
		owner.hurt(attacker.damageSources().playerAttack(attacker), 1.0F);
		helper.assertTrue(owner.getLastHurtByMob() == attacker, "owner should remember the attacker");
		helper.runAfterDelay(15, () -> {
			helper.assertTrue(rex.getTarget() == attacker, "T-Rex should attack the player who hurt its owner, got " + rex.getTarget());
			rex.setTarget(null);
			net.minecraft.world.entity.Mob pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 6));
			owner.tickCount = 200;
			owner.setLastHurtMob(pig);
			// The T-Rex one-shots the pig, so either it is the target or it was killed by the T-Rex.
			helper.succeedWhen(() -> helper.assertTrue(rex.getTarget() == pig || (!pig.isAlive() && pig.getLastHurtByMob() == rex),
					"T-Rex should attack what its owner attacks, got " + rex.getTarget()));
		});
	}

	/** Owner saved with the entity; never despawns (not even on Peaceful, where a wild T-Rex vanishes). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
	public void dinoCollarPersistsAndNeverDespawns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Player owner = survivalPlayer(helper);
		HostileDino rex = collaredTRex(helper, owner);
		helper.assertTrue(rex.requiresCustomPersistence() && !rex.removeWhenFarAway(1.0E6), "collared T-Rex must not despawn far away");
		// Save and load a copy: same owner, still collared.
		var tag = new net.minecraft.nbt.CompoundTag();
		rex.saveWithoutId(tag);
		helper.assertTrue(tag.hasUUID("Owner"), "owner UUID should be saved, got " + tag.getAllKeys());
		HostileDino copy = ModEntities.T_REX.create(level);
		copy.load(tag);
		helper.assertTrue(copy.isCollared() && copy.isOwnedBy(owner), "loaded copy should keep the owner");
		helper.assertTrue(copy.isPersistenceRequired(), "loaded copy should stay persistent");
		// Peaceful: the wild T-Rex is removed by checkDespawn, the collared one stays. Restored in the same tick.
		HostileDino wild = helper.spawn(ModEntities.T_REX, new BlockPos(1, 1, 7));
		Difficulty before = level.getDifficulty();
		level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
		rex.checkDespawn();
		wild.checkDespawn();
		level.getServer().setDifficulty(before, true);
		helper.assertTrue(wild.isRemoved(), "wild T-Rex should despawn on Peaceful");
		helper.assertFalse(rex.isRemoved(), "collared T-Rex must never despawn");
		helper.succeed();
	}

	/** Sneak + empty hand toggles sit; the collar drops when the T-Rex dies. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 40)
	public void dinoCollarSitAndDrop(GameTestHelper helper) {
		Player owner = survivalPlayer(helper);
		HostileDino rex = collaredTRex(helper, owner);
		owner.setShiftKeyDown(true);
		rex.interact(owner, InteractionHand.MAIN_HAND);
		helper.assertTrue(rex.isOrderedToSit(), "sneak + empty hand should make it sit");
		rex.interact(owner, InteractionHand.MAIN_HAND);
		helper.assertFalse(rex.isOrderedToSit(), "again: follow");
		owner.setShiftKeyDown(false);
		rex.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
		helper.runAfterDelay(25, () -> {
			var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(6));
			boolean collar = drops.stream().anyMatch(item -> item.getItem().is(ModItems.DINO_COLLAR));
			helper.assertTrue(collar, "a dead collared T-Rex should drop its collar");
			helper.succeed();
		});
	}

	/** 3 T-Rex Teeth / Leather, Dinosaur Bones, Leather / Iron Chain: ONLY the collar; advancements exist. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void dinoCollarRecipe(GameTestHelper helper) {
		ItemStack e = ItemStack.EMPTY;
		ItemStack t = new ItemStack(ModItems.T_REX_TOOTH);
		ItemStack l = new ItemStack(Items.LEATHER);
		ItemStack b = new ItemStack(ModItems.DINOSAUR_BONES);
		ItemStack c = new ItemStack(Items.CHAIN);
		CraftingContainer full = craftInput(3, 3, List.of(t, t, t, l, b, l, e, c, e));
		List<String> matches = craftingMatches(helper, full);
		helper.assertTrue(matches.equals(List.of("dinosaurs:dino_collar")), "collar grid matches " + matches);
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(t, t, e, l, b, l, e, c, e))).isEmpty(), "2 teeth must not craft");
		helper.assertTrue(craftingMatches(helper, craftInput(3, 3, List.of(t, t, t, l, b, l, e, e, e))).isEmpty(), "no chain must not craft");
		for (String id : new String[]{"recipes/equipment/dino_collar", "collar_t_rex"}) {
			helper.assertTrue(helper.getLevel().getServer().getAdvancements().getAdvancement(Dinosaurs.id(id)) != null, "missing advancement " + id);
		}
		helper.succeed();
	}

	/** Megalodon: twice the 1.6 size (2.5 x 1.8 -> 5 x 3.6). */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void megalodonSize(GameTestHelper helper) {
		var dims = ModEntities.MEGALODON.getDimensions();
		helper.assertTrue(dims.width == 5.0F && dims.height == 3.6F, "megalodon size " + dims);
		helper.succeed();
	}

	/** Spawn weights: defaults, per-species value, global percent (rounded, at least 1), and the biome spawn lists use them. */
	@GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
	public void configSpawnWeights(GameTestHelper helper) {
		DinoConfig config = DinoConfig.get();
		int savedRex = config.spawnWeight(ModEntities.T_REX);
		int savedPercent = config.spawnFrequencyPercent;
		try {
			helper.assertTrue(ModEntities.defaultSpawnWeight(ModEntities.VELOCIRAPTOR) == 14 && ModEntities.defaultSpawnWeight(ModEntities.MEGALODON) == 1, "defaults");
			config.setSpawnWeight(ModEntities.T_REX, 10);
			config.spawnFrequencyPercent = 150;
			helper.assertTrue(config.effectiveSpawnWeight(ModEntities.T_REX) == 15, "10 x 150% = 15, got " + config.effectiveSpawnWeight(ModEntities.T_REX));
			config.spawnFrequencyPercent = 10;
			helper.assertTrue(config.effectiveSpawnWeight(ModEntities.MEGALODON) == 1, "a small multiplier keeps at least 1");
			config.spawnFrequencyPercent = 0;
			helper.assertTrue(config.effectiveSpawnWeight(ModEntities.T_REX) == 0, "0% = no natural spawns");
			config.setSpawnWeight(ModEntities.T_REX, 500);
			helper.assertTrue(config.spawnWeight(ModEntities.T_REX) == DinoConfig.MAX_WEIGHT, "weight is clamped");
		} finally {
			config.setSpawnWeight(ModEntities.T_REX, savedRex);
			config.spawnFrequencyPercent = savedPercent;
		}
		// The biome list built at server start holds the configured (here: default) weight.
		var plains = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS).value();
		int weight = -1;
		for (var entry : plains.getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.MONSTER).unwrap()) {
			if (entry.type == ModEntities.T_REX) {
				weight = entry.getWeight().asInt();
			}
		}
		Dinosaurs.LOGGER.info("[test] plains T-Rex spawn weight {} (config {})", weight, config.effectiveSpawnWeight(ModEntities.T_REX));
		helper.assertTrue(weight == config.effectiveSpawnWeight(ModEntities.T_REX), "plains T-Rex weight " + weight);
		helper.succeed();
	}
}
