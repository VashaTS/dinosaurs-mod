package net.vsh33.dinosaurs.test;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.vsh33.dinosaurs.entity.Megalodon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.client.book.GuideBookContents;
import net.vsh33.dinosaurs.client.book.GuideBookScreen;
import net.vsh33.dinosaurs.entity.HostileDino;
import net.vsh33.dinosaurs.entity.Triceratops;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * Client smoke test for the 1.21.1 / 1.20.1 backports (the Fabric client game test API only exists on 1.21.4+):
 * creates a world, renders every dinosaur, the armor, claw, bone bow, collar, saddle, crafting grid and the guide book,
 * and writes screenshots to -Ddinosaurs.smokeShots. Runs only with -Ddinosaurs.smoke=true; exits the game when done.
 */
public class ClientSmokeTest implements ClientModInitializer {
	private static final AtomicInteger TICKS = new AtomicInteger();
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.smokeShots", "build/smoke-shots"));
	private static final List<String> RESULTS = new ArrayList<>();
	private static final String[][] MOBS = {
			{"t_rex", "11"}, {"velociraptor", "4"}, {"spinosaurus", "11"}, {"triceratops", "7"}, {"stegosaurus", "7"},
			{"ankylosaurus", "6"}, {"brachiosaurus", "14"}, {"parasaurolophus", "7"}, {"megalodon", "12"}, {"pteranodon", "6"},
	};

	@Override
	public void onInitializeClient() {
		if (!Boolean.getBoolean("dinosaurs.smoke")) {
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(mc -> TICKS.incrementAndGet());
		Thread thread = new Thread(() -> {
			int code = 0;
			try {
				run(Minecraft.getInstance());
				RESULTS.add("ALL OK");
			} catch (Throwable t) {
				Dinosaurs.LOGGER.error("[smoke] FAILED", t);
				RESULTS.add("FAILED: " + t);
				code = 1;
			}
			try {
				Files.createDirectories(OUT);
				Files.write(OUT.resolve(Boolean.getBoolean("dinosaurs.xaeroOnly") ? "smoke-result-xaero.txt" : "smoke-result.txt"), RESULTS);
			} catch (IOException ignored) {
			}
			Dinosaurs.LOGGER.info("[smoke] result: {}", RESULTS);
			Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
		}, "dinosaurs-smoke");
		thread.setDaemon(true);
		thread.start();
	}

	// ---- helpers (test thread)

	private static void client(Consumer<Minecraft> action) {
		Minecraft mc = Minecraft.getInstance();
		mc.submit(() -> action.accept(mc)).join();
	}

	private static <T> T computeClient(Function<Minecraft, T> fn) {
		Minecraft mc = Minecraft.getInstance();
		return mc.submit(() -> fn.apply(mc)).join();
	}

	private static void server(Consumer<MinecraftServer> action) {
		MinecraftServer s = computeClient(Minecraft::getSingleplayerServer);
		s.submit(() -> action.accept(s)).join();
	}

	private static <T> T computeServer(Function<MinecraftServer, T> fn) {
		MinecraftServer s = computeClient(Minecraft::getSingleplayerServer);
		return s.submit(() -> fn.apply(s)).join();
	}

	private static void cmd(String command) {
		server(s -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), command));
	}

	private static void waitTicks(int n) {
		int target = TICKS.get() + n;
		while (TICKS.get() < target) {
			sleep(10);
		}
	}

	private static void waitFor(Predicate<Minecraft> cond, int maxTicks, String what) {
		int end = TICKS.get() + maxTicks;
		while (!computeClient(cond::test)) {
			if (TICKS.get() > end) throw new AssertionError("timed out waiting for " + what);
			sleep(50);
		}
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void check(boolean ok, String what) {
		RESULTS.add((ok ? "PASS " : "FAIL ") + what);
		Dinosaurs.LOGGER.info("[smoke] {} {}", ok ? "PASS" : "FAIL", what);
		if (!ok) throw new AssertionError(what);
	}

	private static void shot(String name) {
		client(mc -> mc.getToasts().clear());
		waitTicks(3);
		client(mc -> {
			try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
				Files.createDirectories(OUT);
				image.writeToFile(OUT.resolve(name + ".png"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		RESULTS.add("shot " + name + ".png");
	}

	private static ServerPlayer player(MinecraftServer s) {
		return s.getPlayerList().getPlayers().get(0);
	}

	// ---- the test

	private static void run(Minecraft mc0) {
		waitFor(mc -> mc.screen != null && mc.getOverlay() == null, 20 * 600, "title screen"); // title or first-launch accessibility screen
		waitTicks(20);
		client(mc -> {
			mc.options.pauseOnLostFocus = false;
			mc.options.onboardAccessibility = false;
			mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
			mc.getWindow().setWindowed(1280, 720);
			String name = "smoke" + System.currentTimeMillis();
			GameRules rules = new GameRules();
			rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
			rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
			mc.createWorldOpenFlows().createFreshLevel(name,
					new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT),
					new WorldOptions(20260101L, true, false),
					reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
		});
		waitFor(mc -> mc.level != null && mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null, 20 * 600, "world");
		check(computeServer(s -> BuiltInRegistries.ITEM.containsKey(Dinosaurs.id("guide_book"))), "mod loaded: dinosaurs items registered");
		checkXaeroDefinitions();
		if (Boolean.getBoolean("dinosaurs.xaeroOnly")) {
			xaeroRadar();
			return;
		}
		if (Boolean.getBoolean("dinosaurs.trimOnly")) {
			trims();
			return;
		}
		megalodonCulling();
		if (Boolean.getBoolean("dinosaurs.megalodonOnly")) {
			return;
		}
		trims();
		check(computeServer(s -> s.getRecipeManager().byKey(Dinosaurs.id("guide_book")).isPresent()), "guide book recipe loaded");
		long recipes = computeServer(s -> s.getRecipeManager().getRecipes().stream().filter(r -> r.getId().getNamespace().equals("dinosaurs")).count());
		check(recipes == 14, "dinosaurs recipes loaded: " + recipes);
		long adv = computeServer(s -> s.getAdvancements().getAllAdvancements().stream().filter(a -> a.getId().getNamespace().equals("dinosaurs")).count());
		check(adv == 17, "dinosaurs advancements loaded: " + adv);
		cmd("time set noon");
		cmd("weather clear");
		cmd("gamerule doMobSpawning false");
		cmd("forceload add -24 -24 24 24");
		cmd("fill -24 200 -24 24 200 24 minecraft:grass_block");
		cmd("fill -24 201 -24 24 230 24 minecraft:air");
		cmd("gamemode spectator @a");
		waitTicks(60);
		client(mc -> mc.options.hideGui = true);

		// Every dinosaur.
		for (String[] mob : MOBS) {
			String id = mob[0];
			int dist = Integer.parseInt(mob[1]);
			clearMobs();
			cmd("summon dinosaurs:" + id + " 0 201 0 {NoAI:1b,Rotation:[0f,0f],NoGravity:1b,PersistenceRequired:1b}");
			cmd(String.format("tp @p %.1f %.1f %.1f facing 0 %.1f 0", -dist * 0.7, 201.0 + dist * 0.35, -dist * 0.7, 201.0 + dist * 0.12));
			waitTicks(40);
			boolean present = computeServer(s -> !s.overworld().getEntities(BuiltInRegistries.ENTITY_TYPE.get(Dinosaurs.id(id)), e -> true).isEmpty());
			check(present, "summoned " + id);
			shot("dino_" + id);
		}
		clearMobs();

		// Collared T-Rex next to its owner, saddled Triceratops.
		cmd("gamemode creative @a");
		cmd("tp @p 0.5 201 -6.5 facing 0.5 202 0.5");
		cmd("summon dinosaurs:t_rex 0.5 201 3.5 {NoAI:1b,Rotation:[180f,0f],PersistenceRequired:1b}");
		cmd("summon dinosaurs:triceratops -5.0 201 4.5 {NoAI:1b,Rotation:[200f,0f],PersistenceRequired:1b}");
		waitTicks(20);
		server(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer p = player(s);
			for (HostileDino rex : level.getEntitiesOfClass(HostileDino.class, new AABB(-20, 190, -20, 20, 220, 20))) {
				rex.collar(p);
			}
			for (Triceratops t : level.getEntitiesOfClass(Triceratops.class, new AABB(-20, 190, -20, 20, 220, 20))) {
				t.setSaddled(true);
			}
		});
		waitTicks(10);
		check(computeClient(mc -> mc.level.getEntitiesOfClass(HostileDino.class, new AABB(-20, 190, -20, 20, 220, 20)).stream().allMatch(HostileDino::isCollared)),
				"client sees the T-Rex collared");
		client(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		cmd("tp @p -2.0 203.5 -4.5 facing -2.0 203 3.5");
		waitTicks(20);
		shot("collar_trex_and_saddled_triceratops");
		cmd("tp @p 4.5 204 1.5 facing 0.5 204 3.5");
		waitTicks(20);
		shot("collar_close_up");
		// Riding the saddled Triceratops (third person).
		cmd("ride @p mount @e[type=dinosaurs:triceratops,limit=1]");
		waitTicks(20);
		check(computeClient(mc -> mc.player.getVehicle() instanceof Triceratops), "riding the saddled Triceratops");
		client(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		waitTicks(20);
		shot("riding_triceratops");
		cmd("ride @p dismount");
		client(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

		// Player in full dinosaur armor + T-Rex skull helmet holding the claw (third person, front).
		clearMobs();
		cmd("item replace entity @p armor.head with dinosaurs:t_rex_skull_helmet");
		cmd("item replace entity @p armor.chest with dinosaurs:dinosaur_chestplate");
		cmd("item replace entity @p armor.legs with dinosaurs:dinosaur_leggings");
		cmd("item replace entity @p armor.feet with dinosaurs:dinosaur_boots");
		cmd("item replace entity @p weapon.mainhand with dinosaurs:dinosaur_claw");
		cmd("item replace entity @p weapon.offhand with dinosaurs:dinosaur_bone_bow");
		cmd("tp @p 0.5 201 0.5 0 0");
		waitTicks(20);
		client(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		waitTicks(20);
		shot("armor_claw_third_person");
		client(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		waitTicks(10);
		shot("armor_claw_third_person_back");

		// First person: claw (held model), then pulling the bone bow.
		client(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.options.hideGui = false;
		});
		cmd("item replace entity @p weapon.offhand with minecraft:air");
		waitTicks(10);
		shot("claw_first_person");
		cmd("item replace entity @p weapon.mainhand with dinosaurs:dinosaur_bone_bow");
		waitTicks(10);
		client(mc -> {
			mc.options.keyUse.setDown(true);
			mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
		});
		waitTicks(16);
		check(computeClient(mc -> mc.player.isUsingItem() && mc.player.getUseItem().is(ModItems.DINOSAUR_BONE_BOW)), "pulling the bone bow");
		shot("bone_bow_pulling_first_person");
		client(mc -> {
			mc.options.keyUse.setDown(false);
			mc.player.releaseUsingItem();
		});
		cmd("clear @p");
		waitTicks(10);

		// Crafting the guide book in the 2x2 inventory grid.
		client(mc -> mc.options.hideGui = false);
		cmd("gamemode survival @a");
		waitTicks(10);
		server(s -> {
			ServerPlayer p = player(s);
			p.inventoryMenu.getSlot(1).set(new ItemStack(Items.BOOK));
			p.inventoryMenu.getSlot(2).set(new ItemStack(Items.LEATHER));
			p.inventoryMenu.getSlot(3).set(new ItemStack(ModItems.RAPTOR_CLAW));
			p.inventoryMenu.slotsChanged(p.inventoryMenu.getCraftSlots());
			p.inventoryMenu.broadcastChanges();
		});
		client(mc -> mc.setScreen(new InventoryScreen(mc.player)));
		waitTicks(15);
		check(computeClient(mc -> mc.player.inventoryMenu.getSlot(0).getItem().is(ModItems.GUIDE_BOOK)), "inventory crafting grid offers the guide book");
		shot("crafting_guide_book");
		server(s -> {
			ServerPlayer p = player(s);
			for (int i = 1; i <= 4; i++) p.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
			p.inventoryMenu.broadcastChanges();
		});
		client(mc -> mc.setScreen(null));
		waitTicks(5);

		// Guide book.
		client(mc -> {
			GuideBookScreen.assertContentsFit(mc.font, mc.options.languageCode);
		});
		check(true, "guide book contents fit");
		client(mc -> mc.setScreen(new GuideBookScreen()));
		waitTicks(10);
		check(computeClient(mc -> mc.screen instanceof GuideBookScreen), "guide book screen open");
		shot("book_index");
		book(GuideBookScreen::openDinoList);
		shot("book_dinos");
		for (GuideBookContents.Entry entry : computeClient(mc -> GuideBookContents.create().dinoEntries())) {
			book(s -> s.openEntry(entry.id()));
			shot("book_dino_" + entry.id());
		}
		book(GuideBookScreen::openItemList);
		shot("book_items");
		for (GuideBookContents.Entry entry : computeClient(mc -> GuideBookContents.create().itemEntries())) {
			book(s -> s.openEntry(entry.id()));
			shot("book_item_" + entry.id());
		}
		book(GuideBookScreen::openSettings);
		shot("book_settings");
		client(mc -> mc.setScreen(null));

		// Natural spawning: normal world rules for a while near the player, count dinosaurs.
		clearMobs();
		cmd("forceload remove all");
		cmd("gamemode spectator @a");
		cmd("gamerule doMobSpawning true");
		cmd("time set midnight");
		cmd("tp @p 300 120 300");
		waitTicks(20 * 45);
		String counts = computeServer(s -> {
			StringBuilder sb = new StringBuilder();
			int total = 0;
			for (String[] mob : MOBS) {
				int n = s.overworld().getEntities(BuiltInRegistries.ENTITY_TYPE.get(Dinosaurs.id(mob[0])), e -> true).size();
				total += n;
				if (n > 0) sb.append(' ').append(mob[0]).append('=').append(n);
			}
			return total + sb.toString();
		});
		RESULTS.add("natural spawn after 45 s (night, around 300 120 300): " + counts);
		Dinosaurs.LOGGER.info("[smoke] natural dinosaurs: {}", counts);
		client(mc -> mc.options.hideGui = true);
		cmd("tp @p 300 160 300 facing 330 60 330");
		waitTicks(40);
		shot("natural_world_overview");
	}

	/** Every dinosaur has a Xaero's Minimap icon definition pointing at DinoModel.head (resolved by reflection). */
	private static void checkXaeroDefinitions() {
		try {
			check(net.vsh33.dinosaurs.client.DinoModel.class.getDeclaredField("head").getType() == net.minecraft.client.model.geom.ModelPart.class,
					"DinoModel.head is a ModelPart (Xaero icon definitions)");
		} catch (NoSuchFieldException e) {
			throw new AssertionError("DinoModel.head missing", e);
		}
		String bad = computeClient(mc -> {
			StringBuilder sb = new StringBuilder();
			for (String[] mob : MOBS) {
				var file = new net.minecraft.resources.ResourceLocation("xaerominimap", "entity/icon/definition/dinosaurs/" + mob[0] + ".json");
				var res = mc.getResourceManager().getResource(file);
				try {
					if (res.isEmpty() || !new String(res.get().open().readAllBytes()).contains("net.vsh33.dinosaurs.client.DinoModel;head")) sb.append(' ').append(mob[0]);
				} catch (IOException e) {
					sb.append(' ').append(mob[0]);
				}
			}
			return sb.toString();
		});
		check(bad.isEmpty(), "Xaero icon definitions present for all 10 dinosaurs" + bad);
	}

	private static void xaeroRadar() {
		check(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("xaerominimap"), "Xaero's Minimap loaded");
		String[][] mobs = {
				{"dinosaurs:t_rex", "-8", "-8"}, {"dinosaurs:velociraptor", "0", "-9"}, {"dinosaurs:spinosaurus", "8", "-8"},
				{"dinosaurs:triceratops", "-10", "0"}, {"dinosaurs:stegosaurus", "10", "0"}, {"dinosaurs:ankylosaurus", "-8", "8"},
				{"dinosaurs:brachiosaurus", "0", "10"}, {"dinosaurs:parasaurolophus", "8", "8"}, {"dinosaurs:megalodon", "-4", "-15"},
				{"dinosaurs:pteranodon", "4", "-15"}, {"minecraft:pig", "-4", "-3"}, {"minecraft:cow", "-4", "4"},
		};
		cmd("gamerule doMobSpawning false");
		cmd("gamerule sendCommandFeedback false");
		cmd("time set noon");
		cmd("weather clear");
		cmd("forceload add -32 -32 32 32");
		cmd("fill -30 200 -30 30 200 30 minecraft:grass_block");
		cmd("fill -30 201 -30 30 230 30 minecraft:air");
		cmd("fill -8 197 -19 -1 200 -12 minecraft:water");
		cmd("gamemode creative @a");
		for (String[] mob : mobs) {
			String y = mob[0].endsWith("megalodon") ? "198" : "201";
			cmd("summon " + mob[0] + " " + mob[1] + ".5 " + y + " " + mob[2] + ".5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Invulnerable:1b,Rotation:[0f,0f]}");
		}
		cmd("tp @p 0.5 201 0.5 0 60");
		waitTicks(100);
		client(mc -> mc.gui.getChat().clearMessages(false));
		shot("xaero_radar");
	}

	/**
	 * 1.7.1: the Megalodon must stay visible up close. The camera is put next to its tail with the hitbox off screen (the
	 * model is 14 blocks long, the hitbox 5): 1.7.0 culled the whole shark there, 1.7.1 renders it.
	 */
	private static void megalodonCulling() {
		cmd("gamerule sendCommandFeedback false");
		cmd("gamerule doMobSpawning false");
		cmd("time set noon");
		cmd("weather clear");
		cmd("forceload add -32 -32 32 32");
		cmd("fill -24 188 -24 24 188 24 minecraft:sand");
		cmd("fill -24 189 -24 24 196 24 minecraft:water");
		cmd("fill -24 197 -24 24 204 24 minecraft:water");
		cmd("fill -24 205 -24 24 215 24 minecraft:air");
		cmd("gamemode spectator @a");
		cmd("effect give @a minecraft:night_vision infinite 0 true");
		cmd("summon dinosaurs:megalodon 0.5 194 0.5 {NoAI:1b,Rotation:[0f,0f],PersistenceRequired:1b}");
		client(mc -> mc.options.hideGui = true);
		waitTicks(40);
		String[][] poses = {
				{"megalodon-overview", "14.5 197.0 -2.0", "0.5 196.0 0.5"},
				{"megalodon-close-tail", "2.5 196.0 -3.5", "0.5 196.0 -7.5"},
				{"megalodon-close-snout", "2.5 195.8 4.0", "0.5 195.6 6.0"},
				{"megalodon-close-tail-fin", "1.5 197.5 -4.0", "0.5 197.0 -7.0"}};
		List<String> culledBefore = new ArrayList<>();
		for (String[] p : poses) {
			cmd("tp @p " + p[1] + " facing " + p[2]);
			waitTicks(15);
			boolean[] vis = computeClient(mc -> {
				Megalodon shark = null;
				for (Entity e : mc.level.entitiesForRendering()) {
					if (e instanceof Megalodon m) shark = m;
				}
				if (shark == null) throw new AssertionError("no megalodon on the client");
				net.minecraft.client.renderer.culling.Frustum frustum;
				try {
					java.lang.reflect.Field f = net.minecraft.client.renderer.LevelRenderer.class.getDeclaredField("cullingFrustum");
					f.setAccessible(true);
					frustum = (net.minecraft.client.renderer.culling.Frustum) f.get(mc.levelRenderer);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
				net.minecraft.world.phys.Vec3 c = mc.gameRenderer.getMainCamera().getPosition();
				boolean old = frustum.isVisible(shark.getBoundingBox().inflate(0.5));
				boolean now = mc.getEntityRenderDispatcher().shouldRender(shark, frustum, c.x, c.y, c.z);
				return new boolean[]{old, now};
			});
			Dinosaurs.LOGGER.info("[smoke] {}: hitbox in view (1.7.0 culling) = {}, rendered (1.7.1) = {}", p[0], vis[0], vis[1]);
			check(vis[1], "megalodon rendered at " + p[0] + " (1.7.0 culling: " + (vis[0] ? "visible" : "hidden") + ")");
			if (!vis[0]) culledBefore.add(p[0]);
			shot(p[0]);
		}
		check(!culledBefore.isEmpty(), "close-up poses where 1.7.0 hid the megalodon: " + culledBefore);
		clearMobs();
		cmd("effect clear @a");
		cmd("fill -24 188 -24 24 196 24 minecraft:air");
		cmd("fill -24 197 -24 24 204 24 minecraft:air");
		client(mc -> mc.options.hideGui = false);
	}

	/** Removes mobs without loot or death messages. */
	// ---- 1.7.2: armor trims

	private static final boolean NBT_TRIMS = true; // 1.20.1: trims live in the item NBT (Trim:{...}), 1.21.1: the minecraft:trim component

	private static String trimmed(String item, String pattern, String material) {
		String t = "material:\"minecraft:" + material + "\",pattern:\"minecraft:" + pattern + "\"";
		return NBT_TRIMS ? "dinosaurs:" + item + "{Trim:{" + t + "}}" : "dinosaurs:" + item + "[trim={" + t + "}]";
	}

	private static String standItem(String item, String pattern, String material) {
		String id = "id:\"dinosaurs:" + item + "\"";
		if (pattern == null) {
			return NBT_TRIMS ? "{" + id + ",Count:1b}" : "{" + id + ",count:1}";
		}
		String t = "{material:\"minecraft:" + material + "\",pattern:\"minecraft:" + pattern + "\"}";
		return NBT_TRIMS ? "{" + id + ",Count:1b,tag:{Trim:" + t + "}}" : "{" + id + ",count:1,components:{\"minecraft:trim\":" + t + "}}";
	}

	private static void trimStand(double x, float yaw, String pattern, String material) {
		cmd(String.format(java.util.Locale.ROOT, "summon minecraft:armor_stand %.1f 201 4.5 {ShowArms:1b,Silent:1b,PersistenceRequired:1b,Rotation:[%.0ff,0f],ArmorItems:[%s,%s,%s,%s]}",
				x, yaw, standItem("dinosaur_boots", pattern, material), standItem("dinosaur_leggings", pattern, material),
				standItem("dinosaur_chestplate", pattern, material), standItem("t_rex_skull_helmet", null, null)));
	}

	/**
	 * Trimmed Dinosaur Armor: the trimmed item models are picked for every material (the baked model differs from the
	 * untrimmed one), armor stands and the player wear trims, the inventory and the smithing table show the trimmed icons,
	 * and the T-Rex Skull Helmet (3D model, not trimmable) gets no smithing result. Screenshots trims_*.png.
	 */
	private static void trims() {
		cmd("gamerule sendCommandFeedback false");
		cmd("gamerule doMobSpawning false");
		cmd("time set noon");
		cmd("weather clear");
		cmd("forceload add -16 -16 16 16");
		cmd("fill -12 200 -12 12 200 12 minecraft:grass_block");
		cmd("fill -12 201 -12 12 215 12 minecraft:air");
		cmd("gamemode creative @a");
		clearMobs();
		String[] materials = {"quartz", "iron", "netherite", "redstone", "copper", "gold", "emerald", "diamond", "lapis", "amethyst"};
		StringBuilder missing = new StringBuilder();
		for (String item : new String[]{"dinosaur_chestplate", "dinosaur_leggings", "dinosaur_boots"}) {
			for (String mat : materials) {
				cmd("item replace entity @p hotbar.0 with " + trimmed(item, "rib", mat));
				cmd("item replace entity @p hotbar.1 with dinosaurs:" + item);
				waitTicks(2);
				boolean differs = computeClient(mc -> {
					ItemStack t = mc.player.getInventory().getItem(0), plain = mc.player.getInventory().getItem(1);
					var m1 = mc.getItemRenderer().getModel(t, mc.level, mc.player, 0);
					var m2 = mc.getItemRenderer().getModel(plain, mc.level, mc.player, 0);
					return m1 != m2 && m1 != mc.getModelManager().getMissingModel();
				});
				if (!differs) missing.append(' ').append(item).append('/').append(mat);
			}
		}
		check(missing.isEmpty(), "trimmed item model used for every piece and material" + missing);
		cmd("clear @p");

		trimStand(-2.0, 200, "rib", "gold");
		trimStand(0.5, 180, "sentry", "diamond");
		trimStand(3.0, 160, "silence", "amethyst");
		trimStand(5.5, 160, null, null);
		cmd("tp @p 1.75 202.0 0.6 facing 1.75 202.0 4.5");
		client(mc -> mc.options.hideGui = true);
		waitTicks(40);
		shot("trims_stands-untrimmed_silence-amethyst_sentry-diamond_rib-gold");
		cmd("tp @p 0.5 202 2.4 facing 0.5 202.0 4.5");
		waitTicks(10);
		shot("trims_stand-sentry-diamond-close");
		clearMobs();

		cmd("item replace entity @p armor.head with dinosaurs:t_rex_skull_helmet");
		cmd("item replace entity @p armor.chest with " + trimmed("dinosaur_chestplate", "wild", "redstone"));
		cmd("item replace entity @p armor.legs with " + trimmed("dinosaur_leggings", "wild", "redstone"));
		cmd("item replace entity @p armor.feet with " + trimmed("dinosaur_boots", "wild", "redstone"));
		cmd("tp @p 0.5 201 0.5 180 -15");
		client(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		waitTicks(20);
		shot("trims_player-wild-redstone-front");
		client(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.options.hideGui = false;
		});

		cmd("gamemode survival @a");
		String[] hotbar = {"dinosaurs:dinosaur_chestplate", trimmed("dinosaur_chestplate", "rib", "gold"), trimmed("dinosaur_chestplate", "sentry", "diamond"),
				trimmed("dinosaur_chestplate", "coast", "emerald"), trimmed("dinosaur_leggings", "silence", "amethyst"), trimmed("dinosaur_leggings", "dune", "copper"),
				trimmed("dinosaur_boots", "tide", "lapis"), trimmed("dinosaur_boots", "vex", "netherite"),
				trimmed("dinosaur_boots", "eye", "quartz")};
		for (int i = 0; i < hotbar.length; i++) {
			cmd("item replace entity @p hotbar." + i + " with " + hotbar[i]);
		}
		cmd("item replace entity @p inventory.0 with dinosaurs:dinosaur_leggings");
		cmd("item replace entity @p inventory.1 with dinosaurs:dinosaur_boots");
		cmd("item replace entity @p inventory.2 with " + trimmed("dinosaur_leggings", "rib", "iron"));
		cmd("item replace entity @p inventory.3 with " + trimmed("dinosaur_boots", "rib", "gold"));
		waitTicks(5);
		client(mc -> mc.setScreen(new InventoryScreen(mc.player)));
		waitTicks(15);
		shot("trims_inventory-icons");
		client(mc -> mc.setScreen(null));
		cmd("clear @p");
		waitTicks(5);

		cmd("setblock 1 201 2 minecraft:smithing_table");
		server(s -> {
			ServerPlayer p = player(s);
			net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(1, 201, 2);
			p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new net.minecraft.world.inventory.SmithingMenu(id, inv,
					net.minecraft.world.inventory.ContainerLevelAccess.create(p.level(), pos)), net.minecraft.network.chat.Component.translatable("container.upgrade")));
			p.containerMenu.getSlot(0).set(new ItemStack(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE));
			p.containerMenu.getSlot(1).set(new ItemStack(ModItems.DINOSAUR_CHESTPLATE));
			p.containerMenu.getSlot(2).set(new ItemStack(Items.GOLD_INGOT));
			p.containerMenu.broadcastChanges();
		});
		waitTicks(15);
		check(computeClient(mc -> mc.player.containerMenu.getSlot(3).getItem().is(ModItems.DINOSAUR_CHESTPLATE)
				&& mc.getItemRenderer().getModel(mc.player.containerMenu.getSlot(3).getItem(), mc.level, mc.player, 0)
				!= mc.getItemRenderer().getModel(new ItemStack(ModItems.DINOSAUR_CHESTPLATE), mc.level, mc.player, 0)),
				"smithing table offers a trimmed Dinosaur Chestplate with the trimmed icon");
		shot("trims_smithing-chestplate-rib-gold");
		server(s -> {
			ServerPlayer p = player(s);
			p.containerMenu.getSlot(1).set(new ItemStack(ModItems.T_REX_SKULL_HELMET));
			p.containerMenu.broadcastChanges();
		});
		waitTicks(10);
		check(computeClient(mc -> mc.player.containerMenu.getSlot(3).getItem().isEmpty()), "smithing table gives no trim result for the T-Rex Skull Helmet");
		shot("trims_smithing-skull-helmet-no-trim");
		server(s -> {
			ServerPlayer p = player(s);
			for (int i = 0; i < 3; i++) p.containerMenu.getSlot(i).set(ItemStack.EMPTY);
			p.containerMenu.broadcastChanges();
			p.closeContainer();
		});
		waitTicks(5);
		cmd("gamemode creative @a");
		clearMobs();
	}

	private static void clearMobs() {
		cmd("tp @e[type=!player] 0 -300 0");
		cmd("kill @e[type=!player]");
		waitTicks(2);
		cmd("kill @e[type=item]");
	}

	private static void book(Consumer<GuideBookScreen> action) {
		client(mc -> {
			if (mc.screen instanceof GuideBookScreen screen) action.accept(screen);
		});
		waitTicks(6);
	}

	@SuppressWarnings("unused")
	private static Entity unused() {
		return null;
	}
}
