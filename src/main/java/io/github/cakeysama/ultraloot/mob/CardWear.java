package io.github.cakeysama.ultraloot.mob;

import io.github.cakeysama.ultraloot.config.Balance;
import io.github.cakeysama.ultraloot.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Mob Card wear model (owner decision, docs/GDD.md §3.3).
 * <ul>
 *   <li>Every simulated kill adds 1 wear, committed only together with a successful operation.</li>
 *   <li>Up to {@link Balance#mobCardIntegrity} wear the card is intact: normal cost, normal loot.</li>
 *   <li>Over the next {@link Balance#mobCardDecaySpan} kills it decays linearly: decay {@code d} goes 0 → 1.
 *       Each kill yields one Rotten Flesh instead of its loot with probability {@code d}, and the operation costs
 *       {@code 1 + d × MOB_CARD_DECAY_ENERGY} times the FE.</li>
 *   <li>At {@code d = 1} the card is corrupted: every kill yields one Rotten Flesh at twice the FE. Wear stops
 *       growing; the card never breaks or disappears.</li>
 *   <li>A Card Restorer removes wear for FE; at 0 wear the card works like new.</li>
 * </ul>
 */
public final class CardWear {
    private CardWear() {}

    public static int wear(ItemStack card) {
        Integer value = card.get(ModDataComponents.CARD_WEAR.get());
        return value == null ? 0 : Math.max(0, value);
    }

    /** Wear at which the card is fully corrupted (and stops accumulating). */
    public static int maxWear() {
        return (int) Math.min(Integer.MAX_VALUE, (long) Balance.mobCardIntegrity + Balance.mobCardDecaySpan);
    }

    /** Decay 0 (intact) to 1 (corrupted). */
    public static double decay(int wear) {
        int over = wear - Balance.mobCardIntegrity;
        if (over <= 0) return 0.0;
        return Math.min(1.0, over / (double) Math.max(1, Balance.mobCardDecaySpan));
    }

    public static double decay(ItemStack card) {
        return decay(wear(card));
    }

    /** FE multiplier caused by decay (1.0 for an intact card, 2.0 when corrupted). */
    public static double energyFactor(int wear) {
        return 1.0 + decay(wear) * Balance.MOB_CARD_DECAY_ENERGY;
    }

    /** Remaining quality 0..1 shown on the card's durability bar (1 = brand new, 0 = corrupted). */
    public static double quality(int wear) {
        return 1.0 - Math.min(1.0, wear / (double) Math.max(1, maxWear()));
    }

    /** @return the new wear after {@code kills} more kills (saturates at {@link #maxWear()}, never overflows). */
    public static int addWear(int wear, int kills) {
        long next = (long) Math.max(0, wear) + Math.max(0, kills);
        return (int) Math.min(maxWear(), next);
    }

    public static void setWear(ItemStack card, int wear) {
        if (wear <= 0) {
            card.remove(ModDataComponents.CARD_WEAR.get());
        } else {
            card.set(ModDataComponents.CARD_WEAR.get(), wear);
        }
    }
}
