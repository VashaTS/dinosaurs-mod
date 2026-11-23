package net.vsh33.dinosaurs.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.vsh33.dinosaurs.Dinosaurs;

/**
 * The Dinosaur Upgrade smithing template. Like the Netherite Upgrade, with its own tooltip and empty-slot icons:
 * the base slot cycles chestplate / leggings / boots, the addition slot the three trophies.
 */
public final class DinosaurUpgradeTemplate {
	private static Component text(String key) {
		return Component.translatable("item.dinosaurs.smithing_template.dinosaur_upgrade." + key);
	}

	public static SmithingTemplateItem create(Item.Properties properties) {
		return new SmithingTemplateItem(
				text("applies_to").copy().withStyle(net.minecraft.ChatFormatting.BLUE),
				text("ingredients").copy().withStyle(net.minecraft.ChatFormatting.BLUE),
				text("upgrade").copy().withStyle(net.minecraft.ChatFormatting.GRAY),
				text("base_slot_description"),
				text("additions_slot_description"),
				List.of(ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"), ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
						ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots")),
				List.of(Dinosaurs.id("item/empty_slot/megalodon_tooth"), Dinosaurs.id("item/empty_slot/parasaurolophus_crest"),
						Dinosaurs.id("item/empty_slot/raptor_claw")));
	}

	private DinosaurUpgradeTemplate() {
	}
}
