# Configuration guide

Ultra Loot has one **server** config, created per world at `world/serverconfig/ultraloot-server.toml`
(single player: `saves/<world>/serverconfig/`). To use the same values for every new world, copy the file to the
instance's `defaultconfigs/` folder.

Values are re-read when the file is saved. Profile-related options (auto-discovery, tier and multiplier of auto
profiles) take effect on the next `/reload`.

```toml
[lootGenerator]
	# Ticks per Loot Generator operation at 1x speed (20 - 72000)
	baseTicks = 200
	# Loot Generator FE per operation (E_base) for tiers I-IV
	energyPerOperation = [800, 1600, 3000, 6000]
	# Ticks between nearby inventory scans (20 - 6000)
	scanIntervalTicks = 100
	# Expose every chests/* loot table (any mod) as a selectable loot profile
	autoDiscoverChestTables = true
	# Minimum machine tier for auto-discovered chest profiles (1 - 4)
	autoProfileMinTier = 2
	# Energy multiplier for auto-discovered chest profiles (0.1 - 100.0)
	autoProfileEnergyMultiplier = 1.5

[mobLootGenerator]
	# Ticks per Mob Loot Generator operation at 1x speed (20 - 72000)
	baseTicks = 300
	# Mob Loot Generator FE per operation (E_base) for tiers I-IV
	energyPerOperation = [1200, 2400, 4500, 9000]
	# XP points per simulated kill for mobs without a data map entry (0 - 10000)
	defaultXpPerKill = 8

[experience]
	# mB of experience fluid (tag c:experience) per XP point (1 - 1000)
	fluidRatio = 20

[energy]
	# Minimum fraction of E_base an operation costs regardless of upgrades (0.05 - 1.0)
	minFraction = 0.25
```

## Notes

- **Operation time** = `baseTicks / (tier speed × Speed card)`, never below 5 ticks.
- **Energy per operation** = `E_base × card multipliers × profile (or mob) multiplier`, never below
  `minFraction × E_base` and never below 1 FE. See the [cards guide](CARDS.md).
- `energyPerOperation` must contain four positive integers (one per tier). Non-positive entries are corrected by
  NeoForge's config validation; a list with a different length is ignored with a warning and the current values are kept.
- `fluidRatio = 20` matches the usual convention of other experience-fluid mods. Change it only if your pack uses a
  different ratio.
- Tier speeds, capacities, ranges, card values and slot counts are fixed design values in code
  (`config/Balance.java`) and are not configurable in this version.
- Per-profile tiers and costs are datapack content, not config: see the [datapack guide](DATAPACKS.md).
- Per-mob XP costs live in the `ultraloot:mob_simulation` data map (datapack).
