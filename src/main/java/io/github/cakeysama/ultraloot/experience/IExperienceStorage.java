package io.github.cakeysama.ultraloot.experience;

/**
 * Storage of experience as integer points. Exposed as the {@code ultraloot:experience} block capability so other
 * mods can push XP without converting it to a fluid.
 */
public interface IExperienceStorage {
    /** @return points actually accepted (never negative, never above the free space). */
    int receiveExperience(int points, boolean simulate);

    /** @return points actually removed. */
    int extractExperience(int points, boolean simulate);

    int getExperience();

    int getCapacity();

    default boolean canReceive() {
        return true;
    }

    default boolean canExtract() {
        return false;
    }
}
