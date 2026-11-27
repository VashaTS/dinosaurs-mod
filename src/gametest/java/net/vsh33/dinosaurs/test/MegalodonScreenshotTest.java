package net.vsh33.dinosaurs.test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.vsh33.dinosaurs.entity.Megalodon;

/**
 * 1.7.1: the Megalodon must stay visible up close. The camera is put next to its head / tail with the hitbox off screen
 * (the model is 14 blocks long, the hitbox 5): 1.7.0 culled the whole shark there, 1.7.1 renders it. Each pose logs
 * whether the bare hitbox (the old culling box) and the renderer (the new one) are visible, and takes a screenshot.
 * Screenshots go to /workspace/dino-megalodon/ (-PmegalodonShots=dir); -PmegalodonOnly=true runs only this test.
 */
public class MegalodonScreenshotTest implements FabricClientGameTest {
	static final boolean ONLY = Boolean.getBoolean("dinosaurs.megalodonOnly");
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.megalodonShots", "/workspace/dino-megalodon"));

	private record Pose(String name, double x, double y, double z, double lx, double ly, double lz) {}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (XaeroRadarScreenshotTest.ONLY || ClawScreenshotTest.ONLY || CollarScreenshotTest.ONLY) {
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
			server.runCommand("fill -24 188 -24 24 188 24 minecraft:sand");
			// Two fills: one fill is limited to 32768 blocks.
			server.runCommand("fill -24 189 -24 24 196 24 minecraft:water");
			server.runCommand("fill -24 197 -24 24 204 24 minecraft:water");
			server.runCommand("fill -24 205 -24 24 215 24 minecraft:air");
			server.runCommand("gamemode spectator @a");
			server.runCommand("effect give @a minecraft:night_vision infinite 0 true");
			server.runCommand("tp @p 0.5 210 0.5 0 0");
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			context.waitTicks(40);
			// Shark facing south (+z): the snout reaches z = 5.5, the tail fin z = -6.5; the hitbox only covers z -2..3.
			server.runCommand("summon dinosaurs:megalodon 0.5 194 0.5 {NoAI:1b,Rotation:[0f,0f],PersistenceRequired:1b}");
			context.waitTicks(20);

			List<Pose> poses = List.of(
					new Pose("megalodon-overview", 14.5, 197.0, -2.0, 0.5, 196.0, 0.5),
					new Pose("megalodon-close-tail", 2.5, 196.0, -3.5, 0.5, 196.0, -7.5),
					new Pose("megalodon-close-snout", 2.5, 195.8, 4.0, 0.5, 195.6, 6.0),
					new Pose("megalodon-close-tail-fin", 1.5, 197.5, -4.0, 0.5, 197.0, -7.0));
			List<String> culledBefore = new ArrayList<>();
			for (Pose p : poses) {
				server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f facing %.2f %.2f %.2f", p.x, p.y, p.z, p.lx, p.ly, p.lz));
				context.waitTicks(10);
				boolean[] vis = context.computeOnClient(mc -> {
					Entity shark = null;
					for (Entity e : mc.level.entitiesForRendering()) {
						if (e instanceof Megalodon) {
							shark = e;
						}
					}
					if (shark == null) {
						throw new AssertionError("no megalodon on the client");
					}
					Camera camera = mc.gameRenderer.mainCamera();
					Frustum frustum = camera.getCullFrustum();
					Vec3 c = camera.position();
					boolean old = frustum.isVisible(shark.getBoundingBox().inflate(0.5));
					boolean now = mc.getEntityRenderDispatcher().shouldRender(shark, frustum, c.x, c.y, c.z);
					return new boolean[]{old, now};
				});
				System.out.println("[megalodon-test] " + p.name + ": hitbox in view (1.7.0 culling) = " + vis[0] + ", rendered (1.7.1) = " + vis[1]);
				if (!vis[1]) {
					throw new AssertionError("megalodon culled at pose " + p.name);
				}
				if (!vis[0]) {
					culledBefore.add(p.name);
				}
				context.waitTicks(5);
				context.takeScreenshot(TestScreenshotOptions.of(p.name).disableCounterPrefix().withSize(1280, 720).withDestinationDir(OUT));
			}
			System.out.println("[megalodon-test] poses where 1.7.0 hid the shark: " + culledBefore);
			if (culledBefore.isEmpty()) {
				throw new AssertionError("no close-up pose reproduces the 1.7.0 culling bug, the test proves nothing");
			}
			server.runCommand("kill @e[type=!player]");
		}
	}
}
