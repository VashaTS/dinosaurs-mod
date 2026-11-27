package net.vsh33.dinosaurs.test;

import java.nio.file.Path;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.vsh33.dinosaurs.client.DinoModel;
import net.vsh33.dinosaurs.entity.ModEntities;

/**
 * Xaero's Minimap entity radar with every dinosaur (and a few vanilla mobs for comparison) around the player.
 * Only runs when Xaero's Minimap is on the test classpath: ./gradlew runClientGameTest -PxaeroJar=/path/to/xaerominimap.jar
 * Add -PxaeroRadar=always for icons and names without Tab. Screenshots go to /workspace/dino-xaero/ (-PxaeroShots=dir).
 */
public class XaeroRadarScreenshotTest implements FabricClientGameTest {
	/** Set by build.gradle for the -PxaeroJar run so the other screenshot tests skip. */
	static final boolean ONLY = Boolean.getBoolean("dinosaurs.xaeroOnly");
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.xaeroShots", "/workspace/dino-xaero"));
	// id, x, z (player at 0.5 0.5)
	private static final String[][] MOBS = {
			{"dinosaurs:t_rex", "-8", "-8"}, {"dinosaurs:velociraptor", "0", "-9"}, {"dinosaurs:spinosaurus", "8", "-8"},
			{"dinosaurs:triceratops", "-10", "0"}, {"dinosaurs:stegosaurus", "10", "0"}, {"dinosaurs:ankylosaurus", "-8", "8"},
			{"dinosaurs:brachiosaurus", "0", "10"}, {"dinosaurs:parasaurolophus", "8", "8"}, {"dinosaurs:megalodon", "-4", "-15"},
			{"dinosaurs:pteranodon", "4", "-15"},
			{"minecraft:pig", "-4", "-3"}, {"minecraft:spider", "4", "-3"}, {"minecraft:cow", "-4", "4"}, {"minecraft:creeper", "4", "4"},
	};

	private static void shot(ClientGameTestContext context, String name) {
		context.waitTicks(10);
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(1280, 720).withDestinationDir(OUT));
	}

	/**
	 * Runs with or without Xaero: every dinosaur has a radar icon definition in the mod's resources, and it points at
	 * DinoModel.head, which must still exist (Xaero resolves it by reflection; a rename would silently bring the names back).
	 */
	private static void checkIconDefinitions(ClientGameTestContext context) {
		try {
			if (DinoModel.class.getDeclaredField("head").getType() != ModelPart.class) {
				throw new AssertionError("DinoModel.head should be a ModelPart");
			}
		} catch (NoSuchFieldException e) {
			throw new AssertionError("DinoModel.head is used by the Xaero's Minimap icon definitions", e);
		}
		String missing = context.computeOnClient(mc -> {
			StringBuilder bad = new StringBuilder();
			for (EntityType<?> type : ModEntities.ALL) {
				Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
				Identifier file = Identifier.fromNamespaceAndPath("xaerominimap", "entity/icon/definition/" + id.getNamespace() + "/" + id.getPath() + ".json");
				var resource = mc.getResourceManager().getResource(file);
				try {
					if (resource.isEmpty() || !new String(resource.get().open().readAllBytes()).contains("net.vsh33.dinosaurs.client.DinoModel;head")) {
						bad.append(' ').append(id);
					}
				} catch (java.io.IOException e) {
					bad.append(' ').append(id);
				}
			}
			return bad.toString();
		});
		if (!missing.isEmpty()) {
			throw new AssertionError("missing or wrong Xaero's Minimap icon definition for:" + missing);
		}
		System.out.println("[xaero] icon definitions OK for " + ModEntities.ALL.size() + " dinosaurs");
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TrimScreenshotTest.ONLY) {
			return; // -PtrimOnly run: only the trim screenshots
		}
		checkIconDefinitions(context);
		if (!FabricLoader.getInstance().isModLoaded("xaerominimap")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("forceload add -32 -32 32 32");
			server.runCommand("fill -30 200 -30 30 200 30 minecraft:grass_block");
			server.runCommand("fill -30 201 -30 30 230 30 minecraft:air");
			server.runCommand("fill -8 197 -19 -1 200 -12 minecraft:water");
			server.runCommand("gamemode creative @a");
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			for (String[] mob : MOBS) {
				String y = mob[0].endsWith("megalodon") ? "198" : "201";
				server.runCommand("summon " + mob[0] + " " + mob[1] + ".5 " + y + " " + mob[2]
						+ ".5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Invulnerable:1b,Rotation:[0f,0f]}");
			}
			server.runCommand("tp @p 0.5 201 0.5 0 60");
			context.waitTicks(100);
			if ("always".equals(System.getProperty("dinosaurs.xaeroRadar"))) {
				// -PxaeroRadar=always: radar icons and names always on, no key held.
				shot(context, "radar-icons-names-always");
			} else {
				// Xaero's defaults: dots, and icons while the "List Players" key (Tab) is held.
				shot(context, "radar-default-no-tab");
				context.getInput().holdKey(InputConstants.KEY_TAB);
				context.waitTicks(40);
				shot(context, "radar-default-tab-held");
				context.getInput().releaseKey(InputConstants.KEY_TAB);
				context.waitTicks(10);
			}
		}
	}
}
