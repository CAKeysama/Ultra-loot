package io.github.cakeysama.ultraloot.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * FE buffer of a machine. External code can only insert (rate limited); machines spend energy through
 * {@link #consume(int)}, which is all-or-nothing so an operation is never partially paid.
 */
public class MachineEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public MachineEnergyStorage(int capacity, int maxReceive, Runnable onChanged) {
        super(capacity, maxReceive, 0);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) onChanged.run();
        return received;
    }

    /** Spends exactly {@code amount} FE. @return false (and spends nothing) if not enough is stored. */
    public boolean consume(int amount) {
        if (amount < 0 || energy < amount) return false;
        if (amount > 0) {
            energy -= amount;
            onChanged.run();
        }
        return true;
    }

    public boolean has(int amount) {
        return energy >= amount;
    }

    /** Sets the stored energy (clamped), used when loading and in tests. */
    public void setEnergy(int value) {
        energy = Math.clamp(value, 0, capacity);
    }

    @Override
    public Tag serializeNBT(HolderLookup.Provider provider) {
        return IntTag.valueOf(energy);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, Tag nbt) {
        if (nbt instanceof IntTag intTag) setEnergy(intTag.getAsInt());
    }

    /** Insert-only view used for faces configured as Energy Input. */
    public IEnergyStorage inputView() {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                return MachineEnergyStorage.this.receiveEnergy(toReceive, simulate);
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                return MachineEnergyStorage.this.getEnergyStored();
            }

            @Override
            public int getMaxEnergyStored() {
                return MachineEnergyStorage.this.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }
}
