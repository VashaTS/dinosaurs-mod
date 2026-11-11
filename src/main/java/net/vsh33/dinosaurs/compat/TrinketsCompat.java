package net.vsh33.dinosaurs.compat;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Trinkets (Trinkets Updated) integration. Only ever loaded after FabricLoader reports the "trinkets" mod as present,
 * so the mod runs fine without it. The amulet goes in the chest/necklace slot (see data/trinkets).
 */
public final class TrinketsCompat {
	public static boolean isEquipped(Player player, Item item) {
		TrinketAttachment attachment = TrinketsApi.getAttachment(player);
		return attachment != null && attachment.isEquipped(item);
	}

	private TrinketsCompat() {
	}
}
