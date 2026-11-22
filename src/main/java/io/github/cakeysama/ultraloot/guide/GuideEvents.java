package io.github.cakeysama.ultraloot.guide;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Gives each player one Ultra Loot Guide the first time they join a world ({@code guide.giveOnFirstJoin}). */
@EventBusSubscriber(modid = UltraLoot.MOD_ID)
public final class GuideEvents {
    /** Stored in the player's persisted data, which survives death and dimension changes. */
    static final String RECEIVED_KEY = UltraLoot.MOD_ID + ".received_guide";

    private GuideEvents() {}

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Recorded even while the gift is disabled, so turning it on later does not hand books to veteran players.
        if (markReceived(player) && Balance.giveGuideOnFirstJoin) {
            ItemStack guide = new ItemStack(ModItems.GUIDE_BOOK.get());
            if (!player.getInventory().add(guide)) player.drop(guide, false);
            player.sendSystemMessage(Component.translatable("ultraloot.guide.received"));
        }
    }

    /** @return true the first time for this player (and records it), false afterwards. */
    static boolean markReceived(Player player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(RECEIVED_KEY)) return false;
        persisted.putBoolean(RECEIVED_KEY, true);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
        return true;
    }
}
