# Ultra Loot 0.1.1 — audit report

Date: 2026-11-20. The audited code is commit `b7e844c` ("Ultra Loot 0.1.0…") plus the uncommitted 0.1.1 working
tree. Four roles were reviewed: game design, balance, engineering, and QA/compatibility. All the work was done in
one session. No subagents were used and nothing was committed.

> **0.2.0 update (2026-11-22):** the owner decided P1-01 (dynamic FE buffer), P2-04/B-03 (tier FE efficiency,
> mob tiers) and "hybrid loot"; P3-02, P3-03, P3-04, P3-06 and P3-07 were fixed. P3-05 stays open (assertion kept).
> Status column below reflects 0.2.0; the narrative sections describe 0.1.1. See CHANGELOG.md.

The `game-design` skill was loaded and followed. The `balance-check` skill **failed to load**: its
`hooks/yaml-helper.sh` file is missing. Its SKILL.md was read and its checklist was applied by hand: outliers,
broken progressions, degenerate strategies, and faucets without sinks. Its optional online version check was
skipped.

## 1. Repository and dependency map

```
build: ModDevGradle 2.0.148 · Gradle 9.2.1 (config cache + build cache) · Java 21 toolchain (foojay resolver)
runtime deps: NeoForge 21.1.267 only (no other mod, no library); test: JUnit 5.11.3 + neoforged testframework

UltraLoot (mod entry)
 ├─ registry/      DeferredRegisters → blocks, items, BEs, menus, data components, creative tab, capabilities, data map type
 ├─ config/        Balance (all numbers) · UltraLootConfig (server toml → Balance)
 ├─ machine/       MachineBlock → AbstractMachineBlockEntity ─┬─ LootGeneratorBlockEntity ── loot/LootEngine ── LootProfileManager (reload listener)
 │                 MachineTier/Family/Status/Statistics        │                              └─ inventory/InventoryScanner + TransactionalInserter
 │                 SideConfiguration/RelativeSide              └─ MobLootGeneratorBlockEntity ── mob/MobLootSimulator ── SimulatedEntityCache
 ├─ energy/        MachineEnergyStorage (Capabilities.EnergyStorage.BLOCK)                        └─ mob/MobSimulationData (data map)
 ├─ experience/    ExperienceTank · ExperienceFluidAdapter (c:experience) · ExperienceCapsuleItem · ExperienceMath · ultraloot:experience capability
 ├─ upgrade/       UpgradeItem · UpgradeInventory · UpgradeModifiers (cost/speed service) · UpgradeLevels
 ├─ menu/ network/ MachineMenu (+ GuiSyncDelta, new) · MachineActionPayload (C2S, validated) · sync payload (S2C)
 ├─ client/        screens, tabs, widgets (Dist.CLIENT only)
 └─ gametest/      UltraLootGameTests (ships in the main source set, inert in production)
data: 66 loot profiles · 5 own loot tables · mob_simulation data map (60 mobs) · 33 recipes · tags · empty.nbt
tools: generate_data.py (profiles/recipes) · lang/gen_lang.py (en_us + pt_br) · validate_assets.py
```

## 2. Game Design Audit

The full document is [GDD.md](GDD.md). Main findings:

- The core loop is coherent: FE (and XP) go in, loot comes out, and the player chooses where it goes. Tier
  recipes gate progression well, and profile `min_tier` values follow the recipe gates.
- Failure feedback was the weakest part. One combination of cards and content locked a machine on a **misleading**
  status (P1-01). That is now fixed with two dedicated statuses.
- The four decisions in the brief were checked against the code (GDD §4). Their terms do not appear anywhere in
  the repo. "Hybrid loot" is ambiguous and needs the owner to confirm what it means.
- Onboarding gap: the recipe book does not list Ultra Loot recipes (P3-03).
- Open design questions are in GDD §6.

## 3. Balance Audit

The full document is [BALANCE.md](BALANCE.md), with measured and proposed values for every change. Main findings:

- P1-01: with full card stacks, 23 profiles and 16 mobs cannot run at **any** tier, because the cost exceeds the
  buffer. The status is fixed; the sizing question is left to the owner.
- B-01: Buried Treasure produced diamonds at 1,600 FE each, cheaper than any other source. Its multiplier went from
  2.0 to 3.5, which gives 2,800 FE per diamond, just above the cheapest Tier I source (2,735 FE).
- B-02: there is no XP loop. Loot Generator bottles are a faucet priced at 95–187 FE per point at best.
- B-03: higher tiers cost more FE per result, and mob tier cost is not monotonic. This is documented, not changed.
- Unreachable rule: the 0.25 energy floor can never trigger with the shipped cards (the lowest card product is 0.65).

## 4. Engineering Audit

| Area | Finding |
|---|---|
| FE | Insert-only external view, rate limit per transfer, consumption all-or-nothing after commit. **Correct.** The deadlock when the cost exceeds the buffer is fixed (P1-01). |
| XP | Points-only tank that can only be filled. The fluid adapter converts whole points and leaves remainders untouched. **Correct.** The same deadlock guard was added for XP. |
| Inventory | Simulate, then commit, with round robin and a persisted overflow buffer. Commit-time rejection is never voided. **Correct.** `freeSlots` copied the whole handler into a virtual inventory just to count empty slots; it now counts in place (P3-01). |
| Dirty marking | `setChanged()` ran vanilla `updateNeighbourForOutputSignal` every working tick and on every FE transfer, but machines have no comparator output. It now only marks the chunk dirty (P2-02). |
| GUI sync | Every 5 ticks the snapshot was re-sent **with the full profile list** (66+ compounds) whenever anything changed, and progress changes every tick. Only changed sticky keys are sent now (P2-03). |
| Mekanism / Thermal / Powah | No hard or soft dependency. These mods use `Capabilities.EnergyStorage.BLOCK` and `Capabilities.ItemHandler.BLOCK`, which the machines expose per face. Mekanism's XP-like fluids only work if they are tagged `c:experience` (configurable mB per point). **Not tested in game** (no such mods in the dev environment). |
| Persistence | Progress, FE, XP, cards, inventories, sides, filter, statistics and the overflow buffer are saved. Not saved: cooldown (at most 20 ticks) and status (recomputed every tick). No registry ids or NBT keys changed in 0.1.1. |
| Performance | Scans run on an interval, with no per-tick world scans. InventoryScanner creates a new `BlockCapabilityCache` for every target on each scan (P3-02): cheap, but it could reuse caches for unchanged positions. |
| Build portability | `gradle.properties` pinned a JDK path that only exists on one machine (`C:/Users/conta/.jdks/jdk-21.0.13+9`, which is no longer present). Removed (P1-02). |

## 5. QA and Compatibility Report

Full results: [TEST_REPORT.md](TEST_REPORT.md) and `tests.json`.

| Check | Result |
|---|---|
| `./gradlew clean build --no-build-cache` | PASS: `build/libs/ultraloot-0.1.1.jar` (360,953 bytes) |
| Unit tests | PASS: 129/129 (5 new `GuiSyncDeltaTest`) |
| GameTests | PASS: 33/33 (2 new). The 2 new tests **FAIL without the fix**, as verified by stashing the fix and re-running: `Expected status to be ENERGY_BUFFER_TOO_SMALL, but was NO_ENERGY` and the XP equivalent |
| Old Windows error "Failed to find net/minecraft/server/MinecraftServer.class on the classpath" | **Not reproduced** in 3 runs (`test`, `clean build`, `clean build --no-build-cache`). See §6 |
| runClient | PASS (smoke only): mod loaded, resource reload finished, 0 ERROR lines; no world opened |
| `tools/validate_assets.py` | NOT RUN: Pillow is not installed on this machine. Assets are unchanged since 0.1.0, where it passed |
| Manual GUI checklist (13 checks) | NOT TESTED: needs a person |
| Mekanism / Thermal / Powah / pipes | NOT TESTED: not available in the dev environment |
| Dedicated multiplayer server with 2+ clients | NOT TESTED |

## 6. The old test failure

FML's dev launch handler (`CommonDevLaunchHandler`) looks up the game jar with
`DevEnvUtils.findFileSystemRootOfFileOnClasspath("net/minecraft/server/MinecraftServer.class")`. That error means
the NeoForge/Minecraft artifact from `build/moddev/artifacts/` was **missing from the test runtime classpath**
when the JVM started.

It did not happen in this session with the same Gradle, MDG and NeoForge versions. The causes consistent with the
evidence are:

1. A `clean` running at the same time, or an interrupted setup, deleted `build/moddev/artifacts` (this happened
   once in this session during an unrelated run).
2. OneDrive locking or dehydrating the artifact jar. The project lives under `OneDrive\Documentos`.
3. A configuration-cache entry pointing at an artifact path that no longer existed.

The earlier workaround `build -x test` hid the problem, so it was not used. If the error comes back, run
`./gradlew --stop`, then `./gradlew clean test --no-configuration-cache`. Consider moving the repo out of the
OneDrive-synced folder or marking `build/` "always keep on this device".

## 7. Prioritized issue list

| Id | Sev. | Description | Evidence | Files | Impact | Solution | Validation | State |
|---|---|---|---|---|---|---|---|---|
| — | P0 | No P0 found (no crash, duplication or world corruption) | full suite plus review | — | — | — | — | — |
| P1-01 | P1 | Operation cost above the FE buffer, or XP cost above the tank, locks the machine on "Not enough energy/experience" | BALANCE §4: 23 profiles and 16 mobs at Tier IV with full cards; zombie at Tier I costs 11,466 FE against a 10,000 buffer | `AbstractMachineBlockEntity`, `MobLootGeneratorBlockEntity`, `MachineStatus`, `GuiDraw`, lang | Machine stuck with a misleading message | New statuses `ENERGY_BUFFER_TOO_SMALL` and `XP_TANK_TOO_SMALL`, shown in red, nothing consumed | 2 GameTests, which fail without the fix | **FIXED** (status). 0.2.0: dynamic FE buffer (BALANCE 7.2) |
| P1-02 | P1 | `gradle.properties` hard-codes a JDK path that exists on one machine only | `org.gradle.java.installations.paths=C:/Users/conta/.jdks/…`; the path no longer exists | `gradle.properties` | Build warning; breaks other machines and CI | Removed; toolchain auto-detection and foojay; a comment points to `~/.gradle/gradle.properties` | build3.log has no warning; build is green | **FIXED** |
| P2-01 | P2 | Buried Treasure diamonds cost 1,600 FE, the cheapest in the mod | BALANCE B-01 | `buried_treasure.json`, `tools/generate_data.py` | Dominant strategy at Tier II | multiplier 2.0 → 3.5 | EV model; GameTests green | **FIXED** |
| P2-02 | P2 | `setChanged` sends neighbour updates every working tick and on every FE transfer | vanilla `BlockEntity#setChanged` calls `updateNeighbourForOutputSignal` | `AbstractMachineBlockEntity` | Useless neighbour updates to cables and chests, several per tick per machine | Override that only marks the chunk dirty (machines have no comparator output) | Persistence GameTests green | **FIXED** |
| P2-03 | P2 | GUI sync re-sends the full profile list on every change | `writeGuiData` adds `profiles`; progress changes every tick | `MachineMenu`, new `GuiSyncDelta` | Network traffic of about 66 compounds every 5 ticks per open GUI | Sticky keys sent only when changed; the client merges them | 5 unit tests | **FIXED** |
| P2-04 | P2 | Higher tiers cost more FE per result; mob Tier II costs more per kill than Tier III; no mob tier gating | BALANCE B-03 | `Balance` | Many Tier I machines beat one Tier IV on FE | Options in BALANCE B-03 | TierBalanceTest | **FIXED in 0.2.0** (BALANCE 7.1, 7.3) |
| P2-05 | P2 | Old Windows test failure (MinecraftServer.class) | earlier report | build environment | CI or developer confusion; tests skipped | Root-cause analysis and recovery steps (§6) | 3 green runs | **DOCUMENTED, not reproduced** |
| P3-01 | P3 | `TransactionalInserter.freeSlots` copied the handler to count empty slots | code | `TransactionalInserter` | Allocations on every scan | Count in place | TransactionalInserter tests | **FIXED** |
| P3-02 | P3 | InventoryScanner creates a new `BlockCapabilityCache` for every target on each scan | `InventoryScanner.java:112` | `InventoryScanner` | Small garbage every 100 ticks | Reuse caches for unchanged positions | GameTest inventoryScanReusesCapabilityCaches | **FIXED in 0.2.0** |
| P3-03 | P3 | Recipe book does not list the recipes (no unlock advancements) | KNOWN_LIMITATIONS #13 | data | Onboarding | Generate `recipes/*` advancements | datagen | **FIXED in 0.2.0** |
| P3-04 | P3 | Orb collection stops at the first orb larger than the free tank space (`break`) | `MobLootGeneratorBlockEntity.java:220` | same | Smaller orbs that would fit are skipped until the next pass | `continue` instead of `break` | 5 orb GameTests | **FIXED in 0.2.0** |
| P3-05 | P3 | `IllegalStateException` if the output handler diverges from its own simulation | `MobLootGeneratorBlockEntity.java:171` | same | Theoretical: own handler, same tick | Keep (it is an assertion), or route items to an overflow buffer | — | OPEN |
| P3-06 | P3 | GameTests and `structure/empty.nbt` ship in the production jar | `gametest/` is in `src/main` | build | Inert, a few KB | Move to a `gametest` source set | JAR content check | **FIXED in 0.2.0** |
| P3-07 | P3 | `tools/generate_data.py` writes LF line endings and rewrites every data file | observed in this session | tools | Noisy diffs with `core.autocrlf` | Write with `newline=''` matching the repo, or add `.gitattributes` | generators `--check` | **FIXED in 0.2.0** (gen_common keeps line endings, writes only changes) |
| P3-08 | P3 | CLAUDE.md, TEST_PLAN and INSTALLATION docs pointed at the removed JDK path and said the system has "only Java 8" | docs | docs | Misleading setup | Updated | — | **FIXED** |
| P3-09 | P3 | The 0.25 energy floor is unreachable with the shipped cards | BALANCE §2 | `Balance` | None (safety floor) | Keep | — | WON'T FIX |
