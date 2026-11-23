# Ultra Loot — progress

Last update: 2026-11-22 (session 3: remediation, 0.2.0)

| Phase | State |
|---|---|
| 0 Discovery | COMPLETED |
| 1 Project foundation | COMPLETED |
| 2 Shared core | COMPLETED |
| 3 Loot Generator | COMPLETED |
| 4 Mob Loot Generator | COMPLETED |
| 5 GUI | COMPLETED (compiled + smoke-loaded; manual visual check pending) |
| 6 Assets | COMPLETED |
| 7 Integration & QA | COMPLETED (architect performance review not run — agent session limit) |
| 8 Release candidate | COMPLETED — `build/libs/ultraloot-0.1.0.jar`, not published |

## Session 2 — audit (0.1.1)
| Phase | State |
|---|---|
| Discovery + dependency map | COMPLETED (docs/AUDIT.md §1) |
| Game design / balance / engineering / QA audits | COMPLETED (docs/GDD.md, docs/BALANCE.md, docs/AUDIT.md) |
| Fixes P1-01, P1-02, P2-01..03, P3-01, P3-08 | COMPLETED, tests green |
| Old MinecraftServer.class test error | analysed, not reproduced (docs/AUDIT.md §6) |
| Documentation | COMPLETED |
| Open owner decisions | FE buffer sizing (P1-01), tier FE efficiency (B-03), meaning of "hybrid loot" |

## Session 3 — remediation and owner decisions (0.2.0)
| Phase | State |
|---|---|
| Owner decisions (card wear + restorer, mob tiers, hybrid loot, Mekanism test, client test) | RECORDED (GDD §3.2.x, §4) |
| Dynamic FE buffer, tier FE efficiency, mob tiers, atomic order | COMPLETED, unit + GameTests green |
| Hybrid loot resolver + NO_LOOT_SOURCE | COMPLETED, tests green |
| Card wear + Card Restorer | COMPLETED, tests green |
| P3 fixes (recipe book, orb scan, scan caches, gametest source set, generators) | COMPLETED |
| GUI status classes + UI_UX_GUIDELINES.md | COMPLETED, checked in game (main tabs) |
| Ultra Loot Guide (in-game book, 13 chapters) | COMPLETED, 6 unit tests, checked in game (EN + PT-BR) |
| Docs (ARCHITECTURE, CHANGELOG 0.2.0, MACHINES, CONFIGURATION, DATAPACKS, KNOWN_LIMITATIONS, TEST_PLAN, TEST_REPORT, tests.json, README, AUDIT) | COMPLETED |
| Client GUI + Mekanism validation (computer use) | DONE (partial): checks 18, 19 pass; 14-16 partial; GUI tab clicks blocked (mouse input did not reach the game) |
| Independent review, clean build, final report | COMPLETED: review P2/P3 fixes applied; clean build, 173 unit, 55 GameTests green |
| GitHub release v0.2.0 (pre-release, JAR attached) | COMPLETED (owner request) |
| CurseForge page: logo, pixel-art description (`tools/generate_curseforge_art.py`, `curseforge/art/`) | COMPLETED |
| CurseForge project 1824987 created; file "Ultra Loot 0.2.0" (Release, NeoForge, 1.21.1, Java 21) uploaded | DONE, waiting for moderator approval |
| Chest picking for the Loot Generator (Inventory Scan: All in range / Picked only, checkboxes, NO_PICKED_TARGETS, guide page) | COMPLETED: 175 unit, 58 GameTests green; GUI clicks not checked in the game client |
| Release 0.3.0 (GitHub pre-release v0.3.0, JAR attached) | COMPLETED (owner request); CurseForge file still to upload |

## Environment
- JDK 21 found through JAVA_HOME (`C:\Program Files\Java\jdk-21`, 21.0.9). `~/.jdks` no longer exists; the
  machine-specific path was removed from `gradle.properties`.
- Decompiled NeoForge/Minecraft sources at `C:/Users/conta/.mcsrc` for API lookups (session 1; not re-checked).
## Builds / tests (final)
- 0.1.1 (2026-11-20): clean build green, unit 129/129, GameTests 33/33. Details: `docs/TEST_REPORT.md`.
- 0.2.0 (2026-11-22): `./gradlew clean build --no-build-cache` green -> `build/libs/ultraloot-0.2.0.jar`
  (463,963 bytes, SHA-256 cc6bafc8...8518); unit 173/173; `runGameTestServer` 55/55; generators `--check` clean;
  in-game checks with Mekanism 10.7.19 (see `docs/TEST_REPORT.md`).

## Git
- On `main`, pushed to https://github.com/CAKeysama/Ultra-loot (owner request): 87e991e (0.2.0), 4970713
  (CurseForge prep), 3ef03b4 (pixel-art description). Tag `v0.2.0`. Licence stays All Rights Reserved.
- The CurseForge description loads its images from `raw.githubusercontent.com/.../main/curseforge/art/`:
  keep those files on `main` (moving or renaming them breaks the page).

## Next exact steps
1. Wait for CurseForge moderation of project 1824987; optionally add `curseforge/screenshots/` to its gallery
   (Media tab).
2. Manual GUI clicks that could not be automated: tabs, upgrade slots, side configuration, profile selector,
   scan list, filters, recipe book (TEST_PLAN 1-6, 17), plus hoppers on the Card Restorer.
3. Dedicated server with two players; playtest the balance numbers (all still placeholders until played).
