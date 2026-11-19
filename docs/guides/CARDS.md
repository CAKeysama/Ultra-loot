# Upgrade cards guide

Every machine has five upgrade slots, one per category, so two cards of the same category can never coexist.
Shift-clicking a higher-level card replaces the installed one and returns the old card. Cards are shared by all
tiers of **both** families; the Range and Loot cards have a family-specific effect.

| Card | I | II | III | Energy per operation |
|---|---|---|---|---|
| Speed | 1.25x speed | 1.5x | 2x | x1.15 / x1.35 / x1.75 |
| Quantity | 2x rolls / kills | 3x | 4x | x1.6 / x2.2 / x2.8 |
| Range (Loot Generator) | +8 blocks | +16 | +24 | x1.1 / x1.2 / x1.3 |
| Range (Mob Loot Generator) | collects XP orbs within 2 blocks | 4 | 6 | x1.1 / x1.2 / x1.3 |
| Efficiency | -10% energy | -20% | -35% | x0.9 / x0.8 / x0.65 |
| Loot (Loot Generator) | +1 luck | +2 | +3 | x1.1 / x1.25 / x1.5 |
| Loot (Mob Loot Generator) | Looting I | II | III | x1.1 / x1.25 / x1.5 |

**Quantity** adds independent rolls of the loot table; it never forces stacks beyond what the table rolls.

**Loot Card filters** — any installed Loot Card unlocks the output filter (Loot / Mob Configuration tab):
All, Resources, Food, Equipment. Filters only remove items from what the table produced (they cannot make a table
drop something it doesn't); removed items are destroyed and counted as "filtered". Luck only matters for tables
that use `quality` (e.g. fishing); Looting works exactly as on a player's sword.

## Energy formula

```
E_final = max(E_base x M_profile x M_speed x M_quantity x M_range x M_loot x M_efficiency,
              E_base x 0.25)            // floor: operations are never free
```

`E_base` is the tier value; `M_profile` comes from the loot profile (Loot Generator) or the mob data map
(Mob Loot Generator). The result is rounded up.
