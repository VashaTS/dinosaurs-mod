package net.vsh33.dinosaurs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collection;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.Map;
import net.vsh33.dinosaurs.item.BoneBowEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** /enchant: Infinity + Mending may be combined on a Dinosaur Bone Bow held in the main hand. */
@Mixin(EnchantCommand.class)
public abstract class EnchantCommandMixin {
	@WrapOperation(method = "enchant", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantments(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Map;"))
	private static Map<Enchantment, Integer> dinosaurs$markItem(ItemStack item, Operation<Map<Enchantment, Integer>> original) {
		BoneBowEnchantments.begin(item);
		return original.call(item);
	}

	@WrapOperation(method = "enchant", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;isEnchantmentCompatible(Ljava/util/Collection;Lnet/minecraft/world/item/enchantment/Enchantment;)Z"))
	private static boolean dinosaurs$checkThenClear(Collection<Enchantment> existing, Enchantment target, Operation<Boolean> original) {
		try {
			return original.call(existing, target);
		} finally {
			BoneBowEnchantments.end();
		}
	}
}
