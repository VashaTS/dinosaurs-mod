package net.vsh33.dinosaurs.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
				text("base_slot_description"),
				text("additions_slot_description"),
				List.of(Identifier.withDefaultNamespace("container/slot/chestplate"), Identifier.withDefaultNamespace("container/slot/leggings"),
						Identifier.withDefaultNamespace("container/slot/boots")),
				List.of(Dinosaurs.id("container/slot/megalodon_tooth"), Dinosaurs.id("container/slot/parasaurolophus_crest"),
						Dinosaurs.id("container/slot/raptor_claw")),
				properties);
	}

	private DinosaurUpgradeTemplate() {
	}
}
