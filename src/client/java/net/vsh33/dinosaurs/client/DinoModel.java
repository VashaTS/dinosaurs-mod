package net.vsh33.dinosaurs.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.entity.AttackAnimated;
import net.vsh33.dinosaurs.entity.HostileDino;
import net.vsh33.dinosaurs.entity.Pteranodon;
import net.vsh33.dinosaurs.entity.Triceratops;

/**
 * One model class for all species: parts are looked up by name and animated if present
 * (legs, arms, tail, neck, head, jaw, wings, fins).
 */
public class DinoModel extends HierarchicalModel<Mob> {
	private final ModelPart root;
	private float partialTick;
	private final ModelPart head, neck, jaw, tail, tail2;
	private final ModelPart rightLeg, leftLeg, rightArm, leftArm;
	private final ModelPart rightHindLeg, leftHindLeg, rightFrontLeg, leftFrontLeg;
	private final ModelPart rightWing, leftWing, rightFin, leftFin;
	private final float walkScale;
	private final boolean swimmer;

	public DinoModel(ModelPart root, float walkScale, boolean swimmer) {
		this.root = root;
		java.util.function.Function<String, ModelPart> lookup = name -> this.getAnyDescendantWithName(name).orElse(null);
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
	public ModelPart root() {
		return this.root;
	}

	@Override
	public void prepareMobModel(Mob entity, float limbSwing, float limbSwingAmount, float partialTick) {
		this.partialTick = partialTick;
	}

	/** Collects the per-frame state of the entity, like the 26.x render state. */
	public static DinoRenderState state(Mob entity, float pos, float speed, float age, float yaw, float pitch, float partialTicks) {
		DinoRenderState state = new DinoRenderState();
		state.walkAnimationPos = pos;
		state.walkAnimationSpeed = speed;
		state.ageInTicks = age;
		state.yRot = yaw;
		state.xRot = pitch;
		state.flying = entity instanceof Pteranodon p && p.isFlying();
		state.saddled = entity instanceof Triceratops t && t.isSaddled();
		state.collared = entity instanceof HostileDino dino && dino.isCollared();
		state.sitting = entity instanceof HostileDino dino && dino.isOrderedToSit();
		if (entity instanceof AttackAnimated animated) {
			int ticks = animated.dinosaurs$getAttackTicks();
			state.attackProgress = ticks > 0 ? Math.max(0.0F, (ticks - partialTicks) / 10.0F) : 0.0F;
		}
		return state;
	}

	@Override
	public void setupAnim(Mob entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.setupAnim(state(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, this.partialTick));
	}

	public void setupAnim(DinoRenderState state) {
		this.root.getAllParts().forEach(ModelPart::resetPose);
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
		if (tail != null && tail.getInitialPose().xRot < 0.0F) {
			float bob = Mth.sin(age * 0.06F) * 0.07F + Mth.sin(pos * 0.6662F * 2.0F) * 0.04F * speed;
			tail.xRot += bob;
			if (tail2 != null) tail2.xRot += bob * 0.8F;
		}

		if (state.sitting) {
			// Collared T. rex ordered to sit (DinoRenderer lowers the body): legs folded forward, arms tucked.
			if (rightLeg != null) rightLeg.xRot = -Mth.HALF_PI;
			if (leftLeg != null) leftLeg.xRot = -Mth.HALF_PI;
			if (rightArm != null) rightArm.xRot = 0.6F;
			if (leftArm != null) leftArm.xRot = 0.6F;
			if (tail != null) tail.xRot = tail.getInitialPose().xRot + 0.25F;
			if (tail2 != null) tail2.xRot = tail2.getInitialPose().xRot + 0.1F;
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
