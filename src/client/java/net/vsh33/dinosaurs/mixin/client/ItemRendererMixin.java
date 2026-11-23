package net.vsh33.dinosaurs.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.vsh33.dinosaurs.client.DinosaursClient;
import net.vsh33.dinosaurs.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Dinosaur Claw: the flat icon in the inventory, on the ground and in item frames, the 3D clawed gauntlet everywhere else
 * (in hand, first and third person), like the vanilla trident / spyglass special cases.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
	private BakedModel dinosaurs$clawHeldModel(BakedModel model, ItemStack stack, ItemDisplayContext context) {
		if (stack.is(ModItems.DINOSAUR_CLAW) && context != ItemDisplayContext.GUI && context != ItemDisplayContext.GROUND
				&& context != ItemDisplayContext.FIXED) {
			BakedModel held = Minecraft.getInstance().getModelManager().getModel(DinosaursClient.CLAW_HELD_ID);
			if (held != null) {
				return held;
			}
		}
		return model;
	}
}
