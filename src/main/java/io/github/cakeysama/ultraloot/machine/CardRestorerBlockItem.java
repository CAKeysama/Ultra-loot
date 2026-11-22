package io.github.cakeysama.ultraloot.machine;

import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Block item of the Card Restorer; the tooltip explains what it does and its FE buffer. */
public class CardRestorerBlockItem extends BlockItem {
    public CardRestorerBlockItem(CardRestorerBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("ultraloot.tooltip.energy_capacity", Balance.RESTORER_ENERGY_CAPACITY).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ultraloot.tooltip.card_restorer").withStyle(ChatFormatting.DARK_GRAY));
    }
}
