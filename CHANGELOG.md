# Changelog

All notable changes to Ultra Loot. Format based on [Keep a Changelog](https://keepachangelog.com/); versions follow
[Semantic Versioning](https://semver.org/).

## [0.3.0] — 2026-11-23

Save-compatible with 0.2.0: machines without picks keep filling every inventory in range.

### Added
- **Choose the chests** of a Loot Generator: the Inventory Scan tab has a new **All in range / Picked only** button.
  In Picked only mode a click on a row ticks or unticks that inventory, and loot only goes to ticked ones; chests
  placed later are ignored until picked. Picks are saved as offsets from the machine (`PickedOnly`, `Picked` NBT),
  at most 64 (`Balance.MAX_PICKED_TARGETS`); a double chest counts as picked through either half. The server only
  accepts picks of inventories its last scan listed.
- Scan entry state **Not picked** and machine status **No inventory picked** (`NO_PICKED_TARGETS`, red). New
  statuses, entry states and menu actions are appended, so synced ordinals keep their meaning.
- Guide: new page "Choosing the chests" in the Loot Generator chapter; the red status list mentions the new status.

### Changed
- Inventory Scan tab: the Targets and Range rows moved into the tooltip of the new mode button.

## [0.2.0] — 2026-11-22

Engineering remediation and owner balance decisions. Registry ids, NBT keys and existing data components are
unchanged; worlds from 0.1.x load (new machine status values are appended, so synced ordinals keep their meaning).
Two server config keys were renamed (see Changed).

### Added
- **Mob Card wear** (`ultraloot:card_wear` component): +1 per simulated kill, committed only with a successful
  operation. Intact for 2,048 kills, then decays over 1,024: Rotten Flesh instead of loot with probability equal to
  the decay, FE cost up to 2x. Corrupted cards keep working and never break. Durability bar, tooltip, GUI row.
- **Card Restorer** block (`ultraloot:card_restorer`): repairs worn Mob Cards for FE (8 wear/tick, 40 FE x mob
  energy multiplier per point), input/output slots, automation on every face, GUI, recipe, loot table, pickaxe tag.
- **Hybrid mob loot**: native table first; optional datapack `supplemental` table (`add` / `replace` / `disabled`)
  and `fallback_loot_table` used only when no native table exists. A valid empty table is not a fallback trigger.
- **Mob reward tiers I–III** in the data map (`tier`, `cost_exception`), with FE/XP cost bands; shipped values for
  60 mobs derived from measured loot value (docs/BALANCE.md 7.3).
- Machine status **No loot table for this mob** (`NO_LOOT_SOURCE`, red) with the reason in its tooltip; one log
  line per mob and problem. Blank Mob Cards refuse mobs whose loot cannot be resolved.
- Mob Cards can be inserted by automation through an **XP Input** face (empty card slot only; never extracted).
- **Ultra Loot Guide** (`ultraloot:guide_book`): an in-game book with 13 chapters (47 pages) covering every machine,
  card and status, with crafting grids from the live recipe manager, item icons with tooltips, worn-card examples
  and keyboard/mouse navigation; English and Brazilian Portuguese. Given once on first join
  (`guide.giveOnFirstJoin`, default true); recipe: Book + Redstone Dust. Content is generated from
  `tools/lang/lang_guide.py`; `GuideContentTest` checks every key, item, mob and recipe it references.
- Recipe book unlock advancements for every recipe.
- Status indicators with a shape per class (cyan square processing, gray hollow square inactive, amber "!" warning,
  red "x" blocked) so colour is never the only signal. docs/UI_UX_GUIDELINES.md.
- Dev-only `runClientMekanism` run (Mekanism 10.7.19 from a git-ignored folder; not a dependency).
- Tests: `CardWearTest`, `HybridLootResolverTest`, `TierBalanceTest`, `CardRestorerLogicTest`, new
  `MachineEnergyStorageTest` cases; GameTests for wear, hybrid loot, buffer growth, card automation, orb
  collection, Card Restorer and scanner cache reuse.

### Changed
- **FE buffer is dynamic**: it grows to hold one operation when the tier capacity is too small (cap
  100,000,000 FE). Capacity reported to networks is never below the stored FE; shrinking never discards FE.
- **Tier FE efficiency**: FE per roll 800/720/640/560 and per kill 1,200/1,080/960/840 (tiers I–IV); config keys
  `lootGenerator.energyPerOperation` → `energyPerAttempt`, `mobLootGenerator.energyPerOperation` →
  `energyPerKill` (old keys are no longer read). Profile multipliers rescaled by min tier so FE per roll at the
  profile's own tier is unchanged; auto-profile multiplier 1.5 → 1.67.
- Operation order is card → XP → FE → generate → output fits → commit (items, XP, FE, wear).
- GameTests moved to the `src/gametest` source set and are no longer packaged in the JAR.
- The Loot Generator's inventory scan reuses capability caches between scans (40 → 2 creations in the test).
- Data and asset generators rewrite only files whose content changed and keep the repository line endings;
  `--check` lists out-of-date files (both report 0 on a second run).

### Fixed
- XP orb collection stopped at the first orb that did not fit; it now skips it and keeps collecting smaller orbs.
- Mobs with a valid but empty loot table were refused by Blank Mob Cards.
- A huge configured FE per roll/kill could overflow the base cost to a negative number; it now saturates and the
  machine shows "Cost exceeds FE buffer".
- An operation costing more than the 100,000,000 FE cap no longer raises the buffer to the cap (that FE could never
  be spent).
- The first-join guide flag is recorded even while `guide.giveOnFirstJoin` is off, so enabling it later does not
  hand books to existing players.

## [0.1.1] — 2026-11-20 (unreleased)

Audit release: game design, balance, engineering and QA review. Full report: [docs/AUDIT.md](docs/AUDIT.md).
Save-compatible with 0.1.0: no registry id, NBT key or data component changed.

### Added
- Machine statuses **Cost exceeds FE buffer** (`ENERGY_BUFFER_TOO_SMALL`) and **Cost exceeds XP tank**
  (`XP_TANK_TOO_SMALL`), in English and Brazilian Portuguese.
- 2 GameTests (both new statuses, including recovery after removing a card) and 5 unit tests (`GuiSyncDeltaTest`).

### Changed
- GUI sync leaves the Loot Generator's profile list out of a snapshot when it did not change; the client keeps its
  copy (`menu/GuiSyncDelta`).

### Fixed
- A machine whose cost per operation exceeds its FE buffer (or XP tank) showed "Not enough energy" / "Not enough
  experience" forever, although no amount of power could ever pay it (FE is paid in one piece). It now names the
  real cause and consumes nothing.
- Machines no longer send a comparator neighbour update on every working tick and every FE transfer
  (`setChanged` only marks the chunk dirty; machines have no comparator output).

### Balance
- Buried Treasure profile `energy_multiplier` 2.0 → 3.5: it was the cheapest diamond source in the mod
  (1,600 FE per diamond at Tier II, cheaper than Tier IV End City). Now 2,800 FE, just above the cheapest Tier I
  source (2,735 FE). Derivation: [docs/BALANCE.md](docs/BALANCE.md) B-01.

### Technical
- Removed the machine-specific `org.gradle.java.installations.paths` from `gradle.properties`; the Java 21
  toolchain is detected automatically (or set per user in `~/.gradle/gradle.properties`).
- `TransactionalInserter.freeSlots` counts empty slots in place instead of copying the handler.
- The old Windows error "Failed to find net/minecraft/server/MinecraftServer.class on the classpath" was analysed
  but not reproduced; recovery steps are in docs/AUDIT.md §6. Tests are no longer skipped (`-x test`).

### Documentation
- New: [GDD](docs/GDD.md), [BALANCE](docs/BALANCE.md), [AUDIT](docs/AUDIT.md).
- Updated: README, ARCHITECTURE (cycle, dirty marking, sync), TECHNICAL_DECISIONS (G26, G27), KNOWN_LIMITATIONS,
  MACHINES guide, INSTALLATION, TEST_PLAN, TEST_REPORT, CLAUDE.md, progress.md, tests.json.

## [0.1.0] — 2026-11-19 (unreleased)

First development version for Minecraft 1.21.1 / NeoForge 21.1.

### Added
- **Loot Generator**, tiers I–IV: rolls loot profiles with FE and inserts the result into nearby inventories with
  round robin and all-or-nothing (transactional) insertion; no output inventory, never drops items.
- **Mob Loot Generator**, tiers I–IV: simulates kills of the mob on its Mob Card with FE + stored XP, without spawning
  entities; 27-slot output with manual withdrawal and auto-export through Item Output faces.
- Upgrade cards Speed, Quantity, Range, Efficiency and Loot, levels I–III, one per category, with a central modifier
  service and an energy floor.
- Blank Mob Card / Mob Card (imprint a living mob), Experience Capsule, Blank Card, Machine Frame, Loot Core,
  Soul Core and Logic / Enhanced / Refined / Stellar circuits, with 33 recipes.
- Six-face side configuration (Energy Input, Item Target, XP Input, Item Output, None) with capability invalidation.
- Experience input from bottles, capsules, the `ultraloot:experience` capability, `c:experience` fluids and XP orbs
  (Range card).
- 66 loot profiles (all vanilla chest tables, fishing, archaeology, piglin bartering, 5 Ultra Loot tables) plus
  auto-discovery of other mods' chest tables, gated behind Tier II and 1.5x energy by default.
- Loot filters (All / Resources / Food / Equipment) unlocked by the Loot Card.
- `ultraloot:mob_simulation` data map with XP cost and energy multiplier for 60 vanilla mobs.
- Tabbed GUIs for both machines: Overview, Upgrades, Loot / Mob Configuration, Inventory Scan / Inventory,
  Side Configuration, Statistics.
- Server config `ultraloot-server.toml`.
- English and Brazilian Portuguese translations.
- 124 JUnit test cases and NeoForge GameTests.

### Safety
- Loot tables using `exploration_map` (directly or through nested table references) are never offered as profiles.
- Mobs without a loot table cannot be imprinted on a Mob Card.
- Rolls and simulations that throw are caught; the machine reports Invalid Profile / Invalid Mob instead of crashing.
- Manual rescans from the GUI are throttled to once per second.
