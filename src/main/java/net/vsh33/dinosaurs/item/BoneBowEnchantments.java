package net.vsh33.dinosaurs.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Infinity and Mending are mutually exclusive in vanilla (tag #minecraft:exclusive_set/bow). On the Dinosaur Bone Bow
 * they may be combined. Enchantment.areCompatible has no item argument, so the anvil and the /enchant command mark
 * (per thread) which item they are working on; the compatibility check only relaxes that one pair for this bow.
 */
public final class BoneBowEnchantments {
	private static final ThreadLocal<Boolean> BONE_BOW = ThreadLocal.withInitial(() -> false);

	public static void begin(ItemStack target) {
		BONE_BOW.set(target != null && target.is(ModItems.DINOSAUR_BONE_BOW));
	}

	public static void end() {
		BONE_BOW.set(false);
	}

	public static boolean active() {
		return BONE_BOW.get();
	}

	/** True for Infinity + Mending (either order). */
	public static boolean isInfinityAndMending(Holder<Enchantment> a, Holder<Enchantment> b) {
		return (a.is(Enchantments.INFINITY) && b.is(Enchantments.MENDING)) || (a.is(Enchantments.MENDING) && b.is(Enchantments.INFINITY));
	}

	/** Item-aware version of Enchantment.areCompatible. */
	public static boolean areCompatible(ItemStack target, Holder<Enchantment> a, Holder<Enchantment> b) {
		if (target.is(ModItems.DINOSAUR_BONE_BOW) && isInfinityAndMending(a, b)) {
			return true;
		}
		return Enchantment.areCompatible(a, b);
	}

	private BoneBowEnchantments() {
	}
}
