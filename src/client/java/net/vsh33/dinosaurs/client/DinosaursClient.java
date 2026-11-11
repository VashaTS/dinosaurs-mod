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
import net.vsh33.dinosaurs.item.ModItems;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

		// One-line descriptions under the new items' names.
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() == ModItems.AMULET_OF_DINO_PROTECTION) {
				lines.add(Component.translatable("item.dinosaurs.amulet_of_dino_protection.desc").withStyle(ChatFormatting.GRAY));
			} else if (stack.getItem() == ModItems.DINOSAUR_SADDLE) {
				lines.add(Component.translatable("item.dinosaurs.dinosaur_saddle.desc").withStyle(ChatFormatting.GRAY));
			}
		});
	}

	private static void register(EntityType<? extends Mob> type, String name, Supplier<LayerDefinition> layer, float scale, float shadow, float walk, boolean swimmer) {
		ModelLayerLocation location = new ModelLayerLocation(Dinosaurs.id(name), "main");
		ModelLayerRegistry.registerModelLayer(location, layer::get);
		EntityRendererRegistry.<Mob>register(type, context -> {
			DinoRenderer renderer = new DinoRenderer(context,
					new DinoModel(context.bakeLayer(location), walk, swimmer),
					Dinosaurs.id("textures/entity/" + name + ".png"), scale, shadow);
			if (type == ModEntities.TRICERATOPS) {
				renderer.withSaddle(Dinosaurs.id("textures/entity/" + name + "_saddle.png"));
			}
			return renderer;
		});
	}
}
