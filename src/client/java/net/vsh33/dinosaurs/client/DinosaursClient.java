package net.vsh33.dinosaurs.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.vsh33.dinosaurs.item.DinosaurBoneBowItem;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.vsh33.dinosaurs.Dinosaurs;
import net.vsh33.dinosaurs.entity.ModEntities;
import net.vsh33.dinosaurs.client.book.GuideBookScreen;
import net.vsh33.dinosaurs.item.GuideBookScreens;
import net.vsh33.dinosaurs.item.ModItems;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

public class DinosaursClient implements ClientModInitializer {
	private static final ModelLayerLocation T_REX_COLLAR = new ModelLayerLocation(Dinosaurs.id("t_rex"), "collar");

	/** The 3D clawed gauntlet, shown in hand (the item model itself is the flat icon). */
	public static final ResourceLocation CLAW_HELD_ID = Dinosaurs.id("item/dinosaur_claw_held");

	@Override
	public void onInitializeClient() {
		ModelLoadingPlugin.register(context -> context.addModels(CLAW_HELD_ID));
		// Bone Bow draw animation (item model overrides): "pulling" while drawn, "pull" 0..1 over its 15-tick draw.
		FabricModelPredicateProviderRegistry.register(ModItems.DINOSAUR_BONE_BOW, Dinosaurs.id("pulling"),
				(stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
		FabricModelPredicateProviderRegistry.register(ModItems.DINOSAUR_BONE_BOW, Dinosaurs.id("pull"),
				(stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0.0F
						: (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / (float) DinosaurBoneBowItem.DRAW_TICKS);
		EntityModelLayerRegistry.registerModelLayer(T_REX_COLLAR, DinoCollarModel::createTRexCollar);
		//        type                          layer                                  scale  shadow walk  swimmer
		register(ModEntities.T_REX, "t_rex", DinoModelLayers::createTRex, 2.0F, 1.2F, 0.6F, false);
		register(ModEntities.VELOCIRAPTOR, "velociraptor", DinoModelLayers::createVelociraptor, 0.8F, 0.4F, 1.0F, false);
		register(ModEntities.SPINOSAURUS, "spinosaurus", DinoModelLayers::createSpinosaurus, 2.0F, 1.2F, 0.6F, false);
		register(ModEntities.TRICERATOPS, "triceratops", DinoModelLayers::createTriceratops, 1.6F, 1.0F, 0.8F, false);
		register(ModEntities.STEGOSAURUS, "stegosaurus", DinoModelLayers::createStegosaurus, 1.6F, 1.0F, 0.8F, false);
		register(ModEntities.ANKYLOSAURUS, "ankylosaurus", DinoModelLayers::createAnkylosaurus, 1.5F, 1.0F, 0.9F, false);
		register(ModEntities.BRACHIOSAURUS, "brachiosaurus", DinoModelLayers::createBrachiosaurus, 1.8F, 1.3F, 0.5F, false);
		register(ModEntities.PARASAUROLOPHUS, "parasaurolophus", DinoModelLayers::createParasaurolophus, 1.4F, 0.8F, 0.8F, false);
		register(ModEntities.MEGALODON, "megalodon", DinoModelLayers::createMegalodon, 4.0F, 0.0F, 1.0F, true); // 1.7.0: 2x (was 2.0)
		register(ModEntities.PTERANODON, "pteranodon", DinoModelLayers::createPteranodon, 1.4F, 0.5F, 1.0F, false);

		// The guide book item lives in the common source set; the screen is client-only.
		GuideBookScreens.setOpener(() -> Minecraft.getInstance().setScreen(new GuideBookScreen()));

		// One-line descriptions under the new items' names.
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() == ModItems.AMULET_OF_DINO_PROTECTION) {
				lines.add(Component.translatable("item.dinosaurs.amulet_of_dino_protection.desc").withStyle(ChatFormatting.GRAY));
			} else if (stack.getItem() == ModItems.DINO_COLLAR) {
				lines.add(Component.translatable("item.dinosaurs.dino_collar.desc").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("item.dinosaurs.dino_collar.desc2").withStyle(ChatFormatting.DARK_GRAY));
			} else if (stack.getItem() == ModItems.DINOSAUR_SADDLE) {
				lines.add(Component.translatable("item.dinosaurs.dinosaur_saddle.desc").withStyle(ChatFormatting.GRAY));
			} else if (stack.getItem() == ModItems.T_REX_SKULL) {
				lines.add(Component.translatable("item.dinosaurs.t_rex_skull.desc").withStyle(ChatFormatting.GRAY));
			} else if (stack.getItem() == ModItems.T_REX_SKULL_HELMET) {
				lines.add(Component.translatable("item.dinosaurs.t_rex_skull_helmet.desc").withStyle(ChatFormatting.GRAY));
			} else if (stack.getItem() == ModItems.DINOSAUR_BONES) {
				lines.add(Component.translatable("item.dinosaurs.dinosaur_bones.desc").withStyle(ChatFormatting.GRAY));
			} else if (stack.getItem() == ModItems.DINOSAUR_BONE_BOW) {
				lines.add(Component.translatable("item.dinosaurs.dinosaur_bone_bow.desc").withStyle(ChatFormatting.GRAY));
			} else if (stack.getItem() == ModItems.DINOSAUR_CLAW) {
				lines.add(Component.translatable("item.dinosaurs.dinosaur_claw.desc").withStyle(ChatFormatting.GRAY));
			} else if (stack.getItem() == ModItems.DINOSAUR_CHESTPLATE || stack.getItem() == ModItems.DINOSAUR_LEGGINGS
					|| stack.getItem() == ModItems.DINOSAUR_BOOTS) {
				lines.add(Component.translatable("item.dinosaurs.dinosaur_armor.desc").withStyle(ChatFormatting.GRAY));
			}
		});
	}

	private static void register(EntityType<? extends Mob> type, String name, Supplier<LayerDefinition> layer, float scale, float shadow, float walk, boolean swimmer) {
		ModelLayerLocation location = new ModelLayerLocation(Dinosaurs.id(name), "main");
		EntityModelLayerRegistry.registerModelLayer(location, layer::get);
		EntityRendererRegistry.<Mob>register(type, context -> {
			DinoRenderer renderer = new DinoRenderer(context,
					new DinoModel(context.bakeLayer(location), walk, swimmer),
					Dinosaurs.id("textures/entity/" + name + ".png"), scale, shadow);
			if (type == ModEntities.TRICERATOPS) {
				renderer.withSaddle(Dinosaurs.id("textures/entity/" + name + "_saddle.png"));
			}
			if (type == ModEntities.T_REX) {
				renderer.withCollar(new DinoModel(context.bakeLayer(T_REX_COLLAR), walk, swimmer),
						Dinosaurs.id("textures/entity/t_rex_collar.png"));
			}
			return renderer;
		});
	}
}
