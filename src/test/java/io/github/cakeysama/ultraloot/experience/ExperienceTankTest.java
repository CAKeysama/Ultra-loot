package io.github.cakeysama.ultraloot.experience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceTankTest {
    private static final int CAPACITY = 1_000;
    private static final int RATE = 50;

    private final AtomicInteger changes = new AtomicInteger();
    private ExperienceTank tank;

    @BeforeEach
    void setUp() {
        changes.set(0);
        tank = new ExperienceTank(CAPACITY, RATE, changes::incrementAndGet);
    }

    @Test
    void receiveIsRateLimited() {
        assertEquals(RATE, tank.receiveExperience(10_000, false));
        assertEquals(RATE, tank.getExperience());
        assertEquals(1, changes.get());
    }

    @Test
    void receiveSimulateDoesNotChangeState() {
        assertEquals(20, tank.receiveExperience(20, true));
        assertEquals(0, tank.getExperience());
        assertEquals(0, changes.get());
    }

    @Test
    void receiveNeverExceedsCapacity() {
        tank.setExperience(CAPACITY - 7);
        assertEquals(7, tank.receiveExperience(RATE, false));
        assertEquals(CAPACITY, tank.getExperience());
        assertEquals(0, tank.receiveExperience(RATE, false));
        assertEquals(0, tank.space());
    }

    @Test
    void receiveRejectsZeroAndNegative() {
        assertEquals(0, tank.receiveExperience(0, false));
        assertEquals(0, tank.receiveExperience(-100, false));
        assertEquals(0, tank.insert(-100, false));
        assertEquals(0, tank.getExperience());
        assertEquals(0, changes.get());
    }

    @Test
    void internalInsertBypassesRateLimitButNotCapacity() {
        assertEquals(900, tank.insert(900, false));
        assertEquals(100, tank.insert(5_000, false));
        assertEquals(CAPACITY, tank.getExperience());
    }

    @Test
    void extractionIsNeverAllowed() {
        tank.setExperience(500);
        assertFalse(tank.canExtract());
        assertTrue(tank.canReceive());
        assertEquals(0, tank.extractExperience(100, false));
        assertEquals(500, tank.getExperience());
    }

    @Test
    void consumeIsAllOrNothing() {
        tank.setExperience(30);
        assertFalse(tank.consume(31));
        assertEquals(30, tank.getExperience());
        assertTrue(tank.consume(30));
        assertEquals(0, tank.getExperience());
        assertEquals(1, changes.get());
    }

    @Test
    void consumeRejectsNegative() {
        tank.setExperience(30);
        assertFalse(tank.consume(-5));
        assertEquals(30, tank.getExperience());
        assertTrue(tank.consume(0));
        assertEquals(30, tank.getExperience());
    }

    @Test
    void setExperienceClamps() {
        tank.setExperience(-1);
        assertEquals(0, tank.getExperience());
        tank.setExperience(CAPACITY + 1);
        assertEquals(CAPACITY, tank.getExperience());
        assertEquals(CAPACITY, tank.getCapacity());
    }

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new ExperienceTank(0, 1, () -> {}));
    }
}
