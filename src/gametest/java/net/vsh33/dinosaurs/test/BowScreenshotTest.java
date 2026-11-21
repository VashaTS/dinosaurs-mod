package net.vsh33.dinosaurs.test;

import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.vsh33.dinosaurs.item.ModItems;

/**
 * Screenshots of the Dinosaur Bone Bow: held and drawn in first person (pull stages, FOV zoom), drawn in third
 * person, next to a drawn vanilla bow, in the inventory with its tooltip, and in an anvil combining Infinity with
 * Mending. Written to /workspace/dino-bow/ (system property dinosaurs.bowShots).
 */
public class BowScreenshotTest implements FabricClientGameTest {
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.bowShots", "/workspace/dino-bow"));

	private static void clean(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
	}

	private static void snap(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(1280, 720).withDestinationDir(OUT));
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.waitTicks(10);
		clean(context);
		context.waitTicks(2);
		snap(context, name);
	}

	/** Holds right click for {@code ticks}, screenshots while still drawn, then lets go. */
	private static void drawn(ClientGameTestContext context, String name, int ticks) {
		clean(context);
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(ticks);
		snap(context, name);
		context.getInput().releaseKey(options -> options.keyUse);
		context.waitTicks(10);
	}

	private static ItemStack enchanted(ServerPlayer player, Item item, ResourceKey<Enchantment>... keys) {
		ItemStack stack = new ItemStack(item);
		var lookup = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		for (ResourceKey<Enchantment> key : keys) {
			stack.enchant(lookup.getOrThrow(key), 1);
		}
		return stack;
	}

	@Override
	@SuppressWarnings("unchecked")
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
			server.runCommand("setblock 0 201 9 minecraft:target");
			server.runCommand("setblock 0 202 9 minecraft:hay_block");
			server.runCommand("gamemode survival @a");
			server.runCommand("clear @a");
			server.runCommand("item replace entity @p weapon.mainhand with dinosaurs:dinosaur_bone_bow");
			server.runCommand("item replace entity @p hotbar.8 with minecraft:arrow 64");
			server.runCommand("tp @p 0.5 201 0.5 0 -2");
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			context.waitTicks(40);

			// First person: idle, early pull, full draw (FOV zoom, bone-bow pull textures).
			shot(context, "first-person-idle");
			drawn(context, "first-person-pull-early", 4);
			drawn(context, "first-person-pull-mid", 9);
			drawn(context, "first-person-drawn", 22);
			// Vanilla bow at the same 15-tick hold is still mid-pull (comparison).
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:bow");
			context.waitTicks(5);
			drawn(context, "vanilla-bow-15-ticks", 15);
			server.runCommand("item replace entity @p weapon.mainhand with dinosaurs:dinosaur_bone_bow");
			context.waitTicks(5);
			drawn(context, "bone-bow-15-ticks", 15);

			// Third person (front camera, zoomed in), at rest and drawn.
			context.getInput().pressKey(options -> options.keyToggleGui);
			int fov = context.computeOnClient(mc -> mc.options.fov().get());
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				mc.options.fov().set(35);
			});
			server.runCommand("tp @p 0.5 201 0.5 160 15");
			context.waitTicks(10);
			shot(context, "third-person-front");
			drawn(context, "third-person-front-drawn", 22);
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.fov().set(fov);
			});
			context.getInput().pressKey(options -> options.keyToggleGui);

			// Inventory with the bones and an Infinity + Mending bow, tooltip on the bow.
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				player.getInventory().setItem(0, enchanted(player, ModItems.DINOSAUR_BONE_BOW, Enchantments.INFINITY, Enchantments.MENDING, Enchantments.POWER));
				player.getInventory().setItem(1, new ItemStack(ModItems.DINOSAUR_BONES, 12));
				player.getInventory().setItem(2, new ItemStack(Items.BOW));
				player.inventoryMenu.broadcastChanges();
			});
			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitForScreen(InventoryScreen.class);
			hover(context, 176, 166, 8 + 9, 142 + 9);
			shot(context, "inventory-tooltip");
			hover(context, 176, 166, 8 + 18 + 9, 142 + 9);
			shot(context, "inventory-bones-tooltip");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);

			// Crafting table: an enchanted, named bow upgrades into a bone bow that keeps its enchantments and name.
			server.runCommand("setblock 1 201 2 minecraft:crafting_table");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				BlockPos pos = new BlockPos(1, 201, 2);
				player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, ContainerLevelAccess.create(player.level(), pos)),
						Component.translatable("container.crafting")));
				ItemStack bow = enchanted(player, Items.BOW, Enchantments.POWER, Enchantments.INFINITY, Enchantments.UNBREAKING);
				var lookup = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
				bow.enchant(lookup.getOrThrow(Enchantments.POWER), 5);
				bow.enchant(lookup.getOrThrow(Enchantments.UNBREAKING), 3);
				bow.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
				ItemStack[] grid = {ItemStack.EMPTY, new ItemStack(ModItems.DINOSAUR_BONES), ItemStack.EMPTY,
						new ItemStack(ModItems.DINOSAUR_BONES), bow, new ItemStack(ModItems.DINOSAUR_BONES),
						ItemStack.EMPTY, new ItemStack(Items.NETHERITE_INGOT), ItemStack.EMPTY};
				for (int i = 0; i < 9; i++) {
					player.containerMenu.getSlot(1 + i).set(grid[i]);
				}
				player.containerMenu.broadcastChanges();
			});
			context.waitForScreen(CraftingScreen.class);
			context.waitTicks(5);
			boolean kept = context.computeOnClient(mc -> {
				ItemStack result = mc.player.containerMenu.getSlot(0).getItem();
				return result.is(ModItems.DINOSAUR_BONE_BOW) && result.getEnchantments().size() == 3
						&& "Old Faithful".equals(result.getHoverName().getString());
			});
			if (!kept) {
				throw new AssertionError("crafting table should offer a bone bow with the bow's enchantments and name");
			}
			hover(context, 176, 166, 176 + 40, 166 + 20);
			shot(context, "crafting-enchanted-bow");
			hover(context, 176, 166, 124 + 9, 35 + 9);
			shot(context, "crafting-enchanted-bow-tooltip");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				for (int i = 1; i <= 9; i++) {
					player.containerMenu.getSlot(i).set(ItemStack.EMPTY);
				}
				player.containerMenu.broadcastChanges();
			});
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);

			// Anvil: Infinity bone bow + Mending book gives a bow with both.
			server.runCommand("setblock 2 201 2 minecraft:anvil");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				player.giveExperienceLevels(30);
				BlockPos pos = new BlockPos(2, 201, 2);
				player.openMenu(new SimpleMenuProvider((id, inv, p) -> new AnvilMenu(id, inv, ContainerLevelAccess.create(player.level(), pos)),
						Component.translatable("container.repair")));
				player.containerMenu.getSlot(0).set(enchanted(player, ModItems.DINOSAUR_BONE_BOW, Enchantments.INFINITY));
				player.containerMenu.getSlot(1).set(enchanted(player, Items.ENCHANTED_BOOK, Enchantments.MENDING));
				player.containerMenu.broadcastChanges();
			});
			context.waitForScreen(AnvilScreen.class);
			context.waitTicks(5);
			boolean both = context.computeOnClient(mc -> {
				ItemStack result = mc.player.containerMenu.getSlot(2).getItem();
				return result.is(ModItems.DINOSAUR_BONE_BOW) && result.getEnchantments().size() == 2;
			});
			if (!both) {
				throw new AssertionError("anvil should offer a bone bow with Infinity and Mending");
			}
			hover(context, 176, 166, 176 + 40, 166 + 20);
			shot(context, "anvil-infinity-mending");
			hover(context, 176, 166, 134 + 9, 47 + 9);
			shot(context, "anvil-result-tooltip");
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
