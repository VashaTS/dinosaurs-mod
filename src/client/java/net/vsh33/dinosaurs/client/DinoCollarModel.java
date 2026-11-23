package net.vsh33.dinosaurs.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Dino Collar worn by a collared T. rex (1.7.0), drawn by {@link DinoCollarLayer} with a {@link DinoModel}. Only a "neck"
 * part with the T. rex neck's pivot (see the generated DinoModelLayers.createTRex), so DinoModel animates it exactly like
 * the neck: a studded leather band around the neck, a T-Rex tooth spike on each side and an iron ring hanging under the
 * throat. Texture: textures/entity/t_rex_collar.png (32 x 32, written by tools/gen_collar.py).
 */
public final class DinoCollarModel {
	public static LayerDefinition createTRexCollar() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The T. rex neck box is (-3, -4, -5) 6 x 7 x 6; below y = -1 it is inside the body and in front of z = -4 inside
		// the head, so the band sits on the free upper part of the neck and is inflated to stand out of it.
		PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.5F, -4.0F, -5.5F, 7F, 3F, 7F, new CubeDeformation(0.45F))
				.texOffs(0, 10).addBox(3.9F, -3.0F, -2.5F, 2F, 1F, 1F, new CubeDeformation(0.1F))
				.texOffs(0, 14).addBox(-5.9F, -3.0F, -2.5F, 2F, 1F, 1F, new CubeDeformation(0.1F)), PartPose.offset(0F, 4F, -8F));
		// Iron ring hanging from the left side of the band.
		neck.addOrReplaceChild("collar_ring", CubeListBuilder.create().texOffs(8, 10).addBox(-1F, 0F, -0.5F, 2F, 3F, 1F),
				PartPose.offsetAndRotation(4.2F, -1.6F, -0.5F, 0F, (float) (Math.PI / 2), 0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	private DinoCollarModel() {
	}
}
