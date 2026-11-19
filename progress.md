# Ultra Loot — progress

Last update: 2026-11-19 (session 1, end)

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

## Environment
- JDK 21 (Temurin 21.0.13) at `C:/Users/conta/.jdks/jdk-21.0.13+9` (system Java is 8); also set in
  `gradle.properties` (`org.gradle.java.installations.paths`, machine-specific).
- Decompiled NeoForge/Minecraft sources at `C:/Users/conta/.mcsrc` for API lookups.

## Builds / tests (final)
- `./gradlew build` — green. `./gradlew test` — 124 cases, 0 failures. `./gradlew runGameTestServer` — 31/31.
- `./gradlew runClient` — main menu reached, no Ultra Loot asset errors. Details: `docs/TEST_REPORT.md`.

## Git
- Nothing committed or pushed. All work is uncommitted in the working tree on `main`.

## Next exact steps
1. Owner runs `./gradlew runClient` and walks the manual checklist in `docs/TEST_PLAN.md` (GUI).
2. Independent performance/persistence review (many machines, chunk unload mid-operation).
3. Owner decides on commit / branch / licence; then playtest balance.
