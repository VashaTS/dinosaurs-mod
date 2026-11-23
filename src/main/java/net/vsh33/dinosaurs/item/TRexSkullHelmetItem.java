package net.vsh33.dinosaurs.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * T-Rex Skull Helmet. Equipable (not an ArmorItem), so the head layer renders the 3D item model on the head
 * like a carved pumpkin, while the item still gives armor through its attribute modifiers.
 */
public class TRexSkullHelmetItem extends Item implements Equipable {
	public static final int ARMOR = 4;
	public static final float TOUGHNESS = 4.0F;
	public static final float KNOCKBACK_RESISTANCE = 0.2F;
	public static final int ENCHANTABILITY = 20;

	public TRexSkullHelmetItem(Item.Properties properties) {
		super(properties);
	}

	public static ItemAttributeModifiers createAttributes() {
		ResourceLocation id = ResourceLocation.withDefaultNamespace("armor.helmet");
		return ItemAttributeModifiers.builder()
				.add(Attributes.ARMOR, new AttributeModifier(id, ARMOR, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
				.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id, TOUGHNESS, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
				.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id, KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.HEAD)
				.build();
	}

	@Override
	public EquipmentSlot getEquipmentSlot() {
		return EquipmentSlot.HEAD;
	}

	@Override
	public Holder<SoundEvent> getEquipSound() {
		return SoundEvents.ARMOR_EQUIP_NETHERITE;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return this.swapWithEquipmentSlot(this, level, player, hand);
	}

	@Override
	public int getEnchantmentValue() {
		return ENCHANTABILITY;
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
		return repair.is(ModItems.REPAIRS_T_REX_SKULL_HELMET) || super.isValidRepairItem(stack, repair);
	}
}
