package net.vsh33.dinosaurs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** Draws the saddle (a transparent overlay texture on the same model) on a saddled Triceratops. */
public class DinoSaddleLayer extends RenderLayer<DinoRenderState, DinoModel> {
	private final Identifier texture;

	public DinoSaddleLayer(RenderLayerParent<DinoRenderState, DinoModel> renderer, Identifier texture) {
		super(renderer);
		this.texture = texture;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, DinoRenderState state, float yRot, float xRot) {
		if (state.saddled) {
			coloredCutoutModelCopyLayerRender(this.getParentModel(), this.texture, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
		}
	}
}
