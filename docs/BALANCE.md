# Ultra Loot — balance reference

Version 0.1.1 · audited 2026-11-20. Every number in the code lives in `config/Balance.java` (tier and card
values), in `UltraLootConfig` (server toml), in the loot profile JSONs
(`data/ultraloot/ultraloot/loot_profiles/*.json`), or in the `ultraloot:mob_simulation` data map.
This document records the **measured** values, how they were derived, and every **proposed** change with its
justification. "UNKNOWN" means the number cannot be established from the repository (it needs a playtest or
data from another mod).

## 1. Method

- Values were read from the sources above, not from older docs.
- Expected values (EV) per roll were computed by a script that walks the vanilla loot tables shipped in
  `neoforge-21.1.267-client-extra-aka-minecraft-resources.jar` and the 5 Ultra Loot tables. It models
  `uniform`/`constant`/`binomial` counts, pool rolls, entry weights, `set_count`, `random_chance` and nested
  `loot_table` entries.
- Simplifications of the EV model: conditions it cannot evaluate (killer type, biome and similar) are treated as
  passing. Looting/enchantment bonuses are not included. Two known consequences: creeper music discs and
  magma-cube froglights are counted by the model but never drop in the simulator (KNOWN_LIMITATIONS #3).
- **EV is the average result of one roll. It is not the maximum.** For example, Buried Treasure always gives 1
  diamond and averages 4 emeralds, but one roll can give up to 8 emeralds.
- Feasibility and throughput were computed with the exact cost formula from `UpgradeModifiers`, including the
  `ceil` behaviour.

## 2. Formulas (measured from code)

```
E_final  = ceil( max( E_base(tier) × M × Speed_e × Quantity_e × Range_e × Loot_e × Efficiency_e ,
                      E_base(tier) × M × minEnergyFraction ) )        minEnergyFraction = 0.25
ticks/op = max(MIN_OPERATION_TICKS = 5, round( baseTicks / (TIER_SPEED × Speed_card) ))
attempts = base attempts (Loot) or base kills (Mob) × Quantity
XP/op    = xp_cost(mob) × kills × Quantity          (Mob Loot Generator only)
```

`M` is the profile's `energy_multiplier` (Loot) or the mob's `energy_multiplier` from the data map (Mob).
FE is paid all at once after a successful operation (decision G2). Progress only advances while the buffer holds
`E_final`.

Note: the 0.25 floor can never be reached with the shipped cards. The lowest card product is Efficiency III alone,
0.65. The floor only matters for config or datapack values.

## 3. Tier values (measured)

| Family | Tier | E_base FE | attempts or kills | ticks/op | FE per attempt | attempts/min | FE/t | buffer FE | input FE/t |
|---|---|---|---|---|---|---|---|---|---|
| Loot | I | 800 | 1 | 200 | 800 | 6.0 | 4.0 | 10,000 | 250 |
| Loot | II | 1,600 | 2 | 133 | 800 | 18.0 | 12.0 | 25,000 | 625 |
| Loot | III | 3,000 | 3 | 100 | 1,000 | 36.0 | 30.0 | 50,000 | 1,250 |
| Loot | IV | 6,000 | 4 | 67 | 1,500 | 71.6 | 89.6 | 100,000 | 2,500 |
| Mob | I | 1,200 | 1 | 300 | 1,200 | 4.0 | 4.0 | 10,000 | 250 |
| Mob | II | 2,400 | 1 | 200 | 2,400 | 6.0 | 12.0 | 25,000 | 625 |
| Mob | III | 4,500 | 2 | 150 | 2,250 | 16.0 | 30.0 | 50,000 | 1,250 |
| Mob | IV | 9,000 | 2 | 100 | 4,500 | 24.0 | 90.0 | 100,000 | 2,500 |

The Mob Loot Generator XP tank holds 1,000, 4,000, 12,000 and 40,000 points at tiers I–IV. Its XP input per
transfer is 50, 150, 400 and 1,000. Kills cost the mob's `xp_cost`, which defaults to 8 points (`defaultXpPerKill`).
A Bottle o' Enchanting gives 7 points.

### Upgrade cards (measured)

| Card | Level I | Level II | Level III | Energy factor I / II / III |
|---|---|---|---|---|
| Speed | ×1.25 speed | ×1.5 | ×2 | 1.15 / 1.35 / 1.75 |
| Quantity | ×2 attempts | ×3 | ×4 | 1.6 / 2.2 / 2.8 |
| Range | range or orb radius up | | | 1.1 / 1.2 / 1.3 |
| Efficiency | energy only | | | 0.9 / 0.8 / 0.65 |
| Loot | Looting / filters | | | 1.1 / 1.25 / 1.5 |

With all four non-Efficiency cards at level III, the card product is **9.555**. Adding Efficiency III brings it to
**6.2107**.

## 4. Findings

### P1-01 — Cost above the buffer: permanent deadlock (RESOLVED in 0.2.0 by the dynamic buffer, section 7.2)

Because FE is paid in one piece, an operation whose `E_final` exceeds the tier's FE buffer can **never** run.
Before 0.1.1 the machine showed "Not enough energy" forever, even with a full buffer. That told the player to add
power, which could never help.

This table gives the largest multiplier `M` that still fits the buffer:

| Family | Tier | all level III cards, no Efficiency | all level III cards + Efficiency III |
|---|---|---|---|
| Loot | I | 1.31 | 2.01 |
| Loot | II | 1.64 | 2.52 |
| Loot | III | 1.74 | 2.68 |
| Loot | IV | 1.74 | 2.68 |
| Mob | I | 0.87 | 1.34 |
| Mob | II | 1.09 | 1.68 |
| Mob | III | 1.16 | 1.79 |
| Mob | IV | 1.16 | 1.79 |

These shipped content combinations cannot run with all level III cards and no Efficiency card:

- **Mobs:** 32 of 60 at Tier I (zombie included: 11,466 FE against 10,000), 19 at Tier II, 16 at Tiers III and IV.
  The 16 that cannot run even at Tiers III and IV are blaze, breeze, elder_guardian, enderman, evoker, ghast,
  guardian, magma_cube, phantom, ravager, shulker, vindicator, warden, witch, wither_skeleton and zombified_piglin.
- **Loot profiles:** 23 cannot run even at Tier IV. Examples: ancient_city 200,655 FE,
  trial_chambers_reward_ominous_unique 286,650 FE, bastion_treasure 171,990 FE, and buried_treasure 200,655 FE
  at Tier IV (53,508 FE at Tier II).
- **Without cards, every profile and every mob runs at every tier it is allowed on.**

**Implemented in 0.1.1:** two new statuses, `ENERGY_BUFFER_TOO_SMALL` ("Cost exceeds FE buffer: remove cards or
upgrade tier") and `XP_TANK_TOO_SMALL` ("Cost exceeds XP tank…"). They are shown in red, nothing is consumed,
progress does not advance, and the machine resumes as soon as a card is removed. GameTests cover both cases.

**Not implemented (owner decision; it changes the established design G2 or G1/G20):**

| Option | Effect | Risk |
|---|---|---|
| A. Keep as is (status only) | Card stacks on expensive content are a choice the GUI explains | Players may feel cards are "useless" on high tiers |
| B. Raise buffers, for example to ≥ 3× the most expensive all-card operation | Everything feasible | Makes FE buffering trivially large; changes G1 tier values |
| C. Adaptive buffer: capacity = max(tier capacity, E_final) | Always feasible, no new numbers | Capacity changes with cards; the GUI must show it; edge case of FE above capacity when a card is removed |
| D. Progressive payment (pay per tick) | Feasible | Reverses G2 and adds partial states on blocked operations |

Recommendation: **C**. It keeps G2 (pay once, on success) and every current number.

The worst shipped XP case is warden with Quantity III at Tier I: 250 × 1 × 4 = **1,000 XP**, against a 1,000 tank.
That fits exactly, so the XP status only triggers with datapack or config values.

### B-01 — Buried Treasure was the cheapest diamond and emerald source (CHANGED)

A Buried Treasure roll always contains 1 diamond and a heart of the sea. On average it also gives 4 emeralds,
9.29 iron and 4.64 gold. At its minimum tier (II, 2 rolls per operation, 1,600 FE) with multiplier 2.0, each roll
cost **1,600 FE**, so **1,600 FE per diamond**. It was a Tier II profile that beat the Tier IV End City (4,958 FE
per diamond) by a factor of 3, and the Tier I benchmark by 41%.

FE per diamond, sorted by cost, at each profile's minimum tier with no cards (values after the change):

| Profile | min tier | M | FE per roll | diamonds per roll (EV) | FE per diamond | emeralds per roll (EV) | FE per emerald |
|---|---|---|---|---|---|---|---|
| village_weaponsmith | I | 1.2 | 960 | 0.351 | **2,735** | 0 | — |
| **buried_treasure** (new) | II | **3.5** | 2,800 | 1.000 | **2,800** | 4.000 | 700 |
| trial_chambers_intersection | II | 1.6 | 1,280 | 0.349 | 3,668 | 0 | — |
| archaeology_desert_pyramid | I | 0.6 | 480 | 0.125 | 3,840 | 0.125 | 3,840 |
| jungle_temple | II | 1.4 | 1,120 | 0.273 | 4,103 | 0.182 | 6,154 |
| village_toolsmith | I | 1.2 | 960 | 0.208 | 4,615 | 0 | — |
| nether_bridge | III | 2.0 | 2,000 | 0.411 | 4,866 | 0 | — |
| end_city_treasure | IV | 3.5 | 5,250 | 1.059 | 4,958 | 0.376 | 13,963 |
| abandoned_mineshaft | I | 1.0 | 800 | 0.138 | 5,797 | 0 | — |
| bastion_treasure | IV | 3.0 | 4,500 | 0.600 | 7,500 | 0 | — |
| shipwreck_treasure | II | 1.5 | 1,200 | 0.150 | 8,000 | 3.600 | **333** |
| trial_chambers_reward_ominous | IV | 4.0 | 6,000 | 0.733 | 8,186 | 5.133 | 1,169 |
| village_plains_house | I | 0.8 | 640 | 0 | — | 0.640 | 1,000 |

The rule used is: no profile prices diamonds below the cheapest Tier I source. That source is village_weaponsmith
at 2,735 FE. Solving for M gives at least 3.42. **3.5** is the smallest round value that meets it: 2,800 FE per
diamond. A value of 3.4 would give 2,720, which is below the rule.

| | measured (0.1.0) | proposed and applied (0.1.1) |
|---|---|---|
| buried_treasure `energy_multiplier` | 2.0 | **3.5** |
| FE per diamond (Tier II, no cards) | 1,600 | 2,800 |
| FE per emerald | 400 | 700 (shipwreck_treasure remains the cheapest at 333) |

Save compatibility: profiles are read by id, so existing machines keep the profile and pay the new cost.

### B-02 — Machine XP faucet (checked, no change)

The Mob Loot Generator never outputs XP. Every `xp_cost` in the data map is at least the vanilla kill reward, so
killing mobs is never a net XP gain. Smelting mob drops returns less XP than the kill cost (for example, smelting
beef gives about 0.875 XP against the cow's xp_cost of 4).

Experience bottles from the Loot Generator are a faucet. They convert FE into XP:

| Profile | bottles per roll (EV) | FE per XP point at min tier, no cards |
|---|---|---|
| arcane_vault (Tier III, 2.8) | 2.941 | about 136 (about 95 with Quantity III) |
| pillager_outpost (Tier II, 1.3) | 0.795 | about 187 |
| void_hoard (Tier IV, 3.5) | 1.458 | about 514 |
| ancient_city (Tier IV, 3.5) | 0.523 | about 1,434 |
| shipwreck_treasure (Tier II, 1.5) | 0.15 | about 1,143 |

There is **no closed loop**: the Mob Loot Generator consumes XP but the loop always needs external FE. This
matches the design intent (FE is the universal cost). Whether about 95–187 FE per XP point is "cheap" depends on
the FE generators in the pack, which is **UNKNOWN**.

### B-03 — Tier FE efficiency is inverted (RESOLVED in 0.2.0, see section 7.1)

| Family | FE per attempt, Tier I | Tier II | Tier III | Tier IV |
|---|---|---|---|---|
| Loot | 800 | 800 | 1,000 | 1,500 |
| Mob | 1,200 | 2,400 | 2,250 | 4,500 |

Higher tiers are faster and need fewer blocks, but they cost **more FE per result**. Mob tier efficiency is also
**not monotonic**: Tier II costs more per kill than Tier III. The Mob Loot Generator does not gate mobs by tier, so
several Tier I machines are the most FE-efficient way to farm any mob.

This may be intended: tiers buy throughput per block, not efficiency. G20 does not say either way.

These options are proposed and left for the owner:

1. Flatten mob E_base to 1,200 / 1,200 / 3,000 / 4,500, so FE per kill becomes 1,200 / 1,200 / 1,500 / 2,250.
   That is monotonic and still a premium for speed.
2. Add a `min_tier` to the mob data map (as loot profiles have) for bosses' minions and other high-value mobs.
3. Keep as is and document "higher tiers trade FE efficiency for speed".

### B-04 — Max throughput (reference)

These figures are for a Tier IV Mob Loot Generator with Speed III, Quantity III, Efficiency III and Loot III:

| Mob | kills/min | FE per op | FE/t | XP/t | fits the 100k buffer |
|---|---|---|---|---|---|
| zombie | 192 | 42,998 | 860 | 0.96 | yes |
| blaze | 192 | 64,497 | 1,290 | 3.20 | yes |
| enderman | 192 | 64,497 | 1,290 | 2.88 | yes |
| wither_skeleton | 192 | 107,494 | 2,150 | 6.40 | **no** |
| evoker | 192 | 128,993 | 2,580 (above the 2,500 input cap) | 9.60 | **no** |

The XP item slot can feed at most about 1.4 XP/t with bottles. High-end mob setups need capsules, XP fluid or
the XP capability. That is intended, because it ties the Mob Loot Generator to an XP supply chain.

### Recipes as progression gates (measured)

| Tier | Materials needed |
|---|---|
| I | iron and copper |
| II | diamonds |
| III | netherite ingot and scrap |
| IV | 2 nether stars, shulker shells, dragon's breath, end crystals |

Profile `min_tier` values match these gates. For example, Bastion and Nether Fortress profiles need Tier III, so
nether content needs nether access.

## 5. Measured versus proposed (summary)

| Variable | Measured | Proposed | Status | Justification |
|---|---|---|---|---|
| buried_treasure energy_multiplier | 2.0 | 3.5 | **applied** | B-01: diamond price floor equals the cheapest Tier I source |
| Behaviour when cost exceeds the FE buffer | "Not enough energy" forever | dedicated status | **applied** | P1-01: the old message misled players |
| Behaviour when XP cost exceeds the tank | "Not enough experience" forever | dedicated status | **applied** | same as P1-01, for datapacks and config |
| FE buffer sizing | 10k / 25k / 50k / 100k | option C (adaptive) | **applied in 0.2.0** | P1-01, section 7.2 |
| Mob E_base | 1.2k / 2.4k / 4.5k / 9k | 1.2k / 1.08k / 1.92k / 1.68k | **applied in 0.2.0** | B-03, section 7.1 |
| Mob tiers | none | reward class I-III with cost bands | **applied in 0.2.0** | owner decision, section 7.3 |
| minEnergyFraction | 0.25 | unchanged | — | unreachable with shipped cards; kept as a safety floor |
| Time to reach each tier | UNKNOWN | — | needs playtest | recipe cost is known, playtime is not |
| FE price of one XP point in a real pack | UNKNOWN | — | needs pack data | depends on the generators in the pack |
| Rolls per hour a player finds "fair" | UNKNOWN | — | needs playtest | |

## 6. Exploits checked

| Vector | Result |
|---|---|
| Item duplication on insertion failure | none: simulate then commit, with a persisted overflow buffer (unit tests and GameTests) |
| FE/XP consumed by failed operations | none: paid after commit (GameTests) |
| Mob XP loop | none: B-02 |
| Breaking a machine mid-operation | progress is lost, nothing is paid; FE in the buffer is lost (documented) |
| Swapping profiles or cards mid-progress | progress is clamped or reset; cost is recomputed at completion |
| Capsule rounding | at most a sub-point gain per fill or empty; negligible |
| Exploration maps (lag) | rejected at load |

## 7. Version 0.2.0 rebalance (owner decisions, 2026-11)

### 7.1 Machine tiers: FE efficiency never gets worse with tier

Metric. Within one loot profile or one mob, the expected items and the expected value of one roll or kill do not
depend on the machine tier (the tier only changes speed and rolls per operation). So **FE per roll/kill is the
normalized efficiency**: FE / expected loot and FE / expected loot value both scale with it. The rule is that it
must be non-increasing with tier (`TierBalanceTest.machineFePerAttemptNeverIncreasesWithTier`).

Measured before 0.2.0 (section B-03): Loot 800 / 800 / 1,000 / 1,500 and Mob 1,200 / 2,400 / 2,250 / 4,500 FE per
roll/kill, so higher tiers were up to 3.75x less efficient and the Mob series was not even monotonic.

Applied: every tier is 10% of the Tier I cost cheaper than the tier below (the smallest step that is still clearly
visible in the GUI, no other multiplier involved). E_base = FE per attempt x base attempts.

| Family | Tier | FE per roll/kill | base rolls/kills | E_base | ticks/op | buffer (base) |
|---|---|---:|---:|---:|---:|---:|
| Loot | I | 800 | 1 | 800 | 200 | 10,000 |
| Loot | II | 720 | 2 | 1,440 | 133 | 25,000 |
| Loot | III | 640 | 3 | 1,920 | 100 | 50,000 |
| Loot | IV | 560 | 4 | 2,240 | 67 | 100,000 |
| Mob | I | 1,200 | 1 | 1,200 | 300 | 10,000 |
| Mob | II | 1,080 | 1 | 1,080 | 200 | 25,000 |
| Mob | III | 960 | 2 | 1,920 | 150 | 50,000 |
| Mob | IV | 840 | 2 | 1,680 | 100 | 100,000 |

Profiles. A profile's multiplier was designed against the old FE per roll at its `min_tier`. To keep each
profile's price unchanged at the tier where it unlocks, the generator rescales it by
`old FE per roll(min_tier) / new FE per roll(min_tier)` = 1.0 / 1.111 / 1.5625 / 2.679 for min tier I-IV
(`tools/generate_data.py`, `scaled()`). Above its min tier a profile is now cheaper per roll, never dearer.
Example: ancient_city (min tier IV) 3.5 -> 9.38, so FE per roll stays 9.38 x 560 = 5,250 (before: 3.5 x 1,500).
Auto-discovered profiles (`autoProfileEnergyMultiplier`, min tier II) go from 1.5 to 1.67 for the same reason;
an existing `ultraloot-server.toml` keeps its saved 1.5 until edited (documented in CONFIGURATION.md).

### 7.2 Dynamic FE buffer (replaces the P1-01 deadlock)

Option C was applied: effective capacity = max(tier capacity, FE of one operation), capped at
`Balance.MAX_OPERATION_ENERGY` = 100,000,000. Stored FE is never discarded when the capacity shrinks (the reported
maximum is never below the stored amount; no FE is accepted until it is below the capacity again), saves load
without losing FE, and corrupt values are clamped. `ENERGY_BUFFER_TOO_SMALL` now only appears above the hard cap,
which no shipped content reaches. Every card combination on every profile and mob can run.

### 7.3 Mob tiers

Mobs get a reward class I-III in the data map, assigned from the **measured** expected loot value per kill
(Looting 0, player kill with an iron sword, values from section 1; `tools/balance/mob_ev.py`). Costs rise with
tier in disjoint bands; inside a band they grow linearly with value:

| Mob tier | value per kill | energy multiplier | XP per kill |
|---|---|---|---|
| I | < 5 | 0.5 - 0.9 | 2 - 6 |
| II | 5 - 15 | 1.0 - 1.6 | 8 - 20 |
| III | >= 15 | 1.8 - 4.0 (value capped at 50) | 25 - 120 |

Unlisted and modded mobs default to tier II at 8 XP and 1.0x, the Tier II floor: unknown content is never
cheaper than measured cheap content. `TierBalanceTest` checks that bands are disjoint and that every mob is inside
its band or declares a `cost_exception`.

Explicit exceptions:

- **evoker**: the Totem of Undying makes it worth 127.5 per kill, 2.5x the band cap. Capping it at 4.0 would make it
  the cheapest value in the mod (31 FE per value point). It is priced at 10.0x / 300 XP, about 94 FE per value
  point, the same rate as the top of the Tier III band.
- **wither_skeleton**: 5.5 per kill puts it in Tier II, but its skull gates the Nether Star for Tier IV machines.
  It keeps its pre-0.2 cost 2.5x / 40 XP so skull farming stays a deliberate investment.

FE efficiency per value point falls slightly as tier rises inside the cap (higher tiers pay more per kill but less
per value point), and the mob tier never interacts with the machine tier.

Comparison (machine Tier I and IV FE per kill without cards; FE per value point at machine Tier I; mobs with zero
measured value drop only items that are not in the value table or nothing at Looting 0):

| mob | tier | expected value/kill | FE/kill (machine T-I / T-IV) | XP/kill | expected output/kill | FE per value point (T-I) | XP per value point |
|---|---|---:|---:|---:|---:|---:|---:|
| evoker | III (exception) | 127.50 | 12,000 / 8,400 | 300 | 1.50 items | 94 | 2.35 |
| elder_guardian | III | 43.39 | 4,320 / 3,024 | 102 | 3.06 items | 100 | 2.35 |
| warden | III | 40.00 | 4,020 / 2,814 | 93 | 1.00 items | 100 | 2.33 |
| breeze | III | 24.00 | 2,820 / 1,974 | 49 | 1.50 items | 118 | 2.04 |
| shulker | III | 20.00 | 2,520 / 1,764 | 39 | 0.50 items | 126 | 1.95 |
| witch | III | 15.14 | 2,160 / 1,512 | 25 | 8.00 items | 143 | 1.65 |
| ravager | III | 15.00 | 2,160 / 1,512 | 25 | 1.00 items | 144 | 1.67 |
| ghast | II | 14.00 | 1,860 / 1,302 | 19 | 1.50 items | 133 | 1.36 |
| blaze | II | 8.00 | 1,440 / 1,008 | 12 | 0.50 items | 180 | 1.50 |
| glow_squid | II | 8.00 | 1,440 / 1,008 | 12 | 2.00 items | 180 | 1.50 |
| hoglin | II | 7.50 | 1,380 / 966 | 11 | 3.50 items | 184 | 1.47 |
| vindicator | II | 7.50 | 1,380 / 966 | 11 | 0.50 items | 184 | 1.47 |
| cow | II | 7.00 | 1,320 / 924 | 10 | 3.00 items | 189 | 1.43 |
| mooshroom | II | 7.00 | 1,320 / 924 | 10 | 3.00 items | 189 | 1.43 |
| strider | II | 7.00 | 1,320 / 924 | 10 | 3.50 items | 189 | 1.43 |
| enderman | II | 6.00 | 1,260 / 882 | 9 | 0.50 items | 210 | 1.50 |
| wither_skeleton | II (exception) | 5.50 | 3,000 / 2,100 | 40 | 1.36 items | 545 | 7.27 |
| guardian | II | 5.45 | 1,260 / 882 | 9 | 1.82 items | 231 | 1.65 |
| sheep | II | 5.00 | 1,200 / 840 | 8 | 2.50 items | 240 | 1.60 |
| creeper | I | 4.00 | 960 / 672 | 5 | 1.00 items | 240 | 1.25 |
| phantom | I | 4.00 | 960 / 672 | 5 | 0.50 items | 240 | 1.25 |
| pig | I | 4.00 | 960 / 672 | 5 | 2.00 items | 240 | 1.25 |
| slime | I | 4.00 | 960 / 672 | 5 | 1.00 items | 240 | 1.25 |
| squid | I | 4.00 | 960 / 672 | 5 | 2.00 items | 240 | 1.25 |
| bogged | I | 3.50 | 960 / 672 | 5 | 2.50 items | 274 | 1.43 |
| rabbit | I | 3.50 | 960 / 672 | 5 | 1.60 items | 274 | 1.43 |
| stray | I | 3.50 | 960 / 672 | 5 | 2.50 items | 274 | 1.43 |
| pufferfish | I | 3.05 | 900 / 630 | 4 | 1.05 items | 295 | 1.31 |
| tropical_fish | I | 3.05 | 900 / 630 | 4 | 1.05 items | 295 | 1.31 |
| chicken | I | 3.00 | 900 / 630 | 4 | 2.00 items | 300 | 1.33 |
| donkey | I | 3.00 | 900 / 630 | 4 | 1.00 items | 300 | 1.33 |
| horse | I | 3.00 | 900 / 630 | 4 | 1.00 items | 300 | 1.33 |
| llama | I | 3.00 | 900 / 630 | 4 | 1.00 items | 300 | 1.33 |
| mule | I | 3.00 | 900 / 630 | 4 | 1.00 items | 300 | 1.33 |
| skeleton | I | 3.00 | 900 / 630 | 4 | 2.00 items | 300 | 1.33 |
| trader_llama | I | 3.00 | 900 / 630 | 4 | 1.00 items | 300 | 1.33 |
| cave_spider | I | 2.67 | 840 / 588 | 4 | 1.33 items | 315 | 1.50 |
| spider | I | 2.67 | 840 / 588 | 4 | 1.33 items | 315 | 1.50 |
| cod | I | 2.05 | 780 / 546 | 4 | 1.05 items | 380 | 1.95 |
| salmon | I | 2.05 | 780 / 546 | 4 | 1.05 items | 380 | 1.95 |
| cat | I | 2.00 | 780 / 546 | 4 | 1.00 items | 390 | 2.00 |
| polar_bear | I | 2.00 | 780 / 546 | 4 | 1.00 items | 390 | 2.00 |
| skeleton_horse | I | 2.00 | 780 / 546 | 4 | 1.00 items | 390 | 2.00 |
| zoglin | I | 2.00 | 780 / 546 | 4 | 2.00 items | 390 | 2.00 |
| magma_cube | I | 1.50 | 720 / 504 | 3 | 0.25 items | 480 | 2.00 |
| parrot | I | 1.50 | 720 / 504 | 3 | 1.50 items | 480 | 2.00 |
| zombified_piglin | I | 1.23 | 720 / 504 | 3 | 1.02 items | 588 | 2.45 |
| drowned | I | 1.11 | 720 / 504 | 3 | 1.11 items | 649 | 2.70 |
| husk | I | 1.09 | 720 / 504 | 3 | 1.02 items | 660 | 2.75 |
| zombie | I | 1.09 | 720 / 504 | 3 | 1.02 items | 660 | 2.75 |
| zombie_villager | I | 1.09 | 720 / 504 | 3 | 1.02 items | 660 | 2.75 |
| dolphin | I | 1.00 | 720 / 504 | 3 | 0.50 items | 720 | 3.00 |
| panda | I | 1.00 | 720 / 504 | 3 | 1.00 items | 720 | 3.00 |
| zombie_horse | I | 1.00 | 720 / 504 | 3 | 1.00 items | 720 | 3.00 |
| turtle | I | 0.50 | 660 / 462 | 2 | 1.00 items | 1,320 | 4.00 |
| endermite | I | 0.00 | 600 / 420 | 2 | 0.00 items | n/a (0 value) | n/a |
| fox | I | 0.00 | 600 / 420 | 2 | 0.00 items | n/a (0 value) | n/a |
| goat | I | 0.00 | 600 / 420 | 2 | 0.00 items | n/a (0 value) | n/a |
| ocelot | I | 0.00 | 600 / 420 | 2 | 0.00 items | n/a (0 value) | n/a |
| silverfish | I | 0.00 | 600 / 420 | 2 | 0.00 items | n/a (0 value) | n/a |

### 7.4 Mob Card wear

| Variable | Value | Rationale |
|---|---|---|
| `mobCardIntegrity` | 2,048 kills | about 8.5 hours of a Tier I machine without cards (4 kills/min), about 11 minutes of a Tier IV machine with Speed III + Quantity III (192 kills/min) (`[PLACEHOLDER]`: needs playtest) |
| `mobCardDecaySpan` | 1,024 kills | half the intact life: decay is visible but not abrupt |
| `MOB_CARD_DECAY_ENERGY` | +100% at full decay | owner decision "custa mais por operação" |
| Rotten Flesh chance | equals decay (0 -> 100%) | owner decision: rotten flesh at the end of life |
| Restorer cost | 40 FE x mob energy multiplier per wear point | a full repair of a corrupted zombie card costs 3,072 x 24 = 73,728 FE, the FE of about 102 Tier I zombie kills (720 FE each) |
| Restorer rate | 8 wear/tick, 1,000 FE/t input | full repair in about 19 seconds; rate limited by FE input on expensive mobs |

Sink check: the card wear is an FE sink that scales with use; there is no XP or item sink beyond Rotten Flesh,
which replaces loot rather than adding it. Wear never breaks a card, so no item is ever lost.
