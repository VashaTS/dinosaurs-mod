package net.vsh33.dinosaurs.test;

import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * Screenshots of the T-Rex Skull Helmet: worn by the player (third person, front and back), by zombies and an
 * armor stand (front, side, three-quarter), in first person, in the inventory and in the hand.
 * Written to /workspace/dino-helmet/ (system property dinosaurs.helmetShots).
 */
public class HelmetScreenshotTest implements FabricClientGameTest {
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.helmetShots", "/workspace/dino-helmet"));

	private static void shot(ClientGameTestContext context, String name) {
		context.waitTicks(10);
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(1280, 720).withDestinationDir(OUT));
	}

	private static void camera(ClientGameTestContext context, CameraType type) {
		context.runOnClient(mc -> mc.options.setCameraType(type));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (XaeroRadarScreenshotTest.ONLY) {
			return; // -PxaeroJar run: only the minimap test
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("forceload add -16 -16 16 16");
			server.runCommand("fill -12 200 -12 12 200 12 minecraft:grass_block");
			server.runCommand("fill -12 201 -12 12 210 12 minecraft:air");
			server.runCommand("gamemode creative @a");
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			String nbt = "{NoAI:1b,Silent:1b,PersistenceRequired:1b,equipment:{head:{id:\"dinosaurs:t_rex_skull_helmet\",count:1}},Rotation:[%sf,0f]}";
			server.runCommand("summon minecraft:zombie -2.5 201 4.5 " + String.format(nbt, "180"));
			server.runCommand("summon minecraft:mannequin 0.5 201 4.5 "
					+ String.format(nbt.replace("NoAI:1b,", "immovable:1b,hide_description:1b,"), "90"));
			server.runCommand("summon minecraft:armor_stand 3.5 201 4.5 " + String.format(nbt.replace("NoAI:1b,", "ShowArms:1b,"), "135"));
			server.runCommand("tp @p 0.5 201 0.2 facing 0.5 202.4 4.5");
			context.waitTicks(40);
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(20);

			// Three wearers in a row, then close-ups: front, side profile (player model), three-quarter, back.
			shot(context, "lineup");
			server.runCommand("tp @p -2.5 202 2.2 facing -2.5 202.7 4.5");
			shot(context, "zombie-front");
			server.runCommand("tp @p 0.5 202 2.3 facing 0.5 202.6 4.5");
			shot(context, "mannequin-side");
			server.runCommand("tp @p 1.6 202.6 2.6 facing 0.5 202.5 4.5");
			shot(context, "mannequin-front-side");
			server.runCommand("tp @p 3.5 202 2.3 facing 3.5 202.6 4.5");
			shot(context, "stand-three-quarter");
			server.runCommand("tp @p -1.4 202.4 6.8 facing 0.5 202.5 4.5");
			shot(context, "mannequin-back");

			// The player wearing it.
			server.runCommand("tp @e[type=!player] 0 -200 0");
			server.runCommand("item replace entity @p armor.head with dinosaurs:t_rex_skull_helmet");
			server.runCommand("tp @p 0.5 201 0.5 180 -35");
			context.waitTicks(10);
			boolean worn = context.computeOnClient(mc -> mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.T_REX_SKULL_HELMET));
			if (!worn) {
				throw new AssertionError("player should wear the helmet");
			}
			camera(context, CameraType.THIRD_PERSON_FRONT);
			shot(context, "player-front");
			camera(context, CameraType.THIRD_PERSON_BACK);
			server.runCommand("tp @p 0.5 201 0.5 0 20");
			shot(context, "player-back");
			camera(context, CameraType.FIRST_PERSON);
			context.getInput().pressKey(options -> options.keyToggleGui);
			server.runCommand("item replace entity @p weapon.mainhand with dinosaurs:t_rex_skull");
			server.runCommand("tp @p 0.5 201 0.5 0 10");
			shot(context, "first-person-hand");

			// Inventory: both items in the hotbar, the player preview wearing the helmet.
			server.runCommand("gamemode survival @a");
			server.runCommand("item replace entity @p hotbar.1 with dinosaurs:t_rex_skull_helmet");
			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitForScreen(InventoryScreen.class);
			shot(context, "inventory");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);
		}
	}
}
