package net.vsh33.dinosaurs.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.dinosaurs.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** First person: while drawing the Dinosaur Bone Bow only the bow hand is shown, as with the vanilla bow. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@WrapOperation(method = {"evaluateWhichHandsToRender", "selectionUsingItemWhileHoldingBowLike"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
	private static boolean dinosaurs$boneBowIsBow(ItemStack stack, Item item, Operation<Boolean> original) {
		return original.call(stack, item) || (item == Items.BOW && stack.is(ModItems.DINOSAUR_BONE_BOW));
	}
}
