# Datapacks: loot profiles, mob costs and tags

Everything below is reloadable with `/reload` unless noted.

## 1. Loot profiles (Loot Generator)

A profile is a loot source the player can select in the **Loot Configuration** tab.
File: `data/<namespace>/ultraloot/loot_profiles/<path>.json` → profile id `<namespace>:<path>`.

```json
{
  "loot_table": "minecraft:chests/simple_dungeon",
  "context": "chest",
  "category": "dungeon",
  "icon": "minecraft:mossy_cobblestone",
  "min_tier": 1,
  "energy_multiplier": 1.0
}
```

| Field | Required | Default | Meaning |
|---|---|---|---|
| `loot_table` | yes | — | Any loot table id: vanilla, another mod's, or your own |
| `context` | no | `chest` | How the table is executed (see below) |
| `category` | no | `custom` | Grouping label shown in the GUI |
| `icon` | no | `minecraft:chest` | Item shown next to the profile |
| `min_tier` | no | `1` | Lowest machine tier (1-4) allowed to select it |
| `energy_multiplier` | no | `1.0` | Multiplies the tier's base FE per operation (0.1-100) |

Contexts — the generator builds exactly the parameter set the table type expects:

| `context` | Vanilla parameter set | Parameters provided |
|---|---|---|
| `chest` | `minecraft:chest` | origin (machine centre) |
| `fishing` | `minecraft:fishing` | origin, tool = plain fishing rod, this_entity = a detached open-water bobber (never added to the world) |
| `archaeology` | `minecraft:archaeology` | origin |
| `barter` | `minecraft:barter` | this_entity = a piglin instance that is never added to the world |
| `empty` | `minecraft:empty` | none |

Luck from the Loot Card is passed as the context luck; it only changes results for tables that use `quality`.

Display name: add the translation key `ultraloot.profile.<namespace>.<path with / replaced by .>` to a resource pack;
otherwise the last path segment is shown.

### Auto-discovered profiles

With `lootGenerator.autoDiscoverChestTables = true` (server config, default), every loot table whose path starts
with `chests/` in any namespace (vanilla and every installed mod) becomes a profile `<namespace>:auto/<path>` with
context `chest`, minimum tier `lootGenerator.autoProfileMinTier` (default 2) and energy multiplier
`lootGenerator.autoProfileEnergyMultiplier` (default 1.5), unless an explicit profile already uses that table.
Ultra Loot ships explicit profiles for every vanilla `chests/*` table, so auto profiles are mostly other mods' chests.
Write an explicit profile for a modded table to give it a proper tier and cost.

### Rejected tables

Tables that contain the `minecraft:exploration_map` function — directly or through a nested `minecraft:loot_table`
reference — are never offered (that function searches the world for a structure on every roll). Tables that cannot
be inspected (encoding fails) are not offered either. Tables whose path contains `inject` are skipped by
auto-discovery.

### Your own loot tables

Put them at `data/<namespace>/loot_table/<path>.json` with `"type": "minecraft:chest"` and point a profile at them.

## 2. Mob simulation costs (Mob Loot Generator)

Data map `ultraloot:mob_simulation` on entity types. File:
`data/<namespace>/data_maps/entity_type/mob_simulation.json`

```json
{
  "values": {
    "minecraft:blaze": { "tier": 2, "xp_cost": 12, "energy_multiplier": 1.2 },
    "mymod:frost_golem": { "tier": 3, "xp_cost": 30, "energy_multiplier": 2.0 },
    "mymod:ice_wisp": {
      "xp_cost": 8,
      "supplemental": { "mode": "add", "loot_table": "mymod:extra/ice_wisp" },
      "fallback_loot_table": "mymod:entities/generic_spirit"
    }
  }
}
```

| Field | Required | Default | Meaning |
|---|---|---|---|
| `xp_cost` | yes | - | XP points per simulated kill (0-100000) |
| `energy_multiplier` | no | `1.0` | multiplies E_base (0.1-100) |
| `tier` | no | `2` | mob reward class I-III; shown on the card. Keep costs inside the tier band (docs/BALANCE.md 7.3) |
| `cost_exception` | no | - | text explaining why the cost is outside the band (checked by the test suite for shipped data) |
| `supplemental` | no | - | `{ "mode": "add" \| "replace" \| "disabled", "loot_table": "<id>" }` |
| `fallback_loot_table` | no | - | table used only when the mob has no native loot table |

Mobs without an entry use `mobLootGenerator.defaultXpPerKill` (default 8), 1.0x and tier II. Data maps reload with
`/reload`.

### Hybrid loot: how the drops of one simulated kill are chosen

1. **Native table first.** The mob's own loot table (vanilla or modded, whatever `Mob#getLootTable` returns) is used
   whenever it exists. A table that exists but rolls nothing (goat, fox, silverfish...) is a valid empty result: the
   machine runs, pays, and produces nothing. It never triggers the fallback.
2. **Supplemental** (only when you configure it): `add` rolls the extra table on every kill in addition to the
   primary table; `replace` rolls only the extra table; `disabled` keeps the entry for documentation with no effect.
   Ultra Loot never adds a supplemental table by itself.
3. **Fallback** (only when you configure it): used as the primary table when the native table does not exist.
4. A configured table that does not exist is an error: the machine shows **No loot table for this mob** (red), the
   tooltip says which table is missing, the server log reports it once per mob, and nothing is consumed. Blank Mob
   Cards refuse to imprint mobs whose loot cannot be resolved.

### Mob Card wear

Each simulated kill wears the card by 1 (committed only with a successful operation). After
`mobCard.integrity` kills (default 2,048) the card starts to decay over `mobCard.decaySpan` kills (default 1,024):
each kill yields one Rotten Flesh instead of its loot with probability equal to the decay, and the FE cost rises up
to 2x. A fully corrupted card keeps working (Rotten Flesh only) and never breaks. Repair it in a Card Restorer.
Wear is stored in the `ultraloot:card_wear` item component.

## 3. Tags

| Tag | Type | Use |
|---|---|---|
| `ultraloot:storage_targets` | block | Inventories filled in "Storage only" target mode |
| `ultraloot:scan_blacklist` | block | Never filled, even in "Any inventory" mode |
| `ultraloot:loot_filter/resources` | item | Kept by the Resources filter |
| `ultraloot:loot_filter/food` | item | Kept by the Food filter (anything edible is kept anyway) |
| `ultraloot:loot_filter/equipment` | item | Kept by the Equipment filter (damageable items are kept anyway) |
| `ultraloot:mob_card_blacklist` | entity_type | Mobs that cannot be imprinted or simulated |
