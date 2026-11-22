package io.github.cakeysama.ultraloot.client;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.client.guide.GuideScreen;
import io.github.cakeysama.ultraloot.client.screen.CardRestorerScreen;
import io.github.cakeysama.ultraloot.client.screen.LootGeneratorScreen;
import io.github.cakeysama.ultraloot.client.screen.MobLootGeneratorScreen;
import io.github.cakeysama.ultraloot.guide.GuideBookItem;
import io.github.cakeysama.ultraloot.mob.MobCardItem;
import io.github.cakeysama.ultraloot.registry.ModItems;
import io.github.cakeysama.ultraloot.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only registrations. Nothing in common code references this package. */
@EventBusSubscriber(modid = UltraLoot.MOD_ID, value = Dist.CLIENT)
public final class UltraLootClient {
    private UltraLootClient() {}

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        GuideBookItem.setClientOpener(GuideScreen::open);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.LOOT_GENERATOR.get(), LootGeneratorScreen::new);
        event.register(ModMenus.MOB_LOOT_GENERATOR.get(), MobLootGeneratorScreen::new);
        event.register(ModMenus.CARD_RESTORER.get(), CardRestorerScreen::new);
    }

    /** Mob Card emblem (layer 1) takes the mob's spawn-egg colours. */
    @SubscribeEvent
    static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(MobCardItem::tintColor, ModItems.MOB_CARD.get());
    }
}
