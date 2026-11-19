package io.github.cakeysama.ultraloot.upgrade;

import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An upgrade card of one category and level. Cards are shared by every tier of both machine families. */
public class UpgradeCardItem extends Item {
    private final UpgradeType type;
    private final int level;

    public UpgradeCardItem(UpgradeType type, int level, Properties properties) {
        super(properties);
        this.type = type;
        this.level = level;
    }

    public UpgradeType type() {
        return type;
    }

    public int level() {
        return level;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int i = level - 1;
        String base = "ultraloot.card." + type.id();
        switch (type) {
            case SPEED -> {
                tooltip.add(Component.translatable(base + ".effect", format(Balance.SPEED_MULTIPLIER[i])).withStyle(ChatFormatting.AQUA));
                tooltip.add(energy(Balance.SPEED_ENERGY[i]));
            }
            case QUANTITY -> {
                tooltip.add(Component.translatable(base + ".effect", Balance.QUANTITY_MULTIPLIER[i]).withStyle(ChatFormatting.GOLD));
                tooltip.add(energy(Balance.QUANTITY_ENERGY[i]));
            }
            case RANGE -> {
                tooltip.add(Component.translatable(base + ".effect", Balance.RANGE_BONUS[i]).withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.translatable(base + ".effect_mob", Balance.ORB_COLLECT_RADIUS[i]).withStyle(ChatFormatting.DARK_GREEN));
                tooltip.add(energy(Balance.RANGE_ENERGY[i]));
            }
            case EFFICIENCY -> tooltip.add(Component.translatable(base + ".effect",
                    Math.round((1 - Balance.EFFICIENCY_ENERGY[i]) * 100)).withStyle(ChatFormatting.YELLOW));
            case LOOT -> {
                tooltip.add(Component.translatable(base + ".effect", Balance.LOOT_BONUS[i]).withStyle(ChatFormatting.LIGHT_PURPLE));
                tooltip.add(Component.translatable(base + ".effect_mob", Balance.LOOT_BONUS[i]).withStyle(ChatFormatting.DARK_PURPLE));
                tooltip.add(Component.translatable(base + ".filters").withStyle(ChatFormatting.GRAY));
                tooltip.add(energy(Balance.LOOT_ENERGY[i]));
            }
        }
        tooltip.add(Component.translatable("ultraloot.card.slot_rule").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component energy(double multiplier) {
        return Component.translatable("ultraloot.card.energy", format(multiplier)).withStyle(ChatFormatting.RED);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2fx", value);
    }
}
