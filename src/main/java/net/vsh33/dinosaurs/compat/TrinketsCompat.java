package net.vsh33.dinosaurs.compat;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Trinkets integration. Only ever loaded after FabricLoader reports the "trinkets" mod as present,
 * so the mod runs fine without it. The amulet goes in the chest/necklace slot (see data/trinkets).
 */
public final class TrinketsCompat {
	public static boolean isEquipped(Player player, Item item) {
		return TrinketsApi.getTrinketComponent(player).map(c -> c.isEquipped(item)).orElse(false);
	}

	private TrinketsCompat() {
	}
}
