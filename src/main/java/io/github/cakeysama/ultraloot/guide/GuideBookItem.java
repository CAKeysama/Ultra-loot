package io.github.cakeysama.ultraloot.guide;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;

/**
 * The Ultra Loot Guide. Using it opens the guide screen on the client; the server does nothing (the book is read-only
 * and has no state). The screen is installed by the client entrypoint, so common code never references client classes.
 */
public class GuideBookItem extends Item {
    private static Runnable clientOpener = () -> {};

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    /** Called once by the client entrypoint with the code that opens the guide screen. */
    public static void setClientOpener(Runnable opener) {
        clientOpener = Objects.requireNonNull(opener);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) clientOpener.run();
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("ultraloot.tooltip.guide_book").withStyle(ChatFormatting.GRAY));
    }
}
