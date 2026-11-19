package io.github.cakeysama.ultraloot.machine;

import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/** Block item of a machine; the tooltip lists the tier's base parameters. */
public class MachineBlockItem extends BlockItem {
    private final MachineBlock machine;

    public MachineBlockItem(MachineBlock block, Properties properties) {
        super(block, properties);
        this.machine = block;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MachineTier tier = machine.tier();
        MachineFamily family = machine.family();
        tooltip.add(Component.translatable("ultraloot.tooltip.tier", tier.roman()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("ultraloot.tooltip.speed", String.format(Locale.ROOT, "%.1fx", tier.speed())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ultraloot.tooltip.energy_capacity", tier.energyCapacity()).withStyle(ChatFormatting.GRAY));
        if (family == MachineFamily.LOOT_GENERATOR) {
            tooltip.add(Component.translatable("ultraloot.tooltip.attempts", family.baseAttempts(tier)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("ultraloot.tooltip.range", Balance.LOOT_BASE_RANGE[tier.index()]).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("ultraloot.tooltip.kills", family.baseAttempts(tier)).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("ultraloot.tooltip.xp_capacity", Balance.MOB_XP_CAPACITY[tier.index()]).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("ultraloot.tooltip." + family.id()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
