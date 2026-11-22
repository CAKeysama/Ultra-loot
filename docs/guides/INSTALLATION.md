# Installation guide

## Players (client)

1. Install **NeoForge 21.1.267 or newer** for **Minecraft 1.21.1** (the NeoForge installer, or a launcher such as
   Prism, CurseForge or Modrinth App with a NeoForge 1.21.1 instance).
2. Copy `ultraloot-<version>.jar` into the instance's `mods/` folder.
3. Install at least one mod that generates **Forge Energy (FE)** — the machines do not make their own power.
4. Optional: a recipe viewer (JEI or EMI). The vanilla recipe book does not list Ultra Loot recipes.

## Servers

1. Install the NeoForge 21.1 server and copy the same JAR into the server's `mods/` folder.
2. Every client that joins needs the mod too (it adds blocks, items and menus).
3. Start the server once. The config file `world/serverconfig/ultraloot-server.toml` is created per world; see the
   [configuration guide](CONFIGURATION.md). Changes are applied when the file is saved (no restart needed for most
   values; loot profile lists refresh on `/reload`).

The mod runs entirely on the server: machines tick only on the server, and the client only displays synced data.
It has been checked to load on a dedicated server (`runGameTestServer`).

## Datapacks

Loot profiles, mob XP costs, filters and blacklists can be changed with a datapack placed in the world's `datapacks/`
folder. See the [datapack guide](DATAPACKS.md). Apply changes with `/reload`.

## Developers

Requirements: JDK 21 (Gradle downloads everything else).

```bash
git clone https://github.com/CAKeysama/Ultra-loot.git
cd Ultra-loot
./gradlew build                # build/libs/ultraloot-<version>.jar
./gradlew runClient            # development client (run/ folder)
./gradlew runServer            # development server
./gradlew runGameTestServer    # automated in-world tests
```

Gradle finds a JDK 21 automatically (JAVA_HOME, standard install locations, or a download through the foojay
resolver). If yours is in a non-standard folder, add it to `org.gradle.java.installations.paths` in your personal
`~/.gradle/gradle.properties` (not the project's file, which must stay machine-independent).
