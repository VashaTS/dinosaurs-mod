package net.vsh33.dinosaurs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.vsh33.dinosaurs.item.HelmetEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.20.1 only: the T-Rex Skull Helmet is not an ArmorItem (it renders its 3D model on the head), so it is added to the
 * armor and helmet enchantment categories (Protection, Respiration, Aqua Affinity, ...) for the anvil, /enchant and the
 * enchanting table, like the head-armor tags do on newer versions.
 */
@Mixin(Enchantment.class)
public abstract class HelmetEnchantMixin {
	@WrapOperation(method = "canEnchant", require = 1, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentCategory;canEnchant(Lnet/minecraft/world/item/Item;)Z"))
	private boolean dinosaurs$helmetIsArmor(EnchantmentCategory category, Item item, Operation<Boolean> original) {
		return HelmetEnchantments.isHelmetCategory(category, item) || original.call(category, item);
	}

}
