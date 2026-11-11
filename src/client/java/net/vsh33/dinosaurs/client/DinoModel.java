package net.vsh33.dinosaurs.client;

import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * One model class for all species: parts are looked up by name and animated if present
 * (legs, arms, tail, neck, head, jaw, wings, fins).
 */
public class DinoModel extends EntityModel<DinoRenderState> {
	private final @Nullable ModelPart head, neck, jaw, tail, tail2;
	private final @Nullable ModelPart rightLeg, leftLeg, rightArm, leftArm;
	private final @Nullable ModelPart rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg;
	private final @Nullable ModelPart rightWing, leftWing, rightFin, leftFin;
	private final float walkScale;
	private final boolean swimmer;

	public DinoModel(ModelPart root, float walkScale, boolean swimmer) {
		super(root);
		Function<String, @Nullable ModelPart> lookup = root.createPartLookup();
		this.head = lookup.apply("head");
		this.neck = lookup.apply("neck");
		this.jaw = lookup.apply("jaw");
		this.tail = lookup.apply("tail");
		this.tail2 = lookup.apply("tail2");
		this.rightLeg = lookup.apply("right_leg");
		this.leftLeg = lookup.apply("left_leg");
		this.rightArm = lookup.apply("right_arm");
		this.leftArm = lookup.apply("left_arm");
		this.rightHindLeg = lookup.apply("right_hind_leg");
		this.leftHindLeg = lookup.apply("left_hind_leg");
		this.rightFrontLeg = lookup.apply("right_front_leg");
		this.leftFrontLeg = lookup.apply("left_front_leg");
		this.rightWing = lookup.apply("right_wing");
		this.leftWing = lookup.apply("left_wing");
		this.rightFin = lookup.apply("right_fin");
		this.leftFin = lookup.apply("left_fin");
		this.walkScale = walkScale;
		this.swimmer = swimmer;
	}

	@Override
	public void setupAnim(DinoRenderState state) {
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos * walkScale;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);
		float deg = Mth.DEG_TO_RAD;

		ModelPart look = head != null ? head : null;
		if (look != null && !swimmer) {
			float yaw = Mth.clamp(state.yRot, -60.0F, 60.0F) * deg;
			float pitch = Mth.clamp(state.xRot, -30.0F, 30.0F) * deg;
			if (neck != null) {
				neck.yRot += yaw * 0.5F;
				look.yRot += yaw * 0.5F;
			} else {
				look.yRot += yaw;
			}
			look.xRot += pitch;
		}

		float swing = Mth.cos(pos * 0.6662F) * 1.0F * speed;
		float swingOpp = Mth.cos(pos * 0.6662F + Mth.PI) * 1.0F * speed;
		if (rightLeg != null) rightLeg.xRot += swing;
		if (leftLeg != null) leftLeg.xRot += swingOpp;
		if (rightHindLeg != null) rightHindLeg.xRot += swing;
		if (leftHindLeg != null) leftHindLeg.xRot += swingOpp;
		if (rightFrontLeg != null) rightFrontLeg.xRot += swingOpp;
		if (leftFrontLeg != null) leftFrontLeg.xRot += swing;
		if (rightArm != null) rightArm.xRot += 0.3F + swingOpp * 0.3F;
		if (leftArm != null) leftArm.xRot += 0.3F + swing * 0.3F;

		float tailSway = swimmer ? Mth.sin(age * 0.25F) * 0.25F : Mth.sin(age * 0.08F) * 0.08F + Mth.cos(pos * 0.6662F) * 0.15F * speed;
		if (tail != null) tail.yRot += tailSway;
		if (tail2 != null) tail2.yRot += tailSway * 1.5F;
		if (swimmer && head != null) head.yRot -= tailSway * 0.3F;
		// Drooping tails (rest pose pitched down, e.g. Brachiosaurus) also bob slowly up and down,
		// staying below horizontal: amplitude ~4 degrees around the ~20 degree droop.
		if (tail != null && tail.getInitialPose().xRot() < 0.0F) {
			float bob = Mth.sin(age * 0.06F) * 0.07F + Mth.sin(pos * 0.6662F * 2.0F) * 0.04F * speed;
			tail.xRot += bob;
			if (tail2 != null) tail2.xRot += bob * 0.8F;
		}

		if (jaw != null) {
			jaw.xRot += state.attackProgress * 0.7F + Math.max(0.0F, Mth.sin(age * 0.05F)) * 0.04F;
		}

		if (rightFin != null) rightFin.zRot += Mth.sin(age * 0.2F) * 0.1F;
		if (leftFin != null) leftFin.zRot -= Mth.sin(age * 0.2F) * 0.1F;

		if (rightWing != null && leftWing != null) {
			if (state.flying) {
				float flap = Mth.sin(age * 0.35F) * 0.6F;
				rightWing.zRot += flap;
				leftWing.zRot -= flap;
				if (rightLeg != null) rightLeg.xRot = 1.3F;
				if (leftLeg != null) leftLeg.xRot = 1.3F;
			} else {
				// Wings folded along the body while perched or walking.
				rightWing.xScale = 0.3F;
				leftWing.xScale = 0.3F;
				rightWing.zRot -= 0.25F;
				leftWing.zRot += 0.25F;
			}
		}
	}
}
