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
		for (String id : new String[]{"amulet_of_dino_protection", "dinosaur_saddle", "leather_from_brachiosaurus_hide"}) {
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
}
