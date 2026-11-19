package io.github.cakeysama.ultraloot.energy;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineEnergyStorageTest {
    private static final int CAPACITY = 10_000;
    private static final int RATE = 250;

    private final AtomicInteger changes = new AtomicInteger();
    private MachineEnergyStorage storage;

    @BeforeEach
    void setUp() {
        changes.set(0);
        storage = new MachineEnergyStorage(CAPACITY, RATE, changes::incrementAndGet);
    }

    @Test
    void receiveIsRateLimitedPerCall() {
        assertEquals(RATE, storage.receiveEnergy(100_000, false));
        assertEquals(RATE, storage.getEnergyStored());
        assertEquals(1, changes.get());
    }

    @Test
    void receiveSimulateDoesNotChangeState() {
        assertEquals(100, storage.receiveEnergy(100, true));
        assertEquals(0, storage.getEnergyStored());
        assertEquals(0, changes.get());
    }

    @Test
    void receiveStopsAtCapacity() {
        storage.setEnergy(CAPACITY - 30);
        assertEquals(30, storage.receiveEnergy(RATE, false));
        assertEquals(CAPACITY, storage.getEnergyStored());
        assertEquals(0, storage.receiveEnergy(RATE, false));
        assertEquals(CAPACITY, storage.getMaxEnergyStored());
    }

    @Test
    void receiveRejectsZeroAndNegative() {
        assertEquals(0, storage.receiveEnergy(0, false));
        assertEquals(0, storage.receiveEnergy(-500, false));
        assertEquals(0, storage.getEnergyStored());
        assertEquals(0, changes.get());
    }

    @Test
    void externalExtractionIsImpossible() {
        storage.setEnergy(5_000);
        assertFalse(storage.canExtract());
        assertEquals(0, storage.extractEnergy(1_000, false));
        assertEquals(5_000, storage.getEnergyStored());
    }

    @Test
    void consumeIsAllOrNothing() {
        storage.setEnergy(1_000);
        assertFalse(storage.consume(1_001));
        assertEquals(1_000, storage.getEnergyStored());
        assertEquals(0, changes.get());

        assertTrue(storage.consume(1_000));
        assertEquals(0, storage.getEnergyStored());
        assertEquals(1, changes.get());
    }

    @Test
    void consumeRejectsNegativeAndAcceptsZero() {
        storage.setEnergy(500);
        assertFalse(storage.consume(-1));
        assertTrue(storage.consume(0));
        assertEquals(500, storage.getEnergyStored());
        assertEquals(0, changes.get(), "zero consumption must not mark the machine dirty");
    }

    @Test
    void hasComparesAgainstStoredEnergy() {
        storage.setEnergy(800);
        assertTrue(storage.has(800));
        assertFalse(storage.has(801));
    }

    @Test
    void setEnergyClamps() {
        storage.setEnergy(-10);
        assertEquals(0, storage.getEnergyStored());
        storage.setEnergy(CAPACITY * 3);
        assertEquals(CAPACITY, storage.getEnergyStored());
    }

    @Test
    void nbtRoundTrip() {
        storage.setEnergy(4_321);
        Tag tag = storage.serializeNBT(null);
        MachineEnergyStorage copy = new MachineEnergyStorage(CAPACITY, RATE, () -> {});
        copy.deserializeNBT(null, tag);
        assertEquals(4_321, copy.getEnergyStored());
    }

    @Test
    void deserializeClampsCorruptValues() {
        storage.deserializeNBT(null, IntTag.valueOf(Integer.MAX_VALUE));
        assertEquals(CAPACITY, storage.getEnergyStored());
        storage.deserializeNBT(null, IntTag.valueOf(-50));
        assertEquals(0, storage.getEnergyStored());
    }

    @Test
    void deserializeIgnoresWrongTagType() {
        storage.setEnergy(77);
        storage.deserializeNBT(null, StringTag.valueOf("garbage"));
        assertEquals(77, storage.getEnergyStored());
    }

    @Test
    void inputViewIsInsertOnly() {
        IEnergyStorage view = storage.inputView();
        assertTrue(view.canReceive());
        assertFalse(view.canExtract());
        assertEquals(RATE, view.receiveEnergy(RATE * 4, false));
        assertEquals(0, view.extractEnergy(100, false));
        assertEquals(RATE, view.getEnergyStored());
        assertEquals(CAPACITY, view.getMaxEnergyStored());
        assertEquals(RATE, storage.getEnergyStored());
    }
}
