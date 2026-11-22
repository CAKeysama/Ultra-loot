package io.github.cakeysama.ultraloot.machine;

/**
 * Pure repair math of the Card Restorer (no Minecraft state, unit tested).
 * <p>
 * Every tick the restorer removes up to {@code wearPerTick} wear points, but only as many as the stored FE pays for in
 * full: a tick never pays for a point it does not repair and never repairs a point it did not pay for.
 */
public final class CardRestorerLogic {
    private CardRestorerLogic() {}

    /** Outcome of one tick: {@code repaired} wear points for exactly {@code energy} FE. */
    public record Step(int repaired, long energy) {
        public static final Step NONE = new Step(0, 0L);
    }

    /**
     * FE per wear point: {@code ceil(energyPerWear * multiplier)}, at least 1. Saturates instead of overflowing; a
     * negative or NaN product counts as the minimum.
     */
    public static long costPerWear(double energyPerWear, double multiplier) {
        double raw = Math.ceil(energyPerWear * multiplier);
        if (Double.isNaN(raw) || raw < 1.0) return 1L;
        if (raw >= (double) Long.MAX_VALUE) return Long.MAX_VALUE;
        return (long) raw;
    }

    /**
     * @param wear         current wear of the card
     * @param storedEnergy FE in the buffer
     * @param costPerWear  FE per wear point (values below 1 count as 1)
     * @param wearPerTick  maximum points repaired per tick
     * @return the points to repair and their exact FE cost ({@code repaired * costPerWear <= storedEnergy})
     */
    public static Step repair(int wear, long storedEnergy, long costPerWear, int wearPerTick) {
        if (wear <= 0 || wearPerTick <= 0 || storedEnergy <= 0) return Step.NONE;
        long cost = Math.max(1L, costPerWear);
        long affordable = storedEnergy / cost;
        long repaired = Math.min(Math.min((long) wearPerTick, (long) wear), affordable);
        if (repaired <= 0) return Step.NONE;
        // repaired <= storedEnergy / cost, so the product cannot exceed storedEnergy (no overflow).
        return new Step((int) repaired, repaired * cost);
    }

    /** FE still needed to remove all of {@code wear} (saturating). */
    public static long energyToFinish(int wear, long costPerWear) {
        if (wear <= 0) return 0L;
        long cost = Math.max(1L, costPerWear);
        return cost > Long.MAX_VALUE / wear ? Long.MAX_VALUE : cost * wear;
    }

    /** Ticks still needed to remove all of {@code wear} at full power. */
    public static int ticksToFinish(int wear, int wearPerTick) {
        if (wear <= 0) return 0;
        int rate = Math.max(1, wearPerTick);
        return (int) (((long) wear + rate - 1) / rate);
    }
}
