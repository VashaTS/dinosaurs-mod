package net.vsh33.dinosaurs.item;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
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
		return BowItem.getPowerForTime(timeHeld * BowItem.MAX_DRAW_DURATION / DRAW_TICKS);
	}

	@Override
	public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
		if (!(entity instanceof Player player)) {
			return false;
		}
		ItemStack projectile = player.getProjectile(itemStack);
		if (projectile.isEmpty()) {
			return false;
		}
		int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
		float pow = getPowerForTime(timeHeld);
		if (pow < 0.1F) {
			return false;
		}
		List<ItemStack> fired = draw(itemStack, projectile, player);
		if (level instanceof ServerLevel serverLevel && !fired.isEmpty()) {
			this.shoot(serverLevel, player, player.getUsedItemHand(), itemStack, fired, pow * VELOCITY, 1.0F, pow == 1.0F, null);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + pow * 0.5F);
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}

	@Override
	public int getDefaultProjectileRange() {
		return 22;
	}
}
