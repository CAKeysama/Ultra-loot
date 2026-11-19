# Ultra Loot 0.1.0 — test report

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
