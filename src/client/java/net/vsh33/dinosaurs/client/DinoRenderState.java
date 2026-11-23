package net.vsh33.dinosaurs.client;

/** Per-frame animation state of a dinosaur (the 26.x LivingEntityRenderState fields this mod uses). */
public class DinoRenderState {
	public float walkAnimationPos;
	public float walkAnimationSpeed;
	public float ageInTicks;
	/** Head yaw relative to the body, degrees. */
	public float yRot;
	/** Head pitch, degrees. */
	public float xRot;
	public boolean flying;
	public float attackProgress;
	public boolean saddled;
	/** T. rex wearing a Dino Collar (1.7.0). */
	public boolean collared;
	/** Collared T. rex ordered to sit. */
	public boolean sitting;
}
