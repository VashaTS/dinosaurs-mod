package net.vsh33.dinosaurs.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/** 1.20.1: the T-Rex Skull Helmet (not an ArmorItem) counts as armor and as a helmet for enchanting. */
public final class HelmetEnchantments {
	public static boolean isHelmetCategory(EnchantmentCategory category, Item item) {
		return item == ModItems.T_REX_SKULL_HELMET && (category == EnchantmentCategory.ARMOR || category == EnchantmentCategory.ARMOR_HEAD);
	}

	private HelmetEnchantments() {
	}
}
