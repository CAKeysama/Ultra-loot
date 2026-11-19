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
          MachineStatus / MachineStatistics
energy/   MachineEnergyStorage   FE buffer; external insert-only (rate limited); all-or-nothing consume()
experience/ IExperienceStorage + ExperienceCapability (`ultraloot:experience` BlockCapability)
          ExperienceTank          integer XP points, never negative / above capacity
          ExperienceFluidAdapter  fill-only IFluidHandler for fluids tagged c:experience (20 mB = 1 point)
          ExperienceCapsuleItem / ExperienceMath
inventory/ InventoryScanner       interval scan of loaded chunks' block entities in a cube range
          TransactionalInserter   in-memory simulation + round robin plan, then commit
          RestrictedItemHandler   per-face automation views
upgrade/  UpgradeType / UpgradeCardItem / UpgradeLevels / UpgradeInventory
          UpgradeModifiers        THE modifier service (ticks, attempts, range, orb radius, loot bonus, energy)
loot/     LootProfile (+ JSON Definition codec) / LootProfileManager (reload listener + auto-discovery)
          LootContextType / LootEngine (param sets per context, independent rolls) / LootFilter
mob/      MobCardItem / BlankMobCardItem / MobSimulationData (data map) / MobLootSimulator / SimulatedEntityCache
menu/     MachineMenu (+ Loot/Mob subclasses), TabSlot, MachineAction
network/  MachineSyncPayload (S2C snapshot), MachineActionPayload (C2S request), ModNetwork
registry/ ModBlocks, ModItems, ModBlockEntities (+ capability registration), ModMenus, ModDataComponents, ModCreativeTabs, ModTags
client/   screens, tabs, widgets, colour handlers (client dist only)
gametest/ in-world GameTests
```

## Operation cycle (server only)

```
tickServer
 ├─ preOperationTick      (Loot: rescan every scanIntervalTicks | Mob: XP items 5t, export 10t, orbs 20t)
 ├─ cooldown? -> wait      (set after a blocked operation, BLOCKED_RETRY_TICKS)
 ├─ checkCanOperate        -> status (NO_PROFILE, NO_TARGETS, NO_MOB_CARD, NO_XP, OUTPUT_FULL, ...)
 ├─ energy >= E_final ?    -> else NO_ENERGY (progress frozen)
 ├─ progress++ ; when progress >= ticksPerOperation:
 │     performOperation(E_final)
 │       roll loot (independent attempts, filter)
 │       plan insertion (simulation)  -- fails -> TARGETS_FULL / OUTPUT_FULL, nothing consumed
 │       commit, then consume XP + FE, record statistics
 └─ ACTIVE blockstate with 40-tick hysteresis (no block-update spam)
```

Invariants: FE/XP are consumed only after items are committed; nothing is consumed by a failed operation;
nothing is ever dropped in the world during operation. The Loot Generator keeps items a target rejected after a
successful simulation in a persisted overflow buffer and pauses until it is flushed.

## Energy formula

`E_final = max(E_base(tier) × M_profile/mob × M_speed × M_quantity × M_range × M_loot × M_efficiency, E_base × minEnergyFraction)`
(rounded up, at least 1 FE). See `UpgradeModifiers`.

## Capabilities per face

| Family | Mode | Exposed |
|---|---|---|
| both | ENERGY_INPUT | `Capabilities.EnergyStorage.BLOCK` (insert-only, tier rate limit) |
| Loot | ITEM_TARGET | nothing exposed; adjacent inventories on other faces are not filled |
| Mob | XP_INPUT | `ultraloot:experience`, `Capabilities.FluidHandler.BLOCK` (c:experience fill-only), item handler of the XP slot (insert bottles/capsules, extract empty capsules) |
| Mob | ITEM_OUTPUT | extract-only output view; also active push every 10 ticks |
| null side | — | energy (insert view), Mob: extract-only output, XP storage |

Changing a face calls `level.invalidateCapabilities(pos)`.

## Client/server sync

- Menu slots: vanilla container sync.
- Everything else: `MachineMenu#broadcastChanges` builds `writeGuiData` every 5 ticks and sends a `MachineSyncPayload`
  only when the tag changed. The client menu stores it; screens use typed getters.
- Client actions: `MachineActionPayload` → `MachineMenu#handleAction` validates sender, menu id, distance and
  argument ranges, then calls the block entity, which validates again (allowed modes, Loot Card for filters, tier for profiles).

## Data-driven content

- Loot profiles: `data/<ns>/ultraloot/loot_profiles/*.json` + auto-discovered `chests/*` tables of every namespace
  (`<ns>:auto/<path>`). Tables containing `minecraft:exploration_map` are rejected (expensive structure search).
- Mob costs: data map `ultraloot:mob_simulation` on `entity_type`.
- Tags: `ultraloot:storage_targets`, `ultraloot:scan_blacklist`, `ultraloot:loot_filter/*`, `ultraloot:mob_card_blacklist`.
