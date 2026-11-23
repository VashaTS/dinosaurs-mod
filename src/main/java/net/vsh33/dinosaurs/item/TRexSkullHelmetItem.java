package net.vsh33.dinosaurs.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

	private static final java.util.UUID HELMET_UUID = java.util.UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150");
	private final com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> modifiers;

	{
		var b = com.google.common.collect.ImmutableMultimap.<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier>builder();
		b.put(Attributes.ARMOR, new AttributeModifier(HELMET_UUID, "Armor modifier", ARMOR, AttributeModifier.Operation.ADDITION));
		b.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(HELMET_UUID, "Armor toughness", TOUGHNESS, AttributeModifier.Operation.ADDITION));
		b.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(HELMET_UUID, "Armor knockback resistance", KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION));
		this.modifiers = b.build();
	}

	@Override
	public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.HEAD ? this.modifiers : super.getDefaultAttributeModifiers(slot);
	}

	@Override
	public EquipmentSlot getEquipmentSlot() {
		return EquipmentSlot.HEAD;
	}

	@Override
	public SoundEvent getEquipSound() {
		return SoundEvents.ARMOR_EQUIP_NETHERITE;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		return this.swapWithEquipmentSlot(this, level, player, hand);
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return true;
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
