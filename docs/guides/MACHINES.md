# Machines guide

Both families come in four tiers. All values are defaults from `config/Balance` (some are server-configurable).

| | Tier I | Tier II | Tier III | Tier IV |
|---|---|---|---|---|
| Base speed | 1x | 1.5x | 2x | 3x |
| Upgrade slots | 5 | 5 | 5 | 5 |
| FE buffer | 10,000 | 25,000 | 50,000 | 100,000 |
| Max FE input / transfer | 250 | 625 | 1,250 | 2,500 |

## Loot Generator

Rolls a **loot profile** (a loot table + context) and puts the results straight into nearby inventories.

- Consumes FE only. No internal output inventory, never drops items, never overwrites items.
- Base cycle: 200 ticks at 1x speed (10 s).

| | Tier I | Tier II | Tier III | Tier IV |
|---|---|---|---|---|
| Rolls per operation | 1 | 2 | 3 | 4 |
| Scan range (cube) | 5 | 8 | 12 | 16 |
| E_base FE / operation | 800 | 1,600 | 3,000 | 6,000 |

How it works:
1. Every 100 ticks it scans the block entities of already-loaded chunks within range and caches every inventory
   it may use (chests, double chests counted once, barrels, shulker boxes; in **Any inventory** mode any modded
   inventory except blacklisted ones). Other Ultra Loot machines are never targets.
2. An inventory touching the machine is only used if that face is set to **Item Target** (so it never stuffs loot
   into, say, a battery behind its Energy Input face).
3. When the progress bar completes it rolls the table (each roll is independent), applies the filter, then
   **simulates** the insertion: items are distributed **round robin** (each stack starts at the next inventory and
   spills over to the following ones; full ones are skipped). Only if everything fits is it committed and the FE
   paid. Otherwise nothing is generated, nothing is consumed, and the machine shows "Inventories are full".
4. Profiles have a minimum tier; higher-value profiles cost more FE.

## Mob Loot Generator

Simulates kills of the mob stored on a **Mob Card** using the mob's own loot table. No entity is ever spawned.

- Consumes FE and XP. Has an XP tank, a 27-slot output inventory, a Mob Card slot and an XP item slot.
- Base cycle: 300 ticks at 1x speed (15 s). Each simulated kill costs the mob's XP cost (default 8 points).

| | Tier I | Tier II | Tier III | Tier IV |
|---|---|---|---|---|
| Simulated kills / operation | 1 | 1 | 2 | 2 |
| XP tank (points) | 1,000 | 4,000 | 12,000 | 40,000 |
| Max XP input / transfer | 50 | 150 | 400 | 1,000 |
| E_base FE / operation | 1,200 | 2,400 | 4,500 | 9,000 |

Getting a Mob Card: craft a **Blank Mob Card** and use it on a living mob (the mob is not hurt). Bosses and
blacklisted entities can't be imprinted.

Getting XP into the machine:
- **XP slot** (Overview tab): Bottles o' Enchanting (7 points each) or a filled **Experience Capsule**
  (sneak + use the capsule to store your own XP into it, up to 1,395 points = level 30).
- **XP Input faces**: the `ultraloot:experience` capability (points) and any fluid tagged `c:experience`
  (20 mB = 1 point) from other mods' pipes; hoppers can insert bottles/capsules and pull empty capsules.
- **Range Card**: absorbs XP orbs around the machine.

Output: take items by hand (Inventory tab) and/or let **Item Output** faces push into the adjacent inventory every
10 ticks (up to 64 items per face) — both work at the same time. Pipes can also pull from Item Output faces.
If a whole operation's drops don't fit, the machine pauses ("Output is full") without consuming anything.

Breaking a machine drops its cards and items; stored XP is released as experience orbs.

## Recipes

All recipes are shaped crafting-table recipes unless noted. They have no recipe-book unlock advancements; use JEI/EMI
or this list.

### Components

| Item | Ingredients |
|---|---|
| Machine Frame | 4 iron ingots, 4 glass, 1 redstone |
| Logic Circuit (x2) | 4 copper ingots, 4 redstone, 1 iron ingot |
| Enhanced Circuit | 4 gold ingots, 2 redstone, 2 Logic Circuits, 1 diamond |
| Refined Circuit | 4 emeralds, 1 amethyst shard, 2 quartz, 1 Enhanced Circuit, 1 netherite scrap |
| Stellar Circuit | 4 diamonds, 1 dragon's breath, 2 end crystals, 1 nether star, 1 Refined Circuit |
| Loot Core | 4 gold ingots, 2 lapis lazuli, 2 ender pearls, 1 chest |
| Soul Core | 4 rotten flesh, 1 bone, 1 string, 2 soul sand or soul soil, 1 redstone |
| Blank Card (x2) | 4 paper, 1 redstone, 1 gold nugget |
| Blank Mob Card (shapeless) | Blank Card, soul sand or soul soil, bone |
| Experience Capsule | 1 iron ingot, 4 glass, 1 emerald, 1 redstone |

### Machines

The previous tier goes in the centre of the upgrade recipes.

| Machine | Ingredients |
|---|---|
| Loot Generator I | Machine Frame, Loot Core, Logic Circuit, 4 iron ingots, 2 chests |
| Mob Loot Generator I | Machine Frame, Soul Core, Logic Circuit, 4 iron ingots, 2 iron swords |
| Tier II (both) | Tier I machine, 5 gold ingots, 2 diamonds, 1 Enhanced Circuit |
| Tier III (both) | Tier II machine, 5 emeralds, 1 Refined Circuit, 1 netherite ingot |
| Tier IV (both) | Tier III machine, 4 shulker shells, 2 diamond blocks, 1 Stellar Circuit, 1 nether star |

### Upgrade cards

Level I: a Blank Card in the centre surrounded by the materials below.

| Card | Level I |
|---|---|
| Speed | 4 sugar, 4 redstone |
| Quantity | 1 hopper, 2 chests, 1 redstone |
| Range | 1 ender pearl, 2 redstone, 1 compass |
| Efficiency | 4 copper ingots, 4 lapis lazuli |
| Loot | 4 gold ingots, 1 rabbit's foot, 3 lapis lazuli |

Levels II and III use the pattern `M A M / A X A / M C M`: **X** is the previous level of the card, **C** an Enhanced
Circuit (II) or Refined Circuit (III), **M** the four corners and **A** the three edges.

| Card | Level II (M + A) | Level III (M + A) |
|---|---|---|
| Speed | sugar + glowstone dust | redstone block + blaze powder |
| Quantity | iron ingot + chest | diamond + hopper |
| Range | amethyst shard + ender pearl | eye of ender + spyglass |
| Efficiency | lapis lazuli + gold ingot | lapis block + emerald |
| Loot | gold ingot + lapis block | diamond + rabbit's foot |
