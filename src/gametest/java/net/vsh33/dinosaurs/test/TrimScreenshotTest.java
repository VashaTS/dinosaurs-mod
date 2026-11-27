package net.vsh33.dinosaurs.test;

import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * 1.7.2: armor trims on the Dinosaur Armor. Armor stands and the player wear trimmed pieces (several patterns and
 * materials), the inventory shows the trimmed item icons next to an untrimmed one, the smithing table previews a trim on
 * a Dinosaur Chestplate and offers nothing for the T-Rex Skull Helmet (not trimmable: its 3D model could not show a trim).
 * Screenshots go to /workspace/dino-trims/<mc> (-PtrimShots=dir); -PtrimOnly=true runs only this test.
 */
public class TrimScreenshotTest implements FabricClientGameTest {
	static final boolean ONLY = Boolean.getBoolean("dinosaurs.trimOnly");
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.trimShots", "/workspace/dino-trims"));

	private static String piece(String item, String pattern, String material) {
		return "dinosaurs:" + item + "[trim={material:\"minecraft:" + material + "\",pattern:\"minecraft:" + pattern + "\"}]";
	}

	private static String equipment(String pattern, String material) {
		String t = ",components:{\"minecraft:trim\":{material:\"minecraft:" + material + "\",pattern:\"minecraft:" + pattern + "\"}}";
		return "head:{id:\"dinosaurs:t_rex_skull_helmet\",count:1},chest:{id:\"dinosaurs:dinosaur_chestplate\",count:1" + t + "},"
				+ "legs:{id:\"dinosaurs:dinosaur_leggings\",count:1" + t + "},feet:{id:\"dinosaurs:dinosaur_boots\",count:1" + t + "}";
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.waitTicks(10);
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(2);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(1280, 720).withDestinationDir(OUT));
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!ONLY && (XaeroRadarScreenshotTest.ONLY || ClawScreenshotTest.ONLY || CollarScreenshotTest.ONLY || MegalodonScreenshotTest.ONLY)) {
			return;
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
			String nbt = "{ShowArms:1b,Silent:1b,PersistenceRequired:1b,equipment:{%s},Rotation:[%sf,0f]}";
			server.runCommand("summon minecraft:armor_stand -2.0 201 4.5 " + String.format(nbt, equipment("rib", "gold"), "200"));
			server.runCommand("summon minecraft:armor_stand 0.5 201 4.5 " + String.format(nbt, equipment("sentry", "diamond"), "180"));
			server.runCommand("summon minecraft:armor_stand 3.0 201 4.5 " + String.format(nbt, equipment("silence", "amethyst"), "160"));
			server.runCommand("summon minecraft:armor_stand 5.5 201 4.5 " + String.format(nbt,
					"head:{id:\"dinosaurs:t_rex_skull_helmet\",count:1},chest:{id:\"dinosaurs:dinosaur_chestplate\",count:1},"
					+ "legs:{id:\"dinosaurs:dinosaur_leggings\",count:1},feet:{id:\"dinosaurs:dinosaur_boots\",count:1}", "160"));
			server.runCommand("tp @p 1.75 202.0 0.6 facing 1.75 202.0 4.5");
			context.waitTicks(40);
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(20);
			shot(context, "stands-untrimmed_silence-amethyst_sentry-diamond_rib-gold");
			server.runCommand("tp @p 0.5 202 2.4 facing 0.5 202.0 4.5");
			shot(context, "stand-sentry-diamond-close");
			server.runCommand("tp @p 0.5 202 6.6 facing 0.5 201.9 4.5");
			shot(context, "stands-back");

			// The player wearing a trimmed set (wild / redstone), third person.
			server.runCommand("tp @e[type=!player] 0 -200 0");
			server.runCommand("item replace entity @p armor.head with dinosaurs:t_rex_skull_helmet");
			server.runCommand("item replace entity @p armor.chest with " + piece("dinosaur_chestplate", "wild", "redstone"));
			server.runCommand("item replace entity @p armor.legs with " + piece("dinosaur_leggings", "wild", "redstone"));
			server.runCommand("item replace entity @p armor.feet with " + piece("dinosaur_boots", "wild", "redstone"));
			server.runCommand("tp @p 0.5 201 0.5 180 -15");
			context.waitTicks(10);
			boolean trimmed = context.computeOnClient(mc -> mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.TRIM)
					&& mc.player.getItemBySlot(EquipmentSlot.LEGS).has(DataComponents.TRIM) && mc.player.getItemBySlot(EquipmentSlot.FEET).has(DataComponents.TRIM));
			if (!trimmed) {
				throw new AssertionError("player should wear the trimmed dinosaur armor");
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			shot(context, "player-wild-redstone-front");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runCommand("tp @p 0.5 201 0.5 0 15");
			shot(context, "player-wild-redstone-back");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.getInput().pressKey(options -> options.keyToggleGui);

			// Inventory: an untrimmed chestplate, then trimmed pieces with different materials.
			server.runCommand("gamemode survival @a");
			String[] hotbar = {"dinosaurs:dinosaur_chestplate", piece("dinosaur_chestplate", "rib", "gold"), piece("dinosaur_chestplate", "sentry", "diamond"),
					piece("dinosaur_chestplate", "coast", "emerald"), piece("dinosaur_leggings", "silence", "amethyst"), piece("dinosaur_leggings", "dune", "copper"),
					piece("dinosaur_boots", "tide", "lapis"), piece("dinosaur_boots", "flow", "netherite"), piece("dinosaur_boots", "bolt", "resin")};
			for (int i = 0; i < hotbar.length; i++) {
				server.runCommand("item replace entity @p hotbar." + i + " with " + hotbar[i]);
			}
			server.runCommand("item replace entity @p inventory.0 with dinosaurs:dinosaur_leggings");
			server.runCommand("item replace entity @p inventory.1 with dinosaurs:dinosaur_boots");
			server.runCommand("item replace entity @p inventory.2 with " + piece("dinosaur_leggings", "rib", "quartz"));
			server.runCommand("item replace entity @p inventory.3 with " + piece("dinosaur_boots", "rib", "iron"));
			server.runCommand("item replace entity @p inventory.4 with " + piece("dinosaur_chestplate", "eye", "quartz"));
			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitForScreen(InventoryScreen.class);
			context.getInput().setCursorPos(5, 5); // no tooltip over the icons
			shot(context, "inventory-trimmed-icons");
			hover(context, 176, 166, 8 + 18 + 9, 142 + 9);
			shot(context, "inventory-trimmed-chestplate-tooltip");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);

			// Smithing table: rib template + Dinosaur Chestplate + gold ingot previews the trimmed chestplate.
			server.runCommand("clear @p");
			server.runCommand("setblock 1 201 2 minecraft:smithing_table");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				BlockPos pos = new BlockPos(1, 201, 2);
				player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SmithingMenu(id, inv, ContainerLevelAccess.create(player.level(), pos)),
						Component.translatable("container.upgrade")));
				player.containerMenu.getSlot(0).set(new ItemStack(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE));
				player.containerMenu.getSlot(1).set(new ItemStack(ModItems.DINOSAUR_CHESTPLATE));
				player.containerMenu.getSlot(2).set(new ItemStack(Items.GOLD_INGOT));
				player.containerMenu.broadcastChanges();
			});
			context.waitForScreen(SmithingScreen.class);
			context.waitTicks(5);
			boolean offered = context.computeOnClient(mc -> {
				ItemStack result = mc.player.containerMenu.getSlot(3).getItem();
				return result.is(ModItems.DINOSAUR_CHESTPLATE) && result.has(DataComponents.TRIM);
			});
			if (!offered) {
				throw new AssertionError("smithing table should offer a trimmed Dinosaur Chestplate");
			}
			hover(context, 176, 166, 98 + 9, 48 + 9);
			shot(context, "smithing-chestplate-rib-gold");
			// The T-Rex Skull Helmet takes no trim.
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				player.containerMenu.getSlot(1).set(new ItemStack(ModItems.T_REX_SKULL_HELMET));
				player.containerMenu.broadcastChanges();
			});
			context.waitTicks(5);
			boolean none = context.computeOnClient(mc -> mc.player.containerMenu.getSlot(3).getItem().isEmpty());
			if (!none) {
				throw new AssertionError("smithing table must not trim the T-Rex Skull Helmet");
			}
			hover(context, 176, 166, 176 + 40, 166 + 20);
			shot(context, "smithing-skull-helmet-no-trim");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				for (int i = 0; i < 3; i++) {
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
