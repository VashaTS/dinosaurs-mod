package net.vsh33.dinosaurs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.entity.HostileDino;

public class DinoRenderer extends MobRenderer<Mob, DinoModel> {
	private final ResourceLocation texture;
	private final float modelScale;

	public DinoRenderer(EntityRendererProvider.Context context, DinoModel model, ResourceLocation texture, float modelScale, float shadow) {
		super(context, model, shadow);
		this.texture = texture;
		this.modelScale = modelScale;
	}

	@Override
	public ResourceLocation getTextureLocation(Mob entity) {
		return texture;
	}

	/** Adds the saddle overlay layer (drawn only while the entity is saddled). */
	public DinoRenderer withSaddle(ResourceLocation saddleTexture) {
		this.addLayer(new DinoSaddleLayer(this, saddleTexture));
		return this;
	}

	/** Adds the Dino Collar layer (drawn only while the entity is collared). */
	public DinoRenderer withCollar(DinoModel collarModel, ResourceLocation collarTexture) {
		this.addLayer(new DinoCollarLayer(this, collarModel, collarTexture));
		return this;
	}

	@Override
	protected void scale(Mob entity, PoseStack poseStack, float partialTick) {
		poseStack.scale(modelScale, modelScale, modelScale);
		if (entity instanceof HostileDino dino && dino.isOrderedToSit()) {
			// Sitting: the whole body goes down (model y points down here); DinoModel folds the legs forward.
			poseStack.translate(0.0F, SIT_DROP, 0.0F);
		}
	}

	/** How far a sitting dinosaur's model is lowered, in model units (blocks before the model scale). */
	public static final float SIT_DROP = 10.0F / 16.0F;
}
