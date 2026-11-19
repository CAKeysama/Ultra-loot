package io.github.cakeysama.ultraloot.experience;

/**
 * XP tank of the Mob Loot Generator. Values are always within {@code [0, capacity]}. External insertion is limited
 * per call by {@code maxReceive}; the machine spends XP through {@link #consume(int)} (all-or-nothing).
 */
public class ExperienceTank implements IExperienceStorage {
    private final int capacity;
    private final int maxReceive;
    private final Runnable onChanged;
    private int experience;

    public ExperienceTank(int capacity, int maxReceive, Runnable onChanged) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.onChanged = onChanged;
    }

    @Override
    public int receiveExperience(int points, boolean simulate) {
        return insert(Math.min(points, maxReceive), simulate);
    }

    /** Insertion without the per-call rate limit (used by XP items and orb collection inside the machine). */
    public int insert(int points, boolean simulate) {
        if (points <= 0) return 0;
        int accepted = Math.min(points, capacity - experience);
        if (!simulate && accepted > 0) {
            experience += accepted;
            onChanged.run();
        }
        return accepted;
    }

    /** The tank does not give XP back to automation. */
    @Override
    public int extractExperience(int points, boolean simulate) {
        return 0;
    }

    public boolean consume(int points) {
        if (points < 0 || experience < points) return false;
        if (points > 0) {
            experience -= points;
            onChanged.run();
        }
        return true;
    }

    public int space() {
        return capacity - experience;
    }

    @Override
    public int getExperience() {
        return experience;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public void setExperience(int value) {
        experience = Math.clamp(value, 0, capacity);
    }
}
