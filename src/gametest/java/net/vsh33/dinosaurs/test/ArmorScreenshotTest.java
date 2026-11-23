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
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Screenshots of the Dinosaur Armor (1.5.0), worn with the T-Rex Skull Helmet: zombie, mannequin (front, side, back),
 * armor stands (plain, with a netherite piece next to it, with a rib trim), a baby zombie, the player in third person
 * and the inventory. Written to /workspace/dino-armor/ (system property dinosaurs.armorShots).
 */
public class ArmorScreenshotTest implements FabricClientGameTest {
	private static final Path OUT = Path.of(System.getProperty("dinosaurs.armorShots", "/workspace/dino-armor"));
	private static final String SET = "head:{id:\"dinosaurs:t_rex_skull_helmet\",count:1},chest:{id:\"dinosaurs:dinosaur_chestplate\",count:1},"
			+ "legs:{id:\"dinosaurs:dinosaur_leggings\",count:1},feet:{id:\"dinosaurs:dinosaur_boots\",count:1}";
	private static final String NETHERITE = "chest:{id:\"minecraft:netherite_chestplate\",count:1},legs:{id:\"minecraft:netherite_leggings\",count:1},"
			+ "feet:{id:\"minecraft:netherite_boots\",count:1},head:{id:\"minecraft:netherite_helmet\",count:1}";

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
		if (XaeroRadarScreenshotTest.ONLY || ClawScreenshotTest.ONLY || CollarScreenshotTest.ONLY) {
			return; // -PxaeroJar / -PclawOnly run: only that test
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
			String nbt = "{NoAI:1b,Silent:1b,PersistenceRequired:1b,equipment:{%s},Rotation:[%sf,0f]}";
			server.runCommand("summon minecraft:zombie -3.5 201 4.5 " + String.format(nbt, SET, "180"));
			server.runCommand("summon minecraft:mannequin -1.0 201 4.5 "
					+ String.format(nbt.replace("NoAI:1b,", "immovable:1b,hide_description:1b,"), SET, "180"));
			server.runCommand("summon minecraft:armor_stand 1.5 201 4.5 " + String.format(nbt.replace("NoAI:1b,", "ShowArms:1b,"), SET, "160"));
			server.runCommand("summon minecraft:armor_stand 4.0 201 4.5 " + String.format(nbt.replace("NoAI:1b,", "ShowArms:1b,"), NETHERITE, "200"));
			server.runCommand("summon minecraft:zombie 6.0 201 4.5 " + String.format(nbt.replace("NoAI:1b,", "NoAI:1b,IsBaby:1b,"), SET, "180"));
			server.runCommand("tp @p 0.5 201.2 -1.0 facing 0.5 202.0 4.5");
			context.waitTicks(40);
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(20);

			shot(context, "lineup");
			server.runCommand("tp @p -1.0 202 2.0 facing -1.0 202.0 4.5");
			shot(context, "mannequin-front");
			server.runCommand("tp @p 1.0 202 3.0 facing -1.0 201.9 4.5");
			shot(context, "mannequin-three-quarter");
			server.runCommand("tp @p -3.0 202 4.6 facing -1.0 201.9 4.5");
			shot(context, "mannequin-side");
			server.runCommand("tp @p -1.0 202 7.2 facing -1.0 201.9 4.5");
			shot(context, "mannequin-back");
			server.runCommand("tp @p 2.75 202 1.6 facing 2.75 201.9 4.5");
			shot(context, "dinosaur-vs-netherite");
			server.runCommand("tp @p -3.5 202 2.2 facing -3.5 202.0 4.5");
			shot(context, "zombie-front");
			server.runCommand("tp @p 6.0 201.6 2.6 facing 6.0 201.4 4.5");
			shot(context, "baby-zombie");

			// Rib trim (gold) on the stand: trims work on the new armor.
			server.runCommand("item replace entity @e[type=armor_stand,limit=1,sort=nearest,x=1.5,y=201,z=4.5] armor.chest with "
					+ "dinosaurs:dinosaur_chestplate[trim={material:\"minecraft:gold\",pattern:\"minecraft:rib\"}]");
			server.runCommand("item replace entity @e[type=armor_stand,limit=1,sort=nearest,x=1.5,y=201,z=4.5] armor.legs with "
					+ "dinosaurs:dinosaur_leggings[trim={material:\"minecraft:gold\",pattern:\"minecraft:rib\"}]");
			server.runCommand("tp @p 1.5 202 2.2 facing 1.5 202.0 4.5");
			shot(context, "stand-rib-trim");

			// The player wearing the set.
			server.runCommand("tp @e[type=!player] 0 -200 0");
			server.runCommand("item replace entity @p armor.head with dinosaurs:t_rex_skull_helmet");
			server.runCommand("item replace entity @p armor.chest with dinosaurs:dinosaur_chestplate");
			server.runCommand("item replace entity @p armor.legs with dinosaurs:dinosaur_leggings");
			server.runCommand("item replace entity @p armor.feet with dinosaurs:dinosaur_boots");
			server.runCommand("tp @p 0.5 201 0.5 180 -20");
			context.waitTicks(10);
			boolean worn = context.computeOnClient(mc -> mc.player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DINOSAUR_CHESTPLATE)
					&& mc.player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.DINOSAUR_LEGGINGS)
					&& mc.player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.DINOSAUR_BOOTS));
			if (!worn) {
				throw new AssertionError("player should wear the dinosaur armor");
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			shot(context, "player-front");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			server.runCommand("tp @p 0.5 201 0.5 0 15");
			shot(context, "player-back");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.getInput().pressKey(options -> options.keyToggleGui);

			// Inventory: armor on the player preview, the template and the trophies in the hotbar.
			server.runCommand("gamemode survival @a");
			server.runCommand("item replace entity @p hotbar.0 with dinosaurs:dinosaur_upgrade_smithing_template");
			server.runCommand("item replace entity @p hotbar.1 with minecraft:netherite_chestplate");
			server.runCommand("item replace entity @p hotbar.2 with dinosaurs:megalodon_tooth");
			server.runCommand("item replace entity @p hotbar.3 with dinosaurs:dinosaur_chestplate");
			server.runCommand("item replace entity @p hotbar.4 with dinosaurs:dinosaur_leggings");
			server.runCommand("item replace entity @p hotbar.5 with dinosaurs:dinosaur_boots");
			context.runOnClient(mc -> mc.gui.setScreen(new InventoryScreen(mc.player)));
			context.waitForScreen(InventoryScreen.class);
			shot(context, "inventory");
			hover(context, 176, 166, 8 + 9, 142 + 9);
			shot(context, "inventory-template-tooltip");
			hover(context, 176, 166, 8 + 3 * 18 + 9, 142 + 9);
			shot(context, "inventory-chestplate-tooltip");
			context.runOnClient(mc -> mc.gui.setScreen(null));
			context.waitTicks(5);

			// Smithing table: template only (the empty slots show the armor and trophy icons), then an enchanted,
			// named netherite chestplate + Megalodon Tooth -> a Dinosaur Chestplate that keeps both.
			server.runCommand("setblock 1 201 2 minecraft:smithing_table");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				BlockPos pos = new BlockPos(1, 201, 2);
				player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SmithingMenu(id, inv, ContainerLevelAccess.create(player.level(), pos)),
						Component.translatable("container.upgrade")));
				player.containerMenu.getSlot(0).set(new ItemStack(ModItems.DINOSAUR_UPGRADE_SMITHING_TEMPLATE));
				player.containerMenu.broadcastChanges();
			});
			context.waitForScreen(SmithingScreen.class);
			hover(context, 176, 166, 176 + 40, 166 + 20);
			shot(context, "smithing-template-only");
			hover(context, 176, 166, 26 + 9, 48 + 9);
			shot(context, "smithing-base-slot-hint");
			server.runOnServer(s -> {
				ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
				var lookup = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
				ItemStack chest = new ItemStack(Items.NETHERITE_CHESTPLATE);
				chest.enchant(lookup.getOrThrow(Enchantments.PROTECTION), 4);
				chest.enchant(lookup.getOrThrow(Enchantments.UNBREAKING), 3);
				chest.set(DataComponents.CUSTOM_NAME, Component.literal("Rex Guard"));
				player.containerMenu.getSlot(1).set(chest);
				player.containerMenu.getSlot(2).set(new ItemStack(ModItems.MEGALODON_TOOTH));
				player.containerMenu.broadcastChanges();
			});
			context.waitTicks(5);
			boolean kept = context.computeOnClient(mc -> {
				ItemStack result = mc.player.containerMenu.getSlot(3).getItem();
				return result.is(ModItems.DINOSAUR_CHESTPLATE) && result.getEnchantments().size() == 2 && "Rex Guard".equals(result.getHoverName().getString());
			});
			if (!kept) {
				throw new AssertionError("smithing table should offer a Dinosaur Chestplate with the enchantments and name");
			}
			hover(context, 176, 166, 176 + 40, 166 + 20);
			shot(context, "smithing-chestplate");
			hover(context, 176, 166, 98 + 9, 48 + 9);
			shot(context, "smithing-chestplate-tooltip");
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
