# Ultra Loot — Game Design Document

Version 0.1.1 · 2026-11-20 · Minecraft 1.21.1 / NeoForge 21.1.267.
Numbers are in [BALANCE.md](BALANCE.md). Technical design is in [ARCHITECTURE.md](ARCHITECTURE.md). The
decision log, including the gameplay decisions G1–G25, is in [TECHNICAL_DECISIONS.md](TECHNICAL_DECISIONS.md).

Every feature below is marked **[IMPLEMENTED]** (it is in the code and covered by tests) or **[PLANNED]** (it is not
in the code). Nothing marked PLANNED ships in 0.1.1.

## 1. Vision

Ultra Loot turns **power (FE) and experience into loot**. It does this without spawning mobs and without exploring
the same structures again and again. It is a mid-game to late-game tech mod. The player builds machines that
"replay" a loot table, then decides where the loot goes and how to feed the machines.

Player fantasy: *"I automated the dungeon chest / the blaze farm, and I can see exactly what it costs."*

Pillars:
1. **Predictable.** Every cost is shown before it is paid. Nothing is lost or duplicated. A blocked machine says
   why it is blocked.
2. **Honest to vanilla.** Loot comes from the real loot tables, with their real conditions. Cards add rolls,
   never stack-size multipliers.
3. **Automation-friendly.** Standard NeoForge capabilities, per-face configuration, and no hard dependency on any
   tech mod.

## 2. Core loops

| Loop | Time scale | Player action | Reward |
|---|---|---|---|
| Moment to moment | seconds | Check the status line, add cards, empty outputs | Items flowing into chests |
| Session | 30–120 min | Build or upgrade a tier, choose profiles and mobs, wire FE and XP, configure faces | A new resource stream (for example, nether loot after Tier III) |
| Long term | days | Climb tiers I→IV (iron, then diamonds, then netherite, then nether stars), unlock higher-tier profiles, scale XP supply | Late-game loot (ominous vaults, ancient city, end city) without combat |

## 3. Mechanics

### 3.1 Loot Generator [IMPLEMENTED]

- **Purpose:** turn FE into chest, fishing, archaeology or barter loot.
- **Player experience goal:** you choose what to farm, and it lands in your storage.
- **Inputs:** FE through Energy Input faces; the selected loot profile; upgrade cards.
- **Outputs:** items inserted directly into inventories within range, round robin and transactional. The machine
  has no output inventory of its own.
- **Edge cases:**
  - Adjacent inventories are only used through Item Target faces (G5).
  - Double chests count once.
  - Other Ultra Loot machines are never targets.
  - A modded inventory that rejects items at commit time sends them to the persisted overflow buffer (G4).
- **Failure states, shown as a status:** No profile; Profile needs a higher tier; Loot profile unavailable;
  No inventories in range; Inventories are full; Not enough energy; **Cost exceeds FE buffer (new in 0.1.1)**.

### 3.2 Mob Loot Generator [IMPLEMENTED]

- **Purpose:** turn FE plus XP into a mob's loot-table drops without spawning the mob.
- **Player experience goal:** a mob farm that fits in one block and costs XP, so XP becomes a resource worth
  automating.
- **Inputs:** FE; XP from the XP slot (bottles, capsules), the XP capability, `c:experience` fluids or orbs
  (Range Card); one Mob Card (by hand, or through an XP Input face when the card slot is empty); upgrade cards.
- **Outputs:** drops go to a 27-slot output. Players can take items out but not put them in. Automation can
  extract, and Item Output faces push items out on their own (G19). Automation can never extract the Mob Card.
- **Atomic operation order:** card → XP → FE → simulate → output fits → commit (items, then XP, FE and card wear).
  A failure at any step consumes nothing and leaves the card unchanged.
- **Edge cases:**
  - Swapping the card resets progress; card wear changing does not.
  - Mobs whose loot cannot be resolved (§3.2.2) cannot be imprinted.
  - A native loot table that exists but rolls nothing is valid: the machine runs and produces nothing.
  - Drops that need a specific killer do not drop (KNOWN_LIMITATIONS #3).
- **Failure states:** Insert a Mob Card (gray); Not enough energy / Not enough experience / Output is full (amber);
  This mob cannot be simulated / No loot table for this mob / Cost exceeds XP tank (red). Cost exceeds FE buffer
  only above 100,000,000 FE (§3.3).

#### 3.2.1 Mob Card wear [IMPLEMENTED in 0.2.0, owner decision]

- **Purpose:** an FE sink that scales with use, and a reason to come back to a running farm.
- **Player experience goal:** "my card's data is wearing out": visible, gradual, reversible, never punishing.
- **Rules:** every simulated kill adds 1 wear, committed only with a successful operation. Intact for
  `mobCard.integrity` kills (2,048): normal cost and loot. Then decay `d` rises linearly 0 → 1 over
  `mobCard.decaySpan` kills (1,024): each kill yields one Rotten Flesh instead of its loot with probability `d`,
  and FE per operation is multiplied by `1 + d`. At `d = 1` the card is corrupted: only Rotten Flesh, at 2× FE.
  Wear then stops growing. A card never breaks or disappears. XP cost is not affected.
- **Feedback:** durability bar on the item (hidden while new; green intact, amber decaying, red corrupted),
  tooltip with integrity %, decay effects and a repair hint, an integrity row in the Mob config tab.
- **Edge cases:** wear saturates without integer overflow; corrupted cards still run, so a forgotten farm keeps
  producing Rotten Flesh instead of jamming.

#### 3.2.2 Hybrid loot [IMPLEMENTED in 0.2.0, owner decision]

"Hybrid" = native loot table + explicit Ultra Loot extension + controlled fallback:

1. The mob's **native** table (vanilla or modded) is the primary source whenever it exists. A valid empty table is
   a valid result, never a fallback trigger.
2. A **supplemental** table may be configured per mob in the data map: `add` (rolled in addition to the primary
   table on every kill), `replace` (rolled instead of it) or `disabled`. Nothing is added automatically.
3. A **fallback** table may be configured and is used only when the native table does not exist.
4. A configured table that does not exist is a configuration error: red status with the reason in its tooltip, one
   log line per mob and problem, nothing consumed, and Blank Mob Cards refuse to imprint the mob.

#### 3.2.3 Card Restorer [IMPLEMENTED in 0.2.0, owner decision]

- **Purpose:** the repair half of card wear: restores a worn Mob Card for FE.
- **Inputs:** FE (any face), one worn Mob Card (input slot, by hand or automation). **Output:** the repaired card
  (output slot, extractable by automation).
- **Rules:** each tick repairs up to `cardRestorer.wearPerTick` (8) wear points at `cardRestorer.energyPerWear`
  (40) FE × the mob's energy multiplier per point, paid per repaired point (no FE spent without repair). At 0 wear
  the card moves to the output and works like new.
- **Failure states:** idle (no card, gray), not enough energy (amber), output full (amber).

### 3.3 Tiers [IMPLEMENTED]

Each family has 4 tiers. A higher tier gives more speed, more attempts per operation, a bigger buffer and range,
and access to higher-tier profiles. Recipes gate the tiers: iron, then diamonds, then netherite, then nether stars.
Since 0.2.0 FE per roll or kill is non-increasing with tier (800/720/640/560 and 1,200/1,080/960/840), so a higher
tier is always at least as efficient (BALANCE §7.1). The FE buffer grows to hold one operation whenever the tier
capacity is too small (BALANCE §7.2), so every card combination can run.

Mobs have their own reward tiers I–III (data map `tier`), assigned from measured loot value; FE and XP costs rise
with mob tier in disjoint bands, with two documented exceptions (BALANCE §7.3). Mob tiers are independent from
machine tiers: any machine can simulate any supported mob.

### 3.4 Upgrade cards [IMPLEMENTED]

There are 5 categories, each at levels I–III, with one slot per category (G12, G13):

| Card | Effect |
|---|---|
| Speed | Faster cycle |
| Quantity | Extra independent rolls or kills (G9) |
| Range | Larger scan cube on the Loot Generator; collects orbs on the Mob Loot Generator (G11) |
| Efficiency | Lower energy cost |
| Loot | Luck or Looting, and unlocks filters (G10) |

Every card except Efficiency raises the energy cost. With the dynamic buffer no stack is ever blocked by FE; only
the XP tank can still be too small for a Quantity Card on expensive mobs at Tier I (red status).

### 3.5 Loot profiles [IMPLEMENTED]

There are 66 shipped profiles plus auto-discovered `chests/*` tables from other mods. Auto-discovered profiles
need Tier II and cost 1.67× energy (G7, rescaled in 0.2.0 to keep their FE per roll at Tier II). Each profile has a
`min_tier` and an `energy_multiplier`. Tables that use exploration maps are rejected.

### 3.6 Experience [IMPLEMENTED]

XP is stored as **points in a tank**. Automation can only fill it. It is never exposed as a fluid, and the machine
never drains a player (G17).

The **Experience Capsule** stores up to 30 levels (1,395 points). The player fills it on purpose (sneak + use).
The Range Card's orb collection skips orbs too large for the remaining space and keeps collecting smaller ones.

### 3.7 Side configuration [IMPLEMENTED]

There are six relative faces. Each face can be set to Energy Input, Item Target, XP Input, Item Output or None.
The modes on offer depend on the machine family (G22, G25). XP Input faces also accept a Mob Card into an empty
card slot.

### 3.8 GUI [IMPLEMENTED]

Each machine has a tabbed GUI: Overview, Upgrades, Loot or Mob configuration, Inventory scan or Inventory, Sides
and Statistics. The server owns all state; the client only renders snapshots (G23). Visual language, colour and
status semantics, tooltip inventory and the validation checklist: docs/UI_UX_GUIDELINES.md.

### 3.9 Planned / not in the code [PLANNED]

- JEI/EMI integration pages (none exist; vanilla recipe book unlocks exist since 0.2.0).
- Tested compatibility with Thermal and Powah (Mekanism is covered by the 0.2.0 dev-client test, TEST_REPORT).

## 4. Earlier design decisions (confirmed by the owner in 0.2.0)

| Decision | What the code does |
|---|---|
| XP: tank-based storage | `ExperienceTank` stores points (1k–40k by tier), can only be filled, has no output, exposes the `ultraloot:experience` capability and a fill-only fluid adapter |
| Mob Cards: single-card system | One Mob Card slot (stack limit 1). The card is not consumed; it wears with use and is repaired in the Card Restorer (§3.2.1, §3.2.3) |
| Loot: hybrid | Native table + explicit supplemental ADD/REPLACE/disabled + fallback only without a native table (§3.2.2) |
| Exportation: both | The Mob Loot Generator supports manual removal and automatic export (Item Output faces, pipes) at the same time. The Loot Generator inserts directly into inventories (G3) |

## 5. Onboarding

1. Craft a Tier I Loot Generator (iron and copper), place it next to a chest, feed it FE and choose a profile.
   The status line names every blocker, with a shape and colour for its class.
2. Craft a Blank Mob Card and use it on a cow, then put it in a Mob Loot Generator with some bottles.
3. Recipes unlock in the vanilla recipe book when the player picks up a key ingredient (0.2.0).

Onboarding targets (such as how many new players set up a first working machine without help) are **UNKNOWN**
until a playtest.

## 6. Open questions for the owner

1. Should the Loot Generator get an optional output buffer, so that "export both" also applies to it? That would
   change G3.
2. Card wear numbers (2,048 / 1,024 kills, repair price) are `[PLACEHOLDER]` until playtested.
