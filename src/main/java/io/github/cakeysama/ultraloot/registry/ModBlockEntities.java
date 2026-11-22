package io.github.cakeysama.ultraloot.registry;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.experience.ExperienceCapability;
import io.github.cakeysama.ultraloot.experience.ExperienceFluidAdapter;
import io.github.cakeysama.ultraloot.machine.CardRestorerBlockEntity;
import io.github.cakeysama.ultraloot.machine.LootGeneratorBlockEntity;
import io.github.cakeysama.ultraloot.machine.MachineBlock;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MobLootGeneratorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = UltraLoot.MOD_ID)
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UltraLoot.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LootGeneratorBlockEntity>> LOOT_GENERATOR =
            REGISTER.register("loot_generator", () -> BlockEntityType.Builder.of(LootGeneratorBlockEntity::new,
                    ModBlocks.machines(MachineFamily.LOOT_GENERATOR).stream().map(DeferredBlock::get).toArray(MachineBlock[]::new)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MobLootGeneratorBlockEntity>> MOB_LOOT_GENERATOR =
            REGISTER.register("mob_loot_generator", () -> BlockEntityType.Builder.of(MobLootGeneratorBlockEntity::new,
                    ModBlocks.machines(MachineFamily.MOB_LOOT_GENERATOR).stream().map(DeferredBlock::get).toArray(MachineBlock[]::new)).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CardRestorerBlockEntity>> CARD_RESTORER =
            REGISTER.register("card_restorer", () -> BlockEntityType.Builder.of(CardRestorerBlockEntity::new,
                    ModBlocks.CARD_RESTORER.get()).build(null));

    /**
     * Every generator capability is resolved per face from the machine's side configuration. The Card Restorer has no
     * side configuration: every face accepts FE, inserts cards into its input and extracts from its output.
     */
    @SubscribeEvent
    static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CARD_RESTORER.get(), (be, side) -> be.getEnergyCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CARD_RESTORER.get(), (be, side) -> be.getItemCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LOOT_GENERATOR.get(), (be, side) -> be.getEnergyCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MOB_LOOT_GENERATOR.get(), (be, side) -> be.getEnergyCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MOB_LOOT_GENERATOR.get(), (be, side) -> be.getItemCapability(side));
        event.registerBlockEntity(ExperienceCapability.BLOCK, MOB_LOOT_GENERATOR.get(), (be, side) -> be.getExperienceCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MOB_LOOT_GENERATOR.get(), (be, side) -> {
            var xp = be.getExperienceCapability(side);
            return xp == null ? null : new ExperienceFluidAdapter(xp);
        });
    }

    private ModBlockEntities() {}
}
