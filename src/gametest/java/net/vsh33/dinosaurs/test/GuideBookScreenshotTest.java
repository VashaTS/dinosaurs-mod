package net.vsh33.dinosaurs.test;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.dinosaurs.item.ModItems;
import net.vsh33.dinosaurs.client.book.GuideBookContents;
import net.vsh33.dinosaurs.client.book.GuideBookScreen;

/**
 * Screenshots of the dinosaur field guide (index, chapter lists, every dinosaur, items and settings)
 * in English and Polish, written to /workspace/dino-book/. Fails if any page text overflows.
 */
public class GuideBookScreenshotTest implements FabricClientGameTest {
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.bookShots", "/workspace/dino-book"));

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(3);
		context.takeScreenshot(TestScreenshotOptions.of(name)
				.disableCounterPrefix()
				.withSize(1280, 720)
				.withDestinationDir(OUT));
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
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("forceload add -8 -8 8 8");
			server.runCommand("fill -5 200 -5 5 200 5 minecraft:grass_block");
			server.runCommand("fill -5 201 -5 5 205 5 minecraft:air");
			server.runCommand("tp @p 0.5 201 0.5 facing 4.0 201 4.0");
			server.runCommand("gamemode survival @a");
			context.runOnClient(mc -> mc.getWindow().setWindowed(1280, 720));
			context.waitTicks(20);
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(20);

			context.runOnClient(mc -> GuideBookScreen.assertContentsFit(mc.font, "en_us"));
			server.runCommand("item replace entity @p hotbar.0 with dinosaurs:guide_book");
			crafting(context, server, "en");
			captureBook(context, "en");

			setLanguage(context, "pl_pl");
			context.runOnClient(mc -> GuideBookScreen.assertContentsFit(mc.font, "pl_pl"));
			crafting(context, server, "pl");
			captureBook(context, "pl");
		}
	}

	/** Book + Leather + Raptor Claw in the 2x2 inventory grid: the result slot must show the guide book. */
	private static void crafting(ClientGameTestContext context, TestServerContext server, String lang) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.inventoryMenu.getSlot(1).set(new ItemStack(Items.BOOK));
			player.inventoryMenu.getSlot(2).set(new ItemStack(Items.LEATHER));
			player.inventoryMenu.getSlot(3).set(new ItemStack(ModItems.RAPTOR_CLAW));
			player.inventoryMenu.slotsChanged(player.inventoryMenu.getCraftSlots());
			player.inventoryMenu.broadcastChanges();
		});
		context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(10);
		shot(context, "crafting-" + lang);
		boolean ok = context.computeOnClient(mc -> mc.player.inventoryMenu.getSlot(0).getItem().is(ModItems.GUIDE_BOOK));
		if (!ok) {
			throw new AssertionError("inventory crafting grid should offer the guide book");
		}
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			for (int i = 1; i <= 4; i++) {
				player.inventoryMenu.getSlot(i).set(ItemStack.EMPTY);
			}
			player.inventoryMenu.broadcastChanges();
		});
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(5);
	}

	private static void captureBook(ClientGameTestContext context, String lang) {
		context.runOnClient(mc -> mc.gui.setScreen(new GuideBookScreen()));
		context.waitForScreen(GuideBookScreen.class);
		context.waitTicks(5);
		shot(context, "index-" + lang);
		onScreen(context, GuideBookScreen::openDinoList);
		shot(context, "dinos-" + lang);
		for (GuideBookContents.Entry entry : GuideBookContents.create().dinoEntries()) {
			onScreen(context, s -> s.openEntry(entry.id()));
			shot(context, "dino-" + entry.id() + "-" + lang);
		}
		onScreen(context, GuideBookScreen::openItemList);
		shot(context, "items-" + lang);
		for (GuideBookContents.Entry entry : GuideBookContents.create().itemEntries()) {
			onScreen(context, s -> s.openEntry(entry.id()));
			shot(context, "item-" + entry.id() + "-" + lang);
		}
		onScreen(context, GuideBookScreen::openSettings);
		shot(context, "settings-" + lang);
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(5);
	}

	private static void onScreen(ClientGameTestContext context, java.util.function.Consumer<GuideBookScreen> action) {
		context.runOnClient(mc -> {
			if (mc.gui.screen() instanceof GuideBookScreen screen) {
				action.accept(screen);
			}
		});
		context.waitTicks(5);
	}

	private static void setLanguage(ClientGameTestContext context, String code) {
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected(code);
			mc.options.languageCode = code;
		});
		CompletableFuture<Void> reload = context.computeOnClient(Minecraft::reloadResourcePacks);
		context.waitFor(mc -> reload.isDone(), 30 * 20);
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitFor(mc -> mc.gui.overlay() == null, 30 * 20);
		context.waitTicks(20);
	}
}
