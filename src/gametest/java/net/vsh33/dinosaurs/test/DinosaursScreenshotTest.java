package net.vsh33.dinosaurs.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Renders every mob on a sky platform and saves a screenshot of each (run/screenshots). */
public class DinosaursScreenshotTest implements FabricClientGameTest {
	private static final String[][] MOBS = {
			// id, camera distance
			{"t_rex", "11"}, {"velociraptor", "4"}, {"spinosaurus", "11"}, {"triceratops", "7"}, {"stegosaurus", "7"},
			{"ankylosaurus", "6"}, {"brachiosaurus", "14"}, {"parasaurolophus", "7"}, {"megalodon", "12"}, {"pteranodon", "6"},
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			var server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule doMobSpawning false");
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("forceload add -16 -16 16 16");
			server.runCommand("fill -24 200 -24 24 200 24 minecraft:grass_block");
			server.runCommand("gamemode spectator @a");
			context.waitTicks(60);
			for (String[] mob : MOBS) {
				String id = mob[0];
				int dist = Integer.parseInt(mob[1]);
				server.runCommand("kill @e[type=!player]");
				server.runCommand("summon dinosaurs:" + id + " 0 201 0 {NoAI:1b,Rotation:[0f,0f],NoGravity:1b,PersistenceRequired:1b}");
				// Camera in front-left of the mob, slightly above.
				double cx = -dist * 0.7, cz = -dist * 0.7;
				server.runCommand(String.format("tp @p %.1f %.1f %.1f facing 0 %.1f 0", cx, 201.0 + dist * 0.35, cz, 201.0 + dist * 0.12));
				context.waitTicks(40);
				context.takeScreenshot("dino_" + id);
				server.runCommand(String.format("tp @p %.1f %.1f %.1f facing 0 %.1f 0", dist * 0.9, 201.0 + dist * 0.25, 0.0, 201.0 + dist * 0.12));
				context.waitTicks(20);
				context.takeScreenshot("dino_" + id + "_side");
			}
		}
	}
}
