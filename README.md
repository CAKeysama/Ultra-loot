# Ultra Loot

Tech machines that generate loot from loot tables, for **Minecraft Java 1.21.1** on **NeoForge 21.1**.

- **Loot Generator** — runs FE through a loot profile (dungeon, stronghold, fishing, archaeology, piglin bartering,
  trial chambers, ...) and inserts the result into nearby inventories. It has no output inventory and never drops items:
  if the loot does not fit, it waits.
- **Mob Loot Generator** — reads a Mob Card and simulates kills of that mob using FE and stored experience.
  No entity is ever spawned. Drops go to a 27-slot output that can be emptied by hand or exported through configured faces.

Both families come in four tiers and accept five kinds of upgrade cards.

| | Tier I | Tier II | Tier III | Tier IV |
|---|---|---|---|---|
| Speed | 1x | 1.5x | 2x | 3x |
| FE capacity | 10,000 | 25,000 | 50,000 | 100,000 |
| Upgrade slots | 5 | 5 | 5 | 5 |
| Loot Generator rolls / operation | 1 | 2 | 3 | 4 |
| Loot Generator range (blocks) | 5 | 8 | 12 | 16 |
| Mob Loot Generator kills / operation | 1 | 1 | 2 | 2 |
| Mob Loot Generator XP tank | 1,000 | 4,000 | 12,000 | 40,000 |

## Features

- **Transactional insertion**: loot is planned against a model of every target inventory and only committed when all of
  it fits. Round robin across targets. FE is charged only after a successful delivery. Nothing is lost or duplicated.
- **66 shipped loot profiles** covering every vanilla chest table plus fishing, archaeology, piglin bartering and five
  Ultra Loot tables; chest tables from other mods are discovered automatically (gated behind Tier II by default).
- **Upgrade cards** — Speed, Quantity, Range, Efficiency and Loot, each in levels I–III. One card per category; a higher
  level replaces a lower one. Every effect goes through one modifier service:
  `E_final = E_base × M_speed × M_quantity × M_range × M_efficiency (× M_loot × profile)`, with a floor of 25 % of `E_base`.
- **Mob Cards** — right-click a living mob with a Blank Mob Card to imprint it. The mob is unharmed.
- **Experience input** — Bottles o' Enchanting, the Experience Capsule item, the `ultraloot:experience` block capability,
  any `c:experience` fluid (20 mB per point) and, with a Range card, nearby XP orbs.
- **Six-face side configuration** for energy, item targets, XP input and item output.
- **Tabbed GUI** — Overview, Upgrades, Loot/Mob Configuration, Inventory Scan / Inventory, Side Configuration and Statistics.
- **Data driven** — loot profiles, mob XP costs (data map), filters and blacklists are datapack content.
- **Server config** for timings, energy costs and auto-discovery.
- English and Brazilian Portuguese translations.

## Requirements

- Minecraft Java Edition **1.21.1**
- NeoForge **21.1.267** or newer in the 21.1 line
- Any mod that produces Forge Energy (FE) — for example a generator from a tech mod — to power the machines.

## Installation

Copy `ultraloot-<version>.jar` into the `mods/` folder of a NeoForge 1.21.1 instance (client and/or server).
The mod is required on both sides. See [docs/guides/INSTALLATION.md](docs/guides/INSTALLATION.md).

## Getting started

1. Craft a **Machine Frame**, a **Logic Circuit** and a **Loot Core**, then a **Loot Generator (Tier I)**.
2. Place chests or barrels within 5 blocks, power the machine with FE through an Energy face (back and bottom by default).
3. Open the machine, go to **Loot Configuration** and choose a profile, e.g. *Dungeon*.
4. Add upgrade cards in the **Upgrades** tab. Check reach and free space in **Inventory Scan**.

For the Mob Loot Generator, craft a **Soul Core** instead of the Loot Core, imprint a **Blank Mob Card** on a mob,
insert it in the **Mob Configuration** tab and feed experience through the XP slot or an XP face.

Full recipe list and behaviour: [docs/guides/MACHINES.md](docs/guides/MACHINES.md).

## Documentation

| Guide | Contents |
|---|---|
| [Machines](docs/guides/MACHINES.md) | Both machine families, statuses, recipes |
| [Upgrade cards](docs/guides/CARDS.md) | Card values and the energy formula |
| [Side configuration](docs/guides/SIDE_CONFIGURATION.md) | Face modes and defaults |
| [Datapacks](docs/guides/DATAPACKS.md) | Loot profiles, mob XP costs, tags |
| [Installation](docs/guides/INSTALLATION.md) | Players, servers and developers |
| [Configuration](docs/guides/CONFIGURATION.md) | `ultraloot-server.toml` options |
| [Known limitations](docs/KNOWN_LIMITATIONS.md) | What does not work (yet) |

Developer documentation: [architecture](docs/ARCHITECTURE.md), [technical decisions](docs/TECHNICAL_DECISIONS.md),
[test plan](docs/TEST_PLAN.md), [test report](docs/TEST_REPORT.md), [asset spec](docs/ASSET_SPEC.md).

## Building from source

Requires JDK 21.

```bash
./gradlew build               # JAR in build/libs/ultraloot-<version>.jar (runs unit tests)
./gradlew test                # JUnit 5 unit tests
./gradlew runGameTestServer   # in-world NeoForge GameTests
./gradlew runClient           # development client
```

Asset and data generators (Python 3, no dependencies):

```bash
python tools/generate_assets.py        # block/item textures and models
python tools/generate_gui_textures.py  # GUI textures
python tools/generate_data.py          # loot profiles, recipes, tags, data map
python tools/lang/gen_lang.py          # en_us / pt_br
python tools/validate_assets.py        # checks every registered id has a model and texture
```

## Credits and licence

Created by CAKeysama. The GUI layout takes UX inspiration from tabbed tech-mod machines; all code, textures and models
are original to Ultra Loot. Licence: All Rights Reserved (see `neoforge.mods.toml`).
