package net.vsh33.dinosaurs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.entity.AttackAnimated;
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
		if (entity instanceof AttackAnimated animated) {
			int ticks = animated.dinosaurs$getAttackTicks();
			state.attackProgress = ticks > 0 ? Math.max(0.0F, (ticks - partialTicks) / 10.0F) : 0.0F;
		} else {
			state.attackProgress = 0.0F;
		}
	}

	/** Adds the saddle overlay layer (drawn only while the entity is saddled). */
	public DinoRenderer withSaddle(Identifier saddleTexture) {
		this.addLayer(new DinoSaddleLayer(this, saddleTexture));
		return this;
	}

	@Override
	protected void scale(DinoRenderState state, PoseStack poseStack) {
		poseStack.scale(modelScale, modelScale, modelScale);
	}
}
