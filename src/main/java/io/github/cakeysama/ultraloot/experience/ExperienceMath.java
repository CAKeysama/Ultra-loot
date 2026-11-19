package io.github.cakeysama.ultraloot.experience;

import net.minecraft.world.entity.player.Player;

/** Vanilla XP formulas (points needed to reach a level, total points of a player). */
public final class ExperienceMath {
    private ExperienceMath() {}

    /** Total points needed to go from level 0 to {@code level}. */
    public static int totalForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** Points the player currently has, computed from level and progress (more reliable than totalExperience). */
    public static int playerPoints(Player player) {
        return totalForLevel(player.experienceLevel) + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }
}
