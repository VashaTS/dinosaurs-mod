package net.vsh33.dinosaurs.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vsh33.dinosaurs.item.DinosaurBoneBowItem;
import net.vsh33.dinosaurs.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** FOV zoom while drawing the Dinosaur Bone Bow, like the vanilla bow, reaching full zoom at its 15-tick draw. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@WrapOperation(method = "getFieldOfViewModifier", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
	private boolean dinosaurs$boneBowZoom(ItemStack stack, Object item, Operation<Boolean> original) {
		return original.call(stack, item) || (item == Items.BOW && stack.is(ModItems.DINOSAUR_BONE_BOW));
	}

	@ModifyConstant(method = "getFieldOfViewModifier", constant = @Constant(floatValue = 20.0F))
	private float dinosaurs$boneBowDrawTicks(float ticks) {
		return ((AbstractClientPlayer) (Object) this).getUseItem().is(ModItems.DINOSAUR_BONE_BOW) ? DinosaurBoneBowItem.DRAW_TICKS : ticks;
	}
}
