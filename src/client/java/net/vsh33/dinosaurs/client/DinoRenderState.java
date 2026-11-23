package net.vsh33.dinosaurs.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class DinoRenderState extends LivingEntityRenderState {
	public boolean flying;
	public float attackProgress;
	public boolean saddled;
	/** T. rex wearing a Dino Collar (1.7.0). */
	public boolean collared;
	/** Collared T. rex ordered to sit. */
	public boolean sitting;
}
