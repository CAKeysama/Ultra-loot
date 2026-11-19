# Changelog

All notable changes to Ultra Loot. Format based on [Keep a Changelog](https://keepachangelog.com/); versions follow
[Semantic Versioning](https://semver.org/).

## [0.1.0] — 2026-11-19 (unreleased)

First development version for Minecraft 1.21.1 / NeoForge 21.1.

### Added
- **Loot Generator**, tiers I–IV: rolls loot profiles with FE and inserts the result into nearby inventories with
  round robin and all-or-nothing (transactional) insertion; no output inventory, never drops items.
- **Mob Loot Generator**, tiers I–IV: simulates kills of the mob on its Mob Card with FE + stored XP, without spawning
  entities; 27-slot output with manual withdrawal and auto-export through Item Output faces.
- Upgrade cards Speed, Quantity, Range, Efficiency and Loot, levels I–III, one per category, with a central modifier
  service and an energy floor.
- Blank Mob Card / Mob Card (imprint a living mob), Experience Capsule, Blank Card, Machine Frame, Loot Core,
  Soul Core and Logic / Enhanced / Refined / Stellar circuits, with 33 recipes.
- Six-face side configuration (Energy Input, Item Target, XP Input, Item Output, None) with capability invalidation.
- Experience input from bottles, capsules, the `ultraloot:experience` capability, `c:experience` fluids and XP orbs
  (Range card).
- 66 loot profiles (all vanilla chest tables, fishing, archaeology, piglin bartering, 5 Ultra Loot tables) plus
  auto-discovery of other mods' chest tables, gated behind Tier II and 1.5x energy by default.
- Loot filters (All / Resources / Food / Equipment) unlocked by the Loot Card.
- `ultraloot:mob_simulation` data map with XP cost and energy multiplier for 60 vanilla mobs.
- Tabbed GUIs for both machines: Overview, Upgrades, Loot / Mob Configuration, Inventory Scan / Inventory,
  Side Configuration, Statistics.
- Server config `ultraloot-server.toml`.
- English and Brazilian Portuguese translations.
- 124 JUnit test cases and NeoForge GameTests.

### Safety
- Loot tables using `exploration_map` (directly or through nested table references) are never offered as profiles.
- Mobs without a loot table cannot be imprinted on a Mob Card.
- Rolls and simulations that throw are caught; the machine reports Invalid Profile / Invalid Mob instead of crashing.
- Manual rescans from the GUI are throttled to once per second.
