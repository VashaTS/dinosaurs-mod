package net.vsh33.dinosaurs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Enchanting table side of {@link HelmetEnchantMixin}. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
	@WrapOperation(method = "getAvailableEnchantmentResults", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentCategory;canEnchant(Lnet/minecraft/world/item/Item;)Z"))
	private static boolean dinosaurs$helmetIsArmor(EnchantmentCategory category, Item item, Operation<Boolean> original) {
		return net.vsh33.dinosaurs.item.HelmetEnchantments.isHelmetCategory(category, item) || original.call(category, item);
	}
}
