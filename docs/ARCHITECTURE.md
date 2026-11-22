# Ultra Loot — Architecture

NeoForge 1.21.1 · Java 21 · mod id `ultraloot` · package `io.github.cakeysama.ultraloot`.

## Module map

```
UltraLoot                     mod entrypoint: registers DeferredRegisters + server config
config/   Balance             every tunable number (tier tables, card tables, floors)
          UltraLootConfig     server config (ModConfigSpec) overriding Balance's non-final fields
machine/  MachineTier         T1..T4 (speed, FE capacity, FE input)
          MachineFamily       LOOT_GENERATOR / MOB_LOOT_GENERATOR (allowed side modes, defaults, E_base, base attempts)
          MachineBlock        one block class for all 8 machines (FACING, ACTIVE), opens the menu, drops contents
          MachineBlockItem    tier tooltip
          AbstractMachineBlockEntity   shared cycle: energy, upgrades, sides, stats, filter, GUI snapshot, persistence
          LootGeneratorBlockEntity     scanner + loot profile + transactional insertion into nearby inventories
          MobLootGeneratorBlockEntity  mob card + XP tank + 27-slot output + export + XP intake
          SideConfiguration / RelativeSide / SideMode   per-face roles stored relative to the facing
          MachineStatus / MachineStatistics   (new statuses are appended: ordinals are part of the GUI sync)
          CardRestorerBlock / CardRestorerBlockEntity / CardRestorerLogic / CardRestorerStatus   Mob Card repair
energy/   MachineEnergyStorage   FE buffer; external insert-only (rate limited); all-or-nothing consume();
                                 dynamic capacity: ensureCapacity(cost) grows it to one operation (cap 100,000,000 FE)
experience/ IExperienceStorage + ExperienceCapability (`ultraloot:experience` BlockCapability)
          ExperienceTank          integer XP points, never negative / above capacity
          ExperienceFluidAdapter  fill-only IFluidHandler for fluids tagged c:experience (20 mB = 1 point)
          ExperienceCapsuleItem / ExperienceMath
inventory/ InventoryScanner       interval scan of loaded chunks' block entities in a cube range; TargetCaches keeps
                                 one BlockCapabilityCache per (pos, side) across scans (dropped on reset/removal)
          TransactionalInserter   in-memory simulation + round robin plan, then commit
          RestrictedItemHandler   per-face automation views
          StackedItemHandler      concatenates handlers slot by slot (XP Input face = XP slot + card insert view)
upgrade/  UpgradeType / UpgradeCardItem / UpgradeLevels / UpgradeInventory
          UpgradeModifiers        THE modifier service (ticks, attempts, range, orb radius, loot bonus, energy)
loot/     LootProfile (+ JSON Definition codec) / LootProfileManager (reload listener + auto-discovery)
          LootContextType / LootEngine (param sets per context, independent rolls) / LootFilter
mob/      MobCardItem / BlankMobCardItem / MobSimulationData (data map: tier, costs, supplemental, fallback)
          HybridLootResolver      native table -> supplemental ADD/REPLACE -> fallback -> Problem (pure, unit tested)
          MobLootSimulator        resolves tables, rolls kills, applies card decay (Rotten Flesh) and the filter
          CardWear                wear component maths: integrity, decay, FE factor, saturation (no overflow)
          SimulatedEntityCache
guide/    GuideBookItem (opens the screen through a client-installed hook) / GuideBookData (book structure,
          parsed from assets/ultraloot/guide/guide.json) / GuideEvents (one guide on first join, persisted flag)
menu/     MachineMenu (+ Loot/Mob subclasses), TabSlot, MachineAction
network/  MachineSyncPayload (S2C snapshot), MachineActionPayload (C2S request), ModNetwork
registry/ ModBlocks, ModItems, ModBlockEntities (+ capability registration), ModMenus, ModDataComponents, ModCreativeTabs, ModTags
client/   screens, tabs, widgets, colour handlers (client dist only); GuiDraw.statusClass = colour + shape per status
          guide/GuideScreen: chapter list + paper page; lays authored pages out into screenfuls (text never cut),
          recipes from the client RecipeManager, read-only (no packets)

src/gametest/   GameTests and their structure template, in their own source set: on the dev run classpaths
                (client, server, gameTestServer) but never in the production JAR. `check` compiles them.
```

## Operation cycle (server only)

```
tickServer
 ├─ preOperationTick      (Loot: rescan every scanIntervalTicks | Mob: XP items 5t, export 10t, orbs 20t)
 ├─ ensureCapacity(E_final) (buffer grows to hold one operation; never shrinks below stored FE)
 ├─ cooldown? -> wait      (set after a blocked operation, BLOCKED_RETRY_TICKS)
 ├─ checkCanOperate        -> status (NO_PROFILE, NO_TARGETS, NO_MOB_CARD, XP_TANK_TOO_SMALL, NO_XP, OUTPUT_FULL, ...)
 ├─ E_final > 100,000,000? -> ENERGY_BUFFER_TOO_SMALL (hard cap; unreachable with shipped data)
 ├─ energy >= E_final ?    -> else NO_ENERGY (progress frozen)
 ├─ progress++ ; when progress >= ticksPerOperation:
 │     performOperation(E_final)
 │       card -> XP -> FE checked (nothing taken yet)
 │       generate: roll loot / simulate kills (resolver problem -> NO_LOOT_SOURCE, retried later)
 │       plan insertion (simulation)  -- fails -> TARGETS_FULL / OUTPUT_FULL, nothing consumed
 │       commit: items, then XP, then FE, then card wear (+1 per kill, mutated in place)
 │       record statistics
 └─ ACTIVE blockstate with 40-tick hysteresis (no block-update spam)
```

Invariants: FE/XP/wear are committed only after items are committed; nothing is consumed and the card is not worn
by a failed operation; nothing is ever dropped in the world during operation. Shrinking the buffer (e.g. a card was
removed) never discards stored FE: capacity reported to networks is `max(capacity, stored)` and receive space is
`max(0, capacity - stored)`, so no FE is lost or accepted beyond the cap. Integer maths for costs is guarded
against overflow (`Balance.MAX_OPERATION_ENERGY`).

### Hybrid loot (Mob Loot Generator)

`HybridLootResolver.resolve` returns the tables to roll for one kill or a `Problem`:

1. supplemental `replace` -> only the supplemental table (must exist);
2. native table exists (and is not `minecraft:empty`) -> native, plus supplemental `add`;
3. native missing -> configured fallback, plus supplemental `add`;
4. otherwise `NO_LOOT_TABLE`, `FALLBACK_MISSING` or `SUPPLEMENTAL_MISSING`.

A native table that exists but rolls nothing is a valid result. Existence is checked with the reloadable loot
table registry. Problems become the red `NO_LOOT_SOURCE` status (reason in the tooltip), one log line per mob and
problem, and Blank Mob Cards refuse to imprint the mob.

### Card wear and the Card Restorer

`ultraloot:card_wear` (int component) counts simulated kills. `CardWear` derives: decay `d = clamp((wear -
integrity) / decaySpan, 0, 1)`, FE factor `1 + d`, and per kill a Rotten Flesh instead of the loot with
probability `d`. Wear saturates at `integrity + decaySpan`. The Card Restorer repairs
`min(wearPerTick, wear, storedFE / costPerWear)` points per tick and pays exactly that, then moves the card to its
output slot when the output is empty (`CardRestorerLogic`, unit tested).

`AbstractMachineBlockEntity#setChanged` only marks the chunk dirty (`Level#blockEntityChanged`): machines have no
comparator output, so the vanilla neighbour notification (`updateNeighbourForOutputSignal`) is skipped. It would
otherwise run on every working tick and every FE transfer. The Loot Generator keeps items a target rejected after a
successful simulation in a persisted overflow buffer and pauses until it is flushed.

## Energy formula

`E_final = max(E_base(tier) × M_profile/mob × M_speed × M_quantity × M_range × M_loot × M_efficiency, E_base × minEnergyFraction)`
(rounded up, at least 1 FE). See `UpgradeModifiers`.

## Capabilities per face

| Family | Mode | Exposed |
|---|---|---|
| both | ENERGY_INPUT | `Capabilities.EnergyStorage.BLOCK` (insert-only, tier rate limit) |
| Loot | ITEM_TARGET | nothing exposed; adjacent inventories on other faces are not filled |
| Mob | XP_INPUT | `ultraloot:experience`, `Capabilities.FluidHandler.BLOCK` (c:experience fill-only), item handler = XP slot (insert bottles/capsules, extract empty capsules) + card slot (insert a bound Mob Card into an empty slot; never extract) |
| Mob | ITEM_OUTPUT | extract-only output view; also active push every 10 ticks |
| null side | — | energy (insert view), Mob: extract-only output, XP storage |
| Card Restorer | every face | energy (insert-only); items: insert bound Mob Cards into the input, extract only from the output |

Changing a face calls `level.invalidateCapabilities(pos)`.

## Client/server sync

- Menu slots: vanilla container sync.
- Everything else: `MachineMenu#broadcastChanges` builds `writeGuiData` every 5 ticks and sends a `MachineSyncPayload`
  only when the tag changed. Large, rarely changing keys (`GuiSyncDelta.STICKY_KEYS`, currently the Loot
  Generator's `profiles` list) are left out when they equal the last sent value; the client keeps its previous copy
  (`GuiSyncDelta.merge`). Other keys are never carried over, so a removed key (e.g. card taken out) disappears.
  The client menu stores it; screens use typed getters.
- Client actions: `MachineActionPayload` → `MachineMenu#handleAction` validates sender, menu id, distance and
  argument ranges, then calls the block entity, which validates again (allowed modes, Loot Card for filters, tier for profiles).

## Data-driven content

- Loot profiles: `data/<ns>/ultraloot/loot_profiles/*.json` + auto-discovered `chests/*` tables of every namespace
  (`<ns>:auto/<path>`). Tables containing `minecraft:exploration_map` are rejected (expensive structure search).
- Mob costs: data map `ultraloot:mob_simulation` on `entity_type`.
- Tags: `ultraloot:storage_targets`, `ultraloot:scan_blacklist`, `ultraloot:loot_filter/*`, `ultraloot:mob_card_blacklist`.
