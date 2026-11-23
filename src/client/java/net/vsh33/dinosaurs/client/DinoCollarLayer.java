package net.vsh33.dinosaurs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/**
 * Dino Collar on a collared T. rex: a separate small model (a leather band with iron studs, an iron ring and a tooth
 * pendant) whose "neck" part has the same pivot as the T. rex neck, so the shared {@link DinoModel} animation moves it
 * exactly with the neck.
 */
public class DinoCollarLayer extends RenderLayer<DinoRenderState, DinoModel> {
	private final DinoModel collarModel;
	private final Identifier texture;

	public DinoCollarLayer(RenderLayerParent<DinoRenderState, DinoModel> renderer, DinoModel collarModel, Identifier texture) {
		super(renderer);
		this.collarModel = collarModel;
		this.texture = texture;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, DinoRenderState state, float yRot, float xRot) {
		if (state.collared) {
			coloredCutoutModelCopyLayerRender(this.collarModel, this.texture, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
		}
	}
}
