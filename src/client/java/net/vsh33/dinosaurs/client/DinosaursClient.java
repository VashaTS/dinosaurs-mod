package net.vsh33.dinosaurs.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.entity.ModEntities;
import java.util.function.Supplier;

public class DinosaursClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		//        type                          layer                                  scale  shadow walk  swimmer
		register(ModEntities.T_REX, "t_rex", DinoModelLayers::createTRex, 2.0F, 1.2F, 0.6F, false);
		register(ModEntities.VELOCIRAPTOR, "velociraptor", DinoModelLayers::createVelociraptor, 0.8F, 0.4F, 1.0F, false);
		register(ModEntities.SPINOSAURUS, "spinosaurus", DinoModelLayers::createSpinosaurus, 2.0F, 1.2F, 0.6F, false);
		register(ModEntities.TRICERATOPS, "triceratops", DinoModelLayers::createTriceratops, 1.6F, 1.0F, 0.8F, false);
		register(ModEntities.STEGOSAURUS, "stegosaurus", DinoModelLayers::createStegosaurus, 1.6F, 1.0F, 0.8F, false);
		register(ModEntities.ANKYLOSAURUS, "ankylosaurus", DinoModelLayers::createAnkylosaurus, 1.5F, 1.0F, 0.9F, false);
		register(ModEntities.BRACHIOSAURUS, "brachiosaurus", DinoModelLayers::createBrachiosaurus, 1.8F, 1.3F, 0.5F, false);
		register(ModEntities.PARASAUROLOPHUS, "parasaurolophus", DinoModelLayers::createParasaurolophus, 1.4F, 0.8F, 0.8F, false);
		register(ModEntities.MEGALODON, "megalodon", DinoModelLayers::createMegalodon, 2.0F, 0.0F, 1.0F, true);
		register(ModEntities.PTERANODON, "pteranodon", DinoModelLayers::createPteranodon, 1.4F, 0.5F, 1.0F, false);
	}

	private static void register(EntityType<? extends Mob> type, String name, Supplier<LayerDefinition> layer, float scale, float shadow, float walk, boolean swimmer) {
		ModelLayerLocation location = new ModelLayerLocation(Dinosaurs.id(name), "main");
		ModelLayerRegistry.registerModelLayer(location, layer::get);
		EntityRendererRegistry.<Mob>register(type, context -> new DinoRenderer(context,
				new DinoModel(context.bakeLayer(location), walk, swimmer),
				Dinosaurs.id("textures/entity/" + name + ".png"), scale, shadow));
	}
}
