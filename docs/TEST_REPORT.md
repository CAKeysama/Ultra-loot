# Ultra Loot 0.2.0 — test report

Date: 2026-11-22 · Minecraft 1.21.1 · NeoForge 21.1.267 · JDK 21.0.9 · Gradle 9.2.1 · Windows 11 ·
Mekanism + Mekanism Generators 10.7.19 (dev-only run `runClientMekanism`)

## Results (0.2.0)

| Suite | Command | Result |
|---|---|---|
| Clean build | `./gradlew clean build --no-build-cache` | **PASS**: BUILD SUCCESSFUL, 0 `warning:` lines from javac |
| Unit (JUnit 5, mod loaded) | part of `build` | **PASS**: 173 cases, 0 failures, 0 errors, 0 skipped |
| GameTests | `./gradlew runGameTestServer` | **PASS**: "All 55 required tests passed" |
| Generators | `gen_lang.py`, `generate_data.py`, `generate_guide_assets.py` with `--check` | **PASS**: 0 files would change (422 lang keys, 160 data files) |
| JAR content | `unzip -l` | **PASS**: no gametest classes, no `empty.nbt`; `neoforge.mods.toml` version 0.2.0, NeoForge `[21.1.267,)`, Minecraft `[1.21.1]` |
| Dev client + Mekanism (computer use, F2 screenshots) | `./gradlew runClientMekanism` | **PARTIAL**: see the table below |
| Dedicated server with 2+ players | — | **NOT TESTED** |
| Other mods (Thermal, Powah, XP fluids, pipes other than Mekanism) | — | **NOT TESTED** |

Artifact: `build/libs/ultraloot-0.2.0.jar`, 463,963 bytes, SHA-256
`cc6bafc852396196e2f330ce07fdff1fd73f580c58a2e1c1f3bfc9e173138518`.

### In-game checks (TEST_PLAN manual checks 14-19, plus 1-13 where reachable)

Evidence: `run-mekanism/screenshots/2026-11-22_18.4*.png` (guide), `18.52.*`-`18.59.*` (machines), `19.01.*`
(guide in pt_br). Values that are not visible in a screenshot were read with `/data get` (chat log).

| Check | Result | Evidence |
|---|---|---|
| 14 Status colours | **PARTIAL** | cyan "Running"; amber "! Not enough energy"; red "x Cost exceeds XP tank" (Tier I, evoker card, Quantity III: 1,200 XP/op > 1,000 tank); red "x This mob cannot be simulated" (giant card); gray "Waiting for a card" (Card Restorer). NO_LOOT_SOURCE not reproduced in the client (GameTest covers it) |
| 15 Card wear | **PARTIAL** | Hotbar bars: new card none, wear 1500 green, 2560 amber, 3072 empty. Zombie, Tier I: FE/op 720 / 1,080 / 1,440 (x1 / x1.5 / x2). Mob Configuration integrity row: BLOCKED (mouse) |
| 16 Card Restorer | **PARTIAL** | Corrupted card (wear 3072) restored to wear 0 and moved to the output slot; "Restoring" with quality bar 32% -> 64%, FE to finish 50,112 -> 26,880; then "Waiting for a card". Hoppers and OUTPUT_FULL not tested |
| 17 Recipe book | **BLOCKED** | needs mouse clicks |
| 18 Guide book | **PASS** | given on first join and not again on two later logins; chapter list, arrows/keys, crafting grid, hover tooltip, worn-card bars, reopens on last page; English 51 views, Portuguese 55 views. Tag-ingredient cycling not observed |
| 19 Mekanism | **PASS** | creative energy cube -> universal cable powered the Mob Loot Generator (Energy 0 -> 10,000), the Loot Generator and the Card Restorer. Hopper -> Logistical Transporter inserted a Mob Card through the top (XP Input) face; a transporter under the machine moved 19 Rotten Flesh into a chest |
| Loot Generator | **PASS** | Abandoned Mineshaft profile, "Running", 800 FE/op, chest on top filled with bread, rails, name tags, a golden apple, an enchanted book |
| 1-6 GUI tabs, upgrades, sides, profiles, scan list, filters | **BLOCKED** | mouse input did not reach the game (an always-on-top window covered it; keyboard worked). Main tabs of all three machines checked |

Notes from the client run:
- A Mekanism creative energy cube placed with `/setblock` starts empty and with no output side; it needed
  `energy_containers` and `component_config` NBT. This is Mekanism behaviour, not an Ultra Loot bug.
- The GUI progress bar and status update live while the screen is open (12% -> 44% in about 5 s at 300 ticks/op).

## Changes verified in this run
- Review P2: `MachineFamily#baseEnergyPerOperation` saturates instead of overflowing (new test
  `TierBalanceTest.hugeConfiguredCostSaturatesInsteadOfOverflowing`).
- Review P3: `MachineEnergyStorage#ensureCapacity` keeps the base capacity for a cost above
  `Balance.MAX_OPERATION_ENERGY`, so FE is never pulled in for an operation that can never run
  (`MachineEnergyStorageTest.capacityIsCappedAndOverflowSafe` updated).
- Review P3: guide stacks cached per screen; the first-join flag is recorded even while the gift is disabled.

---

# History: 0.1.1 test report

Date: 2026-11-20 · Minecraft 1.21.1 · NeoForge 21.1.267 · JDK 21.0.9 (JAVA_HOME) · Gradle 9.2.1 · Windows 11

## Results (0.1.1)

| Suite | Command | Result |
|---|---|---|
| Clean build | `./gradlew clean build --no-build-cache` | **PASS**: BUILD SUCCESSFUL, tests re-executed, no compiler warnings (Gradle prints its usual deprecation notice about the build tooling) |
| Unit (JUnit 5, mod loaded) | part of `build` | **PASS**: 129 cases, 0 failures, 0 errors, 0 skipped (5 new in `GuiSyncDeltaTest`) |
| GameTests | `./gradlew runGameTestServer` | **PASS**: "All 33 required tests passed" (2 new) |
| New GameTests without the fix | fix stashed, then `runGameTestServer` | **FAIL as expected**: `Expected status to be ENERGY_BUFFER_TOO_SMALL, but was NO_ENERGY` / `... XP_TANK_TOO_SMALL, but was NO_XP`. The tests detect the bug |
| Lang | `python -I tools/lang/gen_lang.py` | **PASS**: 265 keys, en_us and pt_br identical key sets (2 new keys) |
| Dev client smoke test | `./gradlew runClient` | **PASS (smoke only)**: "Ultra Loot 0.1.1" loaded, resource reload finished ("Sound engine started"), 0 ERROR lines in debug.log, no Ultra Loot warnings. The client was then stopped. No world was opened |
| Manual in-game checklist | TEST_PLAN.md | **NOT TESTED** (13 checks): needs a person at the client |
| Third-party mods (Mekanism, Thermal, Powah, pipes, XP fluids) | — | **NOT TESTED**: not in the dev environment |
| Dedicated server with 2+ players | — | **NOT TESTED** |
| Old error "Failed to find net/minecraft/server/MinecraftServer.class on the classpath" | `test`; `clean build`; `clean build --no-build-cache` | **Not reproduced** in 3 runs. Analysis and recovery steps are in docs/AUDIT.md §6 |

Artifact: `build/libs/ultraloot-0.1.1.jar`, 360,953 bytes, SHA-256
`51bc2ef5429ac9497687b48aea972b5696e45828225f95269637fa8f6521b18a`. The JAR's `neoforge.mods.toml` declares
version 0.1.1, NeoForge `[21.1.267,)` and Minecraft `[1.21.1]`.

Per-test status: `tests.json`. Coverage and the manual checklist: `docs/TEST_PLAN.md`.

## Bugs found in 0.1.1

| Id | Location | Description |
|---|---|---|
| BUG-03 | `AbstractMachineBlockEntity#tickOperation`, `MobLootGeneratorBlockEntity#checkCanOperate` | When the cost per operation exceeded the FE buffer or XP tank, the machine showed NO_ENERGY / NO_XP forever, although the cost could never be paid. Fixed, with regression GameTests |

---

# History: 0.1.0 test report (2026-11-19)

Date: 2026-11-19 · Minecraft 1.21.1 · NeoForge 21.1.267 · JDK 21.0.13 · Windows 11

## Results

| Suite | Command | Result |
|---|---|---|
| Build | `./gradlew build` | BUILD SUCCESSFUL, no compiler warnings |
| Unit (JUnit 5, mod loaded) | `./gradlew test` | 124 cases (116 methods), 0 failures, 0 skipped |
| GameTests (dedicated server) | `./gradlew runGameTestServer` | 31/31 required tests passed (QA: 4 consecutive runs; lead: final run) |
| Asset validation | `python tools/validate_assets.py` | 8 blocks, 26 items, 50 models, 63 textures — 0 errors |
| Lang check | lead script | 263 keys, en_us = pt_br key set, every literal key in code present |
| Dev client smoke test | `./gradlew runClient` | reached the main menu; no Ultra Loot model/texture/lang errors in the log |
| Manual in-game checklist | see TEST_PLAN.md | **not run** (13 checks) — GUI not yet validated by a person |

Per-test status: `tests.json`. Coverage and the manual checklist: `docs/TEST_PLAN.md`.

## Bugs found by the suite (fixed)

| Id | Location | Description |
|---|---|---|
| BUG-01 | `machine/SideConfiguration#fromArray` | Out-of-range saved side value became NONE instead of the family default |
| BUG-02 | `upgrade/UpgradeModifiers#energyPerOperation` | Floating-point noise added 1 FE to exact costs (881 instead of 880) |

Both have regression tests.

## Review findings (fixed during integration)

- Loot rolls / mob simulations that throw are caught (status Invalid Profile / Invalid Mob).
- Profile cache keyed on the server's reloadable registries (refreshes after `/reload`).
- Magma cube simulated at size 2 (otherwise no magma cream).
- Exploration-map detection follows nested loot table references; tables that cannot be encoded are rejected.
- Auto-discovered (modded) chest profiles gated behind Tier II and 1.5x energy (configurable).
- Mobs without a loot table cannot be imprinted.
- Manual GUI rescans throttled (20 ticks).

## Not covered

- Manual GUI checks (rendering, tab switching, tooltips, side-config clicks) — needs a person in the client.
- Auto-profile gating in-world: no `*:auto/*` profile exists without other mods (covered by a unit test instead).
- Interaction with real third-party mods (pipes, FE generators, XP fluid mods) — only NeoForge capabilities are tested.
- The planned independent architecture/performance cross review did not run (agent session limit reached).
