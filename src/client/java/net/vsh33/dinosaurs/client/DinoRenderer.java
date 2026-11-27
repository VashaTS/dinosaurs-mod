package net.vsh33.dinosaurs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
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

	/**
	 * 1.7.1: the models are scaled up and longer than the hitbox (the Megalodon is 14 blocks long with a 5-block hitbox),
	 * so culling with the bare hitbox hid the whole mob as soon as the hitbox left the screen, even with the head or tail
	 * still in view: the shark "disappeared in front of the player's eyes" up close. Cull with a box that covers the model.
	 */
	@Override
	public boolean shouldRender(Mob entity, Frustum frustum, double camX, double camY, double camZ) {
		if (super.shouldRender(entity, frustum, camX, camY, camZ)) {
			return true;
		}
		return entity.shouldRender(camX, camY, camZ) && !entity.noCulling
				&& frustum.isVisible(entity.getBoundingBoxForCulling().inflate(0.5 + cullingMargin()));
	}

	/** How far (blocks) the model can reach past the hitbox: the longest models are about 3.5 model blocks long. */
	public float cullingMargin() {
		return modelScale * CULLING_MARGIN_PER_SCALE;
	}

	public static final float CULLING_MARGIN_PER_SCALE = 1.75F;

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
