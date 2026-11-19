package io.github.cakeysama.ultraloot.machine;

import net.minecraft.nbt.CompoundTag;

/** Lifetime counters of a machine. Persisted with the block entity and shown in the Statistics tab. */
public final class MachineStatistics {
    private long itemsGenerated;
    private long operations;
    private long energyConsumed;
    private long experienceUsed;
    private long itemsFiltered;

    public void recordOperation(int energy, int experience, int items, int filtered) {
        operations++;
        energyConsumed += energy;
        experienceUsed += experience;
        itemsGenerated += items;
        itemsFiltered += filtered;
    }

    public long itemsGenerated() {
        return itemsGenerated;
    }

    public long operations() {
        return operations;
    }

    public long energyConsumed() {
        return energyConsumed;
    }

    public long experienceUsed() {
        return experienceUsed;
    }

    public long itemsFiltered() {
        return itemsFiltered;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("items", itemsGenerated);
        tag.putLong("operations", operations);
        tag.putLong("energy", energyConsumed);
        tag.putLong("xp", experienceUsed);
        tag.putLong("filtered", itemsFiltered);
        return tag;
    }

    public void load(CompoundTag tag) {
        itemsGenerated = tag.getLong("items");
        operations = tag.getLong("operations");
        energyConsumed = tag.getLong("energy");
        experienceUsed = tag.getLong("xp");
        itemsFiltered = tag.getLong("filtered");
    }
}
