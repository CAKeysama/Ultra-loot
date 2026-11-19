package io.github.cakeysama.ultraloot.registry;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import io.github.cakeysama.ultraloot.mob.MobCardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UltraLoot.MOD_ID);

    /** Pre-imprinted Mob Cards shown in the creative tab (any other mob can be imprinted with a Blank Mob Card). */
    private static final List<EntityType<?>> SHOWCASE_MOBS = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER, EntityType.ENDERMAN,
            EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.SLIME, EntityType.WITCH, EntityType.GUARDIAN,
            EntityType.MAGMA_CUBE, EntityType.GHAST, EntityType.PIGLIN, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ultraloot"))
            .icon(() -> ModBlocks.machine(MachineFamily.LOOT_GENERATOR, MachineTier.T4).toStack())
            .displayItems((params, output) -> {
                for (DeferredItem<? extends Item> item : ModItems.ordered()) {
                    if (item != ModItems.MOB_CARD) output.accept(item.get());
                }
                for (EntityType<?> mob : SHOWCASE_MOBS) output.accept(MobCardItem.create(mob));
            })
            .build());

    private ModCreativeTabs() {}
}
