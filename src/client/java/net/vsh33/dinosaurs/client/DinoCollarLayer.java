package net.vsh33.dinosaurs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.entity.HostileDino;

/**
 * Dino Collar on a collared T. rex: a separate small model (a leather band with iron studs, an iron ring and a tooth
 * pendant) whose "neck" part has the same pivot as the T. rex neck, so the shared {@link DinoModel} animation moves it
 * exactly with the neck.
 */
public class DinoCollarLayer extends RenderLayer<Mob, DinoModel> {
	private final DinoModel collarModel;
	private final ResourceLocation texture;

	public DinoCollarLayer(RenderLayerParent<Mob, DinoModel> renderer, DinoModel collarModel, ResourceLocation texture) {
		super(renderer);
		this.collarModel = collarModel;
		this.texture = texture;
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffers, int light, Mob entity, float limbSwing, float limbSwingAmount,
			float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
		if (entity instanceof HostileDino dino && dino.isCollared()) {
			coloredCutoutModelCopyLayerRender(this.getParentModel(), this.collarModel, this.texture, poseStack, buffers, light, entity,
					limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTick, 1.0F, 1.0F, 1.0F);
		}
	}
}
