package net.vsh33.dinosaurs.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Dinosaur field guide: opens the illustrated guidebook screen on right-click. */
public class GuideBookItem extends Item {
	public GuideBookItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (level.isClientSide()) {
			GuideBookScreens.open();
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(
			final ItemStack itemStack,
			final Item.TooltipContext context,
			final TooltipDisplay display,
			final Consumer<Component> builder,
			final TooltipFlag tooltipFlag) {
		builder.accept(Component.translatable("item.dinosaurs.guide_book.desc").withStyle(ChatFormatting.GRAY));
	}
}
