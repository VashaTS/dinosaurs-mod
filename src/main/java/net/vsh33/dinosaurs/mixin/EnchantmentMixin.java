package net.vsh33.dinosaurs.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.vsh33.dinosaurs.item.BoneBowEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Infinity + Mending are compatible while the anvil or /enchant works on a Dinosaur Bone Bow. */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {
	@Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
	private static void dinosaurs$boneBowInfinityMending(Holder<Enchantment> enchantment, Holder<Enchantment> other, CallbackInfoReturnable<Boolean> cir) {
		if (BoneBowEnchantments.active() && BoneBowEnchantments.isInfinityAndMending(enchantment, other)) {
			cir.setReturnValue(true);
		}
	}
}
