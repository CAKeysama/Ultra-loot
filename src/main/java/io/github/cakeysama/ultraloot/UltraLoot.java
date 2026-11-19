package io.github.cakeysama.ultraloot;

import com.mojang.logging.LogUtils;
import io.github.cakeysama.ultraloot.config.UltraLootConfig;
import io.github.cakeysama.ultraloot.registry.ModBlockEntities;
import io.github.cakeysama.ultraloot.registry.ModBlocks;
import io.github.cakeysama.ultraloot.registry.ModCreativeTabs;
import io.github.cakeysama.ultraloot.registry.ModDataComponents;
import io.github.cakeysama.ultraloot.registry.ModItems;
import io.github.cakeysama.ultraloot.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(UltraLoot.MOD_ID)
public final class UltraLoot {
    public static final String MOD_ID = "ultraloot";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UltraLoot(IEventBus modBus, ModContainer container) {
        ModDataComponents.REGISTER.register(modBus);
        ModBlocks.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModBlockEntities.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        ModCreativeTabs.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, UltraLootConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
