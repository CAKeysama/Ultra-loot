package io.github.cakeysama.ultraloot.mob;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Right-click a living mob to imprint it: the blank card becomes a Mob Card of that mob. The mob is unharmed. */
public class BlankMobCardItem extends Item {
    public BlankMobCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob) || !MobCardItem.isSupported(target.getType()) || hasNoLoot(target)) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("ultraloot.mob_card.cannot_imprint").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!player.level().isClientSide) {
            ItemStack card = MobCardItem.create(target.getType());
            stack.consume(1, player);
            if (!player.getInventory().add(card)) player.drop(card, false);
            player.level().playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    /**
     * Server only: a mob whose loot sources cannot be resolved (no native table and no fallback, or a missing
     * configured table) would make a card that never produces anything. A native table that exists but rolls
     * nothing is accepted (valid empty result, docs/GDD.md hybrid loot).
     */
    private static boolean hasNoLoot(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return false;
        return !MobLootSimulator.resolve(level, target, MobSimulationData.of(target.getType())).ok();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("ultraloot.blank_mob_card.hint").withStyle(ChatFormatting.GRAY));
    }
}
