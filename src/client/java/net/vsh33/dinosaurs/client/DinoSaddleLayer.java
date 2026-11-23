package net.vsh33.dinosaurs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.entity.Triceratops;

/** Draws the saddle (a transparent overlay texture on the same model) on a saddled Triceratops. */
public class DinoSaddleLayer extends RenderLayer<Mob, DinoModel> {
	private final ResourceLocation texture;

	public DinoSaddleLayer(RenderLayerParent<Mob, DinoModel> renderer, ResourceLocation texture) {
		super(renderer);
		this.texture = texture;
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffers, int light, Mob entity, float limbSwing, float limbSwingAmount,
			float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity instanceof Triceratops t && t.isSaddled() && !entity.isInvisible()) {
			renderColoredCutoutModel(this.getParentModel(), this.texture, poseStack, buffers, light, entity, -1);
		}
	}
}
