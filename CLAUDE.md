# Ultra Loot — project guide for Claude Code

Ultra Loot is a NeoForge tech mod for Minecraft Java 1.21.1 (formerly codenamed "LootCore" — that name is obsolete, never use it).

- Display name: **Ultra Loot**
- Mod id: `ultraloot`
- Base package: `io.github.cakeysama.ultraloot`
- Minecraft 1.21.1 · NeoForge 21.1.x · Java 21 · ModDevGradle 2.0.x · Gradle wrapper 9.2.1

## Build & test

Needs a JDK 21. Gradle finds it through `JAVA_HOME`, standard install locations or the foojay resolver; the
project's `gradle.properties` holds no machine-specific path (put one in `~/.gradle/gradle.properties` if needed).
On this machine `JAVA_HOME` is `C:\Program Files\Java\jdk-21` (21.0.9).

```bash
./gradlew compileJava          # fast compile check
./gradlew test                 # JUnit 5 (MDG unitTest, mod loaded)
./gradlew runGameTestServer    # NeoForge GameTests, exits with failure count
./gradlew build                # JAR -> build/libs/ultraloot-<version>.jar
```

## Session protocol

On resume read `progress.md`, `tests.json`, this file and `git status` / `git log` before continuing.
Update `progress.md` after each phase. Never push, publish, force-push, `git reset --hard` or `git clean`.

## Code map

| Package | Owner role | Contents |
|---|---|---|
| `registry/` | lead | DeferredRegisters (blocks, items, BEs, menus, components, tabs) |
| `config/` | lead | `Balance` (all tunable numbers), server config spec, side configuration |
| `machine/` | machine-engineer | tiers, families, base block/BE, both machine BEs |
| `energy/`, `experience/` | machine-engineer | FE storage, XP storage + capability + adapters |
| `inventory/` | machine-engineer | inventory scanner, transactional insertion |
| `upgrade/` | loot-engineer | cards, upgrade inventory, modifier service |
| `loot/`, `mob/` | loot-engineer | loot profiles, context adapters, filters, mob simulation, mob cards |
| `menu/`, `network/` | lead | container menus, payloads |
| `client/` | ui-asset-engineer | screens, tabs, widgets (client-only, never referenced from common code) |
| `gametest/` + `src/test` | qa-engineer | GameTests and unit tests |

Resources: `assets/ultraloot/**` (ui-asset-engineer, lang owned by lead), `data/ultraloot/**` (loot-engineer).

## Rules

- Server-authoritative: the client only renders synced data; every C2S payload is validated.
- No item/FE/XP loss or duplication: simulate, then commit; persist everything.
- No per-tick world scans; use intervals + caches.
- All balance numbers live in `config/Balance` — no magic numbers elsewhere.
- No empty methods or TODOs standing in for required features.
- Client classes stay in `client/` and are only reached through `@EventBusSubscriber(value = Dist.CLIENT)` / client entrypoints.
