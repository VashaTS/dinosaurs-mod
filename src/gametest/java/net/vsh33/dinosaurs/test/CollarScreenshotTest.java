package net.vsh33.dinosaurs.test;

import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.vsh33.dinosaurs.entity.HostileDino;
import net.vsh33.dinosaurs.entity.ModEntities;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * Dinosaurs 1.7.0 in a real client: a player with the amulet puts a Dino Collar on a T-Rex (real interaction), the
 * collared T-Rex next to the player, sitting, following / teleporting to the owner; the 2x Megalodon next to a player in a
 * boat; the Cloth Config screen (only when Cloth Config is on the classpath: -PwithCloth=true); the collar recipe and
 * tooltip. Screenshots go to /workspace/dino-collar/ (-PcollarShots=dir); -PcollarOnly=true runs only this test.
 */
public class CollarScreenshotTest implements FabricClientGameTest {
	static final boolean ONLY = Boolean.getBoolean("dinosaurs.collarOnly");
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.collarShots", "/workspace/dino-collar"));

	private static void shot(ClientGameTestContext context, String name) {
		context.waitTicks(10);
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(1280, 720).withDestinationDir(OUT));
	}

	private static HostileDino rex(net.minecraft.server.MinecraftServer s) {
		List<HostileDino> list = s.overworld().getEntitiesOfClass(HostileDino.class, new AABB(-200, 100, -200, 200, 300, 200),
				d -> d.getType() == ModEntities.T_REX);
		return list.isEmpty() ? null : list.getFirst();
	}

	/** NoAI on/off for the collared T-Rex, so it holds still for the screenshots. */
	private static void freeze(TestServerContext server, boolean on) {
		server.runOnServer(s -> rex(s).setNoAi(on));
	}

	private static void pose(TestServerContext server, double x, double y, double z, float yaw) {
		server.runOnServer(s -> {
			HostileDino rex = rex(s);
			rex.teleportTo(x, y, z);
			rex.setYRot(yaw);
			rex.setYBodyRot(yaw);
			rex.setYHeadRot(yaw);
			rex.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		});
	}

	private static void look(TestServerContext server, double[] at, double dx, double dy, double dz) {
		server.runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f",
				at[0] + dx, at[1] + dy, at[2] + dz, at[0], at[1], at[2]));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (XaeroRadarScreenshotTest.ONLY || ClawScreenshotTest.ONLY || MegalodonScreenshotTest.ONLY) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("difficulty normal");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("forceload add -48 -48 48 48");
			server.runCommand("fill -40 200 -40 40 200 40 minecraft:grass_block");
			server.runCommand("fill -40 201 -40 40 215 40 minecraft:air");
			server.runCommand("gamemode survival @a");
			server.runCommand("clear @a");
			server.runCommand("effect give @a minecraft:resistance infinite 255 true");
			server.runCommand("tp @p 0.5 201 0.5 0 0");
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			context.waitTicks(40);

			// ---- Collaring: amulet in the inventory, collar in the hand, use it on a T-Rex (the real interaction path).
			server.runCommand("summon dinosaurs:t_rex 0.5 201 6.5 {Rotation:[180f,0f]}");
			server.runCommand("item replace entity @p hotbar.8 with dinosaurs:amulet_of_dino_protection");
			server.runCommand("item replace entity @p weapon.mainhand with dinosaurs:dino_collar");
			context.waitTicks(10);
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				HostileDino rex = rex(s);
				rex.setTarget(null);
				var result = rex.interact(player, InteractionHand.MAIN_HAND, rex.position());
				if (!result.consumesAction() || !rex.isCollared() || !rex.isOwnedBy(player) || !player.getMainHandItem().isEmpty()) {
					throw new AssertionError("collaring failed: " + result + " collared=" + rex.isCollared());
				}
				if (!player.getAdvancements().getOrStartProgress(s.getAdvancements().get(net.vsh33.dinosaurs.Dinosaurs.id("collar_t_rex"))).isDone()) {
					// The criterion fires from the packet handler; here we used the entity directly, so award it to check the display.
					player.getAdvancements().award(s.getAdvancements().get(net.vsh33.dinosaurs.Dinosaurs.id("collar_t_rex")), "collar");
				}
			});
			context.waitTicks(20);
			// Next to the player, third person, camera from the side.
			server.runCommand("tp @p 0.5 201 0.5 120 15");
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			freeze(server, true);
			pose(server, 3.7, 201, -1.1, 60);
			shot(context, "collared-t-rex-next-to-player");
			// Close-ups of the collar from the front-left, the side and the back (the T-Rex frozen, facing south).
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("gamemode spectator @a");
			pose(server, 0.5, 201, 0.5, 0);
			// Collar band: model y 0..3 and z -13..-7 (px) at render scale 2.0 -> ~2.8 blocks up, ~1.25 blocks in front.
			double[] neck = {0.5, 201 + 2.8, 0.5 + 1.25};
			look(server, neck, 2.6, 0.6, 2.6);
			shot(context, "collar-close-up-front");
			look(server, neck, 3.2, 0.5, -0.2);
			shot(context, "collar-close-up-side");
			look(server, neck, -2.0, 0.8, -3.2);
			shot(context, "collar-close-up-back");
			freeze(server, false);
			// Sitting (sneak + use with an empty hand, real interaction) and wild T-Rex without a collar for comparison.
			server.runCommand("gamemode survival @a");
			server.runCommand("tp @p 0.5 201 -3.5 0 0");
			context.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				HostileDino rex = rex(s);
				player.setShiftKeyDown(true);
				rex.interact(player, InteractionHand.MAIN_HAND, rex.position());
				player.setShiftKeyDown(false);
				if (!rex.isOrderedToSit()) {
					throw new AssertionError("sneak + use should make the T-Rex sit");
				}
			});
			server.runCommand("summon dinosaurs:t_rex 7.5 201 0.5 {NoAI:1b,Rotation:[0f,0f],PersistenceRequired:1b,Tags:[\"wild\"]}");
			server.runCommand("gamemode spectator @a");
			server.runCommand("tp @p -9.0 205 -9.0 facing 3.5 202.5 0.5");
			shot(context, "sitting-collared-vs-wild-standing");
			server.runCommand("kill @e[tag=wild]");
			// Follow / teleport: owner walks 30 blocks away; the T-Rex (standing again) must arrive.
			server.runCommand("gamemode survival @a");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				HostileDino rex = rex(s);
				player.setShiftKeyDown(true);
				rex.interact(player, InteractionHand.MAIN_HAND, rex.position());
				player.setShiftKeyDown(false);
			});
			server.runCommand("tp @p 30.5 201 30.5 135 10");
			context.waitTicks(80);
			double dist = server.computeOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				return Math.sqrt(rex(s).distanceToSqr(player));
			});
			System.out.println("[collar-test] distance to owner after walking away: " + dist);
			if (dist > 12.0) {
				throw new AssertionError("collared T-Rex should follow/teleport to its owner, distance " + dist);
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			freeze(server, true);
			// Step back a little and look at the T-Rex that followed (third person behind the player).
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				HostileDino rex = rex(s);
				var away = player.position().subtract(rex.position()).multiply(1, 0, 1).normalize().scale(3.0);
				player.teleportTo(player.getX() + away.x, player.getY(), player.getZ() + away.z);
			});
			context.waitTicks(2);
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ facing entity @e[type=dinosaurs:t_rex,limit=1] eyes");
			// Turn a bit, or the player in front of the camera hides the T-Rex.
			server.runCommand("execute as @p at @s run tp @s ~ ~ ~ ~-28 ~");
			shot(context, "followed-owner-30-blocks");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			freeze(server, false);
			server.runCommand("kill @e[type=dinosaurs:t_rex]");

			// ---- Megalodon 2x next to a player in a boat (oak boat 1.375 wide; the shark is 5 x 3.6 now).
			server.runCommand("fill -20 195 -20 20 200 20 minecraft:water");
			server.runCommand("fill -20 194 -20 20 194 20 minecraft:sand");
			server.runCommand("summon dinosaurs:megalodon 0.5 196.2 2.5 {NoAI:1b,Rotation:[90f,0f],PersistenceRequired:1b}");
			server.runCommand("summon minecraft:oak_boat 0.5 201 -3.0 {Rotation:[90f,0f]}");
			// A mannequin (looks like a player) sits in the boat, so the spectator camera can show both.
			server.runCommand("summon minecraft:mannequin 0.5 201.5 -3.0 {Tags:[\"rider\"]}");
			context.waitTicks(5);
			server.runCommand("ride @e[tag=rider,limit=1] mount @e[type=minecraft:oak_boat,limit=1]");
			context.waitTicks(20);
			server.runCommand("gamemode spectator @a");
			server.runCommand("tp @p -8.0 205.0 -9.0 facing 0.5 199.0 0.0");
			shot(context, "megalodon-2x-vs-player-in-boat");
			server.runCommand("tp @p 12.5 201.5 -0.5 facing 0.5 198.5 0.5");
			shot(context, "megalodon-2x-side-surface");
			server.runCommand("tp @p 10.5 197.5 -6.5 facing 0.5 197.5 2.5");
			shot(context, "megalodon-2x-underwater");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("fill -20 194 -20 20 200 20 minecraft:grass_block");
			server.runCommand("gamemode survival @a");
			server.runCommand("tp @p 0.5 201 0.5 0 10");
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(10);

			// ---- Config screen (Cloth Config): Spawning tab and Spawn frequency tab.
			if (FabricLoader.getInstance().isModLoaded("cloth-config") || FabricLoader.getInstance().isModLoaded("cloth-config2")) {
				context.runOnClient(mc -> mc.gui.setScreen(net.vsh33.dinosaurs.client.compat.DinoConfigScreen.create(null, false)));
				context.waitTicks(10);
				context.getInput().setCursorPos(0, 0);
				shot(context, "config-spawning");
				context.runOnClient(mc -> mc.gui.setScreen(net.vsh33.dinosaurs.client.compat.DinoConfigScreen.create(null, true)));
				context.waitTicks(10);
				context.getInput().setCursorPos(0, 0);
				shot(context, "config-spawn-frequency");
				context.runOnClient(mc -> mc.gui.setScreen(null));
				context.waitTicks(5);
			} else {
				System.out.println("[collar-test] Cloth Config not on the classpath (-PwithCloth=true): no config screenshots");
			}

			// ---- Inventory tooltip and the crafting table with the recipe laid out.
			server.runCommand("item replace entity @p hotbar.0 with dinosaurs:dino_collar");
			server.runCommand("item replace entity @p hotbar.1 with dinosaurs:t_rex_tooth 3");
			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitForScreen(InventoryScreen.class);
			hover(context, 176, 166, 8 + 9, 142 + 9);
			shot(context, "inventory-collar-tooltip");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				BlockPos pos = new BlockPos(1, 201, 2);
				player.level().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState());
				player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(player.level(), pos)),
						Component.translatable("container.crafting")));
				ItemStack t = new ItemStack(ModItems.T_REX_TOOTH);
				ItemStack[] grid = {t, t.copy(), t.copy(),
						new ItemStack(Items.LEATHER), new ItemStack(ModItems.DINOSAUR_BONES), new ItemStack(Items.LEATHER),
						ItemStack.EMPTY, new ItemStack(Items.IRON_CHAIN), ItemStack.EMPTY};
				for (int i = 0; i < 9; i++) {
					player.containerMenu.getSlot(1 + i).set(grid[i]);
				}
				player.containerMenu.slotsChanged(null);
				player.containerMenu.broadcastChanges();
			});
			context.waitForScreen(CraftingScreen.class);
			context.waitTicks(5);
			boolean crafted = context.computeOnClient(mc -> mc.player.containerMenu.getSlot(0).getItem().is(ModItems.DINO_COLLAR));
			if (!crafted) {
				throw new AssertionError("crafting table should offer the Dino Collar");
			}
			context.getInput().setCursorPos(0, 0);
			shot(context, "collar-recipe-crafting-table");
			hover(context, 176, 166, 124 + 9, 35 + 9);
			shot(context, "collar-recipe-result-tooltip");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				for (int i = 1; i <= 9; i++) {
					player.containerMenu.getSlot(i).set(ItemStack.EMPTY);
				}
				player.containerMenu.broadcastChanges();
			});
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);
		}
	}

	/** Moves the cursor to (gx, gy) inside a centered container GUI of size w x h. */
	private static void hover(ClientGameTestContext context, int w, int h, int gx, int gy) {
		double[] pos = context.computeOnClient(mc -> {
			double scale = mc.getWindow().getGuiScale();
			int left = (mc.getWindow().getGuiScaledWidth() - w) / 2;
			int top = (mc.getWindow().getGuiScaledHeight() - h) / 2;
			return new double[]{(left + gx) * scale, (top + gy) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(2);
	}
}
