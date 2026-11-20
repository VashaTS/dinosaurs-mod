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
		for (String id : new String[]{"amulet_of_dino_protection", "dinosaur_saddle", "leather_from_brachiosaurus_hide", "t_rex_skull_helmet"}) {
			helper.assertTrue(helper.getLevel().recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Dinosaurs.id(id))).isPresent(), "missing recipe " + id);
		}
		for (String id : new String[]{"amulet", "ride_triceratops", "recipes/equipment/dinosaur_saddle", "recipes/equipment/amulet_of_dino_protection", "recipes/misc/leather_from_brachiosaurus_hide"}) {
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
}
