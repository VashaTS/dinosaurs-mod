package net.vsh33.dinosaurs.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Dinosaur Bone Bow: a vanilla bow that draws faster (15 ticks instead of 20) and shoots arrows 1.5x as fast.
 * Arrow damage is speed x base damage, so a fully drawn arrow hits for 9 instead of 6 and flies farther.
 * Enchantments, Infinity, arrows from the inventory and the rest of the bow behaviour are vanilla.
 */
public class DinosaurBoneBowItem extends BowItem {
	/** Ticks to a full draw (vanilla bow: 20). */
	public static final int DRAW_TICKS = 15;
	/** Arrow speed at full draw (vanilla bow: 3.0). */
	public static final float VELOCITY = 4.5F;
	/** Durability (vanilla bow: 384). */
	public static final int DURABILITY = 1152;
	/** Enchantability (vanilla bow: 1, netherite tools: 15). */
	public static final int ENCHANTABILITY = 15;

	public DinosaurBoneBowItem(Properties properties) {
		super(properties);
	}

	/** Same curve as {@link BowItem#getPowerForTime}, reaching 1.0 after {@link #DRAW_TICKS}. */
	public static float getPowerForTime(int timeHeld) {
		return BowItem.getPowerForTime(timeHeld * 20 / DRAW_TICKS);
	}

	/** 1.20.1 BowItem.releaseUsing with the faster draw and {@link #VELOCITY}. */
	@Override
	public void releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
		if (!(entity instanceof Player player)) {
			return;
		}
		boolean infinite = player.getAbilities().instabuild || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, itemStack) > 0;
		ItemStack projectile = player.getProjectile(itemStack);
		if (projectile.isEmpty() && !infinite) {
			return;
		}
		if (projectile.isEmpty()) {
			projectile = new ItemStack(Items.ARROW);
		}
		int timeHeld = this.getUseDuration(itemStack) - remainingTime;
		float pow = getPowerForTime(timeHeld);
		if (pow < 0.1F) {
			return;
		}
		boolean free = infinite && projectile.is(Items.ARROW);
		if (!level.isClientSide) {
			ArrowItem arrowItem = (ArrowItem) (projectile.getItem() instanceof ArrowItem ? projectile.getItem() : Items.ARROW);
			AbstractArrow arrow = arrowItem.createArrow(level, projectile, player);
			arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, pow * VELOCITY, 1.0F);
			if (pow == 1.0F) {
				arrow.setCritArrow(true);
			}
			int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, itemStack);
			if (power > 0) {
				arrow.setBaseDamage(arrow.getBaseDamage() + power * 0.5 + 0.5);
			}
			int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, itemStack);
			if (punch > 0) {
				arrow.setKnockback(punch);
			}
			if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, itemStack) > 0) {
				arrow.setSecondsOnFire(100);
			}
			itemStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
			if (free || player.getAbilities().instabuild && (projectile.is(Items.SPECTRAL_ARROW) || projectile.is(Items.TIPPED_ARROW))) {
				arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			}
			level.addFreshEntity(arrow);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + pow * 0.5F);
		if (!free && !player.getAbilities().instabuild) {
			projectile.shrink(1);
			if (projectile.isEmpty()) {
				player.getInventory().removeItem(projectile);
			}
		}
		player.awardStat(Stats.ITEM_USED.get(this));
	}

	@Override
	public int getEnchantmentValue() {
		return ENCHANTABILITY;
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
		return repair.is(ModItems.DINOSAUR_BONES) || super.isValidRepairItem(stack, repair);
	}

	@Override
	public int getDefaultProjectileRange() {
		return 22;
	}
}
