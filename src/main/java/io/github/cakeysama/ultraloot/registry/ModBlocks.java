package io.github.cakeysama.ultraloot.registry;

import io.github.cakeysama.ultraloot.UltraLoot;
import io.github.cakeysama.ultraloot.machine.CardRestorerBlock;
import io.github.cakeysama.ultraloot.machine.MachineBlock;
import io.github.cakeysama.ultraloot.machine.MachineFamily;
import io.github.cakeysama.ultraloot.machine.MachineTier;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(UltraLoot.MOD_ID);

    private static final Map<MachineFamily, Map<MachineTier, DeferredBlock<MachineBlock>>> MACHINES = new EnumMap<>(MachineFamily.class);

    static {
        for (MachineFamily family : MachineFamily.values()) {
            Map<MachineTier, DeferredBlock<MachineBlock>> byTier = new EnumMap<>(MachineTier.class);
            for (MachineTier tier : MachineTier.values()) {
                byTier.put(tier, REGISTER.register(family.blockId(tier), () -> new MachineBlock(family, tier,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(3.5f, 6.0f)
                                .requiresCorrectToolForDrops()
                                .sound(SoundType.METAL)
                                .lightLevel(state -> state.getValue(MachineBlock.ACTIVE) ? 7 : 0))));
            }
            MACHINES.put(family, byTier);
        }
    }

    /** Single-tier machine that repairs worn Mob Cards with FE. */
    public static final DeferredBlock<CardRestorerBlock> CARD_RESTORER = REGISTER.register("card_restorer",
            () -> new CardRestorerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5f, 6.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(MachineBlock.ACTIVE) ? 7 : 0)));

    public static DeferredBlock<MachineBlock> machine(MachineFamily family, MachineTier tier) {
        return MACHINES.get(family).get(tier);
    }

    public static List<DeferredBlock<MachineBlock>> machines(MachineFamily family) {
        return new ArrayList<>(MACHINES.get(family).values());
    }

    public static List<DeferredBlock<MachineBlock>> allMachines() {
        List<DeferredBlock<MachineBlock>> all = new ArrayList<>();
        for (MachineFamily family : MachineFamily.values()) all.addAll(machines(family));
        return all;
    }

    private ModBlocks() {}
}
