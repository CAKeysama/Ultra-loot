package io.github.cakeysama.ultraloot.experience;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Portable XP container. Sneak + use stores the player's own experience; use releases it back. Placed in the
 * Mob Loot Generator's XP slot it is drained into the machine tank. The player always chooses to move their XP.
 */
public class ExperienceCapsuleItem extends Item {
    public ExperienceCapsuleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static int stored(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(ModDataComponents.STORED_XP.get(), 0), 0, Balance.XP_CAPSULE_CAPACITY);
    }

    public static void setStored(ItemStack stack, int points) {
        int value = Math.clamp(points, 0, Balance.XP_CAPSULE_CAPACITY);
        if (value == 0) {
            stack.remove(ModDataComponents.STORED_XP.get());
        } else {
            stack.set(ModDataComponents.STORED_XP.get(), value);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int stored = stored(stack);
            if (player.isShiftKeyDown()) {
                int move = Math.min(ExperienceMath.playerPoints(player), Balance.XP_CAPSULE_CAPACITY - stored);
                if (move <= 0) return InteractionResultHolder.fail(stack);
                player.giveExperiencePoints(-move);
                setStored(stack, stored + move);
            } else {
                if (stored <= 0) return InteractionResultHolder.fail(stack);
                setStored(stack, 0);
                player.giveExperiencePoints(stored);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stored(stack) > 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stored(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * stored(stack) / Balance.XP_CAPSULE_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.color(0.45f, 0.95f, 0.25f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("ultraloot.capsule.stored", stored(stack), Balance.XP_CAPSULE_CAPACITY).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("ultraloot.capsule.hint").withStyle(ChatFormatting.GRAY));
    }
}
