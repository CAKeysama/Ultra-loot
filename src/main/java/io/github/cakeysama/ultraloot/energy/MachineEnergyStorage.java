package io.github.cakeysama.ultraloot.energy;

import io.github.cakeysama.ultraloot.config.Balance;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * FE buffer of a machine. External code can only insert (rate limited); machines spend energy through
 * {@link #consume(int)}, which is all-or-nothing so an operation is never partially paid.
 * <p>
 * <b>Dynamic capacity.</b> FE is paid in one piece per operation, so the buffer must hold at least one operation.
 * {@link #ensureCapacity(int)} raises the effective capacity to the operation cost when the tier's base capacity is
 * not enough (capped at {@link Balance#MAX_OPERATION_ENERGY}) and returns it to the base when it is. Shrinking never
 * discards stored FE: the reported maximum is never below the stored amount, and no FE is accepted until the stored
 * amount is back under the capacity.
 */
public class MachineEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;
    private final int baseCapacity;

    public MachineEnergyStorage(int capacity, int maxReceive, Runnable onChanged) {
        super(Math.max(0, capacity), Math.max(0, maxReceive), 0);
        this.baseCapacity = Math.max(0, capacity);
        this.onChanged = onChanged;
    }

    /**
     * Sets the effective capacity for an operation costing {@code required} FE.
     *
     * @return true when one operation fits (false only above {@link Balance#MAX_OPERATION_ENERGY})
     */
    public boolean ensureCapacity(int required) {
        // A cost above the cap can never run: keep the base capacity so no FE is pulled in for it.
        int wanted = required > Balance.MAX_OPERATION_ENERGY ? baseCapacity : Math.max(baseCapacity, Math.max(0, required));
        if (wanted != capacity) capacity = wanted;
        return required <= capacity;
    }

    public int baseCapacity() {
        return baseCapacity;
    }

    /** Effective capacity (may be below the stored amount right after it shrank). */
    public int capacity() {
        return capacity;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!canReceive() || toReceive <= 0) return 0;
        int space = Math.max(0, capacity - energy);
        int received = Math.min(space, Math.min(maxReceive, toReceive));
        if (received > 0 && !simulate) {
            energy += received;
            onChanged.run();
        }
        return received;
    }

    /** Never below the stored amount, so networks never see more FE stored than the maximum. */
    @Override
    public int getMaxEnergyStored() {
        return Math.max(capacity, energy);
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

    /**
     * Sets the stored energy, used when loading and in tests. Values are clamped to
     * [0, max(capacity, {@link Balance#MAX_OPERATION_ENERGY})]: a save written with a raised capacity loads without
     * losing FE even before the capacity is recomputed; negative or absurd values are corrected.
     */
    public void setEnergy(int value) {
        energy = Math.clamp(value, 0, Math.max(capacity, Balance.MAX_OPERATION_ENERGY));
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
