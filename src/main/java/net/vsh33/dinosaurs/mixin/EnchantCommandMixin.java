package net.vsh33.dinosaurs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collection;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.vsh33.dinosaurs.item.BoneBowEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** /enchant: Infinity + Mending may be combined on a Dinosaur Bone Bow held in the main hand. */
@Mixin(EnchantCommand.class)
public abstract class EnchantCommandMixin {
	@WrapOperation(method = "enchant", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentsForCrafting(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/enchantment/ItemEnchantments;"))
	private static ItemEnchantments dinosaurs$markItem(ItemStack item, Operation<ItemEnchantments> original) {
		BoneBowEnchantments.begin(item);
		return original.call(item);
	}

	@WrapOperation(method = "enchant", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;isEnchantmentCompatible(Ljava/util/Collection;Lnet/minecraft/core/Holder;)Z"))
	private static boolean dinosaurs$checkThenClear(Collection<Holder<Enchantment>> existing, Holder<Enchantment> target, Operation<Boolean> original) {
		try {
			return original.call(existing, target);
		} finally {
			BoneBowEnchantments.end();
		}
	}
}
