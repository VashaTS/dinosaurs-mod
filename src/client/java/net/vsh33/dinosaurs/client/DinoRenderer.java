package net.vsh33.dinosaurs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.vsh33.dinosaurs.entity.AttackAnimated;
import net.vsh33.dinosaurs.entity.HostileDino;
import net.vsh33.dinosaurs.entity.Pteranodon;
import net.vsh33.dinosaurs.entity.Triceratops;

public class DinoRenderer extends MobRenderer<Mob, DinoRenderState, DinoModel> {
	private final Identifier texture;
	private final float modelScale;

	public DinoRenderer(EntityRendererProvider.Context context, DinoModel model, Identifier texture, float modelScale, float shadow) {
		super(context, model, shadow);
		this.texture = texture;
		this.modelScale = modelScale;
	}

	@Override
	public Identifier getTextureLocation(DinoRenderState state) {
		return texture;
	}

	@Override
	public DinoRenderState createRenderState() {
		return new DinoRenderState();
	}

	@Override
	public void extractRenderState(Mob entity, DinoRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.flying = entity instanceof Pteranodon p && p.isFlying();
		state.saddled = entity instanceof Triceratops t && t.isSaddled();
		state.collared = entity instanceof HostileDino dino && dino.isCollared();
		state.sitting = entity instanceof HostileDino dino && dino.isOrderedToSit();
		if (entity instanceof AttackAnimated animated) {
			int ticks = animated.dinosaurs$getAttackTicks();
			state.attackProgress = ticks > 0 ? Math.max(0.0F, (ticks - partialTicks) / 10.0F) : 0.0F;
		} else {
			state.attackProgress = 0.0F;
		}
	}

	/**
	 * 1.7.1: the models are scaled up and longer than the hitbox (the Megalodon is 14 blocks long with a 5-block hitbox),
	 * so culling with the bare hitbox hid the whole mob as soon as the hitbox left the screen, even with the head or tail
	 * still in view: the shark "disappeared in front of the player's eyes" up close. Cull with a box that covers the model.
	 */
	@Override
	protected AABB getBoundingBoxForCulling(Mob entity) {
		return super.getBoundingBoxForCulling(entity).inflate(cullingMargin());
	}

	/** How far (blocks) the model can reach past the hitbox: the longest models are about 3.5 model blocks long. */
	public float cullingMargin() {
		return modelScale * CULLING_MARGIN_PER_SCALE;
	}

	public static final float CULLING_MARGIN_PER_SCALE = 1.75F;

	/** Adds the saddle overlay layer (drawn only while the entity is saddled). */
	public DinoRenderer withSaddle(Identifier saddleTexture) {
		this.addLayer(new DinoSaddleLayer(this, saddleTexture));
		return this;
	}

	/** Adds the Dino Collar layer (drawn only while the entity is collared). */
	public DinoRenderer withCollar(DinoModel collarModel, Identifier collarTexture) {
		this.addLayer(new DinoCollarLayer(this, collarModel, collarTexture));
		return this;
	}

	@Override
	protected void scale(DinoRenderState state, PoseStack poseStack) {
		poseStack.scale(modelScale, modelScale, modelScale);
		if (state.sitting) {
			// Sitting: the whole body goes down (model y points down here); DinoModel folds the legs forward.
			poseStack.translate(0.0F, SIT_DROP, 0.0F);
		}
	}

	/** How far a sitting dinosaur's model is lowered, in model units (blocks before the model scale). */
	public static final float SIT_DROP = 10.0F / 16.0F;
}
