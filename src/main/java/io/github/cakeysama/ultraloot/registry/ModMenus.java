package io.github.cakeysama.ultraloot.registry;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.menu.CardRestorerMenu;
import io.github.cakeysama.ultraloot.menu.LootGeneratorMenu;
import io.github.cakeysama.ultraloot.menu.MobLootGeneratorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(Registries.MENU, UltraLoot.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<LootGeneratorMenu>> LOOT_GENERATOR =
            REGISTER.register("loot_generator", () -> IMenuTypeExtension.create(LootGeneratorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MobLootGeneratorMenu>> MOB_LOOT_GENERATOR =
            REGISTER.register("mob_loot_generator", () -> IMenuTypeExtension.create(MobLootGeneratorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CardRestorerMenu>> CARD_RESTORER =
            REGISTER.register("card_restorer", () -> IMenuTypeExtension.create(CardRestorerMenu::new));

    private ModMenus() {}
}
