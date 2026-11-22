## Ultra Loot 0.2.0

**Added**
- Mob Card wear: cards decay after 2,048 kills (more Rotten Flesh, up to 2x FE) and never break.
- Card Restorer: repairs worn Mob Cards with FE.
- Hybrid mob loot: native table, plus datapack add/replace tables and a fallback for mobs without one.
- Mob reward tiers I–III with their own FE and XP costs.
- Ultra Loot Guide: in-game book (13 chapters, English and Portuguese), given on first join.
- Status line with a shape per state (working, idle, waiting, blocked) and a "No loot table for this mob" status.
- Mob Cards can be inserted by pipes through an XP Input face. Recipes unlock in the recipe book.

**Changed**
- The FE buffer grows to hold one operation, so big card setups never get stuck.
- Higher tiers are now more energy efficient per roll and per kill.
- Config keys renamed: `lootGenerator.energyPerAttempt`, `mobLootGenerator.energyPerKill`.

**Fixed**
- Machines stuck on "Not enough energy / XP" when one operation was larger than the buffer or tank.
- XP orb collection stopping at the first orb that did not fit.
- Mobs with an empty loot table were refused by Blank Mob Cards.

Worlds from 0.1.x load without changes. Full changelog:
https://github.com/CAKeysama/Ultra-loot/blob/main/CHANGELOG.md
