package net.vsh33.dinosaurs.test;

import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * Screenshots of the Dinosaur Claw (1.6.0): first person, next to a netherite sword, third person,
 * on an armor stand, the inventory tooltip and the crafting table. Written to /workspace/dino-claw/ (dinosaurs.clawShots).
 */
public class ClawScreenshotTest implements FabricClientGameTest {
	/** -PclawOnly=true: only this test runs (quick look at the claw models); the other screenshot tests skip. */
	static final boolean ONLY = Boolean.getBoolean("dinosaurs.clawOnly");
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.clawShots", "/workspace/dino-claw"));

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
			server.runCommand("gamemode survival @a");
			server.runCommand("clear @a");
			server.runCommand("item replace entity @p weapon.mainhand with dinosaurs:dinosaur_claw");
			server.runCommand("item replace entity @p weapon.offhand with minecraft:netherite_sword");
			server.runCommand("tp @p 0.5 201 0.5 0 10");
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			context.waitTicks(40);

			shot(context, "first-person");
			// The left hand uses the mirrored right-hand transform: claw in the off hand, netherite sword in the main hand.
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:netherite_sword");
			server.runCommand("item replace entity @p weapon.offhand with dinosaurs:dinosaur_claw");
			shot(context, "first-person-offhand");
			server.runCommand("item replace entity @p weapon.mainhand with dinosaurs:dinosaur_claw");
			server.runCommand("item replace entity @p weapon.offhand with minecraft:netherite_sword");
			// Third person front (claw in the right hand, netherite sword in the left), zoomed in.
			context.getInput().pressKey(options -> options.keyToggleGui);
			int fov = context.computeOnClient(mc -> mc.options.fov().get());
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				mc.options.fov().set(35);
			});
			server.runCommand("tp @p 0.5 201 0.5 160 15");
			context.waitTicks(10);
			shot(context, "third-person-front");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runCommand("tp @p 0.5 201 0.5 -30 20");
			shot(context, "third-person-back");
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.fov().set(fov);
			});

			// Armor stands: the claw from the front and from the side (right arm toward the camera), a netherite sword for scale.
			String nbt = "{ShowArms:1b,NoGravity:1b,Rotation:[%sf,0f],equipment:{mainhand:{id:\"%s\",count:1}}}";
			server.runCommand("summon minecraft:armor_stand -0.7 201 3.0 " + String.format(nbt, "180", "minecraft:netherite_sword"));
			server.runCommand("summon minecraft:armor_stand 0.5 201 3.0 " + String.format(nbt, "180", "dinosaurs:dinosaur_claw"));
			server.runCommand("summon minecraft:armor_stand 1.9 201 3.0 " + String.format(nbt, "90", "dinosaurs:dinosaur_claw"));
			server.runCommand("tp @p 0.6 201 -0.6 0 12");
			shot(context, "stands-claw-vs-netherite-sword");
			context.getInput().pressKey(options -> options.keyToggleGui);
			server.runCommand("kill @e[type=armor_stand]");

			// Inventory: tooltip on the claw, then on the netherite sword for comparison.
			server.runCommand("item replace entity @p weapon.offhand with minecraft:air");
			server.runCommand("item replace entity @p hotbar.1 with minecraft:netherite_sword");
			server.runCommand("item replace entity @p hotbar.2 with dinosaurs:raptor_claw 2");
			server.runCommand("item replace entity @p hotbar.3 with dinosaurs:t_rex_tooth");
			server.runCommand("item replace entity @p hotbar.4 with dinosaurs:dinosaur_bones");
			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitForScreen(InventoryScreen.class);
			hover(context, 176, 166, 8 + 9, 142 + 9);
			shot(context, "inventory-claw-tooltip");
			hover(context, 176, 166, 8 + 18 + 9, 142 + 9);
			shot(context, "inventory-netherite-sword-tooltip");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);

			// Crafting table with the recipe laid out (the result slot shows the claw).
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				BlockPos pos = new BlockPos(1, 201, 2);
				player.level().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.CRAFTING_TABLE.defaultBlockState());
				player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(player.level(), pos)),
						Component.translatable("container.crafting")));
				ItemStack[] grid = {new ItemStack(ModItems.RAPTOR_CLAW), new ItemStack(ModItems.T_REX_TOOTH), new ItemStack(ModItems.RAPTOR_CLAW),
						new ItemStack(Items.NETHERITE_INGOT), new ItemStack(ModItems.DINOSAUR_BONES), new ItemStack(Items.NETHERITE_INGOT),
						ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY};
				for (int i = 0; i < 9; i++) {
					player.containerMenu.getSlot(1 + i).set(grid[i]);
				}
				player.containerMenu.slotsChanged(null);
				player.containerMenu.broadcastChanges();
			});
			context.waitForScreen(CraftingScreen.class);
			context.waitTicks(5);
			boolean crafted = context.computeOnClient(mc -> mc.player.containerMenu.getSlot(0).getItem().is(ModItems.DINOSAUR_CLAW));
			if (!crafted) {
				throw new AssertionError("crafting table should offer the Dinosaur Claw");
			}
			context.getInput().setCursorPos(0, 0);
			shot(context, "crafting-table");
			hover(context, 176, 166, 124 + 9, 35 + 9);
			shot(context, "crafting-table-result-tooltip");
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
